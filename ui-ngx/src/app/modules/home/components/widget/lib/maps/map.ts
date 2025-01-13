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
  latLngPointToBounds,
  MapSetting,
  MapType,
  MapZoomAction,
  mergeMapDatasources,
  parseCenterPosition,
  TbCircleData,
  TbMapDatasource
} from '@home/components/widget/lib/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { formattedDataFormDatasourceData, isDefinedAndNotNull, mergeDeepIgnoreArray } from '@core/utils';
import { DeepPartial } from '@shared/models/common';
import L, { LatLngBounds, LatLngTuple, PointExpression, Projection } from 'leaflet';
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
import { widgetType } from '@shared/models/widget.models';
import { EntityDataPageLink } from '@shared/models/query/query.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;

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
  protected ignoreUpdateBounds = false;

  protected southWest = new L.LatLng(-Projection.SphericalMercator['MAX_LATITUDE'], -180);
  protected northEast = new L.LatLng(Projection.SphericalMercator['MAX_LATITUDE'], 180);

  protected dataLayers: TbMapDataLayer<any,any>[];

  protected mapElement: HTMLElement;

  protected sidebar: L.TB.SidebarControl;

  private readonly mapResize$: ResizeObserver;

  private tooltipInstances: ITooltipsterInstance[] = [];

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
    }
    this.setupDataLayers();
    this.createdControlButtonTooltip();
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
      const groupsMap = new Map<string, L.TB.GroupData>();
      const customTranslate = this.ctx.$injector.get(CustomTranslatePipe);
      this.dataLayers.forEach(dl => {
        dl.getGroups().forEach(group => {
          let groupData = groupsMap.get(group);
          if (!groupData) {
            groupData = {
              title: customTranslate.transform(group),
              group,
              enabled: true,
              dataLayers: []
            };
            groupsMap.set(group, groupData);
          }
          groupData.dataLayers.push(dl);
        });
      });

      const groupDataLayers = Array.from(groupsMap.values());
      if (groupDataLayers.length) {
        const sidebar = this.getSidebar();
        L.TB.groups({
          groups: groupDataLayers,
          sidebar,
          position: this.settings.controlsPosition,
          uiClass: 'tb-groups',
          paneTitle: this.ctx.translate.instant('widgets.maps.data-layer.groups'),
          buttonTitle: this.ctx.translate.instant('widgets.maps.data-layer.groups'),
        }).addTo(this.map);
        this.map.on('layergroupchange', () => {
          this.updateBounds();
        });
      }

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

  private createdControlButtonTooltip() {
    import('tooltipster').then(() => {
      if ($.tooltipster) {
        this.tooltipInstances.forEach((instance) => {
          instance.destroy();
        });
        this.tooltipInstances = [];
      }
      $(this.mapElement)
      .find('a[role="button"]:not(.leaflet-pm-action)')
      .each((_index, element) => {
        let title: string;
        if (element.title) {
          title = element.title;
          $(element).removeAttr('title');
        } else if (element.parentElement.title) {
          title = element.parentElement.title;
          $(element).parent().removeAttr('title');
        }
        const tooltip =  $(element).tooltipster(
          {
            content: title,
            theme: 'tooltipster-shadow',
            delay: 10,
            triggerClose: {
              click: true,
              tap: true,
              scroll: true,
              mouseleave: true
            },
            side: ['topleft', 'bottomleft'].includes(this.settings.controlsPosition) ? 'right' : 'left',
            distance: 2,
            trackOrigin: true,
            functionBefore: (instance, helper) => {
              if (helper.origin.ariaDisabled === 'true' || helper.origin.parentElement.classList.contains('active')) {
                return false;
              }
            },
          }
        );
        this.tooltipInstances.push(tooltip.tooltipster('instance'));
      });
    });
  }

  private update(subscription: IWidgetSubscription) {
    const dsData = formattedDataFormDatasourceData<TbMapDatasource>(subscription.data,
      undefined, undefined, el => el.datasource.entityId + el.datasource.mapDataIds[0]);
    this.dataLayers.forEach(dl => dl.updateData(dsData));
    this.updateBounds();
  }

  private resize() {
    this.onResize();
    this.map?.invalidateSize();
  }

  private updateBounds() {
    const enabledDataLayers = this.dataLayers.filter(dl => dl.isEnabled());
    const dataLayersBounds = enabledDataLayers.map(dl => dl.getBounds()).filter(b => b.isValid());
    let bounds: L.LatLngBounds;
    if (dataLayersBounds.length) {
      bounds = new L.LatLngBounds(null, null);
      dataLayersBounds.forEach(b => bounds.extend(b));

      const mapBounds = this.map.getBounds();
      if (bounds.isValid() && this.settings.fitMapBounds && !mapBounds.contains(bounds)) {
        if (!this.ignoreUpdateBounds) {
          this.fitBounds(bounds);
        }
      }

    }
  }

  private fitBounds(bounds: LatLngBounds) {
    if (bounds.isValid()) {
      if (!this.settings.fitMapBounds && this.settings.defaultZoomLevel) {
        this.map.setZoom(this.settings.defaultZoomLevel, { animate: false });
        if (this.settings.useDefaultCenterPosition) {
          this.map.panTo(this.defaultCenterPosition, { animate: false });
        }
        else {
          this.map.panTo(bounds.getCenter());
        }
      } else {
        this.map.once('zoomend', () => {
          let minZoom = this.settings.minZoomLevel;
          if (this.settings.defaultZoomLevel) {
            minZoom = Math.max(minZoom, this.settings.defaultZoomLevel);
          }
          if (this.map.getZoom() > minZoom) {
            this.map.setZoom(minZoom, { animate: false });
          }
        });
        if (this.settings.useDefaultCenterPosition) {
          bounds = bounds.extend(this.defaultCenterPosition);
        }
        this.map.fitBounds(bounds, { padding: [10, 10], animate: false });
        this.map.invalidateSize();
      }
    }
  }

  protected abstract defaultSettings(): S;

  protected abstract createMap(): Observable<L.Map>;

  protected abstract onResize(): void;

  protected doSetupControls(): Observable<any> {
    return of(null);
  }

  protected getSidebar(): L.TB.SidebarControl {
    if (!this.sidebar) {
      this.sidebar = L.TB.sidebar({
        container: $(this.containerElement),
        position: this.settings.controlsPosition,
        paneWidth: 220
      }).addTo(this.map);
    }
    return this.sidebar;
  }

  public getCtx(): WidgetContext {
    return this.ctx;
  }

  public getMap(): L.Map {
    return this.map;
  }

  public type(): MapType {
    return this.settings.mapType;
  }

  public destroy() {
    if (this.mapResize$) {
      this.mapResize$.disconnect();
    }
    if (this.map) {
      this.map.remove();
    }
    this.tooltipInstances.forEach((instance) => {
      instance.destroy();
    });
  }

  public abstract positionToLatLng(position: {x: number; y: number}): L.LatLng;

  public abstract toPolygonCoordinates(expression: (LatLngTuple | LatLngTuple[] | LatLngTuple[][])[]): any;

  public abstract convertCircleData(circle: TbCircleData): TbCircleData;

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
            const sidebar = this.getSidebar();
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

  public positionToLatLng(position: {x: number; y: number}): L.LatLng {
    return L.latLng(position.x, position.y) as L.LatLng;
  }

  public toPolygonCoordinates(expression: (LatLngTuple | LatLngTuple[] | LatLngTuple[][])[]): any {
    return (expression).map((el) => {
      if (!Array.isArray(el[0]) && el.length === 2) {
        return el;
      } else if (Array.isArray(el) && el.length) {
        return this.toPolygonCoordinates(el as LatLngTuple[] | LatLngTuple[][]);
      } else {
        return null;
      }
    }).filter(el => !!el);
  }

  public convertCircleData(circle: TbCircleData): TbCircleData {
    const centerPoint = latLngPointToBounds(new L.LatLng(circle.latitude, circle.longitude), this.southWest, this.northEast);
    circle.latitude = centerPoint.lat;
    circle.longitude = centerPoint.lng;
    return circle;
  }

}

