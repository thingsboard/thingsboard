// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.alarm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbAlarmsCountNodeV2Configuration implements NodeConfiguration<TbAlarmsCountNodeV2Configuration> {
    private List<AlarmsCountMapping> alarmsCountMappings;
    private boolean countAlarmsForPropagationEntities;
    private List<EntityType> propagationEntityTypes;
    private String outMsgType;

    @Override
    public TbAlarmsCountNodeV2Configuration defaultConfiguration() {
        TbAlarmsCountNodeV2Configuration configuration = new TbAlarmsCountNodeV2Configuration();
        List<AlarmsCountMapping> alarmsCountMappings = new ArrayList<>();
        AlarmsCountMapping alarmsCountMapping = new AlarmsCountMapping();
        alarmsCountMapping.setTarget("alarmsCount");
        alarmsCountMappings.add(alarmsCountMapping);

        configuration.setCountAlarmsForPropagationEntities(true);
        configuration.setPropagationEntityTypes(Collections.emptyList());
        configuration.setAlarmsCountMappings(alarmsCountMappings);
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());
        return configuration;
    }
}
