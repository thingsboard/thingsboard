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
package org.thingsboard.rule.engine.geo;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.FILTER,
        name = "gps geofencing filter",
        configClazz = TbGpsGeofencingFilterNodeConfiguration.class,
        relationTypes = {TbNodeConnectionType.TRUE, TbNodeConnectionType.FALSE},
        nodeDescription = "Filter incoming messages by GPS based geofencing",
        nodeDetails = "Extracts latitude and longitude parameters from the incoming message and checks them according to configured perimeter. </br>" +
                "Configuration:</br></br>" +
                "<ul>" +
                "<li>Latitude key name - name of the message field that contains location latitude;</li>" +
                "<li>Longitude key name - name of the message field that contains location longitude;</li>" +
                "<li>Perimeter type - Polygon or Circle;</li>" +
                "<li>Fetch perimeter from message metadata - checkbox to load perimeter from message metadata; " +
                "   Enable if your perimeter is specific to device/asset and you store it as device/asset attribute;</li>" +
                "<li>Perimeter key name - name of the metadata key that stores perimeter information;</li>" +
                "<li>For Polygon perimeter type: <ul>" +
                "    <li>Polygon definition - string that contains array of coordinates in the following format: [[lat1, lon1],[lat2, lon2],[lat3, lon3], ... , [latN, lonN]]</li>" +
                "</ul></li>" +
                "<li>For Circle perimeter type: <ul>" +
                "   <li>Center latitude - latitude of the circle perimeter center;</li>" +
                "   <li>Center longitude - longitude of the circle perimeter center;</li>" +
                "   <li>Range - value of the circle perimeter range, double-precision floating-point value;</li>" +
                "   <li>Range units - one of: Meter, Kilometer, Foot, Mile, Nautical Mile;</li>" +
                "</ul></li></ul></br>" +
                "Rule node will use default metadata key names, if the \"Fetch perimeter from message metadata\" is enabled and \"Perimeter key name\" is not configured. " +
                "Default metadata key names for polygon perimeter type is \"perimeter\". Default metadata key names for circle perimeter are: \"centerLatitude\", \"centerLongitude\", \"range\", \"rangeUnit\"." +
                "</br></br>" +
                "Structure of the circle perimeter definition (stored in server-side attribute, for example):" +
                "</br></br>" +
                "{\"latitude\":  48.198618758582384, \"longitude\": 24.65322245153503, \"radius\":  100.0, \"radiusUnit\": \"METER\" }" +
                "</br></br>" +
                "Available radius units: METER, KILOMETER, FOOT, MILE, NAUTICAL_MILE;<br><br>" +
                "Output connections: <code>True</code>, <code>False</code>, <code>Failure</code>",
        configDirective = "tbFilterNodeGpsGeofencingConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/filter/gps-geofencing-filter/"
)
public class TbGpsGeofencingFilterNode extends AbstractGeofencingNode<TbGpsGeofencingFilterNodeConfiguration> {

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ctx.tellNext(msg, checkMatches(msg) ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
    }

    @Override
    protected Class<TbGpsGeofencingFilterNodeConfiguration> getConfigClazz() {
        return TbGpsGeofencingFilterNodeConfiguration.class;
    }

}
