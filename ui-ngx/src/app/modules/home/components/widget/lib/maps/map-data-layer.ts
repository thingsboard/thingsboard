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
  CirclesDataLayerSettings,
  createColorMarkerURI,
  DataLayerColorSettings,
  DataLayerColorType,
  defaultBaseCirclesDataLayerSettings,
  defaultBaseMarkersDataLayerSettings,
  defaultBasePolygonsDataLayerSettings,
  isCutPolygon,
  isJSON,
  isValidLatLng,
  loadImageWithAspect,
  MapDataLayerSettings,
  mapDataSourceSettingsToDatasource,
  MapStringFunction,
  MapType,
  MarkerIconInfo,
  MarkerImageFunction,
  MarkerImageInfo,
  MarkerImageSettings,
  MarkerImageType,
  MarkersDataLayerSettings,
  MarkerType,
  PolygonsDataLayerSettings,
  TbCircleData,
  TbMapDatasource
} from '@home/components/widget/lib/maps/map.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { FormattedData } from '@shared/models/widget.models';
import { Observable, of } from 'rxjs';
import {
  guid,
  isDefined,
  isDefinedAndNotNull,
  isEmptyStr,
  isNotEmptyStr,
  isString,
  mergeDeepIgnoreArray,
  parseTbFunction,
  safeExecuteTbFunction
} from '@core/utils';
import L, { LatLngBounds } from 'leaflet';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { catchError, map } from 'rxjs/operators';
import tinycolor from 'tinycolor2';
import { WidgetContext } from '@home/models/widget-component.models';
import { ImagePipe } from '@shared/pipe/image.pipe';

abstract class TbDataLayerItem<S extends MapDataLayerSettings, L extends TbMapDataLayer<S,L>> {

  protected layer: L.Layer;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: S,
              protected dataLayer: L) {
    this.layer = this.create(data, dsData);
    this.dataLayer.getFeatureGroup().addLayer(this.layer);
  }

  protected abstract create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Layer;

  public abstract update(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void;

  public remove() {
    this.dataLayer.getFeatureGroup().removeLayer(this.layer);
  }

  protected updateLayer(newLayer: L.Layer) {
    this.dataLayer.getFeatureGroup().removeLayer(this.layer);
    this.layer = newLayer;
    this.dataLayer.getFeatureGroup().addLayer(this.layer);
  }

}

export enum MapDataLayerType {
   marker = 'marker',
   polygon = 'polygon',
   circle = 'circle'
}

export abstract class TbMapDataLayer<S extends MapDataLayerSettings, L extends TbMapDataLayer<S,L>> implements L.TB.DataLayer {

  protected settings: S;

  protected datasource: TbMapDatasource;

  protected mapDataId = guid();

  protected featureGroup = L.featureGroup();

  protected layerItems = new Map<string, TbDataLayerItem<S,L>>();

  protected groupsState: {[group: string]: boolean} = {};

  protected enabled = true;

  protected constructor(protected map: TbMap<any>,
                        inputSettings: S) {
    this.settings = mergeDeepIgnoreArray({} as S, this.defaultBaseSettings() as S, inputSettings);
    if (this.settings.groups?.length) {
      this.settings.groups.forEach((group) => {
        this.groupsState[group] = true;
      });
    }
    this.map.getMap().addLayer(this.featureGroup);
  }

  public setup(): Observable<void> {
    this.datasource = mapDataSourceSettingsToDatasource(this.settings);
    this.datasource.dataKeys = this.settings.additionalDataKeys ? [...this.settings.additionalDataKeys] : [];
    this.mapDataId = this.datasource.mapDataIds[0];
    this.datasource = this.setupDatasource(this.datasource);
    return this.doSetup();
  }

  public getDatasource(): TbMapDatasource {
    return this.datasource;
  }

  public getFeatureGroup(): L.FeatureGroup {
    return this.featureGroup;
  }

  public getBounds(): LatLngBounds {
    return this.featureGroup.getBounds();
  }

  public isEnabled(): boolean {
    return this.enabled;
  }

  public getGroups(): string[] {
    return this.settings.groups || [];
  }

