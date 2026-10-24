package ca.mgamble.mist.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.util.Map;

/**
 * Common behaviour for Mist response models.
 * <p>
 * Models map the commonly used fields of each Mist object. Every other field the API returns is kept in
 * {@link #extra()}, so nothing is lost when Mist adds fields or when you need something this library
 * does not model yet; it is also written back out when the model is serialized.
 */
public interface MistModel {

    /** Fields returned by the API that this model does not map explicitly. Never null. */
    Map<String, JsonNode> extra();

    /**
     * Returns an unmapped field, or a {@link MissingNode} if absent, so lookups can be chained:
     * {@code device.extra("env_stat").path("cpu_temp").asInt()}.
     */
    default JsonNode extra(String name) {
        JsonNode node = extra().get(name);
        return node == null ? MissingNode.getInstance() : node;
    }
}
