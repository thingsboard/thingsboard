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
  MapDataLayerSettings, mapDataSourceSettingsToDatasource,
  MarkersDataLayerSettings, PolygonsDataLayerSettings, TbMapDatasource
} from '@home/components/widget/lib/maps/map.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { FormattedData } from '@shared/models/widget.models';
import { Observable, of } from 'rxjs';
import { guid } from '@core/utils';
import L from 'leaflet';

abstract class TbDataLayerItem<S extends MapDataLayerSettings, L extends TbMapDataLayer<S>> {

  protected layer: L.Layer;

}

export enum MapDataLayerType {
   marker = 'marker',
   polygon = 'polygon',
   circle = 'circle'
}

export abstract class TbMapDataLayer<S extends MapDataLayerSettings> {

  protected datasource: TbMapDatasource;

  protected mapDataId = guid();

  protected constructor(protected map: TbMap<any>,
                        protected settings: S) {
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

  public updateData(dsData: FormattedData<TbMapDatasource>[]) {
    const layerData = dsData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
    this.onData(layerData, dsData);
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    return datasource;
  }

  public abstract dataLayerType(): MapDataLayerType;

  protected abstract doSetup(): Observable<void>;

  protected abstract onData(layerData: FormattedData<TbMapDatasource>[], dsData: FormattedData<TbMapDatasource>[]);

}

export class TbMarkersDataLayer extends TbMapDataLayer<MarkersDataLayerSettings> {

  constructor(protected map: TbMap<any>,
              protected settings: MarkersDataLayerSettings) {
    super(map, settings);
  }

  public dataLayerType(): MapDataLayerType {
    return MapDataLayerType.marker;
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    datasource.dataKeys.push(this.settings.xKey, this.settings.yKey);
    return datasource;
  }

  protected doSetup(): Observable<void> {
    return of(null);
  }

  protected onData(layerData: FormattedData<TbMapDatasource>[], dsData: FormattedData<TbMapDatasource>[]) {
    layerData.forEach((data, index) => {
      console.log(`[${this.mapDataId}][${index}]: Markers layer data updated!`);
      console.log(data);
      this.markerData(data, dsData);
    });
  }

  private markerData(data: FormattedData, dsData: FormattedData[]) {
    const xKeyVal = data[this.settings.xKey.label];
    const yKeyVal = data[this.settings.yKey.label];
  }

}

export class TbPolygonsDataLayer extends TbMapDataLayer<PolygonsDataLayerSettings> {

  constructor(protected map: TbMap<any>,
              protected settings: PolygonsDataLayerSettings) {
    super(map, settings);
  }

  public dataLayerType(): MapDataLayerType {
    return MapDataLayerType.polygon;
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    datasource.dataKeys.push(this.settings.polygonKey);
    return datasource;
  }

  protected doSetup(): Observable<void> {
    return of(null);
  }

  protected onData(layerData: FormattedData<TbMapDatasource>[], dsData: FormattedData<TbMapDatasource>[]) {
    layerData.forEach((data, index) => {
      console.log(`[${this.mapDataId}][${index}]: Polygons layer data updated!`);
      console.log(data);
    });
  }

}

export class TbCirclesDataLayer extends TbMapDataLayer<CirclesDataLayerSettings> {

  constructor(protected map: TbMap<any>,
              protected settings: CirclesDataLayerSettings) {
    super(map, settings);
  }

  public dataLayerType(): MapDataLayerType {
    return MapDataLayerType.circle;
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    datasource.dataKeys.push(this.settings.circleKey);
    return datasource;
  }

  protected doSetup(): Observable<void> {
    return of(null);
  }

  protected onData(layerData: FormattedData<TbMapDatasource>[], dsData: FormattedData<TbMapDatasource>[]) {
    layerData.forEach((data, index) => {
      console.log(`[${this.mapDataId}][${index}]: Circles layer data updated!`);
      console.log(data);
    });
  }

}
