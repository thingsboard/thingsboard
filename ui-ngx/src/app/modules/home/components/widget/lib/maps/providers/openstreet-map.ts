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

import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { UnitedMapSettings } from '../map-models';
import { WidgetContext } from '@home/models/widget-component.models';

export class OpenStreetMap extends LeafletMap {
    constructor(ctx: WidgetContext, $container, options: UnitedMapSettings) {
        super(ctx, $container, options);
        const map =  new L.Map($container, {editable: !!options.editablePolygon}).setView(options?.defaultCenterPosition, options?.defaultZoomLevel);
        let tileLayer;
        if (options.useCustomProvider)
            tileLayer = L.tileLayer(options.customProviderTileUrl);
        else
            tileLayer = (L.tileLayer as any).provider(options.mapProvider || 'OpenStreetMap.Mapnik');
        tileLayer.addTo(map);
        super.setMap(map);
        super.initSettings(options);
    }
}
