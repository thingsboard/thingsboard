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
package org.thingsboard.server.common.data.device.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * for tdengine
 *
 * "table": {
 *   "columns": [{"name": "c1","type": "string","len": 10},{"name": "c2","type": "int"}],
 *   "tags": [{"name": "t1","type": "string","len": 10},{"name": "t2","type": "double"}]
 * }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TableInfo {

    private List<Column> columns;
    private List<Column> tags;
}