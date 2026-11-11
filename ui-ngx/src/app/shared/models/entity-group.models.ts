// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { WidgetActionDescriptor, WidgetActionSource, WidgetActionType } from '@shared/models/widget.models';
import { ActivatedRouteSnapshot } from '@angular/router';
import { isEqual, isUndefinedOrNull } from '@core/utils';
import { Customer } from '@shared/models/customer.model';
import { EntityData, EntityDataPageLink, EntityKey, EntityKeyType } from '@shared/models/query/query.models';
import { PageLink } from '@shared/models/page/page-link';
import { RoleId } from '@shared/models/id/role-id';
import { Edge } from '@shared/models/edge.models';
import { OtaPackageId } from '@shared/models/id/ota-package-id';
import { DeviceGroupOtaPackage } from '@shared/models/ota-package.models';
import { TbFunction } from '@shared/models/js-function.models';

export const entityGroupTypes: EntityType[] = [
  EntityType.CUSTOMER,
  EntityType.ASSET,
  EntityType.DEVICE,
  EntityType.USER,
  EntityType.ENTITY_VIEW,
  EntityType.DASHBOARD,
  EntityType.EDGE,
  EntityType.AGENT
];

export const entityGroupActionTypes: WidgetActionType[] = [
  WidgetActionType.openDashboard,
  WidgetActionType.custom
];

export const entityGroupActionSources: {[acionSourceId: string]: WidgetActionSource} = {
  actionCellButton:
    {
      name: 'widget-action.action-cell-button',
      value: 'actionCellButton',
      multiple: true,
    },
  rowClick:
    {
      name: 'widget-action.row-click',
      value: 'rowClick',
      multiple: true,
    }
};

export enum EntityGroupDetailsMode {
  onRowClick = 'onRowClick',
  onActionButtonClick = 'onActionButtonClick',
  disabled = 'disabled'
}

export const entityGroupDetailsModeTranslationMap = new Map<EntityGroupDetailsMode, string>(
  [
    [EntityGroupDetailsMode.onRowClick, 'entity-group.details-mode.on-row-click'],
    [EntityGroupDetailsMode.onActionButtonClick, 'entity-group.details-mode.on-action-button-click'],
    [EntityGroupDetailsMode.disabled, 'entity-group.details-mode.disabled']
  ]
);

export interface EntityGroupSettings {
  groupTableTitle: string;
  enableSearch: boolean;
  enableAdd: boolean;
  enableDelete: boolean;
  enableSelection: boolean;
  enableGroupTransfer: boolean;
  detailsMode: EntityGroupDetailsMode;
  displayPagination: boolean;
  defaultPageSize: number;
  pageStepIncrement: number;
  pageStepCount: number;
  enableCredentialsManagement: boolean;
  enableLoginAsUser: boolean;
  enableUsersManagement: boolean;
  enableCustomersManagement: boolean;
  enableAssetsManagement: boolean;
  enableDevicesManagement: boolean;
  enableEntityViewsManagement: boolean;
  enableDashboardsManagement: boolean;
  enableEdgesManagement: boolean;
  enableSchedulerEventsManagement: boolean;
  addDialogOwnerAndGroupWizard: boolean;
}

export enum EntityGroupSortOrder {
  ASC = 'ASC',
  DESC = 'DESC',
  NONE = 'NONE'
}

export const entityGroupSortOrderTranslationMap = new Map<EntityGroupSortOrder, string>(
  [
    [EntityGroupSortOrder.ASC, 'entity-group.sort-order.asc'],
    [EntityGroupSortOrder.DESC, 'entity-group.sort-order.desc'],
    [EntityGroupSortOrder.NONE, 'entity-group.sort-order.none']
  ]
);

export enum EntityGroupColumnType {
  CLIENT_ATTRIBUTE = 'CLIENT_ATTRIBUTE',
  SHARED_ATTRIBUTE = 'SHARED_ATTRIBUTE',
  SERVER_ATTRIBUTE = 'SERVER_ATTRIBUTE',
  TIMESERIES = 'TIMESERIES',
  ENTITY_FIELD = 'ENTITY_FIELD'
}

