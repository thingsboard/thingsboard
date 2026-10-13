// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.TbAiSettings;
import org.thingsboard.server.service.ai.chat.AiChatService;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER;

@Slf4j
@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/ai/chats")
class AiChatController extends BaseController {

    private final AiChatService aiChatService;
    private final TbAiSettings aiSettings;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JsonNode createChat(@RequestBody JsonNode request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiChatService.createChat(request, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PatchMapping("/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateChat(@PathVariable UUID chatId, @RequestBody JsonNode request) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        aiChatService.updateChat(chatId, request, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping
    public JsonNode listChats() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.READ);
        return aiChatService.listChats(user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{chatId}/messages")
    public JsonNode getChatMessages(@PathVariable UUID chatId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.READ);
        return aiChatService.getChatMessages(chatId, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChat(@PathVariable UUID chatId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        aiChatService.deleteChat(chatId, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(
            value = "/{chatId}/messages",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> sendChatMessage(
            @PathVariable UUID chatId,
            @RequestBody JsonNode request,
            @RequestHeader(AUTHORIZATION_HEADER) String tbAccessToken,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage
    ) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return withTraceLogging(chatId, aiChatService.sendChatMessage(chatId, request, tbAccessToken, acceptLanguage, user));
    }

    private Flux<ServerSentEvent<String>> withTraceLogging(UUID chatId, Flux<ServerSentEvent<String>> events) {
        return events.doOnNext(event -> {
            if (log.isTraceEnabled()) {
                log.trace("[{}] Sending SSE event '{}'{}: {}", chatId, event.event(),
                        event.comment() != null ? " (comment: '" + event.comment() + "')" : "", truncate(event.data()));
            }
        });
    }

    private static final String TRUNCATE_MARKER = "... (truncated)";

    private String truncate(String data) {
        return StringUtils.abbreviate(data, TRUNCATE_MARKER, aiSettings.getSseLogMaxDataLength());
    }

}
