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
  DataLayerEditAction,
  MapDataLayerSettings,
  TbMapDatasource
} from '@shared/models/widget/maps/map.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { FormattedData, WidgetActionType } from '@shared/models/widget.models';
import { Observable } from 'rxjs';
import L from 'leaflet';
import { createTooltip, updateTooltip } from './data-layer-utils';
import { TbDataLayerItem, TbMapDataLayer } from '@home/components/widget/lib/maps/data-layer/map-data-layer';

export abstract class TbLatestDataLayerItem<S extends MapDataLayerSettings = MapDataLayerSettings,
  D extends TbLatestMapDataLayer<S,D> = TbLatestMapDataLayer<any>, L extends L.Layer = L.Layer> extends TbDataLayerItem<S,D,L> {

  protected tooltip: L.Popup;
  protected data: FormattedData<TbMapDatasource>;
  protected selected = false;

  protected constructor(data: FormattedData<TbMapDatasource>,
                        dsData: FormattedData<TbMapDatasource>[],
                        settings: S,
                        dataLayer: D) {
    super(settings, dataLayer);
    this.data = data;
    this.layer = this.create(data, dsData);
    if (this.settings.tooltip?.show) {
      this.tooltip = createTooltip(this.dataLayer.getMap(),
        this.layer, this.settings.tooltip, this.data, () => {
        return !this.isEditing();
      });
      updateTooltip(this.dataLayer.getMap(), this.tooltip,
        this.settings.tooltip, this.dataLayer.dataLayerTooltipProcessor, data, dsData);
    }
    this.bindEvents();
    try {
      this.dataLayer.getDataLayerContainer().addLayer(this.layer);
      this.editModeUpdated();
    } catch (e) {
      console.warn(e);
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
            this.removeDataItem().subscribe(
              () => this.dataLayer.removeItem(this.data.entityId)
            );
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

  public dragModeUpdated() {
    if (this.dataLayer.isEditMode() && !this.selected) {
      if (this.dataLayer.allowDrag()) {
        this.disableDrag();
        this.enableDrag();
        this.addItemClass('tb-draggable');
      } else {
        this.disableDrag();
        this.removeItemClass('tb-draggable');
      }
    }
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

  public isEditing() {
    return false;
  }

  protected bindEvents(): void {
    if (this.dataLayer.isSelectable()) {
      this.layer.on('click', () => {
        if (!this.isEditing()) {
          this.dataLayer.getMap().selectItem(this);
        }
      });
    }
    const clickAction = this.settings.click;
    if (clickAction && clickAction.type !== WidgetActionType.doNothing) {
      this.layer.on('click', (event) => {
        this.dataLayer.getMap().dataItemClick(event.originalEvent, clickAction, this.data);
      });
    }
  }

  protected enableEdit(): void {
    if (this.dataLayer.isHoverable()) {
      this.addItemClass('tb-hoverable');
    }
    if (this.dataLayer.allowDrag()) {
      this.disableDrag();
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

  protected updateTooltip(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]) {
    if (this.settings.tooltip.show) {
      updateTooltip(this.dataLayer.getMap(), this.tooltip,
        this.settings.tooltip, this.dataLayer.dataLayerTooltipProcessor, data, dsData);
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

  protected abstract create(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L;

  protected abstract doUpdate(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void;

  protected abstract doInvalidateCoordinates(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): void;

  protected abstract unbindLabel(): void;

  protected abstract bindLabel(content: L.Content): void;

  protected abstract addItemClass(clazz: string): void;

  protected abstract removeItemClass(clazz: string): void;

  protected abstract enableDrag(): void;

  protected abstract disableDrag(): void;

  protected abstract updateBubblingMouseEvents(): void;

  protected abstract removeDataItemTitle(): string;

  protected abstract removeDataItem(): Observable<any>;

}

export interface UnplacedMapDataItem {
  entity: FormattedData<TbMapDatasource>;
  dataLayer: TbLatestMapDataLayer;
}

export abstract class TbLatestMapDataLayer<S extends MapDataLayerSettings = MapDataLayerSettings,
  D extends TbLatestMapDataLayer<S,D> = any, L extends L.Layer = L.Layer> extends TbMapDataLayer<S, TbLatestDataLayerItem<S,D,L>> implements L.TB.DataLayer {

  protected addEnabled = false;
  protected dragEnabled = false;
  protected editEnabled = false;
  protected removeEnabled = false;

  protected editable = false;
  protected selectable = false;
  protected hoverable = false;

  private editMode = false;

  private unplacedItems: UnplacedMapDataItem[] = [];

  protected constructor(map: TbMap<any>,
                        inputSettings: S) {
    super(map, inputSettings);
    if (this.settings.edit?.enabledActions) {
      this.addEnabled = this.settings.edit.enabledActions.includes(DataLayerEditAction.add);
      this.dragEnabled = this.settings.edit.enabledActions.includes(DataLayerEditAction.move);
      this.editEnabled = this.settings.edit.enabledActions.includes(DataLayerEditAction.edit);
      this.removeEnabled = this.settings.edit.enabledActions.includes(DataLayerEditAction.remove);

      this.editable = this.addEnabled || this.dragEnabled || this.editEnabled || this.removeEnabled;
      this.selectable = this.removeEnabled || this.editEnabled;
      this.hoverable = this.selectable || this.dragEnabled;
    }
    this.snappable = this.settings.edit?.snappable;
    this.enableEditMode();
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

  public allowDrag(): boolean {
    return this.dragEnabled && (!this.map.useDragModeButton() || this.map.dragModeEnabled());
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

  public updateData(dsData: FormattedData<TbMapDatasource>[]) {
    this.unplacedItems.length = 0;
    const layerData = dsData.filter(d => d.$datasource.mapDataIds.includes(this.mapDataId));
    const toDelete = new Set(Array.from(this.layerItems.keys()));
    const updatedItems: TbLatestDataLayerItem<S,D,L>[] = [];
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
      this.removeItem(key);
    });
    if (updatedItems.length) {
      this.layerItemsUpdated(updatedItems);
    }
  }

  public hasUnplacedItems(): boolean {
    return !!this.unplacedItems.length;
  }

  public getUnplacedItems(): UnplacedMapDataItem[] {
    return this.prepareUnplacedItems();
  }

  public enableEditMode() {
    if (this.editable) {
      if (!this.editMode) {
        this.editMode = true;
        this.updateItemsEditMode();
      }
    }
  }

  public disableEditMode() {
    if (this.editMode) {
      this.editMode = false;
      this.updateItemsEditMode();
    }
  }

  public dragModeUpdated() {
    this.updateItemsDragMode();
  }

  protected createDataLayerContainer(): L.FeatureGroup {
    return L.featureGroup([], {snapIgnore: !this.settings.edit?.snappable});
  }

  protected onDataLayerEnabled(): void {
    this.updateItemsEditMode();
  }

  protected onDataLayerDisabled(): void {
    for (const item of this.layerItems) {
      if (item[1].isSelected()) {
        this.getMap().deselectItem(false, true);
        break;
      }
    }
  }

  protected createItemFromUnplaced(unplacedItem: UnplacedMapDataItem): void {
    const index = this.unplacedItems.indexOf(unplacedItem);
    if (index > -1) {
      this.unplacedItems.splice(index, 1);
      const layerItem = this.createLayerItem(unplacedItem.entity, this.map.getData());
      this.layerItems.set(unplacedItem.entity.entityId, layerItem);
      this.map.enabledDataLayersUpdated();
    }
  }

  protected layerItemsUpdated(_updatedItems: TbLatestDataLayerItem<S,D,L>[]): void {
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

  private updateItemsEditMode() {
    this.layerItems.forEach(item => item.editModeUpdated());
  }

  private updateItemsDragMode() {
    this.layerItems.forEach(item => item.dragModeUpdated());
  }

  public abstract placeItem(item: UnplacedMapDataItem, layer: L.Layer): void;

  protected abstract isValidLayerData(layerData: FormattedData<TbMapDatasource>): boolean;

  protected abstract createLayerItem(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): TbLatestDataLayerItem<S,D,L>;

}
