// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DomainOauth2ClientCompositeKey implements Serializable {

    @Transient
    private static final long serialVersionUID = -245388185894468455L;

    private UUID domainId;
    private UUID oauth2ClientId;

}
