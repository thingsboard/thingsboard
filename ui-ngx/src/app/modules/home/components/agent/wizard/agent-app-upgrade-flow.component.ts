// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { Observable } from 'rxjs';
import { PageComponent } from '@shared/components/page.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  AgentInfo,
  AgentAppConfigType,
  dockerComposeConfig,
  AgentApplicationInfo,
  DockerComposeConfig
} from '@shared/models/agent.models';
import { extractCredentialValues } from '@home/pages/agent/util/agent-credentials';
import { classifyStepsForAction } from '@home/pages/agent/util/agent-app-steps';
import { buildStepInputs, createStepBinding, StepBinding } from '@home/pages/agent/util/agent-app-step-inputs';
import { buildUpgradeApplication } from '@home/pages/agent/util/agent-app-payloads';
import { buildUpdateMergeDraft } from '@home/pages/agent/util/agent-app-compose-preview';
import { dumpCompose, dumpRawTemplateCompose } from '@home/pages/agent/util/agent-compose-yaml';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import * as YAML from 'yaml';
import { AgentAppWizardLoaderService } from '@home/pages/agent/wizard/agent-app-wizard-loader.service';
import { AgentAppWizardSubmitService } from '@home/pages/agent/wizard/agent-app-wizard-submit.service';
import { AgentAppUpgradeResult, AgentAppWizardFinish } from '@home/pages/agent/wizard/agent-app-wizard.models';

@Component({
  selector: 'tb-agent-app-upgrade-flow',
  templateUrl: './agent-app-upgrade-flow.component.html',
  styleUrls: ['./agent-app-upgrade-flow.component.scss'],
  standalone: false
})
export class AgentAppUpgradeFlowComponent extends PageComponent implements OnInit {

  @Input() agentId: string;
  @Input() agent: AgentInfo;
  @Input() existingApplication: AgentApplicationInfo;
  @Input() lockedRelatedEntity: EntityId | null = null;
  @Input() embedded = false;
  @Input() showBack = false;
  @Input() stepperOrientation: Observable<StepperOrientation>;
  @Input() stepperLabelPosition: Observable<'bottom' | 'end'>;

  @Output() finished = new EventEmitter<AgentAppWizardFinish>();
  @Output() cancelled = new EventEmitter<AgentAppUpgradeResult | null>();
  @Output() back = new EventEmitter<void>();

  fromVersion: string | null = null;
  toVersion: string | null = null;

  profile: AgentAppProfile | null = null;
  mergedProfile: AgentAppProfile | null = null;
  profileProposedYaml = '';
  profileCurrentYaml = '';
  profileComposeYaml = '';
  private profileMergeRequested = false;
  loadingProfile = false;
  profileLoadError = '';
  profileUpgrading = false;
  profileUpgraded = false;
  showProfileUpgradeStep = false;

  @ViewChild('stepper', { static: false })
  stepper: MatStepper;

  bindings: StepBinding[] = [];

  proposedYaml = '';
  currentYaml = '';
  composeType: string | null = null;
  diffSyncScroll = false;

  selectedType: AgentApplicationType | null = null;
  template: AgentAppTemplate | null = null;
  loadingTemplate = false;
  loadError = '';

  appName = '';
  composeYaml = '';

  mergedApp: AgentApplication | null = null;
  submitting = false;

  credentialValues: Record<string, string> = {};

  relatedEntityId: EntityId | null = null;

  constructor(private translate: TranslateService,
              private agentService: AgentService,
              private loader: AgentAppWizardLoaderService,
              private submitSvc: AgentAppWizardSubmitService) {
    super();
  }

  // Profile-bound upgrade: compose comes from the profile, the user only edits
  // credentials. Steps (backup volumes, pull images) still apply.
  get isProfileBoundUpgrade(): boolean {
    return !!this.existingApplication?.applicationProfileId;
  }

  // True when the linked profile still points to the same template as the
  // application — the profile hasn't moved to nextVersion yet, so the wizard
  // surfaces a profile-upgrade step before Review & Customize.
  get needsProfileUpgrade(): boolean {
    return !!this.profile
      && !!this.existingApplication?.templateVersion
      && this.profile.templateVersion === this.existingApplication.templateVersion;
  }

