/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.util;


import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.model.DDFFileValidator;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.util.Validate;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;

/**
 * A DDF File Validator.
 * <p>
 * Validate a DDF File against the embedded LWM2M schema.
 * <p>
 * Support LWM2M version 1.0 and 1.1.
 */

public class TbDefaultDDFFileValidator implements DDFFileValidator {
    private static String LWM2M_V1_0_SCHEMA_PATH = "/schemas/LWM2M.xsd";
    private static String LWM2M_V1_1_SCHEMA_PATH = "/schemas/LWM2M-v1_1.xsd";

    private final String schema;

    /**
     * Create a {@link DDFFileValidator} using the LWM2M v1.1 schema.
     */
    public TbDefaultDDFFileValidator() {
        this(LwM2mVersion.V1_1);
    }

    /**
     * Create a {@link DDFFileValidator} using schema corresponding to LWM2M {@link LwM2mVersion}.
     */
    public TbDefaultDDFFileValidator(LwM2mVersion version) {
        Validate.notNull(version, "version must not be null");
        if (LwM2mVersion.V1_0.equals(version)) {
            schema = LWM2M_V1_0_SCHEMA_PATH;
        } else if (LwM2mVersion.V1_1.equals(version)) {
            schema = LWM2M_V1_1_SCHEMA_PATH;
        } else {
            throw new IllegalStateException(String.format("Unsupported version %s", version));
        }
    }

    @Override
    public void validate(Node xmlToValidate) throws InvalidDDFFileException {
        try {
            validate(new DOMSource(xmlToValidate));
        } catch (SAXException | IOException e) {
            throw new InvalidDDFFileException(e);
        }
    }

    /**
     * Validate a XML {@link Source} against the embedded LWM2M Schema.
     *
     * @param xmlToValidate an XML source to validate
     * @throws SAXException see {@link Validator#validate(Source)}
     * @throws IOException see {@link Validator#validate(Source)}
     */
    public void validate(Source xmlToValidate) throws SAXException, IOException {
        Validator validator = getEmbeddedLwM2mSchema().newValidator();
        validator.validate(xmlToValidate);
    }

    /**
     * Get the Embedded the LWM2M.xsd Schema.
     *
     * @throws SAXException see {@link SchemaFactory#newSchema(Source)}
     */
    protected Schema getEmbeddedLwM2mSchema() throws SAXException {
        InputStream inputStream = DDFFileValidator.class.getResourceAsStream(schema);
        Source source = new StreamSource(inputStream);
        SchemaFactory schemaFactory = createSchemaFactory();
        return schemaFactory.newSchema(source);
    }

    protected SchemaFactory createSchemaFactory() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
        try {
            // Create Safe SchemaFactory (not vulnerable to XXE Attacks)
            // --------------------------------------------------------
            // There is several recommendation from different source we try to apply all, even if some are maybe
            // redundant.

            // from :
            // https://semgrep.dev/docs/cheat-sheets/java-xxe/
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // from :
            // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#schemafactory
//            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
//            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException("Unable to create SchemaFactory", e);
        }
        return factory;
    }
}
