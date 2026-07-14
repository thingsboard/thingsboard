///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import {
  PeConnectivityMethodPromptData,
  TbPeConnectivityMethodPromptComponent
} from '@home/components/iot-hub/pe-connectivity-method-prompt.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { MatStepper } from '@angular/material/stepper';
import { firstValueFrom } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { PageLink } from '@shared/models/page/page-link';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { DeviceService } from '@core/http/device.service';
import { DashboardService } from '@core/http/dashboard.service';
import { RuleChainService } from '@core/http/rule-chain.service';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { generateSecret } from '@core/utils';
import {
  DeviceInstallStep,
  DevicePackageInfo,
  ENTITY_STEP_TYPES,
  EntityStepOutput,
  EntityStepProgress,
  FormFieldDefinition,
  FormFieldType,
  installMethodIcons as INSTALL_METHOD_ICONS,
  installMethodLabels as INSTALL_METHOD_LABELS,
  InstallStepType,
  peOnlyInstallMethods,
  stepTypeAliasMap
} from '@shared/models/iot-hub/device-package.models';
import { mergeMap } from 'rxjs/operators';

export interface DeviceInstallDialogData {
  item: MpItemVersionView;
  reviewMode?: boolean;
  selectedInstallMethod?: string;
  installState?: Record<string, any>;
}

export type WizardStepType = 'connectivity' | 'placeholder' | 'instruction' | 'form' | 'progress';

export interface WizardStep {
  type: WizardStepType;
  label: string;
  rawSteps: DeviceInstallStep[];
  completed: boolean;
  // Instruction
  markdown?: string;
  // Form
  formFields?: FormFieldDefinition[];
  formGroup?: UntypedFormGroup;
  // Progress
  entitySteps?: EntityStepProgress[];
  progressError?: string;
  progressDone?: boolean;
}

const ENTITY_STEP_MIN_DELAY = 2000;
const DEFAULT_RANDOM_SIZE = 20;

@Component({
  selector: 'tb-device-install-dialog',
  standalone: false,
  templateUrl: './device-install-dialog.component.html',
  styleUrls: ['./device-install-dialog.component.scss']
})
export class TbDeviceInstallDialogComponent extends DialogComponent<TbDeviceInstallDialogComponent> implements OnInit, OnDestroy {

  @ViewChild('installStepper', {static: false}) stepper: MatStepper;

  loading = true;
  packageInfo: DevicePackageInfo;
  zipFiles = new Map<string, string>();
  zipImages = new Map<string, string>();

  // Connectivity
  availableInstallMethods: string[] = [];
  selectedInstallMethod: string | null = null;
  installMethodLabels = INSTALL_METHOD_LABELS;
  installMethodIcons = INSTALL_METHOD_ICONS;
  peOnlyInstallMethods = peOnlyInstallMethods;

  // Wizard
  wizardSteps: WizardStep[] = [];
  wizardStarted = false;
  reviewMode = false;

  // Variable resolution state
  formValues: Record<string, any> = {};
  entityOutputs = new Map<string, EntityStepOutput>();
  transportVars: Record<string, string> = {};
  // form-field key → declared type. Used by resolveTemplateJson() to emit BOOLEAN/INTEGER
  // placeholders as raw JSON values instead of quoted strings.
  private fieldTypes = new Map<string, FormFieldType>();
  gatewayDockerComposeContent: string | null = null;