  get canHaveRelatedEntity(): boolean {
    const type = this.selectedType || this.existingApplication?.appType;
    return type === AgentApplicationType.EDGE || type === AgentApplicationType.GATEWAY;
  }

  get relatedEntityShown(): boolean {
    return this.canHaveRelatedEntity && this.isProfileBoundUpgrade;
  }

  get relatedEntityRequired(): boolean {
    return this.relatedEntityShown;
  }

  ngOnInit() {
    this.relatedEntityId = this.lockedRelatedEntity || this.existingApplication?.relatedEntityId || null;
    this.appName = this.existingApplication.name;
    this.selectedType = this.existingApplication.appType;
    this.fromVersion = this.existingApplication.currentVersion || null;
    this.loadingTemplate = true;
    this.resolveUpgradeTemplate();
    const profileId = this.existingApplication.applicationProfileId?.id;
    if (profileId) {
      this.resolveProfile(profileId);
    }
  }

  private resolveUpgradeTemplate() {
    this.loader.resolveUpgradeTemplate(this.existingApplication).subscribe({
      next: ({ template, sourceTemplate, fromVersion }) => {
        // The detail endpoint doesn't populate currentVersion on the
        // application, so fromVersion is usually null coming in from ngOnInit.
        // Adopt the value resolved from the linked template so the "from → to"
        // row doesn't show a dash.
        if (!this.fromVersion && fromVersion) {
          this.fromVersion = fromVersion;
        }
        this.applyUpgradeTemplate(template, sourceTemplate);
      },
      error: e => this.failUpgradeLoad(e?.messageKey || 'agent.app-upgrade-load-failed')
    });
  }

  private applyUpgradeTemplate(template: AgentAppTemplate, sourceTemplate: AgentAppTemplate) {
    this.template = template;
    this.toVersion = template.currentVersion || null;
    this.initBindings(sourceTemplate);

    this.composeType = dockerComposeConfig(this.existingApplication)?.composeType || null;
    this.proposedYaml = dumpRawTemplateCompose(template, this.composeType || undefined);
    // Profile-bound upgrade: compose comes from the profile and the user only
    // edits credentials, so no app-level merge — the image bump rides on the
    // profile-upgrade step.
    if (this.isProfileBoundUpgrade) {
      this.currentYaml = dumpCompose(this.existingApplication);
      this.composeYaml = this.currentYaml;
      const compose: any = dockerComposeConfig(this.existingApplication)?.compose;
      this.credentialValues = extractCredentialValues(compose, this.selectedType);
      this.loadingTemplate = false;
      this.maybeInitProfileDiff();
      return;
    }
    this.runMergeForPreview(template);
  }

  private runMergeForPreview(template: AgentAppTemplate) {
    const draft = buildUpdateMergeDraft(this.existingApplication, template);
    this.loader.merge(template.currentVersion, template.appType, draft, undefined, undefined, AgentAppEventActionType.UPGRADE).subscribe({
      next: merged => {
        this.mergedApp = merged;
        this.currentYaml = dumpCompose(merged);
        this.composeYaml = this.currentYaml;
        this.loadingTemplate = false;
        this.maybeInitProfileDiff();
      },
      error: () => this.failUpgradeLoad('agent.app-upgrade-load-failed')
    });
  }

  private resolveProfile(profileId: string) {
    this.loadingProfile = true;
    this.loader.loadProfileById(profileId).subscribe({
      next: prof => {
        this.profile = prof;
        this.loadingProfile = false;
        this.maybeInitProfileDiff();
      },
      error: () => {
        this.profileLoadError = this.translate.instant('agent.app-profile-load-failed');
        this.loadingProfile = false;
      }
    });
  }

  private maybeInitProfileDiff() {
    if (!this.needsProfileUpgrade || !this.template || !this.profile || this.profileMergeRequested) { return; }
    this.profileMergeRequested = true;
    this.showProfileUpgradeStep = true;
    const profileComposeType = dockerComposeConfig(this.profile)?.composeType || undefined;
    this.profileProposedYaml = dumpRawTemplateCompose(this.template, profileComposeType);
    const draft = { ...this.profile, templateVersion: this.template.currentVersion } as AgentAppProfile;
    this.agentService.mergeProfileForPreview(
      this.template.currentVersion, draft, undefined, AgentAppEventActionType.UPGRADE
    ).subscribe({
      next: merged => {
        this.mergedProfile = merged;
        this.profileCurrentYaml = dumpCompose(merged);
        this.profileComposeYaml = this.profileCurrentYaml;
      },
      error: () => {
        // Best-effort image bump; fall back to the existing profile compose.
        this.profileCurrentYaml = dumpCompose(this.profile);
        this.profileComposeYaml = this.profileCurrentYaml;
      }
    });
  }

