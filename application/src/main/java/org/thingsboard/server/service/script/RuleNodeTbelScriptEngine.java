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
package org.thingsboard.server.service.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

public class RuleNodeTbelScriptEngine extends RuleNodeScriptEngine<TbelInvokeService, Object> {

    public RuleNodeTbelScriptEngine(TenantId tenantId, TbelInvokeService scriptInvokeService, String script, String... argNames) {
        super(tenantId, scriptInvokeService, script, argNames);
    }

    @Override
    protected Object[] prepareArgs(TbMsg msg) {
        Object[] args = new Object[3];
        if (msg.getData() != null) {
            args[0] = JacksonUtil.fromString(msg.getData(), Object.class);
        } else {
            args[0] = new HashMap<>();
        }
        args[1] = new HashMap<>(msg.getMetaData().getData());
        args[2] = msg.getType();
        return args;
    }

    @Override
    protected List<TbMsg> executeUpdateTransform(TbMsg msg, Object result) {
        if (result instanceof Map msgData) {
            return Collections.singletonList(unbindMsg(msgData, msg));
        } else if (result instanceof Collection resultCollection) {
            List<TbMsg> res = new ArrayList<>(resultCollection.size());
            for (Object resObject : resultCollection) {
                if (resObject instanceof Map msgData) {
                    res.add(unbindMsg(msgData, msg));
                } else {
                    throw wrongResultType(resObject);
                }
            }
            return res;
        }
        throw wrongResultType(result);
    }

    @Override
    protected TbMsg executeGenerateTransform(TbMsg prevMsg, Object result) {
        if (result instanceof Map msgData) {
            return unbindMsg(msgData, prevMsg);
        }
        throw wrongResultType(result);
    }

    @Override
    protected boolean executeFilterTransform(Object result) {
        if (result instanceof Boolean b) {
            return b;
        }
        throw wrongResultType(result);
    }

    @Override
    protected Set<String> executeSwitchTransform(Object result) {
        if (result instanceof String str) {
            return Collections.singleton(str);
        }
        if (result instanceof Collection<?> resultCollection) {
            Set<String> res = new HashSet<>(resultCollection.size());
            for (Object resObject : resultCollection) {
                if (resObject instanceof String str) {
                    res.add(str);
                } else {
                    throw wrongResultType(resObject);
                }
            }
            return res;
        }
        throw wrongResultType(result);
    }

    @Override
    public ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg) {
        return Futures.transform(executeScriptAsync(msg), JacksonUtil::valueToTree, directExecutor());
    }

    @Override
    protected Object convertResult(Object result) {
        return result;
    }

    @Override
    protected String executeToStringTransform(Object result) {
        return result instanceof String str ? str : JacksonUtil.toString(result);
    }

    private static TbMsg unbindMsg(Map msgData, TbMsg msg) {
        String data = null;
        Map<String, String> metadata = null;
        String messageType = null;
        if (msgData.containsKey(RuleNodeScriptFactory.MSG)) {
            data = JacksonUtil.toString(msgData.get(RuleNodeScriptFactory.MSG));
        }
        if (msgData.containsKey(RuleNodeScriptFactory.METADATA)) {
            Object msgMetadataObj = msgData.get(RuleNodeScriptFactory.METADATA);
            if (msgMetadataObj instanceof Map<?, ?> msgMetadataObjAsMap) {
                metadata = msgMetadataObjAsMap.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
            } else {
                metadata = JacksonUtil.convertValue(msgMetadataObj, new TypeReference<>() {});
            }
        }
        if (msgData.containsKey(RuleNodeScriptFactory.MSG_TYPE)) {
            messageType = msgData.get(RuleNodeScriptFactory.MSG_TYPE).toString();
        }
        String newData = data != null ? data : msg.getData();
        TbMsgMetaData newMetadata = metadata != null ? new TbMsgMetaData(metadata) : msg.getMetaData().copy();
        String newMessageType = StringUtils.isNotEmpty(messageType) ? messageType : msg.getType();
        return msg.transform()
                .type(newMessageType)
                .metaData(newMetadata)
                .data(newData)
                .build();
    }

    private TbScriptException wrongResultType(Object result) {
        String className = toClassName(result);
        return new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, null, new ClassCastException("Wrong result type: " + className));
    }

    private static String toClassName(Object result) {
        return result != null ? result.getClass().getSimpleName() : "null";
    }

}
