// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.thingsboard.server.common.data.ImageDescriptor;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbImageDeleteResult;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.util.ThrowingSupplier;
import org.thingsboard.server.dao.resource.ImageCacheKey;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.validator.ResourceDataValidator;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.resource.TbImageService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_IMAGE_SUB_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_INCLUDE_SYSTEM_IMAGES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RESOURCE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.dao.util.ImageUtils.mediaTypeToFileExtension;

@Slf4j
@RestController
@TbCoreComponent
@RequiredArgsConstructor
public class ImageController extends BaseController {

    private final ImageService imageService;
    private final WhiteLabelingService whiteLabelingService;
    private final TbImageService tbImageService;
    private final ResourceDataValidator resourceValidator;

    @Value("${cache.image.systemImagesBrowserTtlInMinutes:0}")
    private int systemImagesBrowserTtlInMinutes;
    @Value("${cache.image.tenantImagesBrowserTtlInMinutes:0}")
    private int tenantImagesBrowserTtlInMinutes;

    private static final String IMAGE_URL = "/api/images/{type}/{key}";
    private static final String SYSTEM_IMAGE = "system";
    private static final String TENANT_IMAGE = "tenant";

    private static final String IMAGE_TYPE_PARAM_DESCRIPTION = "Type of the image: tenant or system";
    private static final String IMAGE_KEY_PARAM_DESCRIPTION = "Image resource key, for example thermostats_dashboard_background.jpeg";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/api/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TbResourceInfo uploadImage(@RequestPart MultipartFile file,
                                      @RequestPart(required = false) String title,
                                      @RequestPart(required = false) String imageSubType) throws Exception {
        SecurityUser user = getCurrentUser();
        TbResource image = new TbResource();
        image.setTenantId(user.getTenantId());
        image.setCustomerId(user.getCustomerId());
        resourceValidator.validateResourceSize(user.getTenantId(), null, file.getSize());

        image.setFileName(file.getOriginalFilename());
        if (StringUtils.isNotEmpty(title)) {
            image.setTitle(title);
        } else {
            image.setTitle(file.getOriginalFilename());
        }

        ResourceSubType subType = ResourceSubType.IMAGE;
        if (StringUtils.isNotEmpty(imageSubType)) {
            subType = ResourceSubType.valueOf(imageSubType);
        }

        image.setResourceType(ResourceType.IMAGE);
        image.setResourceSubType(subType);
        ImageDescriptor descriptor = new ImageDescriptor();
        descriptor.setMediaType(file.getContentType());
        image.setDescriptorValue(descriptor);
        image.setData(file.getBytes());
        image.setPublic(true);
        return tbImageService.save(image, user);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(value = IMAGE_URL, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TbResourceInfo updateImage(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                      @PathVariable String type,
                                      @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                      @PathVariable String key,
                                      @RequestPart MultipartFile file) throws Exception {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        resourceValidator.validateResourceSize(getTenantId(), imageInfo.getId(), file.getSize());

        TbResource image = new TbResource(imageInfo);
        image.setData(file.getBytes());
        image.setFileName(file.getOriginalFilename());
        image.updateDescriptor(ImageDescriptor.class, descriptor -> {
            descriptor.setMediaType(file.getContentType());
            return descriptor;
        });
        return tbImageService.save(image, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(IMAGE_URL + "/info")
    public TbResourceInfo updateImageInfo(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                          @PathVariable String type,
                                          @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                          @PathVariable String key,
                                          @RequestBody TbResourceInfo request) throws ThingsboardException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        TbResourceInfo newImageInfo = new TbResourceInfo(imageInfo);
        newImageInfo.setTitle(request.getTitle());
        return tbImageService.save(newImageInfo, imageInfo, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping(IMAGE_URL + "/public/{isPublic}")
    public TbResourceInfo updateImagePublicStatus(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                  @PathVariable String type,
                                                  @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                  @PathVariable String key,
                                                  @PathVariable boolean isPublic) throws ThingsboardException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        TbResourceInfo newImageInfo = new TbResourceInfo(imageInfo);
        newImageInfo.setPublic(isPublic);
        return tbImageService.save(newImageInfo, imageInfo, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL, produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadImage(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                           @PathVariable String type,
                                                           @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                           @PathVariable String key,
                                                           @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag,
                                                           @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader) throws Exception {
        return downloadIfChanged(type, key, etag, acceptEncodingHeader, false);
    }

    @GetMapping(value = "/api/images/public/{publicResourceKey}", produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadPublicImage(@PathVariable String publicResourceKey,
                                                                 @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag,
                                                                 @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader) throws Exception {
        ImageCacheKey cacheKey = ImageCacheKey.forPublicImage(publicResourceKey);
        return downloadIfChanged(cacheKey, etag, acceptEncodingHeader, () -> imageService.getPublicImageInfoByKey(publicResourceKey));
    }

    @GetMapping(value = "/api/noauth/whiteLabel/loginLogo/{type}/{key}", produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadLoginLogo(HttpServletRequest request,
                                                               @Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                               @PathVariable String type,
                                                               @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                               @PathVariable String key,
                                                               @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag,
                                                               @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader) throws Exception {
        return this.downloadLoginImage(request.getServerName(), type, key, etag, acceptEncodingHeader, false);
    }

    @GetMapping(value = "/api/noauth/whiteLabel/loginFavicon/{type}/{key}", produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadLoginFavicon(HttpServletRequest request,
                                                                  @Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                                  @PathVariable String type,
                                                                  @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                                  @PathVariable String key,
                                                                  @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag,
                                                                  @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader) throws Exception {
        return this.downloadLoginImage(request.getServerName(), type, key, etag, acceptEncodingHeader, true);
    }

    private ResponseEntity<ByteArrayResource> downloadLoginImage(String domainName, String type,
                                                                 String key, String etag, String acceptEncodingHeader, boolean faviconElseLogo) throws Exception {
        var imageKey = whiteLabelingService.getLoginImageKey(domainName, faviconElseLogo);
        if (imageKey != null && imageKey.getResourceKey().equals(key) &&
                ((imageKey.getTenantId().isSysTenantId() && SYSTEM_IMAGE.equals(type)) || (!imageKey.getTenantId().isSysTenantId() && TENANT_IMAGE.equals(type)))) {
            return downloadIfChanged(TenantId.SYS_TENANT_ID, imageKey, etag, acceptEncodingHeader, true);
        } else {
            throw new ThingsboardException("Login image not found", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL + "/export")
    public ResourceExportData exportImage(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                          @PathVariable String type,
                                          @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                          @PathVariable String key) throws Exception {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.READ);
        return imageService.exportImage(imageInfo);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping("/api/image/import")
    public TbResourceInfo importImage(@RequestBody ResourceExportData imageData) throws Exception {
        SecurityUser user = getCurrentUser();
        return tbImageService.importImage(imageData, false, user);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL + "/preview", produces = "image/png")
    public ResponseEntity<ByteArrayResource> downloadImagePreview(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                                  @PathVariable String type,
                                                                  @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                                  @PathVariable String key,
                                                                  @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag,
                                                                  @RequestHeader(name = HttpHeaders.ACCEPT_ENCODING, required = false) String acceptEncodingHeader) throws Exception {
        return downloadIfChanged(type, key, etag, acceptEncodingHeader, true);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(IMAGE_URL + "/info")
    public TbResourceInfo getImageInfo(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                       @PathVariable String type,
                                       @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                       @PathVariable String key) throws ThingsboardException {
        return checkImageInfo(type, key, Operation.READ);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/api/images")
    public PageData<TbResourceInfo> getImages(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                              @RequestParam int pageSize,
                                              @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                              @RequestParam int page,
                                              @Parameter(description = RESOURCE_IMAGE_SUB_TYPE_DESCRIPTION, schema = @Schema(allowableValues = {"IMAGE", "SCADA_SYMBOL"}))
                                              @RequestParam(required = false) String imageSubType,
                                              @Parameter(description = RESOURCE_INCLUDE_SYSTEM_IMAGES_DESCRIPTION)
                                              @RequestParam(required = false) boolean includeSystemImages,
                                              @Parameter(description = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                              @RequestParam(required = false) String textSearch,
                                              @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "resourceType", "tenantId"}))
                                              @RequestParam(required = false) String sortProperty,
                                              @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                              @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getTenantId();
        ResourceSubType subType = ResourceSubType.IMAGE;
        if (StringUtils.isNotEmpty(imageSubType)) {
            subType = ResourceSubType.valueOf(imageSubType);
        }
        if (getCurrentUser().isCustomerUser()) {
            return checkNotNull(imageService.getImagesByCustomerId(tenantId, getCurrentUser().getCustomerId(), subType, pageLink));
        } else if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN || !includeSystemImages) {
            return checkNotNull(imageService.getImagesByTenantId(tenantId, subType, pageLink));
        } else {
            return checkNotNull(imageService.getAllImagesByTenantId(tenantId, subType, pageLink));
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(IMAGE_URL)
    public ResponseEntity<TbImageDeleteResult> deleteImage(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                           @PathVariable String type,
                                                           @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                           @PathVariable String key,
                                                           @RequestParam(name = "force", required = false) boolean force) throws ThingsboardException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.DELETE);
        TbImageDeleteResult result = tbImageService.delete(imageInfo, getCurrentUser(), force);
        return (result.isSuccess() ? ResponseEntity.ok() : ResponseEntity.badRequest()).body(result);
    }

    private ResponseEntity<ByteArrayResource> downloadIfChanged(String type, String key, String etag, String acceptEncodingHeader, boolean preview) throws Exception {
        ImageCacheKey cacheKey = ImageCacheKey.forImage(getTenantId(type), key, preview);
        return downloadIfChanged(getTenantId(), cacheKey, etag, acceptEncodingHeader, false);
    }

    private ResponseEntity<ByteArrayResource> downloadIfChanged(TenantId tenantId, ImageCacheKey cacheKey, String etag,
                                                                String acceptEncodingHeader, boolean skipPermissionCheck) throws Exception {
        return downloadIfChanged(cacheKey, etag, acceptEncodingHeader, () -> checkImageInfo(cacheKey.getTenantId(), cacheKey.getResourceKey(), Operation.READ, skipPermissionCheck));
    }

    private ResponseEntity<ByteArrayResource> downloadIfChanged(ImageCacheKey cacheKey, String etag, String acceptEncodingHeader, ThrowingSupplier<TbResourceInfo> imageInfoSupplier) throws Exception {
        if (StringUtils.isNotEmpty(etag)) {
            etag = StringUtils.remove(etag, '\"'); // etag is wrapped in double quotes due to HTTP specification
            if (etag.equals(tbImageService.getETag(cacheKey))) {
                return response(HttpStatus.NOT_MODIFIED);
            }
        }

        TbResourceInfo imageInfo = checkNotNull(imageInfoSupplier.get());
        String fileName = imageInfo.getFileName();
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        byte[] data;
        if (cacheKey.isPreview()) {
            descriptor = descriptor.getPreviewDescriptor();
            data = imageService.getImagePreview(imageInfo.getTenantId(), imageInfo.getId());
        } else {
            data = imageService.getImageData(imageInfo.getTenantId(), imageInfo.getId());
        }
        tbImageService.putETag(cacheKey, descriptor.getEtag());
        var result = ResponseEntity.ok()
                .header("Content-Type", descriptor.getMediaType())
                .header("Content-Security-Policy", "default-src 'none'")
                .eTag(descriptor.getEtag());
        if (!cacheKey.isPublic()) {
            result
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                    .header("x-filename", fileName);
        }
        if (systemImagesBrowserTtlInMinutes > 0 && imageInfo.getTenantId().isSysTenantId()) {
            result.cacheControl(CacheControl.maxAge(systemImagesBrowserTtlInMinutes, TimeUnit.MINUTES));
        } else if (tenantImagesBrowserTtlInMinutes > 0 && !imageInfo.getTenantId().isSysTenantId()) {
            result.cacheControl(CacheControl.maxAge(tenantImagesBrowserTtlInMinutes, TimeUnit.MINUTES));
        } else {
            result.cacheControl(CacheControl.noCache());
        }
        var responseData = data;
        if (mediaTypeToFileExtension(descriptor.getMediaType()).equals("svg") &&
                StringUtils.isNotEmpty(acceptEncodingHeader) && acceptEncodingHeader.contains("gzip")) {
            result.header(HttpHeaders.CONTENT_ENCODING, "gzip");
            var outputStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                gzipOutputStream.write(data);
                gzipOutputStream.finish();
            }
            responseData = outputStream.toByteArray();
        }
        result.contentLength(responseData.length);
        return result.body(new ByteArrayResource(responseData));
    }

    private TbResourceInfo checkImageInfo(String imageType, String key, Operation operation) throws ThingsboardException {
        TenantId tenantId = getTenantId(imageType);
        return this.checkImageInfo(tenantId, key, operation, false);
    }

    private TbResourceInfo checkImageInfo(TenantId imageTenantId, String key, Operation operation, boolean skipPermissionCheck) throws ThingsboardException {
        TbResourceInfo imageInfo = imageService.getImageInfoByTenantIdAndKey(imageTenantId, key);
        checkNotNull(imageInfo);
        if (!skipPermissionCheck) {
            TenantId userTenantId = getTenantId();
            if (Operation.READ.equals(operation)) {
                if (!(imageTenantId.isSysTenantId() || imageTenantId.equals(userTenantId))) {
                    throw permissionDenied();
                }
            } else {
                if (!imageTenantId.equals(userTenantId)) {
                    throw permissionDenied();
                } else if (getCurrentUser().isCustomerUser() && !getCurrentUser().getCustomerId().equals(imageInfo.getCustomerId())) {
                    throw permissionDenied();
                }
            }
        }
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