// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.edqs.query.SortableEntityData;

import java.util.List;

public interface EntityQueryProcessor {

    List<SortableEntityData> processQuery();

    long count();

}
