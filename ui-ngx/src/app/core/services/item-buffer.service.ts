///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { Dashboard, DashboardLayoutId } from '@app/shared/models/dashboard.models';
import { AliasesInfo, EntityAlias, EntityAliases, EntityAliasInfo } from '@shared/models/alias.models';
import { DatasourceType, Widget, WidgetPosition, WidgetSize } from '@shared/models/widget.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { deepClone, isEqual } from '@core/utils';
import { UtilsService } from '@core/services/utils.service';
import { Observable, of, throwError } from 'rxjs';
import { map } from 'rxjs/operators';
import { FcRuleNode, ruleNodeTypeDescriptors } from '@shared/models/rule-node.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainImport } from '@shared/models/rule-chain.models';
import { Filter, FilterInfo, Filters, FiltersInfo } from '@shared/models/query/query.models';

const WIDGET_ITEM = 'widget_item';
const WIDGET_REFERENCE = 'widget_reference';
const RULE_NODES = 'rule_nodes';
const RULE_CHAIN_IMPORT = 'rule_chain_import';

export interface WidgetItem {
  widget: Widget;
  aliasesInfo: AliasesInfo;
  filtersInfo: FiltersInfo;
  originalSize: WidgetSize;
  originalColumns: number;
}

export interface WidgetReference {
  dashboardId: string;
  sourceState: string;
  sourceLayout: DashboardLayoutId;
  widgetId: string;
  originalSize: WidgetSize;
  originalColumns: number;
}

export interface RuleNodeConnection {
  isInputSource: boolean;
  fromIndex: number;
  toIndex: number;
  label: string;
  labels: string[];
}

