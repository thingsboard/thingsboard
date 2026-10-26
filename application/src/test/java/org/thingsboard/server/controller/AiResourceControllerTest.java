// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ResourceOperationRequest;
import org.thingsboard.ai.common.channel.ResourceUploadRequest;
import org.thingsboard.ai.common.channel.SolutionChatRequest;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ai.transport.TbAiTurnContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiResourceControllerTest extends AbstractAiControllerTest {

    private static final byte[] PNG = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Test
    public void shouldForwardTheFileBase64Encoded_whenUploadingAResource() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID resourceId = UUID.randomUUID();
        ObjectNode info = resourceInfo(resourceId, "chart.png", "image/png", PNG.length);
        var request = new ResourceUploadRequest("chart.png", "image/png", Base64.getEncoder().encodeToString(PNG));
        givenOperation(ChannelProtocol.RESOURCE_UPLOAD, request, success(info));

        // WHEN
        JsonNode result = readResponse(upload("chart.png", "image/png", PNG).andExpect(status().isCreated()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(info);
        TbAiTurnContext context = verifyOperation(ChannelProtocol.RESOURCE_UPLOAD, request);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldRejectAFileOverTheUploadLimit_withoutCallingTheAiService() throws Exception {
        // GIVEN
        loginTenantAdmin();

        // WHEN
        upload("huge.png", "image/png", new byte[7 * 1024 * 1024 + 1])
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(equalTo("The file is too large: huge.png")));

        // THEN
        verifyNoAiCalls();
    }

    @Test
    public void shouldServeTheFileWithItsTypeAndName_whenDownloadingAResource() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID resourceId = UUID.randomUUID();
        ObjectNode content = JacksonUtil.newObjectNode()
                .put("name", "chart.png")
                .put("mediaType", "image/png")
                .put("data", Base64.getEncoder().encodeToString(PNG));
        givenOperation(ChannelProtocol.RESOURCE_GET, new ResourceOperationRequest(resourceId), success(content));

        // WHEN
        MvcResult result = doGet("/api/ai/resources/" + resourceId).andExpect(status().isOk()).andReturn();

        // THEN
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(PNG);
        assertThat(result.getResponse().getContentType()).isEqualTo("image/png");
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("attachment; filename=\"chart.png\"");
        assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    public void shouldDeleteAnUnsentResource() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID resourceId = UUID.randomUUID();
        givenOperation(ChannelProtocol.RESOURCE_DELETE, new ResourceOperationRequest(resourceId), success());

        // WHEN
        doDelete("/api/ai/resources/" + resourceId).andExpect(status().isNoContent());

        // THEN
        verifyOperation(ChannelProtocol.RESOURCE_DELETE, new ResourceOperationRequest(resourceId));
    }

    @Test
    public void shouldReturnTheAcceptedFiles_whenGettingTheConfig() throws Exception {
        // GIVEN
        loginTenantAdmin();
        JsonNode limits = JacksonUtil.toJsonNode("""
                {"maxSize": 5242880, "maxPerMessage": 5, "mediaTypes": ["image/png"], "extensions": [".png"]}
                """);
        givenOperation(ChannelProtocol.RESOURCE_CONFIG, null, success(limits));

        // WHEN
        JsonNode result = doGet("/api/ai/resources/config", JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(limits);
    }

    @Test
    public void shouldSendTheAttachedResources_whenSendingASolutionMessage() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        var request = new SolutionChatRequest(solutionId, SolutionStep.DASHBOARDS_CONFIGURATION, "Like this", List.of(resourceId));
        ObjectNode solution = JacksonUtil.newObjectNode().put("id", solutionId.toString());
        givenOperation(ChannelProtocol.SOLUTION_CHAT, request, success(solution));

        // WHEN
        JsonNode result = readResponse(solutionMessage(solutionId, SolutionStep.DASHBOARDS_CONFIGURATION, """
                {"message": "Like this", "resources": ["%s"]}
                """.formatted(resourceId)).andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        verifyOperation(ChannelProtocol.SOLUTION_CHAT, request);
    }

    @Test
    public void shouldRejectASolutionMessage_whenItHasNeitherTextNorFiles() throws Exception {
        // GIVEN
        loginTenantAdmin();

        // WHEN
        solutionMessage(UUID.randomUUID(), SolutionStep.INITIAL_CONFIGURATION, """
                {"message": "  ", "resources": []}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(equalTo("Message cannot be blank")));

        // THEN
        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenCustomerUserUploads() throws Exception {
        // GIVEN
        loginCustomerUser();

        // WHEN
        upload("chart.png", "image/png", PNG)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        // THEN
        verifyNoAiCalls();
    }

    private ResultActions upload(String name, String mediaType, byte[] content) throws Exception {
        var request = MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/ai/resources")
                .file(new MockMultipartFile("file", name, mediaType, content));
        setJwtToken(request);
        return mockMvc.perform(request);
    }

    private ResultActions solutionMessage(UUID solutionId, SolutionStep step, String body) throws Exception {
        var request = post("/api/ai/solution/" + solutionId + "/" + step + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.getBytes(StandardCharsets.UTF_8));
        setJwtToken(request);
        return mockMvc.perform(request);
    }

    private static ObjectNode resourceInfo(UUID id, String name, String mediaType, int size) {
        return JacksonUtil.newObjectNode()
                .put("id", id.toString())
                .put("name", name)
                .put("mediaType", mediaType)
                .put("size", size);
    }

}
