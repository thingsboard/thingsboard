// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LoadNodesCallback, NavTreeEditCallbacks, NodeSelectedCallback } from '@shared/components/nav-tree.component';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityType } from '@shared/models/entity-type.models';
import {
  EntityGroupInfo,
  EntityGroupParams,
  HierarchyCallbacks, prepareEntityGroupConfiguration
} from '@shared/models/entity-group.models';
import {
  CustomerNodeData,
  customerNodeText,
  CustomersHierarchyNode,
  CustomersHierarchyViewMode,
  EdgeEntityGroupNodeData,
  EdgeEntityGroupsNodeData,
  EdgeNodeCustomerData,
  EdgeNodeData,
  edgeNodeText,
  entitiesNodeText,
  EntityGroupNodeData,
  entityGroupNodeText,
  EntityGroupsNodeData,
  entityGroupsNodeText
} from '@home/pages/customer/customers-hierarchy.models';
import { EntityService } from '@core/http/entity.service';
import { Customer } from '@shared/models/customer.model';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { groupResourceByGroupType, Operation, Resource } from '@shared/models/security.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityGroupStateInfo } from '@home/models/group/group-entities-table-config.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { deepClone } from '@core/utils';
import { EntityGroupsTableConfig } from '@home/components/group/entity-groups-table-config';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupConfigResolver } from '@home/components/group/entity-group-config.resolver';
import { Edge, edgeEntityGroupTypes } from '@shared/models/edge.models';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { RuleChainsTableConfigResolver } from '@home/pages/rulechain/rulechains-table-config.resolver';
import { Authority } from '@shared/models/authority.enum';
import { EdgeService } from '@core/http/edge.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { RuleChainParams } from '@shared/models/rule-chain.models';
import { IntegrationParams } from '@shared/models/integration.models';
import { IntegrationsTableConfigResolver } from '@home/pages/integration/integrations-table-config.resolver';

const groupTypes: EntityType[] = [
  EntityType.USER,
  EntityType.CUSTOMER,
  EntityType.ASSET,
  EntityType.DEVICE,
  EntityType.ENTITY_VIEW,
  EntityType.DASHBOARD,
  EntityType.EDGE
];

@Component({
    selector: 'tb-customers-hierarchy',
    templateUrl: './customers-hierarchy.component.html',
    styleUrls: ['./customers-hierarchy.component.scss'],
    standalone: false
})
export class CustomersHierarchyComponent extends PageComponent implements OnInit {

  isNavTreeOpen = true;

  nodeIdCounter = 0;
  selectedNodeId = -1;

  parentIdToGroupAllNodeId: {[parentNodeId: string]: string} = {};

  entityGroupNodesMap: {[parentNodeId: string]: {[entityGroupId: string]: string}} = {};
  customerNodesMap: {[parentNodeId: string]: {[customerId: string]: string}} = {};
  customerGroupsNodesMap: {[customerNodeId: string]: {[groupsType: string]: string}} = {};
  edgeNodesMap: {[parentNodeId: string]: {[edgeId: string]: string}} = {};
  edgeGroupsNodesMap: {[customerNodeId: string]: {[groupsType: string]: string}} = {};

  internalIdToNodeIds: {[internalId: string]: string[]} = {};
  internalIdToParentNodeIds: {[internalId: string]: string[]} = {};
  nodeIdToInternalId: {[nodeId: string]: string} = {};

  private hierarchyCallbacks: HierarchyCallbacks = {
    groupSelected: this.onGroupSelected.bind(this),
    customerGroupsSelected: this.onCustomerGroupsSelected.bind(this),
    refreshEdgeGroups: this.refreshEdgeGroups.bind(this),
    refreshEntityGroups: this.refreshEntityGroups.bind(this),
    refreshCustomerGroups: this.refreshCustomerGroups.bind(this),
    groupUpdated: this.groupUpdated.bind(this),
    groupDeleted: this.groupDeleted.bind(this),
    groupAdded: this.groupAdded.bind(this),
    customerAdded: this.customerAdded.bind(this),
    customerUpdated: this.customerUpdated.bind(this),
    customersDeleted: this.customersDeleted.bind(this),
    edgeAdded: this.edgeAdded.bind(this),
    edgesDeleted: this.edgesDeleted.bind(this),
    edgeGroupsSelected: this.onEdgeGroupsSelected.bind(this),
    edgeUpdated: this.edgeUpdated.bind(this)
  };

