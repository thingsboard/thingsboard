package org.thingsboard.rule.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;

/**
 * Created by ashvayka on 19.01.18.
 */
public class TbNodeUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T convert(TbNodeConfiguration configuration, Class<T> clazz) throws TbNodeException {
        try {
            return mapper.treeToValue(configuration.getData(), clazz);
        } catch (JsonProcessingException e) {
            throw new TbNodeException(e);
        }
    }

}
