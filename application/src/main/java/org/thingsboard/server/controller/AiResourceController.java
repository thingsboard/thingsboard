// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.resource.AiResourceService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api/ai/resources")
class AiResourceController extends BaseController {

    private final AiResourceService aiResourceService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/config")
    public JsonNode getConfig() throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.READ);
        return aiResourceService.getConfig(user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public JsonNode uploadResource(@RequestPart("file") MultipartFile file) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        return aiResourceService.upload(file, user);
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{resourceId}")
    public ResponseEntity<byte[]> downloadResource(@PathVariable UUID resourceId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.READ);
        AiResourceService.AiResourceContent content = aiResourceService.download(resourceId, user);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition(content.name()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(content.data());
    }

    private static String disposition(String name) {
        ContentDisposition.Builder builder = ContentDisposition.attachment();
        boolean ascii = StandardCharsets.US_ASCII.newEncoder().canEncode(name);
        return (ascii ? builder.filename(name) : builder.filename(name, StandardCharsets.UTF_8)).build().toString();
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@PathVariable UUID resourceId) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI, Operation.WRITE);
        aiResourceService.delete(resourceId, user);
    }

}
