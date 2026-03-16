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

import L, { LeafletMouseEvent } from 'leaflet';
import { MarkerIconInfo, MarkerIconReadyFunction, MarkerImageInfo, WidgetMarkersSettings, } from './map-models';
import { bindPopupActions, createTooltip } from './maps-utils';
import { loadImageWithAspect, parseWithTranslation } from './common-maps-utils';
import tinycolor from 'tinycolor2';
import {
  fillDataPattern,
  isDefined,
  isDefinedAndNotNull,
  processDataPattern,
  safeExecuteTbFunction
} from '@core/utils';
import LeafletMap from './leaflet-map';
import { FormattedData } from '@shared/models/widget.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { ReplaySubject } from 'rxjs';

export class Marker {

    private editing = false;

    leafletMarker: L.Marker;
    labelOffset: L.PointTuple;
    tooltipOffset: L.LatLngTuple;
    markerOffset: L.LatLngTuple;
    tooltip: L.Popup;
    createMarkerIconSubject = new ReplaySubject<MarkerIconInfo>();

  constructor(private map: LeafletMap,
              private location: L.LatLng,
              private settings: Partial<WidgetMarkersSettings>,
              private data?: FormattedData,
              private dataSources?: FormattedData[],
              private onDragendListener?,
              snappable = false) {
        this.leafletMarker = L.marker(this.location, {
          pmIgnore: !settings.draggableMarker,
          snapIgnore: !snappable,
          tbMarkerData: this.data as any
        });

        this.markerOffset = [
          isDefined(settings.markerOffsetX) ? settings.markerOffsetX : 0.5,
          isDefined(settings.markerOffsetY) ? settings.markerOffsetY : 1,
        ];

        this.tooltipOffset = [
          isDefined(settings.tooltipOffsetX) ? settings.tooltipOffsetX : 0,
          isDefined(settings.tooltipOffsetY) ? settings.tooltipOffsetY : -1,
        ];

        this.updateMarkerIcon(this.settings);

        if (settings.showTooltip) {
            this.tooltip = createTooltip(this.leafletMarker, settings, data.$datasource,
              settings.autocloseTooltip, settings.showTooltipAction);
            this.updateMarkerTooltip(data);
        }

        if (this.settings.markerClick) {
            this.leafletMarker.on('click', (event: LeafletMouseEvent) => {
              for (const action in this.settings.markerClick) {
                if (typeof (this.settings.markerClick[action]) === 'function') {
                  this.settings.markerClick[action](event.originalEvent, this.data.$datasource);
                }
              }
            });
        }

        if (settings.draggableMarker && onDragendListener) {
          this.leafletMarker.on('pm:dragstart', () => {
            (this.leafletMarker.dragging as any)._draggable = { _moved: true };
            (this.leafletMarker.dragging as any)._enabled = true;
            this.editing = true;
          });
          this.leafletMarker.on('pm:dragend', (e) => {
            onDragendListener(e, this.data);
            delete (this.leafletMarker.dragging as any)._draggable;
            delete (this.leafletMarker.dragging as any)._enabled;
            this.editing = false;
          });
        }
    }

    setDataSources(data: FormattedData, dataSources: FormattedData[]) {
      this.data = data;
      this.dataSources = dataSources;
      this.leafletMarker.options.tbMarkerData = data as any;
    }

    updateMarkerTooltip(data: FormattedData) {
      if (!this.map.markerTooltipText || this.settings.useTooltipFunction) {
        const pattern = this.settings.useTooltipFunction ?
          safeExecuteTbFunction(this.settings.parsedTooltipFunction, [this.data, this.dataSources, this.data.dsIndex]) : this.settings.tooltipPattern;
        this.map.markerTooltipText = parseWithTranslation.prepareProcessPattern(pattern, true);
        this.map.replaceInfoTooltipMarker = processDataPattern(this.map.markerTooltipText, data);
      }
      this.tooltip.setContent(fillDataPattern(this.map.markerTooltipText, this.map.replaceInfoTooltipMarker, data));
      if (this.tooltip.isOpen() && this.tooltip.getElement()) {
        bindPopupActions(this.tooltip, this.settings, data.$datasource);
      }
    }

    updateMarkerPosition(position: L.LatLng) {
      if (!this.leafletMarker.getLatLng().equals(position) && !this.editing) {
        this.location = position;
        this.leafletMarker.setLatLng(position);
      }
    }

    updateMarkerLabel(settings: Partial<WidgetMarkersSettings>) {
        this.leafletMarker.unbindTooltip();
        if (settings.showLabel) {
            if (!this.map.markerLabelText || settings.useLabelFunction) {
              const pattern = settings.useLabelFunction ?
                safeExecuteTbFunction(settings.parsedLabelFunction, [this.data, this.dataSources, this.data.dsIndex]) : settings.label;
              this.map.markerLabelText = parseWithTranslation.prepareProcessPattern(pattern, true);
              this.map.replaceInfoLabelMarker = processDataPattern(this.map.markerLabelText, this.data);
            }
            const labelText = fillDataPattern(this.map.markerLabelText, this.map.replaceInfoLabelMarker, this.data);
            const labelColor = this.map.ctx.widgetConfig.color;
            this.leafletMarker.bindTooltip(`<div style="color: ${labelColor};"><b>${labelText}</b></div>`,
                { className: 'tb-marker-label', permanent: true, direction: 'top', offset: this.labelOffset });
        }
    }

    updateMarkerColor(color: tinycolor.Instance) {
        this.createDefaultMarkerIcon(color, (iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
        });
    }

