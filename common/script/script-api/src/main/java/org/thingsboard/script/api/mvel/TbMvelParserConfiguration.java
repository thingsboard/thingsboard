/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.script.api.mvel;

import org.mvel2.ParserConfiguration;
import org.mvel2.integration.VariableResolverFactory;

public class TbMvelParserConfiguration extends ParserConfiguration {

    private static final long serialVersionUID = 5558151976348875590L;

    TbMvelParserConfiguration() {
        setClassLoader(new TbMvelClassLoader());
    }

    @Override
    public VariableResolverFactory getVariableFactory(VariableResolverFactory factory) {
        if (Thread.interrupted()) {
            throw new RuntimeException("Thread is interrupted!");
        }
        return new TbMvelResolverFactory(factory);
    }

}
