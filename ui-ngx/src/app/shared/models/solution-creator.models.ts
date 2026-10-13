// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { CalculatedFieldType } from '@shared/models/calculated-field.models';
import { AlarmSeverity } from '@shared/models/alarm.models';
import { ToolExecutionRequested } from '@shared/models/ai-chat.models';

export interface SolutionInfo {
  id: string;
  solutionTitle?: string;
  installed?: boolean;
  built?: boolean;
}

export interface SolutionCreatorInfo {
  id: string;
  createdTime: number;
  states: Record<SolutionStep, SolutionStepState>;
  data: SolutionData;
  metadata?: SolutionMetadata;
}

export interface DashboardsOverview {
  name: string;
  assignedTo: string;
  description: string;
}

export interface SolutionData {
  solutionDescriptor?: SolutionDescriptor;
  dashboardsOverview?: Array<DashboardsOverview>;
  dashboards?: Dashboard & {assignedTo: string};
  solutionTitle?: string;
  solutionDescription?: string;
  entities?: any;
}

export interface SolutionMetadata {
  installed: boolean;
  built: boolean;
  installResult?: SolutionInstallResult;
}

export interface SolutionInstallResult {
  mainDashboardId?: DashboardId;
  createdEntities: Array<BaseData<EntityId>>;
  entityResults?: Record<EntityType, Array<EntityResult>>;
}

export interface EntityResult {
  additionalData: {
    name: string;
    description: string;
    type?: string;
    groupType?: string;
    customer?: string;
    email?: string;
    password?: string;
    userId?: string;
    condition?: string;
    severities?: Array<AlarmSeverity>;
  };
  id: EntityId;
  status: "OK" | "WARN" | "ERROR";
  error?: {
    errorType: "AI" | "TB";
    message?: string;
  };
}

export type SolutionDataKey = AllKeyOf<SolutionData>;
export type SolutionDataValues = $Values<SolutionData>;


export enum SolutionStep {
  INITIAL_CONFIGURATION = 'INITIAL_CONFIGURATION',
  DASHBOARDS_CONFIGURATION = 'DASHBOARDS_CONFIGURATION'
}

export interface SolutionDescriptorIam {
  name: string;
  permissions: string;
  description: string;
}

export interface SolutionDescriptorAlarm {
  alarmName: string;
  condition: string;
  profileNames: string[];
  argumentTypes: Record<string, any>;
  severities: AlarmSeverity[];
}

export interface SolutionDescriptorMetric {
  calculatedFieldName: string;
  calculation: string;
  profileName: string;
  metricType: keyof AiMetricType;
}

export interface SolutionDescriptorEntityBaseTelemetry {
  key: string;
  dataType: string;
  telemetryType: "ATTRIBUTE" | "TIMESERIES";
  classifier?: string;
  enumValues?: string[];
  units?: string;
  calculatedFieldName?: string;
}

export interface SolutionDescriptorEntityBaseRelation {
  type: string;
  cardinality?: string;
  parent: string;
  child: string;
}

export interface SolutionDescriptorEntityBase {
  name: string;
  entityType: string;
  telemetry?: Array<SolutionDescriptorEntityBaseTelemetry>;
  scope?: string;
  description?: string;
}

export interface SolutionDescriptorAssetProfiles extends SolutionDescriptorEntityBase {
  scope: string;
}

export type SolutionDescriptorDeviceProfiles = SolutionDescriptorAssetProfiles

export interface SolutionDescriptorCustomerProfiles extends SolutionDescriptorEntityBase {
  description: string;
}

export interface SolutionDescriptorUserProfiles extends SolutionDescriptorCustomerProfiles {
  scope: string;
}

export type SolutionEntity = SolutionDescriptorAssetProfiles & SolutionDescriptorCustomerProfiles & SolutionDescriptorUserProfiles;

export interface SolutionDescriptorEntityProfiles {
  deviceProfiles?: Array<SolutionDescriptorDeviceProfiles>;
  assetProfiles?: Array<SolutionDescriptorAssetProfiles>;
  customerProfiles?: Array<SolutionDescriptorCustomerProfiles>;
  userProfiles?: Array<SolutionDescriptorUserProfiles>;
}

export interface SolutionDescriptor {
  entityProfiles: SolutionDescriptorEntityProfiles;
  iam?: Array<SolutionDescriptorIam>;
  metrics?: Array<SolutionDescriptorMetric>;
  relations?: Array<SolutionDescriptorEntityBaseRelation>;
  alarms?: Array<SolutionDescriptorAlarm>;
}

export enum SolutionStepStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  READY = 'READY'
}


export interface SolutionStepState {
  chatId: string;
  status: SolutionStepStatus;
  messages: ChatMessage[];
  pendingChanges?: string[];
  skipInterviewAllowed?: boolean;
}

export type ChatMessage = GeneralChatMessage | ApprovalChatMessage;

export interface GeneralChatMessage {
  from: 'USER' | 'AI' | 'SYSTEM' | 'GENERATE';
  content: string;
}

export interface ApprovalChatMessage {
  from: 'APPROVAL';
  data: ToolExecutionRequested;
}

export enum AiMetricType {
  math = CalculatedFieldType.SIMPLE,
  script = CalculatedFieldType.SCRIPT,
  delta = CalculatedFieldType.SCRIPT,
  tsAggregation = CalculatedFieldType.ENTITY_AGGREGATION,
  relatedAggregation = CalculatedFieldType.RELATED_ENTITIES_AGGREGATION,
  propagate = CalculatedFieldType.PROPAGATION,
  geofencing = CalculatedFieldType.GEOFENCING
}