  public toggleGroup(group: string): boolean {
    if (isDefined(this.groupsState[group])) {
      this.groupsState[group] = !this.groupsState[group];
      const enabled = Object.values(this.groupsState).some(v => v);
      if (this.enabled !== enabled) {
        this.enabled = enabled;
        if (this.enabled) {
          this.map.getMap().addLayer(this.featureGroup);
        } else {
          this.map.getMap().removeLayer(this.featureGroup);
        }
        return true;
      }
    }
    return false;
  }

  public updateData(dsData: FormattedData<TbMapDatasource>[]) {
    const layerData = dsData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
    const rawItems = layerData.filter(d => this.isValidLayerData(d));
    const toDelete = new Set(Array.from(this.layerItems.keys()));
    rawItems.forEach((data, index) => {
      let layerItem = this.layerItems.get(data.entityId);
      if (layerItem) {
        layerItem.update(data, dsData);
      } else {
        layerItem = this.createLayerItem(data, dsData);
        this.layerItems.set(data.entityId, layerItem);
      }
      toDelete.delete(data.entityId);
    });
    toDelete.forEach((key) => {
      const item = this.layerItems.get(key);
      item.remove();
      this.layerItems.delete(key);
    });
  }

  public getCtx(): WidgetContext {
    return this.map.getCtx();
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    return datasource;
  }

  protected mapType(): MapType {
    return this.map.type();
  }

  public abstract dataLayerType(): MapDataLayerType;

  protected abstract defaultBaseSettings(): Partial<S>;

  protected abstract doSetup(): Observable<void>;

  protected abstract isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean;

  protected abstract createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbDataLayerItem<S,L>;

}

class TbMarkerDataLayerItem extends TbDataLayerItem<MarkersDataLayerSettings, TbMarkersDataLayer> {

  private location: L.LatLng;
  private marker: L.Marker;
  private labelOffset: L.PointTuple;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: MarkersDataLayerSettings,
              protected dataLayer: TbMarkersDataLayer) {
    super(data, dsData, settings, dataLayer);
  }

  protected create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Layer {
    this.location = this.dataLayer.extractLocation(data);
    this.marker = L.marker(this.location, {
      tbMarkerData: data
    });

    this.updateMarkerIcon(data, dsData);

    return this.marker;
  }
  public update(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    const position = this.dataLayer.extractLocation(data);
    if (!this.marker.getLatLng().equals(position)) {
      this.location = position;
      this.marker.setLatLng(position);
    }
    this.updateMarkerIcon(data, dsData);
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
        this.updateMarkerLabel(data, dsData);
      }
    );
  }

  private updateMarkerLabel(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {

  }

}

abstract class MarkerIconProcessor<S> {

  static fromSettings(dataLayer: TbMarkersDataLayer,
                      settings: MarkersDataLayerSettings): MarkerIconProcessor<any> {
    switch (settings.markerType) {
      case MarkerType.default:
        return new ColorMarkerIconProcessor(dataLayer, settings.markerColor);
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

class ColorMarkerIconProcessor extends MarkerIconProcessor<DataLayerColorSettings> {

  private markerColorFunction: CompiledTbFunction<MapStringFunction>;

  private defaultMarkerIconInfo: MarkerIconInfo;

  constructor(protected dataLayer: TbMarkersDataLayer,
              protected settings: DataLayerColorSettings) {
    super(dataLayer, settings);
  }

  public setup(): Observable<void> {
    if (this.settings.type === DataLayerColorType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.colorFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.markerColorFunction = parsed;
          return null;
        })
      );
    } else {
      const color = tinycolor(this.settings.color);
      this.defaultMarkerIconInfo = this.dataLayer.createColoredMarkerIcon(color);
      return of(null)
    }
  }

