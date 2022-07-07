///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import L, { LatLngBounds, LatLngLiteral, LatLngTuple } from 'leaflet';
import LeafletMap from '../leaflet-map';
import { CircleData, MapImage, PosFuncton, WidgetUnitedMapSettings } from '../map-models';
import { Observable, ReplaySubject } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import {
  aspectCache,
  calculateNewPointCoordinate
} from '@home/components/widget/lib/maps/common-maps-utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { DataSet, DatasourceType, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { isDefinedAndNotNull, isEmptyStr, isNotEmptyStr, parseFunction } from '@core/utils';
import { EntityDataPageLink } from '@shared/models/query/query.models';

const maxZoom = 4; // ?

export class ImageMap extends LeafletMap {

    imageOverlay: L.ImageOverlay;
    aspect = 0;
    width = 0;
    height = 0;
    imageUrl: string;
    posFunction: PosFuncton;

    constructor(ctx: WidgetContext, $container: HTMLElement, options: WidgetUnitedMapSettings) {
        super(ctx, $container, options);
        this.posFunction = parseFunction(options.posFunction, ['origXPos', 'origYPos']) as PosFuncton;
        this.mapImage(options).subscribe((mapImage) => {
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
              _hash: Math.random()
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

    private imageFromUrl(url: string): Observable<MapImage> {
      return aspectCache(url).pipe(
        map( aspect => {
            const mapImage: MapImage = {
              imageUrl: url,
              aspect,
              update: false
            };
            return mapImage;
          }
        ));
    }

    private imageFromAlias(alias: Observable<[DataSet, boolean]>): Observable<MapImage> {
      return alias.pipe(
        mergeMap(res => {
          const mapImage: MapImage = {
            imageUrl: res[0][0][1],
            aspect: null,
            update: res[1]
          };
          return aspectCache(mapImage.imageUrl).pipe(
            map((aspect) => {
                mapImage.aspect = aspect;
                return mapImage;
              }
            ));
        })
      );
    }

    updateBounds(updateImage?: boolean, lastCenterPos?) {
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

    fitBounds(bounds: LatLngBounds, padding?: LatLngTuple) { }

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
          attributionControl: false,
          tap: L.Browser.safari && L.Browser.mobile
        });
        this.updateBounds(updateImage);
      }
    }

    convertPosition(expression): L.LatLng {
      const xPos = expression[this.options.xPosKeyName];
      const yPos = expression[this.options.yPosKeyName];
      if (!isDefinedAndNotNull(xPos) || isEmptyStr(xPos) || isNaN(xPos) || !isDefinedAndNotNull(yPos) || isEmptyStr(yPos) || isNaN(yPos)) {
        return null;
      }
      Object.assign(expression, this.posFunction(xPos, yPos));
      return this.pointToLatLng(
        expression.x * this.width,
        expression.y * this.height);
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

    pointToLatLng(x, y): L.LatLng {
        return L.CRS.Simple.pointToLatLng({ x, y } as L.PointExpression, maxZoom - 1);
    }

    latLngToPoint(latLng: LatLngLiteral): L.Point {
        return L.CRS.Simple.latLngToPoint(latLng, maxZoom - 1);
    }

    convertToCustomFormat(position: L.LatLng, offset = 0, width = this.width, height = this.height): {[key: string]: any} {
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
      const centerPoint = this.pointToLatLng(circle.longitude * width, circle.latitude * height);
      circle.latitude = centerPoint.lat;
      circle.longitude = centerPoint.lng;
      circle.radius = circle.radius * width;
      return circle;
    }
}
