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

import L, {
  FeatureGroup,
  Icon,
  LatLngBounds,
  LatLngTuple,
  markerClusterGroup,
  MarkerClusterGroup,
  MarkerClusterGroupOptions
} from 'leaflet';
import tinycolor from 'tinycolor2';
import 'leaflet-providers';
import 'leaflet.markercluster/dist/leaflet.markercluster';

import {
  defaultSettings,
  FormattedData,
  MapProviders,
  MapSettings,
  MarkerSettings,
  PolygonSettings,
  PolylineSettings,
  ReplaceInfo,
  UnitedMapSettings
} from './map-models';
import { Marker } from './markers';
import { Observable, of } from 'rxjs';
import { Polyline } from './polyline';
import { Polygon } from './polygon';
import {
  createLoadingDiv,
  createTooltip,
  parseArray,
  parseData,
  safeExecute
} from '@home/components/widget/lib/maps/maps-utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { deepClone, isDefinedAndNotNull, isEmptyStr, isString } from '@core/utils';

export default abstract class LeafletMap {

    markers: Map<string, Marker> = new Map();
    polylines: Map<string, Polyline> = new Map();
    polygons: Map<string, Polygon> = new Map();
    map: L.Map;
    options: UnitedMapSettings;
    bounds: L.LatLngBounds;
    datasources: FormattedData[];
    markersCluster: MarkerClusterGroup;
    points: FeatureGroup;
    markersData: FormattedData[] = [];
    polygonsData: FormattedData[] = [];
    defaultMarkerIconInfo: { size: number[], icon: Icon };
    loadingDiv: JQuery<HTMLElement>;
    loading = false;
    replaceInfoLabelMarker: Array<ReplaceInfo> = [];
    markerLabelText: string;
    replaceInfoTooltipMarker: Array<ReplaceInfo> = [];
    markerTooltipText: string;
    drawRoutes: boolean;
    showPolygon: boolean;
    updatePending = false;
    addMarkers: L.Marker[] = [];
    addPolygons: L.Polygon[] = [];

    protected constructor(public ctx: WidgetContext,
                          public $container: HTMLElement,
                          options: UnitedMapSettings) {
        this.options = options;
    }

