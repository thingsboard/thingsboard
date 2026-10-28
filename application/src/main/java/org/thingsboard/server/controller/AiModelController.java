// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.ai.AiModelInfo;
import org.thingsboard.server.common.data.ai.dto.TbChatRequest;
import org.thingsboard.server.common.data.ai.dto.TbChatResponse;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ai.AiChatModelService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.controller.ControllerConstants.AI_MODEL_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Validated
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api/ai/model")
class AiModelController extends BaseController {

    private final AiChatModelService aiChatModelService;

    @ApiOperation(
            value = "Create or update AI model (saveAiModel)",
            notes = "Creates or updates an AI model record.\n\n" +
                    "• **Create:** Omit the `id` to create a new record. The platform assigns a UUID to the new record and returns it in the `id` field of the response.\n\n" +
                    "• **Update:** Include an existing `id` to modify that record. If no matching record exists, the API responds with **404 Not Found**.\n\n" +
                    "Tenant ID for the AI model will be taken from the authenticated user making the request, regardless of any value provided in the request body." +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping
    public AiModel saveAiModel(@RequestBody @Valid AiModel model) throws ThingsboardException {
        var user = getCurrentUser();
        model.setTenantId(user.getTenantId());
        checkEntity(model.getId(), model, Resource.AI_MODEL);
        return tbAiModelService.save(model, user);
    }

    @ApiOperation(
            value = "Get AI model by ID (getAiModelById)",
            notes = "Fetches an AI model record by its `id`." +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{modelUuid}")
    public AiModel getAiModelById(
            @Parameter(
                    description = "ID of the AI model record",
                    required = true,
                    example = "de7900d4-30e2-11f0-9cd2-0242ac120002"
            )
            @PathVariable UUID modelUuid
    ) throws ThingsboardException {
        return checkAiModelId(new AiModelId(modelUuid), Operation.READ);
    }

    @ApiOperation(
            value = "Get AI models (getAiModels)",
            notes = "Returns a page of AI models. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping
    public PageData<AiModel> getAiModels(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = AI_MODEL_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "provider", "modelId"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Filter by the 'customer access enabled' flag; all models are returned when omitted")
            @RequestParam(required = false) Boolean customerAccessEnabled
    ) throws ThingsboardException {
        var user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI_MODEL, Operation.READ);
        var pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return aiModelService.findAiModelsByTenantId(user.getTenantId(), customerAccessEnabled, pageLink);
    }

    @ApiOperation(
            value = "Get AI model infos (getAiModelInfos)",
            notes = "Returns a page of AI model infos: AI models without the configuration and its provider credentials. " +
                    "Customer users only get the models with customer access enabled. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/infos")
    public PageData<AiModelInfo> getAiModelInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = AI_MODEL_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "provider", "modelId"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Filter by the 'customer access enabled' flag; all models are returned when omitted. Ignored for customer users")
            @RequestParam(required = false) Boolean customerAccessEnabled
    ) throws ThingsboardException {
        var user = getCurrentUser();
        accessControlService.checkPermission(user, Resource.AI_MODEL, Operation.READ);
        var pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return aiModelService.findAiModelInfosByTenantId(user.getTenantId(), user.isCustomerUser() ? Boolean.TRUE : customerAccessEnabled, pageLink);
    }

    @ApiOperation(
            value = "Delete AI model by ID (deleteAiModelById)",
            notes = "Deletes the AI model record by its `id`. " +
                    "If a record with the specified `id` exists, the record is deleted and the endpoint returns `true`. " +
                    "If no such record exists, the endpoint returns `false`." +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/{modelUuid}")
    public boolean deleteAiModelById(
            @Parameter(
                    description = "ID of the AI model record",
                    required = true,
                    example = "de7900d4-30e2-11f0-9cd2-0242ac120002"
            )
            @PathVariable UUID modelUuid
    ) throws ThingsboardException {
        var user = getCurrentUser();
        var modelId = new AiModelId(modelUuid);
        accessControlService.checkPermission(user, Resource.AI_MODEL, Operation.DELETE);
        Optional<AiModel> toDelete = aiModelService.findAiModelByTenantIdAndId(user.getTenantId(), modelId);
        if (toDelete.isEmpty()) {
            return false;
        }
        accessControlService.checkPermission(user, Resource.AI_MODEL, Operation.DELETE, modelId, toDelete.get());
        return tbAiModelService.delete(toDelete.get(), user);
    }

    @ApiOperation(
            value = "Send request to AI chat model (sendChatRequest)",
            notes = "Submits a single prompt - made up of an optional system message and a required user message - to the specified AI chat model " +
                    "and returns either the generated answer or an error envelope." +
                    TENANT_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/chat")
    public DeferredResult<TbChatResponse> sendChatRequest(@Valid @RequestBody TbChatRequest tbChatRequest) throws ThingsboardException {
        if (tbChatRequest.chatModelConfig() == null) {
            throw new ThingsboardException("Validation error: chatModelConfig must not be null", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return executeChatRequest(tbChatRequest.chatModelConfig(), tbChatRequest);
    }

    @ApiOperation(
            value = "Send request to a saved AI chat model (sendChatRequestToAiModel)",
            notes = "Submits a single prompt - made up of an optional system message and a required user message - to the AI chat model " +
                    "with the specified `id`, using its saved configuration, and returns either the generated answer or an error envelope. " +
                    "The user must have the 'INVOKE' permission for AI models, and customer users additionally require the model to have customer access enabled. " +
                    "Roles that grant all operations, such as the default 'Customer Administrator' role, include 'INVOKE', " +
                    "so enabling customer access makes the model callable by every customer user with such a role." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/{modelUuid}/chat")
    public DeferredResult<TbChatResponse> sendChatRequestToAiModel(
            @Parameter(
                    description = "ID of the AI model record",
                    required = true,
                    example = "de7900d4-30e2-11f0-9cd2-0242ac120002"
            )
            @PathVariable UUID modelUuid,
            @Valid @RequestBody TbChatRequest tbChatRequest
    ) throws ThingsboardException {
        AiModel model = checkAiModelId(new AiModelId(modelUuid), Operation.INVOKE);
        if (!(model.getConfiguration() instanceof AiChatModelConfig<?> chatModelConfig)) {
            throw new ThingsboardException("AI model with id [" + modelUuid + "] is not a chat model", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return executeChatRequest(chatModelConfig, tbChatRequest);
    }

    private DeferredResult<TbChatResponse> executeChatRequest(AiChatModelConfig<?> chatModelConfig, TbChatRequest tbChatRequest) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        ListenableFuture<TbChatResponse> future = aiChatModelService.sendChatRequestAsync(user.getTenantId(), chatModelConfig, tbChatRequest.toLangChainChatRequest())
                .transform(chatResponse -> (TbChatResponse) new TbChatResponse.Success(chatResponse.aiMessage().text()), directExecutor())
                .catching(Throwable.class, ex -> toFailure(user, ex), directExecutor());

        Integer requestTimeoutSeconds = chatModelConfig.timeoutSeconds();
        return requestTimeoutSeconds != null ? wrapFuture(future, Duration.ofSeconds(requestTimeoutSeconds).toMillis()) : wrapFuture(future);
    }

    private TbChatResponse.Failure toFailure(SecurityUser user, Throwable ex) {
        if (!user.isCustomerUser()) {
            return new TbChatResponse.Failure(ex.getMessage());
        }
        // Provider errors may reveal configuration details that customer users must not see
        log.debug("[{}] Chat request of customer user [{}] failed", user.getTenantId(), user.getId(), ex);
        return new TbChatResponse.Failure("Failed to get a response from the AI model");
    }

}
