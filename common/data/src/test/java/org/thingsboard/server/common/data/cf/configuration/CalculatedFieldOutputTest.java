/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CalculatedFieldOutputTest {

    @Test
    public void testHasContextOnlyChanges_whenTypeChanged_shouldReturnTrue() {
        TimeSeriesOutput output = new TimeSeriesOutput();
        AttributesOutput newOutput = new AttributesOutput();

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenNameChanged_shouldReturnTrue() {
        TimeSeriesOutput output = new TimeSeriesOutput();
        TimeSeriesOutput newOutput = new TimeSeriesOutput();
        newOutput.setName("new");

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenScopeChanged_shouldReturnTrue() {
        AttributesOutput output = new AttributesOutput();
        output.setScope(AttributeScope.SHARED_SCOPE);
        AttributesOutput newOutput = new AttributesOutput();
        newOutput.setScope(AttributeScope.SERVER_SCOPE);

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenDecimalsByDefaultChanged_shouldReturnTrue() {
        AttributesOutput output = new AttributesOutput();
        AttributesOutput newOutput = new AttributesOutput();
        newOutput.setDecimalsByDefault(2);

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenStrategyHasContextOnlyChanges_shouldReturnTrue() {
        AttributesOutputStrategy outputStrategy = mock(AttributesRuleChainOutputStrategy.class);
        given(outputStrategy.hasContextOnlyChanges(any())).willReturn(true);

        AttributesOutput output = new AttributesOutput();
        output.setStrategy(outputStrategy);
        AttributesOutput newOutput = new AttributesOutput();

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenStrategyDoesNotHaveContextOnlyChanges_shouldReturnTrue() {
        AttributesOutputStrategy outputStrategy = mock(AttributesRuleChainOutputStrategy.class);
        given(outputStrategy.hasContextOnlyChanges(any())).willReturn(false);

        AttributesOutput output = new AttributesOutput();
        output.setStrategy(outputStrategy);
        AttributesOutput newOutput = new AttributesOutput();

        assertThat(output.hasContextOnlyChanges(newOutput)).isFalse();
    }

    /* <strategy>.hasContextOnlyChanges() tests*/

    @Test
    public void testAttributesImmediateOutputStrategyHasContextOnlyChanges_whenTypeChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, true, false, true, true);
        AttributesRuleChainOutputStrategy newStrategy = new AttributesRuleChainOutputStrategy();

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesImmediateOutputStrategyHasContextOnlyChanges_whenSaveAttributesChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, true, false, true, true);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesImmediateOutputStrategyHasContextOnlyChanges_whenSendWsUpdateChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, true, true, false, true);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesImmediateOutputStrategyHasContextOnlyChanges_whenProcessCfsChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, true, true, false, false);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, false, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesRuleChainOutputStrategyHasContextOnlyChanges_whenTypeChanged_shouldReturnTrue() {
        AttributesRuleChainOutputStrategy strategy = new AttributesRuleChainOutputStrategy();
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, false, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasContextOnlyChanges_whenTypeChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, false, true, true, true);
        TimeSeriesRuleChainOutputStrategy newStrategy = new TimeSeriesRuleChainOutputStrategy();

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasContextOnlyChanges_whenSaveLatestChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, false, false, true, true);
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(0, false, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasContextOnlyChanges_whenSendWsUpdateChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, true, true, false, true);
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasContextOnlyChanges_whenProcessCfsChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, false);
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesRuleChainOutputStrategyHasContextOnlyChanges_whenProcessCfsChanged_shouldReturnTrue() {
        TimeSeriesRuleChainOutputStrategy strategy = new TimeSeriesRuleChainOutputStrategy();
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, false);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    /* <strategy>.hasRefreshContextOnlyChanges() tests*/

    @Test
    public void testAttributesImmediateOutputStrategyHasRefreshContextOnlyChanges_whenUpdateAttrOnValueChangedChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, false, true, false, true);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, false, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isFalse();
        assertThat(strategy.hasRefreshContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesImmediateOutputStrategyHasRefreshContextOnlyChanges_whenSendAttrUpdatedNotificationChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(false, true, true, false, true);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, false, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isFalse();
        assertThat(strategy.hasRefreshContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasRefreshContextOnlyChanges_whenTtlChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, true);
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(300, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isFalse();
        assertThat(strategy.hasRefreshContextOnlyChanges(newStrategy)).isTrue();
    }

}
