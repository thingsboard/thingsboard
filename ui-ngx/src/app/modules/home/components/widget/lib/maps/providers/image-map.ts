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

import L, { LatLngBounds, LatLngLiteral, LatLngTuple } from 'leaflet';
import LeafletMap from '../leaflet-map';
import { MapImage, PosFuncton, UnitedMapSettings } from '../map-models';
import { Observable, ReplaySubject } from 'rxjs';
import { filter, map, mergeMap } from 'rxjs/operators';
import { aspectCache, calculateNewPointCoordinate, parseFunction } from '@home/components/widget/lib/maps/maps-utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { DataSet, DatasourceType, widgetType } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { isDefinedAndNotNull, isEmptyStr } from '@core/utils';

const maxZoom = 4; // ?

export class ImageMap extends LeafletMap {

    imageOverlay: L.ImageOverlay;
    aspect = 0;
    width = 0;
    height = 0;
    imageUrl: string;
    posFunction: PosFuncton;

    constructor(ctx: WidgetContext, $container: HTMLElement, options: UnitedMapSettings) {
        super(ctx, $container, options);
        this.posFunction = parseFunction(options.posFunction, ['origXPos', 'origYPos']) as PosFuncton;
        this.mapImage(options).subscribe((mapImage) => {
          this.imageUrl = mapImage.imageUrl;
          this.aspect = mapImage.aspect;
          if (mapImage.update) {
            this.onResize(true);
          } else {
            this.onResize();
            super.initSettings(options);
            super.setMap(this.map);
          }
        });
    }

    private mapImage(options: UnitedMapSettings): Observable<MapImage> {
      const imageEntityAlias = options.imageEntityAlias;
      const imageUrlAttribute = options.imageUrlAttribute;
      if (!imageEntityAlias || !imageUrlAttribute) {
        return this.imageFromUrl(options.mapUrl);
      }
      const entityAliasId = this.ctx.aliasController.getEntityAliasId(imageEntityAlias);
      if (!entityAliasId) {
        return this.imageFromUrl(options.mapUrl);
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
        useDashboardTimewindow: false,
        type: widgetType.latest,
        callbacks: {
          onDataUpdated: (subscription) => {
            result.next([subscription.data[0]?.data, isUpdate]);
            isUpdate = true;
          }
        }
      };
      this.ctx.subscriptionApi.createSubscription(imageUrlSubscriptionOptions, true).subscribe(() => { });
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
        filter(result => result[0].length > 0),
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
        let southWest = this.pointToLatLng(0, h);
        let northEast = this.pointToLatLng(w, 0);
        const bounds = new L.LatLngBounds(southWest, northEast);

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
        southWest = this.pointToLatLng(-padding, h + padding);
        northEast = this.pointToLatLng(w + padding, -padding);
        const maxBounds = new L.LatLngBounds(southWest, northEast);
        this.map.setMaxBounds(maxBounds);
        if (lastCenterPos) {
            lastCenterPos.x *= w;
            lastCenterPos.y *= h;
            const center = this.pointToLatLng(lastCenterPos.x, lastCenterPos.y);
            setTimeout(() => {
                this.map.panTo(center, { animate: false });
            }, 0);
        }
    }

    onResize(updateImage?: boolean) {
      let width = this.$container.clientWidth;
      if (width > 0 && this.aspect) {
        let height = width / this.aspect;
        const imageMapHeight = this.$container.clientHeight;
        if (imageMapHeight > 0 && height > imageMapHeight) {
          height = imageMapHeight;
          width = height * this.aspect;
        }
        width *= maxZoom;
        const prevWidth = this.width;
        const prevHeight = this.height;
        if (this.width !== width || updateImage) {
          this.width = width;
          this.height = width / this.aspect;
          if (!this.map) {
            this.initMap(updateImage);
          } else {
            const lastCenterPos = this.latLngToPoint(this.map.getCenter());
            lastCenterPos.x /= prevWidth;
            lastCenterPos.y /= prevHeight;
            this.updateBounds(updateImage, lastCenterPos);
            this.map.invalidateSize(true);
            this.updateMarkers(this.markersData);
            if (this.options.draggableMarker && this.addMarkers.length) {
              this.addMarkers.forEach((marker) => {
                const prevPoint = this.convertToCustomFormat(marker.getLatLng(), prevWidth, prevHeight);
                marker.setLatLng(this.convertPosition(prevPoint));
              });
            }
            this.updatePolygons(this.polygonsData);
            if (this.options.showPolygon && this.options.editablePolygon && this.addPolygons.length) {
              this.addPolygons.forEach((polygon) => {
                const prevPolygonPoint = this.convertToPolygonFormat(polygon.getLatLngs(), prevWidth, prevHeight);
                polygon.setLatLngs(this.convertPositionPolygon(prevPolygonPoint));
              });
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
          zoom: 1,
          crs: L.CRS.Simple,
          attributionControl: false,
          editable: !!this.options.editablePolygon
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

    latLngToPoint(latLng: LatLngLiteral) {
        return L.CRS.Simple.latLngToPoint(latLng, maxZoom - 1);
    }

    convertToCustomFormat(position: L.LatLng, width = this.width, height = this.height): object {
      const point = this.latLngToPoint(position);
      return {
        [this.options.xPosKeyName]: calculateNewPointCoordinate(point.x, width),
        [this.options.yPosKeyName]: calculateNewPointCoordinate(point.y, height)
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

    convertPolygonToCustomFormat(expression: any[][]): object {
      return {
        [this.options.polygonKeyName] : this.convertToPolygonFormat(expression)
      };
    }
}
