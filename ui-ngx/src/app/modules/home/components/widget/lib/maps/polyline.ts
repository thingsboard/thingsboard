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
import { safeExecute } from '@app/core/utils';

export class Polyline {

    leafletPoly: L.Polyline;
    dataSources;
    data;

    constructor(private map: L.Map, locations, data, dataSources, settings) {
        console.log("Polyline -> constructor -> data", data)
        this.dataSources = dataSources;
        this.data = data;
        this.leafletPoly = L.polyline(locations,
            this.getPolyStyle(settings, data, dataSources)
        ).addTo(this.map);
    }

    updatePolyline(settings, data, dataSources) {
        this.leafletPoly.setStyle(this.getPolyStyle(settings, data, dataSources));

    }

    getPolyStyle(settings, data, dataSources): L.PolylineOptions {
        return {
            color: settings.useColorFunction ? safeExecute(settings.colorFunction, [data, dataSources, data[0]?.dsIndex]) : settings.color,
            opacity: settings.strokeOpacity,
            weight: settings.strokeWeight
        }
    }

    removePolyline() {
        this.map.removeLayer(this.leafletPoly);
    }

    getPolylineLatLngs() {
        return this.leafletPoly.getLatLngs();
    }

    setPolylineLatLngs(latLngs) {
        this.leafletPoly.setLatLngs(latLngs);
    }
}