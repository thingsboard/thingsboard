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
package org.thingsboard.rule.engine.geo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.common.util.geo.GeoUtil;
import org.thingsboard.common.util.geo.Perimeter;
import org.thingsboard.common.util.geo.PerimeterType;
import org.thingsboard.common.util.geo.RangeUnit;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collections;
import java.util.List;

public abstract class AbstractGeofencingNode<T extends TbGpsGeofencingFilterNodeConfiguration> implements TbNode {

    protected T config;
    protected JtsSpatialContext jtsCtx;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, getConfigClazz());
        JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
        factory.normWrapLongitude = true;
        jtsCtx = factory.newSpatialContext();
    }

    abstract protected Class<T> getConfigClazz();

    protected boolean checkMatches(TbMsg msg) throws TbNodeException {
        JsonElement msgDataElement = JsonParser.parseString(msg.getData());
        if (!msgDataElement.isJsonObject()) {
            throw new TbNodeException("Incoming Message is not a valid JSON object!");
        }
        JsonObject msgDataObj = msgDataElement.getAsJsonObject();
        double latitude = getValueFromMessageByName(msg, msgDataObj, config.getLatitudeKeyName());
        double longitude = getValueFromMessageByName(msg, msgDataObj, config.getLongitudeKeyName());
        List<Perimeter> perimeters = getPerimeters(msg);
        boolean matches = false;
        for (Perimeter perimeter : perimeters) {
            if (checkMatches(perimeter, latitude, longitude)) {
                matches = true;
                break;
            }
        }
        return matches;
    }

    protected boolean checkMatches(Perimeter perimeter, double latitude, double longitude) throws TbNodeException {
        if (perimeter.getPerimeterType() == PerimeterType.CIRCLE) {
            Coordinates entityCoordinates = new Coordinates(latitude, longitude);
            Coordinates perimeterCoordinates = new Coordinates(perimeter.getCenterLatitude(), perimeter.getCenterLongitude());
            return perimeter.getRange() > GeoUtil.distance(entityCoordinates, perimeterCoordinates, perimeter.getRangeUnit());
        } else if (perimeter.getPerimeterType() == PerimeterType.POLYGON) {
            return GeoUtil.contains(perimeter.getPolygonsDefinition(), new Coordinates(latitude, longitude));
        } else {
            throw new TbNodeException("Unsupported perimeter type: " + perimeter.getPerimeterType() + "!");
        }
    }

    protected List<Perimeter> getPerimeters(TbMsg msg) throws TbNodeException {
        if (config.isFetchPerimeterInfoFromMessageMetadata()) {
            if (StringUtils.isEmpty(config.getPerimeterKeyName())) {
                // Old configuration before "perimeterKeyName" was introduced
                String perimeterValue = msg.getMetaData().getValue("perimeter");
                if (!StringUtils.isEmpty(perimeterValue)) {
                    Perimeter perimeter = new Perimeter();
                    perimeter.setPerimeterType(PerimeterType.POLYGON);
                    perimeter.setPolygonsDefinition(perimeterValue);
                    return Collections.singletonList(perimeter);
                } else if (!StringUtils.isEmpty(msg.getMetaData().getValue("centerLatitude"))) {
                    Perimeter perimeter = new Perimeter();
                    perimeter.setPerimeterType(PerimeterType.CIRCLE);
                    perimeter.setCenterLatitude(Double.parseDouble(msg.getMetaData().getValue("centerLatitude")));
                    perimeter.setCenterLongitude(Double.parseDouble(msg.getMetaData().getValue("centerLongitude")));
                    perimeter.setRange(Double.parseDouble(msg.getMetaData().getValue("range")));
                    perimeter.setRangeUnit(RangeUnit.valueOf(msg.getMetaData().getValue("rangeUnit")));
                    return Collections.singletonList(perimeter);
                } else {
                    throw new TbNodeException("Missing perimeter definition!");
                }
            } else {
                String perimeterValue = msg.getMetaData().getValue(config.getPerimeterKeyName());
                if (!StringUtils.isEmpty(perimeterValue)) {
                    if (config.getPerimeterType().equals(PerimeterType.POLYGON)) {
                        Perimeter perimeter = new Perimeter();
                        perimeter.setPerimeterType(PerimeterType.POLYGON);
                        perimeter.setPolygonsDefinition(perimeterValue);
                        return Collections.singletonList(perimeter);
                    } else {
                        var circleDef = JacksonUtil.toJsonNode(perimeterValue);
                        Perimeter perimeter = new Perimeter();
                        perimeter.setPerimeterType(PerimeterType.CIRCLE);
                        perimeter.setCenterLatitude(circleDef.get("latitude").asDouble());
                        perimeter.setCenterLongitude(circleDef.get("longitude").asDouble());
                        perimeter.setRange(circleDef.get("radius").asDouble());
                        perimeter.setRangeUnit(circleDef.has("radiusUnit") ? RangeUnit.valueOf(circleDef.get("radiusUnit").asText()) : RangeUnit.METER);
                        return Collections.singletonList(perimeter);
                    }
                } else {
                    throw new TbNodeException("Missing perimeter definition!");
                }
            }
        } else {
            Perimeter perimeter = new Perimeter();
            perimeter.setPerimeterType(config.getPerimeterType());
            perimeter.setCenterLatitude(config.getCenterLatitude());
            perimeter.setCenterLongitude(config.getCenterLongitude());
            perimeter.setRange(config.getRange());
            perimeter.setRangeUnit(config.getRangeUnit());
            perimeter.setPolygonsDefinition(config.getPolygonsDefinition());
            return Collections.singletonList(perimeter);
        }
    }

    protected Double getValueFromMessageByName(TbMsg msg, JsonObject msgDataObj, String keyName) throws TbNodeException {
        double value;
        if (msgDataObj.has(keyName) && msgDataObj.get(keyName).isJsonPrimitive()) {
            value = msgDataObj.get(keyName).getAsDouble();
        } else {
            String valueStr = msg.getMetaData().getValue(keyName);
            if (!StringUtils.isEmpty(valueStr)) {
                value = Double.parseDouble(valueStr);
            } else {
                throw new TbNodeException("Incoming Message has no " + keyName + " in data or metadata!");
            }
        }
        return value;
    }

}
