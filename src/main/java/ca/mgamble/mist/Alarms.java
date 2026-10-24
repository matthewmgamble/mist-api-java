package ca.mgamble.mist;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;

/** Request bodies for the alarm ack/unack endpoints. */
final class Alarms {

    private Alarms() {
    }

    static ObjectNode noteBody(String note) {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        if (note != null) {
            body.put("note", note);
        }
        return body;
    }

    static ObjectNode idsBody(Collection<String> alarmIds, String note) {
        if (alarmIds.isEmpty()) {
            throw new IllegalArgumentException("alarmIds must not be empty");
        }
        ObjectNode body = noteBody(note);
        body.putArray("alarm_ids").addAll(alarmIds.stream().map(JsonNodeFactory.instance::textNode).toList());
        return body;
    }
}
