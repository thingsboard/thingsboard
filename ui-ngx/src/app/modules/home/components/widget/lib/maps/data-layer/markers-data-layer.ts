///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  BaseMarkerShapeSettings,
  ClusterMarkerColorFunction,
  DataLayerColorType,
  defaultBaseMarkersDataLayerSettings,
  isValidLatLng,
  loadImageWithAspect,
  MapStringFunction,
  MapType,
  MarkerIconInfo,
  MarkerIconSettings,
  MarkerImageFunction,
  MarkerImageInfo,
  MarkerImageSettings,
  MarkerImageType,
  MarkerPositionFunction,
  MarkersDataLayerSettings,
  MarkerShapeSettings,
  MarkerType,
  TbMapDatasource
} from '@home/components/widget/lib/maps/models/map.models';
import L, { FeatureGroup } from 'leaflet';
import { FormattedData } from '@shared/models/widget.models';
import { forkJoin, Observable, of } from 'rxjs';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { isDefined, isDefinedAndNotNull, isEmptyStr, parseTbFunction, safeExecuteTbFunction } from '@core/utils';
import { catchError, map, switchMap } from 'rxjs/operators';
import tinycolor from 'tinycolor2';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { TbMap } from '@home/components/widget/lib/maps/map';
import {
  createColorMarkerIconElement,
  createColorMarkerShapeURI,
  MarkerShape
} from '@home/components/widget/lib/maps/models/marker-shape.models';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import {
  MapDataLayerType,
  TbDataLayerItem,
  TbMapDataLayer
} from '@home/components/widget/lib/maps/data-layer/map-data-layer';
import { TbImageMap } from '@home/components/widget/lib/maps/image-map';

class TbMarkerDataLayerItem extends TbDataLayerItem<MarkersDataLayerSettings, TbMarkersDataLayer, L.Marker> {

  private location: L.LatLng;
  private marker: L.Marker;
  private labelOffset: L.PointTuple;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: MarkersDataLayerSettings,
              protected dataLayer: TbMarkersDataLayer) {
    super(data, dsData, settings, dataLayer);
  }

  protected create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Marker {
    this.location = this.dataLayer.extractLocation(data, dsData);
    this.marker = L.marker(this.location, {
      tbMarkerData: data
    });

    this.updateMarkerIcon(data, dsData);

    return this.marker;
  }

  protected createEventListeners(data: FormattedData<TbMapDatasource>, _dsData: FormattedData<TbMapDatasource>[]): void {
    this.dataLayer.getMap().markerClick(this.marker, data.$datasource);
  }

  protected unbindLabel() {
    this.marker.unbindTooltip();
  }

  protected bindLabel(content: L.Content): void {
    this.marker.bindTooltip(content, { className: 'tb-marker-label', permanent: true, direction: 'top', offset: this.labelOffset });
  }

  protected doUpdate(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.marker.options.tbMarkerData = data;
    this.updateMarkerPosition(data, dsData);
    this.updateTooltip(data, dsData);
    this.updateMarkerIcon(data, dsData);
  }

  protected doInvalidateCoordinates(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.updateMarkerPosition(data, dsData);
  }

  private updateMarkerPosition(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    const position = this.dataLayer.extractLocation(data, dsData);
    if (!this.marker.getLatLng().equals(position)) {
      this.location = position;
      this.marker.setLatLng(position);
    }
  }

  private updateMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    this.dataLayer.markerIconProcessor.createMarkerIcon(data, dsData).subscribe(
      (iconInfo) => {
        this.marker.setIcon(iconInfo.icon);
        const anchor = iconInfo.icon.options.iconAnchor;
        if (anchor && Array.isArray(anchor)) {
          this.labelOffset = [iconInfo.size[0] / 2 - anchor[0], 10 - anchor[1]];
        } else {
          this.labelOffset = [0, -iconInfo.size[1] * this.dataLayer.markerOffset[1] + 10];
        }
        this.updateLabel(data, dsData);
      }
    );
  }
}

