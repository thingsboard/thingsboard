// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.filter;

import com.google.gson.Gson;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Map;

@RuleNode(
        type = ComponentType.FILTER,
        name = "check fields presence",
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        configClazz = TbCheckMessageNodeConfiguration.class,
        nodeDescription = "Checks the presence of the specified fields in the message and/or metadata.",
        nodeDetails = "By default, the rule node checks that all specified fields are present. " +
                "Uncheck the 'Check that all selected fields are present' if the presence of at least one field is sufficient.<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>",
        configDirective = "tbFilterNodeCheckMessageConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/filter/check-fields-presence/"
)
public class TbCheckMessageNode implements TbNode {

    private static final Gson gson = new Gson();

    private TbCheckMessageNodeConfiguration config;
    private List<String> messageNamesList;
    private List<String> metadataNamesList;

    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckMessageNodeConfiguration.class);
        messageNamesList = config.getMessageNames();
        metadataNamesList = config.getMetadataNames();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            String relationType = config.isCheckAllKeys() ?
                    allKeysData(msg) && allKeysMetadata(msg) ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE :
                    atLeastOneData(msg) || atLeastOneMetadata(msg) ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE;
            ctx.tellNext(msg, relationType);
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    private boolean allKeysData(TbMsg msg) {
        if (!messageNamesList.isEmpty()) {
            Map<String, String> dataMap = dataToMap(msg);
            return processAllKeys(messageNamesList, dataMap);
        }
        return true;
    }

    private boolean allKeysMetadata(TbMsg msg) {
        if (!metadataNamesList.isEmpty()) {
            Map<String, String> metadataMap = metadataToMap(msg);
            return processAllKeys(metadataNamesList, metadataMap);
        }
        return true;
    }

    private boolean atLeastOneData(TbMsg msg) {
        if (!messageNamesList.isEmpty()) {
            Map<String, String> dataMap = dataToMap(msg);
            return processAtLeastOne(messageNamesList, dataMap);
        }
        return false;
    }

    private boolean atLeastOneMetadata(TbMsg msg) {
        if (!metadataNamesList.isEmpty()) {
            Map<String, String> metadataMap = metadataToMap(msg);
            return processAtLeastOne(metadataNamesList, metadataMap);
        }
        return false;
    }

    private boolean processAllKeys(List<String> data, Map<String, String> map) {
        for (String field : data) {
            if (!map.containsKey(field)) {
                return false;
            }
        }
        return true;
    }

    private boolean processAtLeastOne(List<String> data, Map<String, String> map) {
        for (String field : data) {
            if (map.containsKey(field)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> metadataToMap(TbMsg msg) {
        return msg.getMetaData().getData();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> dataToMap(TbMsg msg) {
        return (Map<String, String>) gson.fromJson(msg.getData(), Map.class);
    }

}