    updateMarkerIcon(settings: Partial<WidgetMarkersSettings>) {
        this.createMarkerIcon((iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
            const anchor = iconInfo.icon.options.iconAnchor;
            if (anchor && Array.isArray(anchor)) {
                this.labelOffset = [iconInfo.size[0] / 2 - anchor[0], 10 - anchor[1]];
            } else {
                this.labelOffset = [0, -iconInfo.size[1] * this.markerOffset[1] + 10];
            }
            this.updateMarkerLabel(settings);
            this.createMarkerIconSubject.next(iconInfo);
        });
    }

    private createMarkerIcon(onMarkerIconReady: MarkerIconReadyFunction) {
        if (this.settings.icon) {
          onMarkerIconReady(this.settings.icon);
          return;
        } else if (this.settings.icon$) {
          this.settings.icon$.subscribe((res) => {
            onMarkerIconReady(res);
          });
          return;
        }
        const currentImage: MarkerImageInfo = this.settings.useMarkerImageFunction ?
          safeExecuteTbFunction(this.settings.parsedMarkerImageFunction,
                [this.data, this.settings.markerImages, this.dataSources, this.data.dsIndex]) : this.settings.currentImage;
        let currentColor = this.settings.tinyColor;
        if (this.settings.useColorFunction) {
          const functionColor = safeExecuteTbFunction(this.settings.parsedColorFunction,
            [this.data, this.dataSources, this.data.dsIndex]);
          if (isDefinedAndNotNull(functionColor)) {
            currentColor = tinycolor(functionColor);
          }
        }
        if (currentImage && currentImage.url) {
          loadImageWithAspect(this.map.ctx.$injector.get(ImagePipe), currentImage.url).subscribe(
                (aspectImage) => {
                    if (aspectImage?.aspect) {
                        let width: number;
                        let height: number;
                        if (aspectImage.aspect > 1) {
                            width = currentImage.size;
                            height = currentImage.size / aspectImage.aspect;
                        } else {
                            width = currentImage.size * aspectImage.aspect;
                            height = currentImage.size;
                        }
                        let iconAnchor = currentImage.markerOffset;
                        let popupAnchor = currentImage.tooltipOffset;
                        if (!iconAnchor) {
                            iconAnchor = [width * this.markerOffset[0], height * this.markerOffset[1]];
                        }
                        if (!popupAnchor) {
                            popupAnchor = [width * this.tooltipOffset[0], height * this.tooltipOffset[1]];
                        }
                        const icon = L.icon({
                            iconUrl: aspectImage.url,
                            iconSize: [width, height],
                            iconAnchor,
                            popupAnchor
                        });
                        const iconInfo: MarkerIconInfo = {
                            size: [width, height],
                            icon
                        };
                        onMarkerIconReady(iconInfo);
                    } else {
                        this.createDefaultMarkerIcon(currentColor, onMarkerIconReady);
                    }
                }
            );
        } else {
            this.createDefaultMarkerIcon(currentColor, onMarkerIconReady);
        }
    }

    createDefaultMarkerIcon(color: tinycolor.Instance, onMarkerIconReady: MarkerIconReadyFunction) {
      let icon: MarkerIconInfo;
      if (!tinycolor.equals(color, this.settings.tinyColor)) {
        icon = this.createColoredMarkerIcon(color);
      } else {
        if (!this.map.defaultMarkerIconInfo) {
          this.map.defaultMarkerIconInfo = this.createColoredMarkerIcon(color);
        }
        icon = this.map.defaultMarkerIconInfo;
      }
      onMarkerIconReady(icon);
    }

    createColoredMarkerIcon(color: tinycolor.Instance): MarkerIconInfo {
      return {
        size: [21, 34],
        icon: L.icon({
          iconUrl: this.createColorIconURI(color),
          iconSize: [21, 34],
          iconAnchor: [21 * this.markerOffset[0], 34 * this.markerOffset[1]],
          popupAnchor: [0, -34],
          shadowUrl: 'assets/shadow.png',
          shadowSize: [40, 37],
          shadowAnchor: [12, 35]
        })
      };
    }

    createColorIconURI(color: tinycolor.Instance): string {
      const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="-191.35 -351.18 1083.58 1730.46">` +
                         `<path fill-rule="evenodd" clip-rule="evenodd" fill="#${color.toHex()}" stroke="#000" stroke-width="37" ` +
                         `stroke-miterlimit="10" d="M351.833 1360.78c-38.766-190.3-107.116-348.665-189.903-495.44C100.523 756.469 ` +
        `29.386 655.978-36.434 550.404c-21.972-35.244-40.934-72.477-62.047-109.054-42.216-73.137-76.444-157.935-74.269-267.932 ` +
        `2.125-107.473 33.208-193.685 78.03-264.173C-21-206.69 102.481-301.745 268.164-326.724c135.466-20.425 262.475 14.082 ` +
        `352.543 66.747 73.6 43.038 130.596 100.528 173.92 168.28 45.22 70.716 76.36 154.26 78.971 263.233 1.337 55.83-7.805 ` +
        `107.532-20.684 150.417-13.034 43.41-33.996 79.695-52.646 118.455-36.406 75.659-82.049 144.981-127.855 214.345-136.437 ` +
        `206.606-264.496 417.31-320.58 706.028z"/><circle fill-rule="evenodd" ` +
        `clip-rule="evenodd" cx="352.891" cy="225.779" r="183.332"/></svg>`;
      return 'data:image/svg+xml;base64,' + btoa(svg);
    }

    removeMarker() {
        /*     this.map$.subscribe(map =>
                 this.leafletMarker.addTo(map))*/
    }

    extendBoundsWithMarker(bounds: L.LatLngBounds) {
        bounds.extend(this.leafletMarker.getLatLng());
    }

    getMarkerPosition() {
        return this.leafletMarker.getLatLng();
    }

    setMarkerPosition(latLng: L.LatLngExpression) {
        this.leafletMarker.setLatLng(latLng);
    }
}
