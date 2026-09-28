// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { StepperOrientation } from '@angular/material/stepper';
import { Observable } from 'rxjs';
import { PageComponent } from '@shared/components/page.component';
import { TranslateService } from '@ngx-translate/core';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  AgentInfo,
  dockerComposeConfig,
  AgentApplicationInfo
} from '@shared/models/agent.models';
import { readCredentialValuesFromYaml } from '@home/pages/agent/util/agent-credentials';
import { classifyStepsForAction } from '@home/pages/agent/util/agent-app-steps';
import { buildStepInputs, createStepBinding, StepBinding } from '@home/pages/agent/util/agent-app-step-inputs';
import { buildUpdateApplication } from '@home/pages/agent/util/agent-app-payloads';
import { buildUpdateMergeDraft } from '@home/pages/agent/util/agent-app-compose-preview';
import { composeTemplateKeys, composeTypeLabelKey, dumpCompose, pickComposeType } from '@home/pages/agent/util/agent-compose-yaml';
import {
  AgentAppProfileWizardComponent,
  AgentAppProfileWizardData
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';
import { AgentAppWizardLoaderService } from '@home/pages/agent/wizard/agent-app-wizard-loader.service';
import { AgentAppWizardSubmitService } from '@home/pages/agent/wizard/agent-app-wizard-submit.service';
import { AgentAppUpgradeResult, AgentAppWizardFinish } from '@home/pages/agent/wizard/agent-app-wizard.models';

@Component({
  selector: 'tb-agent-app-update-flow',
  templateUrl: './agent-app-update-flow.component.html',
  styleUrls: ['./agent-app-update-flow.component.scss'],
  standalone: false
})
export class AgentAppUpdateFlowComponent extends PageComponent implements OnInit {

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

  bindings: StepBinding[] = [];

  proposedYaml = '';
  currentYaml = '';
  futureYaml = '';
  diffSyncScroll = false;

  readOnlyCompare = false;
  profileForCompare: AgentAppProfile | null = null;

  selectedType: AgentApplicationType | null = null;
  template: AgentAppTemplate | null = null;
  composeType: string | null = null;
  composeTypeKeys: string[] = [];
  loadingTemplate = false;
  loadError = '';

  appName = '';
  composeYaml = '';

  mergedApp: AgentApplication | null = null;
  submitting = false;

  credentialValues: Record<string, string> = {};

  // Profile-managed update: when checked, the backend won't re-resolve compose
  // from the (possibly upgraded) profile — only carried credentials are applied.
  skipProfileRefetch = false;
  // Profile moved to another template version: Update never applies cross-version
  // config (that's Upgrade's job), so the refetch is forced off.
  profileTemplateDrifted = false;

  relatedEntityId: EntityId | null = null;
  private initialRelatedEntityId: EntityId | null = null;

  constructor(private translate: TranslateService,
              private loader: AgentAppWizardLoaderService,
              private submitSvc: AgentAppWizardSubmitService,
              private dialog: MatDialog) {
    super();
  }

  get isProfileManagedUpdate(): boolean {
    return !!this.existingApplication?.applicationProfileId;
  }

  get canHaveRelatedEntity(): boolean {
    const type = this.selectedType || this.existingApplication?.appType;
    return type === AgentApplicationType.EDGE || type === AgentApplicationType.GATEWAY;
  }

  get relatedEntityShown(): boolean {
    return this.canHaveRelatedEntity && this.isProfileManagedUpdate;
  }

  get relatedEntityRequired(): boolean {
    return this.relatedEntityShown;
  }

  ngOnInit() {
    const existingRelated = this.existingApplication?.relatedEntityId ?? null;
    this.relatedEntityId = this.lockedRelatedEntity || existingRelated;
    this.initialRelatedEntityId = this.lockedRelatedEntity || existingRelated;
    this.appName = this.existingApplication.name;
    this.selectType(this.existingApplication.appType);
  }

  private selectType(type: AgentApplicationType) {
    this.selectedType = type;
    this.template = null;
    this.composeType = null;
    this.composeTypeKeys = [];
    this.composeYaml = '';
    this.mergedApp = null;
    this.loadError = '';
    this.loadTemplateForType(type);
  }

  private loadTemplateForType(type: AgentApplicationType) {
    this.loadingTemplate = true;
    const version = this.existingApplication?.templateVersion;
    const configType = this.existingApplication?.config?.type;
    const template$ = version
      ? this.loader.loadTemplateByVersion(type, version, configType || undefined)
      : this.loader.loadTemplate(type);
    template$.subscribe({
      next: tpl => this.applyTemplate(tpl),
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplate = false;
      }
    });
  }

  private applyTemplate(tpl: AgentAppTemplate) {
    this.loadingTemplate = false;
    this.template = tpl;
    this.initBindings(tpl);
    this.composeTypeKeys = composeTemplateKeys(tpl);
    const savedComposeType = dockerComposeConfig(this.existingApplication)?.composeType;
    this.composeType = savedComposeType && this.composeTypeKeys.includes(savedComposeType)
      ? savedComposeType
      : pickComposeType(tpl);
    // Profile-managed update: skip diff/preview merge — the user can only edit
    // credentials, so hydrate from the existing app and expose skip-refetch.
    if (this.existingApplication?.applicationProfileId) {
      this.composeYaml = dumpCompose(this.existingApplication);
      this.initCredentialValues();
      this.loadProfileForCompare();
      return;
    }
    this.runMergeForPreview(tpl);
  }

  private runMergeForPreview(tpl: AgentAppTemplate) {
    const draft = buildUpdateMergeDraft(this.existingApplication, tpl);
    this.loader.merge(tpl.currentVersion, tpl.appType, draft, this.composeType || undefined, undefined).subscribe({
      next: merged => {
        this.mergedApp = merged;
        this.proposedYaml = dumpCompose(merged);
        this.currentYaml = dumpCompose(this.existingApplication);
        this.composeYaml = this.currentYaml;
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-merge-failed');
      }
    });
  }

  composeTypeLabel(composeType: string): string {
    const labelKey = composeTypeLabelKey(composeType);
    return labelKey ? this.translate.instant(labelKey) : composeType;
  }

  onComposeTypeChange(composeType: string) {
    if (!this.template || this.composeType === composeType) {
      return;
    }
    this.composeType = composeType;
    this.runMergeForPreview(this.template);
  }

  private loadProfileForCompare() {
    const profileId = this.existingApplication?.applicationProfileId?.id;
    if (!profileId) { return; }
    this.loader.loadProfileById(profileId).subscribe({
      next: prof => {
        this.profileForCompare = prof;
        this.profileTemplateDrifted = !!prof.templateVersion && !!this.existingApplication?.templateVersion
          && prof.templateVersion !== this.existingApplication.templateVersion;
        if (this.profileTemplateDrifted) {
          this.skipProfileRefetch = true;
        }
        this.readOnlyCompare = true;
        this.currentYaml = dumpCompose(this.existingApplication);
        this.refreshFutureCompare();
      },
      error: () => { /* comparison is best-effort; skip on failure */ }
    });
  }

  onRelatedEntityChange() {
    if (this.isProfileManagedUpdate) {
      this.refreshFutureCompare();
    }
  }

  private refreshFutureCompare() {
    const prof = this.profileForCompare;
    if (!prof) { return; }
    this.futureYaml = dumpCompose(prof);
    if (!prof.templateVersion) {
      return;
    }
    const draft = { ...this.existingApplication, templateVersion: prof.templateVersion, config: prof.config } as AgentApplication;
    this.loader.merge(prof.templateVersion, prof.appType, draft, undefined, this.relatedEntityId || undefined).subscribe({
      next: merged => {
        this.futureYaml = dumpCompose(merged);
      },
      error: () => { /* keep raw profile compose */ }
    });
  }

  openEditProfile() {
    if (!this.profileForCompare) { return; }
    this.dialog.open<AgentAppProfileWizardComponent, AgentAppProfileWizardData, AgentAppProfile>(
      AgentAppProfileWizardComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: { profile: this.profileForCompare }
      }).afterClosed().subscribe(saved => {
        if (saved) {
          this.profileForCompare = saved;
          this.refreshFutureCompare();
        }
      });
  }

  private initCredentialValues() {
    this.credentialValues = readCredentialValuesFromYaml(
      this.composeYaml, dockerComposeConfig(this.mergedApp)?.compose, this.selectedType);
  }

  private initBindings(template: AgentAppTemplate) {
    this.bindings = classifyStepsForAction(template, AgentAppEventActionType.UPDATE)
      .map(cs => createStepBinding(cs, null));
  }

  canSubmit(): boolean {
    if (this.relatedEntityRequired && !this.relatedEntityId?.id) {
      return false;
    }
    return !!this.selectedType && !!this.appName?.trim() && !!this.composeYaml?.trim() && !this.submitting;
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting = true;
    const application = buildUpdateApplication({
      existingApplication: this.existingApplication,
      appName: this.appName,
      composeYaml: this.composeYaml,
      mergedApp: this.mergedApp,
      composeType: this.composeType || undefined
    });
    const stepInputs = buildStepInputs(this.bindings);
    this.submitSvc.update({
      existingApplicationId: this.existingApplication.id.id,
      application,
      stepInputs,
      relatedEntityId: this.relatedEntityId,
      initialRelatedEntityId: this.initialRelatedEntityId,
      isProfileManagedUpdate: this.isProfileManagedUpdate,
      skipProfileRefetch: this.skipProfileRefetch
    }).subscribe({
      next: event => this.finished.emit({ application: this.existingApplication, event }),
      error: () => {
        this.submitting = false;
      }
    });
  }

  cancel() {
    this.cancelled.emit(null);
  }
}