abstract class MarkerIconProcessor<S> {

  static fromSettings(dataLayer: TbMarkersDataLayer,
                      settings: MarkersDataLayerSettings): MarkerIconProcessor<any> {
    switch (settings.markerType) {
      case MarkerType.shape:
        return new ShapeMarkerIconProcessor(dataLayer, settings.markerShape);
      case MarkerType.icon:
        return new IconMarkerIconProcessor(dataLayer, settings.markerIcon);
      case MarkerType.image:
        return new ImageMarkerIconProcessor(dataLayer, settings.markerImage);
    }
  }

  protected constructor(protected dataLayer: TbMarkersDataLayer,
                        protected settings: S) {}

  public abstract setup(): Observable<void>;

  public abstract createMarkerIcon(data: FormattedData<TbMapDatasource>,
                                   dsData: FormattedData<TbMapDatasource>[]): Observable<MarkerIconInfo>;

}

abstract class BaseColorMarkerShapeProcessor<S extends BaseMarkerShapeSettings> extends MarkerIconProcessor<S> {

  private markerColorFunction: CompiledTbFunction<MapStringFunction>;

  private defaultMarkerIconInfo: MarkerIconInfo;

  protected constructor(protected dataLayer: TbMarkersDataLayer,
                        protected settings: S) {
    super(dataLayer, settings);
  }

  public setup(): Observable<void> {
    const colorSettings = this.settings.color;
    if (colorSettings.type === DataLayerColorType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, colorSettings.colorFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.markerColorFunction = parsed;
          return null;
        })
      );
    } else {
      const color = tinycolor(colorSettings.color);
      return this.createMarkerShape(color, this.settings.size).pipe(
        map((info) => {
            this.defaultMarkerIconInfo = info;
            return null;
          }
        ));
    }
  }

  public createMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): Observable<MarkerIconInfo> {
    const colorSettings = this.settings.color;
    if (colorSettings.type === DataLayerColorType.function) {
      const functionColor = safeExecuteTbFunction(this.markerColorFunction, [data, dsData]);
      let color: tinycolor.Instance;
      if (isDefinedAndNotNull(functionColor)) {
        color = tinycolor(functionColor);
      } else {
        color = tinycolor(colorSettings.color);
      }
      return this.createMarkerShape(color, this.settings.size);
    } else {
      return of(this.defaultMarkerIconInfo);
    }
  }

  protected abstract createMarkerShape(color: tinycolor.Instance, size: number): Observable<MarkerIconInfo>;
}

class ShapeMarkerIconProcessor extends BaseColorMarkerShapeProcessor<MarkerShapeSettings> {

  constructor(protected dataLayer: TbMarkersDataLayer,
              protected settings: MarkerShapeSettings) {
    super(dataLayer, settings);
  }

  protected createMarkerShape(color: tinycolor.Instance, size: number): Observable<MarkerIconInfo> {
    return this.dataLayer.createColoredMarkerShape(this.settings.shape, color, size);
  }

}

class IconMarkerIconProcessor extends BaseColorMarkerShapeProcessor<MarkerIconSettings> {

  constructor(protected dataLayer: TbMarkersDataLayer,
              protected settings: MarkerIconSettings) {
    super(dataLayer, settings);
  }

  protected createMarkerShape(color: tinycolor.Instance, size: number): Observable<MarkerIconInfo> {
    return this.dataLayer.createColoredMarkerIcon(this.settings.icon, color, size);
  }

}

class ImageMarkerIconProcessor extends MarkerIconProcessor<MarkerImageSettings> {

  private markerImageFunction: CompiledTbFunction<MarkerImageFunction>;

  private defaultMarkerIconInfo: MarkerIconInfo;

  constructor(protected dataLayer: TbMarkersDataLayer,
              protected settings: MarkerImageSettings) {
    super(dataLayer, settings);
  }

