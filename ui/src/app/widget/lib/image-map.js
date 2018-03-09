/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
import * as L from 'leaflet';

const maxZoom = 4;

export default class TbImageMap {

    constructor(ctx, $containerElement, utils, initCallback, imageUrl, posFunction, imageEntityAlias, imageUrlAttribute) {

        this.ctx = ctx;
        this.utils = utils;
        this.tooltips = [];

        this.$containerElement = $containerElement;
        this.$containerElement.css('background', '#fff');

        this.aspect = 0;
        this.width = 0;
        this.height = 0;
        this.markers = [];
        this.initCallback = initCallback;

        if (angular.isDefined(posFunction) && posFunction.length > 0) {
            try {
                this.posFunction = new Function('origXPos, origYPos', posFunction);
            } catch (e) {
                this.posFunction = null;
            }
        }
        if (!this.posFunction) {
            this.posFunction = (origXPos, origYPos) => {return {x: origXPos, y: origYPos}};
        }

        if (!this.subscribeForImageAttribute(imageEntityAlias, imageUrlAttribute)) {
            this.loadImage(imageUrl, initCallback);
        }
    }

    subscribeForImageAttribute(imageEntityAlias, imageUrlAttribute) {
        if (!imageEntityAlias || !imageEntityAlias.length ||
            !imageUrlAttribute || !imageUrlAttribute.length) {
            return false;
        }
        var entityAliasId = this.ctx.aliasController.getEntityAliasId(imageEntityAlias);
        if (!entityAliasId) {
            return false;
        }
        var types = this.ctx.$scope.$injector.get('types');
        var datasources = [
            {
                type: types.datasourceType.entity,
                name: imageEntityAlias,
                aliasName: imageEntityAlias,
                entityAliasId: entityAliasId,
                dataKeys: [
                    {
                        type: types.dataKeyType.attribute,
                        name: imageUrlAttribute,
                        label: imageUrlAttribute,
                        settings: {},
                        _hash: Math.random()
                    }
                ]
            }
        ];
        var imageMap = this;
        var imageUrlSubscriptionOptions = {
            datasources: datasources,
            useDashboardTimewindow: false,
            type: types.widgetType.latest.value,
            callbacks: {
                onDataUpdated: (subscription, apply) => {imageMap.imageUrlDataUpdated(subscription, apply)}
            }
        };
        this.ctx.subscriptionApi.createSubscription(imageUrlSubscriptionOptions, true).then(
            (subscription) => {
                imageMap.imageUrlSubscription = subscription;
            }
        );
        return true;
    }

    imageUrlDataUpdated(subscription, apply) {
        var data = subscription.data;
        if (data.length) {
            var keyData = data[0];
            if (keyData && keyData.data && keyData.data[0]) {
                var attrValue = keyData.data[0][1];
                if (attrValue && attrValue.length) {
                    this.loadImage(attrValue, this.aspect > 0 ? null : this.initCallback, true);
                }
            }
        }
        if (apply) {
            this.ctx.$scope.$digest();
        }
    }

    loadImage(imageUrl, initCallback, updateImage) {
        if (!imageUrl) {
            imageUrl = 'data:image/svg+xml;base64,PHN2ZyBpZD0ic3ZnMiIgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTAwIiB3aWR0aD0iMTAwIiB2ZXJzaW9uPSIxLjEiIHhtbG5zOmNjPSJodHRwOi8vY3JlYXRpdmVjb21tb25zLm9yZy9ucyMiIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgdmlld0JveD0iMCAwIDEwMCAxMDAiPgogPGcgaWQ9ImxheWVyMSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAtOTUyLjM2KSI+CiAgPHJlY3QgaWQ9InJlY3Q0Njg0IiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBoZWlnaHQ9Ijk5LjAxIiB3aWR0aD0iOTkuMDEiIHN0cm9rZT0iIzAwMCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiB5PSI5NTIuODYiIHg9Ii40OTUwNSIgc3Ryb2tlLXdpZHRoPSIuOTkwMTAiIGZpbGw9IiNlZWUiLz4KICA8dGV4dCBpZD0idGV4dDQ2ODYiIHN0eWxlPSJ3b3JkLXNwYWNpbmc6MHB4O2xldHRlci1zcGFjaW5nOjBweDt0ZXh0LWFuY2hvcjptaWRkbGU7dGV4dC1hbGlnbjpjZW50ZXIiIGZvbnQtd2VpZ2h0PSJib2xkIiB4bWw6c3BhY2U9InByZXNlcnZlIiBmb250LXNpemU9IjEwcHgiIGxpbmUtaGVpZ2h0PSIxMjUlIiB5PSI5NzAuNzI4MDkiIHg9IjQ5LjM5NjQ3NyIgZm9udC1mYW1pbHk9IlJvYm90byIgZmlsbD0iIzY2NjY2NiI+PHRzcGFuIGlkPSJ0c3BhbjQ2OTAiIHg9IjUwLjY0NjQ3NyIgeT0iOTcwLjcyODA5Ij5JbWFnZSBiYWNrZ3JvdW5kIDwvdHNwYW4+PHRzcGFuIGlkPSJ0c3BhbjQ2OTIiIHg9IjQ5LjM5NjQ3NyIgeT0iOTgzLjIyODA5Ij5pcyBub3QgY29uZmlndXJlZDwvdHNwYW4+PC90ZXh0PgogIDxyZWN0IGlkPSJyZWN0NDY5NCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgaGVpZ2h0PSIxOS4zNiIgd2lkdGg9IjY5LjM2IiBzdHJva2U9IiMwMDAiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgeT0iOTkyLjY4IiB4PSIxNS4zMiIgc3Ryb2tlLXdpZHRoPSIuNjM5ODYiIGZpbGw9Im5vbmUiLz4KIDwvZz4KPC9zdmc+Cg==';
        }
        this.imageUrl = imageUrl;
        var imageMap = this;
        this.utils.loadImageAspect(imageUrl).then(
            (aspect) => {
                imageMap.aspect = aspect;
                imageMap.onresize(updateImage);
                if (initCallback) {
                    setTimeout(initCallback, 0); //eslint-disable-line
                }
            }
        );
    }

