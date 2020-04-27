///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { JsonSettingsSchema } from '@shared/models/widget.models';

export function initSchema(): JsonSettingsSchema {
    return {
        schema: {
            type: 'object',
            properties: {},
            required: []
        },
        form: [],
        groupInfoes: []
    };
}

export function addGroupInfo(schema: JsonSettingsSchema, title: string) {
    schema.groupInfoes.push({
        formIndex: schema.groupInfoes?.length || 0,
        GroupTitle: title
    });
}

export function addToSchema(schema: JsonSettingsSchema, newSchema: JsonSettingsSchema) {
    Object.assign(schema.schema.properties, newSchema.schema.properties);
    schema.schema.required = schema.schema.required.concat(newSchema.schema.required);
    schema.form.push(newSchema.form);
}

export function mergeSchemes(schemes: JsonSettingsSchema[]): JsonSettingsSchema {
    return schemes.reduce((finalSchema: JsonSettingsSchema, schema: JsonSettingsSchema) => {
        return {
            schema: {
                properties: {
                    ...finalSchema.schema.properties,
                    ...schema.schema.properties
                },
                required: [
                    ...finalSchema.schema.required,
                    ...schema.schema.required
                ]
            },
            form: [
                ...finalSchema.form,
                ...schema.form
            ]
        } as JsonSettingsSchema;
    }, initSchema());
}

export function addCondition(schema: JsonSettingsSchema, condition: string, exclude: string[] = []): JsonSettingsSchema {
    schema.form = schema.form.map(element => {
        if (!exclude.includes(element)) {
            if (typeof element === 'string') {
                return {
                    key: element,
                    condition
                }
            }
            if (typeof element === 'object') {
                if (element.condition) {
                    element.condition += ' && ' + condition
                }
                else element.condition = condition;
            }
        }
        return element;
    });
    return schema;
}
