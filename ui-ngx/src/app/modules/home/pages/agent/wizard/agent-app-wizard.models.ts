// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEvent,
  AgentApplicationType,
  AgentInfo,
  AgentApplicationInfo
} from '@shared/models/agent.models';

export interface AgentAppInstallWizardData {
  agentId?: string;
  agent?: AgentInfo | null;
  mode?: 'install' | 'update' | 'upgrade';
  application?: AgentApplicationInfo;
  lockedType?: AgentApplicationType;
  lockedRelatedEntity?: EntityId;
  navigateToAgentOnFinish?: boolean;
  selectAgent?: boolean;
  showBack?: boolean;
}

export interface AgentAppUpgradeResult {
  profileOnly: true;
}

export type AgentAppInstallWizardResult = AgentAppEvent | AgentAppUpgradeResult | null;

export function isAgentAppUpgradeResult(result: AgentAppInstallWizardResult): result is AgentAppUpgradeResult {
  return !!result && (result as AgentAppUpgradeResult).profileOnly === true;
}

// Emitted by a flow component when its work is done. The dispatcher maps this
// to the dialog/embedded completion (close + optional navigate / progress).
export interface AgentAppWizardFinish {
  event: AgentAppEvent | null;
  application?: AgentApplication | null;
  // Deploy-status step: just close with the event; navigation already done by
  // the flow's goToAgent/goToAgentApplication/goToAgentEvents.
  closeOnly?: boolean;
}

export type AgentAppInstallStepKey = 'agent' | 'type' | 'config' | 'summary' | 'deploy';

export const AGENT_APP_INSTALL_STEP_LABELS: Record<AgentAppInstallStepKey, string> = {
  agent: 'agent.deploy-select-agent-step',
  type: 'agent.app-install-step1',
  config: 'agent.app-install-step3',
  summary: 'agent.app-install-step4',
  deploy: 'agent.deploy-install-step'
};

export type AgentAppUpgradeStepKey = 'profile' | 'config' | 'summary';

export const AGENT_APP_UPGRADE_STEP_LABELS: Record<AgentAppUpgradeStepKey, string> = {
  profile: 'agent.app-upgrade-profile-step',
  config: 'agent.app-install-step3',
  summary: 'agent.app-install-step4'
};

export type AgentAppProfileStepKey = 'type' | 'config' | 'save';

export const AGENT_APP_PROFILE_STEP_LABELS: Record<AgentAppProfileStepKey, string> = {
  type: 'agent.app-install-step1',
  config: 'agent.app-install-step3',
  save: 'agent.app-profile-step-save'
};

export interface AgentTypeCard {
  type: AgentApplicationType;
  icon: string;
  labelKey: string;
  descKey: string;
}
