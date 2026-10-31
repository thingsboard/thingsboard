// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.cf;

import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.page.PageData;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface NativeCalculatedFieldRepository {

    PageData<CalculatedField> findCalculatedFields(Pageable pageable);

    void findAllCalculatedFields(int pageSize, Consumer<List<CalculatedField>> pageConsumer);

    void findCalculatedFieldsByTenantId(UUID tenantId, int pageSize, Consumer<List<CalculatedField>> pageConsumer);

}
