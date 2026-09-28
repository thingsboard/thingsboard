// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sqlts;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;

import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqlTimeseriesLatestDaoRowMapperTest {

    @Test
    void byKeyMapperLeavesStrKeyNullAndMapsValuesWithoutCoercion() throws Exception {
        UUID entityId = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("entity_id", UUID.class)).thenReturn(entityId);
        when(rs.getInt("key")).thenReturn(11);
        when(rs.getString("str_v")).thenReturn(null);
        when(rs.getObject("bool_v", Boolean.class)).thenReturn(null);
        when(rs.getObject("long_v", Long.class)).thenReturn(null);
        when(rs.getObject("dbl_v", Double.class)).thenReturn(3.5d);
        when(rs.getString("json_v")).thenReturn(null);
        when(rs.getObject("ts", Long.class)).thenReturn(999L);
        when(rs.getObject("version", Long.class)).thenReturn(2L);

        TsKvLatestEntity entity = SqlTimeseriesLatestDao.TS_KV_LATEST_ROW_MAPPER.mapRow(rs, 0);

        assertThat(entity.getEntityId()).isEqualTo(entityId);
        assertThat(entity.getKey()).isEqualTo(11);
        assertThat(entity.getStrKey()).isNull();
        assertThat(entity.getStrValue()).isNull();
        assertThat(entity.getBooleanValue()).isNull();
        assertThat(entity.getLongValue()).isNull();
        assertThat(entity.getDoubleValue()).isEqualTo(3.5d);
        assertThat(entity.getJsonValue()).isNull();
        assertThat(entity.getTs()).isEqualTo(999L);
        assertThat(entity.getVersion()).isEqualTo(2L);
    }

    @Test
    void withStrKeyMapperPopulatesStrKeyFromJoinedColumn() throws Exception {
        UUID entityId = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("entity_id", UUID.class)).thenReturn(entityId);
        when(rs.getInt("key")).thenReturn(4);
        when(rs.getString("str_v")).thenReturn("v");
        when(rs.getObject("bool_v", Boolean.class)).thenReturn(null);
        when(rs.getObject("long_v", Long.class)).thenReturn(null);
        when(rs.getObject("dbl_v", Double.class)).thenReturn(null);
        when(rs.getString("json_v")).thenReturn(null);
        when(rs.getObject("ts", Long.class)).thenReturn(1L);
        when(rs.getObject("version", Long.class)).thenReturn(null);
        when(rs.getString("str_key")).thenReturn("temperature");

        TsKvLatestEntity entity = SqlTimeseriesLatestDao.TS_KV_LATEST_WITH_STR_KEY_ROW_MAPPER.mapRow(rs, 0);

        assertThat(entity.getStrKey()).isEqualTo("temperature");
        assertThat(entity.getStrValue()).isEqualTo("v");
        assertThat(entity.getVersion()).isNull();
    }
}
