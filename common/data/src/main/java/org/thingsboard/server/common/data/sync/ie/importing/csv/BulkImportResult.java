// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie.importing.csv;

import lombok.Data;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class BulkImportResult<E> {
    private AtomicInteger created = new AtomicInteger();
    private AtomicInteger updated = new AtomicInteger();
    private AtomicInteger errors = new AtomicInteger();
    private Collection<String> errorsList = new ConcurrentLinkedDeque<>();
}
