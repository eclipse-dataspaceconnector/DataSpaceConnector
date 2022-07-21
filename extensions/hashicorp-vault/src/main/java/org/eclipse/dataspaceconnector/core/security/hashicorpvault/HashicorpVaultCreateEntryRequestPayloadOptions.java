/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = HashicorpVaultCreateEntryRequestPayloadOptions.Builder.class)
class HashicorpVaultCreateEntryRequestPayloadOptions {
    @JsonProperty("cas")
    private Integer cas;

    HashicorpVaultCreateEntryRequestPayloadOptions(Integer cas) {
        this.cas = cas;
    }

    public Integer getCas() {
        return this.cas;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private Integer cas;

        Builder() {
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("cas")
        public Builder cas(Integer cas) {
            this.cas = cas;
            return this;
        }

        public HashicorpVaultCreateEntryRequestPayloadOptions build() {
            return new HashicorpVaultCreateEntryRequestPayloadOptions(cas);
        }
    }
}
