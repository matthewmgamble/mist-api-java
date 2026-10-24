/** A Java client for the Juniper Mist cloud API. */
module ca.mgamble.mist {
    requires transitive com.fasterxml.jackson.databind;
    requires transitive java.net.http;
    requires org.slf4j;

    exports ca.mgamble.mist;
    exports ca.mgamble.mist.cache;
    exports ca.mgamble.mist.model;
    exports ca.mgamble.mist.query;

    opens ca.mgamble.mist.model to com.fasterxml.jackson.databind;
}
