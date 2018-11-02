package org.thingsboard.server.common.transport.adaptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class JsonConverterConfig {

    @Value("${json.type_cast_enabled}")
    public void setIsJsonTypeCastEnabled(boolean jsonTypeCastEnabled) {
        JsonConverter.setTypeCastEnabled(jsonTypeCastEnabled);
        log.info("JSON type cast enabled = {}", jsonTypeCastEnabled);
    }
}