  public createMarkerIcon(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): Observable<MarkerIconInfo> {
    if (this.settings.type === DataLayerColorType.function) {
      const functionColor = safeExecuteTbFunction(this.markerColorFunction, [data, dsData]);
      let color: tinycolor.Instance;
      if (isDefinedAndNotNull(functionColor)) {
        color = tinycolor(functionColor);
      } else {
        color = tinycolor(this.settings.color);
      }
      return of(this.dataLayer.createColoredMarkerIcon(color));
    } else {
      return of(this.defaultMarkerIconInfo);
    }
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
        map((aspectImage) => {
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
            return iconInfo;
          } else {
            return this.dataLayer.createDefaultMarkerIcon();
          }
        }),
        catchError(() => of(this.dataLayer.createDefaultMarkerIcon()))
      );
    } else {
      return of(this.dataLayer.createDefaultMarkerIcon());
    }
  }

}

export class TbMarkersDataLayer extends TbMapDataLayer<MarkersDataLayerSettings, TbMarkersDataLayer> {

  public markerIconProcessor: MarkerIconProcessor<any>;

  public markerOffset: L.LatLngTuple;
  public tooltipOffset: L.LatLngTuple;

  constructor(protected map: TbMap<any>,
              inputSettings: MarkersDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return MapDataLayerType.marker;
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    datasource.dataKeys.push(this.settings.xKey, this.settings.yKey);
    return datasource;
  }

  protected defaultBaseSettings(): Partial<MarkersDataLayerSettings> {
    return defaultBaseMarkersDataLayerSettings;
  }

  protected doSetup(): Observable<void> {
    this.markerOffset = [
      isDefined(this.settings.markerOffsetX) ? this.settings.markerOffsetX : 0.5,
      isDefined(this.settings.markerOffsetY) ? this.settings.markerOffsetY : 1,
    ];
    this.tooltipOffset = [0, -1];
    /*   this.tooltipOffset = [
         isDefined(this.settings.tooltipOffsetX) ? this.settings.tooltipOffsetX : 0,
         isDefined(this.settings.tooltipOffsetY) ? this.settings.tooltipOffsetY : -1,
       ];*/

    this.markerIconProcessor = MarkerIconProcessor.fromSettings(this, this.settings);
    return this.markerIconProcessor.setup();
  }

  protected isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean {
    return !!this.extractPosition(layerData);
  }

  protected createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbMarkerDataLayerItem {
    return new TbMarkerDataLayerItem(data, dsData, this.settings, this);
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

  public createDefaultMarkerIcon(): MarkerIconInfo {
    const color = this.settings.markerColor.color || '#FE7569';
    return this.createColoredMarkerIcon(tinycolor(color));
  }

  public createColoredMarkerIcon(color: tinycolor.Instance): MarkerIconInfo {
    return {
      size: [21, 34],
      icon: L.icon({
        iconUrl: createColorMarkerURI(color),
        iconSize: [21, 34],
        iconAnchor: [21 * this.markerOffset[0], 34 * this.markerOffset[1]],
        popupAnchor: [0, -34],
        shadowUrl: 'assets/shadow.png',
        shadowSize: [40, 37],
        shadowAnchor: [12, 35]
      })
    };
  }

  public extractLocation(data: FormattedData<TbMapDatasource>): L.LatLng {
    const position = this.extractPosition(data);
    if (position) {
      return this.map.positionToLatLng(position);
    } else {
      return null;
    }
  }
}

class TbPolygonDataLayerItem extends TbDataLayerItem<PolygonsDataLayerSettings, TbPolygonsDataLayer> {

  private polygon: L.Polygon;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: PolygonsDataLayerSettings,
              protected dataLayer: TbPolygonsDataLayer) {
    super(data, dsData, settings, dataLayer);
  }

  protected create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Layer {
    const polyData = this.dataLayer.extractPolygonCoordinates(data);
    const polyConstructor = isCutPolygon(polyData) || polyData.length !== 2 ? L.polygon : L.rectangle;
    this.polygon = polyConstructor(polyData, {
      fill: true,
      fillColor: '#3a77e7',
      color: '#0742ad',
      weight: 1,
      fillOpacity: 0.4,
      opacity: 1
    });
    return this.polygon;
  }
  public update(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    const polyData = this.dataLayer.extractPolygonCoordinates(data);
    if (isCutPolygon(polyData) || polyData.length !== 2) {
      if (this.polygon instanceof L.Rectangle) {
        this.polygon = L.polygon(polyData, {
          fill: true,
          fillColor: '#3a77e7',
          color: '#0742ad',
          weight: 1,
          fillOpacity: 0.4,
          opacity: 1
        });
        this.updateLayer(this.polygon);
      } else {
        this.polygon.setLatLngs(polyData);
      }
    } else if (polyData.length === 2) {
      const bounds = new L.LatLngBounds(polyData);
      // @ts-ignore
      this.leafletPoly.setBounds(bounds);
    }
  }
}

