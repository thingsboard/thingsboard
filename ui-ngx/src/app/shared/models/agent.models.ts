// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, GroupEntityInfo, HasId } from '@shared/models/base-data';
import { AgentId } from '@shared/models/id/agent-id';
import { AgentProfileId } from '@shared/models/id/agent-profile-id';
import { AgentApplicationId } from '@shared/models/id/agent-application-id';
import { AgentAppEventId } from '@shared/models/id/agent-app-event-id';
import { AgentAppUnitId } from '@shared/models/id/agent-app-unit-id';
import { AgentAppProfileId } from '@shared/models/id/agent-app-profile-id';
import { AgentBulkActionId } from '@shared/models/id/agent-bulk-action-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';

// --- Enums ---

export enum AgentApplicationType {
  GENERIC = 'GENERIC',
  EDGE = 'EDGE',
  GATEWAY = 'GATEWAY'
}

export const agentApplicationTypeTranslationMap = new Map<AgentApplicationType, string>([
  [AgentApplicationType.GENERIC, 'agent.app-type-generic'],
  [AgentApplicationType.EDGE, 'agent.app-type-edge'],
  [AgentApplicationType.GATEWAY, 'agent.app-type-gateway'],
]);

export enum AgentApplicationOrigin {
  INSTALLED = 'INSTALLED',
  DISCOVERED = 'DISCOVERED',
  AUTO_PROVISIONED = 'AUTO_PROVISIONED'
}

export const agentApplicationOriginTranslationMap = new Map<AgentApplicationOrigin, string>([
  [AgentApplicationOrigin.INSTALLED, 'agent.origin-installed'],
  [AgentApplicationOrigin.DISCOVERED, 'agent.origin-discovered'],
  [AgentApplicationOrigin.AUTO_PROVISIONED, 'agent.origin-auto-provisioned'],
]);

export enum AgentAppEventActionType {
  INSTALL = 'INSTALL',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  RESTART = 'RESTART',
  ROLLBACK = 'ROLLBACK',
  UPGRADE = 'UPGRADE',
  AGENT_UPGRADE = 'AGENT_UPGRADE'
}

export const agentAppEventActionTypeTranslationMap = new Map<AgentAppEventActionType, string>([
  [AgentAppEventActionType.INSTALL, 'agent.event-action-install'],
  [AgentAppEventActionType.UPDATE, 'agent.event-action-update'],
  [AgentAppEventActionType.DELETE, 'agent.event-action-delete'],
  [AgentAppEventActionType.RESTART, 'agent.event-action-restart'],
  [AgentAppEventActionType.ROLLBACK, 'agent.event-action-rollback'],
  [AgentAppEventActionType.UPGRADE, 'agent.event-action-upgrade'],
  [AgentAppEventActionType.AGENT_UPGRADE, 'agent.event-action-agent-upgrade'],
]);

const agentScopedAppEventActionTypes = new Set<AgentAppEventActionType>([
  AgentAppEventActionType.AGENT_UPGRADE
]);

export const isAgentScopedAppEventActionType = (actionType: AgentAppEventActionType): boolean =>
  agentScopedAppEventActionTypes.has(actionType);

export enum AgentProcessingStatus {
  PENDING = 'PENDING',
  QUEUED = 'QUEUED',
  PROCESSING = 'PROCESSING',
  FINISHED = 'FINISHED',
  ERROR = 'ERROR',
  START_FAILED = 'START_FAILED'
}

export const agentProcessingStatusTranslationMap = new Map<AgentProcessingStatus, string>([
  [AgentProcessingStatus.PENDING, 'agent.event-status-pending'],
  [AgentProcessingStatus.QUEUED, 'agent.event-status-queued'],
  [AgentProcessingStatus.PROCESSING, 'agent.event-status-processing'],
  [AgentProcessingStatus.FINISHED, 'agent.event-status-finished'],
  [AgentProcessingStatus.ERROR, 'agent.event-status-error'],
  [AgentProcessingStatus.START_FAILED, 'agent.event-status-start-failed'],
]);

export enum ProcessingStartStatus {
  DELIVERY_FAIL = 'DELIVERY_FAIL',
  PENDING = 'PENDING',
  DELIVERED = 'DELIVERED'
}

export const processingStartStatusTranslationMap = new Map<ProcessingStartStatus, string>([
  [ProcessingStartStatus.DELIVERY_FAIL, 'agent.app-event-execution-delivery-failed'],
  [ProcessingStartStatus.PENDING, 'agent.app-event-execution-not-started'],
  [ProcessingStartStatus.DELIVERED, 'agent.app-event-execution-started'],
]);

export enum AgentAppUnitType {
  CONTAINER = 'CONTAINER',
  VOLUME = 'VOLUME',
  NETWORK = 'NETWORK'
}