  public setup(): Observable<void> {
    if (this.settings.type === MarkerImageType.function) {
      return parseTbFunction<MarkerImageFunction>(this.dataLayer.getCtx().http, this.settings.imageFunction, ['data', 'images', 'dsData']).pipe(
        map((parsed) => {
          this.markerImageFunction = parsed;
          return null;
        })
      );
    } else {
      const currentImage: MarkerImageInfo = {
        url: this.settings.image,
        size: this.settings.imageSize || 34
      };
      return this.loadMarkerIconInfo(currentImage).pipe(
        map((iconInfo) => {
            this.defaultMarkerIconInfo = iconInfo;
            return null;
          }
        ));
    }
  }

  public createMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): Observable<MarkerIconInfo> {
    if (this.settings.type === MarkerImageType.function) {
      const currentImage: MarkerImageInfo = safeExecuteTbFunction(this.markerImageFunction, [data, this.settings.images, dsData]);
      return this.loadMarkerIconInfo(currentImage);
    } else {
      return of(this.defaultMarkerIconInfo);
    }
  }

  private loadMarkerIconInfo(image: MarkerImageInfo): Observable<MarkerIconInfo> {
    if (image && image.url) {
      return loadImageWithAspect(this.dataLayer.getCtx().$injector.get(ImagePipe), image.url).pipe(
        switchMap((aspectImage) => {
          if (aspectImage?.aspect) {
            let width: number;
            let height: number;
            if (aspectImage.aspect > 1) {
              width = image.size;
              height = image.size / aspectImage.aspect;
            } else {
              width = image.size * aspectImage.aspect;
              height = image.size;
            }
            let iconAnchor = image.markerOffset;
            let popupAnchor = image.tooltipOffset;
            if (!iconAnchor) {
              iconAnchor = [width * this.dataLayer.markerOffset[0], height * this.dataLayer.markerOffset[1]];
            }
            if (!popupAnchor) {
              popupAnchor = [width * this.dataLayer.tooltipOffset[0], height * this.dataLayer.tooltipOffset[1]];
            }
            const icon = L.icon({
              iconUrl: aspectImage.url,
              iconSize: [width, height],
              iconAnchor,
              popupAnchor
            });
            const iconInfo: MarkerIconInfo = {
              size: [width, height],
              icon
            };
            return of(iconInfo);
          } else {
            return this.dataLayer.createDefaultMarkerIcon();
          }
        }),
        catchError(() => this.dataLayer.createDefaultMarkerIcon())
      );
    } else {
      return this.dataLayer.createDefaultMarkerIcon();
    }
  }

}

export class TbMarkersDataLayer extends TbMapDataLayer<MarkersDataLayerSettings, TbMarkersDataLayer> {

  public markerIconProcessor: MarkerIconProcessor<any>;

  public markerOffset: L.LatLngTuple;
  public tooltipOffset: L.LatLngTuple;

  private markersClusterContainer: L.MarkerClusterGroup;
  private clusterMarkerColorFunction: CompiledTbFunction<ClusterMarkerColorFunction>;
  private positionFunction: CompiledTbFunction<MarkerPositionFunction>;

  constructor(protected map: TbMap<any>,
              inputSettings: MarkersDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return MapDataLayerType.marker;
  }

  protected createDataLayerContainer(): FeatureGroup {
    if (this.settings.markerClustering?.enable) {
      return this.createMarkersClusterContainer();
    } else {
      return super.createDataLayerContainer();
    }
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    datasource.dataKeys.push(this.settings.xKey, this.settings.yKey);
    return datasource;
  }

  protected defaultBaseSettings(map: TbMap<any>): Partial<MarkersDataLayerSettings> {
    return defaultBaseMarkersDataLayerSettings(map.type());
  }

