// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PublicLoginRequest {

    private String publicId;

    @JsonCreator
    public PublicLoginRequest(@JsonProperty("publicId") String publicId) {
        this.publicId = publicId;
   }

    public String getPublicId() {
        return publicId;
    }

}
