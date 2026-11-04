// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.attributes;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;

import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaAttributeDaoRowMapperTest {

    @Test
    void mapsAllColumnsWithStringValueAndNullsForUnusedValueColumns() throws Exception {
        UUID entityId = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("entity_id", UUID.class)).thenReturn(entityId);
        when(rs.getInt("attribute_type")).thenReturn(2);
        when(rs.getInt("attribute_key")).thenReturn(7);
        when(rs.getObject("bool_v", Boolean.class)).thenReturn(null);
        when(rs.getString("str_v")).thenReturn("hello");
        when(rs.getObject("long_v", Long.class)).thenReturn(null);
        when(rs.getObject("dbl_v", Double.class)).thenReturn(null);
        when(rs.getString("json_v")).thenReturn(null);
        when(rs.getObject("last_update_ts", Long.class)).thenReturn(123L);
        when(rs.getObject("version", Long.class)).thenReturn(5L);

        AttributeKvEntity entity = JpaAttributeDao.ATTRIBUTE_KV_ROW_MAPPER.mapRow(rs, 0);

        assertThat(entity.getId().getEntityId()).isEqualTo(entityId);
        assertThat(entity.getId().getAttributeType()).isEqualTo(2);
        assertThat(entity.getId().getAttributeKey()).isEqualTo(7);
        assertThat(entity.getStrValue()).isEqualTo("hello");
        assertThat(entity.getBooleanValue()).isNull();
        assertThat(entity.getLongValue()).isNull();
        assertThat(entity.getDoubleValue()).isNull();
        assertThat(entity.getJsonValue()).isNull();
        assertThat(entity.getLastUpdateTs()).isEqualTo(123L);
        assertThat(entity.getVersion()).isEqualTo(5L);
    }

    @Test
    void mapsLongValueWithoutCoercingOtherNullNumericsToZero() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("entity_id", UUID.class)).thenReturn(UUID.randomUUID());
        when(rs.getInt("attribute_type")).thenReturn(1);
        when(rs.getInt("attribute_key")).thenReturn(3);
        when(rs.getObject("bool_v", Boolean.class)).thenReturn(null);
        when(rs.getString("str_v")).thenReturn(null);
        when(rs.getObject("long_v", Long.class)).thenReturn(42L);
        when(rs.getObject("dbl_v", Double.class)).thenReturn(null);
        when(rs.getString("json_v")).thenReturn(null);
        when(rs.getObject("last_update_ts", Long.class)).thenReturn(10L);
        when(rs.getObject("version", Long.class)).thenReturn(null);

        AttributeKvEntity entity = JpaAttributeDao.ATTRIBUTE_KV_ROW_MAPPER.mapRow(rs, 0);

        assertThat(entity.getLongValue()).isEqualTo(42L);
        assertThat(entity.getDoubleValue()).isNull();
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void withStrKeyMapperPopulatesStrKeyFromJoinedColumn() throws Exception {
        UUID entityId = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("entity_id", UUID.class)).thenReturn(entityId);
        when(rs.getInt("attribute_type")).thenReturn(2);
        when(rs.getInt("attribute_key")).thenReturn(7);
        when(rs.getObject("bool_v", Boolean.class)).thenReturn(null);
        when(rs.getString("str_v")).thenReturn("hello");
        when(rs.getObject("long_v", Long.class)).thenReturn(null);
        when(rs.getObject("dbl_v", Double.class)).thenReturn(null);
        when(rs.getString("json_v")).thenReturn(null);
        when(rs.getObject("last_update_ts", Long.class)).thenReturn(123L);
        when(rs.getObject("version", Long.class)).thenReturn(5L);
        when(rs.getString("str_key")).thenReturn("temperature");

        AttributeKvEntity entity = JpaAttributeDao.ATTRIBUTE_KV_WITH_STR_KEY_ROW_MAPPER.mapRow(rs, 0);

        assertThat(entity.getStrKey()).isEqualTo("temperature");
        assertThat(entity.getStrValue()).isEqualTo("hello");
        assertThat(entity.getLastUpdateTs()).isEqualTo(123L);
        assertThat(entity.getVersion()).isEqualTo(5L);
    }

    @Test
    void withStrKeyMapperLeavesStrKeyNullForOrphanRowWithoutDictionaryEntry() throws Exception {
        UUID entityId = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("entity_id", UUID.class)).thenReturn(entityId);
        when(rs.getInt("attribute_type")).thenReturn(1);
        when(rs.getInt("attribute_key")).thenReturn(3);
        when(rs.getObject("bool_v", Boolean.class)).thenReturn(null);
        when(rs.getString("str_v")).thenReturn(null);
        when(rs.getObject("long_v", Long.class)).thenReturn(42L);
        when(rs.getObject("dbl_v", Double.class)).thenReturn(null);
        when(rs.getString("json_v")).thenReturn(null);
        when(rs.getObject("last_update_ts", Long.class)).thenReturn(10L);
        when(rs.getObject("version", Long.class)).thenReturn(null);
        // The LEFT JOIN on key_dictionary yields a null str_key when the attribute_key has no dictionary entry
        when(rs.getString("str_key")).thenReturn(null);

        AttributeKvEntity entity = JpaAttributeDao.ATTRIBUTE_KV_WITH_STR_KEY_ROW_MAPPER.mapRow(rs, 0);

        assertThat(entity.getStrKey()).isNull();
        assertThat(entity.getLongValue()).isEqualTo(42L);
        assertThat(entity.getVersion()).isNull();
    }
}
