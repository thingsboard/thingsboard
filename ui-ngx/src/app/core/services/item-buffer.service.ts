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

import { Injectable } from '@angular/core';
import { BreakpointId, Dashboard, DashboardLayoutId } from '@app/shared/models/dashboard.models';
import { AliasesInfo, EntityAlias, EntityAliases, EntityAliasInfo, getEntityAliasId } from '@shared/models/alias.models';
import {
  Datasource,
  DatasourceType,
  TargetDeviceType,
  Widget,
  WidgetPosition,
  WidgetSize,
  widgetType
} from '@shared/models/widget.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { deepClone, isDefinedAndNotNull, isEqual } from '@core/utils';
import { UtilsService } from '@core/services/utils.service';
import { Observable, of, throwError } from 'rxjs';
import { map } from 'rxjs/operators';
import { FcRuleNode, ruleNodeTypeDescriptors } from '@shared/models/rule-node.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainImport } from '@shared/models/rule-chain.models';
import { Filter, FilterInfo, Filters, FiltersInfo, getFilterId } from '@shared/models/query/query.models';
import { findWidgetModelDefinition } from '@shared/models/widget/widget-model.definition';

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
  widgetExportInfo?: any;
}

export interface WidgetReference {
  dashboardId: string;
  sourceState: string;
  sourceLayout: DashboardLayoutId;
  widgetId: string;
  originalSize: WidgetSize;
  originalColumns: number;
  breakpoint: string;
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

  public prepareWidgetItem(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId,
                           widget: Widget, breakpoint: BreakpointId): WidgetItem {
    const aliasesInfo: AliasesInfo = {
      datasourceAliases: {},
      targetDeviceAlias: null
    };
    const filtersInfo: FiltersInfo = {
      datasourceFilters: {}
    };
    const originalColumns = this.dashboardUtils.getOriginalColumns(dashboard, sourceState, sourceLayout, breakpoint);
    const originalSize = this.dashboardUtils.getOriginalSize(dashboard, sourceState, sourceLayout, widget, breakpoint);
    const datasources: Datasource[] = widget.type === widgetType.alarm ? [widget.config.alarmSource] : widget.config.datasources;
    if (widget.config && dashboard.configuration
      && dashboard.configuration.entityAliases) {
      let entityAlias: EntityAlias;
      if (datasources) {
        for (let i = 0; i < datasources.length; i++) {
          const datasource = datasources[i];
          if ((datasource.type === DatasourceType.entity ||
            datasource.type === DatasourceType.entityCount ||
            datasource.type === DatasourceType.alarmCount) && datasource.entityAliasId) {
            entityAlias = dashboard.configuration.entityAliases[datasource.entityAliasId];
            if (entityAlias) {
              aliasesInfo.datasourceAliases[i] = this.prepareAliasInfo(entityAlias);
            }
          }
        }
      }
      if (widget.config.targetDevice?.type === TargetDeviceType.entity && widget.config.targetDevice.entityAliasId) {
        const targetDeviceAliasId = widget.config.targetDevice.entityAliasId;
        entityAlias = dashboard.configuration.entityAliases[targetDeviceAliasId];
        if (entityAlias) {
          aliasesInfo.targetDeviceAlias = this.prepareAliasInfo(entityAlias);
        }
      }
    }
    if (widget.config && dashboard.configuration
      && dashboard.configuration.filters) {
      let filter: Filter;
      if (datasources) {
        for (let i = 0; i < datasources.length; i++) {
          const datasource = datasources[i];
          if ((datasource.type === DatasourceType.entity ||
            datasource.type === DatasourceType.entityCount ||
            datasource.type === DatasourceType.alarmCount) && datasource.filterId) {
            filter = dashboard.configuration.filters[datasource.filterId];
            if (filter) {
              filtersInfo.datasourceFilters[i] = this.prepareFilterInfo(filter);
            }
          }
        }
      }
    }
    let widgetExportInfo: any;
    const exportDefinition = findWidgetModelDefinition(widget);
    if (exportDefinition) {
      widgetExportInfo = exportDefinition.prepareExportInfo(dashboard, widget);
    }
    return {
      widget,
      aliasesInfo,
      filtersInfo,
      originalSize,
      originalColumns,
      widgetExportInfo
    };
  }