  canSubmitProfileUpgrade(): boolean {
    return !!this.profile && !!this.template
      && !this.profileUpgrading && !this.profileUpgraded
      && !this.loadError && !this.profileLoadError
      && !this.isProfileComposeYamlInvalid();
  }

  isProfileComposeYamlInvalid(): boolean {
    if (!this.profileComposeYaml?.trim()) { return false; }
    try {
      YAML.parse(this.profileComposeYaml);
      return false;
    } catch (_) {
      return true;
    }
  }

  submitProfileUpgrade() {
    if (!this.canSubmitProfileUpgrade()) { return; }
    let compose: any;
    if (this.profileComposeYaml?.trim()) {
      try {
        compose = YAML.parse(this.profileComposeYaml);
      } catch (_) {
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('agent.app-compose-invalid-yaml'),
          type: 'error',
          duration: 3000,
          verticalPosition: 'bottom',
          horizontalPosition: 'left'
        }));
        return;
      }
    } else {
      compose = dockerComposeConfig(this.mergedProfile)?.compose
        || dockerComposeConfig(this.profile)?.compose
        || { services: {} };
    }
    this.profileUpgrading = true;
    const updated: AgentAppProfile = {
      ...this.profile,
      templateVersion: this.template.currentVersion,
      config: {
        ...(this.profile.config || { type: AgentAppConfigType.DOCKER_COMPOSE }),
        compose
      } as DockerComposeConfig
    };
    this.agentService.saveAgentAppProfile(updated).subscribe({
      next: saved => {
        this.profile = saved;
        // Reflect the committed compose, then lock the diff (its readOnly is
        // bound to profileUpgraded in the template).
        this.profileCurrentYaml = this.profileComposeYaml;
        this.profileUpgraded = true;
        this.profileUpgrading = false;
        // Defer so the linear stepper picks up the just-set [completed]
        // binding before stepper.next() decides whether it can advance.
        setTimeout(() => this.stepper?.next(), 0);
      },
      error: () => {
        this.profileUpgrading = false;
      }
    });
  }

  private failUpgradeLoad(messageKey: string) {
    this.loadError = this.translate.instant(messageKey);
    this.loadingTemplate = false;
  }

  private initBindings(template: AgentAppTemplate) {
    this.bindings = classifyStepsForAction(template, AgentAppEventActionType.UPGRADE)
      .map(cs => createStepBinding(cs, this.existingApplication));
  }

  get hasBackupVolumeInput(): boolean {
    return this.bindings.some(b => b.kind === 'backupVolume');
  }

  get selectedBackupVolumeCount(): number {
    let count = 0;
    for (const b of this.bindings) {
      if (b.kind === 'backupVolume' && b.backupVolumes) {
        count += b.backupVolumes.filter(v => v.selected).length;
      }
    }
    return count;
  }

  canSubmit(): boolean {
    if (this.relatedEntityRequired && !this.relatedEntityId?.id) {
      return false;
    }
    return !!this.template && !!this.existingApplication && !this.submitting && !this.loadError;
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting = true;
    const application = buildUpgradeApplication({
      existingApplication: this.existingApplication,
      template: this.template!,
      profileBound: this.isProfileBoundUpgrade,
      selectedType: this.selectedType,
      credentialValues: this.credentialValues,
      composeYaml: this.composeYaml,
      mergedApp: this.mergedApp
    });
    const stepInputs = buildStepInputs(this.bindings);
    this.submitSvc.upgrade(this.existingApplication.id.id, application, stepInputs).subscribe({
      next: event => this.finished.emit({ application: this.existingApplication, event }),
      error: () => {
        this.submitting = false;
      }
    });
  }

  cancel() {
    // If the profile was already committed via the profile-upgrade step, let
    // the caller refresh even when the user backs out before the app upgrade.
    this.cancelled.emit(this.profileUpgraded ? { profileOnly: true } : null);
  }
}
