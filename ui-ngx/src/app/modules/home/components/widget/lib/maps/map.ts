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
  additionalMapDataSourcesToDatasources,
  BaseMapSettings,
  DataKeyValuePair,
  MapBooleanFunction,
  mapDataLayerTypes,
  MapScale,
  MapType,
  mergeMapDatasources,
  parseCenterPosition,
  TbCircleData,
  TbMapDatasource,
  TbPolygonCoordinates,
  TbPolygonRawCoordinates,
  TbPolylineCoordinates,
  TbPolylineRawCoordinates
} from '@shared/models/widget/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import {
  formattedDataArrayFromDatasourceData,
  formattedDataFormDatasourceData,
  isDefined,
  isDefinedAndNotNull,
  isUndefined,
  mergeDeepIgnoreArray,
  parseTbFunction
} from '@core/utils';
import { DeepPartial } from '@shared/models/common';
import L from 'leaflet';
import { EMPTY, forkJoin, Observable, of } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import '@home/components/widget/lib/maps/leaflet/leaflet-tb';
import {
  TbLatestDataLayerItem,
  TbLatestMapDataLayer,
  UnplacedMapDataItem,
} from '@home/components/widget/lib/maps/data-layer/latest-map-data-layer';
import { IWidgetSubscription, PlaceMapItemActionData, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import {
  FormattedData,
  mapItemTooltipsTranslation,
  MapItemType,
  WidgetAction,
  WidgetActionType,
  widgetType
} from '@shared/models/widget.models';
import { EntityDataPageLink } from '@shared/models/query/query.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { TbMarkersDataLayer } from '@home/components/widget/lib/maps/data-layer/markers-data-layer';
import { TbPolygonsDataLayer } from '@home/components/widget/lib/maps/data-layer/polygons-data-layer';
import { TbCirclesDataLayer } from '@home/components/widget/lib/maps/data-layer/circles-data-layer';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeData, AttributeScope, DataKeyType, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  SelectMapEntityPanelComponent
} from '@home/components/widget/lib/maps/panels/select-map-entity-panel.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { createPlaceItemIcon } from '@shared/models/widget/maps/marker-shape.models';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { MapTimelinePanelComponent } from '@home/components/widget/lib/maps/panels/map-timeline-panel.component';
import { ComponentRef } from '@angular/core';
import { TbTripsDataLayer } from '@home/components/widget/lib/maps/data-layer/trips-data-layer';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { TbMapDataLayer } from '@home/components/widget/lib/maps/data-layer/map-data-layer';
import { EntityType } from '@shared/models/entity-type.models';
import { ShapePatternStorage } from '@home/components/widget/lib/maps/data-layer/shapes-data-layer';
import { TbPolylineDataLayer } from '@home/components/widget/lib/maps/data-layer/polylines-data-layer';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;
import TooltipPositioningSide = JQueryTooltipster.TooltipPositioningSide;

type TooltipInstancesData = { root: HTMLElement, instances: ITooltipsterInstance[] };

export abstract class TbMap<S extends BaseMapSettings> {

  protected settings: S;
  protected map: L.Map;

  protected defaultCenterPosition: [number, number];
  protected ignoreUpdateBounds = false;
  protected bounds: L.LatLngBounds;

  protected southWest = new L.LatLng(-L.Projection.SphericalMercator['MAX_LATITUDE'], -180);
  protected northEast = new L.LatLng(L.Projection.SphericalMercator['MAX_LATITUDE'], 180);

  protected dataLayers: TbMapDataLayer[];
  protected latestDataLayers: TbLatestMapDataLayer[];
  protected tripDataLayers: TbTripsDataLayer[];
  protected dsData: FormattedData<TbMapDatasource>[] = [];

  protected timeline = false;
  protected minTime: number;
  protected maxTime: number;
  protected timeStep: number;
  protected currentTime: number;

  protected selectedDataItem: TbLatestDataLayerItem;

  protected mapLayoutElement: HTMLElement;
  protected mapElement: HTMLElement;

  protected sidebar: L.TB.SidebarControl;

  protected customActionsToolbar: L.TB.TopToolbarControl;
  protected editToolbar: L.TB.BottomToolbarControl;

  protected dragModeButton: L.TB.ToolbarButton;
  protected addMarkerButton: L.TB.ToolbarButton;
  protected addRectangleButton: L.TB.ToolbarButton;
  protected addPolygonButton: L.TB.ToolbarButton;
  protected addCircleButton: L.TB.ToolbarButton;
  protected addPolylineButton: L.TB.ToolbarButton;

  protected timeLineComponentRef: ComponentRef<MapTimelinePanelComponent>;
  protected timeLineComponent: MapTimelinePanelComponent;
  protected locationSnapFilterFunction: CompiledTbFunction<MapBooleanFunction>;

  protected addMarkerDataLayers: TbLatestMapDataLayer<any>[];
  protected addPolygonDataLayers: TbLatestMapDataLayer<any>[];
  protected addCircleDataLayers: TbLatestMapDataLayer<any>[];
  protected addPolylineDataLayers: TbLatestMapDataLayer<any>[];

  protected shapePatternStorage: ShapePatternStorage = {};

  private readonly mapResize$: ResizeObserver;

  private tooltipInstances: TooltipInstancesData[] = [];

  private currentPopover: TbPopoverComponent;
  private currentEditButton: L.TB.ToolbarButton;

  private dragMode = true;
  private createMapItemActionId: string;

  private get isPlacingItem(): boolean {
    return !!this.currentEditButton;
  }

