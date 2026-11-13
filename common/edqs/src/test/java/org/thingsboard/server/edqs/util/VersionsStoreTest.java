// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edqs.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObjectKey;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionsStoreTest {

    private VersionsStore versionsStore;

    @BeforeEach
    public void setUp() {
        versionsStore = new VersionsStore(60);
    }

    @AfterEach
    public void tearDown() {
        versionsStore.shutdown();
    }

    private EdqsObjectKey newKey() {
        return new AttributeKv.Key(UUID.randomUUID(), AttributeScope.SERVER_SCOPE, 1);
    }

    @Test
    public void givenFreshKey_whenIsNew_thenReturnsTrue() {
        EdqsObjectKey key = newKey();
        assertThat(versionsStore.isNew(key, 5L)).isTrue();
    }

    @Test
    public void givenStoredVersion_whenLowerVersion_thenReturnsFalse() {
        EdqsObjectKey key = newKey();
        assertThat(versionsStore.isNew(key, 5L)).isTrue();
        assertThat(versionsStore.isNew(key, 4L)).isFalse();
    }

    @Test
    public void givenStoredVersion_whenEqualVersion_thenReturnsTrue() {
        EdqsObjectKey key = newKey();
        assertThat(versionsStore.isNew(key, 5L)).isTrue();
        // equal is accepted as new (prevVersion.value <= version)
        assertThat(versionsStore.isNew(key, 5L)).isTrue();
    }

    @Test
    public void givenStoredVersion_whenHigherVersion_thenReturnsTrue() {
        EdqsObjectKey key = newKey();
        assertThat(versionsStore.isNew(key, 5L)).isTrue();
        assertThat(versionsStore.isNew(key, 6L)).isTrue();
    }

    @Test
    public void givenRemovedKey_whenLowerVersion_thenReturnsTrue() {
        EdqsObjectKey key = newKey();
        // simulate a delete carrying a high version
        assertThat(versionsStore.isNew(key, 100L)).isTrue();
        // clear on delete
        versionsStore.remove(key);
        // a subsequent re-create with a lower version must be accepted
        assertThat(versionsStore.isNew(key, 1L)).isTrue();
    }

    @Test
    public void givenNoStoredVersion_whenRemove_thenNoError() {
        EdqsObjectKey key = newKey();
        versionsStore.remove(key);
        assertThat(versionsStore.isNew(key, 1L)).isTrue();
    }

    @Test
    public void givenStaleDeleteWithVersionsResetOnDelete_whenIsNew_thenStoredVersionIsKept() {
        EdqsObjectKey key = newKey();
        // entity re-created at version 5 after an earlier delete
        assertThat(versionsStore.isNew(key, 5L)).isTrue();
        // a re-delivered old delete (lower version) with versionsResetOnDelete=true is stale and must NOT clear
        // the stored version — otherwise a duplicate delete would wipe the re-created entity's tombstone protection
        assertThat(versionsStore.isNew(key, 3L, EdqsEventType.DELETED, true)).isFalse();
        // stored version 5 survived: a stale lower-version update is still rejected
        assertThat(versionsStore.isNew(key, 4L)).isFalse();
    }
}
