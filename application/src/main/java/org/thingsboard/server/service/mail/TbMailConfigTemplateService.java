package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface TbMailConfigTemplateService {
    JsonNode findAllMailConfigTemplates() throws IOException;
}
