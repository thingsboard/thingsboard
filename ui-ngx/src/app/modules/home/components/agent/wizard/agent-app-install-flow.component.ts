// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { BehaviorSubject, combineLatest, forkJoin, Observable, of } from 'rxjs';
import { catchError, map, share, startWith } from 'rxjs/operators';
import { PageComponent } from '@shared/components/page.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  AgentInfo,
  isVirtualAppProfile,
  VirtualAgentAppProfile,
  dockerComposeConfig
} from '@shared/models/agent.models';
import { readCredentialValuesFromYaml } from '@home/pages/agent/util/agent-credentials';
import { classifyStepsForAction } from '@home/pages/agent/util/agent-app-steps';
import { buildStepInputs, createStepBinding, StepBinding } from '@home/pages/agent/util/agent-app-step-inputs';
import { buildInstallApplication } from '@home/pages/agent/util/agent-app-payloads';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { buildInstallMergeDraft, buildRelatedMergeDraft } from '@home/pages/agent/util/agent-app-compose-preview';
import { composeTemplateKeys, composeTypeLabelKey, dumpCompose, dumpRawTemplateCompose, pickComposeType } from '@home/pages/agent/util/agent-compose-yaml';
import {
  appProfileSearchMatches,
  appProfileTemplateLabel,
  appProfileTrackKey,
  buildVirtualAppProfiles,
  VIRTUAL_APP_TYPES
} from '@home/pages/agent/util/virtual-app-profiles';
import {
  AgentAppProfileWizardComponent,
  AgentAppProfileWizardData
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AgentAppWizardLoaderService } from '@home/pages/agent/wizard/agent-app-wizard-loader.service';
import { AgentAppWizardSubmitService } from '@home/pages/agent/wizard/agent-app-wizard-submit.service';
import { AgentAppUpgradeResult, AgentAppWizardFinish, AgentTypeCard } from '@home/pages/agent/wizard/agent-app-wizard.models';
import { EdgeTemplateCompatibilityService } from '@home/pages/agent/util/edge-template-compatibility.service';

@Component({
  selector: 'tb-agent-app-install-flow',
  templateUrl: './agent-app-install-flow.component.html',
  styleUrls: ['./agent-app-install-flow.component.scss'],
  standalone: false
})
export class AgentAppInstallFlowComponent extends PageComponent implements OnInit {

  @Input() agentId: string;
  @Input() agent: AgentInfo;
  @Input() lockedType: AgentApplicationType | null = null;
  @Input() lockedRelatedEntity: EntityId | null = null;
  @Input() navigateToAgentOnFinish = false;
  @Input() selectAgent = false;
  @Input() embedded = false;
  @Input() showBack = false;
  @Input() stepperOrientation: Observable<StepperOrientation>;
  @Input() stepperLabelPosition: Observable<'bottom' | 'end'>;

  @Output() finished = new EventEmitter<AgentAppWizardFinish>();
  @Output() cancelled = new EventEmitter<AgentAppUpgradeResult | null>();
  @Output() back = new EventEmitter<void>();

  // Select-agent step state (only when selectAgent === true).
  managedApp: AgentApplication | null = null;
  managedAgent: AgentInfo | null = null;
  checkingManaged = false;

  installedApplication: AgentApplication | null = null;
  installedEvent: AgentAppEvent | null = null;

  @ViewChild('stepper', { static: false })
  stepper: MatStepper;

  bindings: StepBinding[] = [];

  proposedYaml = '';
  currentYaml = '';
  diffSyncScroll = false;

