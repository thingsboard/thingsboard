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
import { createTooltip } from './maps-utils';

export class Polygon {

    map: L.Map;
    leafletPoly: L.Polygon;

    tooltip;

    constructor(latLangs, settings, location, onClickListener) {
        this.leafletPoly = L.polygon(latLangs, {
            fill: true,
            fillColor: settings.polygonColor,
            color: settings.polygonStrokeColor,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity
        }).addTo(this.map);

        if (settings.showTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, location.dsIndex, settings);
        }
        if (onClickListener) {
            this.leafletPoly.on('click', onClickListener);
        }
    }

    removePolygon() {
        this.map.removeLayer(this.leafletPoly);
    }

    updatePolygonColor(settings, color) {
        let style = {
            fill: true,
            fillColor: color,
            color: color,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity
        };
        this.leafletPoly.setStyle(style);
    }

    getPolygonLatLngs() {
        return this.leafletPoly.getLatLngs();
    }

    setPolygonLatLngs(latLngs) {
        this.leafletPoly.setLatLngs(latLngs);
        this.leafletPoly.redraw();
    }
}