class TbImageMap extends TbMap<ImageMapSettings> {

  private maxZoom = 4;

  private width = 0;
  private height = 0;

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

  public positionToLatLng(position: {x: number; y: number}): L.LatLng {
    return this.pointToLatLng(
      position.x * this.width,
      position.y * this.height);
  }

  public pointToLatLng(x: number, y: number): L.LatLng {
    return L.CRS.Simple.pointToLatLng({ x, y } as L.PointExpression, this.maxZoom - 1);
  }

  public toPolygonCoordinates(expression: (LatLngTuple | LatLngTuple[] | LatLngTuple[][])[]): any {
    return (expression).map((el) => {
      if (!Array.isArray(el[0]) && !Array.isArray(el[1]) && el.length === 2) {
        return this.pointToLatLng(
          el[0] * this.width,
          el[1] * this.height
        );
      } else if (Array.isArray(el) && el.length) {
        return this.toPolygonCoordinates(el as LatLngTuple[] | LatLngTuple[][]);
      } else {
        return null;
      }
    }).filter(el => !!el);
  }

  public convertCircleData(circle: TbCircleData): TbCircleData {
    const centerPoint = this.pointToLatLng(circle.latitude * this.width, circle.longitude * this.height);
    circle.latitude = centerPoint.lat;
    circle.longitude = centerPoint.lng;
    circle.radius = circle.radius * this.width;
    return circle;
  }

}
