
export function initSchema() {
    return {
        schema: {
            type: "object",
            properties: {},
            required: []
        },
        form: [],
        groupInfoes: []
    };
}

export function addGroupInfo(schema, title: string) {
    schema.groupInfoes.push({
        "formIndex": schema.groupInfoes?.length || 0,
        "GroupTitle": title
    });
}

export function addToSchema(schema, newSchema) {
    Object.assign(schema.schema.properties, newSchema.schema.properties);
    schema.schema.required = schema.schema.required.concat(newSchema.schema.required);
    schema.form.push(newSchema.form);
}

export function mergeSchemes(schemes: any[]) {
    return schemes.reduce((finalSchema, schema) => {
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
        }
    }, initSchema());
}

export function addCondition(schema, condition: String) {
    schema.form = schema.form.map(element => {
        if (typeof element === 'string') {
            return {
                key: element,
                condition: condition
            }
        }
        if (typeof element == 'object') {
            if (element.condition) {
                element.condition += ' && ' + condition
            }
            else element.condition = condition;
        }
        return element;
    });
    return schema;
}