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
  DataLayerColorSettings,
  DataLayerColorType,
  DataLayerPatternSettings,
  DataLayerPatternType,
  MapDataLayerSettings,
  MapDataLayerType,
  mapDataSourceSettingsToDatasource,
  MapStringFunction,
  MapType,
  TbMapDatasource
} from '@shared/models/widget/maps/map.models';
import {
  createLabelFromPattern,
  guid,
  isDefined,
  isDefinedAndNotNull,
  isNumber,
  isNumeric,
  mergeDeepIgnoreArray,
  parseTbFunction,
  safeExecuteTbFunction
} from '@core/utils';
import L from 'leaflet';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { forkJoin, Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { DataKey, DatasourceType, FormattedData } from '@shared/models/widget.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { WidgetContext } from '@home/models/widget-component.models';
import { ColorRange } from '@shared/models/widget-settings.models';

export class DataLayerPatternProcessor {

  private patternFunction: CompiledTbFunction<MapStringFunction>;
  private pattern: string;

  constructor(private dataLayer: TbMapDataLayer,
              private settings: DataLayerPatternSettings) {}

  public setup(): Observable<void> {
    if (this.settings.type === DataLayerPatternType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.patternFunction, ['data', 'dsData', 'ctx']).pipe(
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
      pattern = safeExecuteTbFunction(this.patternFunction, [data, dsData, this.dataLayer.getCtx()]);
    } else {
      pattern = this.pattern;
    }
    const text = createLabelFromPattern(pattern, data);
    const customTranslate = this.dataLayer.getCtx().$injector.get(CustomTranslatePipe);
    return customTranslate.transform(text);
  }

}

export class DataLayerColorProcessor {

  private colorFunction: CompiledTbFunction<MapStringFunction>;
  private color: string;
  private rangeKey: DataKey;
  private range: ColorRange[];

  constructor(private dataLayer: TbMapDataLayer,
              private settings: DataLayerColorSettings) {}

  public setup(): Observable<void> {
    this.color = this.settings.color;
    if (this.settings.type === DataLayerColorType.range) {
      this.rangeKey = this.settings.rangeKey;
      this.range = this.settings.range;
    } else if (this.settings.type === DataLayerColorType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.colorFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.colorFunction = parsed;
          return null;
        })
      );
    }
    return of(null)
  }

  public processColor(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): string {
    let color: string;
    if (this.settings.type === DataLayerColorType.function) {
      color = safeExecuteTbFunction(this.colorFunction, [data, dsData]);
      if (!color) {
        color = this.color;
      }
    } else if (this.settings.type === DataLayerColorType.range) {
      color = this.color;
      if (this.rangeKey && this.range?.length) {
        const value = data[this.rangeKey.label];
        if (isDefinedAndNotNull(value) && isNumeric(value)) {
          const num = Number(value);
          for (const range of this.range) {
            if (DataLayerColorProcessor.constantRange(range) && range.from === num) {
              color = range.color;
              break;
            } else if ((!isNumber(range.from) || num >= range.from) && (!isNumber(range.to) || num < range.to)) {
              color = range.color;
              break;
            }
          }
        }
      }
    } else {
      color = this.color;
    }
    return color;
  }

  static constantRange(range: ColorRange): boolean {
    return isNumber(range.from) && isNumber(range.to) && range.from === range.to;
  }

}

export abstract class TbDataLayerItem<S extends MapDataLayerSettings = MapDataLayerSettings, D extends TbMapDataLayer = TbMapDataLayer, L extends L.Layer = L.Layer> {

  protected layer: L;

  protected constructor(protected settings: S,
                        protected dataLayer: D) {}

  public getLayer(): L {
    return this.layer;
  }

  public getDataLayer(): D {
    return this.dataLayer;
  }

  public abstract remove(): void;

  public abstract invalidateCoordinates(): void;

}

export abstract class TbMapDataLayer<S extends MapDataLayerSettings = MapDataLayerSettings, I extends TbDataLayerItem = any> {

  protected settings: S;

  protected dataSources: TbMapDatasource[];

  protected mapDataId: string;

  protected dataLayerContainer: L.FeatureGroup;

  protected layerItems = new Map<string, I>();

  protected groupsState: {[group: string]: boolean} = {};

  protected enabled = true;

  protected snappable = false;

