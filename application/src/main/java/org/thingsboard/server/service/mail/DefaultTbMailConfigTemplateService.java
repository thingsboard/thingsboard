package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;

@Service
@Slf4j
public class DefaultTbMailConfigTemplateService implements TbMailConfigTemplateService {

    @Override
    public JsonNode findAllMailConfigTemplates() throws IOException {
        return JacksonUtil.toJsonNode(new ClassPathResource("/templates/mail_config_templates.json").getFile());
    }
}
