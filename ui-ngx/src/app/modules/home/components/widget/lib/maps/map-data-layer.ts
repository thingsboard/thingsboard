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
  CirclesDataLayerSettings,
  DataLayerColorSettings,
  DataLayerColorType,
  DataLayerPatternSettings,
  DataLayerPatternType,
  DataLayerTooltipTrigger,
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
  MarkerIconSettings,
  MarkerImageFunction,
  MarkerImageInfo,
  MarkerImageSettings,
  MarkerImageType,
  MarkersDataLayerSettings,
  MarkerShapeSettings,
  MarkerType,
  PolygonsDataLayerSettings,
  processTooltipTemplate,
  ShapeDataLayerSettings,
  TbCircleData,
  TbMapDatasource
} from '@home/components/widget/lib/maps/map.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { FormattedData } from '@shared/models/widget.models';
import { forkJoin, Observable, of } from 'rxjs';
import {
  createLabelFromPattern,
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
import L, { divIcon, LatLngBounds } from 'leaflet';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { catchError, map, switchMap } from 'rxjs/operators';
import tinycolor from 'tinycolor2';
import { WidgetContext } from '@home/models/widget-component.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import {
  createColorMarkerIconElement,
  createColorMarkerShapeURI,
  MarkerShape
} from '@home/components/widget/lib/maps/marker-shape.models';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

abstract class TbDataLayerItem<S extends MapDataLayerSettings, L extends TbMapDataLayer<S,L>> {

  protected layer: L.Layer;
  protected tooltip: L.Popup;

  protected constructor(data: FormattedData<TbMapDatasource>,
                        dsData: FormattedData<TbMapDatasource>[],
                        protected settings: S,
                        protected dataLayer: L) {
    this.layer = this.create(data, dsData);
    if (this.settings.tooltip?.show) {
      this.createTooltip(data.$datasource);
      this.updateTooltip(data, dsData);
    }
    this.createEventListeners(data, dsData);
    this.dataLayer.getFeatureGroup().addLayer(this.layer);
  }

  protected abstract create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Layer;

  protected abstract unbindLabel(): void;

  protected abstract bindLabel(content: L.Content): void;

  protected abstract createEventListeners(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void;

  public abstract update(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void;

  public remove() {
    this.layer.off();
    this.dataLayer.getFeatureGroup().removeLayer(this.layer);
  }

  protected updateTooltip(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    if (this.settings.tooltip.show) {
      let tooltipTemplate = this.dataLayer.dataLayerTooltipProcessor.processPattern(data, dsData);
      tooltipTemplate = processTooltipTemplate(tooltipTemplate);
      this.tooltip.setContent(tooltipTemplate);
      if (this.tooltip.isOpen() && this.tooltip.getElement()) {
        this.bindTooltipActions(data.$datasource);
      }
    }
  }

  protected updateLabel(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    if (this.settings.label.show) {
      this.unbindLabel();
      const label = this.dataLayer.dataLayerLabelProcessor.processPattern(data, dsData);
      const labelColor = this.dataLayer.getCtx().widgetConfig.color;
      const content: L.Content = `<div style="color: ${labelColor};"><b>${label}</b></div>`;
      this.bindLabel(content);
    }
  }

  private createTooltip(datasource: TbMapDatasource) {
    this.tooltip = L.popup();
    this.layer.bindPopup(this.tooltip, {autoClose: this.settings.tooltip.autoclose, closeOnClick: false});
    if (this.settings.tooltip.trigger === DataLayerTooltipTrigger.hover) {
      this.layer.off('click');
      this.layer.on('mouseover', () => {
        this.layer.openPopup();
      });
      this.layer.on('mousemove', (e) => {
        this.tooltip.setLatLng(e.latlng);
      });
      this.layer.on('mouseout', () => {
        this.layer.closePopup();
      });
    }
    this.layer.on('popupopen', () => {
      this.bindTooltipActions(datasource);
      (this.layer as any)._popup._closeButton.addEventListener('click', (event: Event) => {
        event.preventDefault();
      });
    });
  }

  private bindTooltipActions(datasource: TbMapDatasource) {
    const actions = this.tooltip.getElement().getElementsByClassName('tb-custom-action');
    Array.from(actions).forEach(
      (element: HTMLElement) => {
        const actionName = element.getAttribute('data-action-name');
        this.dataLayer.getMap().tooltipElementClick(element, actionName, datasource);
      });
  }

}

export enum MapDataLayerType {
   marker = 'marker',
   polygon = 'polygon',
   circle = 'circle'
}

class DataLayerPatternProcessor {

  private patternFunction: CompiledTbFunction<MapStringFunction>;
  private pattern: string;

  constructor(private dataLayer: TbMapDataLayer<any, any>,
              private settings: DataLayerPatternSettings) {}

  public setup(): Observable<void> {
    if (this.settings.type === DataLayerPatternType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.patternFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.patternFunction = parsed;
          return null;
        })
      );
    } else {
      this.pattern = this.settings.pattern;
      return of(null)
    }
  }

  public processPattern(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): string {
    let pattern: string;
    if (this.settings.type === DataLayerPatternType.function) {
      pattern = safeExecuteTbFunction(this.patternFunction, [data, dsData]);
    } else {
      pattern = this.pattern;
    }
    const text = createLabelFromPattern(pattern, data);
    const customTranslate = this.dataLayer.getCtx().$injector.get(CustomTranslatePipe);
    return customTranslate.transform(text);
  }

}

