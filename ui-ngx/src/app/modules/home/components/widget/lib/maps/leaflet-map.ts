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

import L, { LatLngBounds, LatLngTuple } from 'leaflet';

import 'leaflet-providers';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import 'leaflet.markercluster/dist/MarkerCluster.Default.css';
import LM from 'leaflet.markercluster/dist/leaflet.markercluster';

import {
  FormattedData,
  MapSettings,
  MarkerSettings,
  PolygonSettings,
  PolylineSettings,
  UnitedMapSettings
} from './map-models';
import { Marker } from './markers';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Polyline } from './polyline';
import { Polygon } from './polygon';
import { DatasourceData } from '@app/shared/models/widget.models';

export default abstract class LeafletMap {

    markers: Map<string, Marker> = new Map();
    polylines: Map<string, Polyline> = new Map();
    polygons: Map<string, Polygon> = new Map();
    dragMode = false;
    map: L.Map;
    map$: BehaviorSubject<L.Map> = new BehaviorSubject(null);
    ready$: Observable<L.Map> = this.map$.pipe(filter(map => !!map));
    options: UnitedMapSettings;
    isMarketCluster: boolean;
    bounds: L.LatLngBounds;
    newMarker: L.Marker;
    datasources: FormattedData[];
    markersCluster: LM.markerClusterGroup;

    constructor(public $container: HTMLElement, options: UnitedMapSettings) {
        this.options = options;
    }

    public initSettings(options: MapSettings) {
        const { initCallback,
            disableScrollZooming,
            useClusterMarkers,
            zoomOnClick,
            showCoverageOnHover,
            removeOutsideVisibleBounds,
            animate,
            chunkedLoading,
            maxClusterRadius,
            maxZoom }: MapSettings = options;
        if (disableScrollZooming) {
            this.map.scrollWheelZoom.disable();
        }
        if (initCallback) {
            setTimeout(options.initCallback, 0);
        }
        if (useClusterMarkers) {
            const clusteringSettings: LM.MarkerClusterGroupOptions = {
                zoomToBoundsOnClick: zoomOnClick,
                showCoverageOnHover,
                removeOutsideVisibleBounds,
                animate,
                chunkedLoading
            };
            if (maxClusterRadius && maxClusterRadius > 0) {
                clusteringSettings.maxClusterRadius = Math.floor(maxClusterRadius);
            }
            if (maxZoom && maxZoom >= 0 && maxZoom < 19) {
                clusteringSettings.disableClusteringAtZoom = Math.floor(maxZoom);
            }
            this.markersCluster = LM.markerClusterGroup(clusteringSettings);
            this.ready$.subscribe(map => map.addLayer(this.markersCluster));
        }
    }

    addMarkerControl() {
        if (this.options.draggableMarker) {
            let mousePositionOnMap: L.LatLng;
            let addMarker: L.Control;
            this.map.on('mouseup', (e: L.LeafletMouseEvent) => {
                mousePositionOnMap = e.latlng;
            });
            const dragListener = (e: L.DragEndEvent) => {
                if (e.type === 'dragend' && mousePositionOnMap) {
                    const newMarker = L.marker(mousePositionOnMap).addTo(this.map);
                    const datasourcesList = document.createElement('div');
                    const customLatLng = this.convertToCustomFormat(mousePositionOnMap);
                    this.datasources.forEach(ds => {
                        const dsItem = document.createElement('p');
                        dsItem.appendChild(document.createTextNode(ds.entityName));
                        dsItem.setAttribute('style', 'font-size: 14px');
                        dsItem.onclick = () => {
                            const updatedEnttity = { ...ds, ...customLatLng };
                            this.saveMarkerLocation(updatedEnttity);
                            this.map.removeLayer(newMarker);
                            this.deleteMarker(ds.entityName);
                            this.createMarker(ds.entityName, updatedEnttity, this.datasources, this.options);
                        }
                        datasourcesList.append(dsItem);
                    });
                    const deleteBtn = document.createElement('a');
                    deleteBtn.appendChild(document.createTextNode('Delete position'));
                    deleteBtn.setAttribute('color', 'red');
                    deleteBtn.onclick = () => {
                        this.map.removeLayer(newMarker);
                    }
                    datasourcesList.append(deleteBtn);
                    const popup = L.popup();
                    popup.setContent(datasourcesList);
                    newMarker.bindPopup(popup).openPopup();
                }
                addMarker.setPosition('topright')
            }
            L.Control.AddMarker = L.Control.extend({
                onAdd(map) {
                    const img = L.DomUtil.create('img') as any;
                    img.src = `assets/add_location.svg`;
                    img.style.width = '32px';
                    img.style.height = '32px';
                    img.title = 'Drag and drop to add marker';
                    img.onclick = this.dragMarker;
                    img.draggable = true;
                    const draggableImg = new L.Draggable(img);
                    draggableImg.enable();
                    draggableImg.on('dragend', dragListener)
                    return img;
                },
                onRemove(map) {
                },
                dragMarker: this.dragMarker
            } as any);
            L.control.addMarker = (opts) => {
                return new L.Control.AddMarker(opts);
            }
            addMarker = L.control.addMarker({ position: 'topright' }).addTo(this.map);
        }
    }

