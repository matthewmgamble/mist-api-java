package ca.mgamble.mist.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Null-normalising helpers for record compact constructors. */
final class Models {

    private Models() {
    }

    static Map<String, JsonNode> extra(Map<String, JsonNode> extra) {
        // LinkedHashMap rather than Map.copyOf: preserves field order and permits JSON nulls as values
        return extra == null || extra.isEmpty() ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(extra));
    }

    static <T> List<T> list(List<T> list) {
        return list == null || list.isEmpty() ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
    }
}
