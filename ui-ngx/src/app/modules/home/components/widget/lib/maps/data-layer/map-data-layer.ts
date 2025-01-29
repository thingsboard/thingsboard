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
  DataLayerColorSettings,
  DataLayerColorType,
  DataLayerEditAction,
  DataLayerPatternSettings,
  DataLayerPatternType,
  DataLayerTooltipTrigger,
  MapDataLayerSettings,
  mapDataSourceSettingsToDatasource,
  MapStringFunction,
  MapType,
  processTooltipTemplate,
  TbMapDatasource
} from '@home/components/widget/lib/maps/models/map.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { FormattedData } from '@shared/models/widget.models';
import { forkJoin, Observable, of } from 'rxjs';
import {
  createLabelFromPattern,
  guid,
  isDefined,
  mergeDeepIgnoreArray,
  parseTbFunction,
  safeExecuteTbFunction
} from '@core/utils';
import L from 'leaflet';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { map } from 'rxjs/operators';
import { WidgetContext } from '@home/models/widget-component.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';

export abstract class TbDataLayerItem<S extends MapDataLayerSettings = MapDataLayerSettings,
  D extends TbMapDataLayer<S,D> = TbMapDataLayer<any, any>, L extends L.Layer = L.Layer> {

  protected layer: L;
  protected tooltip: L.Popup;
  protected data: FormattedData<TbMapDatasource>;
  protected selected = false;

  protected constructor(data: FormattedData<TbMapDatasource>,
                        dsData: FormattedData<TbMapDatasource>[],
                        protected settings: S,
                        protected dataLayer: D) {
    this.data = data;
    this.layer = this.create(data, dsData);
    if (this.settings.tooltip?.show) {
      this.createTooltip(data.$datasource);
      this.updateTooltip(data, dsData);
    }
    this.bindEvents();
    this.createEventListeners(data, dsData);
    try {
      this.dataLayer.getDataLayerContainer().addLayer(this.layer);
      this.editModeUpdated();
    } catch (e) {
      console.warn(e);
    }
  }

  protected abstract create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L;

  protected abstract doUpdate(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void;

  protected abstract doInvalidateCoordinates(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void;

  protected abstract unbindLabel(): void;

  protected abstract bindLabel(content: L.Content): void;

  protected abstract createEventListeners(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void;

  protected abstract addItemClass(clazz: string): void;

  protected abstract removeItemClass(clazz: string): void;

  protected abstract enableDrag(): void;

  protected abstract disableDrag(): void;

  protected abstract updateBubblingMouseEvents(): void;

  protected bindEvents(): void {
    if (this.dataLayer.isSelectable()) {
      this.layer.on('click', () => {
        if (!this.isEditing()) {
          this.dataLayer.getMap().selectItem(this);
        }
      });
    }
  }

  protected enableEdit(): void {
    if (this.dataLayer.isHoverable()) {
      this.addItemClass('tb-hoverable');
    }
    if (this.dataLayer.isDragEnabled()) {
      this.enableDrag();
      this.addItemClass('tb-draggable');
    }
  }

  protected disableEdit(): void {
    if (this.dataLayer.isHoverable()) {
      this.removeItemClass('tb-hoverable');
    }
    if (this.dataLayer.isDragEnabled()) {
      this.disableDrag();
      this.removeItemClass('tb-draggable');
    }
  }

  protected updateSelectedState() {
    if (this.selected) {
      this.addItemClass('tb-selected');
    } else {
      this.removeItemClass('tb-selected');
    }
  }

  public invalidateCoordinates(): void {
    this.doInvalidateCoordinates(this.data, this.dataLayer.getMap().getData());
  }

  public select(): L.TB.ToolbarButtonOptions[] {
    if (!this.selected) {
      this.selected = true;
      this.disableEdit();
      this.updateSelectedState();
      const buttons = this.onSelected();
      if (this.dataLayer.isRemoveEnabled()) {
        buttons.push({
          id: 'remove',
          title: this.removeDataItemTitle(),
          click: () => {
            this.removeDataItem();
          },
          iconClass: 'tb-remove'
        });
      }
      return buttons;
    } else {
      return [];
    }
  }

  public deselect(cancel = false, force = false): boolean {
    if (this.selected) {
      if (this.canDeselect(cancel) || force) {
        this.selected = false;
        this.layer.closePopup();
        this.updateSelectedState();
        this.onDeselected();
        this.editModeUpdated();
      } else {
        return false;
      }
    }
    return true;
  }

  public isSelected() {
    return this.selected;
  }

  public editModeUpdated() {
    if (this.dataLayer.isEditMode() && !this.selected) {
      this.enableEdit();
    } else {
      this.disableEdit();
    }
    this.updateSelectedState();
    this.updateBubblingMouseEvents();
  }

  public update(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void {
    this.data = data;
    this.doUpdate(data, dsData);
  }

  public remove() {
    if (this.selected) {
      this.dataLayer.getMap().deselectItem(false, true);
    }
    this.dataLayer.getDataLayerContainer().removeLayer(this.layer);
    this.layer.off();
  }

  public getLayer(): L {
    return this.layer;
  }

  public getDataLayer(): D {
    return this.dataLayer;
  }

  public isEditing() {
    return false;
  }

  protected updateTooltip(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    if (this.settings.tooltip.show) {
      let tooltipTemplate = this.dataLayer.dataLayerTooltipProcessor.processPattern(data, dsData);
      tooltipTemplate = processTooltipTemplate(tooltipTemplate);
      this.tooltip.setContent(tooltipTemplate);
      if (this.tooltip.isOpen() && this.tooltip.getElement()) {
        this.bindTooltipActions(data.$datasource);
      }
    }
  }

  protected updateLabel(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    if (this.settings.label.show) {
      this.unbindLabel();
      const label = this.dataLayer.dataLayerLabelProcessor.processPattern(data, dsData);
      const labelColor = this.dataLayer.getCtx().widgetConfig.color;
      const content: L.Content = `<div style="color: ${labelColor};"><b>${label}</b></div>`;
      this.bindLabel(content);
    }
  }

  protected canDeselect(cancel = false): boolean {
    return true;
  }

  protected onSelected(): L.TB.ToolbarButtonOptions[] {
    return [];
  }

  protected onDeselected(): void {}

  protected abstract removeDataItemTitle(): string;

  protected abstract removeDataItem(): void;

  private createTooltip(datasource: TbMapDatasource) {
    this.tooltip = L.popup();
    this.layer.bindPopup(this.tooltip, {autoClose: this.settings.tooltip.autoclose, closeOnClick: false});
    this.layer.off('click');
    if (this.settings.tooltip.trigger === DataLayerTooltipTrigger.click) {
      this.layer.on('click', () => {
        if (this.tooltip.isOpen()) {
          this.layer.closePopup();
        } else if (!this.isEditing()) {
          this.layer.openPopup();
        }
      });
    } else if (this.settings.tooltip.trigger === DataLayerTooltipTrigger.hover) {
      this.layer.on('mouseover', () => {
        if (!this.isEditing()) {
          this.layer.openPopup();
        }
      });
      this.layer.on('mousemove', (e) => {
        this.tooltip.setLatLng(e.latlng);
      });
      this.layer.on('mouseout', () => {
        this.layer.closePopup();
      });
    }
    this.layer.on('popupopen', () => {
      this.bindTooltipActions(datasource);
      (this.layer as any)._popup._closeButton.addEventListener('click', (event: Event) => {
        event.preventDefault();
      });
    });
  }

  private bindTooltipActions(datasource: TbMapDatasource) {
    const actions = this.tooltip.getElement().getElementsByClassName('tb-custom-action');
    Array.from(actions).forEach(
      (element: HTMLElement) => {
        const actionName = element.getAttribute('data-action-name');
        this.dataLayer.getMap().tooltipElementClick(element, actionName, datasource);
      });
  }

}

export enum MapDataLayerType {
   marker = 'marker',
   polygon = 'polygon',
   circle = 'circle'
}

class DataLayerPatternProcessor {

  private patternFunction: CompiledTbFunction<MapStringFunction>;
  private pattern: string;

  constructor(private dataLayer: TbMapDataLayer<any, any>,
              private settings: DataLayerPatternSettings) {}

  public setup(): Observable<void> {
    if (this.settings.type === DataLayerPatternType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.patternFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.patternFunction = parsed;
          return null;
        })
      );
    } else {
      this.pattern = this.settings.pattern;
      return of(null)
    }
  }

  public processPattern(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): string {
    let pattern: string;
    if (this.settings.type === DataLayerPatternType.function) {
      pattern = safeExecuteTbFunction(this.patternFunction, [data, dsData]);
    } else {
      pattern = this.pattern;
    }
    const text = createLabelFromPattern(pattern, data);
    const customTranslate = this.dataLayer.getCtx().$injector.get(CustomTranslatePipe);
    return customTranslate.transform(text);
  }

}

export class DataLayerColorProcessor {

  private colorFunction: CompiledTbFunction<MapStringFunction>;
  private color: string;

  constructor(private dataLayer: TbMapDataLayer<any, any>,
              private settings: DataLayerColorSettings) {}

  public setup(): Observable<void> {
    if (this.settings.type === DataLayerColorType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.colorFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.colorFunction = parsed;
          return null;
        })
      );
    } else {
      this.color = this.settings.color;
      return of(null)
    }
  }

  public processColor(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): string {
    let color: string;
    if (this.settings.type === DataLayerColorType.function) {
      color = safeExecuteTbFunction(this.colorFunction, [data, dsData]);
    } else {
      color = this.color;
    }
    return color;
  }

}

