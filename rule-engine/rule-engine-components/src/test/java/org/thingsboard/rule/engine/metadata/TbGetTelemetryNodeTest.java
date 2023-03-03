/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.common.data.kv.Aggregation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class TbGetTelemetryNodeTest {

    TbGetTelemetryNode node;
    TbGetTelemetryNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;

    @Before
    public void setUp() throws Exception {
        ctx = mock(TbContext.class);
        node = spy(new TbGetTelemetryNode());
        config = new TbGetTelemetryNodeConfiguration();
        config.setFetchMode("ALL");
        ObjectMapper mapper = JacksonUtil.OBJECT_MAPPER;
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        willCallRealMethod().given(node).parseAggregationConfig(any());
    }

    @Test
    public void givenAggregationAsString_whenParseAggregation_thenReturnEnum() {
        //compatibility with old configs without "aggregation" parameter
        assertThat(node.parseAggregationConfig(null), is(Aggregation.NONE));
        assertThat(node.parseAggregationConfig(""), is(Aggregation.NONE));

        //common values
        assertThat(node.parseAggregationConfig("MIN"), is(Aggregation.MIN));
        assertThat(node.parseAggregationConfig("MAX"), is(Aggregation.MAX));
        assertThat(node.parseAggregationConfig("AVG"), is(Aggregation.AVG));
        assertThat(node.parseAggregationConfig("SUM"), is(Aggregation.SUM));
        assertThat(node.parseAggregationConfig("COUNT"), is(Aggregation.COUNT));
        assertThat(node.parseAggregationConfig("NONE"), is(Aggregation.NONE));

        //all possible values in future
        for (Aggregation aggEnum : Aggregation.values()) {
            assertThat(node.parseAggregationConfig(aggEnum.name()), is(aggEnum));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenAggregationWhiteSpace_whenParseAggregation_thenException() {
        node.parseAggregationConfig(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenAggregationIncorrect_whenParseAggregation_thenException() {
        node.parseAggregationConfig("TOP");
    }

}