  protected constructor(protected ctx: WidgetContext,
                        protected inputSettings: DeepPartial<S>,
                        protected containerElement: HTMLElement) {
    this.ctx.actionsApi.placeMapItem = this.placeMapItem.bind(this);
    (this.ctx as any).mapInstance = this;
    this.settings = mergeDeepIgnoreArray({} as S, this.defaultSettings(), this.inputSettings as S);

    $(containerElement).empty();
    $(containerElement).addClass('tb-map-container');
    const mapLayoutElement = $('<div class="tb-map-layout"></div>');
    this.mapLayoutElement = mapLayoutElement[0];
    $(containerElement).append(mapLayoutElement);

    const mapElement = $('<div class="tb-map"></div>');
    mapLayoutElement.append(mapElement);

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
    if (this.settings.scales?.length) {
      L.control.scale({
        position: 'bottomright',
        metric: this.settings.scales.includes(MapScale.metric),
        imperial: this.settings.scales.includes(MapScale.imperial)
      }).addTo(this.map);
    }
    if (this.map.zoomControl) {
      this.map.zoomControl.setPosition(this.settings.controlsPosition);
    }
    this.dragMode = !this.settings.dragModeButton;
    const tripsWithMarkers = this.settings.trips?.length ? this.settings.trips.filter(trip => trip.showMarker !== false) : [];
    const showTimeline = this.settings.tripTimeline?.showTimelineControl && tripsWithMarkers.length;

    if (showTimeline) {
      this.timeline = true;
      this.timeStep = this.settings.tripTimeline.timeStep;
      this.timeLineComponentRef = this.ctx.widgetContentContainer.createComponent(MapTimelinePanelComponent);
      this.timeLineComponent = this.timeLineComponentRef.instance;
      this.timeLineComponent.settings = this.settings.tripTimeline;
      this.timeLineComponent.timeChanged.subscribe((time) => {
        this.currentTime = time;
        this.updateTripsTime();
      });
      const parentElement = this.timeLineComponentRef.instance.element.nativeElement;
      const content = parentElement.firstChild;
      parentElement.removeChild(content);
      parentElement.style.display = 'none';
      this.containerElement.append(content);
      this.timeLineComponent.panelElement = content as Element;
    }
    const setup = [this.doSetupControls()];
    if (this.timeline && this.settings.tripTimeline.snapToRealLocation) {
      setup.push(parseTbFunction<MapBooleanFunction>(this.getCtx().http, this.settings.tripTimeline.locationSnapFilter, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.locationSnapFilterFunction = parsed;
          return null;
        })
      ));
    }
    return forkJoin(setup);
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
    this.setupEditMode();
    this.setupCustomActions();
    this.createdControlButtonTooltip(this.mapElement, ['topleft', 'bottomleft'].includes(this.settings.controlsPosition) ? 'right' : 'left');
  }

  private setupDataLayers() {
    this.dataLayers = [];
    this.latestDataLayers = [];
    this.tripDataLayers = [];
    if (this.settings.markers) {
      const markersDataLayers = this.settings.markers.map(settings => new TbMarkersDataLayer(this, settings));
      this.dataLayers.push(...markersDataLayers);
      this.latestDataLayers.push(...markersDataLayers);
    }
    if (this.settings.polygons) {
      const polygonsDataLayers = this.settings.polygons.map(settings => new TbPolygonsDataLayer(this, settings));
      this.dataLayers.push(...polygonsDataLayers);
      this.latestDataLayers.push(...polygonsDataLayers);
    }
    if (this.settings.circles) {
      const circlesDataLayers = this.settings.circles.map(settings => new TbCirclesDataLayer(this, settings));
      this.dataLayers.push(...circlesDataLayers);
      this.latestDataLayers.push(...circlesDataLayers);
    }
    if (this.settings.polylines) {
      const polylinesDataLayers = this.settings.polylines.map(settings => new TbPolylineDataLayer(this, settings));
      this.dataLayers.push(...polylinesDataLayers);
      this.latestDataLayers.push(...polylinesDataLayers);
    }
    if (this.settings.trips) {
      const tripsDataLayers = this.settings.trips.map(settings => new TbTripsDataLayer(this, settings));
      this.dataLayers.push(...tripsDataLayers);
      this.tripDataLayers.push(...tripsDataLayers);
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
          this.updateBounds(true);
        });
      }
      const setup = this.dataLayers.map(dl => dl.setup());
      forkJoin(setup).subscribe(
        () => {
          let datasources: TbMapDatasource[];
          for (const layerType of mapDataLayerTypes) {
            const typeDatasources = this.latestDataLayers.filter(dl => dl.dataLayerType() === layerType)
              .map(dl => dl.getDataSources()).flat();
            if (!datasources) {
              datasources = typeDatasources;
            } else {
              datasources = mergeMapDatasources(datasources, typeDatasources);
            }
          }
          const additionalDatasources = additionalMapDataSourcesToDatasources(this.settings.additionalDataSources);
          datasources = mergeMapDatasources(datasources, additionalDatasources);
          if (datasources.length) {
            const dataLayersSubscriptionOptions: WidgetSubscriptionOptions = {
              datasources,
              hasDataPageLink: true,
              useDashboardTimewindow: false,
              type: widgetType.latest,
              callbacks: {
                onDataUpdated: (subscription) => {
                  try {
                    this.update(subscription);
                  } catch (e) {
                    console.error(e);
                  }
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
          if (this.tripDataLayers.length) {
            const tripDatasources = this.tripDataLayers.map(dl => dl.getDataSources()).flat();
            const tripDataLayersSubscriptionOptions: WidgetSubscriptionOptions = {
              datasources: tripDatasources,
              hasDataPageLink: true,
              ignoreDataUpdateOnIntervalTick: true,
              useDashboardTimewindow: isDefined(this.ctx.widgetConfig.useDashboardTimewindow)
                ? this.ctx.widgetConfig.useDashboardTimewindow : true,
              type: widgetType.timeseries,
              callbacks: {
                onDataUpdated: (subscription) => {
                  try {
                    this.updateTrips(subscription);
                  } catch (e) {
                    console.error(e);
                  }
                },
                onLatestDataUpdated: (subscription) => {
                  try {
                    this.updateTripsWithLatestData(subscription);
                  } catch (e) {
                    console.error(e);
                  }
                }
              }
            };
            if (!tripDataLayersSubscriptionOptions.useDashboardTimewindow) {
              tripDataLayersSubscriptionOptions.timeWindowConfig = this.ctx.widgetConfig.timewindow;
            }

            this.ctx.subscriptionApi.createSubscription(tripDataLayersSubscriptionOptions, false).subscribe(
              (tripDataLayersSubscription) => {
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
                tripDataLayersSubscription.paginatedDataSubscriptionUpdated.subscribe(() => {
                  // this.map.resetState();
                });
                tripDataLayersSubscription.subscribeAllForPaginatedData(pageLink, null);
              }
            );
          }
        }
      );
    }
  }

  private setupEditMode() {

    this.editToolbar = L.TB.bottomToolbar({
      mapElement: $(this.mapElement),
      closeTitle: this.ctx.translate.instant('action.cancel'),
      onClose: () => {
        return this.deselectItem(true);
      }
    });

    this.map.on('click', () => {
      this.deselectItem();
    });

    if (this.latestDataLayers.some(dl => dl.isEditable())) {
      this.map.pm.setGlobalOptions({snappable: false});
      this.map.pm.applyGlobalOptions();
    }

    const dragSupportedDataLayers = this.latestDataLayers.filter(dl => dl.isDragEnabled());
    const showDragModeButton = this.settings.dragModeButton && dragSupportedDataLayers.length;
    const addSupportedDataLayers = this.latestDataLayers.filter(dl => dl.isAddEnabled());

    if (showDragModeButton || addSupportedDataLayers.length) {
      const drawToolbar = L.TB.toolbar({
        position: this.settings.controlsPosition
      }).addTo(this.map);
      if (showDragModeButton) {
        this.dragModeButton = drawToolbar.toolbarButton({
          id: 'dragMode',
          title: this.ctx.translate.instant('widgets.maps.data-layer.drag-drop-mode'),
          iconClass: 'tb-drag-mode',
          click: (e, button) => {
            this.toggleDragMode(e, button);
          }
        });
      }
      this.addMarkerDataLayers = addSupportedDataLayers.filter(dl => dl.dataLayerType() === 'markers');
      if (this.addMarkerDataLayers.length) {
        this.addMarkerButton = drawToolbar.toolbarButton({
          id: 'addMarker',
          title: this.ctx.translate.instant('widgets.maps.data-layer.marker.place-marker'),
          iconClass: 'tb-place-marker',
          click: (e, button) => {
            this.placeMarker(e, button);
          }
        });
        this.addMarkerButton.setDisabled(true);
        this.setPlaceMarkerStyle();
      }
      this.addPolygonDataLayers = addSupportedDataLayers.filter(dl => dl.dataLayerType() === 'polygons');
      if (this.addPolygonDataLayers.length) {
        this.addRectangleButton = drawToolbar.toolbarButton({
          id: 'addRectangle',
          title: this.ctx.translate.instant('widgets.maps.data-layer.polygon.draw-rectangle'),
          iconClass: 'tb-draw-rectangle',
          click: (e, button) => {
            this.drawRectangle(e, button);
          }
        });
        this.addRectangleButton.setDisabled(true);
        this.addPolygonButton = drawToolbar.toolbarButton({
          id: 'addPolygon',
          title: this.ctx.translate.instant('widgets.maps.data-layer.polygon.draw-polygon'),
          iconClass: 'tb-draw-polygon',
          click: (e, button) => {
            this.drawPolygon(e, button);
          }
        });
        this.addPolygonButton.setDisabled(true);
      }
      this.addCircleDataLayers = addSupportedDataLayers.filter(dl => dl.dataLayerType() === 'circles');
      if (this.addCircleDataLayers.length) {
        this.addCircleButton = drawToolbar.toolbarButton({
          id: 'addCircle',
          title: this.ctx.translate.instant('widgets.maps.data-layer.circle.draw-circle'),
          iconClass: 'tb-draw-circle',
          click: (e, button) => {
            this.drawCircle(e, button);
          }
        });
        this.addCircleButton.setDisabled(true);
      }
      this.addPolylineDataLayers = addSupportedDataLayers.filter(dl => dl.dataLayerType() === 'polylines');
      if (this.addPolylineDataLayers.length) {
        this.addPolylineButton = drawToolbar.toolbarButton({
          id: 'addPolyline',
          title: this.ctx.translate.instant('widgets.maps.data-layer.polyline.draw-polyline'),
          iconClass: 'tb-draw-polyline',
          click: (e, button) => {
            this.drawPolyline(e, button);
          }
        });
        this.addPolylineButton.setDisabled(true);
      }
    }
  }

  private toggleDragMode(_e: MouseEvent, button: L.TB.ToolbarButton): void {
    if (this.dragMode) {
      this.disableDragMode();
    } else {
      this.dragMode = true;
      this.latestDataLayers.forEach(dl => dl.dragModeUpdated());
      this.updatePlaceItemState(button);
      this.editToolbar.open([
        {
          id: 'cancel',
          iconClass: 'tb-close',
          title: this.ctx.translate.instant('action.cancel'),
          showText: true,
          click: this.disableDragMode
        }
      ], false);
    }
  }

  private disableDragMode = () => {
    this.dragMode = false;
    this.latestDataLayers.forEach(dl => dl.dragModeUpdated());
    this.updatePlaceItemState();
    this.editToolbar.close();
  }

  private placeMarker(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addMarkerDataLayers, (entity) => this.prepareDrawMode('Marker', {
      placeMarker: this.ctx.translate.instant('widgets.maps.data-layer.marker.place-marker-hint-with-entity', {entityName: entity.entity.entityDisplayName})
    }));
  }

  private drawRectangle(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addPolygonDataLayers, (entity) => this.prepareDrawMode('Rectangle', {
      firstVertex: this.ctx.translate.instant('widgets.maps.data-layer.polygon.rectangle-place-first-point-hint-with-entity', {entityName: entity.entity.entityDisplayName}),
      finishRect: this.ctx.translate.instant('widgets.maps.data-layer.polygon.finish-rectangle-hint-with-entity', {entityName: entity.entity.entityDisplayName})
    }));
  }

  private drawPolygon(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addPolygonDataLayers, (entity) => this.prepareDrawMode('Polygon', {
      firstVertex: this.ctx.translate.instant('widgets.maps.data-layer.polygon.polygon-place-first-point-hint-with-entity', {entityName: entity.entity.entityDisplayName}),
      continueLine: this.ctx.translate.instant('widgets.maps.data-layer.polygon.continue-polygon-hint-with-entity', {entityName: entity.entity.entityDisplayName}),
      finishPoly: this.ctx.translate.instant('widgets.maps.data-layer.polygon.finish-polygon-hint-with-entity', {entityName: entity.entity.entityDisplayName})
    }));
  }

  private drawCircle(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addCircleDataLayers, (entity) => this.prepareDrawMode('Circle', {
      startCircle: this.ctx.translate.instant('widgets.maps.data-layer.circle.place-circle-center-hint-with-entity', {entityName: entity.entity.entityDisplayName}),
      finishCircle: this.ctx.translate.instant('widgets.maps.data-layer.circle.finish-circle-hint-with-entity', {entityName: entity.entity.entityDisplayName}),
    }));
  }

  private drawPolyline(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addPolylineDataLayers, (entity) => this.prepareDrawMode('Line', {
      startPolyline: this.ctx.translate.instant('widgets.maps.data-layer.polyline.polyline-place-first-point-hint-with-entity', {entityName: entity.entity.entityDisplayName}),
      finishPolyline: this.ctx.translate.instant('widgets.maps.data-layer.polyline.finish-polyline-hint-with-entity', {entityName: entity.entity.entityDisplayName}),
    }));
  }

  private placeItem(e: MouseEvent, button: L.TB.ToolbarButton, dataLayers: TbLatestMapDataLayer[],
                    prepareDrawMode: (entity: UnplacedMapDataItem) => void): void {
    if (this.isPlacingItem) {
      this.finishAdd();
      return;
    }
    this.updatePlaceItemState(button);
    const itemsMap = new Map<string, UnplacedMapDataItem>();
    const dataItemsArrays = dataLayers.filter(dl => dl.isEnabled()).map(dl => dl.getUnplacedItems());
    dataItemsArrays.forEach(dataItems => {
      dataItems.forEach(dataItem => {
        itemsMap.set(dataItem.entity.$datasource.entityId, dataItem);
      });
    });
    const items = Array.from(itemsMap.values()).sort((entity1, entity2) => {
      return entity1.entity.entityDisplayName.localeCompare(entity2.entity.entityDisplayName);
    });
    this.selectEntityToPlace(e, items).subscribe((entity) => {
      if (entity) {
        this.map.once('pm:create', (e) => {
          entity.dataLayer.placeItem(entity, e.layer);
          // @ts-ignore
          e.layer._pmTempLayer = true;
          e.layer.remove();
          this.finishAdd();
        });

        prepareDrawMode(entity);

        this.latestDataLayers.forEach(dl => dl.disableEditMode());

        this.editToolbar.open([
          {
            id: 'cancel',
            iconClass: 'tb-close',
            title: this.ctx.translate.instant('action.cancel'),
            showText: true,
            click: this.finishAdd
          }
        ], false);
      } else {
        this.updatePlaceItemState();
      }
    });
  }

  private selectEntityToPlace(e: MouseEvent, entities: UnplacedMapDataItem[]): Observable<UnplacedMapDataItem> {
    if (entities.length === 1) {
      return of(entities[0]);
    } else {
      if (e) {
        e.stopPropagation();
      }
      const trigger = (e.target || e.srcElement || e.currentTarget) as Element;
      const popoverService = this.ctx.$injector.get(TbPopoverService);
      const ctx: any = {
        entities
      };
      const popoverPosition = ['topleft', 'bottomleft'].includes(this.settings.controlsPosition) ? 'rightTop' : 'leftTop';
      const selectMapEntityPanelPopover = popoverService.displayPopover(trigger, this.ctx.renderer,
        this.ctx.widgetContentContainer, SelectMapEntityPanelComponent, popoverPosition, true, null,
        ctx,
        {},
        {}, {}, false);
      this.currentPopover = selectMapEntityPanelPopover;
      return selectMapEntityPanelPopover.tbComponentRef.instance.entitySelected.asObservable().pipe(
        tap(() => {
          this.currentPopover = null;
        })
      );
    }
  }

  private setupCustomActions() {
    const widgetHeaderActions = this.ctx.actionsApi.getActionDescriptors('headerButton');
    const mapActionButtons = this.settings.mapActionButtons;

    const hasMarkerAction =
      mapActionButtons?.some(actionButton => actionButton.action.mapItemType === MapItemType.marker) ||
      widgetHeaderActions.some(action => action.type === WidgetActionType.placeMapItem && action.mapItemType === MapItemType.marker);

    if (hasMarkerAction) {
      this.setPlaceMarkerStyle();
    }

    if (!mapActionButtons?.length) {
      return;
    }

    this.customActionsToolbar = L.TB.topToolbar({
      mapElement: $(this.mapElement),
      iconRegistry: this.ctx.$injector.get(MatIconRegistry)
    });

    const customTranslate = this.ctx.$injector.get(CustomTranslatePipe);

    mapActionButtons.forEach(actionButton => {
      const actionButtonConfig = {
        icon: actionButton.icon,
        color: actionButton.color,
        title: customTranslate.transform(actionButton.label)
      };
      const toolbarButton = this.customActionsToolbar.toolbarButton(actionButtonConfig);
      toolbarButton.onClick((e, button) => this.ctx.actionsApi.handleWidgetAction(e, actionButton.action, null, null, {button}));
    });
  }

  public placeMapItem(actionData: PlaceMapItemActionData): void {
    switch (actionData.action.mapItemType) {
      case MapItemType.marker:
        this.createMarker(actionData);
        break;
      case MapItemType.polygon:
        this.createPolygon(actionData);
        break;
      case MapItemType.rectangle:
        this.createRectangle(actionData);
        break;
      case MapItemType.circle:
        this.createCircle(actionData);
        break;
      case MapItemType.polyline:
        this.createPolyline(actionData);
        break;
    }
  }

  private createMarker(actionData: PlaceMapItemActionData) {
    this.createItem(actionData, () => this.prepareDrawMode('Marker', {
      placeMarker: actionData.action.mapItemTooltips.placeMarker
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.placeMarker)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.placeMarker)
    }));
  }

  private createRectangle(actionData: PlaceMapItemActionData): void {
    this.createItem(actionData, () => this.prepareDrawMode('Rectangle', {
      firstVertex: actionData.action.mapItemTooltips.startRect
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.startRect)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.startRect),
      finishRect: actionData.action.mapItemTooltips.finishRect
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.finishRect)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.finishRect),
    }));
  }

  private createPolygon(actionData: PlaceMapItemActionData): void {
    this.createItem(actionData, () => this.prepareDrawMode('Polygon', {
      firstVertex: actionData.action.mapItemTooltips.firstVertex
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.firstVertex)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.firstVertex),
      continueLine: actionData.action.mapItemTooltips.continueLine
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.continueLine)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.continueLine),
      finishPoly: actionData.action.mapItemTooltips.finishPoly
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.finishPoly)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.finishPoly),
    }));
  }

  private createCircle(actionData: PlaceMapItemActionData): void {
    this.createItem(actionData, () => this.prepareDrawMode('Circle', {
      startCircle: actionData.action.mapItemTooltips.startCircle
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.startCircle)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.startCircle),
      finishCircle: actionData.action.mapItemTooltips.finishCircle
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.finishCircle)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.finishCircle),
    }));
  }

  private createPolyline(actionData: PlaceMapItemActionData): void {
    this.createItem(actionData, () => this.prepareDrawMode('Line', {
      startPolyline: actionData.action.mapItemTooltips.startPolyline
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.startPolyline)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.startPolyline),
      finishPolyline: actionData.action.mapItemTooltips.finishPolyline
        ? this.ctx.utilsService.customTranslation(actionData.action.mapItemTooltips.finishPolyline)
        : this.ctx.translate.instant(mapItemTooltipsTranslation.finishPolyline),
    }));
  }

  private createItem(actionData: PlaceMapItemActionData, prepareDrawMode: () => void) {
    const actionId = 'id' in actionData.action ? actionData.action.id : 'map-button';
    if (this.createMapItemActionId === actionId) {
      this.finishCreatedItem();
      return;
    }
    if (isDefined(this.createMapItemActionId)) {
      this.finishCreatedItem();
    }
    this.updatePlaceItemState(actionData.additionalParams?.button, true);

    this.map.once('pm:create', (e) => {
      actionData.afterPlaceItemCallback(e as any, actionData.action, null, null, {
        coordinates: convertLayerToCoordinates(actionData.action.mapItemType, e.layer),
        layer: e.layer,
        button: actionData.additionalParams?.button
      });

      // @ts-ignore
      e.layer._pmTempLayer = true;
      e.layer.remove();
      this.finishCreatedItem();
    });

    prepareDrawMode();

    this.latestDataLayers.forEach(dl => dl.disableEditMode());

    this.editToolbar.open([
      {
        id: 'cancel',
        iconClass: 'tb-close',
        title: this.ctx.translate.instant('action.cancel'),
        showText: true,
        click: this.finishCreatedItem
      }
    ], false);

    this.createMapItemActionId = actionId;

    const convertLayerToCoordinates = (type: MapItemType, layer: L.Layer): { x: number; y: number } | TbPolygonRawCoordinates | TbCircleData => {
      switch (type) {
        case MapItemType.marker:
          if (layer instanceof L.Marker) {
            return this.latLngToLocationData(layer.getLatLng());
          }
          return null;
        case MapItemType.polygon:
          if (layer instanceof L.Polygon) {
            let coordinates: any = layer.getLatLngs();
            if (coordinates.length === 1) {
              coordinates = coordinates[0];
            }
            return this.coordinatesToPolygonData(coordinates);
          }
          return null;
        case MapItemType.rectangle:
          if (layer instanceof L.Rectangle) {
            const bounds = layer.getBounds();
            return this.coordinatesToPolygonData([bounds.getNorthWest(), bounds.getSouthEast()])
          }
          return null;
        case MapItemType.circle:
          if (layer instanceof L.Circle) {
            return this.coordinatesToCircleData(layer.getLatLng(), layer.getRadius());
          }
          return null;
        case MapItemType.polyline:
          if (layer instanceof L.Polyline) {
            let coordinates: any = layer.getLatLngs();
            if (coordinates.length === 1) {
              coordinates = coordinates[0];
            }
            return this.coordinatesToPolylineData(coordinates);
          }
          return null;
      }
    }
  }

  private finishCreatedItem = () => {
    delete this.createMapItemActionId;
    this.finishAdd();
  }

  private finishAdd = () => {
    if (this.currentPopover) {
      this.currentPopover.hide();
      this.currentPopover = null;
    }
    this.map.off('pm:create');
    this.map.pm.disableDraw();
    this.latestDataLayers.forEach(dl => dl.enableEditMode());
    this.updatePlaceItemState();
    this.editToolbar.close();
  }

  private prepareDrawMode(shape: 'Marker' | 'Rectangle' | 'Polygon' | 'Circle' | 'Line', tooltipsTranslation: Record<string, string>) {
    this.map.pm.setLang('en', {tooltips: tooltipsTranslation}, 'en');
    this.map.pm.enableDraw(shape);
    // @ts-ignore
    L.DomUtil.addClass(this.map.pm.Draw[shape]._hintMarker.getTooltip()._container, 'tb-place-item-label');
  }

  private updatePlaceItemState(editButton?: L.TB.ToolbarButton, disabled = false): void {
    if (editButton) {
      this.deselectItem(false, true);
      editButton.setActive(true);
    } else if (this.currentEditButton) {
      this.currentEditButton.setActive(false);
    }
    this.currentEditButton = editButton;
    this.updateEditButtonsStates(disabled);
  }

  private createdControlButtonTooltip(root: HTMLElement, side: TooltipPositioningSide) {
    import('tooltipster').then(() => {
      let tooltipData = this.tooltipInstances.find(d => d.root === root);
      if (!tooltipData) {
        tooltipData = {
          root,
          instances: []
        }
        this.tooltipInstances.push(tooltipData);
      }
      if ($.tooltipster) {
        tooltipData.instances.forEach((instance) => {
          instance.destroy();
        });
        tooltipData.instances = [];
      }
      $(root)
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
          const tooltip = $(element).tooltipster(
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
              side,
              distance: 2,
              trackOrigin: true,
              functionBefore: (_instance, helper) => {
                if (helper.origin.ariaDisabled === 'true' || helper.origin.parentElement.classList.contains('active')) {
                  return false;
                }
              },
            }
          );
          const instance = tooltip.tooltipster('instance');
          tooltipData.instances.push(instance);
          instance.on('destroyed', () => {
            const index = tooltipData.instances.indexOf(instance);
            if (index > -1) {
              tooltipData.instances.splice(index, 1);
            }
          });
        });
    });
  }

  private update(subscription: IWidgetSubscription) {
    this.dsData = formattedDataFormDatasourceData<TbMapDatasource>(subscription.data,
      undefined, undefined, el => el.datasource.entityId + el.datasource.mapDataIds[0]);
    this.latestDataLayers.forEach(dl => dl.updateData(this.dsData));
    this.updateTripsAppearance();
    this.updateTripsAnchors();
    this.updateBounds();
    this.updateEditButtonsStates();
    this.ctx.updateLabelPatterns();
  }

  private updateTrips(subscription: IWidgetSubscription) {
    const tripsData = formattedDataArrayFromDatasourceData<TbMapDatasource>(subscription.data, el => el.datasource.entityId + el.datasource.mapDataIds[0]);
    const tripsLatestData = formattedDataFormDatasourceData<TbMapDatasource>(subscription.latestData,
      undefined, undefined, el => el.datasource.entityId + el.datasource.mapDataIds[0]);

    let minTime = Infinity;
    let maxTime = -Infinity;
    for (const tripsDataLayer of this.tripDataLayers) {
      const minMax = tripsDataLayer.prepareTripsData(tripsData, tripsLatestData);
      minTime = Math.min(minMax.minTime, minTime);
      maxTime = Math.max(minMax.maxTime, maxTime);
    }
    const prevMinTime = this.minTime;
    const prevMaxTime = this.maxTime;
    this.minTime = minTime;
    this.maxTime = maxTime;
    if (this.timeline) {
      this.timeLineComponent.min = this.minTime;
      this.timeLineComponent.max = this.maxTime;
      const currentTime = this.calculateCurrentTime(prevMinTime, prevMaxTime);
      if (currentTime !== this.currentTime) {
        this.currentTime = currentTime;
        this.timeLineComponent.currentTime = currentTime;
      }
    } else {
      this.currentTime = this.maxTime;
    }
    this.tripDataLayers.forEach(dl => dl.updateTrips());
    this.updateTripsAnchors();
    this.updateBounds();
  }

  private updateTripsWithLatestData(subscription: IWidgetSubscription) {
    const tripsLatestData = formattedDataFormDatasourceData<TbMapDatasource>(subscription.latestData,
      undefined, undefined, el => el.datasource.entityId + el.datasource.mapDataIds[0]);
    this.tripDataLayers.forEach(dl => dl.updateTripsLatestData(tripsLatestData));
    this.updateTripsAnchors();
  }

  private updateTripsAppearance() {
    this.tripDataLayers.forEach(dl => dl.updateAppearance());
  }

  private updateTripsTime() {
    this.tripDataLayers.forEach(dl => dl.updateCurrentTime());
  }

  private updateTripsAnchors() {
    if (this.timeline) {
      if (this.settings.tripTimeline.snapToRealLocation) {
        // Recalculate anchors only for enabled layers
        let anchors: number[] = [];
        const enableTrips = this.tripDataLayers.filter(dl => dl.isEnabled());
        for (const tripsDataLayer of enableTrips) {
          const tripsAnchors = tripsDataLayer.calculateAnchors();
          anchors = [...new Set([...anchors, ...tripsAnchors])];
        }
        anchors.sort((a, b) => a - b);
        this.timeLineComponent.anchors = anchors;
      }
    }
  }

  private calculateCurrentTime(minTime: number, maxTime: number): number {
    if (minTime !== this.minTime || maxTime !== this.maxTime) {
      if (this.minTime >= this.currentTime || isUndefined(this.currentTime) || this.currentTime === Infinity) {
        return this.minTime;
      } else if (this.maxTime <= this.currentTime) {
        return this.maxTime;
      } else {
        return this.minTime + Math.ceil((this.currentTime - this.minTime) / this.settings.tripTimeline.timeStep) * this.settings.tripTimeline.timeStep;
      }
    }
    return this.currentTime;
  }

  private resize() {
    this.onResize();
    this.map?.invalidateSize();
    this.currentPopover?.updatePosition();
  }

  private updateBounds(force = false) {
    const enabledDataLayers = this.dataLayers.filter(dl => dl.isEnabled());
    const dataLayersBounds = enabledDataLayers.map(dl => dl.getBounds()).filter(b => b.isValid());
    let bounds: L.LatLngBounds;
    if (dataLayersBounds.length) {
      bounds = new L.LatLngBounds(null, null);
      dataLayersBounds.forEach(b => bounds.extend(b));
      const mapBounds = this.map.getBounds();
      if (bounds.isValid() &&
        (
          (!this.bounds || !this.bounds.isValid() || (!this.bounds.equals(bounds) || force) && this.settings.fitMapBounds)
          && !mapBounds.contains(bounds)
        )
      ) {
        this.bounds = bounds;
        if (!this.ignoreUpdateBounds && !this.isPlacingItem) {
          this.fitBounds(bounds);
        }
      }

    }
  }

  private updateEditButtonsStates(disabled = false) {
    if (this.currentEditButton || disabled) {
      if (this.dragModeButton && this.dragModeButton !== this.currentEditButton) {
        this.dragModeButton.setDisabled(true);
      }
      if (this.addMarkerButton && this.addMarkerButton !== this.currentEditButton) {
        this.addMarkerButton.setDisabled(true);
      }
      if (this.addRectangleButton && this.addRectangleButton !== this.currentEditButton) {
        this.addRectangleButton.setDisabled(true);
      }
      if (this.addPolygonButton && this.addPolygonButton !== this.currentEditButton) {
        this.addPolygonButton.setDisabled(true);
      }
      if (this.addCircleButton && this.addCircleButton !== this.currentEditButton) {
        this.addCircleButton.setDisabled(true);
      }
      if (this.addPolylineButton && this.addPolylineButton !== this.currentEditButton) {
        this.addPolylineButton.setDisabled(true);
      }
      this.customActionsToolbar?.setDisabled(true);
    } else {
      if (this.dragModeButton) {
        this.dragModeButton.setDisabled(false);
      }
      if (this.addMarkerButton) {
        this.addMarkerButton.setDisabled(!this.addMarkerDataLayers.some(dl => dl.isEnabled() && dl.hasUnplacedItems()));
      }
      if (this.addRectangleButton) {
        this.addRectangleButton.setDisabled(!this.addPolygonDataLayers.some(dl => dl.isEnabled() && dl.hasUnplacedItems()));
      }
      if (this.addPolygonButton) {
        this.addPolygonButton.setDisabled(!this.addPolygonDataLayers.some(dl => dl.isEnabled() && dl.hasUnplacedItems()));
      }
      if (this.addCircleButton) {
        this.addCircleButton.setDisabled(!this.addCircleDataLayers.some(dl => dl.isEnabled() && dl.hasUnplacedItems()));
      }
      if (this.addPolylineButton) {
        this.addPolylineButton.setDisabled(!this.addPolylineDataLayers.some(dl => dl.isEnabled() && dl.hasUnplacedItems()));
      }
      this.customActionsToolbar?.setDisabled(false);
    }
  }

  private setPlaceMarkerStyle() {
    createPlaceItemIcon(this.getCtx().$injector.get(MatIconRegistry), this.getCtx().$injector.get(DomSanitizer)).subscribe(
      ((iconUrl) => {
        const icon = L.icon({
          iconUrl,
          iconSize: [40, 40],
          iconAnchor: [20, 40]
        });
        this.map.pm.setGlobalOptions({
          markerStyle: {
            icon
          }
        });
      })
    );
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
        container: $(this.mapLayoutElement),
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

  public useShapePattern(patternId: string, prevPatternId?: string): L.TB.Pattern {
    if (prevPatternId && patternId !== prevPatternId) {
      this.unUseShapePattern(prevPatternId);
    }
    if (this.shapePatternStorage[patternId]) {
      const patternItem = this.shapePatternStorage[patternId];
      if (patternId !== prevPatternId) {
        patternItem.refCount++;
        return patternItem.pattern;
      } else {
        return patternItem.pattern;
      }
    }
  }

  public unUseShapePattern(patternId: string): void {
    if (patternId) {
      const patternItem = this.shapePatternStorage[patternId];
      if (patternItem) {
        patternItem.refCount--;
        if (patternItem.refCount === 0) {
          patternItem.pattern.remove();
          delete this.shapePatternStorage[patternId];
        }
      }
    }
  }

  public storeShapePattern(patternId: string, pattern: L.TB.Pattern): void {
    pattern.addTo(this.map);
    this.shapePatternStorage[patternId] = {
      pattern,
      refCount: 1
    };
  }

  public enabledDataLayersUpdated() {
    this.updateEditButtonsStates();
    this.updateTripsAnchors();
  }

  public dataItemClick($event: Event, action: WidgetAction, data: FormattedData<TbMapDatasource>) {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    const {entityId, entityName, entityLabel, entityType} = data.$datasource;
    this.ctx.actionsApi.handleWidgetAction($event, action, {
      entityType,
      id: entityId
    }, entityName, data, entityLabel);
  }

  public selectItem(item: TbLatestDataLayerItem, cancel = false, force = false): boolean {
    if (this.isPlacingItem) {
      return false;
    }
    let deselected = true;
    if (this.selectedDataItem) {
      deselected = this.selectedDataItem.deselect(cancel, force);
      if (deselected) {
        this.selectedDataItem = null;
        this.editToolbar.close();
      }
    }
    if (deselected) {
      this.selectedDataItem = item;
      if (this.selectedDataItem) {
        const buttons = this.selectedDataItem.select();
        this.editToolbar.open(buttons);
        this.createdControlButtonTooltip(this.editToolbar.container, 'top');
      }
    }
    this.ignoreUpdateBounds = !!this.selectedDataItem;
    return deselected;
  }

  public deselectItem(cancel = false, force = false): boolean {
    return this.selectItem(null, cancel, force);
  }

  public getEditToolbar(): L.TB.BottomToolbarControl {
    return this.editToolbar;
  }

  public useDragModeButton(): boolean {
    return this.settings.dragModeButton;
  }

  public dragModeEnabled(): boolean {
    return this.dragMode;
  }

  public saveMarkerLocation(data: FormattedData<TbMapDatasource>, lat?: number, lng?: number): Observable<any> {
    const targetDataLayer = this.latestDataLayers.find(dl => dl.dataLayerType() === 'markers' && dl.hasData(data));
    if (targetDataLayer) {
      let location: L.LatLng = null;
      if (isDefinedAndNotNull(lat) && isDefinedAndNotNull(lng)) {
        location = new L.LatLng(lat, lng);
      }
      return (targetDataLayer as TbMarkersDataLayer).saveMarkerLocation(data, location);
    } else {
      return EMPTY;
    }
  }

  public savePolygonLocation(data: FormattedData<TbMapDatasource>, coordinates?: TbPolygonCoordinates): Observable<any> {
    const targetDataLayer = this.latestDataLayers.find(dl => dl.dataLayerType() === 'polygons' && dl.hasData(data));
    if (targetDataLayer) {
      return (targetDataLayer as TbPolygonsDataLayer).savePolygonCoordinates(data, coordinates);
    } else {
      return EMPTY;
    }
  }

  public saveLocation(data: FormattedData<TbMapDatasource>, values: { [key: string]: any }): Observable<any> {
    const datasource = data.$datasource;
    let dataKeys = datasource.dataKeys;
    if (datasource.latestDataKeys) {
      dataKeys = dataKeys.concat(datasource.latestDataKeys);
    }
    const itemData: DataKeyValuePair[] = [];
    for (const dataKeyName of Object.keys(values)) {
      const dataKey = dataKeys.find(key => key.name === dataKeyName);
      if (dataKey) {
        itemData.push({
          dataKey,
          value: values[dataKeyName]
        });
      }
    }
    return this.saveItemData(datasource, itemData, AttributeScope.SERVER_SCOPE);
  }

  public saveItemData(datasource: TbMapDatasource, data: DataKeyValuePair[], attributeScope: AttributeScope): Observable<any> {
    const attributeService = this.ctx.$injector.get(AttributeService);
    const attributes: AttributeData[] = [];
    const timeseries: AttributeData[] = [];
    const entityId: EntityId = {
      entityType: datasource.entityType,
      id: datasource.entityId
    };

    const targetAttributeScope =
      entityId.entityType === EntityType.DEVICE && !!attributeScope ? attributeScope : AttributeScope.SERVER_SCOPE;

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
        targetAttributeScope,
        attributes
      ));
    }
    if (observables.length) {
      return forkJoin(observables);
    } else {
      return of(null);
    }
  }

  // Timeline methods

  public hasTimeline(): boolean {
    return this.timeline;
  }

  public getMinTime(): number {
    return this.minTime;
  }

  public getMaxTime(): number {
    return this.maxTime;
  }

  public getTimeStep(): number {
    return this.timeStep;
  }

  public getCurrentTime(): number {
    return this.currentTime;
  }

  public getLocationSnapFilterFunction(): CompiledTbFunction<MapBooleanFunction> {
    return this.locationSnapFilterFunction;
  }

  public destroy() {
    if (this.mapResize$) {
      this.mapResize$.disconnect();
    }
    if (this.map) {
      this.map.remove();
    }
    this.tooltipInstances.forEach((data) => {
      data.instances.forEach(instance => {
        instance.destroy();
      })
    });
    if (this.timeLineComponentRef) {
      this.timeLineComponentRef.destroy();
    }
  }

  public abstract locationDataToLatLng(position: { x: number; y: number }): L.LatLng;

  public abstract latLngToLocationData(position: L.LatLng): { x: number; y: number };

  public abstract polygonDataToCoordinates(coordinates: TbPolygonRawCoordinates): TbPolygonRawCoordinates;

  public abstract coordinatesToPolygonData(coordinates: TbPolygonCoordinates): TbPolygonRawCoordinates;

  public abstract polylineDataToCoordinates(coordinates: TbPolylineRawCoordinates): TbPolylineRawCoordinates;

  public abstract coordinatesToPolylineData(coordinates: TbPolylineCoordinates): TbPolylineRawCoordinates;

  public abstract circleDataToCoordinates(circle: TbCircleData): TbCircleData;

  public abstract coordinatesToCircleData(center: L.LatLng, radius: number): TbCircleData;


}