export interface UnplacedMapDataItem {
  entity: FormattedData<TbMapDatasource>;
  dataLayer: TbMapDataLayer;
}

export abstract class TbMapDataLayer<S extends MapDataLayerSettings = MapDataLayerSettings, D extends TbMapDataLayer<S,D> = any, L extends L.Layer = L.Layer> implements L.TB.DataLayer {

  protected settings: S;

  protected datasource: TbMapDatasource;

  protected mapDataId = guid();

  protected dataLayerContainer: L.FeatureGroup;

  protected layerItems = new Map<string, TbDataLayerItem<S,D,L>>();

  protected groupsState: {[group: string]: boolean} = {};

  protected enabled = true;

  protected addEnabled = false;
  protected dragEnabled = false;
  protected editEnabled = false;
  protected removeEnabled = false;

  protected editable = false;
  protected selectable = false;
  protected hoverable = false;
  protected snappable = false;

  private editMode = false;

  private unplacedItems: UnplacedMapDataItem[] = [];

  public dataLayerLabelProcessor: DataLayerPatternProcessor;
  public dataLayerTooltipProcessor: DataLayerPatternProcessor;

  protected constructor(protected map: TbMap<any>,
                        inputSettings: S) {
    this.settings = mergeDeepIgnoreArray({} as S, this.defaultBaseSettings(map) as S, inputSettings);
    if (this.settings.groups?.length) {
      this.settings.groups.forEach((group) => {
        this.groupsState[group] = true;
      });
    }

    if (this.settings.edit?.enabledActions) {
      this.addEnabled = this.settings.edit.enabledActions.includes(DataLayerEditAction.add);
      this.dragEnabled = this.settings.edit.enabledActions.includes(DataLayerEditAction.move);
      this.editEnabled = this.settings.edit.enabledActions.includes(DataLayerEditAction.edit);
      this.removeEnabled = this.settings.edit.enabledActions.includes(DataLayerEditAction.remove);

      this.editable = this.addEnabled || this.dragEnabled || this.editEnabled || this.removeEnabled;
      this.selectable = this.removeEnabled || this.editEnabled;
      this.hoverable = this.selectable || this.dragEnabled;
      this.snappable = this.settings.edit.snappable;
    }

    this.dataLayerContainer = this.createDataLayerContainer();
    this.dataLayerLabelProcessor = this.settings.label.show ? new DataLayerPatternProcessor(this, this.settings.label) : null;
    this.dataLayerTooltipProcessor = this.settings.tooltip.show ? new DataLayerPatternProcessor(this, this.settings.tooltip): null;
    this.map.getMap().addLayer(this.dataLayerContainer);
    this.enableEditMode();
  }