export const entityGroupColumnTypeTranslationMap = new Map<EntityGroupColumnType, string>(
  [
    [EntityGroupColumnType.CLIENT_ATTRIBUTE, 'entity-group.column-type.client-attribute'],
    [EntityGroupColumnType.SHARED_ATTRIBUTE, 'entity-group.column-type.shared-attribute'],
    [EntityGroupColumnType.SERVER_ATTRIBUTE, 'entity-group.column-type.server-attribute'],
    [EntityGroupColumnType.TIMESERIES, 'entity-group.column-type.timeseries'],
    [EntityGroupColumnType.ENTITY_FIELD, 'entity-group.column-type.entity-field']
  ]
);

export interface EntityGroupEntityField {
  name: string;
  value: string;
  time?: boolean;
}

export const entityGroupEntityFields: {[fieldName: string]: EntityGroupEntityField} = {
  created_time: {
    name: 'entity-group.entity-field.created-time',
    value: 'created_time',
    time: true
  },
  name: {
    name: 'entity-group.entity-field.name',
    value: 'name'
  },
  type: {
    name: 'entity-group.entity-field.type',
    value: 'type'
  },
  device_profile: {
    name: 'entity-group.entity-field.device_profile',
    value: 'device_profile'
  },
  asset_profile: {
    name: 'entity-group.entity-field.asset_profile',
    value: 'asset_profile'
  },
  assigned_customer: {
    name: 'entity-group.entity-field.assigned_customer',
    value: 'assigned_customer'
  },
  authority: {
    name: 'entity-group.entity-field.authority',
    value: 'authority'
  },
  first_name: {
    name: 'entity-group.entity-field.first_name',
    value: 'first_name'
  },
  last_name: {
    name: 'entity-group.entity-field.last_name',
    value: 'last_name'
  },
  email: {
    name: 'entity-group.entity-field.email',
    value: 'email'
  },
  title: {
    name: 'entity-group.entity-field.title',
    value: 'title'
  },
  country: {
    name: 'entity-group.entity-field.country',
    value: 'country'
  },
  state: {
    name: 'entity-group.entity-field.state',
    value: 'state'
  },
  city: {
    name: 'entity-group.entity-field.city',
    value: 'city'
  },
  address: {
    name: 'entity-group.entity-field.address',
    value: 'address'
  },
  address2: {
    name: 'entity-group.entity-field.address2',
    value: 'address2'
  },
  zip: {
    name: 'entity-group.entity-field.zip',
    value: 'zip'
  },
  phone: {
    name: 'entity-group.entity-field.phone',
    value: 'phone'
  },
  label: {
    name: 'entity-group.entity-field.label',
    value: 'label'
  }
};

export const entityGroupEntityFieldsToKeysMap: {[keyName: string]: string} = {
  created_time: 'createdTime',
  assigned_customer: 'assignedCustomer',
  first_name: 'firstName',
  last_name: 'lastName',
  device_profile: 'type',
  asset_profile: 'type'
};

export interface EntityGroupColumn {
  type: EntityGroupColumnType;
  key: string;
  property?: string;
  columnKey?: string;
  title?: string;
  sortOrder: EntityGroupSortOrder;
  mobileHide: boolean;
  disableSorting?: boolean;
  useCellStyleFunction?: boolean;
  cellStyleFunction?: TbFunction;
  useCellContentFunction?: string;
  cellContentFunction?: TbFunction;
}

export interface EntityGroupConfiguration {
  columns: EntityGroupColumn[];
  settings: EntityGroupSettings;
  actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
}

export interface EntityGroup extends BaseData<EntityGroupId>, ExportableEntity<EntityGroupId> {
  type: EntityType;
  name: string;
  ownerId: EntityId;
  groupAll?: boolean;
  additionalInfo?: any;
  configuration?: EntityGroupConfiguration;
}

