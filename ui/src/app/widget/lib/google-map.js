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

var gmGlobals = {
    loadingGmId: null,
    gmApiKeys: {}
}

export default class TbGoogleMap {
    constructor($containerElement, initCallback, defaultZoomLevel, dontFitMapBounds, minZoomLevel, gmApiKey, gmDefaultMapType) {

        var tbMap = this;
        this.defaultZoomLevel = defaultZoomLevel;
        this.dontFitMapBounds = dontFitMapBounds;
        this.minZoomLevel = minZoomLevel;
        this.tooltips = [];
        this.defaultMapType = gmDefaultMapType;

        function clearGlobalId() {
            if (gmGlobals.loadingGmId && gmGlobals.loadingGmId === tbMap.mapId) {
                gmGlobals.loadingGmId = null;
            }
        }

        function displayError(message) {
            $containerElement.html( // eslint-disable-line angular/angularelement
                "<div class='error'>"+ message + "</div>"
            );
        }

        function initGoogleMap() {

            tbMap.map = new google.maps.Map($containerElement[0], { // eslint-disable-line no-undef
                scrollwheel: false,
                mapTypeId: getGoogleMapTypeId(tbMap.defaultMapType),
                zoom: tbMap.defaultZoomLevel || 8
            });

            if (initCallback) {
                initCallback();
            }

        }

        /* eslint-disable no-undef */

        function getGoogleMapTypeId(mapType) {
            var mapTypeId = google.maps.MapTypeId.ROADMAP;
            if (mapType) {
                if (mapType === 'hybrid') {
                    mapTypeId = google.maps.MapTypeId.HYBRID;
                } else if (mapType === 'satellite') {
                    mapTypeId = google.maps.MapTypeId.SATELLITE;
                } else if (mapType === 'terrain') {
                    mapTypeId = google.maps.MapTypeId.TERRAIN;
                }
            }
            return mapTypeId;
        }

        /* eslint-enable no-undef */

        this.mapId = '' + Math.random().toString(36).substr(2, 9);
        this.apiKey = gmApiKey || '';

        window.gm_authFailure = function() { // eslint-disable-line no-undef, angular/window-service
            if (gmGlobals.loadingGmId && gmGlobals.loadingGmId === tbMap.mapId) {
                gmGlobals.loadingGmId = null;
                gmGlobals.gmApiKeys[tbMap.apiKey].error = 'Unable to authentificate for Google Map API.</br>Please check your API key.';
                displayError(gmGlobals.gmApiKeys[tbMap.apiKey].error);
            }
        };

        this.initMapFunctionName = 'initGoogleMap_' + this.mapId;

        window[this.initMapFunctionName] = function() { // eslint-disable-line no-undef, angular/window-service
            lazyLoad.load({ type: 'js', path: 'https://cdn.rawgit.com/googlemaps/v3-utility-library/master/markerwithlabel/src/markerwithlabel.js' }).then( // eslint-disable-line no-undef
                function success() {
                    gmGlobals.gmApiKeys[tbMap.apiKey].loaded = true;
                    initGoogleMap();
                    for (var p = 0; p < gmGlobals.gmApiKeys[tbMap.apiKey].pendingInits.length; p++) {
                        var pendingInit = gmGlobals.gmApiKeys[tbMap.apiKey].pendingInits[p];
                        pendingInit();
                    }
                    gmGlobals.gmApiKeys[tbMap.apiKey].pendingInits = [];
                },
                function fail(e) {
                    clearGlobalId();
                    gmGlobals.gmApiKeys[tbMap.apiKey].error = 'Google map api load failed!</br>'+e;
                    displayError(gmGlobals.gmApiKeys[tbMap.apiKey].error);
                }
            );

        };

        if (this.apiKey && this.apiKey.length > 0) {
            if (gmGlobals.gmApiKeys[this.apiKey]) {
                if (gmGlobals.gmApiKeys[this.apiKey].error) {
                    displayError(gmGlobals.gmApiKeys[this.apiKey].error);
                } else if (gmGlobals.gmApiKeys[this.apiKey].loaded) {
                    initGoogleMap();
                } else {
                    gmGlobals.gmApiKeys[this.apiKey].pendingInits.push(initGoogleMap);
                }
            } else {
                gmGlobals.gmApiKeys[this.apiKey] = {
                    loaded: false,
                    pendingInits: []
                };
                var googleMapScriptRes = 'https://maps.googleapis.com/maps/api/js?key='+this.apiKey+'&callback='+this.initMapFunctionName;

                gmGlobals.loadingGmId = this.mapId;
                lazyLoad.load({ type: 'js', path: googleMapScriptRes }).then( // eslint-disable-line no-undef
                    function success() {
                        setTimeout(clearGlobalId, 2000); // eslint-disable-line no-undef, angular/timeout-service
                    },
                    function fail(e) {
                        clearGlobalId();
                        gmGlobals.gmApiKeys[tbMap.apiKey].error = 'Google map api load failed!</br>'+e;
                        displayError(gmGlobals.gmApiKeys[tbMap.apiKey].error);
                    }
                );
            }
        } else {
            displayError('No Google Map Api Key provided!');
        }
    }