  public setup(): Observable<any> {
    this.datasource = mapDataSourceSettingsToDatasource(this.settings);
    this.datasource.dataKeys = this.settings.additionalDataKeys ? [...this.settings.additionalDataKeys] : [];
    this.mapDataId = this.datasource.mapDataIds[0];
    this.datasource = this.setupDatasource(this.datasource);
    return forkJoin(
      [
        this.dataLayerLabelProcessor ? this.dataLayerLabelProcessor.setup() : of(null),
        this.dataLayerTooltipProcessor ? this.dataLayerTooltipProcessor.setup() : of(null),
        this.doSetup()
      ]);
  }

  public getDatasource(): TbMapDatasource {
    return this.datasource;
  }

  public getDataLayerContainer(): L.FeatureGroup {
    return this.dataLayerContainer;
  }

  public getBounds(): L.LatLngBounds {
    return this.dataLayerContainer.getBounds();
  }

  public isEnabled(): boolean {
    return this.enabled;
  }

  public isEditMode(): boolean {
    return this.editMode;
  }

  public isAddEnabled(): boolean {
    return this.addEnabled;
  }

  public isDragEnabled(): boolean {
    return this.dragEnabled;
  }

  public isEditEnabled(): boolean {
    return this.editEnabled;
  }

  public isRemoveEnabled(): boolean {
    return this.removeEnabled;
  }

