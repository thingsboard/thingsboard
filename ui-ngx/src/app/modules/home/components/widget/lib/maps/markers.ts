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
import { MarkerSettings, FormattedData } from './map-models';
import { aspectCache, safeExecute, parseTemplate } from '@app/core/utils';
import { createTooltip } from './maps-utils';

export class Marker {

    leafletMarker: L.Marker;
    tooltipOffset: [number, number];
    tooltip: L.Popup;
    location: L.LatLngExpression;
    data: FormattedData;
    dataSources: FormattedData[];

    constructor(private map: L.Map, location: L.LatLngExpression, public settings: MarkerSettings,
        data?, dataSources?, onClickListener?, onDragendListener?) {
        this.setDataSources(data, dataSources);
        this.leafletMarker = L.marker(location, {
            draggable: settings.draggableMarker
        });

        this.createMarkerIcon((iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
            this.tooltipOffset = [0, -iconInfo.size[1] + 10];
            this.updateMarkerLabel(settings);
            this.leafletMarker.addTo(map);
        });

        if (settings.showTooltip) {
            this.tooltip = createTooltip(this.leafletMarker, settings);
            this.tooltip.setContent(parseTemplate(this.settings.tooltipPattern, data));
        }

        if (onClickListener) {
            this.leafletMarker.on('click', onClickListener);
        }

        if (onDragendListener) {
            this.leafletMarker.on('dragend', (e) => onDragendListener(e, this.data));
        }
    }

    setDataSources(data: FormattedData, dataSources: FormattedData[]) {
        this.data = data;
        this.dataSources = dataSources;
    }

    updateMarkerTooltip(data: FormattedData) {
        this.tooltip.setContent(parseTemplate(this.settings.tooltipPattern, data));
    }

    updateMarkerPosition(position: L.LatLngExpression) {
        this.leafletMarker.setLatLng(position);
    }

    updateMarkerLabel(settings: MarkerSettings) {
        this.leafletMarker.unbindTooltip();

        if (settings.showLabel) {
            if (settings.useLabelFunction) {
                settings.labelText = parseTemplate(
                    safeExecute(settings.labelFunction, [this.data, this.dataSources, this.data.dsIndex]), this.data)
            }
            else settings.labelText = parseTemplate(settings.label, this.data);
            this.leafletMarker.bindTooltip(`<div style="color: ${settings.labelColor};"><b>${settings.labelText}</b></div>`,
                { className: 'tb-marker-label', permanent: true, direction: 'top', offset: this.tooltipOffset });
        }
    }

    updateMarkerColor(color) {
        this.createDefaultMarkerIcon(color, (iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
        });
    }

    updateMarkerIcon(settings: MarkerSettings) {
        this.createMarkerIcon((iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
            this.tooltipOffset = [0, -iconInfo.size[1] + 10];
            this.updateMarkerLabel(settings);
        });
    }

    createMarkerIcon(onMarkerIconReady) {

        if (this.settings.icon) {
            onMarkerIconReady({
                size: [30, 30],
                icon: this.settings.icon,
            });
            return;
        }

        const currentImage = this.settings.useMarkerImageFunction ?
            safeExecute(this.settings.markerImageFunction,
                [this.data, this.settings.markerImages, this.dataSources, this.data.dsIndex]) : this.settings.currentImage;


        if (currentImage && currentImage.url) {
            aspectCache(currentImage.url).subscribe(
                (aspect) => {
                    if (aspect) {
                        let width;
                        let height;
                        if (aspect > 1) {
                            width = currentImage.size;
                            height = currentImage.size / aspect;
                        } else {
                            width = currentImage.size * aspect;
                            height = currentImage.size;
                        }
                        const icon = L.icon({
                            iconUrl: currentImage.url,
                            iconSize: [width, height],
                            iconAnchor: [width / 2, height],
                            popupAnchor: [0, -height]
                        });
                        const iconInfo = {
                            size: [width, height],
                            icon
                        };
                        onMarkerIconReady(iconInfo);
                    } else {
                        this.createDefaultMarkerIcon(this.settings.color, onMarkerIconReady);
                    }
                }
            );
        } else {
            this.createDefaultMarkerIcon(this.settings.color, onMarkerIconReady);
        }
    }

    createDefaultMarkerIcon(color, onMarkerIconReady) {
        const pinColor = color.substr(1);
        const icon = L.icon({
            iconUrl: 'https://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|' + pinColor,
            iconSize: [21, 34],
            iconAnchor: [10, 34],
            popupAnchor: [0, -34],
            shadowUrl: 'https://chart.apis.google.com/chart?chst=d_map_pin_shadow',
            shadowSize: [40, 37],
            shadowAnchor: [12, 35]
        });
        const iconInfo = {
            size: [21, 34],
            icon
        };
        onMarkerIconReady(iconInfo);
    }



    removeMarker() {
        /*     this.map$.subscribe(map =>
                 this.leafletMarker.addTo(map))*/
    }

    extendBoundsWithMarker(bounds) {
        bounds.extend(this.leafletMarker.getLatLng());
    }

    getMarkerPosition() {
        return this.leafletMarker.getLatLng();
    }

    setMarkerPosition(latLng) {
        this.leafletMarker.setLatLng(latLng);
    }
}