export interface EntityGroupInfo extends EntityGroup {
  ownerIds: EntityId[];
}

export interface DeviceEntityGroupInfo extends EntityGroupInfo {
  softwareId?: OtaPackageId;
  softwareGroup?: DeviceGroupOtaPackage;
  firmwareId?: OtaPackageId;
  firmwareGroup?: DeviceGroupOtaPackage;
}

export const prepareEntityGroupConfiguration = (groupType: EntityType,
                                                configuration: EntityGroupConfiguration): EntityGroupConfiguration => {
  if (configuration) {
    if (groupType === EntityType.DEVICE) {
      if (configuration.columns) {
        configuration.columns.filter(c => c.key === 'type').forEach(
          typeCol => {
            typeCol.key = 'device_profile';
          }
        );
      }
    } else if (groupType === EntityType.ASSET) {
      if (configuration.columns) {
        configuration.columns.filter(c => c.key === 'type').forEach(
          typeCol => {
            typeCol.key = 'asset_profile';
          }
        );
      }
    }
  }
  return configuration;
};

export interface ShortEntityView {
  id: EntityId;
  name: string;
  [key: string]: any;
}

export const groupColumnTypeToEntityKeyType = (groupColumnType: EntityGroupColumnType): EntityKeyType => {
  switch (groupColumnType) {
    case EntityGroupColumnType.CLIENT_ATTRIBUTE:
      return EntityKeyType.CLIENT_ATTRIBUTE;
    case EntityGroupColumnType.SHARED_ATTRIBUTE:
      return EntityKeyType.SHARED_ATTRIBUTE;
    case EntityGroupColumnType.SERVER_ATTRIBUTE:
      return EntityKeyType.SERVER_ATTRIBUTE;
    case EntityGroupColumnType.TIMESERIES:
      return EntityKeyType.TIME_SERIES;
    case EntityGroupColumnType.ENTITY_FIELD:
      return EntityKeyType.ENTITY_FIELD;
  }
};

export const entityGroupColumnKeyToEntityKey = (column: EntityGroupColumn): string => {
  if (column.type === EntityGroupColumnType.ENTITY_FIELD) {
    const mappedKey = entityGroupEntityFieldsToKeysMap[column.key];
    return mappedKey ? mappedKey : column.key;
  } else {
    return column.key;
  }
};

export const entityGroupColumnToEntityKey = (column: EntityGroupColumn): EntityKey => ({
  type: groupColumnTypeToEntityKeyType(column.type),
  key: entityGroupColumnKeyToEntityKey(column)
});

export const prepareEntityDataColumnMap = (columns: EntityGroupColumn[]): { [entityKeyType: string]: EntityGroupColumn[] } => {
  const result: {[entityKeyType: string]: EntityGroupColumn[]} = {};
  for (const typeKey of Object.keys(EntityGroupColumnType)) {
    const type: EntityGroupColumnType = EntityGroupColumnType[typeKey];
    let typeColumns = columns.filter(c => c.type === type);
    if (typeColumns.length) {
      typeColumns = typeColumns.filter((c, pos, columnsArray) =>
        columnsArray.map(mapCol => mapCol.property).indexOf(c.property) === pos);
      const entityKeyType = groupColumnTypeToEntityKeyType(type);
      result[entityKeyType] = typeColumns;
    }
  }
  return result;
};

export const entityDataToShortEntityView = (entityData: EntityData,
                                            columnsMap: {[entityKeyType: string]: EntityGroupColumn[]},
                                            isUpdate = false): ShortEntityView => {
  const entityView: ShortEntityView = {
    id: entityData.entityId,
    name: ''
  };
  if (entityData.latest) {
    if (entityData.latest[EntityKeyType.ENTITY_FIELD]) {
      const fields = entityData.latest[EntityKeyType.ENTITY_FIELD];
      if (fields.name) {
        entityView.name = fields.name.value;
      } else {
        entityView.name = '';
      }
    }
    for (const entityKeyType of Object.keys(columnsMap)) {
      const latestByType = entityData.latest[entityKeyType];
      const typeColumns = columnsMap[entityKeyType];
      if (latestByType) {
        for (const column of typeColumns) {
          const key = entityGroupColumnKeyToEntityKey(column);
          const tsValue = latestByType[key];
          if (tsValue) {
            entityView[column.property] = tsValue.value;
          }
        }
      }
      if (!isUpdate) {
        for (const column of typeColumns) {
          if (isUndefinedOrNull(entityView[column.property])) {
            entityView[column.property] = '';
          }
        }
      }
    }
  }
  return entityView;
};