  public isEditable(): boolean {
    return this.editable;
  }

  public isHoverable(): boolean {
    return this.hoverable;
  }

  public isSelectable(): boolean {
    return this.selectable;
  }

  public isSnappable(): boolean {
    return this.snappable;
  }

  public getGroups(): string[] {
    return this.settings.groups || [];
  }

  public toggleGroup(group: string): boolean {
    if (isDefined(this.groupsState[group])) {
      this.groupsState[group] = !this.groupsState[group];
      const enabled = Object.values(this.groupsState).some(v => v);
      if (this.enabled !== enabled) {
        this.enabled = enabled;
        if (this.enabled) {
          this.map.getMap().addLayer(this.dataLayerContainer);
          this.updateItemsEditMode();
        } else {
          for (const item of this.layerItems) {
            if (item[1].isSelected()) {
              this.getMap().deselectItem(false, true);
              break;
            }
          }
          this.map.getMap().removeLayer(this.dataLayerContainer);
        }
        this.map.enabledDataLayersUpdated();
        return true;
      }
    }
    return false;
  }

  public updateData(dsData: FormattedData<TbMapDatasource>[]) {
    this.unplacedItems.length = 0;
    const layerData = dsData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
    const toDelete = new Set(Array.from(this.layerItems.keys()));
    const updatedItems: TbDataLayerItem<S,D,L>[] = [];
    layerData.forEach((data) => {
      if (this.isValidLayerData(data)) {
        let layerItem = this.layerItems.get(data.entityId);
        if (layerItem) {
          layerItem.update(data, dsData);
          updatedItems.push(layerItem);
        } else {
          layerItem = this.createLayerItem(data, dsData);
          this.layerItems.set(data.entityId, layerItem);
        }
        toDelete.delete(data.entityId);
      } else {
        this.unplacedItems.push({
          entity: data,
          dataLayer: this
        });
      }
    });
    toDelete.forEach((key) => {
      const item = this.layerItems.get(key);
      item.remove();
      this.layerItems.delete(key);
    });
    if (updatedItems.length) {
      this.layerItemsUpdated(updatedItems);
    }
  }

  public invalidateCoordinates(): void {
    this.layerItems.forEach(item => item.invalidateCoordinates());
  }

  public getCtx(): WidgetContext {
    return this.map.getCtx();
  }

  public getMap(): TbMap<any> {
    return this.map;
  }

  public hasUnplacedItems(): boolean {
    return !!this.unplacedItems.length;
  }

  private prepareUnplacedItems(): UnplacedMapDataItem[] {
    const div = document.createElement('div');
    for (const item of this.unplacedItems) {
      if (!item.entity.entityDisplayName) {
        if (this.settings.label.show) {
          div.innerHTML = this.dataLayerLabelProcessor.processPattern(item.entity, this.getMap().getData());
          item.entity.entityDisplayName = div.textContent || div.innerText || '';
        } else {
          item.entity.entityDisplayName = item.entity.entityName;
        }
      }
    }
    return this.unplacedItems;
  }

  public getUnplacedItems(): UnplacedMapDataItem[] {
    return this.prepareUnplacedItems();
  }

  protected createDataLayerContainer(): L.FeatureGroup {
    return L.featureGroup([], {snapIgnore: !this.snappable});
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    return datasource;
  }

  protected layerItemsUpdated(_updatedItems: TbDataLayerItem<S,D,L>[]): void {
  }

  protected mapType(): MapType {
    return this.map.type();
  }

  public enableEditMode() {
    if (!this.editMode) {
      this.editMode = true;
      this.updateItemsEditMode();
    }
  }

  public disableEditMode() {
    if (this.editMode) {
      this.editMode = false;
      this.updateItemsEditMode();
    }
  }

  private updateItemsEditMode() {
    this.layerItems.forEach(item => item.editModeUpdated());
  }

  public abstract dataLayerType(): MapDataLayerType;

  protected abstract defaultBaseSettings(map: TbMap<any>): Partial<S>;

  protected abstract doSetup(): Observable<any>;

  protected abstract isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean;

  protected abstract createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbDataLayerItem<S,D,L>;

}

