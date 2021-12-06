package org.eclipse.dataspaceconnector.spi.transfer.synchronous;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

//@JsonTypeName("dataspaceconnector:proxyentry")
public class ProxyEntry {

    private String type;
    private Map<String, Object> properties;

    private ProxyEntry() {
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String type;
        private Map<String, Object> properties;

        private Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public ProxyEntry build() {
            ProxyEntry proxyEntry = new ProxyEntry();
            proxyEntry.properties = properties;
            proxyEntry.type = type;
            return proxyEntry;
        }
    }
}