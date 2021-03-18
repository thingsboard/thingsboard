/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mInstance;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.LwM2mResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Data
public class Resource implements HasTenantId, Serializable {

    private static final long serialVersionUID = 7379609705527272306L;

    private TenantId tenantId;
    private ResourceType resourceType;
    private String resourceId;
    private String textSearch;
    private String value;

    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder("Resource{");
        res.append("tenantId=").append(tenantId);
        res.append(", resourceType='").append(resourceType).append('\'');
        res.append(", resourceId='").append(resourceId).append('\'');
        res.append(", textSearch='").append(textSearch).append('\'');
        res.append('}');
        return res.toString();
    }

    public LwM2mObject toLwM2mObject () {
            try {
                DDFFileParser ddfFileParser = new DDFFileParser(new DefaultDDFFileValidator());
                List<ObjectModel> objectModels = ddfFileParser.parseEx(new ByteArrayInputStream(Base64.getDecoder().decode(this.value)), this.textSearch);
                if (objectModels.size() == 0) {
                    return null;
                }
                else {
                    ObjectModel obj = objectModels.get(0);
                    LwM2mObject lwM2mObject = new LwM2mObject();
                    lwM2mObject.setId(obj.id);
                    lwM2mObject.setKeyId(this.resourceId);
                    lwM2mObject.setName(obj.name);
                    lwM2mObject.setMultiple(obj.multiple);
                    lwM2mObject.setMandatory(obj.mandatory);
                    LwM2mInstance instance = new LwM2mInstance();
                    instance.setId(0);
                    List<LwM2mResource> resources = new ArrayList<>();
                    obj.resources.forEach((k, v) -> {
                        if (!v.operations.isExecutable()) {
                            LwM2mResource resource = new LwM2mResource(k, v.name, false, false, false);
                            resources.add(resource);
                        }
                    });
                    instance.setResources(resources.stream().toArray(LwM2mResource[]::new));
                    lwM2mObject.setInstances(new LwM2mInstance[]{instance});
                   return lwM2mObject;
                }
            } catch (IOException | InvalidDDFFileException e) {
                log.error("Could not parse the XML of objectModel with name [{}]", this.textSearch, e);
                return  null;
            }
    }
}
