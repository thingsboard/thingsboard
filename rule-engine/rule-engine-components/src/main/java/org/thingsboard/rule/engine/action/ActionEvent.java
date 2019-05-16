/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

@Slf4j
public class ActionEvent {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static TbMsg onCustomerCreated(Customer customer, RuleNodeId ruleNodeId) {
        try {
            ObjectNode entityNode = mapper.valueToTree(customer);
            return new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, customer.getId(), getActionMetaData(ruleNodeId), mapper.writeValueAsString(entityNode), null, null, 0L);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to process customer action msg: {}", customer.getId(), DataConstants.ENTITY_CREATED, e);
        }
        return null;
    }

    public static TbMsg onDeviceCreated(Device device, RuleNodeId ruleNodeId) {
        try {
            ObjectNode entityNode = mapper.valueToTree(device);
            return new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), getActionMetaData(ruleNodeId), mapper.writeValueAsString(entityNode), null, null, 0L);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to process device action msg: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
        return null;
    }

    public static TbMsg onAssetCreated(Asset asset, RuleNodeId ruleNodeId) {
        try {
            ObjectNode entityNode = mapper.valueToTree(asset);
            return new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, asset.getId(), getActionMetaData(ruleNodeId), mapper.writeValueAsString(entityNode), null, null, 0L);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to process asset action msg: {}", asset.getId(), DataConstants.ENTITY_CREATED, e);
        }
        return null;
    }

    public static TbMsg onAlarmCreated(Alarm alarm, RuleNodeId ruleNodeId) {
        try {
            ObjectNode entityNode = mapper.valueToTree(alarm);
            return new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, alarm.getId(), getActionMetaData(ruleNodeId), mapper.writeValueAsString(entityNode), null, null, 0L);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to process alarm action msg: {}", alarm.getId(), DataConstants.ENTITY_CREATED, e);
        }
        return null;
    }

    private static TbMsgMetaData getActionMetaData(RuleNodeId ruleNodeId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ruleNodeId", ruleNodeId.toString());
        return metaData;
    }

}
