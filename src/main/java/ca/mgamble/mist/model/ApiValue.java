package ca.mgamble.mist.model;

/** An enum whose wire value differs from its Java name. */
public interface ApiValue {

    /** The value sent to / received from the Mist API. */
    String apiValue();
}