  viewMode: CustomersHierarchyViewMode = 'groups';
  entityGroupParams: EntityGroupParams = {
    groupType: EntityType.CUSTOMER,
    hierarchyView: true,
    hierarchyCallbacks: this.hierarchyCallbacks
  };
  entityGroupsTableConfig: EntityGroupsTableConfig | EntityTableConfig<BaseData<HasId>> =
    this.resolveEntityGroupTableConfig(this.entityGroupParams);
  entityGroupStateInfo: EntityGroupStateInfo<BaseData<HasId>> = null;

  public edgeId = '';

  public nodeEditCallbacks: NavTreeEditCallbacks = {};

  private customerData: EdgeNodeCustomerData = {};

  private allowedGroupTypes = groupTypes.filter((groupType) =>
    this.userPermissionsService.hasGenericPermission(groupResourceByGroupType.get(groupType), Operation.READ));

  private allowedEdgeGroupTypes = edgeEntityGroupTypes.filter((groupType) =>
    this.userPermissionsService.hasGenericPermission(groupResourceByGroupType.get(groupType), Operation.READ));

  private edgesSupportEnabled = getCurrentAuthState(this.store).edgesSupportEnabled;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private ngZone: NgZone,
              private entityGroupService: EntityGroupService,
              private entityService: EntityService,
              private edgeService: EdgeService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private entityGroupsTableConfigResolver: EntityGroupsTableConfigResolver,
              private entityGroupConfigResolver: EntityGroupConfigResolver,
              private ruleChainsTableConfigResolver: RuleChainsTableConfigResolver,
              private integrationsTableConfigResolver: IntegrationsTableConfigResolver) {
    super(store);
  }

  ngOnInit() {
  }

  public loadNodes: LoadNodesCallback = (node: CustomersHierarchyNode, cb) => {
    if (node.id === '#') {
      this.entityGroupService.getAllEntityGroups(-1, EntityType.CUSTOMER, true, {ignoreLoading: true}).subscribe((entityGroups) => {
        cb(this.entityGroupsToNodes(node.id, null, entityGroups));
        this.entityGroupParams.nodeId = node.id;
      });
    } else if (node.data && node.data.type) {
      if (node.data.type === 'group') {
        const entityGroup = node.data.entity;
        if (entityGroup.type === EntityType.CUSTOMER) {
          this.entityService.getEntityGroupEntities(entityGroup.id.id,
            EntityType.CUSTOMER, -1, {ignoreLoading: true}).subscribe(
              (customers: Customer[]) => {
              cb(this.customersToNodes(node.id, entityGroup.id.id, customers));
            });
        } else if (entityGroup.type === EntityType.EDGE) {
          const customerData: EdgeNodeCustomerData = {
            customerId: node.data.entity.ownerId.id,
            customerGroupId: node.data.parentEntityGroupId
          };
          this.entityService.getEntityGroupEntities(entityGroup.id.id,
            EntityType.EDGE, -1, {ignoreLoading: true}).subscribe(
            (edges: Edge[]) => {
              cb(this.edgesToNodes(node.id, entityGroup.id.id, edges, customerData));
            });
        } else {
          cb([]);
        }
      } else if (node.data.type === 'customer') {
        const customer = node.data.entity;
        const parentEntityGroupId = node.data.parentEntityGroupId;
        cb(this.loadNodesForCustomer(node.id, parentEntityGroupId, customer));
      } else if (node.data.type === 'groups') {
        const owner = node.data.customer;
        const parentEntityGroupId = node.data.parentEntityGroupId;
        this.entityGroupService.getAllEntityGroupsByOwnerId(-1, owner.id.entityType,
          owner.id.id, node.data.groupsType, {ignoreLoading: true}).subscribe((entityGroups) => {
          cb(this.entityGroupsToNodes(node.id, parentEntityGroupId, entityGroups));
        });
      } else if (node.data.type === 'edge' || node.data.type === 'edgeEntities') {
        const customerData = node.data.customerData;
        const edge = node.data.edge;
        const parentEntityGroupId = node.data.parentEntityGroupId;
        if (node.data.type === 'edge') {
          cb(this.loadNodesForEdge(node.id, parentEntityGroupId, edge, customerData));
        } else {
          this.entityGroupService.getAllEdgeEntityGroups(-1, node.data.edge.id.id, node.data.entityType, {ignoreLoading: true})
            .subscribe((entityGroups) => {
              cb(this.edgeEntityGroupsToNodes(node.id, parentEntityGroupId, customerData, edge, entityGroups));
            });
        }
      }
    }
  };

  public onNodeSelected: NodeSelectedCallback = (node: CustomersHierarchyNode) => {
    let nodeId;
    if (!node) {
      nodeId = -1;
    } else {
      nodeId = node.id;
    }
    if (this.selectedNodeId !== nodeId) {
      this.selectedNodeId = nodeId;
      const entityGroupParams: EntityGroupParams = {
        hierarchyView: true,
        hierarchyCallbacks: this.hierarchyCallbacks
      };
      if (nodeId === -1) {
        entityGroupParams.groupType = EntityType.CUSTOMER;
        entityGroupParams.nodeId = '#';
        entityGroupParams.internalId = '#';
        this.updateGroupsView(entityGroupParams);
      } else if (node.data.type === 'groups' || node.data.type === 'group') {
        if (node.data.type === 'groups') {
          const parentEntityGroupId = node.data.parentEntityGroupId;
          if (parentEntityGroupId) {
            entityGroupParams.entityGroupId = parentEntityGroupId;
            entityGroupParams.groupType = EntityType.CUSTOMER;
            entityGroupParams.childGroupType = node.data.groupsType;
          } else {
            entityGroupParams.groupType = node.data.groupsType;
          }
          entityGroupParams.customerId = node.data.customer.id.id;
          entityGroupParams.nodeId = node.id;
          entityGroupParams.internalId = node.data.internalId;
          this.updateGroupsView(entityGroupParams, node.data.customer);
        } else {
          const entityGroup = node.data.entity;
          const parentEntityGroupId = node.data.parentEntityGroupId;
          if (parentEntityGroupId) {
            entityGroupParams.entityGroupId = parentEntityGroupId;
            entityGroupParams.groupType = EntityType.CUSTOMER;
            entityGroupParams.childEntityGroupId = entityGroup.id.id;
            entityGroupParams.childGroupType = entityGroup.type;
          } else {
            entityGroupParams.entityGroupId = entityGroup.id.id;
            entityGroupParams.groupType = entityGroup.type;
          }
          if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
            entityGroupParams.customerId = entityGroup.ownerId.id;
          } else {
            entityGroupParams.customerId = null;
          }
          entityGroupParams.nodeId = node.id;
          entityGroupParams.internalId = node.data.internalId;
          if (entityGroupParams.childGroupType === EntityType.EDGE) {
            this.customerData = {
              customerId: entityGroup.ownerId.id,
              customerGroupId: parentEntityGroupId
            };
          }
          this.updateGroupView(entityGroupParams, entityGroup);
        }
      } else if (node.data.type === 'edgeEntities' || node.data.type === 'edgeGroup') {
        entityGroupParams.customerId = node.data.customerData.customerId;
        entityGroupParams.nodeId = node.id;
        entityGroupParams.internalId = node.data.internalId;
        entityGroupParams.entityGroupId = node.data.customerData.customerGroupId;
        entityGroupParams.childEntityGroupId = node.data.parentEntityGroupId;
        entityGroupParams.edgeId = node.data.edge.id.id;
        entityGroupParams.groupType = EntityType.EDGE;
        if (node.data.type === 'edgeEntities') {
          entityGroupParams.childGroupType = node.data.entityType;
          this.updateViewByEntityType(node.data.entityType, entityGroupParams, node.data.edge);
        } else {
          const entityGroup = node.data.entityGroup;
          entityGroupParams.edgeEntitiesGroupId = entityGroup.id.id;
          entityGroupParams.edgeEntitiesType = entityGroup.type;
          this.updateGroupView(entityGroupParams, entityGroup);
        }
      }
    }
  };

  private updateGroupsView(entityGroupParams: EntityGroupParams, customer?: Customer) {
    const entityGroupsTableConfig = this.resolveEntityGroupTableConfig(entityGroupParams, customer?.title);
    this.updateView('groups', entityGroupParams, entityGroupsTableConfig, null);
  }

  private updateGroupView(entityGroupParams: EntityGroupParams, entityGroup: EntityGroupInfo) {
    this.entityGroupConfigResolver.constructGroupConfig(entityGroupParams, entityGroup).subscribe(
      (entityGroupStateInfo) => {
        this.updateView('group', entityGroupParams, null, entityGroupStateInfo);
      }
    );
  }

  private updateView(viewMode: CustomersHierarchyViewMode,
                     entityGroupParams: EntityGroupParams,
                     entityGroupsTableConfig: EntityGroupsTableConfig | EntityTableConfig<BaseData<HasId>>,
                     entityGroupStateInfo: EntityGroupStateInfo<BaseData<HasId>>) {
    setTimeout(() => {
      this.viewMode = viewMode;
      this.entityGroupParams = entityGroupParams;
      this.entityGroupsTableConfig = entityGroupsTableConfig;
      this.entityGroupStateInfo = entityGroupStateInfo;
      this.cd.detectChanges();
    }, 0);
  }

  private resolveEntityGroupTableConfig(entityGroupParams: EntityGroupParams, customerTitle?: string): EntityGroupsTableConfig {
    return this.entityGroupsTableConfigResolver.resolveEntityGroupTableConfig(entityGroupParams,
        false, customerTitle) as EntityGroupsTableConfig;
  }

  private updateViewByEntityType(entityType: EntityType, entityGroupParams: EntityGroupParams, edge: Edge) {
    switch (entityType) {
      case EntityType.USER:
      case EntityType.ASSET:
      case EntityType.DEVICE:
      case EntityType.ENTITY_VIEW:
      case EntityType.DASHBOARD:
        this.updateEdgeGroupsView(entityGroupParams, edge);
        break;
      case EntityType.SCHEDULER_EVENT:
        this.updateSchedulerView('scheduler', edge);
        break;
      case EntityType.RULE_CHAIN:
        const ruleChainParams = entityGroupParams as RuleChainParams;
        ruleChainParams.ruleChainScope = 'edge';
        this.updateRuleChains(ruleChainParams);
        break;
      case EntityType.INTEGRATION:
        const integrationParams = entityGroupParams as IntegrationParams;
        integrationParams.integrationScope = 'edge';
        this.updateIntegrations(integrationParams);
        break;
    }
  }

  private updateEdgeGroupsView(entityGroupParams: EntityGroupParams, edge: Edge) {
    const entityGroupsTableConfig = this.resolveEntityGroupTableConfig(entityGroupParams, edge.name);
    this.updateView('groups', entityGroupParams, entityGroupsTableConfig, null);
  }

  private updateSchedulerView(viewMode: CustomersHierarchyViewMode, edge: Edge) {
    this.edgeId = edge.id.id;
    this.viewMode = viewMode;
  }

  private updateRuleChains(ruleChainParams: RuleChainParams) {
    const ruleChainsTableConfig = this.resolveRuleChainsTableConfig(ruleChainParams);
    this.updateView('groups', ruleChainParams, ruleChainsTableConfig, null);
  }

  private resolveRuleChainsTableConfig(ruleChainParams: RuleChainParams): EntityTableConfig<BaseData<HasId>> {
    return this.ruleChainsTableConfigResolver.resolveRuleChainsTableConfig(ruleChainParams);
  }

  private updateIntegrations(integrationParams: IntegrationParams) {
    const integrationsTableConfig = this.resolveIntegrationsTableConfig(integrationParams);
    this.updateView('groups', integrationParams, integrationsTableConfig, null);
  }

  private resolveIntegrationsTableConfig(integrationParams: IntegrationParams): EntityTableConfig<BaseData<HasId>> {
    return this.integrationsTableConfigResolver.resolveIntegrationsTableConfig(integrationParams);
  }

  selectRootNode() {
    setTimeout(() => {
      this.nodeEditCallbacks.deselectAll();
    }, 0);
  }

  private entityGroupsToNodes(parentNodeId: string, parentEntityGroupId: string,
                              entityGroups: EntityGroupInfo[]): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    this.entityGroupNodesMap[parentNodeId] = {};
    if (entityGroups) {
      entityGroups.forEach((entityGroup) => {
        const node = this.createEntityGroupNode(parentNodeId, entityGroup, parentEntityGroupId);
        nodes.push(node);
        if (entityGroup.groupAll) {
          this.parentIdToGroupAllNodeId[parentNodeId] = node.id;
        }
      });
    }
    return nodes;
  }

  private createEntityGroupNode(parentNodeId: string, entityGroup: EntityGroupInfo, parentEntityGroupId: string): CustomersHierarchyNode {
    let nodesMap = this.entityGroupNodesMap[parentNodeId];
    if (!nodesMap) {
      nodesMap = {};
      this.entityGroupNodesMap[parentNodeId] = nodesMap;
    }
    entityGroup.configuration = prepareEntityGroupConfiguration(entityGroup.type, entityGroup.configuration);
    const node: CustomersHierarchyNode = {
      id: (++this.nodeIdCounter) + '',
      icon: false,
      text: entityGroupNodeText(entityGroup),
      children: entityGroup.type === EntityType.CUSTOMER || entityGroup.type === EntityType.EDGE,
      data: {
        type: 'group',
        entity: entityGroup,
        parentEntityGroupId,
        internalId: entityGroup.id.id
      } as EntityGroupNodeData
    };
    nodesMap[entityGroup.id.id] = node.id;
    this.registerNode(node, parentNodeId);
    return node;
  }

  private edgeEntityGroupsToNodes(parentNodeId: string, parentEntityGroupId: string, customerData: EdgeNodeCustomerData,
                                  edge: Edge, entityGroups: EntityGroupInfo[]): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    this.entityGroupNodesMap[parentNodeId] = {};
    if (entityGroups) {
      entityGroups.forEach((entityGroup) => {
        const node = this.createEdgeEntityGroupNode(parentNodeId, entityGroup, parentEntityGroupId, edge, customerData);
        nodes.push(node);
        if (entityGroup.groupAll) {
          this.parentIdToGroupAllNodeId[parentNodeId] = node.id;
        }
      });
    }
    return nodes;
  }

  private createEdgeEntityGroupNode(parentNodeId: string, entityGroup: EntityGroupInfo, parentEntityGroupId: string,
                                    edge: Edge, customerData: EdgeNodeCustomerData): CustomersHierarchyNode {
    let nodesMap = this.entityGroupNodesMap[parentNodeId];
    if (!nodesMap) {
      nodesMap = {};
      this.entityGroupNodesMap[parentNodeId] = nodesMap;
    }
    const node: CustomersHierarchyNode = {
      id: (++this.nodeIdCounter) + '',
      icon: false,
      text: entityGroupNodeText(entityGroup),
      children: false,
      data: {
        type: 'edgeGroup',
        entityGroup,
        parentEntityGroupId,
        edge,
        customerData,
        internalId: entityGroup.id.id
      } as EdgeEntityGroupNodeData
    };
    nodesMap[entityGroup.id.id] = node.id;
    this.registerNode(node, parentNodeId);
    return node;
  }

  private customersToNodes(parentNodeId: string, groupId: string, customers: Customer[]): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    this.customerNodesMap[parentNodeId] = {};
    if (customers) {
      customers.forEach((customer) => {
        const node = this.createCustomerNode(parentNodeId, customer, groupId);
        nodes.push(node);
      });
    }
    return nodes;
  }

  private createCustomerNode(parentNodeId: string, customer: Customer, groupId: string): CustomersHierarchyNode {
    let nodesMap = this.customerNodesMap[parentNodeId];
    if (!nodesMap) {
      nodesMap = {};
      this.customerNodesMap[parentNodeId] = nodesMap;
    }
    const node: CustomersHierarchyNode = {
      id: (++this.nodeIdCounter) + '',
      icon: false,
      text: customerNodeText(customer),
      children: true,
      state: {
        disabled: true
      },
      data: {
        type: 'customer',
        entity: customer,
        parentEntityGroupId: groupId,
        internalId: customer.id.id
      } as CustomerNodeData
    };
    nodesMap[customer.id.id] = node.id;
    this.registerNode(node, parentNodeId);
    return node;
  }

  private edgesToNodes(parentNodeId: string, groupId: string, edges: Edge[], customerData: EdgeNodeCustomerData): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    this.edgeNodesMap[parentNodeId] = {};
    if (edges) {
      edges.forEach((edge) => {
        const node = this.createEdgeNode(parentNodeId, groupId, edge, customerData);
        nodes.push(node);
      });
      return nodes;
    }
  }

  private createEdgeNode(parentNodeId: string, groupId: string, edge: Edge, customerData: EdgeNodeCustomerData): CustomersHierarchyNode {
    let nodesMap = this.edgeNodesMap[parentNodeId];
    if (!nodesMap) {
      nodesMap = {};
      this.edgeNodesMap[parentNodeId] = nodesMap;
    }
    const node: CustomersHierarchyNode = {
      id: (++this.nodeIdCounter) + '',
      icon: false,
      text: edgeNodeText(edge),
      children: true,
      state: {
        disabled: true
      },
      data: {
        type: 'edge',
        edge,
        customerData,
        parentEntityGroupId: groupId,
        internalId: edge.id.id
      } as EdgeNodeData
    };
    nodesMap[edge.id.id] = node.id;
    this.registerNode(node, parentNodeId);
    return node;
  }

  private loadNodesForCustomer(parentNodeId: string, parentEntityGroupId: string,
                               customer: Customer): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    const nodesMap = {};
    this.customerGroupsNodesMap[parentNodeId] = nodesMap;
    if (!this.edgesSupportEnabled) {
      this.allowedGroupTypes = this.allowedGroupTypes.filter((groupType: EntityType) => groupType !== EntityType.EDGE);
    }
    this.allowedGroupTypes.forEach((groupType) => {
      const node: CustomersHierarchyNode = {
        id: (++this.nodeIdCounter) + '',
        icon: false,
        text: entityGroupsNodeText(this.translate, groupType),
        children: true,
        data: {
          type: 'groups',
          groupsType: groupType,
          customer,
          parentEntityGroupId,
          internalId: customer.id.id + '_' + groupType
        } as EntityGroupsNodeData
      };
      nodes.push(node);
      nodesMap[groupType] = node.id;
      this.registerNode(node, parentNodeId);
    });
    return nodes;
  }

  private loadNodesForEdge(parentNodeId: string, parentEntityGroupId: string, edge: Edge,
                           customerData: EdgeNodeCustomerData): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    const nodesMap = {};
    this.edgeGroupsNodesMap[parentNodeId] = nodesMap;
    this.allowedEdgeGroupTypes.forEach((groupType) => {
      const text = entityGroupsNodeText(this.translate, groupType);
      const node = this.createBaseEdgeNode(parentEntityGroupId, edge, customerData, groupType, text, true);
      nodes.push(node);
      nodesMap[groupType] = node.id;
      this.registerNode(node, parentNodeId);
    });
    if (this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.READ)) {
      const entityType = EntityType.SCHEDULER_EVENT;
      const text = entitiesNodeText(this.translate, entityType, 'entity.type-scheduler-events');
      const node = this.createBaseEdgeNode(parentEntityGroupId, edge, customerData, entityType, text, false);
      nodes.push(node);
      nodesMap[entityType] = node.id;
      this.registerNode(node, parentNodeId);
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.READ) &&
        getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN) {
      const entityType = EntityType.RULE_CHAIN;
      const text = entitiesNodeText(this.translate, entityType, 'entity.type-rulechains');
      const node = this.createBaseEdgeNode(parentEntityGroupId, edge, customerData, entityType, text, false);
      nodes.push(node);
      nodesMap[entityType] = node.id;
      this.registerNode(node, parentNodeId);
    }
    if (this.userPermissionsService.hasGenericPermission(Resource.INTEGRATION, Operation.READ) &&
      getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN) {
      const entityType = EntityType.INTEGRATION;
      const text = entitiesNodeText(this.translate, entityType, 'entity.type-integrations');
      const node = this.createBaseEdgeNode(parentEntityGroupId, edge, customerData, entityType, text, false);
      nodes.push(node);
      nodesMap[entityType] = node.id;
      this.registerNode(node, parentNodeId);
    }
    return nodes;
  }

  private createBaseEdgeNode(parentEntityGroupId: string, edge: Edge, customerData: EdgeNodeCustomerData, entityType: EntityType,
                             text: string, children: boolean): CustomersHierarchyNode {
    return {
      id: (++this.nodeIdCounter) + '',
      icon: false,
      children,
      text,
      data: {
        type: 'edgeEntities',
        entityType,
        edge,
        parentEntityGroupId,
        customerData,
        internalId: edge.id.id + '_' + entityType
      } as EdgeEntityGroupsNodeData
    };
  }

  private registerNode(node: CustomersHierarchyNode, parentNodeId: string) {
    let nodeIds = this.internalIdToNodeIds[node.data.internalId];
    if (!nodeIds) {
      nodeIds = [];
      this.internalIdToNodeIds[node.data.internalId] = nodeIds;
    }
    if (nodeIds.indexOf(node.id) === -1) {
      nodeIds.push(node.id);
    }
    let parentNodeIds = this.internalIdToParentNodeIds[node.data.internalId];
    if (!parentNodeIds) {
      parentNodeIds = [];
      this.internalIdToParentNodeIds[node.data.internalId] = parentNodeIds;
    }
    if (parentNodeIds.indexOf(parentNodeId) === -1) {
      parentNodeIds.push(parentNodeId);
    }
    this.nodeIdToInternalId[node.id] = node.data.internalId;
  }

  private onGroupSelected(parentNodeId: string, groupId: string) {
    const nodesMap = this.entityGroupNodesMap[parentNodeId];
    if (nodesMap) {
      const nodeId = nodesMap[groupId];
      if (nodeId) {
        setTimeout(() => {
          this.nodeEditCallbacks.selectNode(nodeId);
          if (!this.nodeEditCallbacks.nodeIsOpen(nodeId)) {
            this.nodeEditCallbacks.openNode(nodeId);
          }
        }, 0);
      }
    } else {
      this.openNode(parentNodeId, () => this.onGroupSelected(parentNodeId, groupId));
    }
  }

  private onCustomerGroupsSelected(parentNodeId: string, customerId: string, groupsType: EntityType) {
    const nodesMap = this.customerNodesMap[parentNodeId];
    if (nodesMap) {
      const customerNodeId = nodesMap[customerId];
      if (customerNodeId) {
        const customerGroupNodeMap = this.customerGroupsNodesMap[customerNodeId];
        if (customerGroupNodeMap) {
          const nodeId = customerGroupNodeMap[groupsType];
          if (nodeId) {
            setTimeout(() => {
              this.nodeEditCallbacks.selectNode(nodeId);
              if (!this.nodeEditCallbacks.nodeIsOpen(nodeId)) {
                this.nodeEditCallbacks.openNode(nodeId);
              }
            }, 0);
          }
        } else {
          this.openNode(customerNodeId, () => this.onCustomerGroupsSelected(parentNodeId, customerId, groupsType));
        }
      }
    } else {
      this.openNode(parentNodeId, () => this.onCustomerGroupsSelected(parentNodeId, customerId, groupsType));
    }
  }

  private onEdgeGroupsSelected(parentNodeId: string, edgeId: string, groupsType: EntityType) {
    const nodesMap = this.edgeNodesMap[parentNodeId];
    if (nodesMap) {
      const edgeNodeId = nodesMap[edgeId];
      if (edgeNodeId) {
        const customerGroupNodeMap = this.edgeGroupsNodesMap[edgeNodeId];
        if (customerGroupNodeMap) {
          const nodeId = customerGroupNodeMap[groupsType];
          if (nodeId) {
            setTimeout(() => {
              this.nodeEditCallbacks.selectNode(nodeId);
              if (!this.nodeEditCallbacks.nodeIsOpen(nodeId)) {
                this.nodeEditCallbacks.openNode(nodeId);
              }
            }, 0);
          }
        } else {
          this.openNode(edgeNodeId, () => this.onEdgeGroupsSelected(parentNodeId, edgeId, groupsType));
        }
      }
    } else {
      this.openNode(parentNodeId, () => this.onEdgeGroupsSelected(parentNodeId, edgeId, groupsType));
    }
  }

  private refreshEntityGroups(internalId: string) {
    let nodeIds: string[];
    if (internalId && internalId !== '#') {
      nodeIds = this.internalIdToNodeIds[internalId];
    } else {
      nodeIds = ['#'];
    }
    if (nodeIds) {
      nodeIds.forEach((nodeId) => {
        if (this.nodeEditCallbacks.nodeIsLoaded(nodeId)) {
          this.nodeEditCallbacks.refreshNode(nodeId);
        }
      });
    }
  }

  private refreshCustomerGroups(customerGroupIds: string[]) {
    customerGroupIds.forEach((groupId) => {
      const nodeIds = this.internalIdToNodeIds[groupId];
      if (nodeIds) {
        nodeIds.forEach((nodeId) => {
          if (this.nodeEditCallbacks.nodeIsLoaded(nodeId)) {
            this.nodeEditCallbacks.refreshNode(nodeId);
          }
        });
      }
    });
  }

  private groupUpdated(entityGroup: EntityGroupInfo) {
    const nodeIds = this.internalIdToNodeIds[entityGroup.id.id];
    if (nodeIds) {
      const nodeText = entityGroupNodeText(entityGroup);
      nodeIds.forEach((nodeId) => {
        this.nodeEditCallbacks.updateNode(nodeId, nodeText, {entity: entityGroup});
      });
    }
  }

  private groupDeleted(groupNodeId: string, entityGroupId: string) {
    const parentId = this.nodeEditCallbacks.getParentNodeId(groupNodeId);
    if (parentId) {
      setTimeout(() => {
        this.nodeEditCallbacks.selectNode(parentId);
      }, 0);
    }
    const nodeIds = this.internalIdToNodeIds[entityGroupId];
    nodeIds.forEach((nodeId) => {
      this.nodeEditCallbacks.deleteNode(nodeId);
    });
  }

  private groupAdded(entityGroup: EntityGroupInfo, existingGroupId: string) {
    const parentNodeIds = this.internalIdToParentNodeIds[existingGroupId];
    if (parentNodeIds) {
      parentNodeIds.forEach((parentNodeId) => {
        if (this.nodeEditCallbacks.nodeIsLoaded(parentNodeId)) {
          let parentEntityGroupId = null;
          const parentNode = this.nodeEditCallbacks.getNode(parentNodeId) as CustomersHierarchyNode;
          if (parentNode && parentNode.data && parentNode.data.parentEntityGroupId) {
            parentEntityGroupId = parentNode.data.parentEntityGroupId;
          }
          const node = this.createEntityGroupNode(parentNodeId, entityGroup, parentEntityGroupId);
          this.nodeEditCallbacks.createNode(parentNodeId, node, 'last');
        }
      });
    }
  }

  private customerAdded(parentNodeId: string, customer: Customer) {
    const parentInternalId = this.nodeIdToInternalId[parentNodeId];
    const parentNodeIds = this.internalIdToNodeIds[parentInternalId];
    if (parentNodeIds) {
      const targetParentNodeIds = deepClone(parentNodeIds);
      parentNodeIds.forEach((nodeId) => {
        const groupAllParentId = this.nodeEditCallbacks.getParentNodeId(nodeId);
        if (groupAllParentId) {
          const groupAllNodeId = this.parentIdToGroupAllNodeId[groupAllParentId];
          if (groupAllNodeId && targetParentNodeIds.indexOf(groupAllNodeId) === -1) {
            targetParentNodeIds.push(groupAllNodeId);
          }
        }
      });
      targetParentNodeIds.forEach((targetParentNodeId) => {
        if (this.nodeEditCallbacks.nodeIsLoaded(targetParentNodeId)) {
          const groupId = this.nodeIdToInternalId[targetParentNodeId];
          if (groupId) {
            const node = this.createCustomerNode(targetParentNodeId, customer, groupId);
            this.nodeEditCallbacks.createNode(targetParentNodeId, node, 'last');
          }
        }
      });
    }
  }

  private customerUpdated(customer: Customer) {
    const nodeIds = this.internalIdToNodeIds[customer.id.id];
    if (nodeIds) {
      nodeIds.forEach((nodeId) => {
        this.nodeEditCallbacks.updateNode(nodeId, customerNodeText(customer));
      });
    }
  }

  private customersDeleted(customerIds: string[]) {
    customerIds.forEach((customerId) => {
      const nodeIds = this.internalIdToNodeIds[customerId];
      nodeIds.forEach((nodeId) => {
        this.nodeEditCallbacks.deleteNode(nodeId);
      });
    });
  }

  private openNode(nodeId: string, openCb: () => void) {
    if (!this.nodeEditCallbacks.nodeIsOpen(nodeId)) {
      this.nodeEditCallbacks.openNode(nodeId, openCb);
    }
  }

  private refreshEdgeGroups(edgeGroupIds: string[]) {
    edgeGroupIds.forEach((groupId) => {
      const nodeIds = this.internalIdToNodeIds[groupId];
      if (nodeIds) {
        nodeIds.forEach((nodeId) => {
          if (this.nodeEditCallbacks.nodeIsLoaded(nodeId)) {
            this.nodeEditCallbacks.refreshNode(nodeId);
          }
        });
      }
    });
  }

  private edgeAdded(parentNodeId: string, edge: Edge) {
    const parentInternalId = this.nodeIdToInternalId[parentNodeId];
    const parentNodeIds = this.internalIdToNodeIds[parentInternalId];
    if (parentNodeIds) {
      const targetParentNodeIds = deepClone(parentNodeIds);
      parentNodeIds.forEach((nodeId) => {
        const groupAllParentId = this.nodeEditCallbacks.getParentNodeId(nodeId);
        if (groupAllParentId) {
          const groupAllNodeId = this.parentIdToGroupAllNodeId[groupAllParentId];
          if (groupAllNodeId && targetParentNodeIds.indexOf(groupAllNodeId) === -1) {
            targetParentNodeIds.push(groupAllNodeId);
          }
        }
      });
      targetParentNodeIds.forEach((targetParentNodeId) => {
        if (this.nodeEditCallbacks.nodeIsLoaded(targetParentNodeId)) {
          const groupId = this.nodeIdToInternalId[targetParentNodeId];
          if (groupId) {
            const node = this.createEdgeNode(targetParentNodeId, groupId, edge, this.customerData);
            this.nodeEditCallbacks.createNode(targetParentNodeId, node, 'last');
          }
        }
      });
    }
  }

  private edgeUpdated(edge: Edge) {
    const nodeIds = this.internalIdToNodeIds[edge.id.id];
    if (nodeIds) {
      nodeIds.forEach((nodeId) => {
        this.nodeEditCallbacks.updateNode(nodeId, edgeNodeText(edge));
      });
    }
  }

  private edgesDeleted(edgeIds: string[]) {
    edgeIds.forEach((edgeId) => {
      const nodeIds = this.internalIdToNodeIds[edgeId];
      nodeIds.forEach((nodeId) => {
        this.nodeEditCallbacks.deleteNode(nodeId);
      });
    });
  }

}
