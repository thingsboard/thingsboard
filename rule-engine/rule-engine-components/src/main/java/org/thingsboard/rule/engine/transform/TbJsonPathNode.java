// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.transform;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "json path",
        configClazz = TbJsonPathNodeConfiguration.class,
        nodeDescription = "Transforms incoming message body using JSONPath expression.",
        nodeDetails = "JSONPath expression specifies a path to an element or a set of elements in a JSON structure.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        icon = "functions",
        configDirective = "tbTransformationNodeJsonPathConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/transformation/json-path/"
)
public class TbJsonPathNode implements TbNode {

    private Configuration configurationJsonPath;
    private JsonPath jsonPath;
    private String jsonPathValue;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbJsonPathNodeConfiguration.class);
        jsonPathValue = config.getJsonPath();
        if (!TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH.equals(jsonPathValue)) {
            configurationJsonPath = Configuration.builder()
                    .jsonProvider(new JacksonJsonNodeJsonProvider())
                    .build();
            jsonPath = JsonPath.compile(config.getJsonPath());
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (!TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH.equals(jsonPathValue)) {
            try {
                Object jsonPathData = jsonPath.read(msg.getData(), configurationJsonPath);
                ctx.tellSuccess(msg.transform()
                        .data(JacksonUtil.toString(jsonPathData))
                        .build());
            } catch (PathNotFoundException e) {
                ctx.tellFailure(msg, e);
            }
        } else {
            ctx.tellSuccess(msg);
        }
    }

}