class DataLayerColorProcessor {

  private colorFunction: CompiledTbFunction<MapStringFunction>;
  private color: string;

  constructor(private dataLayer: TbMapDataLayer<any, any>,
              private settings: DataLayerColorSettings) {}

  public setup(): Observable<void> {
    if (this.settings.type === DataLayerColorType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.colorFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.colorFunction = parsed;
          return null;
        })
      );
    } else {
      this.color = this.settings.color;
      return of(null)
    }
  }

  public processColor(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): string {
    let color: string;
    if (this.settings.type === DataLayerColorType.function) {
      color = safeExecuteTbFunction(this.colorFunction, [data, dsData]);
    } else {
      color = this.color;
    }
    return color;
  }

}

export abstract class TbMapDataLayer<S extends MapDataLayerSettings, L extends TbMapDataLayer<S,L>> implements L.TB.DataLayer {

  protected settings: S;

  protected datasource: TbMapDatasource;

  protected mapDataId = guid();

  protected featureGroup = L.featureGroup();

  protected layerItems = new Map<string, TbDataLayerItem<S,L>>();

  protected groupsState: {[group: string]: boolean} = {};

  protected enabled = true;

  public dataLayerLabelProcessor: DataLayerPatternProcessor;
  public dataLayerTooltipProcessor: DataLayerPatternProcessor;

  protected constructor(protected map: TbMap<any>,
                        inputSettings: S) {
    this.settings = mergeDeepIgnoreArray({} as S, this.defaultBaseSettings() as S, inputSettings);
    if (this.settings.groups?.length) {
      this.settings.groups.forEach((group) => {
        this.groupsState[group] = true;
      });
    }
    this.dataLayerLabelProcessor = this.settings.label.show ? new DataLayerPatternProcessor(this, this.settings.label) : null;
    this.dataLayerTooltipProcessor = this.settings.tooltip.show ? new DataLayerPatternProcessor(this, this.settings.tooltip): null;
    this.map.getMap().addLayer(this.featureGroup);
  }

