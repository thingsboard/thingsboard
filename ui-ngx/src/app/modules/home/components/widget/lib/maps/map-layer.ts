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
  CustomMapLayerSettings, defaultCustomMapLayerSettings,
  defaultGoogleMapLayerSettings, defaultHereMapLayerSettings,
  defaultOpenStreetMapLayerSettings,
  defaultTencentMapLayerSettings,
  GoogleLayerType,
  GoogleMapLayerSettings,
  googleMapLayerTranslationMap, hereLayerTranslationMap, HereLayerType, HereMapLayerSettings,
  MapLayerSettings,
  MapProvider,
  OpenStreetLayerType,
  OpenStreetMapLayerSettings,
  openStreetMapLayerTranslationMap,
  tencentLayerTranslationMap,
  TencentLayerType,
  TencentMapLayerSettings
} from '@home/components/widget/lib/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { mergeDeep } from '@core/utils';
import { Observable, of, switchMap } from 'rxjs';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import L from 'leaflet';
import { catchError, map } from 'rxjs/operators';
import { ResourcesService } from '@core/services/resources.service';

export abstract class TbMapLayer<S extends MapLayerSettings> {

  static fromSettings(ctx: WidgetContext,
                      inputSettings: DeepPartial<MapLayerSettings>) {

    switch (inputSettings.provider) {
      case MapProvider.google:
        return new TbGoogleMapLayer(ctx, inputSettings);
      case MapProvider.openstreet:
        return new TbOpenStreetMapLayer(ctx, inputSettings);
      case MapProvider.tencent:
        return new TbTencentMapLayer(ctx, inputSettings);
      case MapProvider.here:
        return new TbHereMapLayer(ctx, inputSettings);
      case MapProvider.custom:
        return new TbCustomMapLayer(ctx, inputSettings);
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

  public loadLayer(theMap: L.Map): Observable<L.TB.LayerData> {
    return this.createLayer().pipe(
      switchMap((layer) => {
        if (layer) {
          return this.createLayer().pipe(
            map((mini) => {
              if (mini) {
                const attribution = layer.getAttribution();
                const attributionPrefix = attribution ? theMap.attributionControl.options.prefix as string : null;
                return {
                  title: this.title(),
                  attributionPrefix: attributionPrefix,
                  layer,
                  mini
                };
              } else {
                return null;
              }
            })
          );
        } else {
          return of(null);
        }
      })
    );
  }
}

class TbGoogleMapLayer extends TbMapLayer<GoogleMapLayerSettings> {

  static loadedApiKeysGlobal: {[key: string]: boolean} = {};

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): GoogleMapLayerSettings {
    return defaultGoogleMapLayerSettings;
  }

  protected generateTitle(): string {
    const layerType = GoogleLayerType[this.settings.layerType];
    return this.ctx.translate.instant(googleMapLayerTranslationMap.get(layerType));
  }

  protected createLayer(): Observable<L.Layer> {
    return this.loadGoogle().pipe(
      map((loaded) => {
        if (loaded) {
          return (L.gridLayer as any).googleMutant({
            type: this.settings.layerType
          });
        } else {
          return null;
        }
      })
    );
  }

  private loadGoogle(): Observable<boolean> {
    const apiKey = this.settings.apiKey;
    if (TbGoogleMapLayer.loadedApiKeysGlobal[apiKey]) {
      return of(true);
    } else {
      const resourceService = this.ctx.$injector.get(ResourcesService);
      return resourceService.loadResource(`https://maps.googleapis.com/maps/api/js?key=${apiKey}`).pipe(
        map(() => {
          TbGoogleMapLayer.loadedApiKeysGlobal[apiKey] = true;
          return true;
        }),
        catchError((e) => {
          TbGoogleMapLayer.loadedApiKeysGlobal[apiKey] = false;
          console.error(`Google map api load failed!`, e);
          return of(false);
        })
      );
    }
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

class TbTencentMapLayer extends TbMapLayer<TencentMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): TencentMapLayerSettings {
    return defaultTencentMapLayerSettings;
  }

  protected generateTitle(): string {
    const layerType = TencentLayerType[this.settings.layerType];
    return this.ctx.translate.instant(tencentLayerTranslationMap.get(layerType));
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.TB.tileLayer.chinaProvider(TencentLayerType[this.settings.layerType], {
      attribution: '&copy;2024 Tencent - GS(2023)1171号'
    });
    return of(layer);
  }

}

class TbHereMapLayer extends TbMapLayer<HereMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): HereMapLayerSettings {
    return defaultHereMapLayerSettings;
  }

  protected generateTitle(): string {
    const layerType = HereLayerType[this.settings.layerType];
    return this.ctx.translate.instant(hereLayerTranslationMap.get(layerType));
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.tileLayer.provider(HereLayerType[this.settings.layerType], {useV3: true, apiKey: this.settings.apiKey} as any);
    return of(layer);
  }

}

class TbCustomMapLayer extends TbMapLayer<CustomMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): CustomMapLayerSettings {
    return defaultCustomMapLayerSettings;
  }

  protected generateTitle(): string {
    return this.ctx.translate.instant('widgets.maps.custom');
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.tileLayer(this.settings.tileUrl);
    return of(layer);
  }

}