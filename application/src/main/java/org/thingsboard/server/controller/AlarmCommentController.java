/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmCommentService;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_COMMENT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmCommentController extends BaseController {

    public static final String ALARM_ID = "alarmId";
    public static final String ALARM_COMMENT_ID = "commentId";

    private final TbAlarmCommentService tbAlarmCommentService;

    @ApiOperation(value = "Create or update Alarm Comment ",
            notes = "Creates or Updates the Alarm Comment. " +
                    "When creating comment, platform generates Alarm Comment Id as " + UUID_WIKI_LINK +
                    "The newly created Alarm Comment id will be present in the response. Specify existing Alarm Comment id to update the alarm. " +
                    "Referencing non-existing Alarm Comment Id will cause 'Not Found' error. " +
                    "\n\n To create new Alarm comment entity it is enough to specify 'comment' json element with 'text' node, for example: {\"comment\": { \"text\": \"my comment\"}}. " +
                    "\n\n If comment type is not specified the default value 'OTHER' will be saved. If 'alarmId' or 'userId' specified in body it will be ignored." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/alarm/{alarmId}/comment")
    public AlarmComment saveAlarmComment(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION)
                                         @PathVariable(ALARM_ID) String strAlarmId, @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the comment.") @RequestBody AlarmComment alarmComment) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmInfoId(alarmId, Operation.WRITE);
        alarmComment.setAlarmId(alarmId);
        return tbAlarmCommentService.saveAlarmComment(alarm, alarmComment, getCurrentUser());
    }

    @ApiOperation(value = "Delete Alarm comment (deleteAlarmComment)",
            notes = "Deletes the Alarm comment. Referencing non-existing Alarm comment Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/alarm/{alarmId}/comment/{commentId}")
    public void deleteAlarmComment(@Parameter(description = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId, @Parameter(description = ALARM_COMMENT_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_COMMENT_ID) String strCommentId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);

        AlarmCommentId alarmCommentId = new AlarmCommentId(toUUID(strCommentId));
        AlarmComment alarmComment = checkAlarmCommentId(alarmCommentId, alarmId);
        tbAlarmCommentService.deleteAlarmComment(alarm, alarmComment, getCurrentUser());
    }

    @ApiOperation(value = "Get Alarm comments (getAlarmComments)",
            notes = "Returns a page of alarm comments for specified alarm. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/alarm/{alarmId}/comment")
    public PageData<AlarmCommentInfo> getAlarmComments(
            @Parameter(description = ALARM_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ALARM_ID) String strAlarmId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "id"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, null, sortProperty, sortOrder);
        return checkNotNull(alarmCommentService.findAlarmComments(alarm.getTenantId(), alarmId, pageLink));
    }

}
