/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.cf.CalculatedField;

import java.util.Comparator;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CalculatedFieldExportData<E extends ExportableEntity<?>> extends EntityExportData<E> {

    public static final Comparator<CalculatedField> calculatedFieldsComparator = Comparator.comparing(CalculatedField::getName);

    @JsonProperty(index = 102)
    @JsonIgnoreProperties({"entityId", "createdTime", "version"})
    private List<CalculatedField> calculatedFields;

    @JsonIgnore
    @Override
    public boolean hasCalculatedFields() {
        return calculatedFields != null;
    }

    @Override
    public CalculatedFieldExportData<E> sort() {
        super.sort();
        if (calculatedFields != null && !calculatedFields.isEmpty()) {
            calculatedFields.sort(calculatedFieldsComparator);
        }
        return this;
    }

}
