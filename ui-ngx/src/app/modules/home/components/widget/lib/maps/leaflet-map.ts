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

import L, { FeatureGroup, LatLngBounds, LatLngTuple, markerClusterGroup, MarkerClusterGroupOptions, MarkerClusterGroup } from 'leaflet';

import 'leaflet-providers';
import 'leaflet.markercluster/dist/leaflet.markercluster';

import {
    FormattedData,
    MapSettings,
    MarkerSettings,
    PolygonSettings,
    PolylineSettings,
    UnitedMapSettings
} from './map-models';
import { Marker } from './markers';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Polyline } from './polyline';
import { Polygon } from './polygon';
import { createTooltip, parseArray, parseData, safeExecute } from '@home/components/widget/lib/maps/maps-utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { DatasourceData } from '@shared/models/widget.models';

export default abstract class LeafletMap {

    markers: Map<string, Marker> = new Map();
    polylines: Map<string, Polyline> = new Map();
    polygons: Map<string, Polygon> = new Map();
    map: L.Map;
    map$: BehaviorSubject<L.Map> = new BehaviorSubject(null);
    ready$: Observable<L.Map> = this.map$.pipe(filter(map => !!map));
    options: UnitedMapSettings;
    bounds: L.LatLngBounds;
    datasources: FormattedData[];
    markersCluster: MarkerClusterGroup;
    points: FeatureGroup;
    markersData: FormattedData[] = [];

    protected constructor(public ctx: WidgetContext,
                          public $container: HTMLElement,
                          options: UnitedMapSettings) {
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
            const clusteringSettings: MarkerClusterGroupOptions = {
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
            this.markersCluster = markerClusterGroup(clusteringSettings);
            this.ready$.subscribe(map => map.addLayer(this.markersCluster));
        }
    }

