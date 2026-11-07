// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { ExtensionId } from '@shared/models/id/extension-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';
import { baseUrl } from '@core/utils';

export enum ExtensionState {
  PROVISIONING = 'PROVISIONING',
  ACTIVE = 'ACTIVE',
  UPDATING = 'UPDATING',
  DEGRADED = 'DEGRADED',
  FAILED = 'FAILED',
  STOPPED = 'STOPPED',
  DELETING = 'DELETING'
}

export const extensionStateTranslations = new Map<ExtensionState, string>([
  [ExtensionState.PROVISIONING, 'extension.state-provisioning'],
  [ExtensionState.ACTIVE, 'extension.state-active'],
  [ExtensionState.UPDATING, 'extension.state-updating'],
  [ExtensionState.DEGRADED, 'extension.state-degraded'],
  [ExtensionState.FAILED, 'extension.state-failed'],
  [ExtensionState.STOPPED, 'extension.state-stopped'],
  [ExtensionState.DELETING, 'extension.state-deleting']
]);

export const extensionStateColors = new Map<ExtensionState, string>([
  [ExtensionState.PROVISIONING, '#1976D2'],
  [ExtensionState.ACTIVE, '#1B5E20'],
  [ExtensionState.UPDATING, '#1976D2'],
  [ExtensionState.DEGRADED, '#E65100'],
  [ExtensionState.FAILED, '#B71C1C'],
  [ExtensionState.STOPPED, '#616161'],
  [ExtensionState.DELETING, '#616161']
]);

export const extensionStateBackgroundColors = new Map<ExtensionState, string>([
  [ExtensionState.PROVISIONING, 'rgba(25, 118, 210, 0.12)'],
  [ExtensionState.ACTIVE, 'rgba(27, 94, 32, 0.12)'],
  [ExtensionState.UPDATING, 'rgba(25, 118, 210, 0.12)'],
  [ExtensionState.DEGRADED, 'rgba(230, 81, 0, 0.12)'],
  [ExtensionState.FAILED, 'rgba(183, 28, 28, 0.12)'],
  [ExtensionState.STOPPED, 'rgba(97, 97, 97, 0.12)'],
  [ExtensionState.DELETING, 'rgba(97, 97, 97, 0.12)']
]);

export enum ResourceSize {
  XSMALL = 'XSMALL',
  SMALL = 'SMALL',
  MEDIUM = 'MEDIUM',
  LARGE = 'LARGE',
  XLARGE = 'XLARGE',
  XXLARGE = 'XXLARGE'
}

// Must mirror ResourceSize.DEFAULT on the backend, which is deliberately the smallest tier so it fits
// the default tenant-profile quota. Preselecting a bigger tier here makes the create form fail the
// quota check out of the box on a stock install. The catalog endpoint marks the real default with
// `isDefault`, so a form can also read it from there once loaded.
export const defaultResourceSize = ResourceSize.SMALL;

export interface ExtensionResourceSizeInfo {
  name: ResourceSize;
  cpuRequest: string;
  cpuLimit: string;
  memoryRequest: string;
  memoryLimit: string;
  isDefault: boolean;
}

// The resource-size catalog returns CPU as Kubernetes quantities ('250m', '1').
// The UI shows CPU in cores everywhere else (quota footer, tenant profile form),
// so convert millicore-suffixed values for display: '250m' -> '0.25'.
export function cpuQuantityToCores(quantity: string): string {
  if (quantity?.endsWith('m')) {
    const millicores = Number(quantity.slice(0, -1));
    if (!isNaN(millicores)) {
      return String(millicores / 1000);
    }
  }
  return quantity;
}

// How many more single-replica extensions of one tier fit; count === null means unlimited.
export interface ExtensionSizeAvailability {
  size: ResourceSize;
  count: number | null;
}

// Tenant extension limits with the used and the still available part; a null max means unlimited.
export interface ExtensionQuota {
  maxExtensions: number | null;
  usedExtensions: number;
  maxCpuMillicores: number | null;
  usedCpuMillicores: number;
  maxMemoryMb: number | null;
  usedMemoryMb: number;
  availableBySize: Array<ExtensionSizeAvailability>;
}

export interface RegistryAuth {
  registry?: string;
  username: string;
  password: string;
}

export interface ResourceSpec {
  size: ResourceSize;
}

export interface ExtensionContainerSpec {
  image: string;
  tag: string;
  registryAuth?: RegistryAuth;
  replicas: number;
  port: number;
  env?: {[key: string]: string};
  resources?: ResourceSpec;
}

export interface Extension extends Omit<BaseData<ExtensionId>, 'label'>, HasTenantId, HasVersion, ExportableEntity<ExtensionId> {
  tenantId?: TenantId;
  name: string;
  slug?: string;
  slugBase?: string;
  state?: ExtensionState;
  containerSpec: ExtensionContainerSpec;
  lastError?: string;
  lastErrorAt?: number;
  additionalInfo?: any;
  // list projection (ExtensionInfo) extras
  url?: string;
  readyReplicas?: number;
  desiredReplicas?: number;
}

// Route prefix of the extension data plane; mirrors extensions.routing.base-path on the server
// and is only used as a fallback when the server did not send the route itself.
export const extensionRouteBasePath = '/api/extension/route';

/**
 * Route of an extension without the domain, e.g. /api/extension/route/acme-billing. The server sends
 * it on the list (info) view only; on the details view it is rebuilt from the slug.
 */
export function extensionUrlPath(extension: {url?: string; slug?: string}): string {
  return extension?.url || (extension?.slug ? `${extensionRouteBasePath}/${extension.slug}` : '');
}

/**
 * Absolute URL of an extension. The server returns the route as a path only, so the domain of the
 * current ThingsBoard origin is prepended — the same way integrations build their HTTP endpoint —
 * to get a URL that can be copied and called as is.
 */
export function extensionFullUrl(extension: {url?: string; slug?: string}): string {
  const path = extensionUrlPath(extension);
  return path ? `${baseUrl()}${path}` : '';
}

export interface ExtensionStatus {
  extensionId: ExtensionId;
  slug: string;
  state: ExtensionState;
  desiredReplicas?: number;
  readyReplicas?: number;
  podPhaseCounts?: {[phase: string]: number};
  url?: string;
  lastError?: string;
  lastErrorAt?: number;
  snapshotAt?: number;
}
