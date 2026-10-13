// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class MailTemplates {

    public static final String TEST = "test";
    public static final String ACTIVATION = "activation";
    public static final String ACCOUNT_ACTIVATED = "accountActivated";
    public static final String ACCOUNT_LOCKOUT = "accountLockout";
    public static final String API_USAGE_STATE_ENABLED = "apiUsageStateEnabled";
    public static final String API_USAGE_STATE_WARNING = "apiUsageStateWarning";
    public static final String API_USAGE_STATE_DISABLED = "apiUsageStateDisabled";
    public static final String RESET_PASSWORD = "resetPassword"; //NOSONAR, used as constant defining key for mail template
    public static final String PASSWORD_WAS_RESET = "passwordWasReset"; //NOSONAR, used as constant defining key for mail template
    public static final String TWO_FA_VERIFICATION = "twoFaVerification";

    private static final String SUBJECT = "subject";
    private static final String BODY = "body";

    public static String subject(JsonNode config, String template) {
        JsonNode templateNode = getTemplate(config, template);
        if (templateNode.has(SUBJECT)) {
            return templateNode.get(SUBJECT).asText();
        } else {
            throw new IncorrectParameterException("Template '"+template+"' doesn't have subject field.");
        }
    }

    public static String body(JsonNode config, String template, Map<String, Object> model) throws IOException, TemplateException {
        JsonNode templateNode = getTemplate(config, template);
        if (templateNode.has(BODY)) {
            String bodyTemplate = templateNode.get(BODY).asText();
            Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
            configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
            Template freeMakerTemplate = new Template(template, new StringReader(bodyTemplate),
                    configuration);
            StringWriter out = new StringWriter();
            freeMakerTemplate.process(model, out);
            return out.toString();
        } else {
            throw new IncorrectParameterException("Template '"+template+"' doesn't have body field.");
        }
    }

    public static JsonNode getTemplate(JsonNode config, String template) {
        JsonNode templateNode = config.get(template);
        if (templateNode == null) {
            throw new IncorrectParameterException("Can't find template with name '"+template+"'.");
        }
        return templateNode;
    }

}
