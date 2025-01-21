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

import { ShapeDataLayerSettings, TbMapDatasource } from '@home/components/widget/lib/maps/models/map.models';
import L from 'leaflet';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { forkJoin, Observable } from 'rxjs';
import { FormattedData } from '@shared/models/widget.models';
import { DataLayerColorProcessor, TbMapDataLayer } from '@home/components/widget/lib/maps/data-layer/map-data-layer';

export abstract class TbShapesDataLayer<S extends ShapeDataLayerSettings, L extends TbMapDataLayer<S,L>> extends TbMapDataLayer<S, L> {

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
