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

import java.util.concurrent.TimeUnit;

@Data
public class TbLambdaNodeConfiguration implements NodeConfiguration<TbLambdaNodeConfiguration> {

    public static final String DEFAULT_QUALIFIER = "$LATEST";

    private String accessKey;
    private String secretKey;
    private String region;
    private String functionName;
    private InvocationType invocationType;
    private String qualifier;
    private int connectionTimeout;
    private int requestTimeout;
    private boolean tellFailureIfFuncThrowsExc;

    @Override
    public TbLambdaNodeConfiguration defaultConfiguration() {
        TbLambdaNodeConfiguration configuration = new TbLambdaNodeConfiguration();
        configuration.setRegion("us-east-1");
        configuration.setInvocationType(InvocationType.RequestResponse);
        configuration.setQualifier(DEFAULT_QUALIFIER);
        configuration.setConnectionTimeout(10);
        configuration.setRequestTimeout(5);
        configuration.setTellFailureIfFuncThrowsExc(false);
        return configuration;
    }

    public int getConnectionTimeout() {
        return (int) TimeUnit.SECONDS.toMillis(connectionTimeout);
    }

    public int getRequestTimeout() {
        return (int) TimeUnit.SECONDS.toMillis(requestTimeout);
    }

}
