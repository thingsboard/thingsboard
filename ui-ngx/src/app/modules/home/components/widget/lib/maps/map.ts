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
  MapType,
  mergeMapDatasources,
  mergeUnplacedDataItemsArrays,
  parseCenterPosition,
  TbCircleData,
  TbMapDatasource,
  TbPolygonCoordinates,
  TbPolygonRawCoordinates
} from '@home/components/widget/lib/maps/models/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { formattedDataFormDatasourceData, isDefinedAndNotNull, mergeDeepIgnoreArray } from '@core/utils';
import { DeepPartial } from '@shared/models/common';
import L from 'leaflet';
import { forkJoin, Observable, of } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import '@home/components/widget/lib/maps/leaflet/leaflet-tb';
import {
  MapDataLayerType,
  TbDataLayerItem,
  TbMapDataLayer,
  UnplacedMapDataItem,
} from '@home/components/widget/lib/maps/data-layer/map-data-layer';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { FormattedData, WidgetAction, WidgetActionDescriptor, widgetType } from '@shared/models/widget.models';
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
import { createColorMarkerShapeURI, MarkerShape } from '@home/components/widget/lib/maps/models/marker-shape.models';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import tinycolor from 'tinycolor2';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;
import TooltipPositioningSide = JQueryTooltipster.TooltipPositioningSide;

type TooltipInstancesData = {root: HTMLElement, instances: ITooltipsterInstance[]};

export abstract class TbMap<S extends BaseMapSettings> {

  protected settings: S;
  protected map: L.Map;

  protected defaultCenterPosition: [number, number];
  protected ignoreUpdateBounds = false;
  protected bounds: L.LatLngBounds;

  protected southWest = new L.LatLng(-L.Projection.SphericalMercator['MAX_LATITUDE'], -180);
  protected northEast = new L.LatLng(L.Projection.SphericalMercator['MAX_LATITUDE'], 180);

  protected dataLayers: TbMapDataLayer<any,any>[];
  protected dsData: FormattedData<TbMapDatasource>[];

  protected selectedDataItem: TbDataLayerItem;

  protected mapElement: HTMLElement;

  protected sidebar: L.TB.SidebarControl;

  protected customActionsToolbar: L.TB.TopToolbarControl;
  protected editToolbar: L.TB.BottomToolbarControl;

  protected addMarkerButton: L.TB.ToolbarButton;
  protected addRectangleButton: L.TB.ToolbarButton;
  protected addPolygonButton: L.TB.ToolbarButton;
  protected addCircleButton: L.TB.ToolbarButton;

  protected addMarkerDataLayers: TbMapDataLayer<any,any>[];
  protected addPolygonDataLayers: TbMapDataLayer<any,any>[];
  protected addCircleDataLayers: TbMapDataLayer<any,any>[];

  private readonly mapResize$: ResizeObserver;

  private tooltipInstances: TooltipInstancesData[] = [];

  private currentPopover: TbPopoverComponent;
  private currentAddButton: L.TB.ToolbarButton;

  private get isPlacingItem(): boolean {
    return !!this.currentAddButton;
  }

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

     if (this.dataLayers.some(dl => dl.isEditable())) {
       this.map.pm.setGlobalOptions({ snappable: false });
       this.map.pm.applyGlobalOptions();
     }

     const addSupportedDataLayers = this.dataLayers.filter(dl => dl.isAddEnabled());

     if (addSupportedDataLayers.length) {
       const drawToolbar = L.TB.toolbar({
         position: this.settings.controlsPosition
       }).addTo(this.map);
       this.addMarkerDataLayers = addSupportedDataLayers.filter(dl => dl.dataLayerType() === MapDataLayerType.marker);
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
         createColorMarkerShapeURI(this.getCtx().$injector.get(MatIconRegistry), this.getCtx().$injector.get(DomSanitizer), MarkerShape.markerShape1, tinycolor('rgba(255,255,255,0.75)')).subscribe(
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
       this.addPolygonDataLayers = addSupportedDataLayers.filter(dl => dl.dataLayerType() === MapDataLayerType.polygon);
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
       this.addCircleDataLayers = addSupportedDataLayers.filter(dl => dl.dataLayerType() === MapDataLayerType.circle);
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
     }
  }