export class TbPolygonsDataLayer extends TbMapDataLayer<PolygonsDataLayerSettings, TbPolygonsDataLayer> {

  constructor(protected map: TbMap<any>,
              inputSettings: PolygonsDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return MapDataLayerType.polygon;
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    datasource.dataKeys.push(this.settings.polygonKey);
    return datasource;
  }

  protected defaultBaseSettings(): Partial<PolygonsDataLayerSettings> {
    return defaultBasePolygonsDataLayerSettings;
  }

  protected doSetup(): Observable<void> {
    return of(null);
  }

  protected isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean {
    return layerData && ((isNotEmptyStr(layerData[this.settings.polygonKey.label]) && !isJSON(layerData[this.settings.polygonKey.label])
      || Array.isArray(layerData[this.settings.polygonKey.label])));
  }

  protected createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbPolygonDataLayerItem {
    return new TbPolygonDataLayerItem(data, dsData, this.settings, this);
  }

  public extractPolygonCoordinates(data: FormattedData<TbMapDatasource>) {
    let rawPolyData = data[this.settings.polygonKey.label];
    if (isString(rawPolyData)) {
      rawPolyData = JSON.parse(rawPolyData);
    }
    return this.map.toPolygonCoordinates(rawPolyData);
  }
}

class TbCircleDataLayerItem extends TbDataLayerItem<CirclesDataLayerSettings, TbCirclesDataLayer> {

  private circle: L.Circle;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: CirclesDataLayerSettings,
              protected dataLayer: TbCirclesDataLayer) {
    super(data, dsData, settings, dataLayer);
  }

  protected create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Layer {
    const circleData = this.dataLayer.extractCircleCoordinates(data);
    const center = new L.LatLng(circleData.latitude, circleData.longitude);
    this.circle = L.circle(center, {
      radius: circleData.radius,
      fillColor: '#3a77e7',
      color: '#0742ad',
      weight: 1,
      fillOpacity: 0.4,
      opacity: 1
    });
    return this.circle;
  }

  public update(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    const circleData = this.dataLayer.extractCircleCoordinates(data);
    const center = new L.LatLng(circleData.latitude, circleData.longitude);
    if (!this.circle.getLatLng().equals(center)) {
      this.circle.setLatLng(center);
    }
    if (this.circle.getRadius() !== circleData.radius) {
      this.circle.setRadius(circleData.radius);
    }
  }
}

export class TbCirclesDataLayer extends TbMapDataLayer<CirclesDataLayerSettings, TbCirclesDataLayer> {

  constructor(protected map: TbMap<any>,
              inputSettings: CirclesDataLayerSettings) {
    super(map, inputSettings);
  }

  public dataLayerType(): MapDataLayerType {
    return MapDataLayerType.circle;
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    datasource.dataKeys.push(this.settings.circleKey);
    return datasource;
  }

  protected defaultBaseSettings(): Partial<CirclesDataLayerSettings> {
    return defaultBaseCirclesDataLayerSettings;
  }

  protected doSetup(): Observable<void> {
    return of(null);
  }

  protected isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean {
    return layerData && isNotEmptyStr(layerData[this.settings.circleKey.label]) && isJSON(layerData[this.settings.circleKey.label]);
  }

  protected createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbDataLayerItem<CirclesDataLayerSettings, TbCirclesDataLayer> {
    throw new TbCircleDataLayerItem(data, dsData, this.settings, this);
  }

  public extractCircleCoordinates(data: FormattedData<TbMapDatasource>) {
    const circleData: TbCircleData = JSON.parse(data[this.settings.circleKey.label]);
    return this.map.convertCircleData(circleData);
  }


}
