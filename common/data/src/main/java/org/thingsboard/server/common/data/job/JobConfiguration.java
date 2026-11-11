// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.thingsboard.server.common.data.job.task.TaskResult;

import java.io.Serializable;
import java.util.List;

@Schema(
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DUMMY", schema = DummyJobConfiguration.class),
                @DiscriminatorMapping(value = "CF_REPROCESSING", schema = CfReprocessingJobConfiguration.class),
                @DiscriminatorMapping(value = "REPORT", schema = ReportJobConfiguration.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(name = "CF_REPROCESSING", value = CfReprocessingJobConfiguration.class),
        @Type(name = "REPORT", value = ReportJobConfiguration.class),
        @Type(name = "DUMMY", value = DummyJobConfiguration.class),
})
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class JobConfiguration implements Serializable {

    @NotBlank
    private String tasksKey; // internal
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/TaskResult"))
    private List<TaskResult> toReprocess; // internal

    @JsonIgnore
    public abstract JobType getType();

}