  private setupCustomActions() {
    this.customActionsToolbar = L.TB.topToolbar({
      mapElement: $(this.mapElement)
    });

    /*const customButton = this.customActionsToolbar.toolbarButton({
      title: 'Super button',
      icon: 'add'
    });
    this.customActionsToolbar.toolbarButton({
      title: 'Super button 2',
      icon: 'add'
    });
    customButton.onClick(e => {
      console.log("Called!");
    });*/
  }

  private placeMarker(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addMarkerDataLayers,
      (entity) => {
        this.map.pm.setLang('en', {
          tooltips: {
            placeMarker: this.ctx.translate.instant('widgets.maps.data-layer.marker.place-marker-hint', {entityName: entity.entity.entityDisplayName})
          }
        }, 'en');
        this.map.pm.enableDraw('Marker');
        // @ts-ignore
        L.DomUtil.addClass(this.map.pm.Draw.Marker._hintMarker.getTooltip()._container, 'tb-place-item-label');
      }
    );
  }

  private drawRectangle(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addPolygonDataLayers,
      (entity) => {
        this.map.pm.setLang('en', {
          tooltips: {
            firstVertex: this.ctx.translate.instant('widgets.maps.data-layer.polygon.rectangle-place-first-point-hint', {entityName: entity.entity.entityDisplayName}),
            finishRect: this.ctx.translate.instant('widgets.maps.data-layer.polygon.finish-rectangle-hint', {entityName: entity.entity.entityDisplayName})
          }
        }, 'en');
        this.map.pm.enableDraw('Rectangle');
        // @ts-ignore
        L.DomUtil.addClass(this.map.pm.Draw.Rectangle._hintMarker.getTooltip()._container, 'tb-place-item-label');
      }
    );
  }

  private drawPolygon(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addPolygonDataLayers,
      (entity) => {
        this.map.pm.setLang('en', {
          tooltips: {
            firstVertex: this.ctx.translate.instant('widgets.maps.data-layer.polygon.polygon-place-first-point-hint', {entityName: entity.entity.entityDisplayName}),
            continueLine: this.ctx.translate.instant('widgets.maps.data-layer.polygon.continue-polygon-hint', {entityName: entity.entity.entityDisplayName}),
            finishPoly: this.ctx.translate.instant('widgets.maps.data-layer.polygon.finish-polygon-hint', {entityName: entity.entity.entityDisplayName})
          }
        }, 'en');
        this.map.pm.enableDraw('Polygon');
        // @ts-ignore
        L.DomUtil.addClass(this.map.pm.Draw.Polygon._hintMarker.getTooltip()._container, 'tb-place-item-label');
      }
    );
  }

  private drawCircle(e: MouseEvent, button: L.TB.ToolbarButton): void {
    this.placeItem(e, button, this.addCircleDataLayers,
      (entity) => {
        this.map.pm.setLang('en', {
          tooltips: {
            startCircle: this.ctx.translate.instant('widgets.maps.data-layer.circle.place-circle-center-hint', {entityName: entity.entity.entityDisplayName}),
            finishCircle: this.ctx.translate.instant('widgets.maps.data-layer.circle.finish-circle-hint', {entityName: entity.entity.entityDisplayName}),
          }
        }, 'en');
        this.map.pm.enableDraw('Circle');
        // @ts-ignore
        L.DomUtil.addClass(this.map.pm.Draw.Circle._hintMarker.getTooltip()._container, 'tb-place-item-label');
      }
    );
  }

  private placeItem(e: MouseEvent, button: L.TB.ToolbarButton, dataLayers: TbMapDataLayer[],
                    prepareDrawMode: (entity: UnplacedMapDataItem) => void): void {
    if (this.isPlacingItem) {
      return;
    }
    this.updatePlaceItemState(button);
    const items = mergeUnplacedDataItemsArrays(dataLayers.filter(dl => dl.isEnabled()).map(dl => dl.getUnplacedItems())).sort((entity1, entity2) => {
      return entity1.entity.entityDisplayName.localeCompare(entity2.entity.entityDisplayName);
    });
    this.selectEntityToPlace(e, items).subscribe((entity) => {
      if (entity) {

        const finishAdd = () => {
          this.map.off('pm:create');
          this.map.pm.disableDraw();
          this.dataLayers.forEach(dl => dl.enableEditMode());
          this.updatePlaceItemState();
          this.editToolbar.close();
        };

        this.map.once('pm:create', (e) => {
          entity.dataLayer.placeItem(entity, e.layer);
          // @ts-ignore
          e.layer._pmTempLayer = true;
          e.layer.remove();
          finishAdd();
        });

        prepareDrawMode(entity);

        this.dataLayers.forEach(dl => dl.disableEditMode());

        this.editToolbar.open([
          {
            id: 'cancel',
            iconClass: 'tb-close',
            title: this.ctx.translate.instant('action.cancel'),
            showText: true,
            click: finishAdd
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

  private updatePlaceItemState(addButton?: L.TB.ToolbarButton): void {
    if (addButton) {
      this.deselectItem(false, true);
      addButton.setActive(true);
    } else if (this.currentAddButton) {
      this.currentAddButton.setActive(false);
    }
    this.currentAddButton = addButton;
    this.updateAddButtonsStates();
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
    this.dataLayers.forEach(dl => dl.updateData(this.dsData));
    this.updateBounds();
    this.updateAddButtonsStates();
  }

  private resize() {
    this.onResize();
    this.map?.invalidateSize();
    this.currentPopover?.updatePosition();
  }

  private updateBounds() {
    const enabledDataLayers = this.dataLayers.filter(dl => dl.isEnabled());
    const dataLayersBounds = enabledDataLayers.map(dl => dl.getBounds()).filter(b => b.isValid());
    let bounds: L.LatLngBounds;
    if (dataLayersBounds.length) {
      bounds = new L.LatLngBounds(null, null);
      dataLayersBounds.forEach(b => bounds.extend(b));
      const mapBounds = this.map.getBounds();
      if (bounds.isValid() && (!this.bounds || !this.bounds.isValid() || !this.bounds.equals(bounds) && this.settings.fitMapBounds && !mapBounds.contains(bounds))) {
        this.bounds = bounds;
        if (!this.ignoreUpdateBounds && !this.isPlacingItem) {
          this.fitBounds(bounds);
        }
      }

    }
  }

  private updateAddButtonsStates() {
    if (this.currentAddButton) {
      if (this.addMarkerButton && this.addMarkerButton !== this.currentAddButton) {
        this.addMarkerButton.setDisabled(true);
      }
      if (this.addRectangleButton && this.addRectangleButton !== this.currentAddButton) {
        this.addRectangleButton.setDisabled(true);
      }
      if (this.addPolygonButton && this.addPolygonButton !== this.currentAddButton) {
        this.addPolygonButton.setDisabled(true);
      }
      if (this.addCircleButton && this.addCircleButton !== this.currentAddButton) {
        this.addCircleButton.setDisabled(true);
      }
    } else {
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
    }
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

  public enabledDataLayersUpdated() {
    this.updateAddButtonsStates();
  }

  public dataItemClick($event: Event, action: WidgetAction, entityInfo: TbMapDatasource) {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    const { entityId, entityName, entityLabel, entityType } = entityInfo;
    this.ctx.actionsApi.handleWidgetAction($event, action, {
      entityType,
      id: entityId
    }, entityName, null, entityLabel);
  }

  public selectItem(item: TbDataLayerItem, cancel = false, force = false): boolean {
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
    this.tooltipInstances.forEach((data) => {
      data.instances.forEach(instance => {
        instance.destroy();
      })
    });
  }

  public abstract locationDataToLatLng(position: {x: number; y: number}): L.LatLng;

  public abstract latLngToLocationData(position: L.LatLng): {x: number; y: number};

  public abstract polygonDataToCoordinates(coordinates: TbPolygonRawCoordinates): TbPolygonRawCoordinates;

  public abstract coordinatesToPolygonData(coordinates: TbPolygonCoordinates): TbPolygonRawCoordinates;

  public abstract circleDataToCoordinates(circle: TbCircleData): TbCircleData;

  public abstract coordinatesToCircleData(center: L.LatLng, radius: number): TbCircleData;



}
