/*
 * Copyright © 2016-2019 The Thingsboard Authors
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
    constructor($containerElement, utils, initCallback, defaultZoomLevel, dontFitMapBounds, disableScrollZooming, minZoomLevel, gmApiKey, gmDefaultMapType, defaultCenterPosition) {

        var tbMap = this;
        this.utils = utils;
        this.defaultZoomLevel = defaultZoomLevel;
        this.dontFitMapBounds = dontFitMapBounds;
        this.minZoomLevel = minZoomLevel;
        this.tooltips = [];
        this.defaultMapType = gmDefaultMapType;
        this.defaultCenterPosition = defaultCenterPosition;

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
                scrollwheel: !disableScrollZooming,
                mapTypeId: getGoogleMapTypeId(tbMap.defaultMapType),
                zoom: tbMap.defaultZoomLevel || 8,
                center: new google.maps.LatLng(tbMap.defaultCenterPosition[0], tbMap.defaultCenterPosition[1]) // eslint-disable-line no-undef
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
        this.createDefaultMarkerIcon(marker, color, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
        });
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    updateMarkerIcon(marker, settings) {
        this.createMarkerIcon(marker, settings, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
            if (settings.showLabel) {
                marker.set('labelAnchor', new google.maps.Point(100, iconInfo.size[1] + 20));
            }
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
                        var icon = {
                            url: currentImage.url,
                            scaledSize : new google.maps.Size(width, height)
                        };
                        var iconInfo = {
                            size: [width, height],
                            icon: icon
                        };
                        onMarkerIconReady(iconInfo);
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
        var pinColor = color.substr(1);
        var icon = new google.maps.MarkerImage("https://chart.apis.google.com/chart?chst=d_map_pin_letter_withshadow&chld=%E2%80%A2|" + pinColor,
            new google.maps.Size(40, 37),
            new google.maps.Point(0,0),
            new google.maps.Point(10, 37));
        var iconInfo = {
            size: [40, 37],
            icon: icon
        };
        onMarkerIconReady(iconInfo);
    }
    /* eslint-enable no-undef */

    /* eslint-disable no-undef */
    createMarker(location, dsIndex, settings, onClickListener, markerArgs) {
        var marker;
        if (settings.showLabel) {
            marker = new MarkerWithLabel({
                position: location,
                labelContent: '<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>',
                labelClass: "tb-labels"
            });
        } else {
            marker = new google.maps.Marker({
                position: location,
            });
        }
        var gMap = this;
        this.createMarkerIcon(marker, settings, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
            if (settings.showLabel) {
                marker.set('labelAnchor', new google.maps.Point(100, iconInfo.size[1] + 20));
            }
            marker.setMap(gMap.map);
        });

        if (settings.displayTooltip) {
            this.createTooltip(marker, dsIndex, settings, markerArgs);
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
    createTooltip(marker, dsIndex, settings, markerArgs) {
        var popup = new google.maps.InfoWindow({
            content: ''
        });
        var map = this;
        if (settings.displayTooltipAction == 'hover') {
            marker.addListener('mouseover', function () {
                popup.open(this.map, marker);
            });
            marker.addListener('mouseout', function () {
                popup.close();
            });
        } else {
            marker.addListener('click', function() {
                if (settings.autocloseTooltip) {
                    map.tooltips.forEach((tooltip) => {
                        tooltip.popup.close();
                    });
                }
                popup.open(this.map, marker);
            });
        }
        this.tooltips.push( {
            markerArgs: markerArgs,
            popup: popup,
            locationSettings: settings,
            dsIndex: dsIndex
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


	createPolygon(latLangs, settings, location,  onClickListener, markerArgs) {
		let polygon = new google.maps.Polygon({ // eslint-disable-line no-undef
			map: this.map,
			paths: latLangs,
			strokeColor: settings.polygonStrokeColor,
			strokeOpacity: settings.polygonStrokeColor,
			fillColor: settings.polygonColor,
			fillOpacity: settings.polygonOpacity,
			strokeWeight: settings.polygonStrokeWeight
		});

		//initialize-tooltip

		let popup = new google.maps.InfoWindow({ // eslint-disable-line no-undef
			content: ''
		});
		if (!this.tooltips) this.tooltips = [];
		this.tooltips.push({
			markerArgs: markerArgs,
			popup: popup,
			locationSettings: settings,
			dsIndex: location.dsIndex
		});
		let map = this;
		if (onClickListener) {
			google.maps.event.addListener(polygon, 'click', function (event) { // eslint-disable-line no-undef
				if (settings.displayTooltip ) {
					if (settings.autocloseTooltip) {
						map.tooltips.forEach((tooltip) => {
							tooltip.popup.close();
						});
					}
					if (!polygon.anchor) {
						polygon.anchor = new google.maps.MVCObject(); // eslint-disable-line no-undef
					}
					polygon.anchor.set("position", event.latLng);
					popup.open(this.map, polygon.anchor);

				}
				onClickListener();
			});
		}
		return polygon;
	}
	/* eslint-disable no-undef */

	removePolygon (polygon) {
		polygon.setMap(null);
	}

	/* eslint-disable no-undef,no-unused-vars */
	updatePolygonColor (polygon, settings, color) {
		let options = {
			paths: polygon.getPaths(),
			map: this.map,
			strokeColor: color,
			fillColor: color,
			strokeWeight: settings.polygonStrokeWeight
		};
		polygon.setOptions(options);
	}
	/* eslint-disable no-undef ,no-unused-vars*/


	getPolygonLatLngs(polygon) {
		return polygon.getPaths().getArray();
	}

	setPolygonLatLngs(polygon, latLngs) {
		polygon.setPaths(latLngs);
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