export const groupEntitiesPageLinkToEntityDataPageLink =
                              (pageLink: PageLink,
                               columnKeyToEntityKeyMap: {[columnKey: string]: EntityKey}): EntityDataPageLink => {
  const entityDataPageLink: EntityDataPageLink = {
    dynamic: false,
    pageSize: pageLink.pageSize,
    page: pageLink.page,
    textSearch: pageLink.textSearch,
    sortOrder: null
  };
  if (pageLink.sortOrder && pageLink.sortOrder.property) {
    const entityKey = columnKeyToEntityKeyMap[pageLink.sortOrder.property];
    if (entityKey) {
      entityDataPageLink.sortOrder = {
        key: entityKey,
        direction: pageLink.sortOrder.direction
      };
    }
  }
  return entityDataPageLink;
};

export const groupSettingsDefaults = (entityType: EntityType, settings: EntityGroupSettings): EntityGroupSettings => {
  settings = {...{
      groupTableTitle: '',
      enableSearch: true,
      enableAdd: true,
      enableDelete: true,
      enableSelection: true,
      enableGroupTransfer: true,
      detailsMode: EntityGroupDetailsMode.onRowClick,
      displayPagination: true,
      defaultPageSize: 10,
      pageStepIncrement: 10,
      pageStepCount: 3
  }, ...settings};

  if (entityType === EntityType.DEVICE) {
    settings = {...{
        enableCredentialsManagement: true
      }, ...settings};
  }

  if (entityType === EntityType.USER) {
    settings = {...{
        enableLoginAsUser: true
      }, ...settings};
  }

  if (entityType === EntityType.CUSTOMER) {
    settings = {...{
        enableUsersManagement: true,
        enableCustomersManagement: true,
        enableAssetsManagement: true,
        enableDevicesManagement: true,
        enableEntityViewsManagement: true,
        enableDashboardsManagement: true,
        enableEdgesManagement: true
      }, ...settings};
  }

  if (entityType === EntityType.EDGE) {
    settings = {...{
        enableUsersManagement: true,
        enableAssetsManagement: true,
        enableDevicesManagement: true,
        enableEntityViewsManagement: true,
        enableDashboardsManagement: true,
        enableSchedulerEventsManagement: true
      }, ...settings};
  }
  return settings;
};

export const entityGroupsTitle = (groupType: EntityType, shared = false) => {
  const prefix = shared ? 'shared-' : '';
  switch (groupType) {
    case EntityType.ASSET:
      return `entity-group.${prefix}asset-groups`;
    case EntityType.DEVICE:
      return `entity-group.${prefix}device-groups`;
    case EntityType.CUSTOMER:
      return `entity-group.${prefix}customer-groups`;
    case EntityType.USER:
      return `entity-group.user-groups`;
    case EntityType.ENTITY_VIEW:
      return `entity-group.${prefix}entity-view-groups`;
    case EntityType.DASHBOARD:
      return `entity-group.${prefix}dashboard-groups`;
    case EntityType.EDGE:
      return `entity-group.${prefix}edge-groups`;
    case EntityType.AGENT:
      return `entity-group.${prefix}agent-groups`;
  }
};

