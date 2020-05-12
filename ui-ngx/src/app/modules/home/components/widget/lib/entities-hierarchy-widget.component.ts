///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetAction, WidgetContext } from '@home/models/widget-component.models';
import { DatasourceData, DatasourceType, WidgetConfig, widgetType } from '@shared/models/widget.models';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { UtilsService } from '@core/services/utils.service';
import cssjs from '@core/css/css';
import { forkJoin, fromEvent, Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, mergeMap, tap } from 'rxjs/operators';
import { constructTableCssString } from '@home/components/widget/lib/table-widget.models';
import { Overlay } from '@angular/cdk/overlay';
import {
  LoadNodesCallback,
  NavTreeEditCallbacks,
  NodeSearchCallback,
  NodeSelectedCallback,
  NodesInsertedCallback
} from '@shared/components/nav-tree.component';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { deepClone, hashCode } from '@core/utils';
import {
  defaultNodeIconFunction,
  defaultNodeOpenedFunction,
  defaultNodeRelationQueryFunction,
  defaultNodesSortFunction,
  EntitiesHierarchyWidgetSettings,
  HierarchyNavTreeNode,
  HierarchyNodeContext,
  HierarchyNodeDatasource,
  iconUrlHtml,
  loadNodeCtxFunction,
  materialIconHtml,
  NodeDisabledFunction,
  NodeHasChildrenFunction,
  NodeIconFunction,
  NodeOpenedFunction,
  NodeRelationQueryFunction,
  NodesSortFunction,
  NodeTextFunction
} from '@home/components/widget/lib/entities-hierarchy-widget.models';
import { EntityService } from '@core/http/entity.service';
import { EntityRelationsQuery, EntitySearchDirection } from '@shared/models/relation.models';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
  selector: 'tb-entities-hierarchy-widget',
  templateUrl: './entities-hierarchy-widget.component.html',
  styleUrls: ['./entities-hierarchy-widget.component.scss']
})
export class EntitiesHierarchyWidgetComponent extends PageComponent implements OnInit, AfterViewInit {

  @Input()
  ctx: WidgetContext;

  @ViewChild('searchInput') searchInputField: ElementRef;

  public toastTargetId = 'entities-hierarchy-' + this.utils.guid();

  public textSearchMode = false;
  public textSearch = null;

  public nodeEditCallbacks: NavTreeEditCallbacks = {};

  private settings: EntitiesHierarchyWidgetSettings;
  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;
  private datasources: Array<HierarchyNodeDatasource>;

  private nodesMap: {[nodeId: string]: HierarchyNavTreeNode} = {};
  private pendingUpdateNodeTasks: {[nodeId: string]: () => void} = {};
  private nodeIdCounter = 0;

  private nodeRelationQueryFunction: NodeRelationQueryFunction;
  private nodeIconFunction: NodeIconFunction;
  private nodeTextFunction: NodeTextFunction;
  private nodeDisabledFunction: NodeDisabledFunction;
  private nodeOpenedFunction: NodeOpenedFunction;
  private nodeHasChildrenFunction: NodeHasChildrenFunction;
  private nodesSortFunction: NodesSortFunction;

  private searchAction: WidgetAction = {
    name: 'action.search',
    show: true,
    icon: 'search',
    onAction: () => {
      this.enterFilterMode();
    }
  };