export const agentAppUnitTypeTranslationMap = new Map<AgentAppUnitType, string>([
  [AgentAppUnitType.CONTAINER, 'agent.unit-type-container'],
  [AgentAppUnitType.VOLUME, 'agent.unit-type-volume'],
  [AgentAppUnitType.NETWORK, 'agent.unit-type-network'],
]);

export enum AgentAppStepType {
  COMPOSE_TEMPLATE = 'COMPOSE_TEMPLATE',
  COMPOSE = 'COMPOSE',
  COMPOSE_START = 'COMPOSE_START',
  COMPOSE_DOWN = 'COMPOSE_DOWN',
  ROLLBACK = 'ROLLBACK',
  BACKUP_VOLUME = 'BACKUP_VOLUME',
  BACKUP_VOLUME_REMOVE = 'BACKUP_VOLUME_REMOVE',
  COMPOSE_RESTART = 'COMPOSE_RESTART',
  RUN_JOB = 'RUN_JOB',
  AGENT_PREPARE = 'AGENT_PREPARE',
  AGENT_FINALIZE = 'AGENT_FINALIZE'
}

export enum AgentAppConfigType {
  DOCKER_COMPOSE = 'DOCKER_COMPOSE'
}

export enum AgentAppArgumentSource {
  AGENT = 'AGENT',
  OWNER = 'OWNER',
  RELATED_ENTITY = 'RELATED_ENTITY',
  TENANT = 'TENANT',
  DEVICE = 'DEVICE',
  ASSET = 'ASSET',
  CUSTOMER = 'CUSTOMER',
  EDGE = 'EDGE'
}

export const agentAppArgumentSourceTranslationMap = new Map<AgentAppArgumentSource, string>([
  [AgentAppArgumentSource.AGENT, 'agent.argument-source-agent'],
  [AgentAppArgumentSource.OWNER, 'agent.argument-source-owner'],
  [AgentAppArgumentSource.RELATED_ENTITY, 'agent.argument-source-related-entity'],
  [AgentAppArgumentSource.TENANT, 'agent.argument-source-tenant'],
  [AgentAppArgumentSource.DEVICE, 'agent.argument-source-device'],
  [AgentAppArgumentSource.ASSET, 'agent.argument-source-asset'],
  [AgentAppArgumentSource.CUSTOMER, 'agent.argument-source-customer'],
  [AgentAppArgumentSource.EDGE, 'agent.argument-source-edge'],
]);

export interface AgentAppArgumentSourceEntityParams {
  title: string;
  entityType: EntityType;
}

export const agentAppArgumentSourceEntityTypeMap = new Map<AgentAppArgumentSource, AgentAppArgumentSourceEntityParams>([
  [AgentAppArgumentSource.DEVICE, { title: 'agent.argument-source-entity-device', entityType: EntityType.DEVICE }],
  [AgentAppArgumentSource.ASSET, { title: 'agent.argument-source-entity-asset', entityType: EntityType.ASSET }],
  [AgentAppArgumentSource.CUSTOMER, { title: 'agent.argument-source-entity-customer', entityType: EntityType.CUSTOMER }],
  [AgentAppArgumentSource.EDGE, { title: 'agent.argument-source-entity-edge', entityType: EntityType.EDGE }],
]);

export enum AgentAppArgumentValueType {
  ATTRIBUTE = 'ATTRIBUTE',
  LATEST_TELEMETRY = 'LATEST_TELEMETRY'
}

export const agentAppArgumentValueTypeTranslationMap = new Map<AgentAppArgumentValueType, string>([
  [AgentAppArgumentValueType.ATTRIBUTE, 'agent.argument-value-type-attribute'],
  [AgentAppArgumentValueType.LATEST_TELEMETRY, 'agent.argument-value-type-latest-telemetry'],
]);

export enum AgentAppArgumentFormat {
  STRING = 'STRING',
  JSON = 'JSON'
}

export const agentAppArgumentFormatTranslationMap = new Map<AgentAppArgumentFormat, string>([
  [AgentAppArgumentFormat.STRING, 'agent.argument-format-string'],
  [AgentAppArgumentFormat.JSON, 'agent.argument-format-json'],
]);

export interface AgentAppArgument {
  name: string;
  sourceType: AgentAppArgumentSource;
  sourceEntityId?: EntityId;
  valueType: AgentAppArgumentValueType;
  scope?: AttributeScope;
  key: string;
  defaultValue?: string;
  format?: AgentAppArgumentFormat;
}

export enum AgentProvisionType {
  DISABLED = 'DISABLED',
  NO_AUTO_INSTALL = 'NO_AUTO_INSTALL',
  AUTO_INSTALL_PER_APP_TYPE = 'AUTO_INSTALL_PER_APP_TYPE',
  AUTO_INSTALL_PER_APP_PROFILE = 'AUTO_INSTALL_PER_APP_PROFILE'
}

