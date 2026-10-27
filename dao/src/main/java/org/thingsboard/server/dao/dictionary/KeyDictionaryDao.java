// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.dictionary;


import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryEntry;

public interface KeyDictionaryDao {

    Integer getOrSaveKeyId(String strKey);

    String getKey(Integer keyId);

    PageData<KeyDictionaryEntry> findAll(PageLink pageLink);
}