  constructor(protected store: Store<AppState>,
              private elementRef: ElementRef,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private utils: UtilsService,
              private entityService: EntityService,
              private entityRelationService: EntityRelationService) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.entitiesHierarchyWidget = this;
    this.settings = this.ctx.settings;
    this.widgetConfig = this.ctx.widgetConfig;
    this.subscription = this.ctx.defaultSubscription;
    this.datasources = this.subscription.datasources as Array<HierarchyNodeDatasource>;
    this.initializeConfig();
    this.ctx.updateWidgetParams();
  }

  ngAfterViewInit(): void {
    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.updateSearchNodes();
        })
      )
      .subscribe();
  }

  public onDataUpdated() {
    this.updateNodeData(this.subscription.data);
  }

  private initializeConfig() {
    this.ctx.widgetActions = [this.searchAction];

    const testNodeCtx: HierarchyNodeContext = {
      entity: {
        id: {
          entityType: EntityType.DEVICE,
          id: '123'
        },
        name: 'TEST DEV1'
      },
      data: {},
      level: 2
    };
    const parentNodeCtx = deepClone(testNodeCtx);
    parentNodeCtx.level = 1;
    testNodeCtx.parentNodeCtx = parentNodeCtx;

    this.nodeRelationQueryFunction = loadNodeCtxFunction(this.settings.nodeRelationQueryFunction, 'nodeCtx', testNodeCtx);
    this.nodeIconFunction = loadNodeCtxFunction(this.settings.nodeIconFunction, 'nodeCtx', testNodeCtx);
    this.nodeTextFunction = loadNodeCtxFunction(this.settings.nodeTextFunction, 'nodeCtx', testNodeCtx);
    this.nodeDisabledFunction = loadNodeCtxFunction(this.settings.nodeDisabledFunction, 'nodeCtx', testNodeCtx);
    this.nodeOpenedFunction = loadNodeCtxFunction(this.settings.nodeOpenedFunction, 'nodeCtx', testNodeCtx);
    this.nodeHasChildrenFunction = loadNodeCtxFunction(this.settings.nodeHasChildrenFunction, 'nodeCtx', testNodeCtx);

    const testNodeCtx2 = deepClone(testNodeCtx);
    testNodeCtx2.entity.name = 'TEST DEV2';

    this.nodesSortFunction = loadNodeCtxFunction(this.settings.nodesSortFunction, 'nodeCtx1,nodeCtx2', testNodeCtx, testNodeCtx2);

    this.nodeRelationQueryFunction = this.nodeRelationQueryFunction || defaultNodeRelationQueryFunction;
    this.nodeIconFunction = this.nodeIconFunction || defaultNodeIconFunction;
    this.nodeTextFunction = this.nodeTextFunction || ((nodeCtx) => nodeCtx.entity.name);
    this.nodeDisabledFunction = this.nodeDisabledFunction || (() => false);
    this.nodeOpenedFunction = this.nodeOpenedFunction || defaultNodeOpenedFunction;
    this.nodeHasChildrenFunction = this.nodeHasChildrenFunction || (() => true);
    this.nodesSortFunction = this.nodesSortFunction || defaultNodesSortFunction;

    const cssString = constructTableCssString(this.widgetConfig);
    const cssParser = new cssjs();
    cssParser.testMode = false;
    const namespace = 'entities-hierarchy-' + hashCode(cssString);
    cssParser.cssPreviewNamespace = namespace;
    cssParser.createStyleElement(namespace, cssString);
    $(this.elementRef.nativeElement).addClass(namespace);
  }

  private enterFilterMode() {
    this.textSearchMode = true;
    this.textSearch = '';
    this.ctx.hideTitlePanel = true;
    this.ctx.detectChanges(true);
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.textSearch = null;
    this.updateSearchNodes();
    this.ctx.hideTitlePanel = false;
    this.ctx.detectChanges(true);
  }

  private updateSearchNodes() {
    if (this.textSearch != null) {
      this.nodeEditCallbacks.search(this.textSearch);
    } else {
      this.nodeEditCallbacks.clearSearch();
    }
  }

  private updateNodeData(subscriptionData: Array<DatasourceData>) {
    const affectedNodes: string[] = [];
    if (subscriptionData) {
      subscriptionData.forEach((datasourceData) => {
        const datasource = datasourceData.datasource as HierarchyNodeDatasource;
        if (datasource.nodeId) {
          const node = this.nodesMap[datasource.nodeId];
          const key = datasourceData.dataKey.label;
          let value;
          if (datasourceData.data && datasourceData.data.length) {
            value = datasourceData.data[0][1];
          }
          if (node.data.nodeCtx.data[key] !== value) {
            if (affectedNodes.indexOf(datasource.nodeId) === -1) {
              affectedNodes.push(datasource.nodeId);
            }
            node.data.nodeCtx.data[key] = value;
          }
        }
      });
    }
    affectedNodes.forEach((nodeId) => {
      const node: HierarchyNavTreeNode = this.nodeEditCallbacks.getNode(nodeId);
      if (node) {
        this.updateNodeStyle(this.nodesMap[nodeId]);
      } else {
        this.pendingUpdateNodeTasks[nodeId] = () => {
          this.updateNodeStyle(this.nodesMap[nodeId]);
        };
      }
    });
  }

  public loadNodes: LoadNodesCallback = (node, cb) => {
    if (node.id === '#') {
      const tasks: Observable<HierarchyNavTreeNode>[] = [];
      this.datasources.forEach((datasource) => {
        tasks.push(this.datasourceToNode(datasource));
      });
      forkJoin(tasks).subscribe((nodes) => {
        cb(this.prepareNodes(nodes));
        this.updateNodeData(this.subscription.data);
      });
    } else {
      if (node.data && node.data.nodeCtx.entity && node.data.nodeCtx.entity.id && node.data.nodeCtx.entity.id.entityType !== 'function') {
        const relationQuery = this.prepareNodeRelationQuery(node.data.nodeCtx);
        this.entityRelationService.findByQuery(relationQuery, {ignoreErrors: true, ignoreLoading: true}).subscribe(
          (entityRelations) => {
            if (entityRelations.length) {
              const tasks: Observable<HierarchyNavTreeNode>[] = [];
              entityRelations.forEach((relation) => {
                const targetId = relationQuery.parameters.direction === EntitySearchDirection.FROM ? relation.to : relation.from;
                tasks.push(this.entityIdToNode(targetId.entityType as EntityType, targetId.id, node.data.datasource, node.data.nodeCtx));
              });
              forkJoin(tasks).subscribe((nodes) => {
                cb(this.prepareNodes(nodes));
              });
            } else {
              cb([]);
            }
          },
          (error) => {
            let errorText = 'Failed to get relations!';
            if (error && error.status === 400) {
              errorText = 'Invalid relations query returned by \'Node relations query function\'! Please check widget configuration!';
            }
            this.showError(errorText);
          }
        );
      } else {
        cb([]);
      }
    }
  }

  public onNodeSelected: NodeSelectedCallback = (node, event) => {
    let nodeId;
    if (!node) {
      nodeId = -1;
    } else {
      nodeId = node.id;
    }
    if (nodeId !== -1) {
      const selectedNode = this.nodesMap[nodeId];
      if (selectedNode) {
        const descriptors = this.ctx.actionsApi.getActionDescriptors('nodeSelected');
        if (descriptors.length) {
          const entity = selectedNode.data.nodeCtx.entity;
          this.ctx.actionsApi.handleWidgetAction(event, descriptors[0], entity.id, entity.name, { nodeCtx: selectedNode.data.nodeCtx });
        }
      }
    }
  }

  public onNodesInserted: NodesInsertedCallback = (nodes, parent) => {
    if (nodes) {
      nodes.forEach((nodeId) => {
        const task = this.pendingUpdateNodeTasks[nodeId];
        if (task) {
          task();
          delete this.pendingUpdateNodeTasks[nodeId];
        }
      });
    }
  }

  public searchCallback: NodeSearchCallback = (searchText, node) => {
    const theNode = this.nodesMap[node.id];
    if (theNode && theNode.data.searchText) {
      return theNode.data.searchText.includes(searchText.toLowerCase());
    }
    return false;
  }

  private updateNodeStyle(node: HierarchyNavTreeNode) {
    const newText = this.prepareNodeText(node);
    if (node.text !== newText) {
      node.text = newText;
      this.nodeEditCallbacks.updateNode(node.id, node.text);
    }
    const newDisabled = this.nodeDisabledFunction(node.data.nodeCtx);
    if (node.state.disabled !== newDisabled) {
      node.state.disabled = newDisabled;
      if (node.state.disabled) {
        this.nodeEditCallbacks.disableNode(node.id);
      } else {
        this.nodeEditCallbacks.enableNode(node.id);
      }
    }
    const newHasChildren = this.nodeHasChildrenFunction(node.data.nodeCtx);
    if (node.children !== newHasChildren) {
      node.children = newHasChildren;
      this.nodeEditCallbacks.setNodeHasChildren(node.id, node.children);
    }
  }

  private showError(errorText: string) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: errorText,
        type: 'error',
        target: this.toastTargetId,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  private prepareNodes(nodes: HierarchyNavTreeNode[]): HierarchyNavTreeNode[] {
    nodes = nodes.filter((node) => node !== null);
    nodes.sort((node1, node2) => this.nodesSortFunction(node1.data.nodeCtx, node2.data.nodeCtx));
    return nodes;
  }

  private prepareNodeText(node: HierarchyNavTreeNode): string {
    const nodeIcon = this.prepareNodeIcon(node.data.nodeCtx);
    const nodeText = this.nodeTextFunction(node.data.nodeCtx);
    node.data.searchText = nodeText ? nodeText.replace(/<[^>]+>/g, '').toLowerCase() : '';
    return nodeIcon + nodeText;
  }

  private prepareNodeIcon(nodeCtx: HierarchyNodeContext): string {
    let iconInfo = this.nodeIconFunction(nodeCtx);
    if (iconInfo) {
      if (iconInfo === 'default') {
        iconInfo = defaultNodeIconFunction(nodeCtx);
      }
      if (iconInfo && iconInfo !== 'default' && (iconInfo.iconUrl || iconInfo.materialIcon)) {
        if (iconInfo.materialIcon) {
          return materialIconHtml(iconInfo.materialIcon);
        } else {
          return iconUrlHtml(iconInfo.iconUrl);
        }
      } else {
        return '';
      }
    } else {
      return '';
    }
  }

  private datasourceToNode(datasource: HierarchyNodeDatasource, parentNodeCtx?: HierarchyNodeContext): Observable<HierarchyNavTreeNode> {
    return this.resolveEntity(datasource).pipe(
      map(entity => {
        if (entity !== null) {
          const node: HierarchyNavTreeNode = {
            id: (++this.nodeIdCounter) + ''
          };
          this.nodesMap[node.id] = node;
          datasource.nodeId = node.id;
          node.icon = false;
          const nodeCtx: HierarchyNodeContext = {
            parentNodeCtx,
            entity,
            data: {}
          };
          nodeCtx.level = parentNodeCtx ? parentNodeCtx.level + 1 : 1;
          node.data = {
            datasource,
            nodeCtx
          };
          node.state = {
            disabled: this.nodeDisabledFunction(node.data.nodeCtx),
            opened: this.nodeOpenedFunction(node.data.nodeCtx)
          };
          node.text = this.prepareNodeText(node);
          node.children = this.nodeHasChildrenFunction(node.data.nodeCtx);
          return node;
        } else {
          return null;
        }
      })
    );
  }

  private entityIdToNode(entityType: EntityType, entityId: string,
                         parentDatasource: HierarchyNodeDatasource,
                         parentNodeCtx: HierarchyNodeContext): Observable<HierarchyNavTreeNode> {
    const datasource = {
      dataKeys: parentDatasource.dataKeys,
      type: DatasourceType.entity,
      entityType,
      entityId
    } as HierarchyNodeDatasource;
    return this.datasourceToNode(datasource, parentNodeCtx).pipe(
      mergeMap((node) => {
        if (node != null) {
          const subscriptionOptions: WidgetSubscriptionOptions = {
            type: widgetType.latest,
            datasources: [datasource],
            callbacks: {
              onDataUpdated: subscription => {
                this.updateNodeData(subscription.data);
              }
            }
          };
          return this.ctx.subscriptionApi.
                      createSubscription(subscriptionOptions, true).pipe(
                        map(() => node));
        } else {
          return of(node);
        }
      })
    );
  }

  private resolveEntity(datasource: HierarchyNodeDatasource): Observable<BaseData<EntityId>> {
    if (datasource.type === DatasourceType.function) {
      const entity = {
        id: {
          entityType: 'function'
        },
        name: datasource.name
      };
      return of(entity as BaseData<EntityId>);
    } else {
      return this.entityService.getEntity(datasource.entityType, datasource.entityId, {ignoreLoading: true}).pipe(
        catchError(err => of(null))
      );
    }
  }

  private prepareNodeRelationQuery(nodeCtx: HierarchyNodeContext): EntityRelationsQuery {
    let relationQuery = this.nodeRelationQueryFunction(nodeCtx);
    if (relationQuery && relationQuery === 'default') {
      relationQuery = defaultNodeRelationQueryFunction(nodeCtx);
    }
    return relationQuery as EntityRelationsQuery;
  }
}
