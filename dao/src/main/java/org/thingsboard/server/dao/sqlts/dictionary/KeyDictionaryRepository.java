// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sqlts.dictionary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryCompositeKey;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryEntry;

import java.util.Optional;

public interface KeyDictionaryRepository extends JpaRepository<KeyDictionaryEntry, KeyDictionaryCompositeKey> {

    Optional<KeyDictionaryEntry> findByKeyId(int keyId);

    @Query("SELECT e FROM KeyDictionaryEntry e ORDER BY e.keyId ASC")
    Page<KeyDictionaryEntry> findAll(Pageable pageable);

    @Query(value = "INSERT INTO key_dictionary (key) VALUES (:key) ON CONFLICT (key) DO UPDATE SET key = EXCLUDED.key RETURNING key_id", nativeQuery = true)
    Integer upsertAndGetKeyId(@Param("key") String key);

}