  resolveMarkdownVariable: (key: string) => string | undefined = this._resolveMarkdownVariable.bind(this);

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbDeviceInstallDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DeviceInstallDialogData,
    private cdr: ChangeDetectorRef,
    private deviceProfileService: DeviceProfileService,
    private deviceService: DeviceService,
    private dashboardService: DashboardService,
    private ruleChainService: RuleChainService,
    private attributeService: AttributeService,
    private iotHubApiService: IotHubApiService,
    private translate: TranslateService,
    private dialog: MatDialog
  ) {
    super(store, router, dialogRef);
  }

  async ngOnInit(): Promise<void> {
    try {
      const zipData = await firstValueFrom(this.iotHubApiService.getVersionFileData(this.data.item.id, { ignoreLoading: true }).pipe(
        mergeMap((blob: Blob) => blob.arrayBuffer())
      ));
      const JSZip = (await import('jszip')).default;
      const zip = await JSZip.loadAsync(zipData);
      const imageExtensions = new Set(['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp']);
      for (const [path, entry] of Object.entries(zip.files) as [string, any][]) {
        if (!entry.dir) {
          const ext = path.split('.').pop()?.toLowerCase();
          if (imageExtensions.has(ext)) {
            const blob = await entry.async('blob');
            const blobUrl = URL.createObjectURL(blob);
            this.zipImages.set(path, blobUrl);
          } else {
            const content = await entry.async('string');
            this.zipFiles.set(path, content);
          }
        }
      }
      this.packageInfo = JSON.parse(this.zipFiles.get('device-info.json'));
      this.availableInstallMethods = this.packageInfo.installMethods.filter(
        ct => this.packageInfo.installSteps[ct]?.length > 0
      );
    } catch (e) {
      console.error('Failed to parse device package ZIP', e);
    }
    // Fetch transport connectivity settings for variable resolution
    try {
      const connectivity = await firstValueFrom(this.iotHubApiService.getConnectivitySettings({ ignoreErrors: true }));
      if (connectivity) {
        for (const [protocol, info] of Object.entries(connectivity)) {
          if (info) {
            this.transportVars[`${protocol}.host`] = info.host || '';
            this.transportVars[`${protocol}.port`] = String(info.port || '');
          }
        }
      }
    } catch (_e) {
      console.error('Failed to fetch connectivity settings', _e);
    }
    // Review mode: restore state and start wizard with stored values
    if (this.data.reviewMode && this.data.installState && this.data.selectedInstallMethod) {
      this.reviewMode = true;
      this.selectedInstallMethod = this.data.selectedInstallMethod;
      this.restoreInstallState(this.data.installState);
      this.startWizard();
    } else if (this.availableInstallMethods.length === 1
        && !peOnlyInstallMethods.has(this.availableInstallMethods[0])) {
      // Single non-PE method: skip the selector and start the wizard.
      this.selectedInstallMethod = this.availableInstallMethods[0];
      this.startWizard();
    } else {
      // Multiple methods (or only PE-only ones): render the connection
      // method step as the wizard's first step. PE-only cards open the
      // pe-connectivity-method-prompt dialog and never set
      // selectedInstallMethod.
      this.startWizard();
    }
    this.loading = false;
    this.cdr.detectChanges();
  }

  ngOnDestroy() {
    for (const url of this.zipImages.values()) {
      URL.revokeObjectURL(url);
    }
    super.ngOnDestroy();
  }

  // --- Connectivity ---

  selectConnectivity(ct: string): void {
    // PE-only methods open the upgrade prompt dialog instead of being
    // selectable — they never become the active install method.
    if (peOnlyInstallMethods.has(ct)) {
      this.openPeConnectivityPrompt(ct);
      return;
    }
    if (this.selectedInstallMethod === ct) {
      // Re-clicking the already-selected card is a no-op; users who
      // want to proceed without changing selection use the Next button.
      return;
    }
    this.selectedInstallMethod = ct;
    // Auto-advance: configure the wizard for the chosen method and
    // jump to the next step.
    if (this.currentWizardStep?.type === 'connectivity') {
      this.confirmConnectivity();
    }
  }

  private openPeConnectivityPrompt(ct: string): void {
    this.dialog.open<TbPeConnectivityMethodPromptComponent, PeConnectivityMethodPromptData>(
      TbPeConnectivityMethodPromptComponent,
      {
        data: { connectorName: this.installMethodLabels.get(ct) || ct },
        autoFocus: false,
        panelClass: ['tb-dialog']
      }
    );
  }

  onTabChanged(index: number): void {
    const ws = this.wizardSteps[index];
    if (!ws) return;
    if (ws.type === 'instruction' && !ws.markdown) {
      ws.markdown = this.zipFiles.get(ws.rawSteps[0].file) || '';
    } else if (ws.type === 'progress' && !ws.progressDone) {
      this.showCompletedEntitySteps(ws);
    }
  }

  confirmConnectivity(): void {
    if (!this.selectedInstallMethod) {
      return;
    }
    // If we are already inside the wizard (connectivity step is the
    // first one), append the remaining steps and advance the stepper.
    const onConnectivityStep = this.currentWizardStep?.type === 'connectivity';
    if (onConnectivityStep) {
      const connectivityStep = this.currentWizardStep;
      // Drop anything that may have been appended on a previous pass
      // (user came back to pick a different method).
      this.wizardSteps.length = 1;
      this.appendInstallSteps();
      connectivityStep.completed = true;
      this.cdr.detectChanges();
      // Wait for new mat-step children to register before advancing.
      setTimeout(() => {
        this.stepper?.next();
        this.onStepActivated();
      }, 0);
      return;
    }
    this.startWizard();
  }

  // --- Wizard ---

  get isFirstWizardStep(): boolean {
    return !this.stepper || this.stepper.selectedIndex === 0;
  }

  get allProgressDone(): boolean {
    return this.wizardSteps.filter(s => s.type === 'progress').every(s => s.progressDone);
  }

  get isLastWizardStep(): boolean {
    return this.stepper && this.stepper.selectedIndex === this.wizardSteps.length - 1;
  }

  get currentWizardStep(): WizardStep | null {
    if (!this.stepper || !this.wizardSteps.length) {
      return null;
    }
    return this.wizardSteps[this.stepper.selectedIndex] ?? null;
  }

  get nextWizardStepLabel(): string {
    if (!this.stepper || this.stepper.selectedIndex >= this.wizardSteps.length - 1) {
      return '';
    }
    return this.wizardSteps[this.stepper.selectedIndex + 1]?.label || '';
  }

  previousStep(): void {
    if (!this.stepper || this.stepper.selectedIndex <= 0) {
      return;
    }
    const step = this.currentWizardStep;
    if (step?.type === 'progress' && step.progressError) {
      this.goBackToForm();
    } else {
      this.stepper.previous();
    }
  }

  nextStep(): void {
    const step = this.currentWizardStep;
    if (!step) {
      return;
    }
    if (step.type === 'form') {
      if (step.formGroup?.invalid) {
        step.formGroup.markAllAsTouched();
        return;
      }
      Object.assign(this.formValues, step.formGroup.getRawValue());
    }
    if (this.isLastWizardStep) {
      this.done();
      return;
    }
    step.completed = true;
    this.cdr.detectChanges();
    this.stepper.next();
    this.onStepActivated();
  }

  get primaryEntityAction(): { url: string; label: string } | null {
    // Priority: dashboard > device > gateway > integration
    const dashboard = this.entityOutputs.get('dashboard');
    if (dashboard?.url) {
      return { url: dashboard.url, label: 'Open Dashboard' };
    }
    const device = this.entityOutputs.get('device');
    if (device?.url) {
      return { url: device.url, label: 'Open Device' };
    }
    const gateway = this.entityOutputs.get('gateway');
    if (gateway?.url) {
      return { url: gateway.url, label: 'Open Gateway' };
    }
    const integration = this.entityOutputs.get('integration');
    if (integration?.url) {
      return { url: integration.url, label: 'Open Integration' };
    }
    return null;
  }

  openEntity(url: string): void {
    this.dialogRef.close('installed');
    void this.router.navigateByUrl(url);
  }

  done(): void {
    this.dialogRef.close('installed');
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  retryEntitySteps(step: WizardStep): void {
    step.progressError = null;
    void this.runEntitySteps(step);
  }

  async resolveConflict(ws: WizardStep, ep: EntityStepProgress, resolution: string): Promise<void> {
    ep.status = 'running';
    ep.errorMessage = null;
    this.cdr.detectChanges();
    try {
      let output: EntityStepOutput;
      if (resolution === 'use-existing') {
        output = ep.existingEntity;
        // For devices/gateways, fetch credentials
        if (ep.step.type === InstallStepType.DEVICE || ep.step.type === InstallStepType.GATEWAY) {
          const creds = await firstValueFrom(this.deviceService.getDeviceCredentials(output.id, false, {ignoreErrors: true}));
          output = { ...output, token: creds.credentialsId };
          if (ep.step.type === InstallStepType.GATEWAY) {
            output.dockerComposeUrl = `/api/device-connectivity/gateway-launch/${output.id}/docker-compose/download`;
          }
        }
      } else if (resolution === 'overwrite') {
        output = await this.overwriteEntity(ep.step, ep.existingEntity);
      } else {
        // create-copy — just create normally
        output = await this.createEntity(ep.step);
      }
      await this.saveStepAttributes(ep.step, output);
      ep.existingEntity = null;
      ep.conflictType = null;
      this.markEntityStepSuccess(ws, ep, output, resolution);
      // Resume running remaining steps
      await this.runEntitySteps(ws);
    } catch (err: any) {
      ep.status = 'error';
      ep.errorMessage = err?.error?.message || err?.message || 'Unknown error';
      ws.progressError = ep.errorMessage;
      this.cdr.detectChanges();
    }
  }

  goBackToForm(): void {
    // Find the last form step before the current progress step
    const currentIdx = this.stepper.selectedIndex;
    for (let i = currentIdx - 1; i >= 0; i--) {
      if (this.wizardSteps[i].type === 'form') {
        // Reset the progress step so it re-runs when we come back
        const progressStep = this.wizardSteps[currentIdx];
        progressStep.entitySteps = null;
        progressStep.progressError = null;
        progressStep.progressDone = false;
        progressStep.completed = false;
        // Allow navigation back by making intermediate steps editable
        for (let j = i; j < currentIdx; j++) {
          this.wizardSteps[j].completed = false;
        }
        this.stepper.selectedIndex = i;
        return;
      }
    }
  }

  onMarkdownReady(container: HTMLElement): void {
    // Download button handlers
    const buttons = container.querySelectorAll('[data-action="download-gateway-docker-compose"]');
    buttons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        if (this.gatewayDockerComposeContent) {
          const blob = new Blob([this.gatewayDockerComposeContent], { type: 'application/x-yaml' });
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = 'docker-compose.yml';
          link.click();
          URL.revokeObjectURL(url);
        } else {
          const gateway = this.entityOutputs.get('gateway');
          if (gateway?.id) {
            this.deviceService.downloadGatewayDockerComposeFile(gateway.id).subscribe();
          }
        }
      });
    });
  }

  resolveImagePath(path: string): string {
    if (path.startsWith('data:') || path.startsWith('http')) {
      return path;
    }
    return this.zipImages.get(path) || path;
  }

  // Bound function reference passed to <tb-install-form-renderer>'s [resolveImagePath] input.
  // Defined as a property so the template binding stays stable across change detection.
  readonly resolveImagePathFn: (path: string) => string = (path: string) => this.resolveImagePath(path);

  // --- Variable Resolution ---

  resolveVariables(content: string): string {
    return content.replace(/\$\{([^}]+)}/g, (_match, key) => {
      const res = this.resolveVariable(key);
      if (res) {
        return res;
      }
      return '${' + key + '}';
    });
  }

  resolveVariable(key: string): string | undefined {
    if (key in this.formValues) {
      return String(this.formValues[key]);
    }
    if (key in this.transportVars) {
      return this.transportVars[key];
    }
    const dotIdx = key.indexOf('.');
    if (dotIdx > 0) {
      const alias = key.substring(0, dotIdx);
      const prop = key.substring(dotIdx + 1);
      const output = this.entityOutputs.get(alias);
      if (output && prop in output) {
        return String((output as any)[prop]);
      }
    }
    return undefined;
  }

  /**
   * Resolve a JSON template, type-aware. A placeholder that fills an entire JSON string
   * token (e.g. "${mqttCleanSession}") whose form field is BOOLEAN or INTEGER is emitted as
   * a raw JSON boolean/number rather than a quoted string. Authored templates always quote
   * macros so the template file is valid JSON; the recorded form-field type decides the real
   * type at install time. Without this, "cleanSession": "${mqttCleanSession}" parses as the
   * string "false" (truthy) — so an unchecked boolean would be read as true. Placeholders
   * embedded in a larger string (e.g. "v3/${mqttUsername}/...") and STRING/PASSWORD/SELECT
   * fields are left quoted and handled by the regular resolveVariables() pass.
   */
  resolveTemplateJson(raw: string): any {
    const typed = raw.replace(/"\$\{([^"{}]+)}"/g, (whole, key) => {
      const type = this.fieldTypes.get(key);
      if (type !== FormFieldType.BOOLEAN && type !== FormFieldType.INTEGER) {
        return whole;
      }
      const res = this.resolveVariable(key);
      if (res === undefined) {
        return whole;
      }
      if (type === FormFieldType.BOOLEAN) {
        return String(res).trim().toLowerCase() === 'true' ? 'true' : 'false';
      }
      // INTEGER: emit a raw JSON number only when the value really is an integer;
      // otherwise leave it quoted for the regular pass (preserves prior behaviour).
      const trimmed = String(res).trim();
      const num = Number(trimmed);
      return trimmed !== '' && Number.isInteger(num) ? String(num) : whole;
    });
    return JSON.parse(this.resolveVariables(typed));
  }

  private _resolveMarkdownVariable(key: string): string | undefined {
    const res = this.resolveVariable(key);
    if (res) {
      return res;
    }
    // Special action placeholders
    if (key === 'gateway.downloadButton') {
      return '<a href="#" data-action="download-gateway-docker-compose" mat-stroked-button color="primary">⬇ Download docker-compose.yml</a>';
    }
    return undefined;
  }

  // --- Private ---

  private restoreInstallState(installState: Record<string, any>): void {
    for (const [stepName, data] of Object.entries(installState)) {
      if (data.formValues) {
        Object.assign(this.formValues, data.formValues);
      }
      if (data.entityOutput) {
        // Find the step type alias from the raw steps to set the correct entity output key
        const rawSteps = this.packageInfo.installSteps[this.selectedInstallMethod] || [];
        const matchingStep = rawSteps.find(s => s.name === stepName || this.resolveVariables(s.name) === stepName);
        if (matchingStep) {
          const alias = stepTypeAliasMap[matchingStep.type];
          if (alias) {
            this.entityOutputs.set(alias, data.entityOutput);
          }
        }
      }
    }
    // Resolve gateway docker-compose if available
    const gatewayStep = (this.packageInfo.installSteps[this.selectedInstallMethod] || []).find(s => s.type === 'GATEWAY');
    if (gatewayStep?.dockerCompose) {
      const raw = this.zipFiles.get(gatewayStep.dockerCompose);
      if (raw) {
        this.gatewayDockerComposeContent = this.resolveVariables(raw);
      }
    }
  }

  private startWizard(): void {
    if (!this.reviewMode) {
      this.formValues = {};
      this.entityOutputs.clear();
    }
    this.buildWizardSteps();
    this.wizardStarted = true;

    if (this.reviewMode) {
      // Pre-activate all steps for tab mode (tabs render lazily on first select)
      for (const ws of this.wizardSteps) {
        if (ws.type === 'instruction') {
          ws.markdown = this.zipFiles.get(ws.rawSteps[0].file) || '';
        } else if (ws.type === 'progress') {
          this.showCompletedEntitySteps(ws);
        }
      }
    } else {
      // Activate the first step after the stepper renders
      setTimeout(() => this.onStepActivated(), 0);
    }
  }

  private buildWizardSteps(): void {
    this.wizardSteps = [];
    if (!this.reviewMode && this.availableInstallMethods.length > 0 && !this.selectedInstallMethod) {
      // First step: connection method selector. Remaining steps are
      // appended via appendInstallSteps() once a method is confirmed.
      this.wizardSteps.push({
        type: 'connectivity',
        label: this.translate.instant('iot-hub.connection-method'),
        rawSteps: [],
        completed: false
      });
      // Dummy placeholder steps shown in the indicator until the user
      // picks a method. They are replaced with real steps for the
      // chosen connectivity in confirmConnectivity().
      for (const key of [
        'iot-hub.step-prerequisites',
        'iot-hub.step-configuration',
        'iot-hub.step-provisioning'
      ]) {
        this.wizardSteps.push({
          type: 'placeholder',
          label: this.translate.instant(key),
          rawSteps: [],
          completed: false
        });
      }
      return;
    }
    this.appendInstallSteps();
  }

  private appendInstallSteps(): void {
    const rawSteps = this.packageInfo.installSteps[this.selectedInstallMethod] || [];
    let i = 0;
    while (i < rawSteps.length) {
      const step = rawSteps[i];
      if (step.type === InstallStepType.SHOW_INSTRUCTION) {
        this.wizardSteps.push({
          type: 'instruction',
          label: step.name,
          rawSteps: [step],
          completed: this.reviewMode
        });
        i++;
      } else if (step.type === InstallStepType.SHOW_FORM) {
        this.wizardSteps.push({
          type: 'form',
          label: step.name,
          rawSteps: [step],
          completed: this.reviewMode
        });
        i++;
      } else if (ENTITY_STEP_TYPES.has(step.type)) {
        // Group consecutive entity steps
        const group: DeviceInstallStep[] = [];
        while (i < rawSteps.length && ENTITY_STEP_TYPES.has(rawSteps[i].type)) {
          group.push(rawSteps[i]);
          i++;
        }
        this.wizardSteps.push({
          type: 'progress',
          label: 'Provisioning',
          rawSteps: group,
          completed: this.reviewMode
        });
      } else {
        // Skip unsupported steps (CONVERTER, INTEGRATION)
        i++;
      }
    }
    // Initialize form groups
    for (const ws of this.wizardSteps) {
      if (ws.type === 'form' && !ws.formGroup) {
        this.initFormStep(ws);
      }
    }
  }

  private initFormStep(ws: WizardStep): void {
    const formJson = this.zipFiles.get(ws.rawSteps[0].file) || '[]';
    ws.formFields = JSON.parse(formJson) as FormFieldDefinition[];
    const controls: Record<string, UntypedFormControl> = {};
    // In review mode, use stored values; otherwise use defaults
    const storedValues = this.reviewMode && this.data.installState?.[ws.label]?.formValues;
    for (const field of ws.formFields) {
      this.fieldTypes.set(field.key, field.type);
      const validators = [];
      if (field.required) {
        validators.push(Validators.required);
      }
      if (field.validators?.length > 0) {
        validators.push(Validators.pattern(field.validators[0].pattern));
      }
      const initialValue = this.resolveInitialFieldValue(field, storedValues?.[field.key]);
      controls[field.key] = new UntypedFormControl(initialValue, validators);
    }
    ws.formGroup = new UntypedFormGroup(controls);
    if (this.reviewMode) {
      ws.formGroup.disable();
      ws.completed = true;
    }
  }

  private resolveInitialFieldValue(field: FormFieldDefinition, storedValue: any): any {
    if (storedValue !== undefined) {
      return storedValue;
    }
    const isStringLike = field.type === FormFieldType.STRING || field.type === FormFieldType.PASSWORD;
    if (isStringLike && field.randomByDefault) {
      return generateSecret(field.randomSize ?? DEFAULT_RANDOM_SIZE);
    }
    return field.defaultValue ?? (field.type === FormFieldType.BOOLEAN ? false : '');
  }

  private onStepActivated(): void {
    const step = this.currentWizardStep;
    if (!step) {
      return;
    }
    if (step.type === 'instruction') {
      step.markdown = this.zipFiles.get(step.rawSteps[0].file) || '';
    } else if (step.type === 'progress' && !step.progressDone) {
      if (this.reviewMode) {
        this.showCompletedEntitySteps(step);
      } else {
        this.initAndRunEntitySteps(step);
      }
    }
  }

  private showCompletedEntitySteps(ws: WizardStep): void {
    ws.entitySteps = ws.rawSteps.map(s => {
      const alias = stepTypeAliasMap[s.type];
      const output = alias ? this.entityOutputs.get(alias) : null;
      const storedState = this.data.installState?.[s.name] || this.data.installState?.[this.resolveVariables(s.name)];
      return {
        step: s,
        status: 'success' as const,
        resolvedName: this.resolveVariables(s.name),
        entityOutput: output || undefined,
        resolution: storedState?.resolution || 'created'
      };
    });
    ws.progressDone = true;
    ws.completed = true;
  }

  private initAndRunEntitySteps(ws: WizardStep): void {
    ws.entitySteps = ws.rawSteps.map(s => ({
      step: s,
      status: 'pending' as const,
      resolvedName: this.resolveVariables(s.name)
    }));
    ws.progressError = null;
    ws.progressDone = false;
    void this.runEntitySteps(ws);
  }

  private markEntityStepSuccess(ws: WizardStep, ep: EntityStepProgress,
                                output: EntityStepOutput, resolution: string): void {
    ep.entityOutput = output;
    ep.resolution = resolution;
    ep.status = 'success';
    const alias = stepTypeAliasMap[ep.step.type];
    if (alias) {
      this.entityOutputs.set(alias, output);
    }
    for (const remaining of ws.entitySteps) {
      if (remaining.status === 'pending') {
        remaining.resolvedName = this.resolveVariables(remaining.step.name);
      }
    }
    this.cdr.detectChanges();
  }

  private async runEntitySteps(ws: WizardStep): Promise<void> {
    for (const ep of ws.entitySteps) {
      if (ep.status === 'success') {
        continue;
      }
      ep.status = 'running';
      ep.errorMessage = null;
      this.cdr.detectChanges();
      try {
        // Pre-check for existing entity
        const existing = await this.preCheckEntity(ep.step);
        if (existing) {
          ep.status = 'conflict';
          ep.existingEntity = existing;
          ep.conflictType = this.getConflictType(ep.step.type);
          this.cdr.detectChanges();
          return; // Pause — user must choose a resolution
        }
        const startTime = Date.now();
        const output = await this.createEntity(ep.step);
        // Save optional attributes after entity creation
        await this.saveStepAttributes(ep.step, output);
        // Ensure minimum visible time per step
        const elapsed = Date.now() - startTime;
        if (elapsed < ENTITY_STEP_MIN_DELAY) {
          await this.delay(ENTITY_STEP_MIN_DELAY - elapsed);
        }
        this.markEntityStepSuccess(ws, ep, output, 'created');
      } catch (err: any) {
        ep.status = 'error';
        ep.errorMessage = err?.error?.message || err?.message || 'Unknown error';
        ws.progressError = ep.errorMessage;
        this.cdr.detectChanges();
        return;
      }
    }

    // All done — register install with created entity IDs
    ws.progressDone = true;
    ws.completed = true;
    try {
      const createdEntityIds = this.collectCreatedEntityIds();
      const dashboardId = this.findCreatedDashboardId();
      await firstValueFrom(
        this.iotHubApiService.registerDeviceInstall(
          this.data.item.id as string,
          { createdEntityIds, dashboardId, selectedInstallMethod: this.selectedInstallMethod, installState: this.buildInstallState() },
          { ignoreLoading: true }
        )
      );
    } catch (_e) {
      // Non-critical — entities are created, tracking registration failed
      console.error('Failed to register device install', _e);
    }

    // Auto-advance to next step after a short pause
    if (!this.isLastWizardStep) {
      await this.delay(500);
      this.stepper.next();
      this.cdr.detectChanges();
      this.onStepActivated();
    }
  }

  private async createEntity(step: DeviceInstallStep): Promise<EntityStepOutput> {
    const raw = this.zipFiles.get(step.template);
    if (!raw) {
      throw new Error(`Template file not found: ${step.template}`);
    }
    const template = this.resolveTemplateJson(raw);

    switch (step.type) {
      case InstallStepType.DEVICE_PROFILE: {
        const existing = await this.findDeviceProfileByName(template.name);
        if (existing) {
          return existing;
        }
        const result = await firstValueFrom(this.deviceProfileService.saveDeviceProfile(template, {ignoreErrors: true}));
        return { id: result.id.id, name: result.name, url: `/profiles/deviceProfiles/${result.id.id}` };
      }
      case InstallStepType.DEVICE: {
        const result = await firstValueFrom(this.deviceService.saveDevice(template, {ignoreErrors: false}));
        const creds = await this.resolveCredentials(step, result.id.id);
        return { id: result.id.id, name: result.name, url: `/entities/devices/${result.id.id}`, token: creds.credentialsId };
      }
      case InstallStepType.GATEWAY: {
        const result = await firstValueFrom(this.deviceService.saveDevice(template, {ignoreErrors: false}));
        const creds = await this.resolveCredentials(step, result.id.id);
        const output = {
          id: result.id.id,
          name: result.name,
          url: '/entities/gateways',
          token: creds.credentialsId,
          dockerComposeUrl: `/api/device-connectivity/gateway-launch/${result.id.id}/docker-compose/download`
        };
        // Store entity output early so docker-compose template can resolve ${gateway.token} etc.
        this.entityOutputs.set('gateway', output);
        // Resolve custom docker-compose template if provided
        if (step.dockerCompose) {
          const raw = this.zipFiles.get(step.dockerCompose);
          if (raw) {
            this.gatewayDockerComposeContent = this.resolveVariables(raw);
          }
        }
        return output;
      }
      case InstallStepType.GATEWAY_CONNECTOR: {
        const gatewayOutput = this.entityOutputs.get('gateway');
        if (!gatewayOutput) {
          throw new Error('GATEWAY step must precede GATEWAY_CONNECTOR');
        }
        const gatewayEntityId = { entityType: 'DEVICE', id: gatewayOutput.id } as EntityId;
        const connectorName = template.name;

        // Fetch current active_connectors
        let activeConnectors: string[] = [];
        try {
          const attrs = await firstValueFrom(
            this.attributeService.getEntityAttributes(gatewayEntityId, AttributeScope.SHARED_SCOPE, ['active_connectors'], {ignoreErrors: true})
          );
          const existing = attrs?.find(a => a.key === 'active_connectors');
          if (existing?.value && Array.isArray(existing.value)) {
            activeConnectors = existing.value;
          }
        } catch (_e) {
          // First connector — no existing attributes
        }

        // Append connector name
        if (!activeConnectors.includes(connectorName)) {
          activeConnectors.push(connectorName);
        }

        // Save attributes
        await firstValueFrom(this.attributeService.saveEntityAttributes(
          gatewayEntityId, AttributeScope.SHARED_SCOPE,
          [
            { key: 'active_connectors', value: activeConnectors },
            { key: connectorName, value: template }
          ],
          {ignoreErrors: true}
        ));

        return { id: gatewayOutput.id, name: connectorName };
      }
      case InstallStepType.DASHBOARD: {
        const result = await firstValueFrom(this.dashboardService.saveDashboard(template, {ignoreErrors: false}));
        return { id: result.id.id, name: result.title, url: `/dashboards/${result.id.id}` };
      }
      case InstallStepType.RULE_CHAIN: {
        const ruleChain = template.ruleChain || template;
        const metadata = template.metadata;
        const existing = await this.findRuleChainByName(ruleChain.name);
        if (existing) {
          return existing;
        }
        return this.saveRuleChainWithMetadata(ruleChain, metadata);
      }
      default:
        throw new Error(`Unsupported entity step type: ${step.type}`);
    }
  }

  private async saveRuleChainWithMetadata(ruleChain: any, metadata: any): Promise<EntityStepOutput> {
    const saved = await firstValueFrom(this.ruleChainService.saveRuleChain(ruleChain, {ignoreErrors: true}));
    if (metadata) {
      metadata.ruleChainId = saved.id;
      await firstValueFrom(this.ruleChainService.saveRuleChainMetadata(metadata, {ignoreErrors: true}));
    }
    return { id: saved.id.id, name: saved.name, url: `/ruleChains/${saved.id.id}` };
  }

  private async resolveCredentials(step: DeviceInstallStep, deviceId: string): Promise<any> {
    const creds = await firstValueFrom(this.deviceService.getDeviceCredentials(deviceId, false, {ignoreErrors: true}));
    if (step.credentials) {
      const raw = this.zipFiles.get(step.credentials);
      if (raw) {
        const credTemplate = this.resolveTemplateJson(raw);
        creds.credentialsType = credTemplate.credentialsType || creds.credentialsType;
        if (credTemplate.credentialsValue) {
          creds.credentialsValue = typeof credTemplate.credentialsValue === 'string'
            ? credTemplate.credentialsValue
            : JSON.stringify(credTemplate.credentialsValue);
        }
        if (credTemplate.credentialsId) {
          creds.credentialsId = credTemplate.credentialsId;
        }
        await firstValueFrom(this.deviceService.saveDeviceCredentials(creds, {ignoreErrors: true}));
      }
    }
    return creds;
  }

  private async findDeviceProfileByName(name: string): Promise<EntityStepOutput | null> {
    const profiles = await firstValueFrom(this.deviceProfileService.getDeviceProfileNames());
    const match = profiles.find(p => p.name === name);
    return match ? { id: match.id.id, name: match.name, url: `/profiles/deviceProfiles/${match.id.id}` } : null;
  }

  private async findRuleChainByName(name: string): Promise<EntityStepOutput | null> {
    const page = await firstValueFrom(this.ruleChainService.getRuleChains(new PageLink(100, 0, name)));
    const match = page.data.find(rc => rc.name === name);
    return match ? { id: match.id.id, name: match.name, url: `/ruleChains/${match.id.id}` } : null;
  }

  private async findDeviceByName(name: string): Promise<EntityStepOutput | null> {
    try {
      const device = await firstValueFrom(this.deviceService.findByName(name, {ignoreErrors: true}));
      if (device) {
        return { id: device.id.id, name: device.name, url: `/entities/devices/${device.id.id}` };
      }
    } catch (_e) {
      // 404 = not found
    }
    return null;
  }

  private async findDashboardByTitle(title: string): Promise<EntityStepOutput | null> {
    const page = await firstValueFrom(this.dashboardService.getTenantDashboards(new PageLink(10, 0, title), {ignoreErrors: true}));
    const match = page.data.find(d => d.title === title);
    return match ? { id: match.id.id, name: match.title, url: `/dashboards/${match.id.id}` } : null;
  }

  private async overwriteEntity(step: DeviceInstallStep, existing: EntityStepOutput): Promise<EntityStepOutput> {
    const raw = this.zipFiles.get(step.template);
    if (!raw) throw new Error(`Template file not found: ${step.template}`);
    const template = this.resolveTemplateJson(raw);

    switch (step.type) {
      case InstallStepType.DEVICE_PROFILE: {
        template.id = { id: existing.id, entityType: 'DEVICE_PROFILE' };
        const result = await firstValueFrom(this.deviceProfileService.saveDeviceProfile(template, {ignoreErrors: true}));
        return { id: result.id.id, name: result.name, url: `/profiles/deviceProfiles/${result.id.id}` };
      }
      case InstallStepType.DEVICE: {
        template.id = { id: existing.id, entityType: 'DEVICE' };
        const result = await firstValueFrom(this.deviceService.saveDevice(template, {ignoreErrors: false}));
        const creds = await this.resolveCredentials(step, result.id.id);
        return { id: result.id.id, name: result.name, url: `/entities/devices/${result.id.id}`, token: creds.credentialsId };
      }
      case InstallStepType.GATEWAY: {
        template.id = { id: existing.id, entityType: 'DEVICE' };
        const result = await firstValueFrom(this.deviceService.saveDevice(template, {ignoreErrors: false}));
        const creds = await this.resolveCredentials(step, result.id.id);
        const output: EntityStepOutput = {
          id: result.id.id,
          name: result.name,
          url: '/entities/gateways',
          token: creds.credentialsId,
          dockerComposeUrl: `/api/device-connectivity/gateway-launch/${result.id.id}/docker-compose/download`
        };
        this.entityOutputs.set('gateway', output);
        if (step.dockerCompose) {
          const dcRaw = this.zipFiles.get(step.dockerCompose);
          if (dcRaw) this.gatewayDockerComposeContent = this.resolveVariables(dcRaw);
        }
        return output;
      }
      case InstallStepType.DASHBOARD: {
        template.id = { id: existing.id, entityType: 'DASHBOARD' };
        const result = await firstValueFrom(this.dashboardService.saveDashboard(template, {ignoreErrors: false}));
        return { id: result.id.id, name: result.title, url: `/dashboards/${result.id.id}` };
      }
      case InstallStepType.RULE_CHAIN: {
        const ruleChain = template.ruleChain || template;
        const metadata = template.metadata;
        ruleChain.id = { id: existing.id, entityType: 'RULE_CHAIN' };
        return this.saveRuleChainWithMetadata(ruleChain, metadata);
      }
      default:
        throw new Error(`Unsupported overwrite for step type: ${step.type}`);
    }
  }

  private async preCheckEntity(step: DeviceInstallStep): Promise<EntityStepOutput | null> {
    const raw = this.zipFiles.get(step.template);
    if (!raw) return null;
    const template = this.resolveTemplateJson(raw);

    switch (step.type) {
      case InstallStepType.DEVICE_PROFILE:
        return this.findDeviceProfileByName(template.name);
      case InstallStepType.RULE_CHAIN: {
        const name = template.ruleChain?.name || template.name;
        return this.findRuleChainByName(name);
      }
      case InstallStepType.DEVICE:
      case InstallStepType.GATEWAY:
        return this.findDeviceByName(template.name);
      case InstallStepType.DASHBOARD:
        return this.findDashboardByTitle(template.title);
      default:
        return null;
    }
  }

  private getConflictType(stepType: InstallStepType): 'use-or-overwrite' | 'overwrite-or-copy' {
    return stepType === InstallStepType.DASHBOARD ? 'overwrite-or-copy' : 'use-or-overwrite';
  }

  private buildInstallState(): Record<string, any> {
    const state: Record<string, any> = {};
    for (const ws of this.wizardSteps) {
      if (ws.type === 'form' && ws.formGroup) {
        state[ws.label] = { formValues: ws.formGroup.getRawValue() };
      } else if (ws.type === 'progress' && ws.entitySteps) {
        for (const ep of ws.entitySteps) {
          if (ep.status === 'success' && ep.entityOutput) {
            state[ep.step.name] = { entityOutput: ep.entityOutput, resolution: ep.resolution || 'created' };
          }
        }
      }
    }
    return state;
  }

  private collectCreatedEntityIds(): { entityType: string; id: string }[] {
    const ids: { entityType: string; id: string }[] = [];
    for (const ws of this.wizardSteps) {
      if (ws.type === 'progress' && ws.entitySteps) {
        for (const ep of ws.entitySteps) {
          if (ep.status === 'success' && ep.entityOutput) {
            const entityType = this.stepTypeToEntityType(ep.step.type);
            if (entityType) {
              ids.push({ entityType, id: ep.entityOutput.id });
            }
          }
        }
      }
    }
    return ids;
  }

  private findCreatedDashboardId(): { entityType: string; id: string } | undefined {
    for (const ws of this.wizardSteps) {
      if (ws.type === 'progress' && ws.entitySteps) {
        for (const ep of ws.entitySteps) {
          if (ep.step.type === InstallStepType.DASHBOARD && ep.status === 'success' && ep.entityOutput) {
            return { entityType: 'DASHBOARD', id: ep.entityOutput.id };
          }
        }
      }
    }
    return undefined;
  }

  private async saveStepAttributes(step: DeviceInstallStep, output: EntityStepOutput): Promise<void> {
    const entityType = this.stepTypeToEntityType(step.type);
    if (!entityType) {
      return;
    }
    const entityId = { entityType, id: output.id } as EntityId;
    if (step.serverAttributes) {
      const raw = this.zipFiles.get(step.serverAttributes);
      if (raw) {
        const resolved = this.resolveTemplateJson(raw);
        const attrs = Object.entries(resolved).map(([key, value]) => ({ key, value }));
        await firstValueFrom(this.attributeService.saveEntityAttributes(
          entityId, AttributeScope.SERVER_SCOPE, attrs, {ignoreErrors: true}
        ));
      }
    }
    if (step.sharedAttributes) {
      const raw = this.zipFiles.get(step.sharedAttributes);
      if (raw) {
        const resolved = this.resolveTemplateJson(raw);
        const attrs = Object.entries(resolved).map(([key, value]) => ({ key, value }));
        await firstValueFrom(this.attributeService.saveEntityAttributes(
          entityId, AttributeScope.SHARED_SCOPE, attrs, {ignoreErrors: true}
        ));
      }
    }
  }

  private stepTypeToEntityType(stepType: InstallStepType): string | null {
    switch (stepType) {
      case InstallStepType.DEVICE_PROFILE: return 'DEVICE_PROFILE';
      case InstallStepType.DEVICE: return 'DEVICE';
      case InstallStepType.GATEWAY: return 'DEVICE';
      case InstallStepType.GATEWAY_CONNECTOR: return 'DEVICE';
      case InstallStepType.DASHBOARD: return 'DASHBOARD';
      case InstallStepType.RULE_CHAIN: return 'RULE_CHAIN';
      default: return null;
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
