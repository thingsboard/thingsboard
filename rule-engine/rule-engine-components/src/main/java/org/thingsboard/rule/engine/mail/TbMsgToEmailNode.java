// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.mail;

import com.fasterxml.jackson.core.type.TypeReference;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "to email",
        configClazz = TbMsgToEmailNodeConfiguration.class,
        nodeDescription = "Transforms message to email message",
        nodeDetails = "Transforms message to email message. If transformation completed successfully output message type will be set to <code>SEND_EMAIL</code>.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbTransformationNodeToEmailConfig",
        icon = "email",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/transformation/to-email/"
)
public class TbMsgToEmailNode implements TbNode {

    private static final String IMAGES = "images";
    private static final String DYNAMIC = "dynamic";

    public static final String ATTACHMENTS = "attachments";
    public static final String REPORTS = "reports";
    private static final String EMAIL_TIMEZONE = "emailTimezone";

    private static final Pattern dateVarPattern = Pattern.compile("%d\\{([^\\}]*)\\}");

    private TbMsgToEmailNodeConfiguration config;
    private boolean dynamicMailBodyType;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbMsgToEmailNodeConfiguration.class);
        dynamicMailBodyType = DYNAMIC.equals(config.getMailBodyType());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        TbEmail email = convert(msg);
        TbMsg emailMsg = buildEmailMsg(ctx, msg, email);
        ctx.tellNext(emailMsg, TbNodeConnectionType.SUCCESS);
    }

    private TbMsg buildEmailMsg(TbContext ctx, TbMsg msg, TbEmail email) {
        String emailJson = JacksonUtil.toString(email);
        return ctx.transformMsg(msg, TbMsgType.SEND_EMAIL, msg.getOriginator(), msg.getMetaData().copy(), emailJson);
    }

    private TbEmail convert(TbMsg msg) {
        TimeZone tz = null;
        String emailTimezone = msg.getMetaData().getValue(EMAIL_TIMEZONE);
        if (!StringUtils.isEmpty(emailTimezone)) {
            tz = TimeZone.getTimeZone(emailTimezone);
        }
        Date currentDate = new Date();

        TbEmail.TbEmailBuilder builder = TbEmail.builder();
        builder.from(fromTemplate(this.config.getFromTemplate(), msg));
        builder.to(fromTemplate(this.config.getToTemplate(), msg));
        builder.cc(fromTemplate(this.config.getCcTemplate(), msg));
        builder.bcc(fromTemplate(this.config.getBccTemplate(), msg));
        String htmlStr = dynamicMailBodyType ?
                fromTemplate(config.getIsHtmlTemplate(), msg) : config.getMailBodyType();
        builder.html(Boolean.parseBoolean(htmlStr));
        builder.subject(fromTemplateWithDate(this.config.getSubjectTemplate(), msg, currentDate, tz));
        builder.body(fromTemplateWithDate(this.config.getBodyTemplate(), msg, currentDate, tz));
        String attachmentsStr = msg.getMetaData().getValue(ATTACHMENTS);
        if (StringUtils.isNotEmpty(attachmentsStr)) {
            builder.attachments(Arrays.stream(attachmentsStr.split(","))
                    .map(UUID::fromString)
                    .map(BlobEntityId::new).collect(Collectors.toList()));
        }
        String reportsStr = msg.getMetaData().getValue(REPORTS); // TODO: update documentation
        if (StringUtils.isNotEmpty(reportsStr)) {
            builder.reports(Arrays.stream(reportsStr.split(","))
                    .map(id -> new ReportId(UUID.fromString(id)))
                    .toList());
        }
        String imagesStr = msg.getMetaData().getValue(IMAGES);
        if (!StringUtils.isEmpty(imagesStr)) {
            Map<String, String> imgMap = JacksonUtil.fromString(imagesStr, new TypeReference<HashMap<String, String>>() {});
            builder.images(imgMap);
        }
        return builder.build();
    }

    private String fromTemplate(String template, TbMsg msg) {
        return StringUtils.isNotEmpty(template) ? TbNodeUtils.processPattern(template, msg) : null;
    }

    private String fromTemplateWithDate(String template, TbMsg msg, Date currentDate, TimeZone tz) {
        return StringUtils.isNotEmpty(template) ?
                processDatePatterns(TbNodeUtils.processPattern(template, msg), currentDate, tz) : null;
    }

    private String processDatePatterns(String datePattern, Date currentDate, TimeZone tz) {
        String result = datePattern;
        Matcher matcher = dateVarPattern.matcher(datePattern);
        while (matcher.find()) {
            String toReplace = matcher.group(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat(matcher.group(1));
            if (tz != null) {
                dateFormat.setTimeZone(tz);
            }
            String replacement = dateFormat.format(currentDate);
            result = result.replace(toReplace, replacement);
        }
        return result;
    }

}
