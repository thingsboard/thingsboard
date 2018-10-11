/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.model;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.ShortCustomerInfo;

import java.util.HashSet;

public interface MultipleCustomerAssignmentEntity {
    ObjectMapper objectMapper = new ObjectMapper();
    JavaType assignedCustomersType = objectMapper.getTypeFactory().constructCollectionType(HashSet.class, ShortCustomerInfo.class);

    String getAssignedCustomers();
    void setAssignedCustomers(String assignedCustomers);
}
