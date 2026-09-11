// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.aws.lambda;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbAwsLambdaNodeConfiguration implements NodeConfiguration<TbAwsLambdaNodeConfiguration> {

    public static final String DEFAULT_QUALIFIER = "$LATEST";

    @NotBlank
    private String accessKey;
    @NotBlank
    private String secretKey;
    @NotBlank
    private String region;
    @NotBlank
    private String functionName;
    private String qualifier;
    @Min(0)
    private int connectionTimeout;
    @Min(0)
    private int requestTimeout;
    private boolean tellFailureIfFuncThrowsExc;

    @Override
    public TbAwsLambdaNodeConfiguration defaultConfiguration() {
        TbAwsLambdaNodeConfiguration configuration = new TbAwsLambdaNodeConfiguration();
        configuration.setRegion("us-east-1");
        configuration.setQualifier(DEFAULT_QUALIFIER);
        configuration.setConnectionTimeout(10);
        configuration.setRequestTimeout(5);
        configuration.setTellFailureIfFuncThrowsExc(false);
        return configuration;
    }

}
