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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetContext } from '@home/models/widget-component.models';
import { Datasource, DatasourceType, WidgetConfig } from '@shared/models/widget.models';
import { IWidgetSubscription } from '@core/api/widget-api.models';
import { UtilsService } from '@core/services/utils.service';
import { LoadNodesCallback } from '@shared/components/nav-tree.component';
import { EntityType } from '@shared/models/entity-type.models';
import {
  EdgeGroupNodeData,
  edgeGroupsNodeText,
  edgeGroupsTypes,
  entityNodeText,
  EdgeOverviewNode,
  EntityNodeData,
  EntityNodeDatasource
} from '@home/components/widget/lib/edges-overview-widget.models';
import { EdgeService } from '@core/http/edge.service';
import { EntityService } from '@core/http/entity.service';
import { TranslateService } from '@ngx-translate/core';
import { PageLink } from '@shared/models/page/page-link';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { isDefined } from '@core/utils';

interface EdgesOverviewWidgetSettings {
  enableDefaultTitle: boolean;
}

@Component({
    selector: 'tb-edges-overview-widget',
    templateUrl: './edges-overview-widget.component.html',
    styleUrls: ['./edges-overview-widget.component.scss'],
    standalone: false
})
export class EdgesOverviewWidgetComponent extends PageComponent implements OnInit {

  @Input()
  ctx: WidgetContext;

  public toastTargetId = 'edges-overview-' + this.utils.guid();
  public customerTitle: string = null;
  public edgeIsDatasource: boolean = true;

  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;
  private datasources: Array<EntityNodeDatasource>;
  private settings: EdgesOverviewWidgetSettings;

  private nodeIdCounter = 0;

  constructor(protected store: Store<AppState>,
              private edgeService: EdgeService,
              private entityService: EntityService,
              private translateService: TranslateService,
              private utils: UtilsService,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.widgetConfig = this.ctx.widgetConfig;
    this.subscription = this.ctx.defaultSubscription;
    this.datasources = this.subscription.datasources as Array<EntityNodeDatasource>;
    this.settings = this.ctx.settings;
    this.initializeConfig();
    this.ctx.updateWidgetParams();
  }

  public loadNodes: LoadNodesCallback = (node, cb) => {
    const datasource: Datasource = this.datasources[0];
    if (node.id === '#' && datasource) {
      if (datasource.type === DatasourceType.entity && datasource.entity.id.entityType === EntityType.EDGE) {
        var selectedEdge: BaseData<EntityId> = datasource.entity;
        this.updateTitle(selectedEdge);
        this.getCustomerTitle(selectedEdge.id.id);
        cb(this.loadNodesForEdge(selectedEdge));
      } else if (datasource.type === DatasourceType.function) {
        cb(this.loadNodesForEdge(datasource.entity));
      } else {
        this.edgeIsDatasource = false;
        cb([]);
      }
    }
    else if (node.data && node.data.entity.id.entityType === EntityType.EDGE) {
      const edgeId = node.data.entity.id.id;
      const entityType = node.data.entityType;
      const pageLink = new PageLink(datasource.pageLink.pageSize);
      this.entityService.getAssignedToEdgeEntitiesByType(edgeId, entityType, pageLink).subscribe(
        (entities) => {
          if (entities.data.length > 0) {
            cb(this.entitiesToNodes(entities.data));
          } else {
            cb([]);
          }
        }
      );
    } else {
      cb([]);
    }
  }

  private loadNodesForEdge(entity: BaseData<HasId>): EdgeOverviewNode[] {
    const nodes: EdgeOverviewNode[] = [];
    const authUser = getCurrentAuthUser(this.store);
    var allowedGroupTypes: EntityType[] = edgeGroupsTypes;
    if (authUser.authority === Authority.CUSTOMER_USER) {
      allowedGroupTypes = edgeGroupsTypes.filter(type => type !== EntityType.RULE_CHAIN);
    }
    allowedGroupTypes.forEach((entityType) => {
      const node: EdgeOverviewNode = {
        id: (++this.nodeIdCounter)+'',
        icon: false,
        text: edgeGroupsNodeText(this.translateService, entityType),
        children: true,
        data: {
          entityType,
          entity,
          internalId: entity.id.id + '_' + entityType
        } as EdgeGroupNodeData
      };
      nodes.push(node);
    });
    return nodes;
  }

  private createEntityNode(entity: BaseData<HasId>): EdgeOverviewNode {
    return {
      id: (++this.nodeIdCounter)+'',
      icon: false,
      text: entityNodeText(entity),
      children: false,
      state: {
        disabled: false
      },
      data: {
        entity: entity,
        internalId: entity.id.id
      } as EntityNodeData
    } as EdgeOverviewNode;
  }

  private entitiesToNodes(entities: BaseData<HasId>[]): EdgeOverviewNode[] {
    const nodes: EdgeOverviewNode[] = [];
    if (entities) {
      entities.forEach((entity) => {
        const node = this.createEntityNode(entity);
        nodes.push(node);
      });
    }
    return nodes;
  }

  private getCustomerTitle(edgeId: string) {
    this.edgeService.getEdgeInfo(edgeId).subscribe(
      (edge) => {
        if (edge.customerTitle) {
          this.customerTitle = this.translateService.instant('edge.assigned-to-customer', {customerTitle: edge.customerTitle});
        } else {
          this.customerTitle = null;
        }
        this.cd.detectChanges();
      });
  }

  private initializeConfig(): void {
    const edgeIsDatasource: boolean = this.datasources[0] && this.datasources[0].type === DatasourceType.entity && this.datasources[0].entity.id.entityType === EntityType.EDGE;
    if (edgeIsDatasource) {
      const edge = this.datasources[0].entity;
      this.updateTitle(edge);
    }
  }

  private updateTitle(edge: BaseData<EntityId>): void {
    const displayDefaultTitle: boolean = isDefined(this.settings.enableDefaultTitle) ? this.settings.enableDefaultTitle : false;
    this.ctx.widgetTitle = displayDefaultTitle ? `${edge.name} Quick Overview` : this.widgetConfig.title;
  }
}