  typeCards: AgentTypeCard[] = [
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
  private advanceAfterTypeSelect = false;

  appName = '';
  composeYaml = '';

  mergedApp: AgentApplication | null = null;
  submitting = false;

  useProfile = true;
  selectedProfile: AgentAppProfile | null = null;
  loadingProfiles = false;

  profileSelectFormGroup: UntypedFormGroup;
  filteredProfiles: Observable<AgentAppProfile[]>;

  readonly trackKey = appProfileTrackKey;

  @ViewChild('profileInput', { static: false }) profileInput: ElementRef<HTMLInputElement>;

  private availableProfiles$ = new BehaviorSubject<AgentAppProfile[]>([]);
  private availableProfilesValue: AgentAppProfile[] = [];

  get helpLinkId(): string {
    switch (this.selectedType || this.lockedType) {
      case AgentApplicationType.EDGE:
        return 'agentEdgeInstall';
      case AgentApplicationType.GATEWAY:
        return 'agentGatewayInstall';
      default:
        return 'agentAppInstall';
    }
  }

  get availableProfiles(): AgentAppProfile[] {
    return this.availableProfilesValue;
  }

  set availableProfiles(profiles: AgentAppProfile[]) {
    this.availableProfilesValue = profiles;
    this.availableProfiles$.next(profiles);
  }

  credentialValues: Record<string, string> = {};

  relatedEntityId: EntityId | null = null;
  relatedEntityName: string | null = null;

  private savedProfile: AgentAppProfile | null = null;
  private savedCustomCompose: string | null = null;
  private pendingCustomCompose: string | null = null;

  readonly canCreateAppProfile: boolean;

  constructor(private router: Router,
              private translate: TranslateService,
              private agentService: AgentService,
              private loader: AgentAppWizardLoaderService,
              private submitSvc: AgentAppWizardSubmitService,
              private userPermissionsService: UserPermissionsService,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private edgeTemplateCompatibility: EdgeTemplateCompatibilityService,
              private destroyRef: DestroyRef) {
    super();
    this.canCreateAppProfile = this.userPermissionsService.hasGenericPermission(Resource.AGENT_APP_PROFILE, Operation.CREATE);
    this.profileSelectFormGroup = this.fb.group({ profile: [null] });
    const text$ = this.profileSelectFormGroup.get('profile').valueChanges.pipe(
      startWith(this.profileSelectFormGroup.get('profile').value),
      map(value => typeof value === 'string' ? value : '')
    );
    this.filteredProfiles = combineLatest([this.availableProfiles$, text$]).pipe(
      map(([all, text]) => {
        if (!text || !text.length) {
          return all;
        }
        return all.filter(p => appProfileSearchMatches(p, text));
      }),
      share()
    );
    this.profileSelectFormGroup.get('profile').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      const selected = (value && typeof value !== 'string') ? value as AgentAppProfile : null;
      if (selected) {
        if (isVirtualAppProfile(selected)) {
          this.materializeAndSelect(selected);
        } else if (selected.id?.id !== this.selectedProfile?.id?.id) {
          this.selectProfile(selected);
        }
      } else if (this.selectedProfile) {
        this.clearProfileSelection();
      }
    });
  }

  ngOnInit() {
    this.relatedEntityId = this.lockedRelatedEntity || null;
    if (this.selectAgent && this.lockedRelatedEntity?.id && this.lockedRelatedEntity?.entityType) {
      this.checkingManaged = true;
      this.loader.loadManagedApp(
        this.lockedRelatedEntity.entityType as string,
        this.lockedRelatedEntity.id
      ).subscribe({
        next: app => {
          this.managedApp = app;
          this.checkingManaged = false;
          if (this.managedApp?.agentId?.id) {
            this.agentService.getAgentInfoById(this.managedApp.agentId.id).subscribe(a => { this.managedAgent = a; });
          }
        },
        error: () => {
          this.managedApp = null;
          this.checkingManaged = false;
        }
      });
    }
    this.loader.prewarmTemplates(this.typeCards.map(card => card.type));
    if (this.lockedType) {
      this.selectType(this.lockedType);
    }
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
    this.mergedApp = null;
    this.selectedProfile = null;
    this.profileSelectFormGroup.get('profile').patchValue(null, { emitEvent: false });
    this.availableProfiles = [];
    this.savedProfile = null;
    this.savedCustomCompose = null;
    this.pendingCustomCompose = null;
    this.appName = this.defaultAppName(type);
    this.loadError = '';
    this.advanceAfterTypeSelect = !this.lockedType;

    if (this.useProfile) {
      this.loadProfilesForType(type);
    } else {
      this.loadTemplateForType(type);
    }
  }

  private advanceFromTypeStep() {
    if (!this.advanceAfterTypeSelect || this.loadError) {
      return;
    }
    this.advanceAfterTypeSelect = false;
    setTimeout(() => this.stepper?.next(), 0);
  }

  toggleUseProfile(value: boolean) {
    this.advanceAfterTypeSelect = false;
    if (value) {
      this.savedCustomCompose = this.composeYaml || null;
    } else {
      this.savedProfile = this.selectedProfile;
    }
    this.useProfile = value;
    this.selectedProfile = null;
    this.profileSelectFormGroup.get('profile').patchValue(null, { emitEvent: false });
    this.template = null;
    this.composeYaml = '';
    this.mergedApp = null;
    this.loadError = '';
    this.credentialValues = {};
    if (this.selectedType) {
      if (value) {
        this.loadProfilesForType(this.selectedType);
        if (this.savedProfile) {
          this.selectProfile(this.savedProfile);
        }
      } else {
        this.availableProfiles = [];
        this.pendingCustomCompose = this.savedCustomCompose;
        this.loadTemplateForType(this.selectedType);
      }
    }
  }

  isVirtualProfile(profile: AgentAppProfile): boolean {
    return isVirtualAppProfile(profile);
  }

  profileTemplateLabel(profile: AgentAppProfile): string {
    return appProfileTemplateLabel(profile, this.translate);
  }

  private materializeAndSelect(virtualProfile: VirtualAgentAppProfile) {
    this.loadingProfiles = true;
    this.loadError = '';
    this.loader.materializeProfile(virtualProfile).subscribe({
      next: profile => {
        this.loadingProfiles = false;
        this.availableProfiles = this.availableProfiles.map(p =>
          isVirtualAppProfile(p) && p.templateVersion === profile.templateVersion ? profile : p);
        this.cacheRealProfiles(virtualProfile.appType);
        this.selectProfile(profile);
      },
      error: () => {
        this.loadingProfiles = false;
        this.loadError = this.translate.instant('agent.app-install-profile-materialize-failed');
        this.profileSelectFormGroup.get('profile').patchValue(null, { emitEvent: false });
        this.clearProfileSelection();
      }
    });
  }

  private cacheRealProfiles(appType: AgentApplicationType) {
    this.loader.cacheProfiles(appType, this.availableProfiles.filter(p => !isVirtualAppProfile(p)));
  }

  selectProfile(profile: AgentAppProfile) {
    if (!profile) { return; }
    this.selectedProfile = profile;
    this.profileSelectFormGroup.get('profile').patchValue(profile, { emitEvent: false });
    this.composeYaml = dumpCompose(profile);
    this.initCredentialValues();
    if (!profile.templateVersion) {
      return;
    }
    this.loadingTemplate = true;
    this.loader.loadTemplateByVersion(profile.appType, profile.templateVersion).subscribe({
      next: tpl => {
        this.template = tpl;
        this.initBindings(tpl);
        this.composeType = pickComposeType(tpl);
        this.loadingTemplate = false;
        if (this.relatedEntityId?.id) {
          this.mergeWithRelatedEntity();
        }
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplate = false;
      }
    });
  }

  createProfile($event?: Event) {
    if ($event) { $event.stopPropagation(); }
    const appType = this.selectedType || this.lockedType;
    if (!appType) { return; }
    const presetName = this.relatedEntityName ? `${this.relatedEntityName} profile` : undefined;
    this.dialog.open<AgentAppProfileWizardComponent, AgentAppProfileWizardData, AgentAppProfile>(
      AgentAppProfileWizardComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: { lockedAppType: appType, presetName }
      }).afterClosed().subscribe(saved => {
        if (saved) {
          this.availableProfiles = [
            ...this.availableProfiles.filter(p => !(isVirtualAppProfile(p) && p.templateVersion === saved.templateVersion)),
            saved
          ];
          this.cacheRealProfiles(appType);
          this.selectProfile(saved);
        }
      });
  }

  openEditProfile() {
    if (!this.selectedProfile || isVirtualAppProfile(this.selectedProfile)) { return; }
    this.dialog.open<AgentAppProfileWizardComponent, AgentAppProfileWizardData, AgentAppProfile>(
      AgentAppProfileWizardComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: { profile: this.selectedProfile }
      }).afterClosed().subscribe(saved => {
        if (saved) {
          this.availableProfiles = this.availableProfiles.map(p => p.id?.id === saved.id?.id ? saved : p);
          this.cacheRealProfiles(saved.appType);
          this.selectProfile(saved);
        }
      });
  }

  get canHaveRelatedEntity(): boolean {
    return this.selectedType === AgentApplicationType.EDGE || this.selectedType === AgentApplicationType.GATEWAY;
  }

  get relatedEntityRequired(): boolean {
    return this.canHaveRelatedEntity;
  }

  onRelatedEntityChange(_entity: EntityId | null) {
    if (!this.template) {
      return;
    }
    this.mergeWithRelatedEntity();
  }

  // Default the app name to "<related entity> application" once the entity is chosen.
  onRelatedEntityName(name: string | null) {
    this.relatedEntityName = name || null;
    if (name) {
      this.appName = `${name} application`;
    }
  }

  private mergeWithRelatedEntity() {
    const tpl = this.template;
    if (!tpl) { return; }
    const draft = buildRelatedMergeDraft(
      this.agentId, this.selectedType!, this.appName, tpl,
      this.composeYaml, dockerComposeConfig(this.mergedApp)?.compose);
    this.loader.merge(
      tpl.currentVersion, tpl.appType, draft, this.useProfile ? undefined : (this.composeType || undefined),
      this.relatedEntityId || undefined, undefined, true
    ).subscribe({
      next: merged => {
        this.mergedApp = merged;
        const mergedYaml = dumpCompose(merged);
        if (this.useProfile) {
          this.composeYaml = mergedYaml;
          this.initCredentialValues();
        } else {
          this.proposedYaml = dumpRawTemplateCompose(tpl, this.composeType);
          this.currentYaml = mergedYaml;
          this.composeYaml = mergedYaml;
        }
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-merge-failed');
      }
    });
  }

  private initCredentialValues() {
    this.credentialValues = readCredentialValuesFromYaml(
      this.composeYaml, dockerComposeConfig(this.mergedApp)?.compose, this.selectedType);
  }

  displayProfileFn(profile?: AgentAppProfile): string {
    return profile ? profile.name : '';
  }

  clearProfile() {
    this.profileSelectFormGroup.get('profile').patchValue(null, { emitEvent: true });
    setTimeout(() => {
      this.profileInput?.nativeElement.blur();
      this.profileInput?.nativeElement.focus();
    }, 0);
  }

  private clearProfileSelection() {
    this.selectedProfile = null;
    this.composeYaml = '';
    this.credentialValues = {};
    this.template = null;
    this.bindings = [];
    this.mergedApp = null;
  }

  private loadProfilesForType(type: AgentApplicationType) {
    this.loadingProfiles = true;
    const templates$: Observable<AgentAppTemplate[]> = VIRTUAL_APP_TYPES.includes(type) && this.canCreateAppProfile
      ? this.loader.loadTemplatesByType(type).pipe(catchError(() => of([])))
      : of([]);
    forkJoin([this.loader.loadProfiles(type), templates$]).subscribe({
      next: ([allProfiles, allTemplates]) => {
        const profiles = this.edgeTemplateCompatibility.filterProfiles(allProfiles);
        const templates = this.edgeTemplateCompatibility.filterTemplates(allTemplates);
        this.availableProfiles = [...profiles, ...buildVirtualAppProfiles(templates, allProfiles)];
        this.loadingProfiles = false;
        this.advanceFromTypeStep();
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingProfiles = false;
      }
    });
  }

  private loadTemplateForType(type: AgentApplicationType) {
    this.loadingTemplate = true;
    this.loader.loadTemplate(type).subscribe({
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
    this.composeType = pickComposeType(tpl);
    this.composeTypeKeys = composeTemplateKeys(tpl);
    if (!this.useProfile) {
      this.proposedYaml = dumpRawTemplateCompose(tpl, this.composeType);
      this.currentYaml = this.pendingCustomCompose ?? this.proposedYaml;
      this.composeYaml = this.currentYaml;
    }
    this.runMergeForPreview(tpl);
    this.advanceFromTypeStep();
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
    this.pendingCustomCompose = null;
    this.proposedYaml = dumpRawTemplateCompose(this.template, this.composeType);
    if (this.relatedEntityId?.id) {
      this.mergeWithRelatedEntity();
    } else {
      this.runMergeForPreview(this.template);
    }
  }

  private runMergeForPreview(tpl: AgentAppTemplate) {
    const draft = buildInstallMergeDraft(this.agentId, this.selectedType!, this.appName, tpl);
    this.loader.merge(tpl.currentVersion, tpl.appType, draft, this.composeType || undefined, this.relatedEntityId || undefined, undefined, true).subscribe({
      next: merged => {
        this.mergedApp = merged;
        if (!this.useProfile) {
          this.proposedYaml = dumpRawTemplateCompose(tpl, this.composeType);
          this.currentYaml = this.pendingCustomCompose ?? dumpCompose(merged);
          this.composeYaml = this.currentYaml;
          this.pendingCustomCompose = null;
        } else {
          this.composeYaml = dumpCompose(merged);
        }
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-merge-failed');
      }
    });
  }

  private initBindings(template: AgentAppTemplate) {
    this.bindings = classifyStepsForAction(template, AgentAppEventActionType.INSTALL)
      .map(cs => createStepBinding(cs, null));
  }

  private defaultAppName(type: AgentApplicationType): string {
    switch (type) {
      case AgentApplicationType.EDGE: return 'tb-edge';
      case AgentApplicationType.GATEWAY: return 'tb-gateway';
      default: return 'my-app';
    }
  }

  onSelectedAgentChange(agent: AgentInfo | null) {
    this.agent = agent;
    this.agentId = agent?.id?.id;
  }

  canProceedFromAgent(): boolean {
    return !!this.agent && !this.managedApp;
  }

  canProceedFromType(): boolean {
    return !!this.selectedType && !this.loadingTemplate && !this.loadError;
  }

  goToAgent() {
    const app = this.installedApplication || this.managedApp;
    const agentId = app?.agentId?.id || this.agentId;
    if (agentId) {
      this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId));
    }
    this.finishDeploy();
  }

  goToAgentApplication() {
    const app = this.installedApplication || this.managedApp;
    const agentId = app?.agentId?.id || this.agentId;
    const appId = app?.id?.id;
    if (agentId && appId) {
      this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId, 'applications', appId));
    }
    this.finishDeploy();
  }

  goToAgentEvents() {
    const app = this.installedApplication || this.managedApp;
    const agentId = app?.agentId?.id || this.agentId;
    if (agentId) {
      this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId, 'events'));
    }
    this.finishDeploy();
  }

  private finishDeploy() {
    this.finished.emit({
      event: this.installedEvent,
      application: this.installedApplication || this.managedApp,
      closeOnly: true
    });
  }

  canSubmit(): boolean {
    if (this.relatedEntityRequired && !this.relatedEntityId?.id) {
      return false;
    }
    if (this.useProfile) {
      return !!this.selectedType && !!this.selectedProfile && !!this.appName?.trim() && !!this.composeYaml?.trim() && !this.submitting;
    }
    return !!this.selectedType && !!this.appName?.trim() && !!this.composeYaml?.trim() && !this.submitting;
  }

  submit() {
    if (!this.canSubmit() || this.installedApplication) {
      return;
    }
    this.submitting = true;
    const application = buildInstallApplication({
      selectedType: this.selectedType,
      agentId: this.agentId,
      appName: this.appName,
      composeYaml: this.composeYaml,
      mergedApp: this.mergedApp,
      template: this.template,
      useProfile: this.useProfile,
      selectedProfile: this.selectedProfile,
      composeType: this.composeType || undefined
    });
    const stepInputs = buildStepInputs(this.bindings);
    this.submitSvc.install(application, stepInputs, this.relatedEntityId).subscribe({
      next: resp => {
        if (this.selectAgent) {
          this.installedApplication = resp?.application ?? null;
          this.installedEvent = resp?.event ?? null;
          this.submitting = false;
          const agentId = this.installedApplication?.agentId?.id || this.agentId;
          if (agentId) {
            this.agentService.getAgentInfoById(agentId).subscribe(a => { this.agent = a; });
          }
          setTimeout(() => this.stepper?.next(), 0);
        } else {
          this.finished.emit({ application: resp?.application ?? null, event: resp?.event ?? null });
        }
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  cancel() {
    this.cancelled.emit(null);
  }
}
