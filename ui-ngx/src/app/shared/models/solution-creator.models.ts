// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { CalculatedFieldType } from '@shared/models/calculated-field.models';
import { AlarmSeverity } from '@shared/models/alarm.models';
import { AiAction, ToolExecutionRequested, ToolExecutionResult, AiResourceInfo } from '@shared/models/ai-chat.models';

export const NEW_SOLUTION_ID = 'new';

export interface SolutionInfo {
  id: string;
  createdTime?: number;
  solutionTitle?: string;
  installed?: boolean;
  built?: boolean;
  pinned?: boolean;
  status?: 'RUNNING' | 'NEEDS_YOU' | 'FAILED';
  statusReason?: string;
}

export type SolutionOperation = 'INITIAL_CONFIGURATION' | 'DASHBOARDS_CONFIGURATION' | 'CREATE' | 'INSTALL';

export interface SolutionEstimate {
  credits: number;
  seconds: number;
}

export interface SolutionCreatorInfo {
  id: string;
  createdTime: number;
  states: Record<SolutionStep, SolutionStepState>;
  data: SolutionData;
  metadata?: SolutionMetadata;
  estimates?: Partial<Record<SolutionOperation, SolutionEstimate>>;
}

export interface SolutionProgressItem {
  type: EntityType;
  name?: string;
  state: 'WAIT' | 'WORKING' | 'DONE';
  profile?: string;
  customer?: string;
}

export interface SolutionInstallProgress {
  running: boolean;
  phase?: string;
  entityResults: Partial<Record<EntityType, EntityResult[]>>;
  stage?: 'CREATE' | 'INSTALL';
  items?: SolutionProgressItem[];
}

export interface SolutionUpdateRequest {
  title?: string;
  pinned?: boolean;
  step?: SolutionStep;
  feedback?: {answer: string; rating?: 'UP' | 'DOWN' | 'NONE'; problem?: string; comment?: string};
}

export interface AiQuestionOption {
  label: string;
  hint?: string;
}

export interface AiQuestion {
  id: string;
  label?: string;
  question: string;
  why?: string;
  options: AiQuestionOption[];
  recommended?: string | string[];
  multiple?: boolean;
}

export interface AiQuestionAnswer {
  question: AiQuestion;
  answer: string;
}

export interface DashboardsOverview {
  name: string;
  assignedTo: string;
  overview?: string;
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
  installedTime?: number;
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

export type ChatMessage = GeneralChatMessage | ApprovalChatMessage | ActionChatMessage;

export interface AiStartedFrom {
  type: string;
  entityId?: EntityId;
}

export interface GeneralChatMessage {
  from: 'USER' | 'AI' | 'SYSTEM' | 'GENERATE';
  content: string;
  references?: Array<{name: string; entityType: string; id: string}>;
  status?: 'INCOMPLETE' | 'STOPPED';
  rating?: 'UP' | 'DOWN';
  reported?: boolean;
  startedFrom?: AiStartedFrom;
  resources?: AiResourceInfo[];
  time?: number;
  questions?: AiQuestion[];
  answers?: AiQuestionAnswer[];
  done?: boolean;
  lead?: string;
  note?: string;
  entities?: EntityId[];
  entitiesMeta?: string;
  hint?: {lead: string; text: string};
  chips?: Array<{id: string; icon: string; label: string}>;
}

export interface ActionChatMessage {
  from: 'ACTION';
  data: AiAction;
}

export interface ApprovalChatMessage {
  from: 'APPROVAL';
  data: ToolExecutionRequested;
  result?: ToolExecutionResult;
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