  protected doSetup(): Observable<void> {
    this.markerOffset = [
      isDefined(this.settings.markerOffsetX) ? this.settings.markerOffsetX : 0.5,
      isDefined(this.settings.markerOffsetY) ? this.settings.markerOffsetY : 1,
    ];
    this.tooltipOffset = [
      isDefined(this.settings.tooltip?.offsetX) ? this.settings.tooltip?.offsetX : 0,
      isDefined(this.settings.tooltip?.offsetY) ? this.settings.tooltip?.offsetY : -1,
    ];
    this.markerIconProcessor = MarkerIconProcessor.fromSettings(this, this.settings);
    const setup$: Observable<void>[] = [];
    if (this.settings.markerClustering?.enable && this.settings.markerClustering.useClusterMarkerColorFunction) {
      setup$.push(
        parseTbFunction<ClusterMarkerColorFunction>(this.getCtx().http, this.settings.markerClustering.clusterMarkerColorFunction, ['data', 'childCount']).pipe(
          map((parsed) => {
            this.clusterMarkerColorFunction = parsed;
            return null;
          })
        )
      );
    }
    if (this.map.type() === MapType.image) {
      setup$.push(
        parseTbFunction<MarkerPositionFunction>(this.getCtx().http, this.settings.positionFunction, ['origXPos', 'origYPos', 'data', 'dsData', 'aspect']).pipe(
          map((parsed) => {
            this.positionFunction = parsed;
            return null;
          })
        )
      );
    }
    setup$.push(this.markerIconProcessor.setup());
    return forkJoin(setup$).pipe(map(() => null));
  }

  protected isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean {
    return !!this.extractPosition(layerData);
  }

  protected createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbMarkerDataLayerItem {
    return new TbMarkerDataLayerItem(data, dsData, this.settings, this);
  }

  protected layerItemsUpdated(updatedItems: TbDataLayerItem<MarkersDataLayerSettings, TbMarkersDataLayer, L.Marker>[]) {
    if (this.settings.markerClustering?.enable) {
      this.markersClusterContainer.refreshClusters(updatedItems.map(item => item.getLayer()));
    }
    super.layerItemsUpdated(updatedItems);
  }

  private createMarkersClusterContainer(): L.FeatureGroup {
    const markerClusterOptions: L.MarkerClusterGroupOptions = {
      spiderfyOnMaxZoom: this.settings.markerClustering?.spiderfyOnMaxZoom,
      zoomToBoundsOnClick: this.settings.markerClustering?.zoomOnClick,
      showCoverageOnHover: this.settings.markerClustering?.showCoverageOnHover,
      removeOutsideVisibleBounds: this.settings.markerClustering?.lazyLoad,
      animate: this.settings.markerClustering?.zoomAnimation,
      chunkedLoading: this.settings.markerClustering?.chunkedLoad,
      pmIgnore: true,
      spiderLegPolylineOptions: {
        pmIgnore: true
      },
      polygonOptions: {
        pmIgnore: true
      }
    };
    if (this.settings.markerClustering?.useClusterMarkerColorFunction) {
      markerClusterOptions.iconCreateFunction = (cluster) => {
        const childCount = cluster.getChildCount();
        const data = cluster.getAllChildMarkers().map(clusterMarker => clusterMarker.options.tbMarkerData);
        const markerColor: string = this.clusterMarkerColorFunction ?
          safeExecuteTbFunction(this.clusterMarkerColorFunction, [data, childCount]) : null;
        if (isDefinedAndNotNull(markerColor) && tinycolor(markerColor).isValid()) {
          const parsedColor = tinycolor(markerColor);
          const alpha = parsedColor.getAlpha();
          return L.divIcon({
            html: `<div style="background-color: ${parsedColor.setAlpha(alpha * 0.4).toRgbString()};" ` +
              `class="marker-cluster tb-cluster-marker-element">` +
              `<div style="background-color: ${parsedColor.setAlpha(alpha * 0.9).toRgbString()};"><span>` + childCount + '</span></div></div>',
            iconSize: new L.Point(40, 40),
            className: 'tb-cluster-marker-container'
          });
        } else {
          let c = ' marker-cluster-';
          if (childCount < 10) {
            c += 'small';
          } else if (childCount < 100) {
            c += 'medium';
          } else {
            c += 'large';
          }
          return new L.DivIcon({
            html: '<div><span>' + childCount + '</span></div>',
            className: 'marker-cluster' + c,
            iconSize: new L.Point(40, 40)
          });
        }
      }
    }
    if (this.settings.markerClustering?.maxClusterRadius && this.settings.markerClustering.maxClusterRadius > 0) {
      markerClusterOptions.maxClusterRadius = Math.floor(this.settings.markerClustering.maxClusterRadius);
    }
    if (this.settings.markerClustering?.maxZoom && this.settings.markerClustering.maxZoom >= 0 && this.settings.markerClustering.maxZoom < 19) {
      markerClusterOptions.disableClusteringAtZoom = Math.floor(this.settings.markerClustering.maxZoom);
    }
    this.markersClusterContainer = new L.MarkerClusterGroup(markerClusterOptions);
    return this.markersClusterContainer;
  }

