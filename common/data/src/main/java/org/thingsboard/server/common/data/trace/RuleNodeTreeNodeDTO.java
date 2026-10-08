/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.data.trace;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rule node tree node")
public class RuleNodeTreeNodeDTO {
    @Schema(description = "Rule node id (UUID)")
    private String ruleNodeId;

    @Schema(description = "User-entered rule node name, resolved server-side")
    private String ruleNodeName;

    @Schema(description = "System rule node name")
    private String ruleNodeType;

    @Schema(description = "Owning rule chain id (UUID)")
    private String ruleChainId;

    @Schema(description = "Owning rule chain name, resolved server-side")
    private String ruleChainName;

    @Schema(description = "Queue id (UUID)")
    private String queueId;

    @Schema(description = "Queue name, resolved server-side")
    private String queueName;

    @Schema(description = "Label on the edge into this node; null at the root (not captured today)")
    private String relation;

    @Schema(description = "Total time spent in this node across the range (ms)")
    private long totalTime;

    @Schema(description = "Total executions of this node across the range")
    private long totalCount;

    @Schema(description = "Error executions of this node across the range")
    private long errors;

    @Schema(description = "Downstream nodes")
    private List<RuleNodeTreeNodeDTO> children;
}
