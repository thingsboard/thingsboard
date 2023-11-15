/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.resource.TbImageService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.function.Supplier;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

@Slf4j
@RestController
@TbCoreComponent
@RequiredArgsConstructor
public class ImageController extends BaseController {

    private final ImageService imageService;
    private final TbImageService tbImageService;

    private static final String IMAGE_URL = "/api/images/{type}/{key}";
    private static final String SYSTEM_IMAGE = "system";
    private static final String TENANT_IMAGE = "tenant";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping("/api/image")
    public TbResourceInfo uploadImage(@RequestPart MultipartFile file) throws Exception {
        SecurityUser user = getCurrentUser();
        TbResource image = new TbResource();
        image.setTenantId(user.getTenantId());
        accessControlService.checkPermission(user, Resource.TB_RESOURCE, Operation.CREATE, null, image);

        image.setFileName(file.getOriginalFilename());
        image.setTitle(file.getOriginalFilename());
        image.setResourceType(ResourceType.IMAGE);
        ImageDescriptor descriptor = new ImageDescriptor();
        descriptor.setMediaType(file.getContentType());
        image.setDescriptor(JacksonUtil.valueToTree(descriptor));
        image.setData(file.getBytes());
        return tbImageService.save(image, user);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(IMAGE_URL)
    public TbResourceInfo updateImage(@PathVariable String type,
                                      @PathVariable String key,
                                      @RequestPart MultipartFile file) throws Exception {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);

        TbResource image = new TbResource(imageInfo);
        image.setData(file.getBytes());
        descriptor.setMediaType(file.getContentType());
        return tbImageService.save(image, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(IMAGE_URL + "/info")
    public TbResourceInfo updateImageInfo(@PathVariable String type,
                                          @PathVariable String key,
                                          @RequestBody TbResourceInfo newImageInfo) throws ThingsboardException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        imageInfo.setTitle(newImageInfo.getTitle());
        return tbImageService.save(imageInfo, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL, produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadImage(@PathVariable String type,
                                                           @PathVariable String key,
                                                           @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        TenantId tenantId = getTenantId();
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.READ);
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        return downloadIfChanged(etag, descriptor, imageInfo.getFileName(),
                () -> imageService.getImageData(tenantId, imageInfo.getId()));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL + "/preview", produces = "image/png")
    public ResponseEntity<ByteArrayResource> downloadImagePreview(@PathVariable String type,
                                                                  @PathVariable String key,
                                                                  @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        TenantId tenantId = getTenantId();
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.READ);
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        return downloadIfChanged(etag, descriptor.getPreviewDescriptor(), imageInfo.getFileName(),
                () -> imageService.getImagePreview(tenantId, imageInfo.getId()));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(IMAGE_URL + "/info")
    public TbResourceInfo getImageInfo(@PathVariable String type,
                                       @PathVariable String key) throws ThingsboardException {
        return checkImageInfo(type, key, Operation.READ);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/api/images")
    public PageData<TbResourceInfo> getImages(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                              @RequestParam int pageSize,
                                              @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                              @RequestParam int page,
                                              @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                              @RequestParam(required = false) String textSearch,
                                              @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                              @RequestParam(required = false) String sortProperty,
                                              @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                              @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getTenantId();
        if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
            return checkNotNull(imageService.getImagesByTenantId(tenantId, pageLink));
        } else {
            return checkNotNull(imageService.getAllImagesByTenantId(tenantId, pageLink));
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(IMAGE_URL)
    public void deleteImage(@PathVariable String type,
                            @PathVariable String key) throws ThingsboardException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.DELETE);
        tbImageService.delete(imageInfo, getCurrentUser());
    }

    private ResponseEntity<ByteArrayResource> downloadIfChanged(String actualEtag, ImageDescriptor imageDescriptor,
                                                                String fileName, Supplier<byte[]> dataSupplier) {
        if (imageDescriptor.getEtag().equals(actualEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(actualEtag)
                    .build();
        }

        byte[] data = dataSupplier.get();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                .header("x-filename", fileName)
                .contentLength(data.length)
                .header("Content-Type", imageDescriptor.getMediaType())
                .cacheControl(CacheControl.noCache())
                .eTag(imageDescriptor.getEtag())
                .body(new ByteArrayResource(data));
    }

    private TbResourceInfo checkImageInfo(String imageType, String key, Operation operation) throws ThingsboardException {
        TenantId tenantId = getTenantId(imageType);
        TbResourceInfo imageInfo = imageService.getImageInfoByTenantIdAndKey(tenantId, key);
        checkEntity(getCurrentUser(), checkNotNull(imageInfo), operation);
        return imageInfo;
    }

    private TenantId getTenantId(String imageType) throws ThingsboardException {
        TenantId tenantId;
        if (imageType.equals(TENANT_IMAGE)) {
            tenantId = getTenantId();
        } else if (imageType.equals(SYSTEM_IMAGE)) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            throw new IllegalArgumentException("Invalid image URL");
        }
        return tenantId;
    }

}