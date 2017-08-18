/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public abstract class DataValidator<D extends BaseData<?>> {

    private static EmailValidator emailValidator = EmailValidator.getInstance();
    
    public void validate(D data) {
        try {
            if (data == null) {
                throw new DataValidationException("Data object can't be null!");
            }
            validateDataImpl(data);
            if (data.getId() == null) {
                validateCreate(data);
            } else {
                validateUpdate(data);
            }
        } catch (DataValidationException e) {
            log.error("Data object is invalid: [{}]", e.getMessage());
            throw e;
        }
    }
    
    protected void validateDataImpl(D data) {
    }
    
    protected void validateCreate(D data) {
    }

    protected void validateUpdate(D data) {
    }
    
    protected boolean isSameData(D existentData, D actualData) {
        if (actualData.getId() == null) {
            return false;
        } else if (!existentData.getId().equals(actualData.getId())) {
            return false;
        }
        return true;
    }
    
    protected static void validateEmail(String email) {
        if (!emailValidator.isValid(email)) {
            throw new DataValidationException("Invalid email address format '" + email + "'!");
        }
    }
    
    protected static void validateJsonStructure(JsonNode expectedNode, JsonNode actualNode) {
        Set<String> expectedFields = new HashSet<>();        
        Iterator<String> fieldsIterator = expectedNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            expectedFields.add(fieldsIterator.next());
        }
        
        Set<String> actualFields = new HashSet<>();        
        fieldsIterator = actualNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            actualFields.add(fieldsIterator.next());
        }
        
        if (!expectedFields.containsAll(actualFields) || !actualFields.containsAll(expectedFields)) {
            throw new DataValidationException("Provided json structure is different from stored one '" + actualNode + "'!");
        }
        
        for (String field : actualFields) {
            if (!actualNode.get(field).isTextual()) {
                throw new DataValidationException("Provided json structure can't contain non-text values '" + actualNode + "'!");
            }
        }
    }
}
