/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server;

import com.google.gson.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.json.JsonException;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MProvider;
import org.thingsboard.server.transport.lwm2m.server.json.LwM2mNodeDeserializer;
import org.thingsboard.server.transport.lwm2m.server.json.LwM2mNodeSerializer;
import org.thingsboard.server.transport.lwm2m.server.json.RegistrationSerializer;
import org.thingsboard.server.transport.lwm2m.server.json.ResponseSerializer;
import org.thingsboard.server.transport.lwm2m.server.model.ObjectModelSerDes;

import javax.annotation.PostConstruct;
import java.util.*;

import static org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MProvider.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.transport.lwm2m.server.adaptors.LwM2MProvider.DEVICE_TELEMETRY_TOPIC;

@Service("LwM2MTransportRequest")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
@Slf4j
public class LwM2MTransportRequest {

    private ObjectModelSerDes serializer;
    private Gson gson;
    private JsonObject jsonModelAttributes;
    private JsonObject jsonModelTelemetry;
    @Autowired
    private LwM2MTransportService service;

    @Autowired
    private LeshanServer lhServer;

    @Autowired
    private LwM2MTransportCtx context;


    @PostConstruct
    public void init() {

        this.serializer = new ObjectModelSerDes();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Registration.class,
                new RegistrationSerializer(lhServer.getPresenceService()));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mResponse.class, new ResponseSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    @SneakyThrows
    public void doGet(String pathInfo, String contentFormatParam) {
        // all registered clients
        if (pathInfo == null) {
            Collection<Registration> registrations = new ArrayList<>();
            for (Iterator<Registration> iterator = lhServer.getRegistrationService().getAllRegistrations(); iterator
                    .hasNext(); ) {
                registrations.add(iterator.next());
            }
            String json = this.gson.toJson(registrations.toArray(new Registration[]{}));
            JsonElement jsonElement = LwM2MProvider.validateJsonPayload(json);
            log.info("EndPoint: get all registered clients: [{}]", jsonElement);
            return;
        }
        String[] path = StringUtils.split(pathInfo, '/');
        if (path.length < 1) {
            log.info("Invalid path");
            return;
        }
        String clientEndpoint = path[0];
        Registration registration = lhServer.getRegistrationService().getByEndpoint(clientEndpoint);
        if (registration != null) {
            // /clientEndpoint : get client
            if (path.length == 1) {
                String json = this.gson.toJson(registration);
                JsonElement jsonElement = LwM2MProvider.validateJsonPayload(json);
                log.info("EndPoint: get client: [{}]", jsonElement);
                return;
            }

            // /clientEndpoint/LWRequest/discover : do LightWeight M2M discover request on a given client.
            if (path.length >= 3 && "discover".equals(path[path.length - 1])) {
                String target = StringUtils.substringBetween(pathInfo, clientEndpoint, "/discover");
                try {
                    // create & process request
                    DiscoverRequest request = new DiscoverRequest(target);
                    DiscoverResponse cResponse = lhServer.send(registration, request, context.getTimeout());
                    processDeviceResponse(pathInfo, cResponse);
                } catch (RuntimeException | InterruptedException e) {
                    log.error("EndPoint: get client/discover: with id [{}]: [{}]", clientEndpoint, e);
                }
                return;
            }

            // //clientEndpoint/objectspecs :  // Get Model for this registration
            if (path.length == 2 && "objectspecs".equals(path[path.length - 1])) {
                // Get Model for this registration
                try {
                    LwM2mModel model = lhServer.getModelProvider().getObjectModel(registration);
                    String objectModels = serializer.sSerialize(model.getObjectModels());
                    JsonElement jsonObjectModels = LwM2MProvider.validateJsonPayload(objectModels);
                    log.info("EndPoint: get objectspecs: [{}]", jsonObjectModels);
                    // for test tmp
                    jsonModelAttributes = new JsonObject();
                    jsonModelTelemetry = new JsonObject();
                    getAttrTelemetryGson(jsonObjectModels);
                    Link[] objectLinks = registration.getObjectLinks();
                    String[] msgs = new String[2];
                    setAttrTelemetry(objectLinks, msgs, clientEndpoint, registration);
                    String msgAtr = msgs[0];
                    String msgTelemetry = msgs[1];

                    service.processDevicePublish(msgAtr, DEVICE_ATTRIBUTES_TOPIC, -1, clientEndpoint);
                    service.processDevicePublish(msgTelemetry, DEVICE_TELEMETRY_TOPIC, -1, clientEndpoint);
                } catch (JsonException e) {
                    log.error("EndPoint: get  objectspecs: with id [{}]: [{}]", clientEndpoint, e);
                }
                return;
            }

            // /clientEndpoint/LWRequest : do LightWeight M2M read request on a given client.
            try {
                String target = StringUtils.removeStart(pathInfo, "/" + clientEndpoint);
                // get content format
                ContentFormat contentFormat = contentFormatParam != null ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;
                // create & process request
                ReadRequest request = new ReadRequest(contentFormat, target);
                ReadResponse cResponse = lhServer.send(registration, request, context.getTimeout());
                processDeviceResponse(pathInfo, cResponse);
            } catch (RuntimeException | InterruptedException e) {
                log.error("EndPoint: get client/read: with id [{}]: [{}]", clientEndpoint, e);
            }
        } else {
            log.warn("EndPoint: get: no registered client with id [{}]", clientEndpoint);
        }


    }

