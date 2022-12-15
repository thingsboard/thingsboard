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
package org.thingsboard.server.transport.mqtt.util.sparkplug.json;

/**
 * Validates JSON.
 */
public class JsonValidator {
	
	protected static final String JSON_SCHEMA_FILENAME = "payload.json";
	
	private static JsonValidator instance = null;
	
	/**
	 * Constructor.
	 */
	protected JsonValidator() {
	}

	/**
	 * Returns the {@link JsonValidator} instance.
	 * 
	 * @return the {@link JsonValidator} instance.
	 */
	public static JsonValidator getInstance() {
		if (instance == null) {
			instance = new JsonValidator();
		}
		return instance;
	}
	
	/**
	 * Returns loads and returns the {@link JsonSchema} instance associated with this validator.
	 * 
	 * @return the {@link JsonSchema} instance associated with this validator.
	 * @throws IOException
	 * @throws ProcessingException
	 */
/*	protected JsonSchema getSchema() throws IOException, ProcessingException {	
		//Get file from resources folder
		ClassLoader classLoader = getClass().getClassLoader();
		File schemaFile = new File(classLoader.getResource(JSON_SCHEMA_FILENAME).getFile());
        return JsonSchemaFactory.byDefault().getJsonSchema(JsonLoader.fromFile(schemaFile));
	}*/
	
	/**
	 * Returns true if the supplied JSON text is valid, false otherwise.
	 * 
	 * @param jsonText a {@link String} representing JSON text.
	 * @return true if the supplied JSON text is valid, false otherwise.
	 * @throws ProcessingException
	 * @throws IOException
	 */
/*	public boolean isJsonValid(String jsonText) throws ProcessingException, IOException {
        return getSchema().validate(JsonLoader.fromString(jsonText)).isSuccess();
    }*/
}
