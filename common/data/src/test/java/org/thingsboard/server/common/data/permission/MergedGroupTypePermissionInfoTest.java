// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MergedGroupTypePermissionInfoTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ofEmptyGroups(final boolean hasGenericRead) {
        assertThat(MergedGroupTypePermissionInfo.ofEmptyGroups(hasGenericRead))
                .isEqualTo(new MergedGroupTypePermissionInfo(List.of(), hasGenericRead));
    }

    @Test
    void testConstants() {
        assertThat(MergedGroupTypePermissionInfo.MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_TRUE)
                .isEqualTo(new MergedGroupTypePermissionInfo(List.of(), true));
        assertThat(MergedGroupTypePermissionInfo.MERGED_GROUP_TYPE_PERMISSION_INFO_EMPTY_GROUPS_HAS_GENERIC_READ_FALSE)
                .isEqualTo(new MergedGroupTypePermissionInfo(List.of(), false));
    }

    @Test
    void testImmutableEntityGroupIdsEmpty() {
        List<EntityGroupId> entityGroupIds = new ArrayList<>();
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(new ArrayList<>(), true);
        assertThat(mgtpi.getEntityGroupIds()).isEmpty();
        assertThat(mgtpi.isHasGenericRead()).isTrue();
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().add(new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testImmutableEntityGroupIds() {
        List<EntityGroupId> entityGroupIds = new ArrayList<>();
        entityGroupIds.add(new EntityGroupId(UUID.randomUUID()));
        entityGroupIds.add(new EntityGroupId(UUID.randomUUID()));
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(entityGroupIds, true);
        assertThat(mgtpi.getEntityGroupIds()).containsExactlyElementsOf(entityGroupIds);
        assertThat(mgtpi.getEntityGroupIds()).isNotSameAs(entityGroupIds);
        assertThat(mgtpi.isHasGenericRead()).isTrue();
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().add(new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().set(0, new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().remove(entityGroupIds.size() - 1))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testImmutableEntityGroupIdsOfImmutableList() {
        List<EntityGroupId> immutableEntityGroupIds = List.of(
                new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()));
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(immutableEntityGroupIds, true);
        assertThat(mgtpi.getEntityGroupIds()).containsExactlyElementsOf(immutableEntityGroupIds);
        assertThat(mgtpi.getEntityGroupIds()).isSameAs(immutableEntityGroupIds); // immutables does not convert to another immutables
        assertThat(mgtpi.isHasGenericRead()).isTrue();
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().add(new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().set(0, new EntityGroupId(UUID.randomUUID())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().remove(0))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> mgtpi.getEntityGroupIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testImmutableEntityGroupIdsNullable() {
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(null, false);
        assertThat(mgtpi.getEntityGroupIds()).isNull();
        assertThat(mgtpi.isHasGenericRead()).isFalse();
    }

    @Test
    void testAddIdImmutableWithInitialNullIds() {
        EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(null, true);
        MergedGroupTypePermissionInfo added = mgtpi.addId(id);
        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(List.of(id));
        assertThat(added.isHasGenericRead()).isTrue();
    }

    @Test
    void testAddIdImmutableWithInitialEmptyIds() {
        EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(Collections.emptyList(), true);
        MergedGroupTypePermissionInfo added = mgtpi.addId(id);
        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(List.of(id));
        assertThat(added.isHasGenericRead()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAddIdImmutableWithInitialExistedIds(final boolean hasGenericRead) {
        final List<EntityGroupId> ids = List.of(new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()));
        final EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        final List<EntityGroupId> allIds = new ArrayList<>(ids);
        allIds.add(id);

        final MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(ids, hasGenericRead);
        final MergedGroupTypePermissionInfo added = mgtpi.addId(id);

        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(allIds);
        assertThat(added.isHasGenericRead()).isEqualTo(hasGenericRead);
    }

    @Test
    void testAddIdsImmutableWithInitialEmptyIds() {
        EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(Collections.emptyList(), true);
        MergedGroupTypePermissionInfo added = mgtpi.addIds(List.of(id));
        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(List.of(id));
        assertThat(added.isHasGenericRead()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAddIdsImmutableWithInitialExistedIds(final boolean hasGenericRead) {
        final List<EntityGroupId> ids = List.of(new EntityGroupId(UUID.randomUUID()), new EntityGroupId(UUID.randomUUID()));
        final EntityGroupId id = new EntityGroupId(UUID.randomUUID());
        final List<EntityGroupId> allIds = new ArrayList<>(ids);
        allIds.add(id);

        final MergedGroupTypePermissionInfo mgtpi = new MergedGroupTypePermissionInfo(ids, hasGenericRead);
        final MergedGroupTypePermissionInfo added = mgtpi.addIds(List.of(id));

        assertThat(added).isNotSameAs(mgtpi);
        assertThat(added.getEntityGroupIds()).containsExactlyElementsOf(allIds);
        assertThat(added.isHasGenericRead()).isEqualTo(hasGenericRead);
    }

}
