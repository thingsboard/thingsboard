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

import L, { LatLngLiteral, LatLngBounds, LatLngTuple } from 'leaflet';
import LeafletMap from '../leaflet-map';
import { UnitedMapSettings } from '../map-models';
import { Observable } from 'rxjs';
import { map, filter, switchMap } from 'rxjs/operators';
import { aspectCache, calculateNewPointCoordinate, parseFunction } from '@home/components/widget/lib/maps/maps-utils';

const maxZoom = 4;// ?

export class ImageMap extends LeafletMap {

    imageOverlay;
    aspect = 0;
    width = 0;
    height = 0;
    imageUrl;
    posFunction;

    constructor($container: HTMLElement, options: UnitedMapSettings) {
        super($container, options);
        this.posFunction = parseFunction(options.posFunction, ['origXPos', 'origYPos']) as ((origXPos, origYPos) => { x, y });
        this.imageUrl = options.mapUrl;
        aspectCache(this.imageUrl).subscribe(aspect => {
            this.aspect = aspect;
            this.onResize();
            super.setMap(this.map);
            super.initSettings(options);
        });
    }

    setImageAlias(alias: Observable<any>) {
        alias.pipe(filter(result => result), map(el => el[1]), switchMap(res => {
            this.imageUrl = res;
            return aspectCache(res);
        })).subscribe(aspect => {
            this.aspect = aspect;
            this.onResize(true);
        });
    }

    updateBounds(updateImage?, lastCenterPos?) {
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

    onResize(updateImage?) {
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
            if (this.width !== width) {
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
                }
            }
        }
    }

    fitBounds(bounds: LatLngBounds, useDefaultZoom = false, padding?: LatLngTuple) { }

    initMap(updateImage?) {
        if (!this.map && this.aspect > 0) {
            const center = this.pointToLatLng(this.width / 2, this.height / 2);
            this.map = L.map(this.$container, {
                minZoom: 1,
                maxZoom,
                scrollWheelZoom: !this.options.disableScrollZooming,
                center,
                zoom: 1,
                crs: L.CRS.Simple,
                attributionControl: false
            });
            this.updateBounds(updateImage);
        }
    }

    convertPosition(expression): L.LatLng {
        if (isNaN(expression[this.options.xPosKeyName]) || isNaN(expression[this.options.yPosKeyName])) return null;
        Object.assign(expression, this.posFunction(expression[this.options.xPosKeyName], expression[this.options.yPosKeyName]));
        return this.pointToLatLng(
          expression.x * this.width,
          expression.y * this.height);
    }

    pointToLatLng(x, y): L.LatLng {
        return L.CRS.Simple.pointToLatLng({ x, y } as L.PointExpression, maxZoom - 1);
    }

    latLngToPoint(latLng: LatLngLiteral) {
        return L.CRS.Simple.latLngToPoint(latLng, maxZoom - 1);
    }

    convertToCustomFormat(position: L.LatLng): object {
        const point = this.latLngToPoint(position);
        return {
            [this.options.xPosKeyName]: calculateNewPointCoordinate(point.x, this.width),
            [this.options.yPosKeyName]: calculateNewPointCoordinate(point.y, this.height)
        }
    }
}
