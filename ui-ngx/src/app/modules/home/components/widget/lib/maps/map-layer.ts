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
  defaultOpenStreetMapLayerSettings,
  MapLayerSettings,
  MapProvider, OpenStreetLayerType,
  OpenStreetMapLayerSettings, openStreetMapLayerTranslationMap
} from '@home/components/widget/lib/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { mergeDeep } from '@core/utils';
import { Observable, of, switchMap } from 'rxjs';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import L from 'leaflet';
import { map } from 'rxjs/operators';

export abstract class TbMapLayer<S extends MapLayerSettings> {

  static fromSettings(ctx: WidgetContext,
                      inputSettings: DeepPartial<MapLayerSettings>) {

    switch (inputSettings.provider) {
      case MapProvider.google:
        break;
      case MapProvider.openstreet:
        return new TbOpenStreetMapLayer(ctx, inputSettings);
      case MapProvider.tencent:
        break;
      case MapProvider.here:
        break;
      case MapProvider.custom:
        break;
    }
  }

  protected settings: S;

  protected constructor(protected ctx: WidgetContext,
                        protected inputSettings: DeepPartial<MapLayerSettings>) {
    this.settings = mergeDeep({} as S, this.defaultSettings(), this.inputSettings as S);
  }

  protected abstract defaultSettings(): S;

  protected title(): string {
    const customTranslate = this.ctx.$injector.get(CustomTranslatePipe);
    if (this.settings.label) {
      return customTranslate.transform(this.settings.label);
    } else {
      return this.generateTitle();
    }
  }

  protected abstract generateTitle(): string;

  protected abstract createLayer(): Observable<L.Layer>;

  public loadLayer(): Observable<L.TB.LayerData> {
    return this.createLayer().pipe(
      switchMap((layer) => {
        return this.createLayer().pipe(
          map((mini) => {
            return {
              title: this.title(),
              layer,
              mini
            };
          })
        );
      })
    );
  }

}

class TbOpenStreetMapLayer extends TbMapLayer<OpenStreetMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
      super(ctx, inputSettings);
  }

  protected defaultSettings(): OpenStreetMapLayerSettings {
      return defaultOpenStreetMapLayerSettings;
  }

  protected generateTitle(): string {
    const layerType = OpenStreetLayerType[this.settings.layerType];
    return this.ctx.translate.instant(openStreetMapLayerTranslationMap.get(layerType));
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.tileLayer.provider(OpenStreetLayerType[this.settings.layerType]);
    return of(layer);
  }

}