    onresize(updateImage) {
        if (this.aspect > 0) {
            var width = this.$containerElement.width();
            if (width > 0) {
                var height = width / this.aspect;
                var imageMapHeight = this.$containerElement.height();
                if (imageMapHeight > 0 && height > imageMapHeight) {
                    height = imageMapHeight;
                    width = height * this.aspect;
                }
                width *= maxZoom;
                var prevWidth = this.width;
                var prevHeight = this.height;
                if (this.width !== width) {
                    this.width = width;
                    this.height = width / this.aspect;
                    if (!this.map) {
                        this.initMap(updateImage);
                    } else {
                        var lastCenterPos = this.latLngToPoint(this.map.getCenter());
                        lastCenterPos.x /= prevWidth;
                        lastCenterPos.y /= prevHeight;
                        this.updateBounds(updateImage, lastCenterPos);
                        this.map.invalidateSize(true);
                        this.updateMarkers();
                    }
                }
            }
        }
    }

    initMap(updateImage) {
        if (!this.map && this.aspect > 0) {
            var center = this.pointToLatLng(this.width/2, this.height/2);
            this.map = L.map(this.$containerElement[0], {
                minZoom: 1,
                maxZoom: maxZoom,
                center: center,
                zoom: 1,
                crs: L.CRS.Simple,
                attributionControl: false
            });
            this.updateBounds(updateImage);
            this.updateMarkers();
        }
    }

    pointToLatLng(x, y) {
        return L.CRS.Simple.pointToLatLng({x:x, y:y}, maxZoom-1);
    }

    latLngToPoint(latLng) {
        return L.CRS.Simple.latLngToPoint(latLng, maxZoom-1);
    }

    inited() {
        return angular.isDefined(this.map);
    }

    updateBounds(updateImage, lastCenterPos) {
        var w = this.width;
        var h = this.height;
        var southWest = this.pointToLatLng(0, h);
        var northEast = this.pointToLatLng(w, 0);
        var bounds = new L.LatLngBounds(southWest, northEast);

        if (updateImage && this.imageOverlay) {
            this.imageOverlay.remove();
            this.imageOverlay = null;
        }

        if (this.imageOverlay) {
            this.imageOverlay.setBounds(bounds);
        } else {
            this.imageOverlay = L.imageOverlay(this.imageUrl, bounds).addTo(this.map);
        }
        var padding = 200 * maxZoom;
        southWest = this.pointToLatLng(-padding, h + padding);
        northEast = this.pointToLatLng(w+padding, -padding);
        var maxBounds = new L.LatLngBounds(southWest, northEast);
        this.map.setMaxBounds(maxBounds);
        if (lastCenterPos) {
            lastCenterPos.x *= w;
            lastCenterPos.y *= h;
            var center = this.pointToLatLng(lastCenterPos.x, lastCenterPos.y);
            this.ctx.$scope.$injector.get('$mdUtil').nextTick(() => {
                this.map.panTo(center, {animate: false});
            });
        }
    }

    updateMarkerLabel(marker, settings) {
        marker.unbindTooltip();
        marker.bindTooltip('<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>',
            { className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset });
    }

