///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import {
  DEFAULT_ZOOM_LEVEL,
  defaultGeoMapSettings,
  GeoMapSettings,
  latLngPointToBounds,
  MapZoomAction,
  TbCircleData,
  TbPolygonCoordinate,
  TbPolygonCoordinates,
  TbPolygonRawCoordinate,
  TbPolygonRawCoordinates,
  TbPolylineCoordinate,
  TbPolylineCoordinates,
  TbPolylineRawCoordinate,
  TbPolylineRawCoordinates
} from '@shared/models/widget/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { forkJoin, Observable, of } from 'rxjs';
import L from 'leaflet';
import { map, tap } from 'rxjs/operators';
import { TbMapLayer } from '@home/components/widget/lib/maps/map-layer';
import { TbMap } from '@home/components/widget/lib/maps/map';

export class TbGeoMap extends TbMap<GeoMapSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<GeoMapSettings>,
              protected containerElement: HTMLElement) {
    super(ctx, inputSettings, containerElement);
  }

  protected defaultSettings(): GeoMapSettings {
    return defaultGeoMapSettings;
  }

  protected createMap(): Observable<L.Map> {
    const map = L.map(this.mapElement, {
      scrollWheelZoom: this.settings.zoomActions.includes(MapZoomAction.scroll),
      doubleClickZoom: this.settings.zoomActions.includes(MapZoomAction.doubleClick),
      zoomControl: this.settings.zoomActions.includes(MapZoomAction.controlButtons),
      zoom: this.settings.defaultZoomLevel || DEFAULT_ZOOM_LEVEL,
      center: this.defaultCenterPosition
    }).setView(this.defaultCenterPosition, this.settings.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
    return of(map);
  }

  protected onResize(): void {}

  protected fitBounds(bounds: L.LatLngBounds) {
    if (bounds.isValid()) {
      if (!this.settings.fitMapBounds && this.settings.defaultZoomLevel) {
        this.map.setZoom(this.settings.defaultZoomLevel, { animate: false });
        if (this.settings.useDefaultCenterPosition) {
          this.map.panTo(this.defaultCenterPosition, { animate: false });
        }
        else {
          this.map.panTo(bounds.getCenter());
        }
      } else {
        this.map.once('zoomend', () => {
          let minZoom = this.settings.minZoomLevel;
          if (this.settings.defaultZoomLevel) {
            minZoom = Math.max(minZoom, this.settings.defaultZoomLevel);
          }
          if (this.map.getZoom() > minZoom) {
            this.map.setZoom(minZoom, { animate: false });
          }
        });
        if (this.settings.useDefaultCenterPosition) {
          bounds = bounds.extend(this.defaultCenterPosition);
        }
        this.map.fitBounds(bounds, { padding: [50, 50], animate: false });
        this.map.invalidateSize();
      }
    }
  }

  protected doSetupControls(): Observable<any> {
    return this.loadLayers().pipe(
      tap((layers: L.TB.LayerData[]) => {
        if (layers.length) {
          const layer = layers[0];
          layer.layer.addTo(this.map);
          this.map.attributionControl.setPrefix(layer.attributionPrefix);
          if (layers.length > 1) {
            const sidebar = this.getSidebar();
            L.TB.layers({
              layers,
              sidebar,
              position: this.settings.controlsPosition,
              uiClass: 'tb-layers',
              paneTitle: this.ctx.translate.instant('widgets.maps.layer.map-layers'),
              buttonTitle: this.ctx.translate.instant('widgets.maps.layer.layers'),
            }).addTo(this.map);
          }
        }
      })
    );

  }

  private loadLayers(): Observable<L.TB.LayerData[]> {
    const layers = this.settings.layers.map(settings => TbMapLayer.fromSettings(this.ctx, settings));
    return forkJoin(layers.map(layer => layer.loadLayer(this.map))).pipe(
      map((layersData) => {
        return layersData.filter(l => l !== null);
      })
    );
  }

  public locationDataToLatLng(position: {x: number; y: number}): L.LatLng {
    return L.latLng(position.x, position.y) as L.LatLng;
  }

  public latLngToLocationData(position: L.LatLng): {x: number; y: number} {
    position = position ? latLngPointToBounds(position, this.southWest, this.northEast, 0) : {lat: null, lng: null} as L.LatLng;
    return {
      x: position.lat,
      y: position.lng
    }
  }

  public polygonDataToCoordinates(expression: TbPolygonRawCoordinates): TbPolygonRawCoordinates {
    return (expression).map((el: TbPolygonRawCoordinate) => {
      if (!Array.isArray(el[0]) && !Array.isArray(el[1]) && el.length === 2) {
        return el;
      } else if (Array.isArray(el) && el.length) {
        return this.polygonDataToCoordinates(el as TbPolygonRawCoordinates) as TbPolygonRawCoordinate;
      } else {
        return null;
      }
    }).filter(el => !!el);
  }

  public coordinatesToPolygonData(coordinates: TbPolygonCoordinates): TbPolygonRawCoordinates {
    if (coordinates.length) {
      return coordinates.map((point: TbPolygonCoordinate) => {
        if (Array.isArray(point)) {
          return this.coordinatesToPolygonData(point) as TbPolygonRawCoordinate;
        } else {
          const convertPoint = latLngPointToBounds(point, this.southWest, this.northEast);
          return [convertPoint.lat, convertPoint.lng];
        }
      });
    }
    return [];
  }

  public circleDataToCoordinates(circle: TbCircleData): TbCircleData {
    const centerPoint = latLngPointToBounds(new L.LatLng(circle.latitude, circle.longitude), this.southWest, this.northEast);
    circle.latitude = centerPoint.lat;
    circle.longitude = centerPoint.lng;
    return circle;
  }

  public coordinatesToCircleData(center: L.LatLng, radius: number): TbCircleData {
    let circleData: TbCircleData = null;
    if (center) {
      const position = latLngPointToBounds(center, this.southWest, this.northEast);
      circleData = {
        latitude: position.lat,
        longitude: position.lng,
        radius
      };
    }
    return circleData;
  }

  public polylineDataToCoordinates(expression: TbPolylineRawCoordinates): TbPolylineRawCoordinates {
    return (expression).map((el: TbPolylineRawCoordinate) => {
      if (!Array.isArray(el[0]) && !Array.isArray(el[1]) && el.length === 2) {
        return el;
      } else if (Array.isArray(el) && el.length) {
        return this.polylineDataToCoordinates(el as TbPolylineRawCoordinates) as TbPolylineRawCoordinate;
      } else {
        return null;
      }
    }).filter(el => !!el);
  }

  public coordinatesToPolylineData(coordinates: TbPolylineCoordinates): TbPolylineRawCoordinates {
    if (coordinates.length) {
      return coordinates.map((point: TbPolylineCoordinate) => {
        if (Array.isArray(point)) {
          return this.coordinatesToPolylineData(point) as TbPolylineRawCoordinate;
        } else {
          const convertPoint = latLngPointToBounds(point, this.southWest, this.northEast);
          return [convertPoint.lat, convertPoint.lng];
        }
      });
    }
    return [];
  }
}