    public initSettings(options: MapSettings) {
        this.options.tinyColor = tinycolor(this.options.color || defaultSettings.color);
        const { useClusterMarkers,
            zoomOnClick,
            showCoverageOnHover,
            removeOutsideVisibleBounds,
            animate,
            chunkedLoading,
            maxClusterRadius,
            maxZoom }: MapSettings = options;
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
                    const icon = L.icon({
                      iconRetinaUrl: 'marker-icon-2x.png',
                      iconUrl: 'marker-icon.png',
                      shadowUrl: 'marker-shadow.png',
                      iconSize: [25, 41],
                      iconAnchor: [12, 41],
                      popupAnchor: [1, -34],
                      tooltipAnchor: [16, -28],
                      shadowSize: [41, 41]
                    });
                    const customLatLng = this.convertToCustomFormat(mousePositionOnMap);
                    mousePositionOnMap.lat = customLatLng[this.options.latKeyName];
                    mousePositionOnMap.lng = customLatLng[this.options.lngKeyName];

                    const newMarker = L.marker(mousePositionOnMap, { icon }).addTo(this.map);
                    this.addMarkers.push(newMarker);
                    const datasourcesList = document.createElement('div');
                    const header = document.createElement('p');
                    header.appendChild(document.createTextNode('Select entity:'));
                    header.setAttribute('style', 'font-size: 14px; margin: 8px 0');
                    datasourcesList.append(header);
                    this.datasources.forEach(ds => {
                        const dsItem = document.createElement('p');
                        dsItem.appendChild(document.createTextNode(ds.entityName));
                        dsItem.setAttribute('style', 'font-size: 14px; margin: 8px 0; cursor: pointer');
                        dsItem.onclick = () => {
                            const updatedEnttity = { ...ds, ...customLatLng };
                            this.saveMarkerLocation(updatedEnttity).subscribe(() => {
                              this.map.removeLayer(newMarker);
                              const markerIndex = this.addMarkers.indexOf(newMarker);
                              if (markerIndex > -1) {
                                this.addMarkers.splice(markerIndex, 1);
                              }
                              this.deleteMarker(ds.entityName);
                              this.createMarker(ds.entityName, updatedEnttity, this.datasources, this.options);
                            });
                        };
                        datasourcesList.append(dsItem);
                    });
                    datasourcesList.append(document.createElement('br'));
                    const deleteBtn = document.createElement('a');
                    deleteBtn.appendChild(document.createTextNode('Discard changes'));
                    deleteBtn.onclick = () => {
                        this.map.removeLayer(newMarker);
                        const markerIndex = this.addMarkers.indexOf(newMarker);
                        if (markerIndex > -1) {
                          this.addMarkers.splice(markerIndex, 1);
                        }
                    };
                    datasourcesList.append(deleteBtn);
                    const popup = L.popup();
                    popup.setContent(datasourcesList);
                    newMarker.bindPopup(popup).openPopup();
                }
                addMarker.setPosition('topright');
            };
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
                    draggableImg.on('dragend', dragListener);
                    return img;
                },
                onRemove() {
                },
                dragMarker: this.dragMarker
            } as any);
            L.control.addMarker = (opts) => {
                return new L.Control.AddMarker(opts);
            };
            addMarker = L.control.addMarker({ position: 'topright' }).addTo(this.map);
        }
    }

  addPolygonControl() {
    if (this.options.showPolygon && this.options.editablePolygon) {
      let mousePositionOnMap: L.LatLng[];
      let addPolygon: L.Control;
      this.map.on('mousemove', (e: L.LeafletMouseEvent) => {
        const polygonOffset = this.options.provider === MapProviders.image ? 10 : 0.01;
        const latlng1 = e.latlng;
        const latlng2 = L.latLng(e.latlng.lat, e.latlng.lng + polygonOffset);
        const latlng3 = L.latLng(e.latlng.lat - polygonOffset, e.latlng.lng);
        mousePositionOnMap = [latlng1, latlng2, latlng3];
      });
      const dragListener = (e: L.DragEndEvent) => {
        if (e.type === 'dragend' && mousePositionOnMap) {
          const newPolygon = L.polygon(mousePositionOnMap).addTo(this.map);
          this.addPolygons.push(newPolygon);
          const datasourcesList = document.createElement('div');
          const customLatLng = {[this.options.polygonKeyName]: this.convertToPolygonFormat(mousePositionOnMap)};
          const header = document.createElement('p');
          header.appendChild(document.createTextNode('Select entity:'));
          header.setAttribute('style', 'font-size: 14px; margin: 8px 0');
          datasourcesList.append(header);
          this.datasources.forEach(ds => {
            const dsItem = document.createElement('p');
            dsItem.appendChild(document.createTextNode(ds.entityName));
            dsItem.setAttribute('style', 'font-size: 14px; margin: 8px 0; cursor: pointer');
            dsItem.onclick = () => {
              const updatedEnttity = { ...ds, ...customLatLng };
              this.savePolygonLocation(updatedEnttity).subscribe(() => {
                this.map.removeLayer(newPolygon);
                const polygonIndex = this.addPolygons.indexOf(newPolygon);
                if (polygonIndex > -1) {
                  this.addPolygons.splice(polygonIndex, 1);
                }
                this.deletePolygon(ds.entityName);
              });
            };
            datasourcesList.append(dsItem);
          });
          datasourcesList.append(document.createElement('br'));
          const deleteBtn = document.createElement('a');
          deleteBtn.appendChild(document.createTextNode('Discard changes'));
          deleteBtn.onclick = () => {
            this.map.removeLayer(newPolygon);
            const polygonIndex = this.addPolygons.indexOf(newPolygon);
            if (polygonIndex > -1) {
              this.addPolygons.splice(polygonIndex, 1);
            }
          };
          datasourcesList.append(deleteBtn);
          const popup = L.popup();
          popup.setContent(datasourcesList);
          newPolygon.bindPopup(popup).openPopup();
        }
        addPolygon.setPosition('topright');
      };
      L.Control.AddPolygon = L.Control.extend({
        onAdd() {
          const img = L.DomUtil.create('img') as any;
          img.src = `assets/add_polygon.svg`;
          img.style.width = '32px';
          img.style.height = '32px';
          img.title = 'Drag and drop to add Polygon';
          img.onclick = this.dragPolygonVertex;
          img.draggable = true;
          const draggableImg = new L.Draggable(img);
          draggableImg.enable();
          draggableImg.on('dragend', dragListener);
          return img;
        },
        onRemove() {
        },
        dragPolygonVertex: this.dragPolygonVertex
      } as any);
      L.control.addPolygon = (opts) => {
        return new L.Control.AddPolygon(opts);
      };
      addPolygon = L.control.addPolygon({ position: 'topright' }).addTo(this.map);
    }
  }

    public setLoading(loading: boolean) {
      if (this.loading !== loading) {
        this.loading = loading;
        if (this.loading) {
          if (!this.loadingDiv) {
            this.loadingDiv = createLoadingDiv(this.ctx.translate.instant('common.loading'));
          }
          this.$container.append(this.loadingDiv[0]);
        } else {
          if (this.loadingDiv) {
            this.loadingDiv.remove();
          }
        }
      }
    }

    public setMap(map: L.Map) {
        this.map = map;
        if (this.options.useDefaultCenterPosition) {
          this.map.panTo(this.options.defaultCenterPosition);
          this.bounds = map.getBounds();
        } else {
          this.bounds = new L.LatLngBounds(null, null);
        }
        if (this.options.disableScrollZooming) {
          this.map.scrollWheelZoom.disable();
        }
        if (this.options.draggableMarker) {
          this.addMarkerControl();
        }
        if (this.options.editablePolygon) {
          this.addPolygonControl();
        }
        if (this.options.useClusterMarkers) {
          this.map.addLayer(this.markersCluster);
        }
        if (this.updatePending) {
          this.updatePending = false;
          this.updateData(this.drawRoutes, this.showPolygon);
        }
    }

    public saveMarkerLocation(datasource: FormattedData, lat?: number, lng?: number): Observable<any> {
      return of(null);
    }

    public savePolygonLocation(datasource: FormattedData, coordinates?: Array<[number, number]>): Observable<any> {
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
                      minZoom = Math.max(minZoom, this.options.defaultZoomLevel);
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
      if (!expression) {
        return null;
      }
      const lat = expression[this.options.latKeyName];
      const lng = expression[this.options.lngKeyName];
      if (!isDefinedAndNotNull(lat) || isString(lat) || isNaN(lat) || !isDefinedAndNotNull(lng) || isString(lng) || isNaN(lng)) {
        return null;
      }
      return L.latLng(lat, lng) as L.LatLng;
    }

    convertPositionPolygon(expression: (LatLngTuple | LatLngTuple[] | LatLngTuple[][])[]) {
          return (expression).map((el) => {
            if (!Array.isArray(el[0]) && el.length === 2) {
              return el;
            } else if (Array.isArray(el) && el.length) {
              return this.convertPositionPolygon(el as LatLngTuple[] | LatLngTuple[][]);
            } else {
              return null;
            }
        }).filter(el => !!el);
    }

    convertToCustomFormat(position: L.LatLng): object {
      if (position.lng > 180) {
        position.lng = 180;
      } else if (position.lng < -180) {
        position.lng = -180;
      }
      return {
        [this.options.latKeyName]: position.lat,
        [this.options.lngKeyName]: position.lng
      };
    }

    convertToPolygonFormat(points: Array<any>): Array<any> {
      if (points.length) {
        return points.map(point => {
          if (point.length) {
            return this.convertToPolygonFormat(point);
          } else {
            return [point.lat, point.lng];
          }
        });
      } else {
        return [];
      }
    }

    convertPolygonToCustomFormat(expression: any[][]): object {
      return {
        [this.options.polygonKeyName] : this.convertToPolygonFormat(expression)
      };
    }

    updateData(drawRoutes: boolean, showPolygon: boolean) {
      this.drawRoutes = drawRoutes;
      this.showPolygon = showPolygon;
      if (this.map) {
        const data = this.ctx.data;
        const formattedData = parseData(this.ctx.data);
        if (drawRoutes) {
          this.updatePolylines(parseArray(data), false);
        }
        if (showPolygon) {
          this.updatePolygons(formattedData, false);
        }
        this.updateMarkers(formattedData, false);
        this.updateBoundsInternal();
        if (this.options.draggableMarker || this.options.editablePolygon) {
          this.datasources = formattedData;
        }
      } else {
        this.updatePending = true;
      }
    }

  private updateBoundsInternal() {
    const bounds = new L.LatLngBounds(null, null);
    if (this.drawRoutes) {
      this.polylines.forEach((polyline) => {
        bounds.extend(polyline.leafletPoly.getBounds());
      });
    }
    if (this.showPolygon) {
      this.polygons.forEach((polygon) => {
        bounds.extend(polygon.leafletPoly.getBounds());
      });
    }
    if ((this.options as MarkerSettings).useClusterMarkers && this.markersCluster.getBounds().isValid()) {
      bounds.extend(this.markersCluster.getBounds());
    } else {
      this.markers.forEach((marker) => {
        bounds.extend(marker.leafletMarker.getLatLng());
      });
    }

    const mapBounds = this.map.getBounds();
    if (bounds.isValid() && (!this.bounds || !this.bounds.isValid() || !this.bounds.equals(bounds)
        && this.options.fitMapBounds ? !mapBounds.contains(bounds) : false)) {
      this.bounds = bounds;
      this.fitBounds(bounds);
    }
  }

  // Markers
    updateMarkers(markersData: FormattedData[], updateBounds = true, callback?) {
      const rawMarkers = markersData.filter(mdata => !!this.convertPosition(mdata));
      const toDelete = new Set(Array.from(this.markers.keys()));
      const createdMarkers: Marker[] = [];
      const updatedMarkers: Marker[] = [];
      const deletedMarkers: Marker[] = [];
      let m: Marker;
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
          m = this.updateMarker(data.entityName, data, markersData, this.options);
          if (m) {
            updatedMarkers.push(m);
          }
        } else {
          m = this.createMarker(data.entityName, data, markersData, this.options as MarkerSettings, updateBounds, callback);
          if (m) {
            createdMarkers.push(m);
          }
        }
        toDelete.delete(data.entityName);
      });
      toDelete.forEach((key) => {
        m = this.deleteMarker(key);
        if (m) {
          deletedMarkers.push(m);
        }
      });
      this.markersData = markersData;
      if ((this.options as MarkerSettings).useClusterMarkers) {
        if (createdMarkers.length) {
          this.markersCluster.addLayers(createdMarkers.map(marker => marker.leafletMarker));
        }
        if (updatedMarkers.length) {
          this.markersCluster.refreshClusters(updatedMarkers.map(marker => marker.leafletMarker));
        }
        if (deletedMarkers.length) {
          this.markersCluster.removeLayers(deletedMarkers.map(marker => marker.leafletMarker));
        }
      }
    }

    dragMarker = (e, data = {} as FormattedData) => {
        if (e.type !== 'dragend') {
          return;
        }
        this.saveMarkerLocation({ ...data, ...this.convertToCustomFormat(e.target._latlng) }).subscribe();
    }

    private createMarker(key: string, data: FormattedData, dataSources: FormattedData[], settings: MarkerSettings,
                         updateBounds = true, callback?): Marker {
      const newMarker = new Marker(this, this.convertPosition(data), settings, data, dataSources, this.dragMarker);
      if (callback) {
        newMarker.leafletMarker.on('click', () => {
          callback(data, true);
        });
      }
      if (this.bounds && updateBounds && !(this.options as MarkerSettings).useClusterMarkers) {
        this.fitBounds(this.bounds.extend(newMarker.leafletMarker.getLatLng()));
      }
      this.markers.set(key, newMarker);
      if (!this.options.useClusterMarkers) {
        this.map.addLayer(newMarker.leafletMarker);
      }
      return newMarker;
    }

    private updateMarker(key: string, data: FormattedData, dataSources: FormattedData[], settings: MarkerSettings): Marker {
        const marker: Marker = this.markers.get(key);
        const location = this.convertPosition(data);
        marker.updateMarkerPosition(location);
        if (settings.showTooltip) {
            marker.updateMarkerTooltip(data);
        }
        marker.setDataSources(data, dataSources);
        marker.updateMarkerIcon(settings);
        return marker;
    }

    deleteMarker(key: string): Marker {
      const marker = this.markers.get(key);
      const leafletMarker = marker?.leafletMarker;
      if (leafletMarker) {
          if (!this.options.useClusterMarkers) {
            this.map.removeLayer(leafletMarker);
          }
          this.markers.delete(key);
      }
      return marker;
    }

    deletePolygon(key: string) {
      const polygon = this.polygons.get(key)?.leafletPoly;
      if (polygon) {
        this.map.removeLayer(polygon);
        this.polygons.delete(key);
      }
      return polygon;
    }

    updatePoints(pointsData: FormattedData[], getTooltip: (point: FormattedData, setTooltip?: boolean) => string) {
      if (this.points) {
          this.map.removeLayer(this.points);
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
      this.map.addLayer(this.points);
    }

    // Polyline

    updatePolylines(polyData: FormattedData[][], updateBounds = true, activePolyline?: FormattedData) {
        const keys: string[] = [];
        polyData.forEach((dataSource: FormattedData[]) => {
            const data = activePolyline || dataSource[0];
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
        const poly = new Polyline(this.map,
            dataSources.map(el => this.convertPosition(el)).filter(el => !!el), data, dataSources, settings);
        if (updateBounds) {
          const bounds = poly.leafletPoly.getBounds();
          this.fitBounds(bounds);
        }
        this.polylines.set(data.entityName, poly);
    }

    updatePolyline(data: FormattedData, dataSources: FormattedData[], settings: PolylineSettings, updateBounds = true) {
        const poly = this.polylines.get(data.entityName);
        const oldBounds = poly.leafletPoly.getBounds();
        poly.updatePolyline(dataSources.map(el => this.convertPosition(el)).filter(el => !!el), data, dataSources, settings);
        const newBounds = poly.leafletPoly.getBounds();
        if (updateBounds && oldBounds.toBBoxString() !== newBounds.toBBoxString()) {
            this.fitBounds(newBounds);
        }
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
    this.polygonsData = deepClone(polyData);
    polyData.forEach((data: FormattedData) => {
      if (data && isDefinedAndNotNull(data[this.options.polygonKeyName]) && !isEmptyStr(data[this.options.polygonKeyName])) {
        if (isString((data[this.options.polygonKeyName]))) {
          data[this.options.polygonKeyName] = JSON.parse(data[this.options.polygonKeyName]);
        }
        data[this.options.polygonKeyName] = this.convertPositionPolygon(data[this.options.polygonKeyName]);

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

  dragPolygonVertex = (e?, data = {} as FormattedData) => {
    if (e === undefined || (e.type !== 'editable:vertex:dragend' && e.type !== 'editable:vertex:deleted')) {
      return;
    }
    this.savePolygonLocation({ ...data, ...this.convertPolygonToCustomFormat(e.layer._latlngs) }).subscribe();
  }

    createPolygon(polyData: FormattedData, dataSources: FormattedData[], settings: PolygonSettings, updateBounds = true) {
      const polygon = new Polygon(this.map, polyData, dataSources, settings, this.dragPolygonVertex);
      if (updateBounds) {
        const bounds = polygon.leafletPoly.getBounds();
        this.fitBounds(bounds);
      }
      this.polygons.set(polyData.entityName, polygon);
    }

    updatePolygon(polyData: FormattedData, dataSources: FormattedData[], settings: PolygonSettings, updateBounds = true) {
      const poly = this.polygons.get(polyData.entityName);
      const oldBounds = poly.leafletPoly.getBounds();
      poly.updatePolygon(polyData, dataSources, settings);
      const newBounds = poly.leafletPoly.getBounds();
      if (updateBounds && oldBounds.toBBoxString() !== newBounds.toBBoxString()) {
          this.fitBounds(newBounds);
      }
    }

    removePolygon(name: string) {
      const poly = this.polygons.get(name);
      if (poly) {
        this.map.removeLayer(poly.leafletPoly);
        this.polygons.delete(name);
      }
    }
}
