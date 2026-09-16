// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sqlts.dictionary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Generated;

import static org.thingsboard.server.dao.model.ModelConstants.KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.KEY_ID_COLUMN;

@Data
@Entity
@Table(name = "key_dictionary")
@IdClass(KeyDictionaryCompositeKey.class)
public final class KeyDictionaryEntry {

    @Id
    @Column(name = KEY_COLUMN)
    private String key;

    @Column(name = KEY_ID_COLUMN, unique = true, columnDefinition = "int", insertable = false, updatable = false)
    private Integer keyId;

}
