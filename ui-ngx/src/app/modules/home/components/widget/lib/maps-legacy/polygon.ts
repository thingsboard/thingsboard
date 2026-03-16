///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { createTooltip, isCutPolygon } from './maps-utils';
import { functionValueCalculator, parseWithTranslation } from './common-maps-utils';
import { WidgetPolygonSettings } from './map-models';
import { FormattedData } from '@shared/models/widget.models';
import { fillDataPattern, processDataPattern, safeExecuteTbFunction } from '@core/utils';
import LeafletMap from '@home/components/widget/lib/maps-legacy/leaflet-map';

export class Polygon {

    private editing = false;

    leafletPoly: L.Polygon;
    tooltip: L.Popup;

    constructor(private map: LeafletMap,
                private data: FormattedData,
                private dataSources: FormattedData[],
                private settings: Partial<WidgetPolygonSettings>,
                private onDragendListener?,
                snappable = false) {
        const polygonColor = this.getPolygonColor(settings);
        const polygonStrokeColor = this.getPolygonStrokeColor(settings);
        const polyData = data[this.settings.polygonKeyName];
        const polyConstructor = isCutPolygon(polyData) || polyData.length !== 2 ? L.polygon : L.rectangle;
        this.leafletPoly = polyConstructor(polyData, {
          fill: true,
          fillColor: polygonColor,
          color: polygonStrokeColor,
          weight: settings.polygonStrokeWeight,
          fillOpacity: settings.polygonOpacity,
          opacity: settings.polygonStrokeOpacity,
          pmIgnore: !settings.editablePolygon,
          snapIgnore: !snappable
        }).addTo(this.map.map);

        if (settings.showPolygonLabel) {
          this.updateLabel(settings);
        }

        if (settings.showPolygonTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, settings, data.$datasource,
              settings.autoClosePolygonTooltip, settings.showPolygonTooltipAction);
            this.updateTooltip(data);
        }
        this.createEventListeners();
    }

    private createEventListeners() {
      if (this.settings.editablePolygon && this.onDragendListener) {
        // Change position (call in drag drop mode)
        this.leafletPoly.on('pm:dragstart', () => this.editing = true);
        this.leafletPoly.on('pm:dragend', () => this.editing = false);
        // Rotate (call in rotate mode)
        this.leafletPoly.on('pm:rotatestart', () => this.editing = true);
        this.leafletPoly.on('pm:rotateend', () => this.editing = false);
        // Change size/point (call in edit mode)
        this.leafletPoly.on('pm:markerdragstart', () => this.editing = true);
        this.leafletPoly.on('pm:markerdragend', () => this.editing = false);
        this.leafletPoly.on('pm:edit', (e) => this.onDragendListener(e, this.data));
      }

      if (this.settings.polygonClick) {
        this.leafletPoly.on('click', (event: LeafletMouseEvent) => {
          for (const action in this.settings.polygonClick) {
            if (typeof (this.settings.polygonClick[action]) === 'function') {
              this.settings.polygonClick[action](event.originalEvent, this.data.$datasource);
            }
          }
        });
      }
    }

    updateTooltip(data: FormattedData) {
        const pattern = this.settings.usePolygonTooltipFunction ?
          safeExecuteTbFunction(this.settings.parsedPolygonTooltipFunction, [this.data, this.dataSources, this.data.dsIndex]) :
            this.settings.polygonTooltipPattern;
        this.tooltip.setContent(parseWithTranslation.parseTemplate(pattern, data, true));
    }

    updateLabel(settings: Partial<WidgetPolygonSettings>) {
        this.leafletPoly.unbindTooltip();
        if (settings.showPolygonLabel) {
            if (!this.map.polygonLabelText || settings.usePolygonLabelFunction) {
                const pattern = settings.usePolygonLabelFunction ?
                  safeExecuteTbFunction(settings.parsedPolygonLabelFunction,
                    [this.data, this.dataSources, this.data.dsIndex]) : settings.polygonLabel;
                this.map.polygonLabelText = parseWithTranslation.prepareProcessPattern(pattern, true);
                this.map.replaceInfoLabelPolygon = processDataPattern(this.map.polygonLabelText, this.data);
            }
            const polygonLabelText = fillDataPattern(this.map.polygonLabelText, this.map.replaceInfoLabelPolygon, this.data);
            const labelColor = this.map.ctx.widgetConfig.color;
            this.leafletPoly.bindTooltip(`<div style="color: ${labelColor};"><b>${polygonLabelText}</b></div>`,
              { className: 'tb-polygon-label', permanent: true, direction: 'center' })
              .openTooltip(this.leafletPoly.getBounds().getCenter());
        }
    }

    updatePolygon(data: FormattedData, dataSources: FormattedData[], settings: Partial<WidgetPolygonSettings>) {
      if (this.editing) {
        return;
      }
      this.data = data;
      this.dataSources = dataSources;
      const polyData = data[this.settings.polygonKeyName];
      if (isCutPolygon(polyData) || polyData.length !== 2) {
        if (this.leafletPoly instanceof L.Rectangle) {
          this.map.map.removeLayer(this.leafletPoly);
          const polygonColor = this.getPolygonColor(settings);
          const polygonStrokeColor = this.getPolygonStrokeColor(settings);
          this.leafletPoly = L.polygon(polyData, {
            fill: true,
            fillColor: polygonColor,
            color: polygonStrokeColor,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity,
            pmIgnore: !settings.editablePolygon
          }).addTo(this.map.map);
          if (settings.showPolygonTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, settings, data.$datasource,
              settings.autoClosePolygonTooltip, settings.showPolygonTooltipAction);
          }
          this.createEventListeners();
        } else {
          this.leafletPoly.setLatLngs(polyData);
        }
      } else if (polyData.length === 2) {
        const bounds = new L.LatLngBounds(polyData);
        // @ts-ignore
        this.leafletPoly.setBounds(bounds);
      }
      if (settings.showPolygonTooltip) {
        this.updateTooltip(this.data);
      }
      if (settings.showPolygonLabel) {
        this.updateLabel(settings);
      }
      this.updatePolygonColor(settings);
    }

    removePolygon() {
        this.map.map.removeLayer(this.leafletPoly);
    }

    updatePolygonColor(settings: Partial<WidgetPolygonSettings>) {
        const polygonColor = this.getPolygonColor(settings);
        const polygonStrokeColor = this.getPolygonStrokeColor(settings);
        const style: L.PathOptions = {
            fillColor: polygonColor,
            color: polygonStrokeColor
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

    private getPolygonColor(settings: Partial<WidgetPolygonSettings>): string | null {
      return functionValueCalculator(settings.usePolygonColorFunction, settings.parsedPolygonColorFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.polygonColor);
    }

  private getPolygonStrokeColor(settings: Partial<WidgetPolygonSettings>): string | null {
    return functionValueCalculator(settings.usePolygonStrokeColorFunction, settings.parsedPolygonStrokeColorFunction,
      [this.data, this.dataSources, this.data.dsIndex], settings.polygonStrokeColor);
  }
}
