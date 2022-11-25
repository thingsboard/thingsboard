package org.thingsboard.server.service.edge.instructions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EdgeInstallInstructions {

    @ApiModelProperty(position = 1, value = "Markdown with docker install instructions")
    private String dockerInstallInstructions;
}
