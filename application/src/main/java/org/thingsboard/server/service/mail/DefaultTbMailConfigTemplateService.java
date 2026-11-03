// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;

@Service
@Slf4j
public class DefaultTbMailConfigTemplateService implements TbMailConfigTemplateService {

    private JsonNode mailConfigTemplates;

    @PostConstruct
    private void postConstruct() throws IOException {
        mailConfigTemplates = JacksonUtil.toJsonNode(new ClassPathResource("/templates/mail_config_templates.json").getInputStream());
    }

    @Override
    public JsonNode findAllMailConfigTemplates() {
        return mailConfigTemplates;
    }
}
