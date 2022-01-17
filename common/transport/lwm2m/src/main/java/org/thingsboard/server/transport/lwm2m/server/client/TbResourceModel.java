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
package org.thingsboard.server.transport.lwm2m.server.client;

import org.eclipse.leshan.core.model.ResourceModel;

import java.io.Serializable;

public class TbResourceModel extends ResourceModel implements Serializable {

    private static final long serialVersionUID = -2082846558899793932L;

    public TbResourceModel(Integer id, String name, Operations operations, Boolean multiple, Boolean mandatory, Type type, String rangeEnumeration, String units, String description) {
        super(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }
}
