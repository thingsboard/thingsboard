// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.alarm;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNodeConfiguration;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbAlarmsCountNodeConfiguration extends TbAbstractLatestNodeConfiguration implements NodeConfiguration<TbAlarmsCountNodeConfiguration> {

    private boolean countAlarmsForChildEntities;
    private List<AlarmsCountMapping> alarmsCountMappings;

    @Override
    public TbAlarmsCountNodeConfiguration defaultConfiguration() {
        TbAlarmsCountNodeConfiguration configuration = new TbAlarmsCountNodeConfiguration();

        configuration.setParentEntitiesQuery(new ParentEntitiesGroup());

        configuration.setCountAlarmsForChildEntities(false);

        List<AlarmsCountMapping> alarmsCountMappings = new ArrayList<>();
        AlarmsCountMapping alarmsCountMapping = new AlarmsCountMapping();
        alarmsCountMapping.setTarget("alarmsCount");
        alarmsCountMappings.add(alarmsCountMapping);

        configuration.setAlarmsCountMappings(alarmsCountMappings);

        configuration.setPeriodTimeUnit(TimeUnit.MINUTES);
        configuration.setPeriodValue(5);
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());

        return configuration;
    }
}
