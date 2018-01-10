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

var tmGlobals = {
    loadingTmId: null,
    tmApiKeys: {}
}

export default class TbTencentMap {
    constructor($containerElement,utils, initCallback, defaultZoomLevel, dontFitMapBounds, minZoomLevel, tmApiKey, tmDefaultMapType) {
        var tbMap = this;
        this.utils = utils;
        this.defaultZoomLevel = defaultZoomLevel;
        this.dontFitMapBounds = dontFitMapBounds;
        this.minZoomLevel = minZoomLevel;
        this.tooltips = [];
        this.defaultMapType = tmDefaultMapType;

        function clearGlobalId() {
            if (tmGlobals.loadingTmId && tmGlobals.loadingTmId === tbMap.mapId) {
                tmGlobals.loadingTmId = null;
            }
        }

        function displayError(message) {
            $containerElement.html( // eslint-disable-line angular/angularelement
                "<div class='error'>"+ message + "</div>"
            );
        }

        function initTencentMap() {
            tbMap.map = new qq.maps.Map($containerElement[0], { // eslint-disable-line no-undef
                scrollwheel: true,
                mapTypeId: getTencentMapTypeId(tbMap.defaultMapType),
                zoom: tbMap.defaultZoomLevel || 8
            });

            if (initCallback) {
                initCallback();
            }
        }

        /* eslint-disable no-undef */

        function getTencentMapTypeId(mapType) {
            var mapTypeId =qq.maps.MapTypeId.ROADMAP;
            if (mapType) {
                if (mapType === 'hybrid') {
                   mapTypeId = qq.maps.MapTypeId.HYBRID;
                } else if (mapType === 'satellite') {
                   mapTypeId = qq.maps.MapTypeId.SATELLITE;
                } else if (mapType === 'terrain') {
                   mapTypeId = qq.maps.MapTypeId.ROADMAP;
                }
            }
            return mapTypeId;
        }

        /* eslint-enable no-undef */

        this.mapId = '' + Math.random().toString(36).substr(2, 9);
        this.apiKey = tmApiKey || '84d6d83e0e51e481e50454ccbe8986b';

        window.gm_authFailure = function() { // eslint-disable-line no-undef, angular/window-service
            if (tmGlobals.loadingTmId && tmGlobals.loadingTmId === tbMap.mapId) {
                tmGlobals.loadingTmId = null;
                tmGlobals.tmApiKeys[tbMap.apiKey].error = 'Unable to authentificate for tencent Map API.</br>Please check your API key.';
                displayError(tmGlobals.tmApiKeys[tbMap.apiKey].error);
            }
        };

        this.initMapFunctionName = 'initTencentMap_' + this.mapId;

        window[this.initMapFunctionName] = function() { // eslint-disable-line no-undef, angular/window-service
           initTencentMap();
        };
        if (this.apiKey && this.apiKey.length > 0) {
            if (tmGlobals.tmApiKeys[this.apiKey]) {
                if (tmGlobals.tmApiKeys[this.apiKey].error) {
                    displayError(tmGlobals.tmApiKeys[this.apiKey].error);
                } else if (tmGlobals.tmApiKeys[this.apiKey].loaded) {
                    initTencentMap();
                } else {
                    tmGlobals.tmApiKeys[this.apiKey].pendingInits.push(initTencentMap);
                }
            } else {
                tmGlobals.tmApiKeys[this.apiKey] = {
                    loaded: false,
                    pendingInits: []
                };
                var tencentMapScriptRes = 'http://map.qq.com/api/js?v=2.exp&key='+this.apiKey+'&callback='+this.initMapFunctionName;

                tmGlobals.loadingTmId = this.mapId;
                lazyLoad.load({ type: 'js', path: tencentMapScriptRes }).then( // eslint-disable-line no-undef
                    function success() {
                        setTimeout(clearGlobalId, 2000); // eslint-disable-line no-undef, angular/timeout-service
                    },
                    function fail(e) {
                        clearGlobalId();
                        tmGlobals.tmApiKeys[tbMap.apiKey].error = 'tencent map api load failed!</br>'+e;
                        displayError(tmGlobals.tmApiKeys[tbMap.apiKey].error);
                    }
                );
            }
        } else {
            displayError('No tencent Map Api Key provided!');
        }
    }

    inited() {
        return angular.isDefined(this.map);
    }

    /* eslint-disable no-undef,no-unused-vars*/
    updateMarkerLabel(marker, settings) {

    }
    /* eslint-enable no-undef,no-unused-vars */

    /* eslint-disable no-undef,no-unused-vars */
    updateMarkerColor(marker, color) {
        this.createDefaultMarkerIcon(marker, color, (icon) => {
            marker.setIcon(icon);
        });
    }
    /* eslint-enable no-undef,,no-unused-vars */

    /* eslint-disable no-undef */
    updateMarkerIcon(marker, settings) {
        this.createMarkerIcon(marker, settings, (icon) => {
            marker.setIcon(icon);
        });
    }
    /* eslint-disable no-undef */

