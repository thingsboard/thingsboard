// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
                @DiscriminatorMapping(value = "DUMMY", schema = DummyJobConfiguration.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(name = "DUMMY", value = DummyJobConfiguration.class),
})
@Data
public abstract class JobConfiguration implements Serializable {

    @NotBlank
    private String tasksKey; // internal
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/TaskResult"))
    private List<TaskResult> toReprocess; // internal

    @JsonIgnore
    public abstract JobType getType();

}
