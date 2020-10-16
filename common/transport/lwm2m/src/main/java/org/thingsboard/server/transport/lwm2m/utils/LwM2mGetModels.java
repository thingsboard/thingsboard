/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;

@Slf4j
@Component("LwM2mGetModels")
@ConditionalOnExpression("('${(service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled}'=='true') || ('${service.type:null}'=='monolith'  && '${transport.lwm2m.enabled}'=='true') || '${service.type:null}'=='tb-core'")
public class LwM2mGetModels {

    @Getter
    @Value("${transport.lwm2m.model_path_file:}")
    private String modelPathFile;

    @Getter
    @Setter
    private List<ObjectModel> models;

    @PostConstruct
    public void init() {
        models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadObjectsFromDir(new File(getModelPathFile())));
    }
}
