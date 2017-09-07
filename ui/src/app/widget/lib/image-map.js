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

import 'tooltipster/dist/css/tooltipster.bundle.min.css';
import 'tooltipster/dist/js/tooltipster.bundle.min.js';
import 'tooltipster/dist/css/plugins/tooltipster/sideTip/themes/tooltipster-sideTip-shadow.min.css';

import './image-map.scss';

const pinShape = '<path id="pin" d="m 12.033721,23.509909 c 0.165665,-3.220958 1.940547,-8.45243 4.512974,-11.745035 1.401507,-1.7940561 2.046337,-3.5425327 2.046337,-4.6032909 0,-3.6844827 -2.951858,-6.67149197 -6.592948,-6.67149197 l -1.68e-4,0 c -3.6412584,0 -6.5929483,2.98700927 -6.5929483,6.67149197 0,1.0607582 0.6448307,2.8092348 2.0463367,4.6032909 2.5724276,3.292605 4.3471416,8.524077 4.5129736,11.745035 l 0.06745,0 z" style="fill:#f2756a;fill-opacity:1;fill-rule:nonzero;stroke:#000000;stroke-opacity:1"/>';
const circleShape = '<circle id="circle" fill-rule="evenodd" cy="6.9234" cx="12" clip-rule="evenodd" r="1.5"/>';
const pinSvg = `<svg class="image-map-pin-image" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">${pinShape}${circleShape}</svg>`;

export default class TbImageMap {

