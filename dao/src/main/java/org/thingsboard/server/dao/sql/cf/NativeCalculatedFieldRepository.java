// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.cf;

import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.page.PageData;

public interface NativeCalculatedFieldRepository {

    PageData<CalculatedField> findCalculatedFields(Pageable pageable);

    PageData<CalculatedFieldLink> findCalculatedFieldLinks(Pageable pageable);

}