  public dataLayerLabelProcessor: DataLayerPatternProcessor;
  public dataLayerTooltipProcessor: DataLayerPatternProcessor;

  protected constructor(protected map: TbMap<any>,
                        inputSettings: S) {
    this.settings = mergeDeepIgnoreArray({} as S, this.defaultBaseSettings(map) as S, inputSettings);
    if (this.settings.groups?.length) {
      this.settings.groups.forEach((group) => {
        this.groupsState[group] = true;
      });
    }
    this.dataLayerContainer = this.createDataLayerContainer();
    this.dataLayerLabelProcessor = this.settings.label.show ? new DataLayerPatternProcessor(this, this.settings.label) : null;
    this.dataLayerTooltipProcessor = this.settings.tooltip.show ? new DataLayerPatternProcessor(this, this.settings.tooltip): null;
    this.map.getMap().addLayer(this.dataLayerContainer);
  }

  public setup(): Observable<any> {
    this.dataSources = [];
    const datasource = mapDataSourceSettingsToDatasource(this.settings);
    this.mapDataId = datasource.mapDataIds[0];
    this.dataSources.push(datasource);
    if (this.settings.additionalDataSources?.length && datasource.type !== DatasourceType.function) {
      this.dataSources.push(...this.settings.additionalDataSources.map(ds => mapDataSourceSettingsToDatasource(ds, this.mapDataId)));
    }
    const dataKeys = this.calculateDataKeys();
    const latestDataKeys = this.calculateLatestDataKeys();
    this.dataSources.forEach(ds => ds.dataKeys.push(...dataKeys));
    if (latestDataKeys?.length) {
      this.dataSources.forEach(ds => ds.latestDataKeys.push(...latestDataKeys));
    }
    return forkJoin(
      [
        this.dataLayerLabelProcessor ? this.dataLayerLabelProcessor.setup() : of(null),
        this.dataLayerTooltipProcessor ? this.dataLayerTooltipProcessor.setup() : of(null),
        this.doSetup()
      ]);
  }

  public removeItem(key: string): void {
    const item = this.layerItems.get(key);
    if (item) {
      item.remove();
      this.layerItems.delete(key);
    }
  }

  public invalidateCoordinates(): void {
    this.layerItems.forEach(item => item.invalidateCoordinates());
  }

  public getCtx(): WidgetContext {
    return this.map.getCtx();
  }

  public getMap(): TbMap<any> {
    return this.map;
  }

  public mapType(): MapType {
    return this.map.type();
  }

  public getDataSources(): TbMapDatasource[] {
    return this.dataSources;
  }

  public getDataLayerContainer(): L.FeatureGroup {
    return this.dataLayerContainer;
  }

  public getBounds(): L.LatLngBounds {
    return this.dataLayerContainer.getBounds();
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
          this.map.getMap().addLayer(this.dataLayerContainer);
          this.onDataLayerEnabled();
        } else {
          this.onDataLayerDisabled();
          this.map.getMap().removeLayer(this.dataLayerContainer);
        }
        this.map.enabledDataLayersUpdated();
        return true;
      }
    }
    return false;
  }

  public hasData(data: FormattedData<TbMapDatasource>): boolean {
    return data.$datasource.mapDataIds.includes(this.mapDataId);
  }

  protected createDataLayerContainer(): L.FeatureGroup {
    return L.featureGroup([], {snapIgnore: true});
  }

  protected calculateDataKeys(): DataKey[] {
    const dataKeys = this.settings.additionalDataKeys ? [...this.settings.additionalDataKeys] : [];
    const colorRangeKeys = this.allColorSettings().filter(settings => settings.type === DataLayerColorType.range && settings.rangeKey)
      .map(settings => settings.rangeKey);
    dataKeys.push(...colorRangeKeys);
    dataKeys.push(...this.getDataKeys());
    return dataKeys;
  }

  protected calculateLatestDataKeys(): DataKey[] {
    return [];
  }

  protected getDataKeys(): DataKey[] {
    return [];
  }

  protected allColorSettings(): DataLayerColorSettings[] {
    return [];
  }

  protected onDataLayerEnabled(): void {}

  protected onDataLayerDisabled(): void {}

  public abstract dataLayerType(): MapDataLayerType;

  protected abstract defaultBaseSettings(map: TbMap<any>): Partial<S>;

  protected abstract doSetup(): Observable<any>;

}

