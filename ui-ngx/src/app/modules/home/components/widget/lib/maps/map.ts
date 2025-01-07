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
  additionalMapDataSourcesToDatasources,
  BaseMapSettings,
  DEFAULT_ZOOM_LEVEL,
  defaultGeoMapSettings,
  defaultImageMapSettings,
  GeoMapSettings,
  ImageMapSettings,
  MapSetting,
  MapType,
  MapZoomAction, mergeMapDatasources,
  parseCenterPosition, TbMapDatasource
} from '@home/components/widget/lib/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { formattedDataFormDatasourceData, isDefinedAndNotNull, mergeDeepIgnoreArray } from '@core/utils';
import { DeepPartial } from '@shared/models/common';
import L from 'leaflet';
import { forkJoin, Observable, of } from 'rxjs';
import { TbMapLayer } from '@home/components/widget/lib/maps/map-layer';
import { map, switchMap, tap } from 'rxjs/operators';
import '@home/components/widget/lib/maps/leaflet/leaflet-tb';
import {
  MapDataLayerType,
  TbCirclesDataLayer,
  TbMapDataLayer,
  TbMarkersDataLayer,
  TbPolygonsDataLayer
} from '@home/components/widget/lib/maps/map-data-layer';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { FormattedData, widgetType } from '@shared/models/widget.models';
import { EntityDataPageLink } from '@shared/models/query/query.models';

export abstract class TbMap<S extends BaseMapSettings> {

  static fromSettings(ctx: WidgetContext,
                      inputSettings: DeepPartial<MapSetting>,
                      mapElement: HTMLElement): TbMap<MapSetting> {
    switch (inputSettings.mapType) {
      case MapType.geoMap:
        return new TbGeoMap(ctx, inputSettings, mapElement);
      case MapType.image:
        return new TbImageMap(ctx, inputSettings, mapElement);
    }
  }

  protected settings: S;
  protected map: L.Map;

  protected defaultCenterPosition: [number, number];
  protected bounds: L.LatLngBounds;

  protected dataLayers: TbMapDataLayer<any>[];

  protected mapElement: HTMLElement;

  private readonly mapResize$: ResizeObserver;

  protected constructor(protected ctx: WidgetContext,
                        protected inputSettings: DeepPartial<S>,
                        protected containerElement: HTMLElement) {
    this.settings = mergeDeepIgnoreArray({} as S, this.defaultSettings(), this.inputSettings as S);
    $(containerElement).empty();
    $(containerElement).addClass('tb-map-layout');
    const mapElement = $('<div class="tb-map"></div>');
    $(containerElement).append(mapElement);

    this.mapResize$ = new ResizeObserver(() => {
      this.resize();
    });
    this.mapResize$.observe(this.containerElement);

    this.mapElement = mapElement[0];

    this.defaultCenterPosition = parseCenterPosition(this.settings.defaultCenterPosition);

    this.createMap().pipe(
      switchMap((map) => {
        this.map = map;
        return this.setupControls();
      })
    ).subscribe(() => {
      this.initMap();
    });
  }

  private setupControls(): Observable<any> {
    this.map.zoomControl.setPosition(this.settings.controlsPosition);
    return this.doSetupControls();
  }

  private initMap() {

    this.map.on('move', () => {
      this.ctx.updatePopoverPositions();
    });
    this.map.on('zoomstart', () => {
      this.ctx.setPopoversHidden(true);
    });
    this.map.on('zoomend', () => {
      this.ctx.setPopoversHidden(false);
      this.ctx.updatePopoverPositions();
      setTimeout(() => {
        this.ctx.updatePopoverPositions();
      });
    });
    if (this.settings.useDefaultCenterPosition) {
      this.map.panTo(this.defaultCenterPosition);
      this.bounds = this.map.getBounds();
    } else {
      this.bounds = new L.LatLngBounds(null, null);
    }
    this.setupDataLayers();
  }