    /* eslint-disable no-undef */
    createMarkerIcon(marker, settings, onMarkerIconReady) {
        var currentImage = settings.currentImage;
        var gMap = this;
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

                        var icon = new qq.maps.MarkerImage(currentImage.url,
                            qq.maps.Size(width, height),
                            new qq.maps.Point(0,0),
                            new qq.maps.Point(10, 37));

                        onMarkerIconReady(icon);
                    } else {
                        gMap.createDefaultMarkerIcon(marker, settings.color, onMarkerIconReady);
                    }
                }
            );
        } else {
            this.createDefaultMarkerIcon(marker, settings.color, onMarkerIconReady);
        }
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    createDefaultMarkerIcon(marker, color, onMarkerIconReady) {
       /* var pinColor = color.substr(1);*/
        var icon = new qq.maps.MarkerImage("http://api.map.qq.com/doc/img/nilt.png",
            new qq.maps.Size(40, 37),
            new qq.maps.Point(0,0),
            new qq.maps.Point(10, 37));

        onMarkerIconReady(icon);
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    createMarker(location, settings, onClickListener, markerArgs) {
         var marker = new qq.maps.Marker({
           map: this.map,
           position:location
         });

        var gMap = this;
        this.createMarkerIcon(marker, settings, (icon) => {
            marker.setIcon(icon);
            marker.setMap(gMap.map)
        });

        if (settings.displayTooltip) {
            this.createTooltip(marker, settings.tooltipPattern, settings.tooltipReplaceInfo, settings.autocloseTooltip, markerArgs);
        }

        if (onClickListener) {
            qq.maps.event.addListener(marker, 'click', onClickListener);
        }

        return marker;
    }

    /* eslint-disable no-undef */
    removeMarker(marker) {
        marker.setMap(null);
    }

    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    createTooltip(marker, pattern, replaceInfo, autoClose, markerArgs) {
        var popup = new qq.maps.InfoWindow({
            map :this.map
        });
        var map = this;
        qq.maps.event.addListener(marker, 'click', function() {
            if (autoClose) {
                map.tooltips.forEach((tooltip) => {
                    tooltip.popup.close();
                });
            }
            popup.open();
            popup.setPosition(marker);
        });
        this.tooltips.push( {
            markerArgs: markerArgs,
            popup: popup,
            pattern: pattern,
            replaceInfo: replaceInfo
        });
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    updatePolylineColor(polyline, settings, color) {
        var options = {
            path: polyline.getPath(),
            strokeColor: color,
            strokeOpacity: settings.strokeOpacity,
            strokeWeight: settings.strokeWeight,
            map: this.map
        };
        polyline.setOptions(options);
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    createPolyline(locations, settings) {
        var polyline = new qq.maps.Polyline({
            path: locations,
            strokeColor: settings.color,
            strokeOpacity: settings.strokeOpacity,
            strokeWeight: settings.strokeWeight,
            map: this.map
        });

        return polyline;
    }
    /* eslint-enable no-undef */

    removePolyline(polyline) {
        polyline.setMap(null);
    }

    /* eslint-disable no-undef ,no-unused-vars*/
    fitBounds(bounds) {
        if (this.dontFitMapBounds && this.defaultZoomLevel) {
            this.map.setZoom(this.defaultZoomLevel);
            this.map.setCenter(bounds.getCenter());
        } else {
            var tbMap = this;
            qq.maps.event.addListenerOnce(this.map, 'bounds_changed', function() { // eslint-disable-line no-undef
                if (!tbMap.defaultZoomLevel && tbMap.map.getZoom() > tbMap.minZoomLevel) {
                    tbMap.map.setZoom(tbMap.minZoomLevel);
                }
            });
            this.map.fitBounds(bounds);
        }
    }
    /* eslint-enable no-undef,no-unused-vars */

    createLatLng(lat, lng) {
        return new qq.maps.LatLng(lat, lng); // eslint-disable-line no-undef
    }

    extendBoundsWithMarker(bounds, marker) {
        bounds.extend(marker.getPosition());
    }

    getMarkerPosition(marker) {
        return marker.getPosition();
    }

    setMarkerPosition(marker, latLng) {
        marker.setPosition(latLng);
    }

    getPolylineLatLngs(polyline) {
        return polyline.getPath().getArray();
    }

    setPolylineLatLngs(polyline, latLngs) {
        polyline.setPath(latLngs);
    }

    createBounds() {
        return new qq.maps.LatLngBounds(); // eslint-disable-line no-undef
    }

    extendBounds(bounds, polyline) {
        if (polyline && polyline.getPath()) {
            var locations = polyline.getPath();
            for (var i = 0; i < locations.getLength(); i++) {
                bounds.extend(locations.getAt(i));
            }
        }
    }

    invalidateSize() {
        qq.maps.event.trigger(this.map, "resize"); // eslint-disable-line no-undef
    }

    getTooltips() {
        return this.tooltips;
    }

}
