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
  DataKeyValuePair,
  MapActionHandler,
  MapType,
  mergeMapDatasources,
  parseCenterPosition,
  TbCircleData,
  TbMapDatasource, TbPolygonCoordinates, TbPolygonRawCoordinates
} from '@home/components/widget/lib/maps/models/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { formattedDataFormDatasourceData, isDefinedAndNotNull, mergeDeepIgnoreArray } from '@core/utils';
import { DeepPartial } from '@shared/models/common';
import L from 'leaflet';
import { forkJoin, Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import '@home/components/widget/lib/maps/leaflet/leaflet-tb';
import { MapDataLayerType, TbMapDataLayer, } from '@home/components/widget/lib/maps/data-layer/map-data-layer';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { FormattedData, WidgetActionDescriptor, widgetType } from '@shared/models/widget.models';
import { EntityDataPageLink } from '@shared/models/query/query.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { TbMarkersDataLayer } from '@home/components/widget/lib/maps/data-layer/markers-data-layer';
import { TbPolygonsDataLayer } from '@home/components/widget/lib/maps/data-layer/polygons-data-layer';
import { TbCirclesDataLayer } from '@home/components/widget/lib/maps/data-layer/circles-data-layer';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeData, AttributeScope, DataKeyType, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;

export abstract class TbMap<S extends BaseMapSettings> {

  protected settings: S;
  protected map: L.Map;

  protected defaultCenterPosition: [number, number];
  protected ignoreUpdateBounds = false;

  protected southWest = new L.LatLng(-L.Projection.SphericalMercator['MAX_LATITUDE'], -180);
  protected northEast = new L.LatLng(L.Projection.SphericalMercator['MAX_LATITUDE'], 180);

  protected dataLayers: TbMapDataLayer<any,any>[];
  protected dsData: FormattedData<TbMapDatasource>[];

  protected mapElement: HTMLElement;

  protected sidebar: L.TB.SidebarControl;

  private readonly mapResize$: ResizeObserver;

  private readonly tooltipActions: { [name: string]: MapActionHandler };
  private readonly markerClickActions: { [name: string]: MapActionHandler };
  private readonly polygonClickActions: { [name: string]: MapActionHandler };
  private readonly circleClickActions: { [name: string]: MapActionHandler };

  private tooltipInstances: ITooltipsterInstance[] = [];

  protected constructor(protected ctx: WidgetContext,
                        protected inputSettings: DeepPartial<S>,
                        protected containerElement: HTMLElement) {
    this.settings = mergeDeepIgnoreArray({} as S, this.defaultSettings(), this.inputSettings as S);

    this.tooltipActions = this.loadActions('tooltipAction');
    this.markerClickActions = this.loadActions('markerClick');
    this.polygonClickActions = this.loadActions('polygonClick');
    this.circleClickActions = this.loadActions('circleClick');

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
    if (this.map.zoomControl) {
      this.map.zoomControl.setPosition(this.settings.controlsPosition);
    }
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
    this.setupEditMode();
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

  private setupEditMode() {
    const dragEnabled = this.dataLayers.some(dl => dl.isDragEnabled());
    if (dragEnabled) {
      //this.map.pm.enableGlobalDragMode();
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
            functionBefore: (_instance, helper) => {
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
    this.dsData = formattedDataFormDatasourceData<TbMapDatasource>(subscription.data,
      undefined, undefined, el => el.datasource.entityId + el.datasource.mapDataIds[0]);
    this.dataLayers.forEach(dl => dl.updateData(this.dsData));
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

  private loadActions(name: string): { [name: string]: MapActionHandler } {
    const descriptors = this.ctx.actionsApi.getActionDescriptors(name);
    const actions: { [name: string]: MapActionHandler } = {};
    descriptors.forEach(descriptor => {
      actions[descriptor.name] = ($event: Event, datasource: TbMapDatasource) => this.onCustomAction(descriptor, $event, datasource);
    });
    return actions;
  }

  private onCustomAction(descriptor: WidgetActionDescriptor, $event: Event, entityInfo: TbMapDatasource) {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    const { entityId, entityName, entityLabel, entityType } = entityInfo;
    this.ctx.actionsApi.handleWidgetAction($event, descriptor, {
      entityType,
      id: entityId
    }, entityName, null, entityLabel);
  }

  protected abstract defaultSettings(): S;

  protected abstract createMap(): Observable<L.Map>;

  protected abstract onResize(): void;

  protected abstract fitBounds(bounds: L.LatLngBounds): void;

  protected doSetupControls(): Observable<any> {
    return of(null);
  }

  protected invalidateDataLayersCoordinates(): void {
    this.dataLayers.forEach(dl => dl.invalidateCoordinates());
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

  public getData(): FormattedData<TbMapDatasource>[] {
    return this.dsData;
  }

  public getMap(): L.Map {
    return this.map;
  }

  public type(): MapType {
    return this.settings.mapType;
  }

  public tooltipElementClick(element: HTMLElement, action: string, datasource: TbMapDatasource): void {
    if (element && this.tooltipActions[action]) {
      element.onclick = ($event) =>
      {
        this.tooltipActions[action]($event, datasource);
        return false;
      };
    }
  }

  public markerClick(marker: L.Layer, datasource: TbMapDatasource): void {
    if (Object.keys(this.markerClickActions).length) {
      marker.on('click', (event: L.LeafletMouseEvent) => {
        for (const action in this.markerClickActions) {
          this.markerClickActions[action](event.originalEvent, datasource);
        }
      });
    }
  }

  public polygonClick(polygon: L.Layer, datasource: TbMapDatasource): void {
    if (Object.keys(this.polygonClickActions).length) {
      polygon.on('click', (event: L.LeafletMouseEvent) => {
        for (const action in this.polygonClickActions) {
          this.polygonClickActions[action](event.originalEvent, datasource);
        }
      });
    }
  }

  public circleClick(circle: L.Layer, datasource: TbMapDatasource): void {
    if (Object.keys(this.circleClickActions).length) {
      circle.on('click', (event: L.LeafletMouseEvent) => {
        for (const action in this.circleClickActions) {
          this.circleClickActions[action](event.originalEvent, datasource);
        }
      });
    }
  }

  public saveItemData(datasource: TbMapDatasource, data: DataKeyValuePair[]): Observable<any> {
    const attributeService = this.ctx.$injector.get(AttributeService);
    const attributes: AttributeData[] = [];
    const timeseries: AttributeData[] = [];
    const entityId: EntityId = {
      entityType: datasource.entityType,
      id: datasource.entityId
    };
    data.forEach(pair => {
      const key = pair.dataKey;
      if (key.type === DataKeyType.attribute) {
        attributes.push({
          key: key.name,
          value: pair.value
        });
      } else if (key.type === DataKeyType.timeseries) {
        timeseries.push({
          key: key.name,
          value: pair.value
        });
      }
    });
    const observables: Observable<any>[] = [];
    if (timeseries.length) {
      observables.push(attributeService.saveEntityTimeseries(
        entityId,
        LatestTelemetry.LATEST_TELEMETRY,
        timeseries
      ));
    }
    if (attributes.length) {
      observables.push(attributeService.saveEntityAttributes(
        entityId,
        AttributeScope.SERVER_SCOPE,
        attributes
      ));
    }
    if (observables.length) {
      return forkJoin(observables);
    } else {
      return of(null);
    }
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

  public abstract locationDataToLatLng(position: {x: number; y: number}): L.LatLng;

  public abstract latLngToLocationData(position: L.LatLng): {x: number; y: number};

  public abstract polygonDataToCoordinates(coordinates: TbPolygonRawCoordinates): TbPolygonRawCoordinates;

  public abstract coordinatesToPolygonData(coordinates: TbPolygonCoordinates): TbPolygonRawCoordinates;

  public abstract circleDataToCoordinates(circle: TbCircleData): TbCircleData;

  public abstract coordinatesToCircleData(center: L.LatLng, radius: number): TbCircleData;



}
