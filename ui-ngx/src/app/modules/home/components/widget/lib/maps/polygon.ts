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

import L, { LatLngExpression, LeafletMouseEvent } from 'leaflet';
import { createTooltip, functionValueCalculator, parseWithTranslation, safeExecute } from './maps-utils';
import 'leaflet-editable/src/Leaflet.Editable';
import { FormattedData, PolygonSettings } from './map-models';

export class Polygon {

    leafletPoly: L.Polygon;
    tooltip: L.Popup;
    data: FormattedData;
    dataSources: FormattedData[];

    constructor(public map, polyData: FormattedData, dataSources: FormattedData[], private settings: PolygonSettings, onDragendListener?) {
        this.dataSources = dataSources;
        this.data = polyData;
        const polygonColor = this.getPolygonColor(settings);
        this.leafletPoly = L.polygon(polyData[this.settings.polygonKeyName], {
          fill: true,
          fillColor: polygonColor,
          color: settings.polygonStrokeColor,
          weight: settings.polygonStrokeWeight,
          fillOpacity: settings.polygonOpacity,
          opacity: settings.polygonStrokeOpacity
        }).addTo(this.map);
        if (settings.editablePolygon) {
            this.leafletPoly.enableEdit(this.map);
            if (onDragendListener) {
                this.leafletPoly.on('editable:vertex:dragend', e => onDragendListener(e, this.data));
                this.leafletPoly.on('editable:vertex:deleted', e => onDragendListener(e, this.data));
            }
        }


        if (settings.showPolygonTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, settings, polyData.$datasource);
            this.updateTooltip(polyData);
        }
        if (settings.polygonClick) {
            this.leafletPoly.on('click', (event: LeafletMouseEvent) => {
                for (const action in this.settings.polygonClick) {
                    if (typeof (this.settings.polygonClick[action]) === 'function') {
                        this.settings.polygonClick[action](event.originalEvent, polyData.datasource);
                    }
                }
            });
        }
    }

    updateTooltip(data: FormattedData) {
        const pattern = this.settings.usePolygonTooltipFunction ?
            safeExecute(this.settings.polygonTooltipFunction, [this.data, this.dataSources, this.data.dsIndex]) :
            this.settings.polygonTooltipPattern;
        this.tooltip.setContent(parseWithTranslation.parseTemplate(pattern, data, true));
    }

    updatePolygon(data: FormattedData, dataSources: FormattedData[], settings: PolygonSettings) {
        this.data = data;
        this.dataSources = dataSources;
        if (settings.editablePolygon) {
          this.leafletPoly.disableEdit();
        }
        this.leafletPoly.setLatLngs(data[this.settings.polygonKeyName]);
      if (settings.editablePolygon) {
        this.leafletPoly.enableEdit(this.map);
      }
        if (settings.showPolygonTooltip)
            this.updateTooltip(this.data);
        this.updatePolygonColor(settings);
    }

    removePolygon() {
        this.map.removeLayer(this.leafletPoly);
    }

    updatePolygonColor(settings: PolygonSettings) {
        const polygonColor = this.getPolygonColor(settings);
        const style: L.PathOptions = {
            fill: true,
            fillColor: polygonColor,
            color: settings.polygonStrokeColor,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity
        };
        this.leafletPoly.setStyle(style);
    }

    getPolygonLatLngs() {
        return this.leafletPoly.getLatLngs();
    }

    setPolygonLatLngs(latLngs: LatLngExpression[]) {
        this.leafletPoly.setLatLngs(latLngs);
        this.leafletPoly.redraw();
    }

    private getPolygonColor(settings: PolygonSettings): string | null {
      return functionValueCalculator(settings.usePolygonColorFunction, settings.polygonColorFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.polygonColor);
    }
}
