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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.util.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TbDDFFileParser {
    private static final TbDefaultDDFFileValidator ddfFileValidator = new TbDefaultDDFFileValidator();

    public List<ObjectModel> parse(InputStream inputStream, String streamName)
            throws InvalidDDFFileException, IOException {
        streamName = streamName == null ? "" : streamName;

        log.debug("Parsing DDF file {}", streamName);

        try {
            // Parse XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            // Get DDF file validator
            LwM2m.LwM2mVersion lwm2mVersion = null;
            ddfFileValidator.validate(document);

            // Build list of ObjectModel
            ArrayList<ObjectModel> objects = new ArrayList<>();
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("Object");
            for (int i = 0; i < nodeList.getLength(); i++) {
                objects.add(parseObject(nodeList.item(i), streamName, lwm2mVersion, true));
            }
            return objects;
        } catch (InvalidDDFFileException | SAXException e) {
            throw new InvalidDDFFileException(e, "Invalid DDF file %s", streamName);
        }
        catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create Document Builder", e);
        }
    }

    private ObjectModel parseObject(Node object, String streamName, LwM2m.LwM2mVersion schemaVersion, boolean validate)
            throws InvalidDDFFileException {

        Node objectType = object.getAttributes().getNamedItem("ObjectType");
        if (validate && (objectType == null || !"MODefinition".equals(objectType.getTextContent()))) {
            throw new InvalidDDFFileException(
                    "Object element in %s MUST have a ObjectType attribute equals to 'MODefinition'.", streamName);
        }

        Integer id = null;
        String name = null;
        String description = null;
        String version = ObjectModel.DEFAULT_VERSION;
        Boolean multiple = null;
        Boolean mandatory = null;
        Map<Integer, ResourceModel> resources = new HashMap<>();
        String urn = null;
        String description2 = null;
        String lwm2mVersion = ObjectModel.DEFAULT_VERSION;

        for (int i = 0; i < object.getChildNodes().getLength(); i++) {
            Node field = object.getChildNodes().item(i);
            if (field.getNodeType() != Node.ELEMENT_NODE)
                continue;

            switch (field.getNodeName()) {
                case "ObjectID":
                    id = Integer.valueOf(field.getTextContent());
                    break;
                case "Name":
                    name = field.getTextContent();
                    break;
                case "Description1":
                    description = field.getTextContent();
                    break;
                case "ObjectVersion":
                    if (!StringUtils.isEmpty(field.getTextContent())) {
                        version = field.getTextContent();
                    }
                    break;
                case "MultipleInstances":
                    if ("Multiple".equals(field.getTextContent())) {
                        multiple = true;
                    } else if ("Single".equals(field.getTextContent())) {
                        multiple = false;
                    }
                    break;
                case "Mandatory":
                    if ("Mandatory".equals(field.getTextContent())) {
                        mandatory = true;
                    } else if ("Optional".equals(field.getTextContent())) {
                        mandatory = false;
                    }
                    break;
                case "Resources":
                    for (int j = 0; j < field.getChildNodes().getLength(); j++) {
                        Node item = field.getChildNodes().item(j);
                        if (item.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        if (item.getNodeName().equals("Item")) {
                            ResourceModel resource = parseResource(item, streamName);
                            if (validate && resources.containsKey(resource.id)) {
                                throw new InvalidDDFFileException(
                                        "Object %s in %s contains at least 2 resources with same id %s.",
                                        id != null ? id : "", streamName, resource.id);
                            } else {
                                resources.put(resource.id, resource);
                            }
                        }
                    }
                    break;
                case "ObjectURN":
                    urn = field.getTextContent();
                    break;
                case "LWM2MVersion":
                    if (!StringUtils.isEmpty(field.getTextContent())) {
                        lwm2mVersion = field.getTextContent();
                        if (schemaVersion != null && !schemaVersion.toString().equals(lwm2mVersion)) {
                            throw new InvalidDDFFileException(
                                    "LWM2MVersion is not consistent with xml shema(xsi:noNamespaceSchemaLocation) in %s : %s  expected but was %s.",
                                    streamName, schemaVersion, lwm2mVersion);
                        }
                    }
                    break;
                case "Description2":
                    description2 = field.getTextContent();
                    break;
                default:
                    break;
            }
        }

        return new ObjectModel(id, name, description, version, multiple, mandatory, resources.values(), urn,
                lwm2mVersion, description2);

    }

    private ResourceModel parseResource(Node item, String streamName) throws DOMException, InvalidDDFFileException {

        Integer id = Integer.valueOf(item.getAttributes().getNamedItem("ID").getTextContent());
        String name = null;
        ResourceModel.Operations operations = null;
        Boolean multiple = false;
        Boolean mandatory = false;
        ResourceModel.Type type = null;
        String rangeEnumeration = null;
        String units = null;
        String description = null;

        for (int i = 0; i < item.getChildNodes().getLength(); i++) {
            Node field = item.getChildNodes().item(i);
            if (field.getNodeType() != Node.ELEMENT_NODE)
                continue;

            switch (field.getNodeName()) {
                case "Name":
                    name = field.getTextContent();
                    break;
                case "Operations":
                    String strOp = field.getTextContent();
                    if (strOp != null && !strOp.isEmpty()) {
                        operations = ResourceModel.Operations.valueOf(strOp);
                    } else {
                        operations = ResourceModel.Operations.NONE;
                    }
                    break;
                case "MultipleInstances":
                    if ("Multiple".equals(field.getTextContent())) {
                        multiple = true;
                    } else if ("Single".equals(field.getTextContent())) {
                        multiple = false;
                    }
                    break;
                case "Mandatory":
                    if ("Mandatory".equals(field.getTextContent())) {
                        mandatory = true;
                    } else if ("Optional".equals(field.getTextContent())) {
                        mandatory = false;
                    }
                    break;
                case "Type":
                    switch (field.getTextContent()) {
                        case "String":
                            type = ResourceModel.Type.STRING;
                            break;
                        case "Integer":
                            type = ResourceModel.Type.INTEGER;
                            break;
                        case "Float":
                            type = ResourceModel.Type.FLOAT;
                            break;
                        case "Boolean":
                            type = ResourceModel.Type.BOOLEAN;
                            break;
                        case "Opaque":
                            type = ResourceModel.Type.OPAQUE;
                            break;
                        case "Time":
                            type = ResourceModel.Type.TIME;
                            break;
                        case "Objlnk":
                            type = ResourceModel.Type.OBJLNK;
                            break;
                        case "Unsigned Integer":
                            type = ResourceModel.Type.UNSIGNED_INTEGER;
                            break;
                        case "Corelnk":
                            type = ResourceModel.Type.CORELINK;
                            break;
                        case "":
                            type = ResourceModel.Type.NONE;
                            break;
                        default:
                            break;
                    }
                    break;
                case "RangeEnumeration":
                    rangeEnumeration = field.getTextContent();
                    break;
                case "Units":
                    units = field.getTextContent();
                    break;
                case "Description":
                    description = field.getTextContent();
                    break;
                default:
                    break;
            }
        }
        return new ResourceModel(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }
}
