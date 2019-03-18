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
var tmGlobals = {
	loadingTmId: null,
	tmApiKeys: {}
}

export default class TbTencentMap {
	constructor($containerElement, utils, initCallback, defaultZoomLevel, dontFitMapBounds, minZoomLevel, tmApiKey, tmDefaultMapType) {
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
				"<div class='error'>" + message + "</div>"
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
			var mapTypeId = qq.maps.MapTypeId.ROADMAP;
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

		window.tm_authFailure = function () { // eslint-disable-line no-undef, angular/window-service
			if (tmGlobals.loadingTmId && tmGlobals.loadingTmId === tbMap.mapId) {
				tmGlobals.loadingTmId = null;
				tmGlobals.tmApiKeys[tbMap.apiKey].error = 'Unable to authentificate for tencent Map API.</br>Please check your API key.';
				displayError(tmGlobals.tmApiKeys[tbMap.apiKey].error);
			}
		};

		this.initMapFunctionName = 'initTencentMap_' + this.mapId;

		window[this.initMapFunctionName] = function () { // eslint-disable-line no-undef, angular/window-service
			tmGlobals.tmApiKeys[tbMap.apiKey].loaded = true;
			initTencentMap();
			for (var p = 0; p < tmGlobals.tmApiKeys[tbMap.apiKey].pendingInits.length; p++) {
				var pendingInit = tmGlobals.tmApiKeys[tbMap.apiKey].pendingInits[p];
				pendingInit();
			}
			tmGlobals.tmApiKeys[tbMap.apiKey].pendingInits = [];
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
				var tencentMapScriptRes = 'https://map.qq.com/api/js?v=2.exp&key=' + this.apiKey + '&callback=' + this.initMapFunctionName;

				tmGlobals.loadingTmId = this.mapId;
				lazyLoad.load({type: 'js', path: tencentMapScriptRes}).then( // eslint-disable-line no-undef
					function success() {
						setTimeout(clearGlobalId, 2000); // eslint-disable-line no-undef, angular/timeout-service
					},
					function fail(e) {
						clearGlobalId();
						tmGlobals.tmApiKeys[tbMap.apiKey].error = 'tencent map api load failed!</br>' + e;
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

	createMarkerLabelStyle(settings) {
		return {
			width: "200px",
			textAlign: "center",
			color: settings.labelColor,
			background: "none",
			border: "none",
			fontSize: "12px",
			fontFamily: "\"Helvetica Neue\", Arial, Helvetica, sans-serif",
			fontWeight: "bold"
		};
	}

	/* eslint-disable no-undef,no-unused-vars*/
	updateMarkerLabel(marker, settings) {
		if (marker.label) {
			marker.label.setContent(settings.labelText);
			marker.label.setStyle(this.createMarkerLabelStyle(settings));
		}
	}

	/* eslint-enable no-undef,no-unused-vars */

	/* eslint-disable no-undef,no-unused-vars */
	updateMarkerColor(marker, color) {
		this.createDefaultMarkerIcon(marker, color, (iconInfo) => {
			marker.setIcon(iconInfo.icon);
		});
	}

	/* eslint-enable no-undef,,no-unused-vars */

	/* eslint-disable no-undef */
	updateMarkerIcon(marker, settings) {
		this.createMarkerIcon(marker, settings, (iconInfo) => {
			marker.setIcon(iconInfo.icon);
			if (marker.label) {
				marker.label.setOffset(new qq.maps.Size(-100, -iconInfo.size[1] - 20));
			}
		});
	}

	/* eslint-disable no-undef */

	/* eslint-disable no-undef */
	createMarkerIcon(marker, settings, onMarkerIconReady) {
		var currentImage = settings.currentImage;
		var tMap = this;
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
							new qq.maps.Size(width, height),
							new qq.maps.Point(0, 0),
							new qq.maps.Point(width / 2, height),
							new qq.maps.Size(width, height));
						var iconInfo = {
							size: [width, height],
							icon: icon
						};
						onMarkerIconReady(iconInfo);
					} else {
						tMap.createDefaultMarkerIcon(marker, settings.color, onMarkerIconReady);
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
		var icon = new qq.maps.MarkerImage("https://chart.apis.google.com/chart?chst=d_map_pin_letter_withshadow&chld=%E2%80%A2|" + pinColor,
			new qq.maps.Size(40, 37),
			new qq.maps.Point(0, 0),
			new qq.maps.Point(10, 37));
		var iconInfo = {
			size: [40, 37],
			icon: icon
		};
		onMarkerIconReady(iconInfo);
	}

	/* eslint-enable no-undef */

	/* eslint-disable no-undef */
	createMarker(location, dsIndex, settings, onClickListener, markerArgs) {
		var marker = new qq.maps.Marker({
			position: location
		});
		var tMap = this;
		this.createMarkerIcon(marker, settings, (iconInfo) => {
			marker.setIcon(iconInfo.icon);
			marker.setMap(tMap.map);
			if (settings.showLabel) {
				marker.label = new qq.maps.Label({
					clickable: false,
					content: settings.labelText,
					offset: new qq.maps.Size(-100, -iconInfo.size[1] - 20),
					style: tMap.createMarkerLabelStyle(settings),
					visible: true,
					position: location,
					map: tMap.map,
					zIndex: 1000
				});
			}
		});

		if (settings.displayTooltip) {
			this.createTooltip(marker, dsIndex, settings, markerArgs);
		}

		if (onClickListener) {
			qq.maps.event.addListener(marker, 'click', onClickListener);
		}

		return marker;
	}

	/* eslint-disable no-undef */
	removeMarker(marker) {
		marker.setMap(null);
		if (marker.label) {
			marker.label.setMap(null);
		}
	}

	/* eslint-enable no-undef */

	/* eslint-disable no-undef */
	createTooltip(marker, dsIndex, settings, markerArgs) {
		var popup = new qq.maps.InfoWindow({
			map: this.map
		});
		var map = this;
		qq.maps.event.addListener(marker, 'click', function () {
			if (settings.autocloseTooltip) {
				map.tooltips.forEach((tooltip) => {
					tooltip.popup.close();
				});
			}
			popup.open();
			popup.setPosition(marker);
		});
		map.tooltips.push({
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

	/* eslint-disable no-undef */
	createPolygon(latLangs, settings, location,  onClickListener, markerArgs) {
		let polygon = new qq.maps.Polygon({
			map: this.map,
			path: latLangs,
			strokeColor: settings.polygonStrokeColor,
			fillColor: settings.polygonColor,
			strokeWeight: settings.polygonStrokeWeight
		});
		//initialize-tooltip
		let popup = new qq.maps.InfoWindow({
			content: ''
		});
		if (!this.tooltips) this.tooltips = [];
		this.tooltips.push({
			markerArgs: markerArgs,
			popup: popup,
			locationSettings: settings,
			dsIndex: location.dsIndex
		});

		if (onClickListener) {
			qq.maps.event.addListener(polygon, 'click', function (event) {
				if (settings.autocloseTooltip) {
					map.tooltips.forEach((tooltip) => {
						tooltip.popup.close();
					});
				}
				if (settings.displayTooltip) {
					popup.setMap(this.map);
					popup.setPosition(event.latLng);
					popup.open();
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
			path: polygon.getPath(),
			map: this.map,
			strokeColor: color,
			fillColor: color,
			strokeWeight: settings.polygonStrokeWeight
		}
		polygon.setOptions(options);
	}
	/* eslint-disable no-undef ,no-unused-vars*/


	getPolygonLatLngs(polygon) {
		return polygon.getPath().getArray();
	}

	setPolygonLatLngs(polygon, latLngs) {
		polygon.setPath(latLngs);
	}

	/* eslint-disable no-undef ,no-unused-vars*/
	fitBounds(bounds) {
		if (this.dontFitMapBounds && this.defaultZoomLevel) {
			this.map.setZoom(this.defaultZoomLevel);
			this.map.setCenter(bounds.getCenter());
		} else {
			var tbMap = this;
			qq.maps.event.addListenerOnce(this.map, 'bounds_changed', function () { // eslint-disable-line no-undef
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
		if (marker.label) {
			marker.label.setPosition(latLng);
		}
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