    public ReadResponse doGetRead(String pathInfo, String contentFormatParam, String clientEndpoint, Registration registration) {
        // /clientEndpoint/LWRequest : do LightWeight M2M read request on a given client.
        try {
            String target = StringUtils.removeStart(pathInfo, "/" + clientEndpoint);
            // get content format
            ContentFormat contentFormat = contentFormatParam != null ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;
            // create & process request
            ReadRequest request = new ReadRequest(contentFormat, target);
            return lhServer.send(registration, request, context.getTimeout());

        } catch (RuntimeException | InterruptedException e) {
            log.error("EndPoint: get client/read: with id [{}]: [{}]", clientEndpoint, e);
            return null;
        }
    }

    public JsonElement processDeviceResponse(String pathInfo, LwM2mResponse cResponse) throws AdaptorException {
        JsonElement jsonElement = null;
        if (cResponse != null) {
            String response = this.gson.toJson(cResponse);
            jsonElement = LwM2MProvider.validateJsonPayload(response);
            log.info("EndPoint: get client: [{}]", jsonElement);
        } else {
            log.warn("Request [{}] timed out.", pathInfo);
        }
        return jsonElement;
    }

    private void getAttrTelemetryGson(JsonElement jsonDeviceObjects) {
        jsonDeviceObjects.getAsJsonArray().forEach(je -> {
            JsonObject jo = je.getAsJsonObject();
            if (jo.has("name") && jo.has("id")) {
                String nameObj = jo.get("name").getAsString();
                String idObj = jo.get("id").getAsString();
                if (jo.has("resourcedefs")) {
                    JsonElement jrs = jo.get("resourcedefs");
                    String finalIdObj = idObj;
                    String finalnameObj = nameObj;
                    JsonArray arrayResAttr = new JsonArray();
                    JsonArray arrayResTel = new JsonArray();
                    jrs.getAsJsonArray().forEach(jr -> {
                        JsonObject jro = jr.getAsJsonObject();
                        if (jro.has("operations") && jro.has("name") && jro.has("id")) {
                            String nameRes = jro.get("name").getAsString();
                            String idRes = jro.get("id").getAsString();
                            // key == pathResource: value objectName_instatnce==0_resourseName}
                            // attributes
                            if (jro.get("operations").getAsString().equals("R")) {
                                JsonObject attributes = new JsonObject();
                                attributes.addProperty("/" + finalIdObj + "/0/" + idRes, finalnameObj + "_0_" + nameRes);
                                arrayResAttr.add(attributes);
                            }
                            // telemetry
                            else if (jro.get("operations").getAsString().equals("RW")) {
                                JsonObject telemetry = new JsonObject();
                                telemetry.addProperty("/" + finalIdObj + "/0/" + idRes, finalnameObj + "_0_" + nameRes);
                            }

                        }
                    });
                    this.jsonModelAttributes.add("/" + idObj + "/0/", arrayResAttr);
                    this.jsonModelTelemetry.add("/" + idObj + "/0/", arrayResTel);
                }
            } else {
                log.error("Can't parse value: [{}]", jo);
            }
        });
    }

