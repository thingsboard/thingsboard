// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { EdgeProfileId } from '@shared/models/id/edge-profile-id';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { IntegrationId } from '@shared/models/id/integration-id';
import { SchedulerEventId } from '@shared/models/id/scheduler-event-id';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { edgeEntityGroupTypes } from '@shared/models/edge.models';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface EdgeProfileEntityGroup {
  groupType: EntityType;
  entityGroupId: EntityGroupId;
}

export interface EdgeProfileData {
  entityGroups: EdgeProfileEntityGroup[];
  ruleChainIds: RuleChainId[];
  integrationIds: IntegrationId[];
  schedulerEventIds: SchedulerEventId[];
}

export interface EdgeProfile extends BaseData<EdgeProfileId>, HasTenantId, HasVersion {
  tenantId?: TenantId;
  name: string;
  description?: string;
  default?: boolean;
  rootRuleChainId?: RuleChainId;
  profileData?: EdgeProfileData;
}

export enum EdgeAssignmentSource {
  PROFILE = 'PROFILE',
  EDGE = 'EDGE'
}

export interface EdgeAssignmentRow {
  id: EntityId;
  name: string;
  groupType?: EntityType;
  ownerId?: EntityId;
  ownerName?: string;
  root?: boolean;
  source?: EdgeAssignmentSource;
}

export interface EdgeAssignments {
  edgeProfileId?: EdgeProfileId;
  edgeProfileName?: string;
  rootRuleChainId?: RuleChainId;
  profileRootRuleChainId?: RuleChainId;
  edgesCount: number;
  ruleChains: EdgeAssignmentRow[];
  entityGroups: EdgeAssignmentRow[];
  integrations: EdgeAssignmentRow[];
  schedulerEvents: EdgeAssignmentRow[];
}

export type EdgeAssignmentSectionKey = string;

export interface EdgeAssignmentSection {
  key: EdgeAssignmentSectionKey;
  group: 'entity' | 'logic';
  entityType: EntityType;
  groupType?: EntityType;
  icon: string;
  name: string;
  addLabel: string;
  assignLabel: string;
}

interface GroupSectionMeta {
  icon: string;
  name: string;
  addLabel: string;
  assignLabel: string;
}

// Display metadata per group type. The list of types itself is edgeEntityGroupTypes (shared with the edge pages and
// mirrored by EdgeProfileData.SYNCABLE_GROUP_TYPES on the server): a type added there gets a section here
// automatically, with generic labels until an entry is added to this map.
const GROUP_SECTION_META = new Map<EntityType, GroupSectionMeta>([
  [EntityType.DEVICE, { icon: 'devices_other', name: 'edge-profile.section-device-groups',
    addLabel: 'edge-profile.add-device-group', assignLabel: 'edge.assign-device-group-to-this-edge' }],
  [EntityType.ASSET, { icon: 'domain', name: 'edge-profile.section-asset-groups',
    addLabel: 'edge-profile.add-asset-group', assignLabel: 'edge.assign-asset-group-to-this-edge' }],
  [EntityType.ENTITY_VIEW, { icon: 'visibility', name: 'edge-profile.section-entity-view-groups',
    addLabel: 'edge-profile.add-entity-view-group', assignLabel: 'edge.assign-entity-view-group-to-this-edge' }],
  [EntityType.DASHBOARD, { icon: 'dashboard', name: 'edge-profile.section-dashboard-groups',
    addLabel: 'edge-profile.add-dashboard-group', assignLabel: 'edge.assign-dashboard-group-to-this-edge' }],
  [EntityType.USER, { icon: 'person', name: 'edge-profile.section-user-groups',
    addLabel: 'edge-profile.add-user-group', assignLabel: 'edge.assign-user-group-to-this-edge' }]
]);

// Preferred display order; types missing from it (new ones) are appended in edgeEntityGroupTypes order.
const GROUP_SECTION_ORDER: EntityType[] = [EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.USER];

const groupSectionKey = (groupType: EntityType): EdgeAssignmentSectionKey =>
  groupType.toLowerCase().replace(/_([a-z])/g, (_, c: string) => c.toUpperCase()) + 'Groups';

const groupSection = (groupType: EntityType): EdgeAssignmentSection => {
  const meta = GROUP_SECTION_META.get(groupType) ?? {
    icon: 'folder',
    name: entityTypeTranslations.get(groupType)?.typePlural ?? groupType,
    addLabel: 'edge-profile.add-group',
    assignLabel: 'edge.assign-group-to-this-edge'
  };
  return { key: groupSectionKey(groupType), group: 'entity', entityType: EntityType.ENTITY_GROUP, groupType, ...meta };
};

const orderedGroupTypes: EntityType[] = [
  ...GROUP_SECTION_ORDER.filter(t => edgeEntityGroupTypes.includes(t)),
  ...edgeEntityGroupTypes.filter(t => !GROUP_SECTION_ORDER.includes(t))
];

export const EDGE_ASSIGNMENT_SECTIONS: EdgeAssignmentSection[] = [
  ...orderedGroupTypes.map(groupSection),
  { key: 'ruleChains', group: 'logic', entityType: EntityType.RULE_CHAIN, icon: 'account_tree',
    name: 'edge-profile.section-rule-chains', addLabel: 'edge-profile.add-rule-chain', assignLabel: 'edge.assign-rule-chain-to-this-edge' },
  { key: 'integrations', group: 'logic', entityType: EntityType.INTEGRATION, icon: 'input',
    name: 'edge-profile.section-integrations', addLabel: 'edge-profile.add-integration', assignLabel: 'edge.assign-integration-to-this-edge' },
  { key: 'schedulerEvents', group: 'logic', entityType: EntityType.SCHEDULER_EVENT, icon: 'schedule',
    name: 'edge-profile.section-scheduler-events', addLabel: 'edge-profile.add-scheduler-event', assignLabel: 'edge.assign-scheduler-event-to-this-edge' }
];

export const sectionRows = (assignments: EdgeAssignments, section: EdgeAssignmentSection): EdgeAssignmentRow[] => {
  if (!assignments) {
    return [];
  }
  switch (section.entityType) {
    case EntityType.RULE_CHAIN: return assignments.ruleChains;
    case EntityType.INTEGRATION: return assignments.integrations;
    case EntityType.SCHEDULER_EVENT: return assignments.schedulerEvents;
    case EntityType.ENTITY_GROUP: return assignments.entityGroups.filter(r => r.groupType === section.groupType);
    default: throw new Error(`Unsupported edge assignment section ${section.key}`);
  }
};
