// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AgentApplication,
  AgentApplicationOrigin,
  AgentApplicationType,
  AgentAppProfile,
  AgentAppTemplate,
  AgentAppConfigType,
  DockerComposeConfig,
  dockerComposeConfig
} from '@shared/models/agent.models';
import { AgentId } from '@shared/models/id/agent-id';
import { applyCredentialValuesToCompose } from '@home/pages/agent/util/agent-credentials';
import { parseComposeYaml } from '@home/pages/agent/util/agent-compose-yaml';

export interface UpgradePayloadOpts {
  existingApplication: AgentApplication;
  template: AgentAppTemplate;
  // Profile-bound upgrade: compose comes from the existing app and only the
  // edited credentials are written back. Otherwise the user-edited YAML is used.
  profileBound: boolean;
  selectedType: AgentApplicationType | null;
  credentialValues: Record<string, string>;
  composeYaml: string;
  mergedApp: AgentApplication | null;
}

export function buildUpgradeApplication(opts: UpgradePayloadOpts): AgentApplication {
  let outboundCompose: any;
  if (opts.profileBound) {
    outboundCompose = dockerComposeConfig(opts.existingApplication)?.compose;
    if (outboundCompose) {
      applyCredentialValuesToCompose(outboundCompose, opts.selectedType, opts.credentialValues);
    }
  } else {
    outboundCompose = parseComposeYaml(opts.composeYaml, dockerComposeConfig(opts.mergedApp)?.compose);
  }
  return {
    ...opts.existingApplication,
    templateVersion: opts.template.currentVersion,
    config: composeConfig(opts.existingApplication, outboundCompose)
  };
}

/** Carries the existing config's own fields forward while replacing the compose document. */
function composeConfig(source: AgentApplication | null | undefined, compose: any,
                       composeType?: string): DockerComposeConfig {
  const existing = dockerComposeConfig(source);
  return {
    ...existing,
    type: existing?.type ?? AgentAppConfigType.DOCKER_COMPOSE,
    compose,
    ...(composeType !== undefined ? { composeType } : {})
  };
}

export interface UpdatePayloadOpts {
  existingApplication: AgentApplication;
  appName: string;
  composeYaml: string;
  mergedApp: AgentApplication | null;
  composeType?: string;
}

export function buildUpdateApplication(opts: UpdatePayloadOpts): AgentApplication {
  return {
    ...opts.existingApplication,
    name: opts.appName.trim(),
    config: composeConfig(opts.existingApplication,
      parseComposeYaml(opts.composeYaml, dockerComposeConfig(opts.mergedApp)?.compose), opts.composeType)
  };
}

export interface InstallPayloadOpts {
  selectedType: AgentApplicationType | null;
  agentId: string;
  appName: string;
  composeYaml: string;
  mergedApp: AgentApplication | null;
  template: AgentAppTemplate | null;
  useProfile: boolean;
  selectedProfile: AgentAppProfile | null;
  composeType?: string;
}

export function buildInstallApplication(opts: InstallPayloadOpts): AgentApplication {
  const compose = () => parseComposeYaml(opts.composeYaml, dockerComposeConfig(opts.mergedApp)?.compose);
  const agentId = new AgentId(opts.agentId);
  let application: AgentApplication;
  if (opts.selectedType === AgentApplicationType.GENERIC) {
    application = {
      name: opts.appName.trim(),
      appType: AgentApplicationType.GENERIC,
      agentId,
      templateVersion: opts.template?.currentVersion,
      config: composeConfig(null, compose(), opts.composeType),
      origin: AgentApplicationOrigin.INSTALLED
    };
  } else {
    const base: AgentApplication = opts.mergedApp || {
      agentId,
      appType: opts.selectedType,
      templateVersion: opts.template?.currentVersion,
      origin: AgentApplicationOrigin.INSTALLED
    };
    application = {
      ...base,
      name: opts.appName.trim(),
      config: composeConfig(opts.mergedApp, compose(), opts.composeType)
    };
  }
  // Attach profile reference if using a profile-based install.
  if (opts.useProfile && opts.selectedProfile) {
    application.applicationProfileId = opts.selectedProfile.id;
    application.templateVersion = opts.selectedProfile.templateVersion;
  }
  return application;
}
