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

export interface AgentTypeCard {
  type: AgentApplicationType;
  icon: string;
  labelKey: string;
  descKey: string;
}
