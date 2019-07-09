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
import 'leaflet/dist/leaflet.css';
import * as L from 'leaflet';
import 'leaflet-providers';

export default class TbOpenStreetMap {

	constructor($containerElement, utils, initCallback, defaultZoomLevel, dontFitMapBounds, disableScrollZooming, minZoomLevel, mapProvider, credentials, defaultCenterPosition) {

		this.utils = utils;
		this.defaultZoomLevel = defaultZoomLevel;
		this.dontFitMapBounds = dontFitMapBounds;
		this.minZoomLevel = minZoomLevel;
		this.tooltips = [];

		if (!mapProvider) {
			mapProvider = {
				name: "OpenStreetMap.Mapnik"
			};
		}

		if (mapProvider.name.startsWith("HERE.")) {
			credentials.app_id = credentials.app_id || "AhM6TzD9ThyK78CT3ptx";
			credentials.app_code = credentials.app_code || "p6NPiITB3Vv0GMUFnkLOOg";
		}

        defaultCenterPosition = defaultCenterPosition || [0,0];
		this.map = L.map($containerElement[0]).setView(defaultCenterPosition, this.defaultZoomLevel || 8);

		if (disableScrollZooming) {
			this.map.scrollWheelZoom.disable();
		}

		var tileLayer = mapProvider.isCustom ? L.tileLayer(mapProvider.name) : L.tileLayer.provider(mapProvider.name, credentials);
		tileLayer.addTo(this.map);

		if (initCallback) {
			setTimeout(initCallback, 0); //eslint-disable-line
		}

	}

	inited() {
		return angular.isDefined(this.map);
	}

	updateMarkerLabel(marker, settings) {
		marker.unbindTooltip();
		marker.bindTooltip('<div style="color: ' + settings.labelColor + ';"><b>' + settings.labelText + '</b></div>',
			{className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset});
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
				marker.tooltipOffset = [0, -iconInfo.size[1] + 10];
				marker.bindTooltip('<div style="color: ' + settings.labelColor + ';"><b>' + settings.labelText + '</b></div>',
					{className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset});
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
							iconAnchor: [width / 2, height],
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

	createMarker(location, dsIndex, settings, onClickListener, markerArgs) {
		var marker = L.marker(location, {});
		var opMap = this;
		this.createMarkerIcon(marker, settings, (iconInfo) => {
			marker.setIcon(iconInfo.icon);
			if (settings.showLabel) {
				marker.tooltipOffset = [0, -iconInfo.size[1] + 10];
				marker.bindTooltip('<div style="color: ' + settings.labelColor + ';"><b>' + settings.labelText + '</b></div>',
					{className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset});
			}
			marker.addTo(opMap.map);
		});

		if (settings.displayTooltip) {
			this.createTooltip(marker, dsIndex, settings, markerArgs);
		}

		if (onClickListener) {
			marker.on('click', onClickListener);
		}

		return marker;
	}

	removeMarker(marker) {
		this.map.removeLayer(marker);
	}

	createTooltip(marker, dsIndex, settings, markerArgs) {
		var popup = L.popup();
		popup.setContent('');
		marker.bindPopup(popup, {autoClose: settings.autocloseTooltip, closeOnClick: false});
		if (settings.displayTooltipAction == 'hover') {
			marker.off('click');
			marker.on('mouseover', function () {
				this.openPopup();
			});
			marker.on('mouseout', function () {
				this.closePopup();
			});
		}
		this.tooltips.push({
			markerArgs: markerArgs,
			popup: popup,
			locationSettings: settings,
			dsIndex: dsIndex
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

	createPolygon(latLangs, settings, location,  onClickListener, markerArgs) {
		let polygon = L.polygon(latLangs, {
			fill: true,
			fillColor: settings.polygonColor,
			color: settings.polygonStrokeColor,
			weight: settings.polygonStrokeWeight,
			fillOpacity: settings.polygonOpacity,
			opacity: settings.polygonStrokeOpacity
		}).addTo(this.map);

		if (settings.displayTooltip) {
			this.createTooltip(polygon, location.dsIndex, settings, markerArgs);
		}
		if (onClickListener) {
			polygon.on('click', onClickListener);
		}
		return polygon;
	}

	removePolygon(polygon) {
		this.map.removeLayer(polygon);
	}

	updatePolygonColor(polygon, settings, color) {
		let style = {
			fill: true,
			fillColor: color,
			color: color,
			weight: settings.polygonStrokeWeight,
			fillOpacity: settings.polygonOpacity,
			opacity: settings.polygonStrokeOpacity
		};
		polygon.setStyle(style);
	}

	getPolygonLatLngs(polygon) {
		return polygon.getLatLngs();
	}

	setPolygonLatLngs(polygon, latLngs) {
		polygon.setLatLngs(latLngs);
		polygon.redraw();
	}

	fitBounds(bounds) {
		if (bounds.isValid()) {
			if (this.dontFitMapBounds && this.defaultZoomLevel) {
				this.map.setZoom(this.defaultZoomLevel, {animate: false});
				this.map.panTo(bounds.getCenter(), {animate: false});
			} else {
				var tbMap = this;
				this.map.once('zoomend', function () {
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
		if (polyline && polyline.getLatLngs() && polyline.getBounds()) {
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