  private setupDataLayers() {
    this.dataLayers = [];
    if (this.settings.markers) {
      this.dataLayers.push(...this.settings.markers.map(settings => new TbMarkersDataLayer(this, settings)));
    }
    if (this.settings.polygons) {
      this.dataLayers.push(...this.settings.polygons.map(settings => new TbPolygonsDataLayer(this, settings)));
    }
    if (this.settings.circles) {
      this.dataLayers.push(...this.settings.circles.map(settings => new TbCirclesDataLayer(this, settings)));
    }
    if (this.dataLayers.length) {
      const setup = this.dataLayers.map(dl => dl.setup());
      forkJoin(setup).subscribe(
        () => {
          let datasources: TbMapDatasource[];
          for (const layerType of (Object.keys(MapDataLayerType) as MapDataLayerType[])) {
            const typeDatasources = this.dataLayers.filter(dl => dl.dataLayerType() === layerType).map(dl => dl.getDatasource());
            if (!datasources) {
              datasources = typeDatasources;
            } else {
              datasources = mergeMapDatasources(datasources, typeDatasources);
            }
          }
          const additionalDatasources = additionalMapDataSourcesToDatasources(this.settings.additionalDataSources);
          datasources = mergeMapDatasources(datasources, additionalDatasources);
          const dataLayersSubscriptionOptions: WidgetSubscriptionOptions = {
            datasources,
            hasDataPageLink: true,
            useDashboardTimewindow: false,
            type: widgetType.latest,
            callbacks: {
              onDataUpdated: (subscription) => {
                this.update(subscription);
              }
            }
          };
          this.ctx.subscriptionApi.createSubscription(dataLayersSubscriptionOptions, false).subscribe(
            (dataLayersSubscription) => {
              let pageSize = this.settings.mapPageSize;
              if (isDefinedAndNotNull(this.ctx.widgetConfig.pageSize)) {
                pageSize = Math.max(pageSize, this.ctx.widgetConfig.pageSize);
              }
              const pageLink: EntityDataPageLink = {
                page: 0,
                pageSize,
                textSearch: null,
                dynamic: true
              };
              dataLayersSubscription.paginatedDataSubscriptionUpdated.subscribe(() => {
                // this.map.resetState();
              });
              dataLayersSubscription.subscribeAllForPaginatedData(pageLink, null);
            }
          );
        }
      );
    }
  }

  private update(subscription: IWidgetSubscription) {
    const dsData = formattedDataFormDatasourceData<TbMapDatasource>(subscription.data,
      undefined, undefined, el => el.datasource.entityId + el.datasource.mapDataIds[0]);
    this.dataLayers.forEach(dl => dl.updateData(dsData));
  }

  private resize() {
    this.onResize();
    this.map?.invalidateSize();
  }

  protected abstract defaultSettings(): S;

  protected abstract createMap(): Observable<L.Map>;

  protected abstract onResize(): void;

  protected doSetupControls(): Observable<any> {
    return of(null);
  }

  public destroy() {
    if (this.mapResize$) {
      this.mapResize$.disconnect();
    }
    if (this.map) {
      this.map.remove();
    }
  }

}

class TbGeoMap extends TbMap<GeoMapSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<GeoMapSettings>,
              protected containerElement: HTMLElement) {
    super(ctx, inputSettings, containerElement);
  }

  protected defaultSettings(): GeoMapSettings {
    return defaultGeoMapSettings;
  }

  protected createMap(): Observable<L.Map> {
    const map = L.map(this.mapElement, {
      scrollWheelZoom: this.settings.zoomActions.includes(MapZoomAction.scroll),
      doubleClickZoom: this.settings.zoomActions.includes(MapZoomAction.doubleClick),
      zoomControl: this.settings.zoomActions.includes(MapZoomAction.controlButtons)
    }).setView(this.defaultCenterPosition, this.settings.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
    return of(map);
  }

  protected onResize(): void {}

  protected doSetupControls(): Observable<any> {
    return this.loadLayers().pipe(
      tap((layers: L.TB.LayerData[]) => {
        if (layers.length) {
          const layer = layers[0];
          layer.layer.addTo(this.map);
          this.map.attributionControl.setPrefix(layer.attributionPrefix);
          if (layers.length > 1) {
            const sidebar = L.TB.sidebar({
              container: $(this.containerElement),
              position: this.settings.controlsPosition,
              paneWidth: 220
            }).addTo(this.map);
            L.TB.layers({
              layers,
              sidebar,
              position: this.settings.controlsPosition,
              uiClass: 'tb-layers',
              paneTitle: this.ctx.translate.instant('widgets.maps.layer.map-layers'),
              buttonTitle: this.ctx.translate.instant('widgets.maps.layer.layers'),
            }).addTo(this.map);
          }
        }
      })
    );

  }

  private loadLayers(): Observable<L.TB.LayerData[]> {
    const layers = this.settings.layers.map(settings => TbMapLayer.fromSettings(this.ctx, settings));
    return forkJoin(layers.map(layer => layer.loadLayer(this.map))).pipe(
      map((layersData) => {
        return layersData.filter(l => l !== null);
      })
    );
  }


}

class TbImageMap extends TbMap<ImageMapSettings> {

  private maxZoom = 4;

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<ImageMapSettings>,
              protected mapElement: HTMLElement) {
    super(ctx, inputSettings, mapElement);
  }

  protected defaultSettings(): ImageMapSettings {
    return defaultImageMapSettings;
  }

  protected createMap(): Observable<L.Map> {
    const map = L.map(this.mapElement, {
      scrollWheelZoom: this.settings.zoomActions.includes(MapZoomAction.scroll),
      doubleClickZoom: this.settings.zoomActions.includes(MapZoomAction.doubleClick),
      zoomControl: this.settings.zoomActions.includes(MapZoomAction.controlButtons),
      minZoom: 1,
      maxZoom: this.maxZoom,
      zoom: 1,
      crs: L.CRS.Simple,
      attributionControl: false
    }).setView(this.defaultCenterPosition, this.settings.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
    return of(map);
  }

  protected onResize(): void {}
}
