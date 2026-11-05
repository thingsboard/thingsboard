// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.resource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ResourceOperationRequest;
import org.thingsboard.ai.common.channel.ResourceUploadRequest;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultAiResourceService implements AiResourceService {

    private final TbAiService tbAiService;
    private final TbAiOperations operations;

    @Override
    public JsonNode getConfig(SecurityUser user) {
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.RESOURCE_CONFIG, null), user, tokenProvider), user, false);
    }

    @Override
    public JsonNode upload(MultipartFile file, SecurityUser user) throws ThingsboardException {
        if (file == null || file.isEmpty()) {
            throw new ThingsboardException("The file is empty", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        long maxSize = getConfig(user).path("maxSize").asLong(0);
        if (maxSize > 0 && file.getSize() > maxSize) {
            throw new ThingsboardException("The file is too large: " + file.getOriginalFilename(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        String data;
        try {
            data = base64(file);
        } catch (IOException e) {
            throw new ThingsboardException("Failed to read the file", e, ThingsboardErrorCode.GENERAL);
        }
        var request = new ResourceUploadRequest(file.getOriginalFilename(), file.getContentType(), data);
        return tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.RESOURCE_UPLOAD, request), user, tokenProvider), user, false);
    }

    private static String base64(MultipartFile file) throws IOException {
        var encoded = new ByteArrayOutputStream((int) Math.min(Integer.MAX_VALUE - 8, (file.getSize() + 2) / 3 * 4));
        try (InputStream in = file.getInputStream(); OutputStream encoder = Base64.getEncoder().wrap(encoded)) {
            in.transferTo(encoder);
        }
        return encoded.toString(StandardCharsets.ISO_8859_1);
    }

    @Override
    public AiResourceContent download(UUID resourceId, SecurityUser user) {
        JsonNode content = tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.RESOURCE_GET, new ResourceOperationRequest(resourceId)), user, tokenProvider), user, false);
        return new AiResourceContent(content.path("name").asText(), content.path("mediaType").asText(),
                Base64.getDecoder().decode(content.path("data").asText()));
    }

    @Override
    public void delete(UUID resourceId, SecurityUser user) {
        tbAiService.process(tokenProvider -> operations.execute(
                new TbAiOperation(ChannelProtocol.RESOURCE_DELETE, new ResourceOperationRequest(resourceId)), user, tokenProvider), user, false);
    }

}