    addMarkerControl() {
        if (this.options.draggableMarker) {
            let mousePositionOnMap: L.LatLng;
            let addMarker: L.Control;
            this.map.on('mousemove', (e: L.LeafletMouseEvent) => {
                mousePositionOnMap = e.latlng;
            });
            const dragListener = (e: L.DragEndEvent) => {
                if (e.type === 'dragend' && mousePositionOnMap) {
                    const icon = new L.Icon.Default();
                    icon.options.shadowSize = [0, 0];
                    const newMarker = L.marker(mousePositionOnMap, { icon }).addTo(this.map);
                    const datasourcesList = document.createElement('div');
                    const customLatLng = this.convertToCustomFormat(mousePositionOnMap);
                    this.datasources.forEach(ds => {
                        const dsItem = document.createElement('p');
                        dsItem.appendChild(document.createTextNode(ds.entityName));
                        dsItem.setAttribute('style', 'font-size: 14px');
                        dsItem.onclick = () => {
                            const updatedEnttity = { ...ds, ...customLatLng };
                            this.saveMarkerLocation(updatedEnttity).subscribe(() => {
                              this.map.removeLayer(newMarker);
                              this.deleteMarker(ds.entityName);
                              this.createMarker(ds.entityName, updatedEnttity, this.datasources, this.options);
                            });
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
                onAdd() {
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
                onRemove() {
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

    public setDataSources(dataSources: FormattedData[]) {
        this.datasources = dataSources;
    }

    public saveMarkerLocation(_e: FormattedData, lat?: number, lng?: number): Observable<any> {
      return of(null);
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

    fitBounds(bounds: LatLngBounds, padding?: LatLngTuple) {
        if (bounds.isValid()) {
            this.bounds = !!this.bounds ? this.bounds.extend(bounds) : bounds;
            if (!this.options.fitMapBounds && this.options.defaultZoomLevel) {
                this.map.setZoom(this.options.defaultZoomLevel, { animate: false });
                if (this.options.useDefaultCenterPosition) {
                    this.map.panTo(this.options.defaultCenterPosition, { animate: false });
                }
                else {
                    this.map.panTo(this.bounds.getCenter());
                }
            } else {
                this.map.once('zoomend', () => {
                    let minZoom = this.options.minZoomLevel;
                    if (this.options.defaultZoomLevel) {
                      minZoom = Math.min(minZoom, this.options.defaultZoomLevel);
                    }
                    if (this.map.getZoom() > minZoom) {
                        this.map.setZoom(minZoom, { animate: false });
                    }
                });
                if (this.options.useDefaultCenterPosition) {
                    this.bounds = this.bounds.extend(this.options.defaultCenterPosition);
                }
                this.map.fitBounds(this.bounds, { padding: padding || [50, 50], animate: false });
                this.map.invalidateSize();
            }
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
            [this.options.latKeyName]: position.lat % 90,
            [this.options.lngKeyName]: position.lng % 180
        }
    }

    updateData(data: DatasourceData[], formattedData: FormattedData[], drawRoutes: boolean, showPolygon: boolean) {
      this.ready$.subscribe(() => {
        if (drawRoutes) {
          this.updatePolylines(parseArray(data), false);
        }
        if (showPolygon) {
          this.updatePolygons(formattedData, false);
        }
        this.updateMarkers(formattedData, false);
        this.updateBoundsInternal(drawRoutes, showPolygon);
      });
    }

  private updateBoundsInternal(drawRoutes: boolean, showPolygon: boolean) {
    const bounds = new L.LatLngBounds(null, null);
    if (drawRoutes) {
      this.polylines.forEach((polyline) => {
        bounds.extend(polyline.leafletPoly.getBounds());
      });
    }
    if (showPolygon) {
      this.polygons.forEach((polygon) => {
        bounds.extend(polygon.leafletPoly.getBounds());
      });
    }
    this.markers.forEach((marker) => {
      bounds.extend(marker.leafletMarker.getLatLng());
    });

    const mapBounds = this.map.getBounds();
    if (bounds.isValid() && (!this.bounds || !mapBounds.contains(bounds))) {
      this.bounds = bounds;
      this.fitBounds(bounds);
    }
  }

  // Markers
    updateMarkers(markersData: FormattedData[], updateBounds = true, callback?) {
        const rawMarkers = markersData.filter(mdata => !!this.convertPosition(mdata));
        this.ready$.subscribe(() => {
          const keys: string[] = [];
          rawMarkers.forEach(data => {
            if (data.rotationAngle || data.rotationAngle === 0) {
              const currentImage = this.options.useMarkerImageFunction ?
                safeExecute(this.options.markerImageFunction,
                  [data, this.options.markerImages, markersData, data.dsIndex]) : this.options.currentImage;
              const style = currentImage ? 'background-image: url(' + currentImage.url + ');' : '';
              this.options.icon = L.divIcon({
                html: `<div class="arrow"
                     style="transform: translate(-10px, -10px)
                     rotate(${data.rotationAngle}deg);
                     ${style}"><div>`
              });
            } else {
              this.options.icon = null;
            }
            if (this.markers.get(data.entityName)) {
              this.updateMarker(data.entityName, data, markersData, this.options)
            } else {
              this.createMarker(data.entityName, data, markersData, this.options as MarkerSettings, updateBounds, callback);
            }
            keys.push(data.entityName);
          });
          const toDelete: string[] = [];
          this.markers.forEach((v, mKey) => {
            if (!keys.includes(mKey)) {
              toDelete.push(mKey);
            }
          });
          toDelete.forEach((key) => {
            this.deleteMarker(key);
          });
          this.markersData = markersData;
        });
    }

    dragMarker = (e, data = {} as FormattedData) => {
        if (e.type !== 'dragend') return;
        this.saveMarkerLocation({ ...data, ...this.convertToCustomFormat(e.target._latlng) }).subscribe();
    }

    private createMarker(key: string, data: FormattedData, dataSources: FormattedData[], settings: MarkerSettings,
                         updateBounds = true, callback?) {
        const newMarker = new Marker(this.convertPosition(data), settings, data, dataSources, this.dragMarker);
        if (callback)
            newMarker.leafletMarker.on('click', () => { callback(data, true) });
        if (this.bounds && updateBounds)
            this.fitBounds(this.bounds.extend(newMarker.leafletMarker.getLatLng()));
        this.markers.set(key, newMarker);
        if (this.options.useClusterMarkers) {
            this.markersCluster.addLayer(newMarker.leafletMarker);
        }
        else {
            this.map.addLayer(newMarker.leafletMarker);
        }
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
        if (settings.useClusterMarkers) {
          this.markersCluster.refreshClusters()
        }
        marker.setDataSources(data, dataSources);
        marker.updateMarkerIcon(settings);
    }

    deleteMarker(key: string) {
        let marker = this.markers.get(key)?.leafletMarker;
        if (marker) {
            if (this.options.useClusterMarkers) {
              this.markersCluster.removeLayer(marker);
            } else {
              this.map.removeLayer(marker);
            }
            this.markers.delete(key);
            marker = null;
        }
    }

    updatePoints(pointsData: FormattedData[], getTooltip: (point: FormattedData, setTooltip?: boolean) => string) {
        this.map$.subscribe(map => {
            if (this.points) {
                map.removeLayer(this.points);
            }
            this.points = new FeatureGroup();
            pointsData.filter(pdata => !!this.convertPosition(pdata)).forEach(data => {
                const point = L.circleMarker(this.convertPosition(data), {
                    color: this.options.pointColor,
                    radius: this.options.pointSize
                });
                if (!this.options.pointTooltipOnRightPanel) {
                    point.on('click', () => getTooltip(data));
                }
                else {
                    createTooltip(point, this.options, data.$datasource, getTooltip(data, false));
                }
                this.points.addLayer(point);
            });
            map.addLayer(this.points);
        });
    }

    // Polyline

    updatePolylines(polyData: FormattedData[][], updateBounds = true, data?: FormattedData) {
        const keys: string[] = [];
        polyData.forEach((dataSource: FormattedData[]) => {
            data = data || dataSource[0];
            if (dataSource.length && data.entityName === dataSource[0].entityName) {
                if (this.polylines.get(data.entityName)) {
                    this.updatePolyline(data, dataSource, this.options, updateBounds);
                } else {
                    this.createPolyline(data, dataSource, this.options, updateBounds);
                }
                keys.push(data.entityName);
            }
        });
        const toDelete: string[] = [];
        this.polylines.forEach((v, mKey) => {
          if (!keys.includes(mKey)) {
            toDelete.push(mKey);
          }
        });
        toDelete.forEach((key) => {
          this.removePolyline(key);
        });
    }

    createPolyline(data: FormattedData, dataSources: FormattedData[], settings: PolylineSettings, updateBounds = true) {
        this.ready$.subscribe(() => {
            const poly = new Polyline(this.map,
                dataSources.map(el => this.convertPosition(el)).filter(el => !!el), data, dataSources, settings);
            if (updateBounds) {
              const bounds = poly.leafletPoly.getBounds();
              this.fitBounds(bounds);
            }
            this.polylines.set(data.entityName, poly);
        });
    }

    updatePolyline(data: FormattedData, dataSources: FormattedData[], settings: PolylineSettings, updateBounds = true) {
        this.ready$.subscribe(() => {
            const poly = this.polylines.get(data.entityName);
            const oldBounds = poly.leafletPoly.getBounds();
            poly.updatePolyline(dataSources.map(el => this.convertPosition(el)).filter(el => !!el), data, dataSources, settings);
            const newBounds = poly.leafletPoly.getBounds();
            if (updateBounds && oldBounds.toBBoxString() !== newBounds.toBBoxString()) {
                this.fitBounds(newBounds);
            }
        });
    }

    removePolyline(name: string) {
        const poly = this.polylines.get(name);
        if (poly) {
            this.map.removeLayer(poly.leafletPoly);
            if (poly.polylineDecorator) {
                this.map.removeLayer(poly.polylineDecorator);
            }
            this.polylines.delete(name);
        }
    }

    // Polygon

    updatePolygons(polyData: FormattedData[], updateBounds = true) {
        const keys: string[] = [];
        polyData.forEach((data: FormattedData) => {
            if (data && data.hasOwnProperty(this.options.polygonKeyName)) {
                if (typeof (data[this.options.polygonKeyName]) === 'string') {
                    data[this.options.polygonKeyName] = JSON.parse(data[this.options.polygonKeyName]) as LatLngTuple[];
                }
                if (this.polygons.get(data.entityName)) {
                    this.updatePolygon(data, polyData, this.options, updateBounds);
                } else {
                    this.createPolygon(data, polyData, this.options, updateBounds);
                }
                keys.push(data.entityName);
            }
        });
        const toDelete: string[] = [];
        this.polygons.forEach((v, mKey) => {
          if (!keys.includes(mKey)) {
            toDelete.push(mKey);
          }
        });
        toDelete.forEach((key) => {
          this.removePolygon(key);
        });
    }

    createPolygon(polyData: FormattedData, dataSources: FormattedData[], settings: PolygonSettings, updateBounds = true) {
        this.ready$.subscribe(() => {
            const polygon = new Polygon(this.map, polyData, dataSources, settings);
            if (updateBounds) {
              const bounds = polygon.leafletPoly.getBounds();
              this.fitBounds(bounds);
            }
            this.polygons.set(polyData.entityName, polygon);
        });
    }

    updatePolygon(polyData: FormattedData, dataSources: FormattedData[], settings: PolygonSettings, updateBounds = true) {
        this.ready$.subscribe(() => {
            const poly = this.polygons.get(polyData.entityName);
            const oldBounds = poly.leafletPoly.getBounds();
            poly.updatePolygon(polyData, dataSources, settings);
            const newBounds = poly.leafletPoly.getBounds();
            if (updateBounds && oldBounds.toBBoxString() !== newBounds.toBBoxString()) {
                this.fitBounds(newBounds);
            }
        });
    }

    removePolygon(name: string) {
      const poly = this.polygons.get(name);
      if (poly) {
        this.map.removeLayer(poly.leafletPoly);
        this.polygons.delete(name);
      }
    }
}