  private extractPosition(data: FormattedData<TbMapDatasource>):  {x: number; y: number} {
    if (data) {
      const xKeyVal = data[this.settings.xKey.label];
      const yKeyVal = data[this.settings.yKey.label];
      switch (this.mapType()) {
        case MapType.geoMap:
          if (!isValidLatLng(xKeyVal, yKeyVal)) {
            return null;
          }
          break;
        case MapType.image:
          if (!isDefinedAndNotNull(xKeyVal) || isEmptyStr(xKeyVal) || isNaN(xKeyVal) || !isDefinedAndNotNull(yKeyVal) || isEmptyStr(yKeyVal) || isNaN(yKeyVal)) {
            return null;
          }
          break;
      }
      return {x: xKeyVal, y: yKeyVal};
    } else {
      return null;
    }
  }

  public createDefaultMarkerIcon(): Observable<MarkerIconInfo> {
    const color = this.settings.markerShape?.color?.color || '#307FE5';
    return this.createColoredMarkerShape(MarkerShape.markerShape1, tinycolor(color));
  }

  public createColoredMarkerShape(shape: MarkerShape, color: tinycolor.Instance, size = 34): Observable<MarkerIconInfo> {
    return createColorMarkerShapeURI(this.getCtx().$injector.get(MatIconRegistry), this.getCtx().$injector.get(DomSanitizer), shape, color).pipe(
      map((iconUrl) => {
        return {
          size: [size, size],
          icon: L.icon({
            iconUrl,
            iconSize: [size, size],
            iconAnchor: [size * this.markerOffset[0], size * this.markerOffset[1]],
            popupAnchor: [size * this.tooltipOffset[0], size * this.tooltipOffset[1]]
          })
        };
      })
    );
  }

  public createColoredMarkerIcon(icon: string, color: tinycolor.Instance, size = 34): Observable<MarkerIconInfo> {
    return createColorMarkerIconElement(this.getCtx().$injector.get(MatIconRegistry), this.getCtx().$injector.get(DomSanitizer), icon, color).pipe(
      map((element) => {
        return {
          size: [size, size],
          icon: L.divIcon({
            html: element.outerHTML,
            className: 'tb-marker-div-icon',
            iconSize: [size, size],
            iconAnchor: [size * this.markerOffset[0], size * this.markerOffset[1]],
            popupAnchor: [size * this.tooltipOffset[0], size * this.tooltipOffset[1]]
          })
        };
      })
    );
  }

  public extractLocation(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.LatLng {
    let position = this.extractPosition(data);
    if (position) {
      if (this.map.type() === MapType.image && this.positionFunction) {
        const imageMap = this.map as TbImageMap;
        position = this.positionFunction.execute(position.x, position.y, data, dsData, imageMap.getAspect()) || {x: 0, y: 0};
      }
      return this.map.positionToLatLng(position);
    } else {
      return null;
    }
  }
}