    public setMap(map: L.Map) {
        this.map = map;
        if (this.options.useDefaultCenterPosition) {
            this.map.panTo(this.options.defaultCenterPosition);
            this.bounds = map.getBounds();
        }
        else this.bounds = new L.LatLngBounds(null, null);
        if (this.options.draggableMarker) {
            this.addMarkerControl();
        }
        this.map$.next(this.map);
    }

    public setDataSources(dataSources) {
        this.datasources = dataSources;
    }

    public saveMarkerLocation(e) {

    }

    createLatLng(lat: number, lng: number): L.LatLng {
        return L.latLng(lat, lng);
    }

    createBounds(): L.LatLngBounds {
        return this.map.getBounds();
    }

    extendBounds(bounds: L.LatLngBounds, polyline: L.Polyline) {
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

    fitBounds(bounds: LatLngBounds, useDefaultZoom = false, padding?: LatLngTuple) {
        if (bounds.isValid()) {
            if ((!this.options.fitMapBounds || useDefaultZoom) && this.options.defaultZoomLevel) {
                this.map.setZoom(this.options.defaultZoomLevel, { animate: false });
                this.map.panTo(bounds.getCenter(), { animate: false });
            } else {
                this.map.once('zoomend', () => {
                    if (!this.options.defaultZoomLevel && this.map.getZoom() > this.options.minZoomLevel) {
                        this.map.setZoom(this.options.minZoomLevel, { animate: false });
                    }
                });
                this.map.fitBounds(bounds, { padding: padding || [50, 50], animate: false });
            }
            this.bounds = this.bounds.extend(bounds);
        }
    }

    convertPosition(expression: object): L.LatLng {
        if (!expression) return null;
        const lat = expression[this.options.latKeyName];
        const lng = expression[this.options.lngKeyName];
        if (isNaN(lat) || isNaN(lng))
            return null;
        else
            return L.latLng(lat, lng) as L.LatLng;
    }

    convertToCustomFormat(position: L.LatLng): object {
        return {
            [this.options.latKeyName]: position.lat % 180,
            [this.options.lngKeyName]: position.lng % 180
        }
    }

    // Markers
    updateMarkers(markersData) {
        markersData.forEach(data => {
            if (this.convertPosition(data)) {
                if (data.rotationAngle || data.rotationAngle === 0) {
                    this.options.icon = L.divIcon({
                        html: `<div class="arrow" style="transform: translate(-10px, -10px) rotate(${data.rotationAngle}deg);"><div>`
                    })
                }
                else {
                    this.options.icon = null;
                }
                if (this.markers.get(data.entityName)) {
                    this.updateMarker(data.entityName, data, markersData, this.options)
                }
                else {
                    this.createMarker(data.entityName, data, markersData, this.options as MarkerSettings);
                }
            }
        });
    }

    dragMarker = (e, data?) => {
        if (e.type !== 'dragend') return;
        this.saveMarkerLocation({ ...data, ...this.convertToCustomFormat(e.target._latlng) });
    }

    private createMarker(key: string, data: FormattedData, dataSources: FormattedData[], settings: MarkerSettings) {
        this.ready$.subscribe(() => {
            const newMarker = new Marker(this.convertPosition(data), settings, data, dataSources, this.dragMarker);
            this.fitBounds(this.bounds.extend(newMarker.leafletMarker.getLatLng()), settings.draggableMarker && this.markers.size > 2);
            this.markers.set(key, newMarker);
            if (this.options.useClusterMarkers) {
                this.markersCluster.addLayer(newMarker.leafletMarker);
            }
            else {
                this.map.addLayer(newMarker.leafletMarker);
            }
        });
    }

    private updateMarker(key: string, data: FormattedData, dataSources: FormattedData[], settings: MarkerSettings) {
        const marker: Marker = this.markers.get(key);
        const location = this.convertPosition(data)
        if (!location.equals(marker.location)) {
            marker.updateMarkerPosition(location);
        }
        if (settings.showTooltip) {
            marker.updateMarkerTooltip(data);
        }
        if (settings.useClusterMarkers)
            this.markersCluster.refreshClusters()
        marker.setDataSources(data, dataSources);
        marker.updateMarkerIcon(settings);
    }

    deleteMarker(key: string) {
        let marker = this.markers.get(key)?.leafletMarker;
        if (marker) {
            this.map.removeLayer(marker);
            this.markers.delete(key);
            marker = null;
        }
    }

    // Polyline

    updatePolylines(polyData: FormattedData[][]) {
        polyData.forEach((data: FormattedData[]) => {
            if (data.length) {
                const dataSource = polyData.map(arr => arr[0]);
                if (this.polylines.get(data[0].entityName)) {
                    this.updatePolyline(data[0].entityName, data, dataSource, this.options);
                }
                else {
                    this.createPolyline(data, dataSource, this.options);
                }
            }
        })
    }

    createPolyline(data: FormattedData[], dataSources: FormattedData[], settings: PolylineSettings) {
        if (data.length)
            this.ready$.subscribe(() => {
                const poly = new Polyline(this.map,
                    data.map(el => this.convertPosition(el)).filter(el => !!el), data, dataSources, settings);
                const bounds = this.bounds.extend(poly.leafletPoly.getBounds());
                this.fitBounds(bounds)
                this.polylines.set(data[0].entityName, poly)
            });
    }

    updatePolyline(key: string, data: FormattedData[], dataSources: FormattedData[], settings: PolylineSettings) {
        this.ready$.subscribe(() => {
            this.polylines.get(key).updatePolyline(settings, data, dataSources);
        });
    }

    // Polygon

    updatePolygons(polyData: DatasourceData[]) {
        polyData.forEach((data: DatasourceData) => {
            if (data.data.length && data.dataKey.name === this.options.polygonKeyName) {
                if (typeof (data?.data[0][1]) === 'string') {
                    data.data = JSON.parse(data.data[0][1]) as LatLngTuple[];
                }
                if (this.polygons.get(data.datasource.entityName)) {
                    this.updatePolygon(data.datasource.entityName, data.data, polyData, this.options);
                }
                else {
                    this.createPolygon(data.datasource.entityName, data.data, polyData, this.options);
                }
            }
        });
    }

    createPolygon(key: string, data: LatLngTuple[], dataSources: DatasourceData[], settings: PolygonSettings) {
        this.ready$.subscribe(() => {
            const polygon = new Polygon(this.map, data, dataSources, settings);
            const bounds = this.bounds.extend(polygon.leafletPoly.getBounds());
            if (bounds.isValid()) {
                this.map.fitBounds(bounds);
                this.bounds = bounds;
            }
            this.polygons.set(key, polygon);
        });
    }

    updatePolygon(key: string, data: LatLngTuple[], dataSources: DatasourceData[], settings: PolygonSettings) {
        this.ready$.subscribe(() => {
            const poly = this.polygons.get(key);
            poly.updatePolygon(data, dataSources, settings);
            this.fitBounds(poly.leafletPoly.getBounds());
        });
    }
}
