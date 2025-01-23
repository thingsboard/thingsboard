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
  defaultBasePolygonsDataLayerSettings,
  isCutPolygon, isJSON,
  PolygonsDataLayerSettings,
  TbMapDatasource
} from '@home/components/widget/lib/maps/models/map.models';
import L from 'leaflet';
import { FormattedData } from '@shared/models/widget.models';
import { TbShapesDataLayer } from '@home/components/widget/lib/maps/data-layer/shapes-data-layer';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { Observable } from 'rxjs';
import { isNotEmptyStr, isString } from '@core/utils';
import { MapDataLayerType, TbDataLayerItem } from '@home/components/widget/lib/maps/data-layer/map-data-layer';

class TbPolygonDataLayerItem extends TbDataLayerItem<PolygonsDataLayerSettings, TbPolygonsDataLayer> {

  private polygonContainer: L.FeatureGroup;
  private polygon: L.Polygon;
  private polygonStyle: L.PathOptions;

  constructor(data: FormattedData<TbMapDatasource>,
              dsData: FormattedData<TbMapDatasource>[],
              protected settings: PolygonsDataLayerSettings,
              protected dataLayer: TbPolygonsDataLayer) {
    super(data, dsData, settings, dataLayer);
  }

  protected create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.Layer {
    const polyData = this.dataLayer.extractPolygonCoordinates(data);
    const polyConstructor = isCutPolygon(polyData) || polyData.length !== 2 ? L.polygon : L.rectangle;
    this.polygonStyle = this.dataLayer.getShapeStyle(data, dsData);
    this.polygon = polyConstructor(polyData, {
      ...this.polygonStyle
    });

    this.polygonContainer = L.featureGroup();
    this.polygon.addTo(this.polygonContainer);

    this.updateLabel(data, dsData);
    return this.polygonContainer;
  }

  protected createEventListeners(data: FormattedData<TbMapDatasource>, _dsData: FormattedData<TbMapDatasource>[]): void {
    this.dataLayer.getMap().polygonClick(this.polygonContainer, data.$datasource);
  }

  protected unbindLabel() {
    this.polygonContainer.unbindTooltip();
  }

  protected bindLabel(content: L.Content): void {
    this.polygonContainer.bindTooltip(content, {className: 'tb-polygon-label', permanent: true, direction: 'center'})
    .openTooltip(this.polygonContainer.getBounds().getCenter());
  }

  protected doUpdate(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.polygonStyle = this.dataLayer.getShapeStyle(data, dsData);
    this.updatePolygonShape(data);
    this.updateTooltip(data, dsData);
    this.updateLabel(data, dsData);
    this.polygon.setStyle(this.polygonStyle);
  }

  protected doInvalidateCoordinates(data: FormattedData<TbMapDatasource>, _dsData: FormattedData<TbMapDatasource>[]): void {
    this.updatePolygonShape(data);
  }

  private updatePolygonShape(data: FormattedData<TbMapDatasource>) {
    const polyData = this.dataLayer.extractPolygonCoordinates(data);
    if (isCutPolygon(polyData) || polyData.length !== 2) {
      if (this.polygon instanceof L.Rectangle) {
        this.polygonContainer.removeLayer(this.polygon);
        this.polygon = L.polygon(polyData, {
          ...this.polygonStyle
        });
        this.polygon.addTo(this.polygonContainer);
      } else {
        this.polygon.setLatLngs(polyData);
      }
    } else if (polyData.length === 2) {
      const bounds = new L.LatLngBounds(polyData);
      (this.polygon as L.Rectangle).setBounds(bounds);
    }
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

  protected defaultBaseSettings(map: TbMap<any>): Partial<PolygonsDataLayerSettings> {
    return defaultBasePolygonsDataLayerSettings(map.type());
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
