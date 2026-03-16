///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  CustomMapLayerSettings,
  defaultCustomMapLayerSettings,
  defaultGoogleMapLayerSettings,
  defaultHereMapLayerSettings,
  defaultLayerTitle,
  defaultOpenStreetMapLayerSettings,
  defaultTencentMapLayerSettings,
  GoogleMapLayerSettings,
  HereMapLayerSettings,
  MapLayerSettings,
  MapProvider,
  OpenStreetMapLayerSettings,
  ReferenceLayerType,
  TencentMapLayerSettings
} from '@shared/models/widget/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { mergeDeep } from '@core/utils';
import { Observable, of, shareReplay, switchMap } from 'rxjs';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import L from 'leaflet';
import { catchError, map } from 'rxjs/operators';
import { ResourcesService } from '@core/services/resources.service';
import { StyleSpecification } from '@maplibre/maplibre-gl-style-spec';

const referenceLayerStyleUrlMap = new Map<ReferenceLayerType, string>(
  [
    [ReferenceLayerType.openstreetmap_hybrid, '/assets/map/openstreetmap_hybrid_reference_style.json'],
    [ReferenceLayerType.world_edition_hybrid, '/assets/map/world_edition_hybrid_reference_style.json'],
    [ReferenceLayerType.enhanced_contrast_hybrid, '/assets/map/enhanced_contrast_hybrid_reference_style.json']
  ]
);

const referenceLayerCache = new Map<ReferenceLayerType, Observable<StyleSpecification>>();

interface TbMapLayerData {
  layer: L.Layer;
  attribution: boolean;
}

export abstract class TbMapLayer<S extends MapLayerSettings> {

  static fromSettings(ctx: WidgetContext,
                      inputSettings: DeepPartial<MapLayerSettings>) {

    switch (inputSettings.provider) {
      case MapProvider.openstreet:
        return new TbOpenStreetMapLayer(ctx, inputSettings);
      case MapProvider.google:
        return new TbGoogleMapLayer(ctx, inputSettings);
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

  public loadLayer(theMap: L.Map): Observable<L.TB.LayerData> {
    return this.generateLayer().pipe(
      switchMap((layerData) => {
        if (layerData) {
          return this.generateLayer().pipe(
            map((miniLayerData) => {
              if (miniLayerData) {
                const attributionPrefix = layerData.attribution ? theMap.attributionControl.options.prefix as string : null;
                return {
                  title: this.title(),
                  attributionPrefix: attributionPrefix,
                  layer: layerData.layer,
                  mini: miniLayerData.layer
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

  private generateLayer(): Observable<TbMapLayerData> {
    return this.createLayer().pipe(
      switchMap((baseLayer) => {
        if (baseLayer) {
          if (this.settings.referenceLayer) {
            return this.loadReferenceLayer(this.settings.referenceLayer).pipe(
              map((referenceLayer) => {
                  if (referenceLayer) {
                    const layer = L.featureGroup();
                    let baseLayerLoaded = false;
                    let referenceLayerLoaded = false;
                    baseLayer.addTo(layer);
                    referenceLayer.addTo(layer);
                    baseLayer.once('load', () => {
                      baseLayerLoaded = true;
                      if (referenceLayerLoaded) {
                        layer.fire('load');
                      }
                    });
                    referenceLayer.once('load', () => {
                      referenceLayerLoaded = true;
                      if (baseLayerLoaded) {
                        layer.fire('load');
                      }
                    });
                    return {
                      layer,
                      attribution: !!baseLayer.getAttribution() || !!referenceLayer.getAttribution()
                    };
                  } else {
                    return {
                      layer: baseLayer,
                      attribution: !!baseLayer.getAttribution()
                    };
                  }
              }));
          } else {
            return of({
              layer: baseLayer,
              attribution: !!baseLayer.getAttribution()
            });
          }
        } else {
          return of(null);
        }
      }
    ));
  }

  private loadReferenceLayer(referenceLayer: ReferenceLayerType): Observable<L.Layer> {
    let spec$ = referenceLayerCache.get(referenceLayer);
    if (!spec$) {
      const styleUrl = referenceLayerStyleUrlMap.get(referenceLayer);
      spec$ = this.ctx.http.get<StyleSpecification>(styleUrl).pipe(
        shareReplay({
          bufferSize: 1,
          refCount: true
        })
      );
      referenceLayerCache.set(referenceLayer, spec$);
    }
    return spec$.pipe(
      map(spec => {
        return L.TB.MapLibreGL.mapLibreGLLayer({
          style: spec,
        });
      })
    );
  }

  private title(): string {
    const customTranslate = this.ctx.$injector.get(CustomTranslatePipe);
    if (this.settings.label) {
      return customTranslate.transform(this.settings.label);
    } else {
      return this.generateTitle();
    }
  }

  private generateTitle(): string {
    const translationKey = defaultLayerTitle(this.settings);
    if (translationKey) {
      return this.ctx.translate.instant(translationKey);
    } else {
      return 'Unknown';
    }
  };

  protected abstract defaultSettings(): S;

  protected abstract createLayer(): Observable<L.Layer>;

}

class TbOpenStreetMapLayer extends TbMapLayer<OpenStreetMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): OpenStreetMapLayerSettings {
    return defaultOpenStreetMapLayerSettings;
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.tileLayer.provider(this.settings.layerType);
    return of(layer);
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
    const apiKey = this.settings.apiKey || defaultGoogleMapLayerSettings.apiKey;
    if (TbGoogleMapLayer.loadedApiKeysGlobal[apiKey]) {
      return of(true);
    } else {
      const resourceService = this.ctx.$injector.get(ResourcesService);
      return resourceService.loadResource(`https://maps.googleapis.com/maps/api/js?key=${apiKey}&loading=async`).pipe(
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

class TbTencentMapLayer extends TbMapLayer<TencentMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): TencentMapLayerSettings {
    return defaultTencentMapLayerSettings;
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.TB.tileLayer.chinaProvider(this.settings.layerType, {
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

  protected createLayer(): Observable<L.Layer> {
    const apiKey = this.settings.apiKey || defaultHereMapLayerSettings.apiKey;
    const layer = L.tileLayer.provider(this.settings.layerType, {useV3: true, apiKey} as any);
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

  protected createLayer(): Observable<L.Layer> {
    const layer = L.tileLayer(this.settings.tileUrl);
    return of(layer);
  }

}