export const agentProvisionTypeTranslationMap = new Map<AgentProvisionType, string>([
  [AgentProvisionType.DISABLED, 'agent.provision-type-disabled'],
  [AgentProvisionType.NO_AUTO_INSTALL, 'agent.provision-type-no-auto-install'],
  [AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE, 'agent.provision-type-per-app-type'],
  [AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE, 'agent.provision-type-per-app-profile'],
]);

export const agentProvisionTypeDescriptionMap = new Map<AgentProvisionType, string>([
  [AgentProvisionType.DISABLED, 'agent.provision-type-disabled-description'],
  [AgentProvisionType.NO_AUTO_INSTALL, 'agent.provision-type-no-auto-install-description'],
  [AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE, 'agent.provision-type-per-app-type-description'],
  [AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE, 'agent.provision-type-per-app-profile-description'],
]);

export const agentProvisionTypeSupportsAppAutoInstall = (type: AgentProvisionType): boolean =>
  type === AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE || type === AgentProvisionType.AUTO_INSTALL_PER_APP_PROFILE;

export enum AgentBulkActionStatus {
  QUEUED = 'QUEUED',
  IN_PROGRESS = 'IN_PROGRESS',
  STARTED = 'STARTED',
  START_FAILED = 'START_FAILED'
}

export const agentBulkActionStatusTranslationMap = new Map<AgentBulkActionStatus, string>([
  [AgentBulkActionStatus.QUEUED, 'agent.bulk-status-queued'],
  [AgentBulkActionStatus.IN_PROGRESS, 'agent.bulk-status-in-progress'],
  [AgentBulkActionStatus.STARTED, 'agent.bulk-status-started'],
  [AgentBulkActionStatus.START_FAILED, 'agent.bulk-status-start-failed'],
]);

// --- Entities ---

export interface Agent extends BaseData<AgentId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name?: string;
  description?: string;
  routingKey?: string;
  secret?: string;
  agentProfileId?: AgentProfileId;
  version?: number;
}

export interface AgentInfo extends Agent, GroupEntityInfo<AgentId> {
  customerTitle: string;
  customerIsPublic: boolean;
  agentProfileName?: string;
  // Derived state (backend may populate from ACTIVITY_STATE telemetry).
  active?: boolean;
  // The image reference the agent reported on its last connect, e.g. 'thingsboard/tb-remote-agent:1.1.0'.
  agentVersion?: string;
  // Resolved server-side from the published version graph; absent when no upgrade applies.
  upgradeTargetImageRef?: string;
}

export interface AgentInstructions {
  instructions: string;
}

export interface AgentProfile extends BaseData<AgentProfileId> {
  tenantId?: TenantId;
  name: string;
  description?: string;
  provisionKey?: string;
  provisionSecret?: string;
  provisionType?: AgentProvisionType;
  default?: boolean;
  version?: number;
}

export type AgentProfileInfo = Omit<AgentProfile, 'provisionKey' | 'provisionSecret'>;

export interface AgentAppConfig {
  type: AgentAppConfigType;
  arguments?: AgentAppArgument[];
}

export interface DockerComposeConfig extends AgentAppConfig {
  compose: any;
  composeType?: string;
}

export function dockerComposeConfig(source: { config?: AgentAppConfig } | null | undefined): DockerComposeConfig | undefined {
  return source?.config as DockerComposeConfig | undefined;
}

export interface AgentApplication extends BaseData<AgentApplicationId> {
  tenantId?: TenantId;
  agentId: AgentId;
  name?: string;
  appType: AgentApplicationType;
  templateVersion?: string;
  desiredTemplateVersion?: string;
  config?: AgentAppConfig;
  version?: number;
  projectName?: string;
  origin?: AgentApplicationOrigin;
  applicationProfileId?: AgentAppProfileId;
  profileConfigVersion?: number;
}

export interface AgentApplicationSaveRequest extends AgentApplication {
  relatedEntityIdNext?: EntityId | null;
  relatedEntityIdPrev?: EntityId | null;
}

export interface AgentApplicationInfo extends AgentApplication {
  currentVersion?: string;
  nextVersion?: string;
  profileConfigOutdated?: boolean;
  profileName?: string;
  profileTemplateVersion?: string;
  agentName?: string;
  relatedEntityId?: EntityId;
}

export interface StepField<T = any> {
  value: T;
  userChoice: boolean;
}

export interface AgentAppStep {
  id: string;
  title: string;
  type: AgentAppStepType;
  templateOnly?: boolean;
  nextId?: string;
  composeTemplates?: Record<string, any>;
  // map keyed by field name: { [field]: { value, userChoice } }
  state?: { [field: string]: StepField };
}

