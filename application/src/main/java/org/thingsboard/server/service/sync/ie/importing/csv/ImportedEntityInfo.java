// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.ie.importing.csv;

import lombok.Data;

@Data
public class ImportedEntityInfo<E> {
    private E entity;
    private boolean isUpdated;
    private E oldEntity;
}
