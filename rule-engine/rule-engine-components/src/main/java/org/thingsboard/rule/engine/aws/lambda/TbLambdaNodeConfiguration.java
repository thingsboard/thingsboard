/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.aws.lambda;

import com.amazonaws.services.lambda.model.InvocationType;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbLambdaNodeConfiguration implements NodeConfiguration<TbLambdaNodeConfiguration> {

    private String accessKey;
    private String secretKey;
    private String region;
    private String functionName;
    private String invocationType;
    private String qualifier;
    private int connectionTimeout;
    private int requestTimeout;
    private boolean tellFailureIfFuncThrowsExc;

    @Override
    public TbLambdaNodeConfiguration defaultConfiguration() {
        TbLambdaNodeConfiguration configuration = new TbLambdaNodeConfiguration();
        configuration.setRegion("us-east-1");
        configuration.setInvocationType(InvocationType.RequestResponse.name());
        configuration.setConnectionTimeout(10000);
        configuration.setRequestTimeout(5000);
        configuration.setTellFailureIfFuncThrowsExc(false);
        return configuration;
    }
}