export interface AgentAppStepState {
  [key: string]: any;
}

export interface AgentAppEvent extends BaseData<AgentAppEventId> {
  tenantId?: TenantId;
  applicationId?: AgentApplicationId;
  agentId?: AgentId;
  applicationName?: string;
  actionType: AgentAppEventActionType;
  startStatus: ProcessingStartStatus;
  processingStatus: AgentProcessingStatus;
  currentStepId?: string;
  currentActivity?: string;
  errorMessage?: string;
  updatedTime: number;
  stepStates?: { [stepId: string]: AgentAppStepState };
  bulkActionId?: string;
  resolvedArguments?: { [key: string]: string };
}

export interface AgentAppEventInfo extends AgentAppEvent {
  applicationName?: string;
  agentName?: string;
}

export interface AgentAppInstallResponse {
  application: AgentApplication;
  event: AgentAppEvent;
}

export interface AgentAppUnit extends BaseData<AgentAppUnitId> {
  agentApplicationId: AgentApplicationId;
  identifier: string;
  type: AgentAppUnitType;
  /** Client-side enrichment: SERVER_SCOPE attribute, populated by the UI. */
  image?: string;
  /** Client-side enrichment: SERVER_SCOPE attribute, populated by the UI. */
  state?: string;
}

export interface AgentAppTemplate {
  tenantId?: TenantId;
  appType: AgentApplicationType;
  configType?: AgentAppConfigType;
  currentVersion: string;
  previousVersion?: string;
  nextVersion?: string;
  startSteps?: AgentAppStep[];
  upgradeSteps?: AgentAppStep[];
  deleteSteps?: AgentAppStep[];
  rollbackSteps?: AgentAppStep[];
  restartSteps?: AgentAppStep[];
  version?: number;
}

export interface AgentAppProfile extends BaseData<AgentAppProfileId> {
  tenantId?: TenantId;
  name: string;
  description?: string;
  appType: AgentApplicationType;
  templateVersion: string;
  config?: AgentAppConfig;
  version?: number;
}

export interface AgentAppProfileInfo extends AgentAppProfile {
  templateCurrentVersion?: string;
}

// Placeholder entry shown in profile pickers for a template version the tenant has no profile for yet.
// Not persisted (id is null); selecting it materializes a real profile via the materialize endpoint.
export interface VirtualAgentAppProfile extends AgentAppProfile {
  virtual: true;
  // Compose type the UI picked for this template (see pickComposeType); passed to the materialize endpoint.
  defaultComposeType?: string;
}

export function isVirtualAppProfile(profile: AgentAppProfile | null | undefined): profile is VirtualAgentAppProfile {
  return !!profile && (profile as VirtualAgentAppProfile).virtual === true;
}

export interface AgentAppProfileRelationInfo extends AgentAppProfileInfo {
  agentProfileId?: AgentProfileId;
  assignedApplicationsCount?: number;
  additionalInfo?: any;
}

export interface AgentBulkAction extends BaseData<AgentBulkActionId> {
  tenantId?: TenantId;
  agentProfileId: string;
  applicationProfileId: string;
  actionType: AgentAppEventActionType;
  status: AgentBulkActionStatus;
  errorMsg?: string;
  processingStartedTime?: number;
  total: number;
  submitted: number;
  skipCounts?: { [reason: string]: number };
}

export interface AgentBulkActionEventStats {
  countsByStatus: { [status in AgentProcessingStatus]?: number };
  total: number;
}

// --- Request/Response DTOs ---

export interface AgentAppEventRequest {
  actionType: AgentAppEventActionType;
  application?: AgentApplication;
  stepInputs?: { [stepId: string]: AgentAppStepState };
  bulkActionId?: string;
  skipProfileRefetch?: boolean;
  relatedEntityId?: EntityId;
}

export interface BulkOperationRequest {
  actionType: AgentAppEventActionType;
  stepInputs?: { [stepId: string]: AgentAppStepState };
}

export interface BulkOperationPreview {
  total: number;
  eligible: number;
  skippedCountsByReason: { [reason: string]: number };
  skippedSample: SkippedApp[];
}

export interface SkippedApp {
  agentId?: AgentId;
  agentName?: string;
  applicationId: AgentApplicationId;
  applicationName?: string;
  reason: SkipReason;
  msg?: string;
}

export enum SkipReason {
  VERSION_MISMATCH = 'VERSION_MISMATCH',
  ACTIVE_EVENT = 'ACTIVE_EVENT',
  RATE_LIMIT_EXCEEDED = 'RATE_LIMIT_EXCEEDED',
  ERROR = 'ERROR'
}

export interface BulkOperationResult {
  total: number;
  submitted: number;
  skipped: SkippedApp[];
}
