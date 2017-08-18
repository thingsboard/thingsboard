/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.core.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.parser.ParseException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.extensions.api.component.Processor;
import org.thingsboard.server.extensions.api.rules.*;
import org.thingsboard.server.extensions.core.utils.VelocityUtils;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Processor(name = "(Deprecated) Alarm Deduplication Processor", descriptor = "AlarmDeduplicationProcessorDescriptor.json",
        configuration = AlarmDeduplicationProcessorConfiguration.class)
@Slf4j
public class AlarmDeduplicationProcessor extends SimpleRuleLifecycleComponent
        implements RuleProcessor<AlarmDeduplicationProcessorConfiguration> {

    public static final String IS_NEW_ALARM = "isNewAlarm";
    private ObjectMapper mapper = new ObjectMapper();
    private AlarmDeduplicationProcessorConfiguration configuration;
    private Template alarmIdTemplate;
    private Template alarmBodyTemplate;

    @Override
    public void init(AlarmDeduplicationProcessorConfiguration configuration) {
        this.configuration = configuration;
        try {
            this.alarmIdTemplate = VelocityUtils.create(configuration.getAlarmIdTemplate(), "Alarm Id Template");
            this.alarmBodyTemplate = VelocityUtils.create(configuration.getAlarmBodyTemplate(), "Alarm Body Template");
        } catch (ParseException e) {
            log.error("Failed to create templates based on provided configuration!", e);
            throw new RuntimeException("Failed to create templates based on provided configuration!", e);
        }
    }

    @Override
    public RuleProcessingMetaData process(RuleContext ctx, ToDeviceActorMsg msg) throws RuleException {
        RuleProcessingMetaData md = new RuleProcessingMetaData();
        VelocityContext context = VelocityUtils.createContext(ctx.getDeviceMetaData(), msg.getPayload());
        String alarmId = VelocityUtils.merge(alarmIdTemplate, context);
        String alarmBody = VelocityUtils.merge(alarmBodyTemplate, context);
        Optional<Event> existingEvent = ctx.findEvent(DataConstants.ALARM, alarmId);
        if (!existingEvent.isPresent()) {
            Event event = new Event();
            event.setType(DataConstants.ALARM);
            event.setUid(alarmId);
            event.setBody(mapper.createObjectNode().put("body", alarmBody));
            Optional<Event> savedEvent = ctx.saveIfNotExists(event);
            if (savedEvent.isPresent()) {
                log.info("New Alarm detected: '{}'", alarmId);
                md.put(IS_NEW_ALARM, Boolean.TRUE);
                md.put("alarmId", alarmId);
                md.put("alarmBody", alarmBody);
                for (Object key : context.getKeys()) {
                    md.put(key.toString(), context.get(key.toString()));
                }
            }
        }
        return md;
    }
}