export const edgeEntitiesTitle = (entityType: EntityType) => {
  switch (entityType) {
    case EntityType.ASSET:
    case EntityType.DEVICE:
    case EntityType.CUSTOMER:
    case EntityType.USER:
    case EntityType.ENTITY_VIEW:
    case EntityType.DASHBOARD:
    case EntityType.EDGE:
      return entityGroupsTitle(entityType);
    case EntityType.SCHEDULER_EVENT:
      return 'scheduler.scheduler';
    case EntityType.RULE_CHAIN:
      return 'rulechain.rulechains';
    case EntityType.INTEGRATION:
      return 'edge.integrations';
  }
};

export interface HierarchyCallbacks {
  groupSelected?: (parentNodeId: string, groupId: string) => void;
  customerGroupsSelected?: (parentNodeId: string, customerId: string, groupsType: EntityType) => void;
  refreshEntityGroups?: (internalId: string) => void;
  refreshCustomerGroups?: (customerGroupIds: string[]) => void;
  refreshEdgeGroups?: (edgeGroupIds: string[]) => void;
  groupUpdated?: (entityGroup: EntityGroupInfo) => void;
  groupDeleted?: (groupNodeId: string, entityGroupId: string) => void;
  groupAdded?: (entityGroup: EntityGroupInfo, existingGroupId: string) => void;
  customerAdded?: (parentNodeId: string, customer: Customer) => void;
  customerUpdated?: (customer: Customer) => void;
  customersDeleted?: (customerIds: string[]) => void;
  edgeGroupsSelected?: (parentNodeId: string, edgeId: string, groupsType: EntityType) => void;
  edgeAdded?: (parentNodeId: string, edge: Edge) => void;
  edgeUpdated?: (edge: Edge) => void;
  edgesDeleted?: (edgeIds: string[]) => void;
}

export interface EntityGroupParams {
  customerId?: string;
  entityGroupId?: string;
  childEntityGroupId?: string;
  groupType?: EntityType;
  childGroupType?: EntityType;
  shared?: boolean;
  hierarchyView?: boolean;
  nodeId?: string;
  internalId?: string;
  hierarchyCallbacks?: HierarchyCallbacks;
  edge?: Edge;
  edgeId?: string;
  edgeEntitiesType?: EntityType;
  edgeEntitiesGroupId?: string;
  backNavigationCommands?: any[];
}

export interface ShareGroupRequest {
  ownerId: EntityId;
  allUserGroup: boolean;
  userGroupId?: EntityGroupId;
  readElseWrite: boolean;
  roleIds?: RoleId[];
}

export const resolveGroupParams = (route: ActivatedRouteSnapshot): EntityGroupParams => {
  let routeParams = {...route.params};
  let routeData = {...route.data};
  let backNavigationCommands: any[];
  if (route.data.backNavigationCommands) {
    backNavigationCommands = routeData.backNavigationCommands;
  }
  while (route.parent !== null) {
    route = route.parent;
    if (routeParams.entityGroupId && route.params.entityGroupId &&
        !isEqual(routeParams.entityGroupId, route.params.entityGroupId)) {
      routeParams.childEntityGroupId = routeParams.entityGroupId;
    }
    if (routeData.groupType && route.data.groupType &&
      !isEqual(routeData.groupType, route.data.groupType)) {
      routeData.childGroupType = routeData.groupType;
    }
    if (isUndefinedOrNull(routeData.edgeEntitiesGroupId)) {
      routeData.edgeEntitiesGroupId = routeParams.childEntityGroupId;
    }
    routeParams = {...routeParams, ...route.params};
    routeData = { ...routeData, ...route.data };
    if (route.data.backNavigationCommands && !backNavigationCommands) {
      backNavigationCommands = routeData.backNavigationCommands;
    }
  }
  return {
    customerId: routeParams.customerId,
    entityGroupId: routeParams.entityGroupId,
    groupType: routeData.groupType,
    shared: routeData.shared,
    childEntityGroupId: routeParams.childEntityGroupId,
    childGroupType: routeData.childGroupType,
    edgeId: routeParams.edgeId,
    edgeEntitiesType: routeData.edgeEntitiesType,
    edgeEntitiesGroupId: routeData.edgeEntitiesGroupId,
    backNavigationCommands
  };
};
