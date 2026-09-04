// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.query;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityDataAdapterTest {

    @Test
    public void testConvertValue() {
        assertThat(EntityDataAdapter.convertValue("500")).isEqualTo("500");
        assertThat(EntityDataAdapter.convertValue("500D")).isEqualTo("500D"); //do not convert to Double !!!
        assertThat(EntityDataAdapter.convertValue("0101010521130565")).isEqualTo("0101010521130565"); //do not convert to Double !!!
        assertThat(EntityDataAdapter.convertValue("89010303310033979663")).isEqualTo("89010303310033979663"); //do not convert to Double !!!
        assertThat(EntityDataAdapter.convertValue("89914009129080723322")).isEqualTo("89914009129080723322");
        assertThat(EntityDataAdapter.convertValue("899140091AAAA29080723322")).isEqualTo("899140091AAAA29080723322");
        assertThat(EntityDataAdapter.convertValue("899140091.29080723322")).isEqualTo("899140091.29080723322");
    }
}
