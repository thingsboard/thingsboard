/**
 * Asset Profile Type Definitions
 * Mirrors ThingsBoard Java implementation
 * Based on: common/data/src/main/java/org/thingsboard/server/common/data/asset/AssetProfile.java
 */

import { EntityId, TenantId, RuleChainId, DashboardId } from './device.types'

// ==================== ID Types ====================

export interface AssetProfileId extends EntityId {
  entityType: 'ASSET_PROFILE'
}

// ==================== Asset Profile ====================

export interface AssetProfile {
  id?: AssetProfileId
  createdTime?: number
  tenantId: TenantId
  name: string
  description?: string
  image?: string // URL or Base64 data
  isDefault: boolean
  defaultRuleChainId?: RuleChainId
  defaultDashboardId?: DashboardId
  defaultQueueName?: string
  defaultEdgeRuleChainId?: RuleChainId
  externalId?: AssetProfileId
  version?: number
}

// ==================== Asset Profile Info (for lists) ====================

export interface AssetProfileInfo {
  id: AssetProfileId
  tenantId: TenantId
  name: string
  image?: string
  defaultDashboardId?: DashboardId
}

// ==================== Helper Functions ====================

export function createDefaultAssetProfile(): AssetProfile {
  return {
    tenantId: { id: '', entityType: 'TENANT' },
    name: '',
    description: '',
    isDefault: false,
  }
}

export function isValidAssetProfile(profile: AssetProfile): boolean {
  return profile.name.trim().length > 0
}

export function getAssetProfileDisplayName(profile: AssetProfile | AssetProfileInfo): string {
  return profile.name || 'Unnamed Profile'
}
