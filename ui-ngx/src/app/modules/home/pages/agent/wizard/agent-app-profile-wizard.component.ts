// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import * as YAML from 'yaml';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { StepperOrientation } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  AgentAppConfigType,
  dockerComposeConfig
} from '@shared/models/agent.models';
import {
  composeTemplateKeys,
  composeTypeLabelKey,
  dumpRawTemplateCompose,
  dumpYaml,
  pickComposeType
} from '@home/pages/agent/util/agent-compose-yaml';
import { orderTemplatesNewestFirst } from '@home/pages/agent/util/template-version-order';
import { EdgeTemplateCompatibilityService } from '@home/pages/agent/util/edge-template-compatibility.service';

export interface AgentAppProfileWizardData {
  profile?: AgentAppProfile;
  lockedAppType?: AgentApplicationType;
  presetName?: string;
}

interface TypeCard {
  type: AgentApplicationType;
  icon: string;
  labelKey: string;
  descKey: string;
}

@Component({
  selector: 'tb-agent-app-profile-wizard',
  templateUrl: './agent-app-profile-wizard.component.html',
  styleUrls: ['./agent-app-profile-wizard.component.scss'],
  standalone: false
})
export class AgentAppProfileWizardComponent
  extends DialogComponent<AgentAppProfileWizardComponent, AgentAppProfile>
  implements OnInit {

  // Left (read-only) = raw template compose; right (editable) = same content,
  // user-editable. This is the value persisted on submit.
  proposedYaml = '';
  currentYaml = '';

  typeCards: TypeCard[] = [
    { type: AgentApplicationType.GENERIC, icon: 'inventory_2', labelKey: 'agent.app-install-type-generic', descKey: 'agent.app-install-type-generic-desc' },
    { type: AgentApplicationType.EDGE, icon: 'router', labelKey: 'agent.app-install-type-edge', descKey: 'agent.app-install-type-edge-desc' },
    { type: AgentApplicationType.GATEWAY, icon: 'hub', labelKey: 'agent.app-install-type-gateway', descKey: 'agent.app-install-type-gateway-desc' }
  ];

  selectedType: AgentApplicationType | null = null;
  template: AgentAppTemplate | null = null;
  composeType: string | null = null;
  composeTypeKeys: string[] = [];
  loadingTemplate = false;
  loadError = '';

  // Template version selection
  availableTemplates: AgentAppTemplate[] = [];
  selectedTemplateVersion: string | null = null;
  loadingTemplates = false;
  private templatesByTypeCache = new Map<AgentApplicationType, AgentAppTemplate[]>();

  profileForm: UntypedFormGroup;
  composeYaml = '';
  diffSyncScroll = false;

  submitting = false;

  stepperOrientation: Observable<StepperOrientation>;
  stepperLabelPosition: Observable<'bottom' | 'end'>;

  get lockedAppType(): AgentApplicationType | null {
    return this.data?.lockedAppType ?? this.data?.profile?.appType ?? null;
  }

  get editMode(): boolean {
    return !!this.data?.profile?.id?.id;
  }

  get profileName(): string {
    return this.profileForm.get('name').value ?? '';
  }

  get profileDescription(): string {
    return this.profileForm.get('description').value ?? '';
  }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private breakpointObserver: BreakpointObserver,
              private edgeTemplateCompatibility: EdgeTemplateCompatibilityService,
              private fb: UntypedFormBuilder,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppProfileWizardData,
              public dialogRef: MatDialogRef<AgentAppProfileWizardComponent, AgentAppProfile>) {
    super(store, router, dialogRef);
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['']
    });
    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'horizontal' : 'vertical'));
    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'end' : 'bottom'));
  }

  ngOnInit() {
    // Prefetch templates for all types so type selection is instant.
    this.typeCards.forEach(card => {
      this.agentService.getAgentAppTemplatesByAppType(card.type).subscribe({
        next: templates => this.templatesByTypeCache.set(card.type, templates),
        error: () => {}
      });
    });
    if (this.editMode) {
      this.initForEdit(this.data.profile);
    } else {
      if (this.data?.presetName) {
        this.profileForm.get('name').setValue(this.data.presetName);
      }
      if (this.lockedAppType) {
        this.selectType(this.lockedAppType);
      }
    }
  }

  get titleKey(): string {
    return this.editMode ? 'agent.app-profile-wizard-edit-title' : 'agent.app-profile-wizard-title';
  }

  get profileNamePlaceholder(): string {
    if (this.selectedType === AgentApplicationType.EDGE) {
      return this.translate.instant('agent.app-profile-name-placeholder-edge');
    }
    if (this.selectedType === AgentApplicationType.GATEWAY) {
      return this.translate.instant('agent.app-profile-name-placeholder-gateway');
    }
    return this.translate.instant('agent.app-profile-name-placeholder-generic');
  }

  private initForEdit(profile: AgentAppProfile) {
    this.selectedType = profile.appType;
    this.profileForm.patchValue({ name: profile.name, description: profile.description || '' });
    this.selectedTemplateVersion = profile.templateVersion || null;
    this.loadTemplatesForType(profile.appType);
    if (!profile.templateVersion) {
      return;
    }
    this.loadingTemplate = true;
    this.agentService.getAgentAppTemplateByVersion(profile.appType, AgentAppConfigType.DOCKER_COMPOSE, profile.templateVersion).subscribe({
      next: tpl => {
        this.template = tpl;
        this.composeType = dockerComposeConfig(profile)?.composeType || pickComposeType(tpl);
        this.composeTypeKeys = composeTemplateKeys(tpl);
        this.proposedYaml = dumpRawTemplateCompose(tpl, this.composeType);
        const compose: any = dockerComposeConfig(profile)?.compose;
        this.currentYaml = compose ? (dumpYaml(compose, 0).trimEnd() + '\n') : this.proposedYaml;
        this.composeYaml = this.currentYaml;
        this.loadingTemplate = false;
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplate = false;
      }
    });
  }

  selectType(type: AgentApplicationType) {
    if (this.selectedType === type) {
      return;
    }
    this.selectedType = type;
    this.template = null;
    this.composeType = null;
    this.composeTypeKeys = [];
    this.composeYaml = '';
    this.selectedTemplateVersion = null;
    this.loadError = '';
    this.loadTemplatesForType(type);
  }

  private loadTemplatesForType(type: AgentApplicationType) {
    const cached = this.templatesByTypeCache.get(type);
    if (cached) {
      this.availableTemplates = orderTemplatesNewestFirst(this.supportedTemplates(cached));
      this.selectLatestTemplate();
      return;
    }
    this.loadingTemplates = true;
    this.agentService.getAgentAppTemplatesByAppType(type).subscribe({
      next: templates => {
        this.templatesByTypeCache.set(type, templates);
        this.availableTemplates = orderTemplatesNewestFirst(this.supportedTemplates(templates));
        this.loadingTemplates = false;
        this.selectLatestTemplate();
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplates = false;
      }
    });
  }

  // Keeps the edited profile's own version selectable even when it is one an add-on Edge cannot run.
  private supportedTemplates(templates: AgentAppTemplate[]): AgentAppTemplate[] {
    const editedVersion = this.editMode ? this.data?.profile?.templateVersion : null;
    const supported = this.edgeTemplateCompatibility.filterTemplates(templates);
    const edited = editedVersion && !supported.some(t => t.currentVersion === editedVersion)
      ? (templates || []).filter(t => t.currentVersion === editedVersion)
      : [];
    return [...supported, ...edited];
  }

  private selectLatestTemplate() {
    if (this.selectedTemplateVersion || !this.availableTemplates.length) {
      return;
    }
    const latest = this.availableTemplates.find(t => !t.nextVersion) || this.availableTemplates[0];
    this.selectedTemplateVersion = latest.currentVersion;
    this.applyTemplate(latest);
  }

  onTemplateSelected(version: string) {
    this.selectedTemplateVersion = version;
    const tpl = this.availableTemplates.find(t => t.currentVersion === version);
    if (tpl) {
      this.applyTemplate(tpl);
    }
  }

  private applyTemplate(tpl: AgentAppTemplate) {
    this.loadingTemplate = false;
    this.template = tpl;
    this.composeType = pickComposeType(tpl);
    this.composeTypeKeys = composeTemplateKeys(tpl);
    this.rebuildComposePreview();
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
    if (this.editMode) {
      this.proposedYaml = dumpRawTemplateCompose(this.template, this.composeType);
      return;
    }
    this.rebuildComposePreview();
  }

  private rebuildComposePreview() {
    const tpl = this.template;
    if (!tpl) {
      return;
    }
    this.proposedYaml = dumpRawTemplateCompose(tpl, this.composeType);
    this.currentYaml = this.proposedYaml;
    this.composeYaml = this.currentYaml;
    const draft: AgentAppProfile = {
      name: this.profileName.trim() || 'preview',
      appType: this.selectedType,
      templateVersion: tpl.currentVersion
    };
    this.agentService.mergeProfileForPreview(tpl.currentVersion, draft, this.composeType || undefined, undefined, !this.editMode).subscribe({
      next: merged => {
        const compose: any = dockerComposeConfig(merged)?.compose;
        if (compose) {
          this.currentYaml = dumpYaml(compose, 0).trimEnd() + '\n';
          this.composeYaml = this.currentYaml;
        }
      },
      error: () => { /* keep raw template seed */ }
    });
  }

  cancel() {
    this.dialogRef.close(undefined);
  }

  canProceedFromType(): boolean {
    return !!this.selectedType && !!this.selectedTemplateVersion && !!this.template
      && !this.loadingTemplate && !this.loadingTemplates && !this.loadError;
  }

  canSubmit(): boolean {
    return !!this.selectedType && this.profileForm.valid && !!this.profileName.trim() && !!this.composeYaml?.trim()
      && !this.submitting && !this.isComposeYamlInvalid();
  }

  isComposeYamlInvalid(): boolean {
    if (!this.composeYaml?.trim()) { return false; }
    try {
      YAML.parse(this.composeYaml);
      return false;
    } catch (_) {
      return true;
    }
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    let compose: any;
    try {
      compose = YAML.parse(this.composeYaml);
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
    this.submitting = true;

    const profile: any = {
      ...(this.editMode ? this.data.profile : {}),
      name: this.profileName.trim(),
      appType: this.selectedType,
      templateVersion: this.template?.currentVersion,
      config: {
        type: AgentAppConfigType.DOCKER_COMPOSE,
        compose,
        composeType: this.composeType || undefined
      },
      description: this.profileDescription.trim() || undefined,
    };

    this.agentService.saveAgentAppProfile(profile).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: () => {
        this.submitting = false;
      }
    });
  }

}