    updateMarkerColor(marker, color) {
        this.createDefaultMarkerIcon(marker, color, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
        });
    }

    updateMarkerIcon(marker, settings) {
        this.createMarkerIcon(marker, settings, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
            if (settings.showLabel) {
                marker.unbindTooltip();
                marker.tooltipOffset = [0, -iconInfo.size[1] * marker.offsetY + 10];
                marker.bindTooltip('<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>',
                    { className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset });
            }
        });
    }

    createMarkerIcon(marker, settings, onMarkerIconReady) {
        var currentImage = settings.currentImage;
        var opMap = this;
        if (currentImage && currentImage.url) {
            this.utils.loadImageAspect(currentImage.url).then(
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
                            iconAnchor: [marker.offsetX * width, marker.offsetY * height],
                            popupAnchor: [0, -height]
                        });
                        var iconInfo = {
                            size: [width, height],
                            icon: icon
                        };
                        onMarkerIconReady(iconInfo);
                    } else {
                        opMap.createDefaultMarkerIcon(marker, settings.color, onMarkerIconReady);
                    }
                }
            );
        } else {
            this.createDefaultMarkerIcon(marker, settings.color, onMarkerIconReady);
        }
    }

    createDefaultMarkerIcon(marker, color, onMarkerIconReady) {
        var pinColor = color.substr(1);
        var icon = L.icon({
            iconUrl: 'https://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|' + pinColor,
            iconSize: [21, 34],
            iconAnchor: [21 * marker.offsetX, 34 * marker.offsetY],
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

    createMarker(position, settings, onClickListener, markerArgs) {
        var pos = this.posFunction(position.x, position.y);
        var x = pos.x * this.width;
        var y = pos.y * this.height;
        var location = this.pointToLatLng(x, y);
        var marker = L.marker(location, {});//.addTo(this.map);
        marker.position = position;
        marker.offsetX = settings.markerOffsetX;
        marker.offsetY = settings.markerOffsetY;
        var opMap = this;
        this.createMarkerIcon(marker, settings, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
            if (settings.showLabel) {
                marker.tooltipOffset = [0, -iconInfo.size[1] * marker.offsetY + 10];
                marker.bindTooltip('<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>',
                    { className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset });
            }
            marker.addTo(opMap.map);
        });

        if (settings.displayTooltip) {
            this.createTooltip(marker, settings.tooltipPattern, settings.tooltipReplaceInfo, settings.autocloseTooltip, markerArgs);
        }

        if (onClickListener) {
            marker.on('click', onClickListener);
        }
        this.markers.push(marker);
        return marker;
    }

    updateMarkers() {
        this.markers.forEach((marker) => {
            this.updateMarkerLocation(marker);
        });
    }

    updateMarkerLocation(marker) {
        this.setMarkerPosition(marker, marker.position);
    }

    removeMarker(marker) {
        this.map.removeLayer(marker);
        var index = this.markers.indexOf(marker);
        if (index > -1) {
            marker.pinElement.remove();
            this.markers.splice(index, 1);
        }
    }

    createTooltip(marker, pattern, replaceInfo, autoClose, markerArgs) {
        var popup = L.popup();
        popup.setContent('');
        marker.bindPopup(popup, {autoClose: autoClose, closeOnClick: false});
        this.tooltips.push( {
            markerArgs: markerArgs,
            popup: popup,
            pattern: pattern,
            replaceInfo: replaceInfo
        });
    }

    updatePolylineColor(/*polyline, settings, color*/) {
    }

    createPolyline(/*locations, settings*/) {
    }

    removePolyline(/*polyline*/) {
    }

    fitBounds() {
    }

    createLatLng(x, y) {
        return new Position(x, y);
    }

    extendBoundsWithMarker() {
    }

    getMarkerPosition(marker) {
        return marker.position;
    }

    setMarkerPosition(marker, position) {
        marker.position = position;
        var pos = this.posFunction(position.x, position.y);
        var x = pos.x * this.width;
        var y = pos.y * this.height;
        var location = this.pointToLatLng(x, y);
        marker.setLatLng(location);
    }

    getPolylineLatLngs(/*polyline*/) {
    }

    setPolylineLatLngs(/*polyline, latLngs*/) {
    }

    createBounds() {
        return {};
    }

    extendBounds() {
    }

    invalidateSize() {
        this.onresize();
    }

    getTooltips() {
        return this.tooltips;
    }

}

class Position {
    constructor(x, y) {
        this.x = x;
        this.y = y;
    }

    equals(loc) {
        return loc && loc.x == this.x && loc.y == this.y;
    }
}