    constructor(ctx, $containerElement, initCallback, imageUrl, posFunction, imageEntityAlias, imageUrlAttribute) {

        this.ctx = ctx;
        this.tooltips = [];

        $containerElement.append('<div id="image-map-container"><div id="image-map"></div></div>');

        this.imageMapContainer = angular.element('#image-map-container', $containerElement);
        this.imageMap = angular.element('#image-map', $containerElement);
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
                    this.loadImage(attrValue, this.aspect > 0 ? null : this.initCallback);
                }
            }
        }
        if (apply) {
            this.ctx.$scope.$digest();
        }
    }

    loadImage(imageUrl, initCallback) {
        if (!imageUrl) {
            imageUrl = 'data:image/svg+xml;base64,PHN2ZyBpZD0ic3ZnMiIgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTAwIiB3aWR0aD0iMTAwIiB2ZXJzaW9uPSIxLjEiIHhtbG5zOmNjPSJodHRwOi8vY3JlYXRpdmVjb21tb25zLm9yZy9ucyMiIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgdmlld0JveD0iMCAwIDEwMCAxMDAiPgogPGcgaWQ9ImxheWVyMSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAtOTUyLjM2KSI+CiAgPHJlY3QgaWQ9InJlY3Q0Njg0IiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBoZWlnaHQ9Ijk5LjAxIiB3aWR0aD0iOTkuMDEiIHN0cm9rZT0iIzAwMCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiB5PSI5NTIuODYiIHg9Ii40OTUwNSIgc3Ryb2tlLXdpZHRoPSIuOTkwMTAiIGZpbGw9IiNlZWUiLz4KICA8dGV4dCBpZD0idGV4dDQ2ODYiIHN0eWxlPSJ3b3JkLXNwYWNpbmc6MHB4O2xldHRlci1zcGFjaW5nOjBweDt0ZXh0LWFuY2hvcjptaWRkbGU7dGV4dC1hbGlnbjpjZW50ZXIiIGZvbnQtd2VpZ2h0PSJib2xkIiB4bWw6c3BhY2U9InByZXNlcnZlIiBmb250LXNpemU9IjEwcHgiIGxpbmUtaGVpZ2h0PSIxMjUlIiB5PSI5NzAuNzI4MDkiIHg9IjQ5LjM5NjQ3NyIgZm9udC1mYW1pbHk9IlJvYm90byIgZmlsbD0iIzY2NjY2NiI+PHRzcGFuIGlkPSJ0c3BhbjQ2OTAiIHg9IjUwLjY0NjQ3NyIgeT0iOTcwLjcyODA5Ij5JbWFnZSBiYWNrZ3JvdW5kIDwvdHNwYW4+PHRzcGFuIGlkPSJ0c3BhbjQ2OTIiIHg9IjQ5LjM5NjQ3NyIgeT0iOTgzLjIyODA5Ij5pcyBub3QgY29uZmlndXJlZDwvdHNwYW4+PC90ZXh0PgogIDxyZWN0IGlkPSJyZWN0NDY5NCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgaGVpZ2h0PSIxOS4zNiIgd2lkdGg9IjY5LjM2IiBzdHJva2U9IiMwMDAiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgeT0iOTkyLjY4IiB4PSIxNS4zMiIgc3Ryb2tlLXdpZHRoPSIuNjM5ODYiIGZpbGw9Im5vbmUiLz4KIDwvZz4KPC9zdmc+Cg==';
        }
        this.imageMap.css({backgroundImage: 'url('+imageUrl+')'});
        var imageMap = this;
        var testImage = document.createElement('img'); // eslint-disable-line
        testImage.style.visibility = 'hidden';
        testImage.onload = function() {
            imageMap.aspect = testImage.width / testImage.height;
            document.body.removeChild(testImage); //eslint-disable-line
            imageMap.onresize();
            if (initCallback) {
                setTimeout(initCallback, 0); //eslint-disable-line
            } else {
                imageMap.onresize();
            }
        }
        document.body.appendChild(testImage); //eslint-disable-line
        testImage.src = imageUrl;
    }

    onresize() {
        if (this.aspect > 0) {
            var width = this.imageMapContainer.width();
            if (width > 0) {
                var height = width / this.aspect;
                var imageMapHeight = this.imageMapContainer.height();
                if (imageMapHeight > 0 && height > imageMapHeight) {
                    height = imageMapHeight;
                    width = height * this.aspect;
                }
                if (this.width !== width) {
                    this.width = width;
                    this.height = width / this.aspect;
                    this.imageMap.css({width: this.width, height: this.height});
                    this.markers.forEach((marker) => {
                        this.updateMarkerDimensions(marker);
                    });
                }
            }
        }
    }

    inited() {
        return this.aspect > 0 ? true : false;
    }

    updateMarkerLabel(marker, settings) {
        if (settings.showLabel) {
            marker.labelElement.css({color: settings.labelColor});
            marker.labelElement.html(`<b>${settings.labelText}</b>`);
        }
    }

    updateMarkerColor(marker, color) {
        marker.pinSvgElement.css({fill: color});
    }

    updateMarkerImage(marker, settings, image, maxSize) {
        var testImage = new Image(); // eslint-disable-line no-undef
        var imageMap = this;
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
            var size = Math.max(width, height);
            marker.size = size;
            if (marker.imgElement) {
                marker.imgElement.remove();
            }
            marker.imgElement = angular.element(`<img src="${image}" aria-label="pin" class="image-map-pin-image"/>`);
            var left = (size - width)/2;
            var top = (size - height)/2;
            marker.imgElement.css({width: width, height: height, left: left, top: top});
            marker.pinElement.append(marker.imgElement);
            imageMap.updateMarkerDimensions(marker);
        }
        testImage.src = image;
    }

    updateMarkerDimensions(marker) {
        var pinElement = marker.pinElement;
        pinElement.css({width: marker.size, height: marker.size});
        var left = marker.x * this.width - marker.size * marker.offsetX;
        var top = marker.y * this.height - marker.size * marker.offsetY;
        pinElement.css({left: left, top: top});
    }

    createMarker(position, settings, onClickListener, markerArgs) {
        var marker = {
              size: 34,
              position: position
        };
        var pos = this.posFunction(position.x, position.y);
        marker.x = pos.x;
        marker.y = pos.y;
        marker.offsetX = settings.markerOffsetX;
        marker.offsetY = settings.markerOffsetY;
        marker.pinElement = angular.element('<div class="image-map-pin"></div>');

        if (settings.showLabel) {
            marker.labelElement = angular.element(`<div class="image-map-pin-title"><b>${settings.labelText}</b></div>`);
            marker.labelElement.css({color: settings.labelColor});
            marker.pinElement.append(marker.labelElement);
        }

        marker.imgElement = angular.element(pinSvg);
        marker.pinSvgElement = marker.imgElement.find('#pin');
        marker.pinElement.append(marker.imgElement);

        marker.pinSvgElement.css({fill: settings.color});

        this.updateMarkerDimensions(marker);

        this.imageMap.append(marker.pinElement);

        if (settings.useMarkerImage) {
            this.updateMarkerImage(marker, settings, settings.markerImage, settings.markerImageSize || 34);
        }

        if (settings.displayTooltip) {
            this.createTooltip(marker, settings.tooltipPattern, settings.tooltipReplaceInfo, markerArgs);
        }

        if (onClickListener) {
            marker.pinElement.on('click', onClickListener);
        }

        this.markers.push(marker);
        return marker;
    }

    removeMarker(marker) {
        var index = this.markers.indexOf(marker);
        if (index > -1) {
            marker.pinElement.remove();
            this.markers.splice(index, 1);
        }
    }

    createTooltip(marker, pattern, replaceInfo, markerArgs) {
        var popup = new Popup(this.ctx, marker.pinElement);
        popup.setContent('');
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
        marker.x = pos.x;
        marker.y = pos.y;
        this.updateMarkerDimensions(marker);
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

class Popup {
    constructor(ctx, anchor) {
        anchor.tooltipster(
            {
                theme: 'tooltipster-shadow',
                delay: 100,
                trigger: 'custom',
                triggerOpen: {
                    click: true,
                    tap: true
                },
                trackOrigin: true
            }
        );
        this.tooltip = anchor.tooltipster('instance');
        var contentElement = angular.element('<div class="image-map-pin-tooltip">' +
                '<a class="image-map-pin-tooltip-close-button" id="close" style="outline: none;">×</a>' +
                '<div id="tooltip-content">' +
                '</div>' +
            '</div>');
        var $compile = ctx.$scope.$injector.get('$compile');
        $compile(contentElement)(ctx.$scope);
        var popup = this;
        contentElement.find('#close').on('click', function() {
            popup.tooltip.close();
        });
        this.content = contentElement.find('#tooltip-content');
        this.tooltip.content(contentElement);
    }

    setContent(content) {
        this.content.html(content);
    }
}
