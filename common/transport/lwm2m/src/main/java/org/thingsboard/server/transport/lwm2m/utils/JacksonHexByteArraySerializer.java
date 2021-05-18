package org.thingsboard.server.transport.lwm2m.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.eclipse.leshan.core.util.Hex;

import java.io.IOException;

public class JacksonHexByteArraySerializer extends JsonSerializer<byte[]> {
    @Override
    public void serialize(byte[] bytes, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        String str = Hex.encodeHexString(bytes);
        jsonGenerator.writeString(str);
    }
}
