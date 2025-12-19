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

import L, { LatLngBounds, LatLngLiteral, LatLngTuple, PointExpression } from 'leaflet';
import LeafletMap from '../leaflet-map';
import {
  CircleData,
  defaultImageMapProviderSettings,
  MapImage,
  PosFunction,
  WidgetUnitedMapSettings
} from '../map-models';
import { combineLatest, Observable, of, ReplaySubject, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { calculateNewPointCoordinate, loadImageWithAspect } from '@home/components/widget/lib/maps-legacy/common-maps-utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { DataSet, DatasourceType, FormattedData, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { isDefinedAndNotNull, isEmptyStr, isNotEmptyStr, parseTbFunction } from '@core/utils';
import { EntityDataPageLink } from '@shared/models/query/query.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { CompiledTbFunction } from '@shared/models/js-function.models';

const maxZoom = 4; // ?

export class ImageMap extends LeafletMap {

    imageOverlay: L.ImageOverlay;
    aspect = 0;
    width = 0;
    height = 0;
    imageUrl: string;
    posFunction: CompiledTbFunction<PosFunction>;

    constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
        super(ctx, $container, options);

        const initData = {
          posFunction: parseTbFunction<PosFunction>(this.ctx.http, options.posFunction,
            ['origXPos', 'origYPos', 'data', 'dsData', 'dsIndex', 'aspect']),
          mapImage: this.mapImage(options)
        };

        combineLatest(initData).subscribe(inited => {
          this.posFunction = inited.posFunction;
          const mapImage = inited.mapImage;
          this.imageUrl = mapImage.imageUrl;
          this.aspect = mapImage.aspect;
          if (mapImage.update) {
            this.onResize(true);
          } else {
            this.onResize();
            super.setMap(this.map);
          }
        });
    }

    private mapImage(options: WidgetUnitedMapSettings): Observable<MapImage> {
      const imageEntityAlias = options.imageEntityAlias;
      const imageUrlAttribute = options.imageUrlAttribute;
      if (!imageEntityAlias || !imageUrlAttribute) {
        return this.imageFromUrl(options.mapImageUrl);
      }
      const entityAliasId = this.ctx.aliasController.getEntityAliasId(imageEntityAlias);
      if (!entityAliasId) {
        return this.imageFromUrl(options.mapImageUrl);
      }
      const datasources = [
        {
          type: DatasourceType.entity,
          name: imageEntityAlias,
          aliasName: imageEntityAlias,
          entityAliasId,
          dataKeys: [
            {
              type: DataKeyType.attribute,
              name: imageUrlAttribute,
              label: imageUrlAttribute,
              settings: {},
            }
          ]
        }
      ];
      const result = new ReplaySubject<[DataSet, boolean]>();
      let isUpdate = false;
      const imageUrlSubscriptionOptions: WidgetSubscriptionOptions = {
        datasources,
        hasDataPageLink: true,
        singleEntity: true,
        useDashboardTimewindow: false,
        type: widgetType.latest,
        callbacks: {
          onDataUpdated: (subscription) => {
            if (isNotEmptyStr(subscription.data[0]?.data[0]?.[1])) {
              result.next([subscription.data[0].data, isUpdate]);
            } else {
              result.next([[[0, options.mapImageUrl]], isUpdate]);
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
      return this.imageFromAlias(result);
    }

    private imageFromUrl(url: string, update = false): Observable<MapImage> {
      return loadImageWithAspect(this.ctx.$injector.get(ImagePipe), url).pipe(
        switchMap( aspectImage => {
            if (aspectImage) {
              return of({
                imageUrl: aspectImage.url,
                aspect: aspectImage.aspect,
                update
              });
            } else {
              return this.imageFromUrl(defaultImageMapProviderSettings.mapImageUrl, update);
            }
          }
        ),
        catchError(() => this.imageFromUrl(defaultImageMapProviderSettings.mapImageUrl, update))
      );
    }

    private imageFromAlias(alias: Observable<[DataSet, boolean]>): Observable<MapImage> {
      return alias.pipe(
        switchMap(res => {
          const update = res[1];
          const url = res[0][0][1];
          const mapImage: MapImage = {
            imageUrl: null,
            aspect: null,
            update
          };
          return loadImageWithAspect(this.ctx.$injector.get(ImagePipe), url).pipe(
            switchMap((aspectImage) => {
                if (aspectImage) {
                  mapImage.aspect = aspectImage.aspect;
                  mapImage.imageUrl = aspectImage.url;
                  return of(mapImage);
                } else {
                  return this.imageFromUrl(defaultImageMapProviderSettings.mapImageUrl, update);
                }
              }),
            catchError(() => this.imageFromUrl(defaultImageMapProviderSettings.mapImageUrl, update))
          );
        })
      );
    }

    updateBounds(updateImage?: boolean, lastCenterPos?: L.Point) {
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
            this.imageOverlay = L.imageOverlay(this.imageUrl, bounds).addTo(this.map);
        }
        const padding = 200 * maxZoom;
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

    onResize(updateImage?: boolean) {
      let width = this.$container.clientWidth;
      if (width > 0 && this.aspect) {
        let height = Math.round(width / this.aspect);
        const imageMapHeight = this.$container.clientHeight;
        if (imageMapHeight > 0 && height > imageMapHeight) {
          height = imageMapHeight;
          width = Math.round(height * this.aspect);
        }
        width *= maxZoom;
        const prevWidth = this.width;
        const prevHeight = this.height;
        if (this.width !== width || updateImage) {
          this.width = width;
          this.height = Math.round(width / this.aspect);
          if (!this.map) {
            this.initMap(updateImage);
          } else {
            const lastCenterPos = this.latLngToPoint(this.map.getCenter());
            lastCenterPos.x /= prevWidth;
            lastCenterPos.y /= prevHeight;
            this.updateBounds(updateImage, lastCenterPos);
            (this.map as any)._enforcingBounds = true;
            this.map.invalidateSize(false);
            (this.map as any)._enforcingBounds = false;
            this.updateMarkers(this.markersData);
            if (this.options.showPolygon) {
              this.updatePolygons(this.polygonsData);
            }
            if (this.options.showCircle) {
              this.updateCircle(this.circleData);
            }
          }
        }
      }
    }

    fitBounds(_bounds: LatLngBounds, _padding?: PointExpression) { }

    initMap(updateImage?: boolean) {
      if (!this.map && this.aspect > 0) {
        const center = this.pointToLatLng(this.width / 2, this.height / 2);
        this.map = L.map(this.$container, {
          minZoom: 1,
          maxZoom,
          scrollWheelZoom: !this.options.disableScrollZooming,
          center,
          doubleClickZoom: !this.options.disableDoubleClickZooming,
          zoomControl: !this.options.disableZoomControl,
          zoom: 1,
          crs: L.CRS.Simple,
          attributionControl: false
        });
        this.updateBounds(updateImage);
      }
    }

    extractPosition(data: FormattedData): {x: number; y: number} {
      if (!data) {
        return null;
      }
      const xPos = data[this.options.xPosKeyName];
      const yPos = data[this.options.yPosKeyName];
      if (!isDefinedAndNotNull(xPos) || isEmptyStr(xPos) || isNaN(xPos) || !isDefinedAndNotNull(yPos) || isEmptyStr(yPos) || isNaN(yPos)) {
        return null;
      }
      return {x: xPos, y: yPos};
    }

    positionToLatLng(position: {x: number; y: number}): L.LatLng {
      return this.pointToLatLng(
        position.x * this.width,
        position.y * this.height);
    }

    convertPosition(data: FormattedData, dsData: FormattedData[]): L.LatLng {
      const position = this.extractPosition(data);
      if (position) {
        const converted = this.posFunction.execute(position.x, position.y, data, dsData, data.dsIndex, this.aspect) || {x: 0, y: 0};
        return this.positionToLatLng(converted);
      } else {
        return null;
      }
    }

    convertPositionPolygon(expression: (LatLngTuple | LatLngTuple[] | LatLngTuple[][])[]){
      return (expression).map((el) => {
        if (!Array.isArray(el[0]) && !Array.isArray(el[1]) && el.length === 2) {
          return this.pointToLatLng(
            el[0] * this.width,
            el[1] * this.height
          );
        } else if (Array.isArray(el) && el.length) {
          return this.convertPositionPolygon(el as LatLngTuple[] | LatLngTuple[][]);
        } else {
          return null;
        }
      }).filter(el => !!el);
    }

    pointToLatLng(x: number, y: number): L.LatLng {
        return L.CRS.Simple.pointToLatLng({ x, y } as L.PointExpression, maxZoom - 1);
    }

    latLngToPoint(latLng: LatLngLiteral): L.Point {
        return L.CRS.Simple.latLngToPoint(latLng, maxZoom - 1);
    }

    convertToCustomFormat(position: L.LatLng, _offset = 0, width = this.width, height = this.height): {[key: string]: any} {
      if (!position) {
        return {
          [this.options.xPosKeyName]: null,
          [this.options.yPosKeyName]: null
        };
      }
      const point = this.latLngToPoint(position);
      const customX = calculateNewPointCoordinate(point.x, width);
      const customY = calculateNewPointCoordinate(point.y, height);
      if (customX === 0) {
        point.x = 0;
      } else if (customX === 1) {
        point.x = width;
      }

      if (customY === 0) {
        point.y = 0;
      } else if (customY === 1) {
        point.y = height;
      }

      return {
        [this.options.xPosKeyName]: customX,
        [this.options.yPosKeyName]: customY
      };
    }

    convertToPolygonFormat(points: Array<any>, width = this.width, height = this.height): Array<any> {
      if (points.length) {
        return points.map(point => {
          if (point.length) {
            return this.convertToPolygonFormat(point, width, height);
          } else {
            const pos = this.latLngToPoint(point);
            return [calculateNewPointCoordinate(pos.x, width), calculateNewPointCoordinate(pos.y, height)];
          }
        });
      } else {
        return [];
      }
    }

    convertPolygonToCustomFormat(expression: any[][]): {[key: string]: any} {
      const coordinate = expression ? this.convertToPolygonFormat(expression) : null;
      return {
        [this.options.polygonKeyName]: coordinate
      };
    }

    convertCircleToCustomFormat(expression: L.LatLng, radius: number, width = this.width,
                                height = this.height): {[key: string]: CircleData} {
      let circleDara: CircleData = null;
      if (expression) {
        const point = this.latLngToPoint(expression);
        const customX = calculateNewPointCoordinate(point.x, width);
        const customY = calculateNewPointCoordinate(point.y, height);
        const customRadius = calculateNewPointCoordinate(radius, width);
        circleDara = {
          latitude: customX,
          longitude: customY,
          radius: customRadius
        };
      }
      return {
        [this.options.circleKeyName]: circleDara
      };
    }

    convertToCircleFormat(circle: CircleData, width = this.width, height = this.height): CircleData {
      const centerPoint = this.pointToLatLng(circle.latitude * width, circle.longitude * height);
      circle.latitude = centerPoint.lat;
      circle.longitude = centerPoint.lng;
      circle.radius = circle.radius * width;
      return circle;
    }
}
