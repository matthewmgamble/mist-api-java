package ca.mgamble.mist;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** A local HTTP server standing in for the Mist API in tests. */
final class FakeMist implements AutoCloseable {

    record Request(String method, String path, Map<String, String> query, Map<String, List<String>> headers,
                   String body) {
        String header(String name) {
            return headers.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(name))
                    .map(e -> e.getValue().get(0))
                    .findFirst().orElse(null);
        }
    }

    record Response(int status, Map<String, String> headers, String body) {
        static Response json(String body) {
            return new Response(200, Map.of(), body);
        }

        static Response status(int status, String body) {
            return new Response(status, Map.of(), body);
        }

        Response withHeader(String name, String value) {
            Map<String, String> h = new LinkedHashMap<>(headers);
            h.put(name, value);
            return new Response(status, h, body);
        }
    }

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile Function<Request, Response> handler = r -> Response.status(404, "{\"detail\":\"not stubbed\"}");

    FakeMist() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", exchange -> {
            try (exchange) {
                String body;
                try (InputStream in = exchange.getRequestBody()) {
                    body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                Request request = new Request(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
                        parseQuery(exchange.getRequestURI().getRawQuery()), exchange.getRequestHeaders(), body);
                requests.add(request);
                Response response = handler.apply(request);
                response.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                byte[] bytes = response.body() == null ? new byte[0] : response.body().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
                if (bytes.length > 0) {
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                }
            }
        });
        server.start();
    }

    /** Responds to every request with this function. */
    FakeMist handle(Function<Request, Response> handler) {
        this.handler = handler;
        return this;
    }

    /** Responds to successive requests with these responses; the last one repeats. */
    FakeMist respond(Response... responses) {
        List<Response> queue = new ArrayList<>(List.of(responses));
        return handle(r -> queue.size() > 1 ? queue.remove(0) : queue.get(0));
    }

    String baseUrl() {
        return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
    }

    List<Request> requests() {
        return requests;
    }

    Request lastRequest() {
        return requests.get(requests.size() - 1);
    }

    /** A client pointed at this server that records retry waits instead of sleeping. */
    MistClient client(List<Duration> sleeps) {
        return clientBuilder(sleeps).build();
    }

    MistClient.Builder clientBuilder(List<Duration> sleeps) {
        return MistClient.builder().apiToken("test-token").baseUrl(baseUrl()).sleeper(sleeps::add);
    }

    static String fixture(String operationId) {
        try {
            return Files.readString(Path.of("src/test/resources/fixtures", operationId + ".json"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> query = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return query;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            query.put(key, value);
        }
        return query;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
