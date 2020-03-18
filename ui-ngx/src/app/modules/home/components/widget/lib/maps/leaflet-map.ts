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
import './add-marker';

import 'leaflet-providers';
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'
import 'leaflet.markercluster/dist/leaflet.markercluster'

import { MapOptions, MarkerSettings } from './map-models';
import { Marker } from './markers';
import { Observable, of, BehaviorSubject, Subject } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Polyline } from './polyline';
import { Polygon } from './polygon';

export default abstract class LeafletMap {

    markers: Map<string, Marker> = new Map();
    dragMode = false;
    tooltips = [];
    poly: Polyline;
    polygon: Polygon;
    map: L.Map;
    map$: BehaviorSubject<L.Map> = new BehaviorSubject(null);
    ready$: Observable<L.Map> = this.map$.pipe(filter(map => !!map));
    options: MapOptions;
    isMarketCluster;
    bounds: L.LatLngBounds;

    constructor($container: HTMLElement, options: MapOptions) {
        this.options = options;
    }

    public initSettings(options: MapOptions) {
        const { initCallback,
            defaultZoomLevel,
            dontFitMapBounds,
            disableScrollZooming,
            minZoomLevel,
            mapProvider,
            credentials,
            defaultCenterPosition,
            draggebleMarker,
            markerClusteringSetting }: MapOptions = options;
        if (disableScrollZooming) {
            this.map.scrollWheelZoom.disable();
        }
        if (initCallback) {
            setTimeout(options.initCallback, 0);
        }
    }

    addMarkerControl() {
        if (this.options.draggebleMarker)
            L.Control.AddMarker  = L.Control.extend({
                onAdd (map) {
                    const img = L.DomUtil.create('img') as any;
                    img.src = `assets/add_location.svg`;
                    img.style.width = '32px';
                    img.style.height = '32px';
                    img.onclick = this.dragMarker;
                    return img;
                },
                addHooks () {
                    L.DomEvent.on(window as any, 'onclick', this.enableDragMode, this);
                },
                onRemove (map) {
                },
                dragMarker: ($event) => {
                    this.dragMode = !this.dragMode;
                }
            } as any);

        L.control.addMarker = function (opts) {
            return new L.Control.AddMarker(opts);
        }
        L.control.addMarker({ position: 'topright' }).addTo(this.map);
    }

    /*inited() {/// !!!!
        return !!this.map;
    }*/

    public setMap(map: L.Map) {
        this.map = map;
        if (this.options.useDefaultCenterPosition) {
            this.map.panTo(this.options.defaultCenterPosition);
            this.bounds = map.getBounds();
        }
        else this.bounds = new L.LatLngBounds(null, null);
        if (this.options.draggebleMarker) {
            this.addMarkerControl();
            this.map.on('click', (e: L.LeafletMouseEvent) => {
                if (this.dragMode)
                    this.saveMarkerLocation(this.convertToCustomFormat(e.latlng));
            })
        }
        this.map$.next(this.map);
    }

    public saveMarkerLocation(e) {

    }

    getContainer() {
        return this.map;
    }

    createLatLng(lat, lng) {
        return L.latLng(lat, lng);
    }

    createBounds() {
        return this.map.getBounds();
    }

    extendBounds(bounds, polyline) {
        if (polyline && polyline.getLatLngs() && polyline.getBounds()) {
            bounds.extend(polyline.getBounds());
        }
    }

    invalidateSize() {
        this.map?.invalidateSize(true);
    }

    onResize() {

    }

    getCenter() {
        return this.map.getCenter();
    }

    convertPosition(expression: any): L.LatLng {
        return L.latLng(expression[this.options.latKeyName], expression[this.options.lngKeyName]) as L.LatLng;
    }

    convertToCustomFormat(position: L.LatLng): object {
        return {
            [this.options.latKeyName]: position.lat,
            [this.options.lngKeyName]: position.lng
        }
    }

    // Markers
    updateMarkers(markersData) {
        markersData.forEach(data => {
            if (data.rotationAngle) {
                this.options.icon = L.divIcon({
                    html: `<div class="arrow" style="transform: rotate(${data.rotationAngle}deg)"><div>`
                })
            }
            else {
                this.options.icon = null;
            }
            if (this.markers.get(data.aliasName)) {
                this.updateMarker(data.aliasName, data, markersData, this.options as MarkerSettings)
            }
            else {
                this.createMarker(data.aliasName, data, markersData, this.options as MarkerSettings);
            }
        });
    }

    private createMarker(key, data, dataSources, settings: MarkerSettings) {
        this.ready$.subscribe(() => {
            const newMarker = new Marker(this.map, this.convertPosition(data), settings, data, dataSources);
            this.map.fitBounds(this.bounds.extend(newMarker.leafletMarker.getLatLng()));
            this.markers.set(key, newMarker);
        });
    }

    private updateMarker(key, data, dataSources, settings: MarkerSettings) {
        const marker: Marker = this.markers.get(key);
        const location = this.convertPosition(data)
        if (!location.equals(marker.location)) {
            marker.updateMarkerPosition(location);
        }
        if (settings.showTooltip) {
            marker.updateMarkerTooltip(data);
        }
        marker.setDataSources(data, dataSources);
        marker.updateMarkerIcon(settings);
    }

    private deleteMarker() {

    }

    // polyline

    updatePolylines(polyData: Array<Array<any>>) {
        polyData.forEach(data => {
            if (data.length) {
                const dataSource = polyData.map(arr => arr[0]);
                if (this.poly) {
                    this.updatePolyline(data, dataSource, this.options);
                }
                else {
                    this.createPolyline(data, dataSource, this.options);
                }
            }
        })
    }

    createPolyline(data, dataSources, settings) {
        if (data.length)
            this.ready$.subscribe(() => {
                this.poly = new Polyline(this.map, data.map(data => this.convertPosition(data)), data, dataSources, settings);
                const bounds = this.bounds.extend(this.poly.leafletPoly.getBounds());
                if (bounds.isValid()) {
                    this.map.fitBounds(bounds);
                    this.bounds = bounds;
                }
            });
    }

    updatePolyline(data, dataSources, settings) {
        this.ready$.subscribe(() => {
            this.poly.updatePolyline(settings, data, dataSources);
        });
    }

    // polygon

    updatePolygons(polyData: Array<Array<any>>) {
        polyData.forEach((data: any) => {
            if (data.data.length && data.dataKey.name === this.options.polygonKeyName) {
                if (typeof (data?.data[0][1]) === 'string') {
                    data.data = JSON.parse(data.data[0][1]);
                }
                if (this.polygon) {
                    this.updatePolygon(data.data, polyData, this.options);
                }
                else {
                    this.createPolygon(data.data, polyData, this.options);
                }
            }
        });
    }

    createPolygon(data, dataSources, settings) {
        this.ready$.subscribe(() => {
            this.polygon = new Polygon(this.map, data, dataSources, settings);
            const bounds = this.bounds.extend(this.polygon.leafletPoly.getBounds());
            if (bounds.isValid()) {
                this.map.fitBounds(bounds);
                this.bounds = bounds;
            }
        });
    }

    updatePolygon(data, dataSources, settings) {
        this.ready$.subscribe(() => {
            //   this.polygon.updatePolygon(settings, data, dataSources);
        });
    }
}