    inited() {
        return angular.isDefined(this.map);
    }

    /* eslint-disable no-undef */
    updateMarkerLabel(marker, settings) {
        marker.set('labelContent', '<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>');
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    updateMarkerColor(marker, color) {
        var pinColor = color.substr(1);
        var pinImage = new google.maps.MarkerImage("https://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|" + pinColor,
            new google.maps.Size(21, 34),
            new google.maps.Point(0,0),
            new google.maps.Point(10, 34));
        marker.setIcon(pinImage);
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    updateMarkerImage(marker, settings, image, maxSize) {
        var testImage = new Image();
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
            var pinImage = {
                url: image,
                scaledSize : new google.maps.Size(width, height)
            }
            marker.setIcon(pinImage);
            if (settings.showLabel) {
                marker.set('labelAnchor', new google.maps.Point(100, height + 20));
            }
        }
        testImage.src = image;
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    createMarker(location, settings, onClickListener, markerArgs) {
        var height = 34;
        var pinColor = settings.color.substr(1);
        var pinImage = new google.maps.MarkerImage("https://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|" + pinColor,
            new google.maps.Size(21, 34),
            new google.maps.Point(0,0),
            new google.maps.Point(10, 34));
        var pinShadow = new google.maps.MarkerImage("https://chart.apis.google.com/chart?chst=d_map_pin_shadow",
            new google.maps.Size(40, 37),
            new google.maps.Point(0, 0),
            new google.maps.Point(12, 35));
        var marker;
        if (settings.showLabel) {
            marker = new MarkerWithLabel({
                position: location,
                map: this.map,
                icon: pinImage,
                shadow: pinShadow,
                labelContent: '<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>',
                labelClass: "tb-labels",
                labelAnchor: new google.maps.Point(100, height + 20)
            });
        } else {
            marker = new google.maps.Marker({
                position: location,
                map: this.map,
                icon: pinImage,
                shadow: pinShadow
            });
        }

        if (settings.useMarkerImage) {
            this.updateMarkerImage(marker, settings, settings.markerImage, settings.markerImageSize || 34);
        }

        if (settings.displayTooltip) {
            this.createTooltip(marker, settings.tooltipPattern, settings.tooltipReplaceInfo, markerArgs);
        }

        if (onClickListener) {
            marker.addListener('click', onClickListener);
        }

        return marker;
    }

    removeMarker(marker) {
        marker.setMap(null);
    }

    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    createTooltip(marker, pattern, replaceInfo, markerArgs) {
        var popup = new google.maps.InfoWindow({
            content: ''
        });
        marker.addListener('click', function() {
            popup.open(this.map, marker);
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
        var polyline = new google.maps.Polyline({
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

    /* eslint-disable no-undef */
    fitBounds(bounds) {
        if (this.dontFitMapBounds && this.defaultZoomLevel) {
            this.map.setZoom(this.defaultZoomLevel);
            this.map.setCenter(bounds.getCenter());
        } else {
            var tbMap = this;
            google.maps.event.addListenerOnce(this.map, 'bounds_changed', function() { // eslint-disable-line no-undef
                if (!tbMap.defaultZoomLevel && tbMap.map.getZoom() > tbMap.minZoomLevel) {
                    tbMap.map.setZoom(tbMap.minZoomLevel);
                }
            });
            this.map.fitBounds(bounds);
        }
    }
    /* eslint-enable no-undef */

    createLatLng(lat, lng) {
        return new google.maps.LatLng(lat, lng); // eslint-disable-line no-undef
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
        return new google.maps.LatLngBounds(); // eslint-disable-line no-undef
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
        google.maps.event.trigger(this.map, "resize"); // eslint-disable-line no-undef
    }

    getTooltips() {
        return this.tooltips;
    }

}
