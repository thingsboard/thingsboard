import L from 'leaflet';
import { createTooltip } from './maps-utils';
import { MarkerSettings } from './map-models';
import { Observable } from 'rxjs';
import { aspectCache } from '@app/core/utils';

export class Marker {

    leafletMarker: L.Marker;
    // map: L.Map;

    tooltipOffset;
    tooltip;

    constructor(private map: L.Map, location: L.LatLngExpression, settings: MarkerSettings, onClickListener?, markerArgs?, onDragendListener?) {
        //this.map = map;
        this.leafletMarker = L.marker(location, {
            draggable: settings.draggable
        });

        this.createMarkerIcon(settings, (iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
            if (settings.showLabel) {
                this.tooltipOffset = [0, -iconInfo.size[1] + 10];
                this.updateMarkerLabel(settings)
            }

            this.leafletMarker.addTo(map)
        }
        );

        if (settings.displayTooltip) {
            this.tooltip = createTooltip(this.leafletMarker, settings, markerArgs);
        }

        if (onClickListener) {
            this.leafletMarker.on('click', onClickListener);
        }

        if (onDragendListener) {
            this.leafletMarker.on('dragend', onDragendListener);
        }

    }

    updateMarkerLabel(settings) {
        this.leafletMarker.unbindTooltip();
        if (settings.showLabel)
            this.leafletMarker.bindTooltip(`<div style="color: ${settings.labelColor};"><b>${settings.labelText}</b></div>`,
                { className: 'tb-marker-label', permanent: true, direction: 'top', offset: this.tooltipOffset });
    }

    updateMarkerColor(color) {
        this.createDefaultMarkerIcon(color, (iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
        });
    }

    updateMarkerIcon( settings) {
        this.createMarkerIcon(settings, (iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
            if (settings.showLabel) {
                this.tooltipOffset = [0, -iconInfo.size[1] + 10];
                this.updateMarkerLabel(settings)
            }
        });
    }



    createMarkerIcon(settings, onMarkerIconReady) {
        var currentImage = settings.currentImage;
        // var opMap = this;
        if (currentImage && currentImage.url) {
            aspectCache(currentImage.url).subscribe(
                (aspect) => {
                    if (aspect) {
                        var width;
                        var height;
                        if (aspect > 1) {
                            width = currentImage.size;
                            height = currentImage.size / aspect;
                        } else {
                            width = currentImage.size * aspect;
                            height = currentImage.size;
                        }
                        var icon = L.icon({
                            iconUrl: currentImage.url,
                            iconSize: [width, height],
                            iconAnchor: [width / 2, height],
                            popupAnchor: [0, -height]
                        });
                        var iconInfo = {
                            size: [width, height],
                            icon: icon
                        };
                        onMarkerIconReady(iconInfo);
                    } else {
                        this.createDefaultMarkerIcon(settings.color, onMarkerIconReady);
                    }
                }
            );
        } else {
            this.createDefaultMarkerIcon(settings.color, onMarkerIconReady);
        }
    }

    createDefaultMarkerIcon(color, onMarkerIconReady) {
        var pinColor = color.substr(1);
        var icon = L.icon({
            iconUrl: 'https://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|' + pinColor,
            iconSize: [21, 34],
            iconAnchor: [10, 34],
            popupAnchor: [0, -34],
            shadowUrl: 'https://chart.apis.google.com/chart?chst=d_map_pin_shadow',
            shadowSize: [40, 37],
            shadowAnchor: [12, 35]
        });
        var iconInfo = {
            size: [21, 34],
            icon: icon
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