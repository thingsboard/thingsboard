/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static org.thingsboard.server.common.msg.TbMsgDataType.JSON;

public class RuleVelocityUtils {

    public static VelocityContext createContext(TbMsg msg) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("originator", msg.getOriginator());
        context.put("type", msg.getType());
        context.put("metadata", msg.getMetaData().values());
        if (msg.getDataType() == JSON) {
            Map map = new ObjectMapper().readValue(msg.getData(), Map.class);
            context.put("msg", map);
        } else {
            context.put("msg", msg.getData());
        }
        return context;
    }

    public static String merge(Template template, VelocityContext context) {
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    public static Template create(String source, String templateName) throws ParseException {
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        StringReader reader = new StringReader(source);
        SimpleNode node = runtimeServices.parse(reader, templateName);
        Template template = new Template();
        template.setRuntimeServices(runtimeServices);
        template.setData(node);
        template.initDocument();
        return template;
    }


}
