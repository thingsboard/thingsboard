/*
 * Copyright © 2016-2017 The Thingsboard Authors
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

import 'leaflet/dist/leaflet.css';
import L from 'leaflet/dist/leaflet';

export default class TbOpenStreetMap {

    constructor($containerElement, initCallback, defaultZoomLevel, dontFitMapBounds, minZoomLevel) {

        this.defaultZoomLevel = defaultZoomLevel;
        this.dontFitMapBounds = dontFitMapBounds;
        this.minZoomLevel = minZoomLevel;
        this.tooltips = [];

        this.map = L.map($containerElement[0]).setView([0, 0], this.defaultZoomLevel || 8);

        L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(this.map);

        if (initCallback) {
            setTimeout(initCallback, 0); //eslint-disable-line
        }

    }

    inited() {
        return angular.isDefined(this.map);
    }

    updateMarkerColor(marker, color) {
        var pinColor = color.substr(1);
        var icon = L.icon({
            iconUrl: 'http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|' + pinColor,
            iconSize: [21, 34],
            iconAnchor: [10, 34],
            popupAnchor: [0, -34],
            shadowUrl: 'http://chart.apis.google.com/chart?chst=d_map_pin_shadow',
            shadowSize: [40, 37],
            shadowAnchor: [12, 35]
        });
        marker.setIcon(icon);
    }

    updateMarkerImage(marker, settings, image, maxSize) {
        var testImage = new Image(); // eslint-disable-line no-undef
        testImage.onload = function() {
            var width;
            var height;
            var aspect = testImage.width / testImage.height;
            if (aspect > 1) {
                width = maxSize;
                height = maxSize / aspect;
            } else {
                width = maxSize * aspect;
                height = maxSize;
            }
            var icon = L.icon({
                iconUrl: image,
                iconSize: [width, height],
                iconAnchor: [width/2, height],
                popupAnchor: [0, -height]
            });
            marker.setIcon(icon);
            if (settings.showLabel) {
                marker.unbindTooltip();
                marker.bindTooltip('<div style="color: '+ settings.labelColor +';"><b>'+settings.label+'</b></div>',
                    { className: 'tb-marker-label', permanent: true, direction: 'top', offset: [0, -height + 10] });
            }
        }
        testImage.src = image;
    }

    createMarker(location, settings, onClickListener) {
        var height = 34;
        var pinColor = settings.color.substr(1);
        var icon = L.icon({
            iconUrl: 'http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|' + pinColor,
            iconSize: [21, 34],
            iconAnchor: [10, 34],
            popupAnchor: [0, -34],
            shadowUrl: 'http://chart.apis.google.com/chart?chst=d_map_pin_shadow',
            shadowSize: [40, 37],
            shadowAnchor: [12, 35]
        });

        var marker = L.marker(location, {icon: icon}).addTo(this.map);

        if (settings.showLabel) {
            marker.bindTooltip('<div style="color: '+ settings.labelColor +';"><b>'+settings.label+'</b></div>',
                { className: 'tb-marker-label', permanent: true, direction: 'top', offset: [0, -height + 10] });
        }

        if (settings.useMarkerImage) {
            this.updateMarkerImage(marker, settings, settings.markerImage, settings.markerImageSize || 34);
        }

        if (settings.displayTooltip) {
            this.createTooltip(marker, settings.tooltipPattern, settings.tooltipReplaceInfo);
        }

        if (onClickListener) {
            marker.on('click', onClickListener);
        }

        return marker;
    }

    removeMarker(marker) {
        this.map.removeLayer(marker);
    }

    createTooltip(marker, pattern, replaceInfo) {
        var popup = L.popup();
        popup.setContent('');
        marker.bindPopup(popup, {autoClose: false, closeOnClick: false});
        this.tooltips.push( {
            popup: popup,
            pattern: pattern,
            replaceInfo: replaceInfo
        });
    }

    updatePolylineColor(polyline, settings, color) {
        var style = {
            color: color,
            opacity: settings.strokeOpacity,
            weight: settings.strokeWeight
        };
        polyline.setStyle(style);
    }

    createPolyline(locations, settings) {
        var polyline = L.polyline(locations,
            {
                color: settings.color,
                opacity: settings.strokeOpacity,
                weight: settings.strokeWeight
            }
        ).addTo(this.map);
        return polyline;
    }

    removePolyline(polyline) {
        this.map.removeLayer(polyline);
    }

    fitBounds(bounds) {
        if (bounds.isValid()) {
            if (this.dontFitMapBounds && this.defaultZoomLevel) {
                this.map.setZoom(this.defaultZoomLevel, {animate: false});
                this.map.panTo(bounds.getCenter(), {animate: false});
            } else {
                var tbMap = this;
                this.map.once('zoomend', function() {
                    if (!tbMap.defaultZoomLevel && tbMap.map.getZoom() > tbMap.minZoomLevel) {
                        tbMap.map.setZoom(tbMap.minZoomLevel, {animate: false});
                    }
                });
                this.map.fitBounds(bounds, {padding: [50, 50], animate: false});
            }
        }
    }

    createLatLng(lat, lng) {
        return L.latLng(lat, lng);
    }

    extendBoundsWithMarker(bounds, marker) {
        bounds.extend(marker.getLatLng());
    }

    getMarkerPosition(marker) {
        return marker.getLatLng();
    }

    setMarkerPosition(marker, latLng) {
        marker.setLatLng(latLng);
    }

    getPolylineLatLngs(polyline) {
        return polyline.getLatLngs();
    }

    setPolylineLatLngs(polyline, latLngs) {
        polyline.setLatLngs(latLngs);
    }

    createBounds() {
        return L.latLngBounds();
    }

    extendBounds(bounds, polyline) {
        if (polyline && polyline.getLatLngs()) {
            bounds.extend(polyline.getBounds());
        }
    }

    invalidateSize() {
        this.map.invalidateSize(true);
    }

    getTooltips() {
        return this.tooltips;
    }

}
