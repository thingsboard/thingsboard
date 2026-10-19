// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Output {

    private String name;
    private OutputType type;
    private AttributeScope scope;
    private Integer decimalsByDefault;

}
