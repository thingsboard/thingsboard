///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  calculateNewPointCoordinate,
  defaultImageMapSettings,
  defaultImageMapSourceSettings,
  ImageMapSettings,
  imageMapSourceSettingsToDatasource,
  ImageSourceType,
  loadImageWithAspect,
  MapZoomAction,
  TbCircleData,
  TbPolygonCoordinate,
  TbPolygonCoordinates,
  TbPolygonRawCoordinate,
  TbPolygonRawCoordinates, TbPolylineCoordinate, TbPolylineCoordinates, TbPolylineRawCoordinate,
  TbPolylineRawCoordinates
} from '@shared/models/widget/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { Observable, of, ReplaySubject, switchMap } from 'rxjs';
import L from 'leaflet';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { catchError } from 'rxjs/operators';
import { DataSet, widgetType } from '@shared/models/widget.models';
import { WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { isNotEmptyStr } from '@core/utils';
import { EntityDataPageLink } from '@shared/models/query/query.models';

interface ImageLayerData {
  imageUrl: string;
  aspect: number;
  update?: boolean;
}

export class TbImageMap extends TbMap<ImageMapSettings> {

  private maxZoom: number;
  private width: number;
  private height: number;
  private imageLayerData: ImageLayerData;
  private initMapSubject: ReplaySubject<L.Map>;

  private imageOverlay: L.ImageOverlay;

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<ImageMapSettings>,
              protected containerElement: HTMLElement) {
    super(ctx, inputSettings, containerElement);
  }

  protected defaultSettings(): ImageMapSettings {
    return defaultImageMapSettings;
  }

  protected createMap(): Observable<L.Map> {
    this.maxZoom = 4;
    this.width = 0;
    this.height = 0;
    this.imageLayerData = {
      imageUrl: null,
      aspect: 0
    };
    this.initMapSubject = new ReplaySubject<L.Map>();
    this.loadImageLayerData().subscribe((data) => {
      this.imageLayerData = data;
      if (this.imageLayerData.update) {
        this.onResize(true);
      } else {
        this.onResize();
        this.initMapSubject.next(this.map);
        this.initMapSubject.complete();
      }
    });
    return this.initMapSubject.asObservable();
  }

  protected onResize(updateImage?: boolean): void {
    let width = this.mapElement.clientWidth;
    if (width > 0 && this.imageLayerData.aspect) {
      let height = Math.round(width / this.imageLayerData.aspect);
      const imageMapHeight = this.mapElement.clientHeight;
      if (imageMapHeight > 0 && height > imageMapHeight) {
        height = imageMapHeight;
        width = Math.round(height * this.imageLayerData.aspect);
      }
      width *= this.maxZoom;
      const prevWidth = this.width;
      const prevHeight = this.height;
      if (this.width !== width || updateImage) {
        this.width = width;
        this.height = Math.round(width / this.imageLayerData.aspect);
        if (!this.map) {
          this.doCreateMap(updateImage);
        } else {
          const lastCenterPos = this.latLngToPoint(this.map.getCenter());
          lastCenterPos.x /= prevWidth;
          lastCenterPos.y /= prevHeight;
          this.updateMaxBounds(updateImage, lastCenterPos);
          (this.map as any)._enforcingBounds = true;
          this.map.invalidateSize(false);
          (this.map as any)._enforcingBounds = false;
          this.invalidateDataLayersCoordinates();
        }
      }
    }
  }

  protected fitBounds(_bounds: L.LatLngBounds) {}

  public locationDataToLatLng(position: {x: number; y: number}): L.LatLng {
    return this.pointToLatLng(
      position.x * this.width,
      position.y * this.height);
  }

  public latLngToLocationData(position: L.LatLng): {x: number; y: number} {
    if (!position) {
      return {
        x: null,
        y: null
      };
    }
    const point = this.latLngToPoint(position);
    const posX = calculateNewPointCoordinate(point.x, this.width);
    const posY = calculateNewPointCoordinate(point.y, this.height);
    return {
      x: posX,
      y: posY
    };
  }

  public polygonDataToCoordinates(expression: TbPolygonRawCoordinates): TbPolygonRawCoordinates {
    return expression.map((el: TbPolygonRawCoordinate) => {
      if (!Array.isArray(el[0]) && !Array.isArray(el[1]) && el.length === 2) {
        const latLng = this.pointToLatLng(
          el[0] * this.width,
          el[1] * this.height
        );
        return [latLng.lat, latLng.lng] as TbPolygonRawCoordinate;
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
          const pos = this.latLngToPoint(point);
          return [calculateNewPointCoordinate(pos.x, this.width), calculateNewPointCoordinate(pos.y, this.height)];
        }
      });
    } else {
      return [];
    }
  }

  public circleDataToCoordinates(circle: TbCircleData): TbCircleData {
    const centerPoint = this.pointToLatLng(circle.latitude * this.width, circle.longitude * this.height);
    circle.latitude = centerPoint.lat;
    circle.longitude = centerPoint.lng;
    circle.radius = circle.radius * this.width;
    return circle;
  }

  public coordinatesToCircleData(center: L.LatLng, radius: number): TbCircleData {
    let circleData: TbCircleData = null;
    if (center) {
      const point = this.latLngToPoint(center);
      const posX = calculateNewPointCoordinate(point.x, this.width);
      const posY = calculateNewPointCoordinate(point.y, this.height);
      const convertedRadius = calculateNewPointCoordinate(radius, this.width);
      circleData = {
        latitude: posX,
        longitude: posY,
        radius: convertedRadius
      };
    }
    return circleData;
  }

  public getAspect(): number {
    return this.imageLayerData.aspect;
  }

  private pointToLatLng(x: number, y: number): L.LatLng {
    return L.CRS.Simple.pointToLatLng({ x, y } as L.PointExpression, this.maxZoom - 1);
  }

  private latLngToPoint(latLng: L.LatLngLiteral): L.Point {
    return L.CRS.Simple.latLngToPoint(latLng, this.maxZoom - 1);
  }

  private doCreateMap(updateImage?: boolean) {
    if (!this.map && this.imageLayerData.aspect > 0) {
      const center = this.pointToLatLng(this.width / 2, this.height / 2);
      this.map = L.map(this.mapElement, {
        scrollWheelZoom: this.settings.zoomActions.includes(MapZoomAction.scroll),
        doubleClickZoom: this.settings.zoomActions.includes(MapZoomAction.doubleClick),
        zoomControl: this.settings.zoomActions.includes(MapZoomAction.controlButtons),
        minZoom: 1,
        maxZoom: this.maxZoom,
        zoom: 1,
        center,
        crs: L.CRS.Simple,
        attributionControl: false
      });
      this.updateMaxBounds(updateImage);
    }
  }

  private updateMaxBounds(updateImage?: boolean, lastCenterPos?: L.Point) {
    const w = this.width;
    const h = this.height;
    this.southWest = this.pointToLatLng(0, h);
    this.northEast = this.pointToLatLng(w, 0);
    const bounds = new L.LatLngBounds(this.southWest, this.northEast);

    if (updateImage && this.imageOverlay) {
      this.imageOverlay.remove();
      this.imageOverlay = null;
    }

    if (this.imageOverlay) {
      this.imageOverlay.setBounds(bounds);
    } else {
      this.imageOverlay = L.imageOverlay(this.imageLayerData.imageUrl, bounds).addTo(this.map);
    }
    const padding = 200 * this.maxZoom;
    const southWest = this.pointToLatLng(-padding, h + padding);
    const northEast = this.pointToLatLng(w + padding, -padding);
    const maxBounds = new L.LatLngBounds(southWest, northEast);
    (this.map as any)._enforcingBounds = true;
    this.map.setMaxBounds(maxBounds);
    if (lastCenterPos) {
      lastCenterPos.x *= w;
      lastCenterPos.y *= h;
      const center = this.pointToLatLng(lastCenterPos.x, lastCenterPos.y);
      this.map.panTo(center, { animate: false });
    }
    (this.map as any)._enforcingBounds = false;
  }

  private loadImageLayerData(): Observable<ImageLayerData> {
    const imageSource = this.settings.imageSource;
    if (imageSource.sourceType === ImageSourceType.image) {
      return this.imageFromUrl(imageSource.url);
    } else {
      const datasource = imageMapSourceSettingsToDatasource(imageSource);
      const result = new ReplaySubject<[DataSet, boolean]>();
      let isUpdate = false;
      const imageUrlSubscriptionOptions: WidgetSubscriptionOptions = {
        datasources: [datasource],
        hasDataPageLink: true,
        singleEntity: true,
        useDashboardTimewindow: false,
        type: widgetType.latest,
        callbacks: {
          onDataUpdated: (subscription) => {
            if (isNotEmptyStr(subscription.data[0]?.data[0]?.[1])) {
              result.next([subscription.data[0].data, isUpdate]);
            } else {
              result.next([[[0, imageSource.url]], isUpdate]);
            }
            isUpdate = true;
          }
        }
      };
      this.ctx.subscriptionApi.createSubscription(imageUrlSubscriptionOptions, true).subscribe((subscription) => {
        const pageLink: EntityDataPageLink = {
          page: 0,
          pageSize: 1,
          textSearch: null,
          dynamic: true
        };
        subscription.subscribeAllForPaginatedData(pageLink, null);
      });
      return this.imageFromEntityData(result);
    }
  }

  private imageFromUrl(url: string, update = false): Observable<ImageLayerData> {
    return loadImageWithAspect(this.ctx.$injector.get(ImagePipe), url).pipe(
      switchMap( aspectImage => {
          if (aspectImage) {
            return of({
              imageUrl: aspectImage.url,
              aspect: aspectImage.aspect,
              update
            });
          } else {
            return this.imageFromUrl(defaultImageMapSourceSettings.url, update);
          }
        }
      ),
      catchError(() => this.imageFromUrl(defaultImageMapSourceSettings.url, update))
    );
  }

  private imageFromEntityData(entityData: Observable<[DataSet, boolean]>): Observable<ImageLayerData> {
    return entityData.pipe(
      switchMap(res => {
        const update = res[1];
        const url = res[0][0][1];
        const layerData: ImageLayerData = {
          imageUrl: null,
          aspect: null,
          update
        };
        return loadImageWithAspect(this.ctx.$injector.get(ImagePipe), url).pipe(
          switchMap((aspectImage) => {
            if (aspectImage) {
              layerData.aspect = aspectImage.aspect;
              layerData.imageUrl = aspectImage.url;
              return of(layerData);
            } else {
              return this.imageFromUrl(defaultImageMapSourceSettings.url, update);
            }
          }),
          catchError(() => this.imageFromUrl(defaultImageMapSourceSettings.url, update))
        );
      })
    );
  }

  public polylineDataToCoordinates(expression: TbPolylineRawCoordinates): TbPolylineRawCoordinates{
    return expression.map((el: TbPolylineRawCoordinate) => {
    if (!Array.isArray(el[0]) && !Array.isArray(el[1]) && el.length === 2) {
      const latLng = this.pointToLatLng(
        el[0] * this.width,
        el[1] * this.height
      );
      return [latLng.lat, latLng.lng] as TbPolylineRawCoordinate;
    } else if (Array.isArray(el) && el.length) {
      return this.polylineDataToCoordinates(el as TbPolylineRawCoordinates) as TbPolylineRawCoordinate;
    }
    else {
      return null;
    }
  }).filter(el => !!el);
  }

  public coordinatesToPolylineData(coordinates: TbPolylineCoordinates): TbPolylineRawCoordinates{
    if (coordinates.length) {
      return coordinates.map((point: TbPolylineCoordinate) => {
        if (Array.isArray(point)) {
          return this.coordinatesToPolylineData(point) as TbPolylineRawCoordinate;
        } else {
          const pos = this.latLngToPoint(point);
          return [calculateNewPointCoordinate(pos.x, this.width), calculateNewPointCoordinate(pos.y, this.height)];
        }
      });
    } else {
      return [];
    }
  }

}