export interface RuleNodesReference {
  nodes: FcRuleNode[];
  connections: RuleNodeConnection[];
  originX?: number;
  originY?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ItemBufferService {

  private namespace = 'tbBufferStore';
  private delimiter = '.';

  constructor(private dashboardUtils: DashboardUtilsService,
              private ruleChainService: RuleChainService,
              private utils: UtilsService) {}

  public prepareWidgetItem(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId, widget: Widget): WidgetItem {
    const aliasesInfo: AliasesInfo = {
      datasourceAliases: {},
      targetDeviceAliases: {}
    };
    const filtersInfo: FiltersInfo = {
      datasourceFilters: {}
    };
    const originalColumns = this.getOriginalColumns(dashboard, sourceState, sourceLayout);
    const originalSize = this.getOriginalSize(dashboard, sourceState, sourceLayout, widget);
    if (widget.config && dashboard.configuration
      && dashboard.configuration.entityAliases) {
      let entityAlias: EntityAlias;
      if (widget.config.datasources) {
        for (let i = 0; i < widget.config.datasources.length; i++) {
          const datasource = widget.config.datasources[i];
          if ((datasource.type === DatasourceType.entity || datasource.type === DatasourceType.entityCount) && datasource.entityAliasId) {
            entityAlias = dashboard.configuration.entityAliases[datasource.entityAliasId];
            if (entityAlias) {
              aliasesInfo.datasourceAliases[i] = this.prepareAliasInfo(entityAlias);
            }
          }
        }
      }
      if (widget.config.targetDeviceAliasIds) {
        for (let i = 0; i < widget.config.targetDeviceAliasIds.length; i++) {
          const targetDeviceAliasId = widget.config.targetDeviceAliasIds[i];
          if (targetDeviceAliasId) {
            entityAlias = dashboard.configuration.entityAliases[targetDeviceAliasId];
            if (entityAlias) {
              aliasesInfo.targetDeviceAliases[i] = this.prepareAliasInfo(entityAlias);
            }
          }
        }
      }
    }
    if (widget.config && dashboard.configuration
      && dashboard.configuration.filters) {
      let filter: Filter;
      if (widget.config.datasources) {
        for (let i = 0; i < widget.config.datasources.length; i++) {
          const datasource = widget.config.datasources[i];
          if ((datasource.type === DatasourceType.entity || datasource.type === DatasourceType.entityCount) && datasource.filterId) {
            filter = dashboard.configuration.filters[datasource.filterId];
            if (filter) {
              filtersInfo.datasourceFilters[i] = this.prepareFilterInfo(filter);
            }
          }
        }
      }
    }
    return {
      widget,
      aliasesInfo,
      filtersInfo,
      originalSize,
      originalColumns
    };
  }

  public copyWidget(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId, widget: Widget): void {
    const widgetItem = this.prepareWidgetItem(dashboard, sourceState, sourceLayout, widget);
    this.storeSet(WIDGET_ITEM, widgetItem);
  }

  public copyWidgetReference(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId, widget: Widget): void {
    const widgetReference = this.prepareWidgetReference(dashboard, sourceState, sourceLayout, widget);
    this.storeSet(WIDGET_REFERENCE, widgetReference);
  }

  public hasWidget(): boolean {
    return this.storeHas(WIDGET_ITEM);
  }

  public canPasteWidgetReference(dashboard: Dashboard, state: string, layout: DashboardLayoutId): boolean {
    const widgetReference: WidgetReference = this.storeGet(WIDGET_REFERENCE);
    if (widgetReference) {
      if (widgetReference.dashboardId === dashboard.id.id) {
        if ((widgetReference.sourceState !== state || widgetReference.sourceLayout !== layout)
          && dashboard.configuration.widgets[widgetReference.widgetId]) {
          return true;
        }
      }
    }
    return false;
  }

  public pasteWidget(targetDashboard: Dashboard, targetState: string,
                     targetLayout: DashboardLayoutId, position: WidgetPosition,
                     onAliasesUpdateFunction: () => void,
                     onFiltersUpdateFunction: () => void): Observable<Widget> {
    const widgetItem: WidgetItem = this.storeGet(WIDGET_ITEM);
    if (widgetItem) {
      const widget = widgetItem.widget;
      const aliasesInfo = widgetItem.aliasesInfo;
      const filtersInfo = widgetItem.filtersInfo;
      const originalColumns = widgetItem.originalColumns;
      const originalSize = widgetItem.originalSize;
      let targetRow = -1;
      let targetColumn = -1;
      if (position) {
        targetRow = position.row;
        targetColumn = position.column;
      }
      widget.id = this.utils.guid();
      return this.addWidgetToDashboard(targetDashboard, targetState,
                                targetLayout, widget, aliasesInfo, filtersInfo,
                                onAliasesUpdateFunction, onFiltersUpdateFunction,
                                originalColumns, originalSize, targetRow, targetColumn).pipe(
        map(() => widget)
      );
    } else {
      return throwError('Failed to read widget from buffer!');
    }
  }

  public pasteWidgetReference(targetDashboard: Dashboard, targetState: string,
                              targetLayout: DashboardLayoutId, position: WidgetPosition): Observable<Widget> {
    const widgetReference: WidgetReference = this.storeGet(WIDGET_REFERENCE);
    if (widgetReference) {
      const widget = targetDashboard.configuration.widgets[widgetReference.widgetId];
      if (widget) {
        const originalColumns = widgetReference.originalColumns;
        const originalSize = widgetReference.originalSize;
        let targetRow = -1;
        let targetColumn = -1;
        if (position) {
          targetRow = position.row;
          targetColumn = position.column;
        }
        return this.addWidgetToDashboard(targetDashboard, targetState,
          targetLayout, widget, null,
          null, null, null, originalColumns,
          originalSize, targetRow, targetColumn).pipe(
          map(() => widget)
        );
      } else {
        return throwError('Failed to read widget reference from buffer!');
      }
    } else {
      return throwError('Failed to read widget reference from buffer!');
    }
  }

  public addWidgetToDashboard(dashboard: Dashboard, targetState: string,
                              targetLayout: DashboardLayoutId, widget: Widget,
                              aliasesInfo: AliasesInfo,
                              filtersInfo: FiltersInfo,
                              onAliasesUpdateFunction: () => void,
                              onFiltersUpdateFunction: () => void,
                              originalColumns: number,
                              originalSize: WidgetSize,
                              row: number,
                              column: number): Observable<Dashboard> {
    let theDashboard: Dashboard;
    if (dashboard) {
      theDashboard = dashboard;
    } else {
      theDashboard = {};
    }
    theDashboard = this.dashboardUtils.validateAndUpdateDashboard(theDashboard);
    let callAliasUpdateFunction = false;
    let callFilterUpdateFunction = false;
    if (aliasesInfo) {
      const newEntityAliases = this.updateAliases(theDashboard, widget, aliasesInfo);
      const aliasesUpdated = !isEqual(newEntityAliases, theDashboard.configuration.entityAliases);
      if (aliasesUpdated) {
        theDashboard.configuration.entityAliases = newEntityAliases;
        if (onAliasesUpdateFunction) {
          callAliasUpdateFunction = true;
        }
      }
    }
    if (filtersInfo) {
      const newFilters = this.updateFilters(theDashboard, widget, filtersInfo);
      const filtersUpdated = !isEqual(newFilters, theDashboard.configuration.filters);
      if (filtersUpdated) {
        theDashboard.configuration.filters = newFilters;
        if (onFiltersUpdateFunction) {
          callFilterUpdateFunction = true;
        }
      }
    }
    this.dashboardUtils.addWidgetToLayout(theDashboard, targetState, targetLayout, widget,
                                          originalColumns, originalSize, row, column);
    if (callAliasUpdateFunction) {
      onAliasesUpdateFunction();
    }
    if (callFilterUpdateFunction) {
      onFiltersUpdateFunction();
    }
    return of(theDashboard);
  }

  public copyRuleNodes(nodes: FcRuleNode[], connections: RuleNodeConnection[]) {
    const ruleNodes: RuleNodesReference = {
      nodes: [],
      connections: []
    };
    let top = -1;
    let left = -1;
    let bottom = -1;
    let right = -1;
    for (let i = 0; i < nodes.length; i++) {
      const origNode = nodes[i];
      const node: FcRuleNode = {
        id: '',
        connectors: [],
        additionalInfo: origNode.additionalInfo,
        configuration: origNode.configuration,
        debugMode: origNode.debugMode,
        x: origNode.x,
        y: origNode.y,
        name: origNode.name,
        componentClazz: origNode.component.clazz,
        ruleChainType: origNode.ruleChainType
      };
      if (origNode.targetRuleChainId) {
        node.targetRuleChainId = origNode.targetRuleChainId;
      }
      if (origNode.error) {
        node.error = origNode.error;
      }
      ruleNodes.nodes.push(node);
      if (i === 0) {
        top = node.y;
        left = node.x;
        bottom = node.y + 50;
        right = node.x + 170;
      } else {
        top = Math.min(top, node.y);
        left = Math.min(left, node.x);
        bottom = Math.max(bottom, node.y + 50);
        right = Math.max(right, node.x + 170);
      }
    }
    ruleNodes.originX = left + (right - left) / 2;
    ruleNodes.originY = top + (bottom - top) / 2;
    connections.forEach(connection => {
      ruleNodes.connections.push(connection);
    });
    this.storeSet(RULE_NODES, ruleNodes);
  }

  public hasRuleNodes(): boolean {
    return this.storeHas(RULE_NODES);
  }

  public pasteRuleNodes(x: number, y: number): RuleNodesReference {
    const ruleNodes: RuleNodesReference = this.storeGet(RULE_NODES);
    if (ruleNodes) {
      const deltaX = x - ruleNodes.originX;
      const deltaY = y - ruleNodes.originY;
      for (const node of ruleNodes.nodes) {
        const component = this.ruleChainService.getRuleNodeComponentByClazz(node.ruleChainType, node.componentClazz);
        if (component) {
          let icon = ruleNodeTypeDescriptors.get(component.type).icon;
          let iconUrl: string = null;
          if (component.configurationDescriptor.nodeDefinition.icon) {
            icon = component.configurationDescriptor.nodeDefinition.icon;
          }
          if (component.configurationDescriptor.nodeDefinition.iconUrl) {
            iconUrl = component.configurationDescriptor.nodeDefinition.iconUrl;
          }
          delete node.componentClazz;
          node.component = component;
          node.nodeClass = ruleNodeTypeDescriptors.get(component.type).nodeClass;
          node.icon = icon;
          node.iconUrl = iconUrl;
          node.connectors = [];
          node.x = Math.round(node.x + deltaX);
          node.y = Math.round(node.y + deltaY);
        } else {
          return null;
        }
      }
      return ruleNodes;
    }
    return null;
  }

  public hasRuleChainImport(): boolean {
    return this.storeHas(RULE_CHAIN_IMPORT);
  }

  public storeRuleChainImport(ruleChainImport: RuleChainImport): void {
    this.storeSet(RULE_CHAIN_IMPORT, ruleChainImport);
  }

  public getRuleChainImport(): RuleChainImport {
    const ruleChainImport: RuleChainImport = this.storeGet(RULE_CHAIN_IMPORT);
    this.storeRemove(RULE_CHAIN_IMPORT);
    return ruleChainImport;
  }

  private getOriginalColumns(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId): number {
    let originalColumns = 24;
    let gridSettings = null;
    const state = dashboard.configuration.states[sourceState];
    const layoutCount = Object.keys(state.layouts).length;
    if (state) {
      const layout = state.layouts[sourceLayout];
      if (layout) {
        gridSettings = layout.gridSettings;

      }
    }
    if (gridSettings &&
      gridSettings.columns) {
      originalColumns = gridSettings.columns;
    }
    originalColumns = originalColumns * layoutCount;
    return originalColumns;
  }

  private getOriginalSize(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId, widget: Widget): WidgetSize {
    const layout = dashboard.configuration.states[sourceState].layouts[sourceLayout];
    const widgetLayout = layout.widgets[widget.id];
    return {
      sizeX: widgetLayout.sizeX,
      sizeY: widgetLayout.sizeY
    };
  }

  private prepareAliasInfo(entityAlias: EntityAlias): EntityAliasInfo {
    return {
      alias: entityAlias.alias,
      filter: entityAlias.filter
    };
  }

  private prepareFilterInfo(filter: Filter): FilterInfo {
    return {
      filter: filter.filter,
      keyFilters: filter.keyFilters,
      editable: filter.editable
    };
  }

  private prepareWidgetReference(dashboard: Dashboard, sourceState: string,
                                 sourceLayout: DashboardLayoutId, widget: Widget): WidgetReference {
    const originalColumns = this.getOriginalColumns(dashboard, sourceState, sourceLayout);
    const originalSize = this.getOriginalSize(dashboard, sourceState, sourceLayout, widget);
    return {
      dashboardId: dashboard.id.id,
      sourceState,
      sourceLayout,
      widgetId: widget.id,
      originalSize,
      originalColumns
    };
  }

  private updateAliases(dashboard: Dashboard, widget: Widget, aliasesInfo: AliasesInfo): EntityAliases {
    const entityAliases = deepClone(dashboard.configuration.entityAliases);
    let aliasInfo: EntityAliasInfo;
    let newAliasId: string;
    for (const datasourceIndexStr of Object.keys(aliasesInfo.datasourceAliases)) {
      const datasourceIndex = Number(datasourceIndexStr);
      aliasInfo = aliasesInfo.datasourceAliases[datasourceIndex];
      newAliasId = this.getEntityAliasId(entityAliases, aliasInfo);
      widget.config.datasources[datasourceIndex].entityAliasId = newAliasId;
    }
    for (const targetDeviceAliasIndexStr of Object.keys(aliasesInfo.targetDeviceAliases)) {
      const targetDeviceAliasIndex = Number(targetDeviceAliasIndexStr);
      aliasInfo = aliasesInfo.targetDeviceAliases[targetDeviceAliasIndex];
      newAliasId = this.getEntityAliasId(entityAliases, aliasInfo);
      widget.config.targetDeviceAliasIds[targetDeviceAliasIndex] = newAliasId;
    }
    return entityAliases;
  }

  private updateFilters(dashboard: Dashboard, widget: Widget, filtersInfo: FiltersInfo): Filters {
    const filters = deepClone(dashboard.configuration.filters);
    let filterInfo: FilterInfo;
    let newFilterId: string;
    for (const datasourceIndexStr of Object.keys(filtersInfo.datasourceFilters)) {
      const datasourceIndex = Number(datasourceIndexStr);
      filterInfo = filtersInfo.datasourceFilters[datasourceIndex];
      newFilterId = this.getFilterId(filters, filterInfo);
      widget.config.datasources[datasourceIndex].filterId = newFilterId;
    }
    return filters;
  }

  private isEntityAliasEqual(alias1: EntityAliasInfo, alias2: EntityAliasInfo): boolean {
    return isEqual(alias1.filter, alias2.filter);
  }

  private getEntityAliasId(entityAliases: EntityAliases, aliasInfo: EntityAliasInfo): string {
    let newAliasId: string;
    for (const aliasId of Object.keys(entityAliases)) {
      if (this.isEntityAliasEqual(entityAliases[aliasId], aliasInfo)) {
        newAliasId = aliasId;
        break;
      }
    }
    if (!newAliasId) {
      const newAliasName = this.createEntityAliasName(entityAliases, aliasInfo.alias);
      newAliasId = this.utils.guid();
      entityAliases[newAliasId] = {id: newAliasId, alias: newAliasName, filter: aliasInfo.filter};
    }
    return newAliasId;
  }

  private createEntityAliasName(entityAliases: EntityAliases, alias: string): string {
    let c = 0;
    let newAlias = alias;
    let unique = false;
    while (!unique) {
      unique = true;
      for (const entAliasId of Object.keys(entityAliases)) {
        const entAlias = entityAliases[entAliasId];
        if (newAlias === entAlias.alias) {
          c++;
          newAlias = alias + c;
          unique = false;
        }
      }
    }
    return newAlias;
  }

  private isFilterEqual(filter1: FilterInfo, filter2: FilterInfo): boolean {
    return isEqual(filter1.keyFilters, filter2.keyFilters);
  }

  private getFilterId(filters: Filters, filterInfo: FilterInfo): string {
    let newFilterId: string;
    for (const filterId of Object.keys(filters)) {
      if (this.isFilterEqual(filters[filterId], filterInfo)) {
        newFilterId = filterId;
        break;
      }
    }
    if (!newFilterId) {
      const newFilterName = this.createFilterName(filters, filterInfo.filter);
      newFilterId = this.utils.guid();
      filters[newFilterId] = {id: newFilterId, filter: newFilterName,
        keyFilters: filterInfo.keyFilters, editable: filterInfo.editable};
    }
    return newFilterId;
  }

  private createFilterName(filters: Filters, filter: string): string {
    let c = 0;
    let newFilter = filter;
    let unique = false;
    while (!unique) {
      unique = true;
      for (const entFilterId of Object.keys(filters)) {
        const entFilter = filters[entFilterId];
        if (newFilter === entFilter.filter) {
          c++;
          newFilter = filter + c;
          unique = false;
        }
      }
    }
    return newFilter;
  }

  private storeSet(key: string, elem: any) {
    localStorage.setItem(this.getNamespacedKey(key), JSON.stringify(elem));
  }

  private storeGet(key: string): any {
    let obj = null;
    const saved = localStorage.getItem(this.getNamespacedKey(key));
    try {
      if (typeof saved === 'undefined' || saved === 'undefined') {
        obj = undefined;
      } else {
        obj = JSON.parse(saved);
      }
    } catch (e) {
      this.storeRemove(key);
    }
    return obj;
  }

  private storeHas(key: string): boolean {
    const saved = localStorage.getItem(this.getNamespacedKey(key));
    return typeof saved !== 'undefined' && saved !== 'undefined' && saved !== null;
  }

  private storeRemove(key: string) {
    localStorage.removeItem(this.getNamespacedKey(key));
  }

  private getNamespacedKey(key: string): string {
    return [this.namespace, key].join(this.delimiter);
  }
}