  public setup(): Observable<any> {
    this.datasource = mapDataSourceSettingsToDatasource(this.settings);
    this.datasource.dataKeys = this.settings.additionalDataKeys ? [...this.settings.additionalDataKeys] : [];
    this.mapDataId = this.datasource.mapDataIds[0];
    this.datasource = this.setupDatasource(this.datasource);
    return forkJoin(
      [
        this.dataLayerLabelProcessor ? this.dataLayerLabelProcessor.setup() : of(null),
        this.dataLayerTooltipProcessor ? this.dataLayerTooltipProcessor.setup() : of(null),
        this.doSetup()
      ]);
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
    rawItems.forEach((data) => {
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
  public getMap(): TbMap<any> {
    return this.map;
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    return datasource;
  }

  protected mapType(): MapType {
    return this.map.type();
  }

  public abstract dataLayerType(): MapDataLayerType;

  protected abstract defaultBaseSettings(): Partial<S>;

  protected abstract doSetup(): Observable<any>;

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

  protected createEventListeners(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.dataLayer.getMap().markerClick(this.marker, data.$datasource);
  }

  protected unbindLabel() {
    this.marker.unbindTooltip();
  }

  protected bindLabel(content: L.Content): void {
    this.marker.bindTooltip(content, { className: 'tb-marker-label', permanent: true, direction: 'top', offset: this.labelOffset });
  }

  public update(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    const position = this.dataLayer.extractLocation(data);
    if (!this.marker.getLatLng().equals(position)) {
      this.location = position;
      this.marker.setLatLng(position);
    }
    this.updateTooltip(data, dsData);
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
    this.tooltipOffset = [
      isDefined(this.settings.tooltip?.offsetX) ? this.settings.tooltip?.offsetX : 0,
      isDefined(this.settings.tooltip?.offsetY) ? this.settings.tooltip?.offsetY : -1,
    ];

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

  private polygonContainer: L.FeatureGroup;
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
    const style = this.dataLayer.getShapeStyle(data, dsData);
    this.polygon = polyConstructor(polyData, {
      ...style
    });

    this.polygonContainer = L.featureGroup();
    this.polygon.addTo(this.polygonContainer);

    this.updateLabel(data, dsData);
    return this.polygonContainer;
  }

  protected createEventListeners(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.dataLayer.getMap().polygonClick(this.polygonContainer, data.$datasource);
  }

  protected unbindLabel() {
    this.polygonContainer.unbindTooltip();
  }

  protected bindLabel(content: L.Content): void {
    this.polygonContainer.bindTooltip(content, {className: 'tb-polygon-label', permanent: true, direction: 'center'})
      .openTooltip(this.polygonContainer.getBounds().getCenter());
  }

  public update(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    const polyData = this.dataLayer.extractPolygonCoordinates(data);
    const style = this.dataLayer.getShapeStyle(data, dsData);
    if (isCutPolygon(polyData) || polyData.length !== 2) {
      if (this.polygon instanceof L.Rectangle) {
        this.polygonContainer.removeLayer(this.polygon);
        this.polygon = L.polygon(polyData, {
          ...style
        });
        this.polygon.addTo(this.polygonContainer);
      } else {
        this.polygon.setLatLngs(polyData);
      }
    } else if (polyData.length === 2) {
      const bounds = new L.LatLngBounds(polyData);
      // @ts-ignore
      this.leafletPoly.setBounds(bounds);
    }
    this.updateTooltip(data, dsData);
    this.updateLabel(data, dsData);
    this.polygon.setStyle(style);
  }
}

abstract class TbShapesDataLayer<S extends ShapeDataLayerSettings, L extends TbMapDataLayer<S,L>> extends TbMapDataLayer<S, L> {

  public fillColorProcessor: DataLayerColorProcessor;
  public strokeColorProcessor: DataLayerColorProcessor;

  protected constructor(protected map: TbMap<any>,
              inputSettings: S) {
    super(map, inputSettings);
  }

  protected doSetup(): Observable<any> {
    this.fillColorProcessor = new DataLayerColorProcessor(this, this.settings.fillColor);
    this.strokeColorProcessor = new DataLayerColorProcessor(this, this.settings.strokeColor);
    return forkJoin([this.fillColorProcessor.setup(), this.strokeColorProcessor.setup()]);
  }

  public getShapeStyle(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.PathOptions {
    const fill = this.fillColorProcessor.processColor(data, dsData);
    const stroke = this.strokeColorProcessor.processColor(data, dsData);
    return {
      fill: true,
      fillColor: fill,
      color: stroke,
      weight: this.settings.strokeWeight,
      fillOpacity: 1,
      opacity: 1
    };
  }
}

export class TbPolygonsDataLayer extends TbShapesDataLayer<PolygonsDataLayerSettings, TbPolygonsDataLayer> {

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

  protected doSetup(): Observable<any> {
    return super.doSetup();
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
    const style = this.dataLayer.getShapeStyle(data, dsData);
    this.circle = L.circle(center, {
      radius: circleData.radius,
      ...style
    });
    this.updateLabel(data, dsData);
    return this.circle;
  }

  protected createEventListeners(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.dataLayer.getMap().circleClick(this.circle, data.$datasource);
  }

  protected unbindLabel() {
    this.circle.unbindTooltip();
  }

  protected bindLabel(content: L.Content): void {
    this.circle.bindTooltip(content, { className: 'tb-polygon-label', permanent: true, direction: 'center'})
      .openTooltip(this.circle.getLatLng());
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
    this.updateTooltip(data, dsData);
    this.updateLabel(data, dsData);
    const style = this.dataLayer.getShapeStyle(data, dsData);
    this.circle.setStyle(style);
  }
}

export class TbCirclesDataLayer extends TbShapesDataLayer<CirclesDataLayerSettings, TbCirclesDataLayer> {

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
    return super.doSetup();
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