  public copyWidget(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId, widget: Widget, breakpoint: BreakpointId) {
    const widgetItem = this.prepareWidgetItem(dashboard, sourceState, sourceLayout, widget, breakpoint);
    this.storeSet(WIDGET_ITEM, widgetItem);
  }

  public copyWidgetReference(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId,
                             widget: Widget, breakpoint: BreakpointId): void {
    const widgetReference = this.prepareWidgetReference(dashboard, sourceState, sourceLayout, widget, breakpoint);
    this.storeSet(WIDGET_REFERENCE, widgetReference);
  }

  public hasWidget(): boolean {
    return this.storeHas(WIDGET_ITEM);
  }

  public canPasteWidgetReference(dashboard: Dashboard, state: string, layout: DashboardLayoutId, breakpoint: string): boolean {
    const widgetReference: WidgetReference = this.storeGet(WIDGET_REFERENCE);
    if (widgetReference) {
      if (widgetReference.dashboardId === dashboard.id.id) {
        if ((widgetReference.sourceState !== state || widgetReference.sourceLayout !== layout || widgetReference.breakpoint !== breakpoint)
          && dashboard.configuration.widgets[widgetReference.widgetId]) {
          return true;
        }
      }
    }
    return false;
  }

  public pasteWidget(targetDashboard: Dashboard, targetState: string,
                     targetLayout: DashboardLayoutId,
                     breakpoint: string,
                     position: WidgetPosition,
                     onAliasesUpdateFunction: () => void,
                     onFiltersUpdateFunction: () => void): Observable<Widget> {
    const widgetItem: WidgetItem = this.storeGet(WIDGET_ITEM);
    if (widgetItem) {
      const widget = widgetItem.widget;
      const aliasesInfo = widgetItem.aliasesInfo;
      const filtersInfo = widgetItem.filtersInfo;
      const originalColumns = widgetItem.originalColumns;
      const originalSize = widgetItem.originalSize;
      const widgetExportInfo = widgetItem.widgetExportInfo;
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
                                originalColumns, originalSize, targetRow, targetColumn, breakpoint, widgetExportInfo).pipe(
        map(() => widget)
      );
    } else {
      return throwError('Failed to read widget from buffer!');
    }
  }

  public pasteWidgetReference(targetDashboard: Dashboard,
                              targetState: string,
                              targetLayout: DashboardLayoutId,
                              breakpoint: string,
                              position: WidgetPosition): Observable<Widget> {
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
          originalSize, targetRow, targetColumn, breakpoint).pipe(
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
                              column: number,
                              breakpoint = 'default',
                              widgetExportInfo?: any): Observable<Dashboard> {
    let theDashboard: Dashboard;
    if (dashboard) {
      theDashboard = dashboard;
    } else {
      theDashboard = {};
    }
    theDashboard = this.dashboardUtils.validateAndUpdateDashboard(theDashboard);
    let callAliasUpdateFunction = false;
    let callFilterUpdateFunction = false;
    let newEntityAliases: EntityAliases;
    let newFilters: Filters;
    const exportDefinition = findWidgetModelDefinition(widget);
    if (exportDefinition && widgetExportInfo || aliasesInfo) {
      newEntityAliases = deepClone(dashboard.configuration.entityAliases);
    }
    if (exportDefinition && widgetExportInfo || filtersInfo) {
      newFilters = deepClone(dashboard.configuration.filters);
    }
    if (aliasesInfo) {
      this.updateAliases(widget, newEntityAliases, aliasesInfo);
    }
    if (filtersInfo) {
      this.updateFilters(widget, newFilters, filtersInfo);
    }
    if (exportDefinition && widgetExportInfo) {
      exportDefinition.updateFromExportInfo(widget, newEntityAliases, newFilters, widgetExportInfo);
    }
    const aliasesUpdated = newEntityAliases && !isEqual(newEntityAliases, theDashboard.configuration.entityAliases);
    if (aliasesUpdated) {
      theDashboard.configuration.entityAliases = newEntityAliases;
      if (onAliasesUpdateFunction) {
        callAliasUpdateFunction = true;
      }
    }
    const filtersUpdated = newFilters && !isEqual(newFilters, theDashboard.configuration.filters);
    if (filtersUpdated) {
      theDashboard.configuration.filters = newFilters;
      if (onFiltersUpdateFunction) {
        callFilterUpdateFunction = true;
      }
    }

    this.dashboardUtils.addWidgetToLayout(theDashboard, targetState, targetLayout, widget,
                                          originalColumns, originalSize, row, column, breakpoint);
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
        debugSettings: {
          failuresEnabled: origNode.debugSettings?.failuresEnabled,
          allEnabled: origNode.debugSettings?.allEnabled || origNode.debugSettings?.allEnabledUntil > new Date().getTime(),
          allEnabledUntil: 0
        },
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
      if (isDefinedAndNotNull(origNode.singletonMode)) {
        node.singletonMode = origNode.singletonMode;
      }
      if (isDefinedAndNotNull(origNode.queueName)) {
        node.queueName = origNode.queueName;
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
                                 sourceLayout: DashboardLayoutId, widget: Widget, breakpoint: BreakpointId): WidgetReference {
    const originalColumns = this.dashboardUtils.getOriginalColumns(dashboard, sourceState, sourceLayout, breakpoint);
    const originalSize = this.dashboardUtils.getOriginalSize(dashboard, sourceState, sourceLayout, widget, breakpoint);
    return {
      dashboardId: dashboard.id.id,
      sourceState,
      sourceLayout,
      widgetId: widget.id,
      originalSize,
      originalColumns,
      breakpoint
    };
  }

  private updateAliases(widget: Widget, entityAliases: EntityAliases, aliasesInfo: AliasesInfo): void {
    let aliasInfo: EntityAliasInfo;
    let newAliasId: string;
    for (const datasourceIndexStr of Object.keys(aliasesInfo.datasourceAliases)) {
      const datasourceIndex = Number(datasourceIndexStr);
      aliasInfo = aliasesInfo.datasourceAliases[datasourceIndex];
      newAliasId = getEntityAliasId(entityAliases, aliasInfo);
      if (widget.type === widgetType.alarm) {
        widget.config.alarmSource.entityAliasId = newAliasId;
      } else {
        widget.config.datasources[datasourceIndex].entityAliasId = newAliasId;
      }
    }
    if (aliasesInfo.targetDeviceAlias) {
      aliasInfo = aliasesInfo.targetDeviceAlias;
      newAliasId = getEntityAliasId(entityAliases, aliasInfo);
      if (widget.config.targetDevice?.type !== TargetDeviceType.entity) {
        widget.config.targetDevice = {
          type: TargetDeviceType.entity
        };
      }
      widget.config.targetDevice.entityAliasId = newAliasId;
    }
  }

  private updateFilters(widget: Widget, filters: Filters, filtersInfo: FiltersInfo): void {
    let filterInfo: FilterInfo;
    let newFilterId: string;
    for (const datasourceIndexStr of Object.keys(filtersInfo.datasourceFilters)) {
      const datasourceIndex = Number(datasourceIndexStr);
      filterInfo = filtersInfo.datasourceFilters[datasourceIndex];
      newFilterId = getFilterId(filters, filterInfo);
      if (widget.type === widgetType.alarm) {
        widget.config.alarmSource.filterId = newFilterId;
      } else {
        widget.config.datasources[datasourceIndex].filterId = newFilterId;
      }
    }
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
