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

export class TencentMap extends LeafletMap {
    constructor($container, options: UnitedMapSettings) {
        super($container, options);
        const txUrl = 'http://rt{s}.map.gtimg.com/realtimerender?z={z}&x={x}&y={y}&type=vector&style=0';
        const map = L.map($container).setView(options?.defaultCenterPosition, options?.defaultZoomLevel);
        const txLayer = L.tileLayer(txUrl, { subdomains: '0123', tms: true }).addTo(map);
        txLayer.addTo(map);
        super.setMap(map);
        super.initSettings(options);
    }
}