    public void setAttrTelemetry(Link[] objectLinks, String[] msgs, String endpointId, Registration registration) {
        JsonObject jsonAtr = new JsonObject();
        JsonObject jsonTel = new JsonObject();
        log.info("objectLinks: [{}]", objectLinks);
        List<Link> listLinks = Arrays.asList(objectLinks);
        listLinks.forEach(link -> {
            String[] linkSplits = link.getUrl().split("/");
            if (linkSplits.length > 1) {
                log.info("link.getUrl, link.getAttributes: [{}] [{}]", linkSplits[1], link.getAttributes());
                // request value [{objectLink}]
                String pathInfo = "/" + endpointId + "/" + linkSplits[1];
                ReadResponse cResponse = doGetRead(pathInfo, ContentFormat.TLV.getName(), endpointId, registration);
                try {
                    if (cResponse != null) {
                        JsonElement jsonElement = processDeviceResponse(pathInfo, cResponse);
                        if (jsonElement != null) {
                            String iddObj = jsonElement.getAsJsonObject().get("content").getAsJsonObject().get("id").getAsString();
                            jsonElement.getAsJsonObject().get("content").getAsJsonObject().get("instances").getAsJsonArray().forEach(instance -> {
                                String idInst = instance.getAsJsonObject().get("id").getAsString();
                                instance.getAsJsonObject().get("resources").getAsJsonArray().forEach(resource -> {
                                    String idRes = resource.getAsJsonObject().get("id").getAsString();
                                    String valueRes;
                                    if (resource.getAsJsonObject().has("value")) {
                                        valueRes = resource.getAsJsonObject().get("value").getAsString();
                                    } else {
                                        valueRes = resource.getAsJsonObject().get("values").getAsJsonObject().toString();
                                    }
                                    this.jsonModelAttributes.entrySet().stream().forEach(ress -> {
                                        log.info("jsonModelAttributes key, value: [{}] [{}]", ress.getKey().split("/")[1], ress.getValue());
                                        if (iddObj.equals(ress.getKey().split("/")[1])) {
                                            ress.getValue().getAsJsonArray().forEach(res -> {
                                                res.getAsJsonObject().entrySet().forEach(resVal -> {
                                                    if (idRes.equals(resVal.getKey().split("/")[2])) {
                                                         String nameAttr = resVal.getValue().getAsString().replaceAll("_0_", "_" + idInst + "_");
                                                        jsonAtr.addProperty(nameAttr, valueRes);
                                                    }
                                                });
                                            });
                                        }
                                    });

                                    this.jsonModelTelemetry.entrySet().stream().forEach(ress -> {
                                        log.info("jsonModelAttributes key, value: [{}] [{}]", ress.getKey().split("/")[1], ress.getValue());
                                        if (iddObj.equals(ress.getKey().split("/")[1])) {
                                            ress.getValue().getAsJsonArray().forEach(res -> {
                                                res.getAsJsonObject().entrySet().forEach(resVal -> {
                                                    if (idRes.equals(resVal.getKey().split("/")[2])) {
                                                        log.info("jsonModelAttributesResourses: [{}]", res);
                                                        String nameAttr = resVal.getValue().getAsString().replaceAll("_0_", "_" + idInst + "_");
                                                        jsonTel.addProperty(nameAttr, valueRes);
                                                    }
                                                });
                                            });
                                        }
                                    });

                                });
                            });
                        }
                    }
                } catch (AdaptorException e) {
                    e.printStackTrace();
                }
            }
        });
        if (!jsonAtr.isJsonNull()) msgs[0] = jsonAtr.toString();
        if (!jsonTel.isJsonNull()) msgs[1] = jsonTel.toString();
    }
}
