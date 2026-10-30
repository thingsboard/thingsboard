// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, DestroyRef, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatStepper } from '@angular/material/stepper';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { firstValueFrom, Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { PageLink } from '@shared/models/page/page-link';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import {
  DeviceInstalledItemDescriptor,
  DevicePackageEntityState,
  DeviceUpdateResult,
  DeviceUpdateStep
} from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { DeviceService } from '@core/http/device.service';
import { DashboardService } from '@core/http/dashboard.service';
import { RuleChainService } from '@core/http/rule-chain.service';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { generateSecret, isEqual } from '@core/utils';
import { ConverterService } from '@core/http/converter.service';
import { IntegrationService } from '@core/http/integration.service';
import { DialogService } from '@core/services/dialog.service';
import { Converter, ConverterType } from '@shared/models/converter.models';
import { AiDashboardGenerationService } from '@home/components/ai/ai-dashboard-generation.service';
import {
  cleanFormFieldValue,
  DeviceInstallStep,
  DevicePackageInfo,
  ENTITY_STEP_TYPES,
  EntityStepOutput,
  EntityStepProgress,
  FormFieldDefinition,
  FormFieldType,
  installMethodIcons as INSTALL_METHOD_ICONS,
  installMethodLabels as INSTALL_METHOD_LABELS,
  InstallMethod,
  InstallStepType,
  pastedPortOf,
  stepTypeAliasMap
} from '@shared/models/iot-hub/device-package.models';
import { mergeMap } from 'rxjs/operators';
import {
  DeviceConnectivityHostSource,
  DeviceConnectivityProtocol,
  ResolvedDeviceConnectivityInfo
} from '@shared/models/settings.models';

export interface DeviceInstallDialogData {
  item: MpItemVersionView;
  reviewMode?: boolean;
  selectedInstallMethod?: string;
  installState?: Record<string, any>;
  entityGroupId?: string;
  customerId?: string;
  // Set when the wizard updates an installed package to the item version
  update?: DeviceInstallUpdate;
}

/** Update: an entity the installed version has for a step. */
interface PreviousEntity {
  output: EntityStepOutput;
  // The user kept an entity they already had at install: the update doesn't change it either
  useExisting: boolean;
  // Other installed device packages use it, so overwriting it affects them
  shared: boolean;
  // What the install state records for it, so that the next update decides again
  resolution: string;
  // The install state entry it was found under: the installed step's name when the new version renamed it
  stateKey: string;
  // What the server's check found for it
  state?: DevicePackageEntityState;
}

/** Update: what the update does with the entity of a step, decided on the Update step before anything changes. */
interface EntityStepPlan {
  // keep: the new version doesn't change it; overwrite: it takes the new version's template; create: a new entity
  action: 'keep' | 'overwrite' | 'create';
  previous?: PreviousEntity;
  // Overwrite: warnings the user confirms first
  shared?: boolean;
  modified?: boolean;
  // Create: an entity with this name already exists, and the user chooses a resolution for it
  existing?: EntityStepOutput;
  resolution?: string;
  confirmed: boolean;
  // Overwrite: the version the user reviewed; the server rejects the update if someone changed the entity since
  version?: number;
  // Modified: the entity as the check found it, which the user can download before it is overwritten
  current?: any;
}

/**
 * The installed package an update changes. The steps of the new version change the entities
 * the package created in place, so the device keeps its id, credentials and history.
 */
export interface DeviceInstallUpdate {
  installedItemId: string;
  descriptor: DeviceInstalledItemDescriptor;
  // The installed version, which the update compares the new one with
  fromVersionId: string;
  fromVersion: string;
}

// ThingsBoard error codes whose message is written for the user: validation, permission, not found, limits
const SHOWN_ERROR_CODES = new Set([20, 30, 31, 32, 33, 34, 35, 40, 41]);
const MAX_SHOWN_ERROR_LENGTH = 200;

/** The message as it is when it reads like one written for the user, null for an internal one (a stack trace, a URL, too long). */
const readableMessage = (message: unknown): string | null =>
  typeof message === 'string' && message.trim() && message.length <= MAX_SHOWN_ERROR_LENGTH &&
  !/exception|java\.|https?:\/\/|\bat [\w.$]+\(/i.test(message) ? message : null;

// Fields the server sets, or that the update keeps as they are: they don't count as changes the user made.
// Nor do the debug settings, which change nothing the entity does, and whether a profile is the default one.
const SERVER_SET_KEYS = new Set(['id', 'name', 'title', 'routingKey', 'version', 'createdTime', 'tenantId', 'customerId',
  'ownerId', 'assignedCustomers', 'externalId', 'ruleChainId', 'resources', 'debugMode', 'debugSettings', 'default']);

// The same for a rule chain, whose first node and root flag the server sets, and for its nodes
const RULE_CHAIN_SERVER_SET_KEYS = new Set([...SERVER_SET_KEYS, 'firstRuleNodeId', 'root']);
const RULE_NODE_SERVER_SET_KEYS = new Set(['id', 'createdTime', 'ruleChainId', 'externalId', 'debugMode', 'debugSettings']);

// Image and resource links: the server turns a dashboard's base64 images and resource links into its own on save
const IMAGE_OR_RESOURCE_LINK = /^(data:|tb-image;|tb-resource;|\/api\/images\/|\/api\/resource\/)/;

/** The value with every image and resource link replaced by the same marker, so they compare as equal. */
const withoutImageLinks = (value: any): any => {
  if (typeof value === 'string') {
    return IMAGE_OR_RESOURCE_LINK.test(value) ? '<link>' : value;
  }
  if (Array.isArray(value)) {
    return value.map(withoutImageLinks);
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, withoutImageLinks(item)]));
  }
  return value;
};

/**
 * For packages installed before checksums were kept: true when the entity still has the values the template
 * gave it. Fields the template doesn't set (defaults the server added) and fields the server dropped don't count,
 * so a change there goes unnoticed; skipKeys are left out at the top level only.
 */
const matchesTemplate = (template: any, actual: any, skipKeys: Set<string> = SERVER_SET_KEYS): boolean => {
  if (template === null || template === undefined || template === '') {
    return actual === null || actual === undefined || actual === '';
  }
  if (Array.isArray(template)) {
    return Array.isArray(actual) && actual.length === template.length &&
      template.every((value, i) => matchesTemplate(value, actual[i], new Set()));
  }
  if (typeof template === 'object') {
    return actual !== null && typeof actual === 'object' && !Array.isArray(actual) &&
      Object.keys(template).every(key => skipKeys.has(key) || actual[key] === undefined ||
        matchesTemplate(template[key], actual[key], new Set()));
  }
  return template === actual;
};

/**
 * matchesTemplate for rule chain metadata. The server upgrades a node's configuration saved with an older
 * configurationVersion, so such a node is compared without its configuration.
 */
const matchesRuleChainMetadata = (template: any, actual: any): boolean => {
  const actualNodes: any[] = actual?.nodes ?? [];
  const nodes = (template.nodes ?? []).map((node: any, i: number) => {
    const comparable = Object.fromEntries(Object.entries(node).filter(([key]) => !RULE_NODE_SERVER_SET_KEYS.has(key)));
    if ((node.configurationVersion ?? 0) !== (actualNodes[i]?.configurationVersion ?? 0)) {
      delete comparable.configuration;
      delete comparable.configurationVersion;
    }
    return comparable;
  });
  return matchesTemplate({ ...template, nodes }, actual);
};

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
  // Fields the dialog adds on top of the package's form file
  extraFormFields?: FormFieldDefinition[];
  // Key of the step's form values in the install state; the label when not set
  stateKey?: string;
  formGroup?: UntypedFormGroup;
  // Connection check of the integration the form configures; null when the form configures none
  connectionCheck?: ConnectionCheck | null;
  // Progress
  entitySteps?: EntityStepProgress[];
  progressError?: string;
  progressDone?: boolean;
  // Update: the user reviews what the update changes and confirms the warnings (checking: still finding it out)
  reviewing?: boolean;
  checking?: boolean;
  // Update: the entities are being changed
  updating?: boolean;
}

export interface ConnectionCheck {
  // The INTEGRATION step template that the form values fill
  template: string;
  status: 'idle' | 'checking' | 'success' | 'error';
  // The values the status is for, as JSON: only the fields the integration template uses
  checkedValues?: string;
}

const ENTITY_STEP_MIN_DELAY = 2000;
const DEFAULT_RANDOM_SIZE = 20;
const TRANSPORT_HOST_FIELD_KEY = 'tbTransportHost';
const TRANSPORT_PORT_FIELD_KEY = 'tbTransportPort';
// Not the translated label, so review mode finds the values in any UI language
const TRANSPORT_STEP_STATE_KEY = 'tbTransportConnection';

/**
 * A transport whose ${<protocol>.host} / ${<protocol>.port} the install form lets the user check,
 * with the translation keys of its fields. The wording is per transport (the label, the hints and
 * the help popup name the protocol and its default port), so each entry brings its own keys.
 */
interface HostFieldTransport {
  protocol: DeviceConnectivityProtocol;
  hostLabel: string;
  portLabel: string;
  // Hint by the source of the host; REQUEST is used when the source is unknown
  hostHints: Partial<Record<DeviceConnectivityHostSource, string>> & { REQUEST: string };
  // Hint by the source of a host that resolved to a loopback address; loopbackHint when there is none
  loopbackHints: Partial<Record<DeviceConnectivityHostSource, string>>;
  loopbackHint: string;
  // Review mode: the stored value is shown without its source
  reviewHostHint: string;
  // Host set in Device connectivity: the fields are read-only
  configuredHint: string;
  hostError: string;
  portError: string;
  helpPopupId: string;
  helpPopupText: string;
}

// Install methods whose packages get the transport fields. Other methods use the host differently
// (a ChirpStack package builds the ThingsBoard URL from it, a gateway puts it into docker-compose)
// and keep the resolved value until they are handled separately.
const HOST_FIELD_TRANSPORTS = new Map<string, HostFieldTransport>([
  [InstallMethod.DIRECT_MQTT, {
    protocol: 'mqtt',
    hostLabel: 'iot-hub.device-install-mqtt-host',
    portLabel: 'iot-hub.device-install-mqtt-port',
    hostHints: {
      REQUEST: 'iot-hub.device-install-mqtt-host-request-hint',
      BASE_URL: 'iot-hub.device-install-mqtt-host-base-url-hint'
    },
    loopbackHints: {
      REQUEST: 'iot-hub.device-install-mqtt-host-opened-via-loopback-hint',
      BASE_URL: 'iot-hub.device-install-mqtt-host-base-url-loopback-hint'
    },
    loopbackHint: 'iot-hub.device-install-mqtt-host-loopback-hint',
    reviewHostHint: 'iot-hub.device-install-mqtt-host-hint',
    configuredHint: 'iot-hub.device-install-mqtt-host-configured-hint',
    hostError: 'iot-hub.device-install-transport-host-error',
    portError: 'iot-hub.device-install-transport-port-error',
    helpPopupId: 'iot-hub/device-connection-host',
    helpPopupText: 'iot-hub.device-install-which-host'
  }]
]);

/**
 * Reduces what a user may paste (a URL, host:port, [IPv6]:port) to the bare host: the port is
 * a separate setting, and the scheme or path would end up in the firmware as part of the host.
 */
const toBareHost = (value: string): string => {
  const hostPort = value.trim().replace(/^[a-z][a-z\d+.-]*:\/\//i, '').replace(/[/?#].*$/, '');
  const bracketed = hostPort.match(/^\[([^\]]*)](:\d*)?$/);
  if (bracketed) {
    return bracketed[1];
  }
  // A bare IPv6 address has several colons; a single one separates the port.
  const parts = hostPort.split(':');
  return parts.length === 2 ? parts[0] : hostPort;
};

// localhost, 127.0.0.0/8, ::1 and 0.0.0.0: a device can't reach the server at any of them.
// Unlike DeviceConnectivityUtil.isLocalhost this doesn't resolve hostnames, and it also
// rejects 0.0.0.0, which isn't a loopback address but is just as unreachable.
// Takes a bare host (see toBareHost).
const isLoopbackHost = (host: string): boolean => {
  const h = host.toLowerCase();
  return h === 'localhost' || h === '::1' || h === '0.0.0.0' || /^127(\.\d{1,3}){3}$/.test(h);
};

// Host name, IPv4 or IPv6 characters only: the value goes into C string literals and JSON templates.
const HOST_PATTERN = /^[a-z\d._:-]+$/i;

const transportHostValidator = (control: AbstractControl): ValidationErrors | null => {
  if (!control.value) {
    return null;
  }
  const host = toBareHost(control.value);
  return !HOST_PATTERN.test(host) || isLoopbackHost(host) ? { transportHost: true } : null;
};

const isValidPort = (port: number): boolean => Number.isInteger(port) && port >= 1 && port <= 65535;

const transportPortValidator = (control: AbstractControl): ValidationErrors | null =>
  control.value === null || control.value === '' || isValidPort(Number(control.value)) ? null : { transportPort: true };

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

  // Wizard
  wizardSteps: WizardStep[] = [];
  wizardStarted = false;
  reviewMode = false;
  allowAiDashboardGenerate = false;
  /**
   * Set once the server has an installed item for this device. From that moment every way out of
   * the wizard — including the header X and the Cancel button on a still-pending later step —
   * has to report 'installed', or the caller keeps showing pre-install counts and badges.
   */
  private installRegistered = false;
  // Update: the package's entities as the server finds them, by install step
  private updateStates = new Map<string, DevicePackageEntityState>();
  // Update: the files of the installed version, which the new version is compared with
  private previousFiles = new Map<string, string>();
  private previousPackageInfo: DevicePackageInfo | null = null;
  private entityPlans = new WeakMap<EntityStepProgress, EntityStepPlan>();

  // Variable resolution state
  formValues: Record<string, any> = {};
  private formTemplateSubscriptions: Subscription[] = [];
  entityOutputs = new Map<string, EntityStepOutput>();
  transportVars: Record<string, string> = {};
  // Routing key pre-allocated for the in-flight INTEGRATION step so ${routingKey}
  // placeholders (e.g. inside clientConfiguration.httpEndpoint emitted by the
  // export pipeline) substitute to the value we'll save.
  private pendingRoutingKey: string | undefined;
  // protocol → where the server took its host from
  private transportHostSources: Partial<Record<DeviceConnectivityProtocol, DeviceConnectivityHostSource>> = {};
  // form-field key → declared type. Used by resolveTemplateJson() to emit BOOLEAN/INTEGER
  // placeholders as raw JSON values instead of quoted strings.
  private fieldTypes = new Map<string, FormFieldType>();
  // Transport whose ${<protocol>.host} the transport host field overrides
  private hostFieldProtocol: DeviceConnectivityProtocol | null = null;
  gatewayDockerComposeContent: string | null = null;

  resolveMarkdownVariable: (key: string) => string | undefined = this._resolveMarkdownVariable.bind(this);

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbDeviceInstallDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DeviceInstallDialogData,
    private destroyRef: DestroyRef,
    private cdr: ChangeDetectorRef,
    private deviceProfileService: DeviceProfileService,
    private deviceService: DeviceService,
    private dashboardService: DashboardService,
    private ruleChainService: RuleChainService,
    private attributeService: AttributeService,
    private iotHubApiService: IotHubApiService,
    private converterService: ConverterService,
    private integrationService: IntegrationService,
    private aiDashboardGenerationService: AiDashboardGenerationService,
    private dialogService: DialogService,
    private translate: TranslateService
  ) {
    super(store, router, dialogRef);
  }

  async ngOnInit(): Promise<void> {
    this.allowAiDashboardGenerate = this.aiDashboardGenerationService.isAllowedDashboardGenerate({ requireTelemetry: true });
    try {
      await this.loadPackageFiles(this.data.item.id as string, this.zipFiles, this.zipImages);
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
        for (const [protocol, info] of Object.entries(connectivity) as [DeviceConnectivityProtocol, ResolvedDeviceConnectivityInfo][]) {
          if (info) {
            this.transportVars[`${protocol}.host`] = info.host || '';
            this.transportVars[`${protocol}.port`] = String(info.port || '');
            this.transportHostSources[protocol] = info.hostSource;
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
      // After startWizard: the transport fields it adds take part in resolving the file
      this.resolveGatewayDockerCompose();
    } else if (this.availableInstallMethods.includes(this.data.update?.descriptor.selectedInstallMethod)) {
      // Update: the package keeps the method it was installed with
      this.selectedInstallMethod = this.data.update.descriptor.selectedInstallMethod;
      await this.loadPreviousVersion();
      if (this.isTextOnlyUpdate() && await this.registerTextOnlyUpdate()) {
        return;
      }
      this.startWizard();
    } else if (this.availableInstallMethods.length === 1) {
      // Single method: skip the selector and start the wizard.
      this.selectedInstallMethod = this.availableInstallMethods[0];
      this.startWizard();
    } else {
      // Multiple methods: render the connection method step as the
      // wizard's first step.
      this.startWizard();
    }
    this.loading = false;
    this.cdr.detectChanges();
  }

  ngOnDestroy() {
    this.formTemplateSubscriptions.forEach(subscription => subscription.unsubscribe());
    for (const url of this.zipImages.values()) {
      URL.revokeObjectURL(url);
    }
    super.ngOnDestroy();
  }

  // --- Connectivity ---

  selectConnectivity(ct: string): void {
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
      setTimeout(() => this.stepper?.next(), 0);
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
    if (this.data.update && step?.type === 'progress') {
      if (step.updating || step.checking) {
        return;
      }
      if (!step.progressDone) {
        // Coming back to it checks the update again, e.g. with the configuration changed
        step.entitySteps = null;
        step.reviewing = false;
        step.progressError = null;
      }
      this.stepper.previous();
    } else if (step?.type === 'progress' && step.progressError) {
      this.goBackToForm();
    } else {
      this.stepper.previous();
    }
  }

  async nextStep(): Promise<void> {
    const step = this.currentWizardStep;
    if (!step) {
      return;
    }
    if (step.type === 'form' && step.formGroup?.invalid) {
      step.formGroup.markAllAsTouched();
      return;
    }
    // Checks the connection unless these values already passed the check; a failed check stops here
    const check = this.connectionCheckOf(step);
    if (check && !(check.status === 'success' && check.checkedValues === this.checkedValuesOf(step, check))) {
      if (check.status === 'checking' || !(await this.checkConnection(step))) {
        return;
      }
    }
    if (this.isLastWizardStep) {
      this.done();
      return;
    }
    step.completed = true;
    this.cdr.detectChanges();
    this.stepper.next();
  }

  connectionCheckOf(ws: WizardStep): ConnectionCheck | undefined {
    if (this.reviewMode || ws.type !== 'form' || !ws.formFields) {
      return undefined;
    }
    if (ws.connectionCheck === undefined) {
      const template = this.hasCheckConnectionField(ws) ? this.findIntegrationTemplate(ws) : undefined;
      ws.connectionCheck = template ? { template, status: 'idle' } : null;
    }
    return ws.connectionCheck ?? undefined;
  }

  /** True when a field of the package's form asks Next to check the connection. */
  private hasCheckConnectionField(ws: WizardStep): boolean {
    return !!ws.formFields?.some(field => field.checkConnection);
  }

  /** True while Next checks the connection; the form and the buttons wait for it. */
  connectionChecking(ws: WizardStep | undefined): boolean {
    return !!ws && ws.connectionCheck?.status === 'checking';
  }

  private async checkConnection(ws: WizardStep): Promise<boolean> {
    const check = this.connectionCheckOf(ws);
    if (!check) {
      return true;
    }
    if (ws.formGroup.invalid) {
      ws.formGroup.markAllAsTouched();
      return false;
    }
    this.cleanFormStep(ws);
    const values = ws.formGroup.getRawValue();
    check.status = 'checking';
    this.cdr.detectChanges();
    const committedValues = this.formValues;
    let body: any;
    try {
      // The integration is built the way Provisioning builds it, from the values not committed yet
      this.formValues = { ...committedValues, ...values };
      body = this.resolveTemplateJson(this.zipFiles.get(check.template));
    } finally {
      this.formValues = committedValues;
    }
    // The check requires a routing key; Provisioning adds one the same way
    if (!body.routingKey) body.routingKey = this.generateUuid();
    if (!body.configuration) body.configuration = {};
    if (body.configuration.metadata == null) body.configuration.metadata = {};
    try {
      await firstValueFrom(this.integrationService.checkIntegrationConnection(body, { ignoreErrors: true }));
      check.status = 'success';
    } catch {
      check.status = 'error';
    }
    check.checkedValues = this.checkedValuesOf(ws, check);
    this.cdr.detectChanges();
    if (check.status === 'error') {
      // The backend's error is not shown: the package's text names the fields to check
      const checkedField = ws.formFields.find(field => field.checkConnection);
      const method = this.installMethodLabels.get(this.selectedInstallMethod) || this.selectedInstallMethod;
      await firstValueFrom(this.dialogService.alert(
        this.translate.instant('iot-hub.connection-check-failed-title', { method }),
        checkedField?.checkConnectionFailedText || this.translate.instant('iot-hub.connection-check-failed-text'),
        this.translate.instant('iot-hub.back-to-configuration')));
    }
    return check.status === 'success';
  }

  /**
   * The values of the connection fields: the ones the integration template puts into its configuration
   * (host, port, credentials), not e.g. its name. Other fields don't reset the check result.
   */
  private checkedValuesOf(ws: WizardStep, check: ConnectionCheck): string {
    const content = this.zipFiles.get(check.template) ?? '';
    const configurationStart = content.indexOf('"configuration"');
    const template = configurationStart >= 0 ? content.substring(configurationStart) : content;
    const values = this.cleanedFormValues(ws);
    const used = ws.formFields
      .filter(field => template.includes(`\${${field.key}}`))
      .map(field => [field.key, values[field.key]]);
    return JSON.stringify(used);
  }

  private findIntegrationTemplate(ws: WizardStep): string | undefined {
    for (const step of this.wizardSteps.flatMap(w => w.rawSteps)) {
      if (step.type !== InstallStepType.INTEGRATION || !step.template) {
        continue;
      }
      const content = this.zipFiles.get(step.template) ?? '';
      if (ws.formFields.some(field => content.includes(`\${${field.key}}`))) {
        return step.template;
      }
    }
    return undefined;
  }

  onStepSelectionChange(event: StepperSelectionEvent): void {
    if (event.selectedIndex <= event.previouslySelectedIndex) {
      return;
    }
    // Forward moves can come from the stepper header, which bypasses nextStep(),
    // so commit every step being passed over here.
    let formValuesChanged = false;
    for (let i = event.previouslySelectedIndex; i < event.selectedIndex; i++) {
      const ws = this.wizardSteps[i];
      if (ws.type === 'form') {
        // Keep the bare host, so the stored state and the form after Back show what the device gets
        const hostControl = ws.formGroup.get(TRANSPORT_HOST_FIELD_KEY);
        if (hostControl?.enabled && hostControl.value) {
          hostControl.setValue(toBareHost(hostControl.value), { emitEvent: false });
        }
        this.cleanFormStep(ws);
        Object.assign(this.formValues, ws.formGroup.getRawValue());
        formValuesChanged = true;
      }
      ws.completed = true;
    }
    if (formValuesChanged) {
      // Instruction steps stay rendered, and their markdown only changes when the resolver does:
      // after Back and a corrected value, they would otherwise keep showing the old one.
      this.resolveMarkdownVariable = this._resolveMarkdownVariable.bind(this);
    }
    this.onStepActivated(this.wizardSteps[event.selectedIndex]);
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

  get canGenerateAiDashboard(): boolean {
    return this.allowAiDashboardGenerate
      && !this.entityOutputs.get('dashboard')?.url
      && !!this.entityOutputs.get('device')?.id;
  }

  generateDashboardWithAi(): void {
    const deviceId = this.entityOutputs.get('device')?.id;
    if (!deviceId) {
      return;
    }
    this.aiDashboardGenerationService.generateWithTelemetryCheck({
      deviceId,
      destroyRef: this.destroyRef,
      noTelemetry: { hideSendTelemetry: true }
    });
  }

  done(): void {
    // Update: the installed item keeps the old version until the registration succeeds
    this.dialogRef.close(this.data.update && !this.installRegistered ? false : 'installed');
  }

  cancel(): void {
    // Update: the server is applying it; closing now would hide whether it did
    if (this.isUpdating) {
      return;
    }
    this.dialogRef.close(this.installRegistered ? 'installed' : false);
  }

  get isUpdating(): boolean {
    return this.wizardSteps.some(ws => ws.updating);
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
        output = await this.useExistingEntity(ep.step, ep.existingEntity);
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
      ep.errorMessage = this.failureMessage(err);
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
  // Arrow form preserves `this` when invoked from the renderer's template.
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
    if (this.hostFieldProtocol && key === `${this.hostFieldProtocol}.host`) {
      const host = toBareHost(String(this.formValues[TRANSPORT_HOST_FIELD_KEY] ?? ''));
      if (host) {
        return host;
      }
    }
    if (this.hostFieldProtocol && key === `${this.hostFieldProtocol}.port`) {
      const port = this.formValues[TRANSPORT_PORT_FIELD_KEY];
      if (port !== undefined && port !== null && port !== '') {
        return String(port);
      }
    }
    if (key in this.transportVars) {
      return this.transportVars[key];
    }
    // Smart platform URL: prefer https (skipping default 443), fall back to http
    // (skipping default 80). Emitted by the export pipeline for baseUrl /
    // httpEndpoint fields so the original installer's host doesn't leak.
    if (key === 'baseUrl') {
      return this.platformBaseUrl();
    }
    if (key === 'routingKey' && this.pendingRoutingKey) {
      return this.pendingRoutingKey;
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

  private platformBaseUrl(): string | undefined {
    const httpsHost = this.transportVars['https.host'];
    if (httpsHost) {
      const port = this.transportVars['https.port'];
      return port && port !== '443' ? `https://${httpsHost}:${port}` : `https://${httpsHost}`;
    }
    const httpHost = this.transportVars['http.host'];
    if (httpHost) {
      const port = this.transportVars['http.port'];
      return port && port !== '80' ? `http://${httpHost}:${port}` : `http://${httpHost}`;
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

  /** withFormValues false: only the entity outputs, e.g. to keep the values the user has just entered. */
  private restoreInstallState(installState: Record<string, any>, withFormValues = true): void {
    for (const [stepName, data] of Object.entries(installState)) {
      if (withFormValues && data.formValues) {
        Object.assign(this.formValues, data.formValues);
      }
      if (data.entityOutput) {
        // Find the step type alias from the raw steps to set the correct entity output key
        const rawSteps = this.packageInfo.installSteps[this.selectedInstallMethod] || [];
        const matchingStep = rawSteps.find(s => s.name === stepName || this.resolveVariables(s.name) === stepName) ??
          // Update: a step the new version renamed
          (this.data.update ? rawSteps.find(s => this.findPreviousStep(s)?.name === stepName) : undefined);
        if (matchingStep) {
          const alias = stepTypeAliasMap[matchingStep.type];
          if (alias) {
            this.entityOutputs.set(alias, data.entityOutput);
          }
        }
      }
    }
  }

  private resolveGatewayDockerCompose(): void {
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
    if (this.data.update) {
      // The instructions before the Update step refer to the installed entities, e.g. ${device.token}
      this.restoreInstallState(this.data.update.descriptor.installState ?? {}, false);
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
    } else if (this.data.update) {
      setTimeout(() => this.goToUpdateStart(), 0);
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
    const firstInstallStep = this.wizardSteps.length;
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
      } else if (ENTITY_STEP_TYPES.has(step.type) && this.data.update) {
        // An update is one transaction: every entity step goes in one Update step, after the last form
        // the entities may use, i.e. where the last group of entity steps is
        if (!rawSteps.slice(i + 1).some(s => ENTITY_STEP_TYPES.has(s.type))) {
          this.wizardSteps.push({
            type: 'progress',
            label: this.translate.instant('iot-hub.device-update-step'),
            rawSteps: rawSteps.filter(s => ENTITY_STEP_TYPES.has(s.type)),
            completed: false
          });
        }
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
        // Unknown step type — skip
        i++;
      }
    }
    this.addTransportFields(firstInstallStep);
    // Initialize form groups
    for (const ws of this.wizardSteps) {
      if (ws.type === 'form' && !ws.formGroup) {
        this.initFormStep(ws);
      }
    }
  }

  /**
   * Packages write ${<protocol>.host} and ${<protocol>.port} into sketches and templates. Show them
   * in the last form before the host is first used. When the system administrator set the host in
   * Device connectivity, they are read-only. Otherwise the user can correct them — and must, when
   * the host resolved to localhost, which on the device is the device itself.
   */
  private addTransportFields(fromIndex: number): void {
    this.hostFieldProtocol = null;
    const transport = HOST_FIELD_TRANSPORTS.get(this.selectedInstallMethod);
    const protocol = transport?.protocol;
    const serverHost = protocol ? this.transportVars[`${protocol}.host`] : undefined;
    if (serverHost === undefined) {
      return;
    }
    // The Device connectivity host isn't validated, so it can come with a port or a scheme (localhost:1883)
    const resolvedHost = toBareHost(serverHost);
    const steps = this.wizardSteps.slice(fromIndex);
    const firstUse = steps.findIndex(ws => this.usesVariable(ws, `${protocol}.host`));
    if (firstUse < 0) {
      return;
    }
    const loopback = !resolvedHost || isLoopbackHost(resolvedHost);
    const hostSource = this.transportHostSources[protocol];
    const readOnly = hostSource === 'DEVICE_CONNECTIVITY' && !loopback;
    let formStep = steps.slice(0, firstUse).reverse().find(ws => ws.type === 'form');
    if (!formStep) {
      formStep = {
        type: 'form',
        label: this.translate.instant('iot-hub.device-install-transport-host-step'),
        stateKey: TRANSPORT_STEP_STATE_KEY,
        rawSteps: [],
        completed: this.reviewMode
      };
      this.wizardSteps.splice(fromIndex + firstUse, 0, formStep);
    }
    let hostHint: string;
    if (readOnly) {
      hostHint = transport.configuredHint;
    } else if (this.reviewMode) {
      hostHint = transport.reviewHostHint;
    } else if (loopback) {
      hostHint = (hostSource && transport.loopbackHints[hostSource]) || transport.loopbackHint;
    } else {
      hostHint = (hostSource && transport.hostHints[hostSource]) || transport.hostHints.REQUEST;
    }
    const helpPopupId = readOnly ? undefined : transport.helpPopupId;
    const group = this.translate.instant('iot-hub.device-install-thingsboard-connection');
    formStep.extraFormFields = [{
      key: TRANSPORT_HOST_FIELD_KEY,
      label: this.translate.instant(transport.hostLabel),
      type: FormFieldType.STRING,
      defaultValue: loopback ? '' : resolvedHost,
      required: true,
      readOnly,
      helpText: this.translate.instant(hostHint, { host: resolvedHost }),
      // Shown on the transportHost error of transportHostValidator
      errorMessage: this.translate.instant(transport.hostError),
      helpPopupId,
      helpPopupText: helpPopupId ? this.translate.instant(transport.helpPopupText) : undefined,
      group,
      row: TRANSPORT_HOST_FIELD_KEY
    }];
    if (steps.some(ws => this.usesVariable(ws, `${protocol}.port`))) {
      const resolvedPort = Number(this.transportVars[`${protocol}.port`]);
      formStep.extraFormFields.push({
        key: TRANSPORT_PORT_FIELD_KEY,
        label: this.translate.instant(transport.portLabel),
        type: FormFieldType.INTEGER,
        defaultValue: isValidPort(resolvedPort) ? resolvedPort : '',
        required: true,
        readOnly,
        // Shown on the transportPort error of transportPortValidator
        errorMessage: this.translate.instant(transport.portError),
        group,
        row: TRANSPORT_HOST_FIELD_KEY,
        width: 200
      });
    }
    this.hostFieldProtocol = protocol;
  }

  private usesVariable(ws: WizardStep, key: string): boolean {
    const placeholder = `\${${key}}`;
    return ws.rawSteps.some(step =>
      [step.file, step.template, step.serverAttributes, step.sharedAttributes, step.credentials, step.dockerCompose]
        .some(file => file && this.zipFiles.get(file)?.includes(placeholder)));
  }

  private initFormStep(ws: WizardStep): void {
    const formJson = (ws.rawSteps[0] && this.zipFiles.get(ws.rawSteps[0].file)) || '[]';
    // These are for the fields the dialog adds; a package can't set them
    const packageFields = (JSON.parse(formJson) as FormFieldDefinition[])
      .map(({ helpPopupId, helpPopupText, errorMessage, ...field }) => field);
    ws.formFields = [...packageFields, ...(ws.extraFormFields ?? [])];
    const controls: Record<string, UntypedFormControl> = {};
    // In review mode, use stored values; an update starts from the values of the installed version,
    // except the transport fields, which take the current Device connectivity settings; otherwise use defaults
    let storedValues: Record<string, any> | undefined;
    if (this.reviewMode) {
      storedValues = this.data.installState?.[ws.stateKey ?? ws.label]?.formValues;
    } else if (this.data.update && ws.stateKey !== TRANSPORT_STEP_STATE_KEY && ws.rawSteps[0]) {
      // Also when the new version renamed the step
      storedValues = this.storedStateOf(ws.rawSteps[0])?.state.formValues;
    }
    for (const field of ws.formFields) {
      this.fieldTypes.set(field.key, field.type);
      const validators = [];
      if (field.required) {
        validators.push(Validators.required);
      }
      if (field.validators?.length > 0) {
        const pattern = Validators.pattern(field.validators[0].pattern);
        validators.push((control: AbstractControl) =>
          pattern({ value: cleanFormFieldValue(field, control.value) } as AbstractControl));
      }
      if (field.key === TRANSPORT_HOST_FIELD_KEY) {
        validators.push(transportHostValidator);
      } else if (field.key === TRANSPORT_PORT_FIELD_KEY) {
        validators.push(transportPortValidator);
      }
      const initialValue = this.resolveInitialFieldValue(field, storedValues?.[field.key]);
      controls[field.key] = new UntypedFormControl({ value: initialValue, disabled: !!field.readOnly }, validators);
    }
    ws.formGroup = new UntypedFormGroup(controls);
    if (this.reviewMode) {
      ws.formGroup.disable();
      ws.completed = true;
    } else {
      this.applyDefaultValueTemplates(ws, storedValues);
    }
  }

  /**
   * Keeps each field with a defaultValueTemplate filled from the other fields until the user edits it.
   * A template with an empty field gives an empty value, so the placeholder shows instead.
   */
  private applyDefaultValueTemplates(ws: WizardStep, storedValues: Record<string, any> | undefined): void {
    const templated = ws.formFields.filter(field => field.defaultValueTemplate && storedValues?.[field.key] === undefined);
    if (!templated.length) {
      return;
    }
    const update = () => {
      for (const field of templated) {
        const control = ws.formGroup.controls[field.key];
        if (!control.dirty) {
          control.setValue(this.renderFormTemplate(field.defaultValueTemplate, this.cleanedFormValues(ws)), { emitEvent: false });
        }
      }
    };
    update();
    this.formTemplateSubscriptions.push(ws.formGroup.valueChanges.subscribe(update));
  }

  /** Applies each field's cleanupPattern to its control, so the stored values are the cleaned ones. */
  private cleanFormStep(ws: WizardStep): void {
    for (const field of ws.formFields) {
      const control = ws.formGroup.controls[field.key];
      const port = pastedPortOf(field, control?.value);
      const portControl = field.portField ? ws.formGroup.controls[field.portField] : undefined;
      if (port !== undefined && portControl?.enabled) {
        portControl.setValue(port, { emitEvent: false });
      }
      const cleaned = cleanFormFieldValue(field, control?.value);
      if (control?.enabled && cleaned !== control.value) {
        control.setValue(cleaned, { emitEvent: false });
      }
    }
  }

  private cleanedFormValues(ws: WizardStep): Record<string, any> {
    const values = ws.formGroup.getRawValue();
    for (const field of ws.formFields) {
      const port = pastedPortOf(field, values[field.key]);
      if (port !== undefined && field.portField in values) {
        values[field.portField] = port;
      }
      values[field.key] = cleanFormFieldValue(field, values[field.key]);
    }
    return values;
  }

  private renderFormTemplate(template: string, values: Record<string, any>): string {
    let missing = false;
    const result = template.replace(/\$\{([^}]+)}/g, (_, key: string) => {
      const value = values[key];
      if (value === undefined || value === null || value === '') {
        missing = true;
        return '';
      }
      return String(value);
    });
    return missing ? '' : result;
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

  private onStepActivated(step = this.currentWizardStep): void {
    if (!step) {
      return;
    }
    if (step.type === 'instruction') {
      step.markdown = this.zipFiles.get(step.rawSteps[0].file) || '';
      // Nothing to validate here, so the linear stepper header may move past it right away.
      step.completed = true;
    } else if (step.type === 'progress' && !step.progressDone) {
      if (this.reviewMode) {
        this.showCompletedEntitySteps(step);
      } else if (this.data.update) {
        if (!step.entitySteps) {
          void this.checkUpdate(step);
        }
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
        ep.errorMessage = this.failureMessage(err);
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
      this.installRegistered = true;
    } catch (_e) {
      // Non-critical — entities are created, tracking registration failed
      console.error('Failed to register device install', _e);
    }

    // Auto-advance to next step after a short pause
    if (!this.isLastWizardStep) {
      await this.delay(500);
      this.stepper.next();
      this.cdr.detectChanges();
    }
  }

  private async createEntity(step: DeviceInstallStep): Promise<EntityStepOutput> {
    const raw = this.zipFiles.get(step.template);
    if (!raw) {
      throw new Error(`Template file not found: ${step.template}`);
    }
    if (step.type === InstallStepType.INTEGRATION) {
      this.pendingRoutingKey = this.generateUuid();
    }
    try {
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
        if (this.data.customerId) {
          template.customerId = new CustomerId(this.data.customerId);
        }
        const entityGroupIds = this.data.entityGroupId ? [this.data.entityGroupId] : null;
        const result = await firstValueFrom(this.deviceService.saveDevice(template, entityGroupIds, {ignoreErrors: false}));
        const creds = await this.resolveCredentials(step, result.id.id);
        return { id: result.id.id, name: result.name, url: `/entities/devices/all/${result.id.id}`, token: creds.credentialsId };
      }
      case InstallStepType.GATEWAY: {
        if (this.data.customerId) {
          template.customerId = new CustomerId(this.data.customerId);
        }
        const entityGroupIds = this.data.entityGroupId ? [this.data.entityGroupId] : null;
        const result = await firstValueFrom(this.deviceService.saveDevice(template, entityGroupIds, {ignoreErrors: false}));
        const creds = await this.resolveCredentials(step, result.id.id);
        const output = {
          id: result.id.id,
          name: result.name,
          url: '/entities/gateways',
          token: creds.credentialsId,
          dockerComposeUrl: `/api/device-connectivity/gateway-launch/${result.id.id}/docker-compose/download`
        };
        this.setGatewayOutput(step, output);
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
        if (this.data.customerId) {
          template.customerId = new CustomerId(this.data.customerId);
        }
        const result = await firstValueFrom(this.dashboardService.saveDashboard(template, null, {ignoreErrors: false}));
        return { id: result.id.id, name: result.title, url: `/dashboards/all/${result.id.id}` };
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
      case InstallStepType.UPLINK_CONVERTER: {
        const converter: Converter = { ...template, type: ConverterType.UPLINK } as Converter;
        const result = await firstValueFrom(this.converterService.saveConverter(converter, {ignoreErrors: true}));
        return { id: result.id.id, name: result.name, url: `/converters/${result.id.id}` };
      }
      case InstallStepType.DOWNLINK_CONVERTER: {
        const converter: Converter = { ...template, type: ConverterType.DOWNLINK } as Converter;
        const result = await firstValueFrom(this.converterService.saveConverter(converter, {ignoreErrors: true}));
        return { id: result.id.id, name: result.name, url: `/converters/${result.id.id}` };
      }
      case InstallStepType.INTEGRATION: {
        // ${formKey} / ${alias.prop} / ${tb.baseUrl} / ${routingKey} substituted by
        // resolveVariables() above. The secret picker may have inserted
        // ${secret:NAME;type:TYPE} references — PE resolves those at runtime via
        // SecretConfigurationService.
        const body: any = template;

        this.attachConverterReferences(body);

        if (!body.routingKey) body.routingKey = this.pendingRoutingKey;
        if (!body.configuration) body.configuration = {};
        if (body.configuration.metadata == null) body.configuration.metadata = {};

        const result = await firstValueFrom(
          this.integrationService.saveIntegration(body, { ignoreErrors: true })
        );
        return this.buildIntegrationOutput(result);
      }
      default:
        throw new Error(`Unsupported entity step type: ${step.type}`);
    }
    } finally {
      this.pendingRoutingKey = undefined;
    }
  }

  private buildIntegrationOutput(result: any): EntityStepOutput {
    // httpEndpoint / baseUrl live at configuration.* for HTTP/KPN/Sigfox/ThingPark
    // and at configuration.clientConfiguration.* for ChirpStack/Particle/Coap/etc.
    const cfg: any = result.configuration ?? {};
    const clientCfg: any = cfg.clientConfiguration ?? {};
    return {
      id: result.id.id,
      name: result.name,
      url: `/integrations/${result.id.id}`,
      routingKey: result.routingKey,
      httpEndpoint: clientCfg.httpEndpoint ?? cfg.httpEndpoint,
      baseUrl: clientCfg.baseUrl ?? cfg.baseUrl
    };
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

  /** Converters are unique by (tenant, name, type). Match both. */
  private async findConverterByNameAndType(name: string, type: ConverterType): Promise<EntityStepOutput | null> {
    const page = await firstValueFrom(this.converterService.getConverters(new PageLink(100, 0, name), {ignoreErrors: true}));
    const match = page.data.find(c => c.name === name && c.type === type);
    return match ? { id: match.id.id, name: match.name, url: `/converters/${match.id.id}` } : null;
  }

  /** Wire converter references from earlier UPLINK_CONVERTER / DOWNLINK_CONVERTER steps onto the
   *  Integration body. Mutates `body` in place. Does not override values the template already
   *  baked (creators may reference pre-existing converters by id directly in the template). */
  private attachConverterReferences(body: any): void {
    const uplink = this.entityOutputs.get('uplinkConverter');
    if (uplink && body.defaultConverterId == null) {
      body.defaultConverterId = { id: uplink.id, entityType: 'CONVERTER' };
    }
    const downlink = this.entityOutputs.get('downlinkConverter');
    if (downlink && body.downlinkConverterId == null) {
      body.downlinkConverterId = { id: downlink.id, entityType: 'CONVERTER' };
    }
  }

  /** Integrations are unique by name within tenant. */
  private async findIntegrationByName(name: string): Promise<EntityStepOutput | null> {
    const page = await firstValueFrom(this.integrationService.getIntegrations(new PageLink(100, 0, name), {ignoreErrors: true}));
    const match = page.data.find(i => i.name === name);
    return match ? { id: match.id.id, name: match.name, url: `/integrations/${match.id.id}`, routingKey: match.routingKey } : null;
  }

  private async findDeviceByName(name: string): Promise<EntityStepOutput | null> {
    try {
      const device = await firstValueFrom(this.deviceService.findByName(name, {ignoreErrors: true}));
      if (device) {
        return { id: device.id.id, name: device.name, url: `/entities/devices/all/${device.id.id}` };
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
    // Overwriting an existing integration reuses its routingKey, so resolve
    // ${routingKey} placeholders against that value rather than allocating a new one.
    if (step.type === InstallStepType.INTEGRATION) {
      this.pendingRoutingKey = existing.routingKey ?? this.generateUuid();
    }
    try {
    const template = this.resolveTemplateJson(raw);
    // A version exported with the template (e.g. "version": 1) would be rejected as stale by the entity it overwrites
    delete template.version;
    delete template.ruleChain?.version;
    switch (step.type) {
      case InstallStepType.DEVICE_PROFILE: {
        template.id = { id: existing.id, entityType: 'DEVICE_PROFILE' };
        const result = await firstValueFrom(this.deviceProfileService.saveDeviceProfile(template, {ignoreErrors: true}));
        return { id: result.id.id, name: result.name, url: `/profiles/deviceProfiles/${result.id.id}` };
      }
      case InstallStepType.DEVICE: {
        template.id = { id: existing.id, entityType: 'DEVICE' };
        if (this.data.customerId) {
          template.customerId = new CustomerId(this.data.customerId);
        }
        const entityGroupIds = this.data.entityGroupId ? [this.data.entityGroupId] : null;
        const result = await firstValueFrom(this.deviceService.saveDevice(template, entityGroupIds, {ignoreErrors: false}));
        const creds = await this.resolveCredentials(step, result.id.id);
        return { id: result.id.id, name: result.name, url: `/entities/devices/all/${result.id.id}`, token: creds.credentialsId };
      }
      case InstallStepType.GATEWAY: {
        template.id = { id: existing.id, entityType: 'DEVICE' };
        if (this.data.customerId) {
          template.customerId = new CustomerId(this.data.customerId);
        }
        const entityGroupIds = this.data.entityGroupId ? [this.data.entityGroupId] : null;
        const result = await firstValueFrom(this.deviceService.saveDevice(template, entityGroupIds, {ignoreErrors: false}));
        const creds = await this.resolveCredentials(step, result.id.id);
        const output: EntityStepOutput = {
          id: result.id.id,
          name: result.name,
          url: '/entities/gateways',
          token: creds.credentialsId,
          dockerComposeUrl: `/api/device-connectivity/gateway-launch/${result.id.id}/docker-compose/download`
        };
        this.setGatewayOutput(step, output);
        return output;
      }
      case InstallStepType.DASHBOARD: {
        template.id = { id: existing.id, entityType: 'DASHBOARD' };
        if (this.data.customerId) {
          template.customerId = new CustomerId(this.data.customerId);
        }
        const result = await firstValueFrom(this.dashboardService.saveDashboard(template, null, {ignoreErrors: false}));
        return { id: result.id.id, name: result.title, url: `/dashboards/all/${result.id.id}` };
      }
      case InstallStepType.RULE_CHAIN: {
        const ruleChain = template.ruleChain || template;
        const metadata = template.metadata;
        ruleChain.id = { id: existing.id, entityType: 'RULE_CHAIN' };
        return this.saveRuleChainWithMetadata(ruleChain, metadata);
      }
      case InstallStepType.UPLINK_CONVERTER: {
        const converter: Converter = { ...template, type: ConverterType.UPLINK, id: { id: existing.id, entityType: 'CONVERTER' } } as Converter;
        const result = await firstValueFrom(this.converterService.saveConverter(converter, {ignoreErrors: true}));
        return { id: result.id.id, name: result.name, url: `/converters/${result.id.id}` };
      }
      case InstallStepType.DOWNLINK_CONVERTER: {
        const converter: Converter = { ...template, type: ConverterType.DOWNLINK, id: { id: existing.id, entityType: 'CONVERTER' } } as Converter;
        const result = await firstValueFrom(this.converterService.saveConverter(converter, {ignoreErrors: true}));
        return { id: result.id.id, name: result.name, url: `/converters/${result.id.id}` };
      }
      case InstallStepType.INTEGRATION: {
        const body: any = template;

        this.attachConverterReferences(body);

        body.id = { id: existing.id, entityType: 'INTEGRATION' };
        body.routingKey = this.pendingRoutingKey;
        if (!body.configuration) body.configuration = {};
        if (body.configuration.metadata == null) body.configuration.metadata = {};

        const result = await firstValueFrom(
          this.integrationService.saveIntegration(body, { ignoreErrors: true })
        );
        return this.buildIntegrationOutput(result);
      }
      default:
        throw new Error(`Unsupported overwrite for step type: ${step.type}`);
    }
    } finally {
      this.pendingRoutingKey = undefined;
    }
  }

  /** A device or gateway taken as it is: the token comes from its current credentials. */
  private async useExistingEntity(step: DeviceInstallStep, existing: EntityStepOutput): Promise<EntityStepOutput> {
    if (step.type !== InstallStepType.DEVICE && step.type !== InstallStepType.GATEWAY) {
      return existing;
    }
    const creds = await firstValueFrom(this.deviceService.getDeviceCredentials(existing.id, false, {ignoreErrors: true}));
    const output: EntityStepOutput = { ...existing, token: creds.credentialsId };
    if (step.type === InstallStepType.GATEWAY) {
      output.dockerComposeUrl = `/api/device-connectivity/gateway-launch/${output.id}/docker-compose/download`;
      this.setGatewayOutput(step, output);
    }
    return output;
  }

  /** Stores the gateway output early, so the custom docker-compose template, if any, resolves ${gateway.token} etc. */
  private setGatewayOutput(step: DeviceInstallStep, output: EntityStepOutput): void {
    this.entityOutputs.set('gateway', output);
    if (step.dockerCompose) {
      const raw = this.zipFiles.get(step.dockerCompose);
      if (raw) {
        this.gatewayDockerComposeContent = this.resolveVariables(raw);
      }
    }
  }

  /**
   * Update: the entity the installed version created for this step. It is found by the id kept in
   * the install state, not by name, so an entity the user renamed is still found. Null when this
   * is not an update, when the installed version had no such step, or when the entity was deleted since.
   * Any other failure to load it is thrown: creating the entity again would duplicate it.
   */
  private async findPreviousEntity(step: DeviceInstallStep): Promise<PreviousEntity | null> {
    const installState = this.data.update?.descriptor.installState;
    // A connector lives in the gateway's attributes: its step runs again over the same gateway
    if (!installState || step.type === InstallStepType.GATEWAY_CONNECTOR) {
      return null;
    }
    const stored = this.storedStateOf(step);
    const key = stored?.key;
    const id: string = stored?.state.entityOutput?.id;
    if (!id) {
      return null;
    }
    try {
      const output = await this.loadPreviousEntity(step, id);
      if (!output) {
        return null;
      }
      return {
        output,
        useExisting: stored.state.resolution === 'use-existing',
        shared: !!this.updateStates.get(key)?.shared,
        resolution: stored.state.resolution || 'created',
        stateKey: key,
        state: this.updateStates.get(key)
      };
    } catch (e: any) {
      if (e?.status === 404) {
        // Deleted since: the update creates it again
        return null;
      }
      throw e;
    }
  }

  private async loadPreviousEntity(step: DeviceInstallStep, id: string): Promise<EntityStepOutput | null> {
    const config = {ignoreErrors: true};
    switch (step.type) {
      case InstallStepType.DEVICE_PROFILE: {
        const profile = await firstValueFrom(this.deviceProfileService.getDeviceProfileInfo(id, config));
        return { id, name: profile.name, url: `/profiles/deviceProfiles/${id}` };
      }
      case InstallStepType.DEVICE: {
        const device = await firstValueFrom(this.deviceService.getDevice(id, config));
        return { id, name: device.name, url: `/entities/devices/all/${id}` };
      }
      case InstallStepType.GATEWAY: {
        const gateway = await firstValueFrom(this.deviceService.getDevice(id, config));
        return { id, name: gateway.name, url: '/entities/gateways' };
      }
      case InstallStepType.DASHBOARD: {
        const dashboard = await firstValueFrom(this.dashboardService.getDashboardInfo(id, config));
        return { id, name: dashboard.title, url: `/dashboards/all/${id}` };
      }
      case InstallStepType.RULE_CHAIN: {
        const ruleChain = await firstValueFrom(this.ruleChainService.getRuleChain(id, config));
        return { id, name: ruleChain.name, url: `/ruleChains/${id}` };
      }
      case InstallStepType.UPLINK_CONVERTER:
      case InstallStepType.DOWNLINK_CONVERTER: {
        const converter = await firstValueFrom(this.converterService.getConverter(id, config));
        const type = step.type === InstallStepType.UPLINK_CONVERTER ? ConverterType.UPLINK : ConverterType.DOWNLINK;
        return converter.type === type ? { id, name: converter.name, url: `/converters/${id}` } : null;
      }
      case InstallStepType.INTEGRATION: {
        // With httpEndpoint and baseUrl, which the instructions resolve for an integration kept as it is
        return this.buildIntegrationOutput(await firstValueFrom(this.integrationService.getIntegration(id, config)));
      }
      default:
        return null;
    }
  }

  /**
   * What the user sees for a failed request: ThingsBoard's validation and limit messages as they are,
   * anything else (a server error, the network, a bug) as a common message, with the details in the console.
   */
  private failureMessage(error: any): string {
    const body = error?.error;
    const message = SHOWN_ERROR_CODES.has(body?.errorCode) ? readableMessage(body.message) : null;
    if (message) {
      return message;
    }
    console.error('Device package step failed', error);
    return this.translate.instant('iot-hub.device-install-unexpected-error');
  }

  // --- Update ---

  /** Update: what the update does with the entity of a step, once the Update step has checked it. */
  updatePlanOf(ep: EntityStepProgress): EntityStepPlan | undefined {
    return this.entityPlans.get(ep);
  }

  /** Update: the warnings and name conflicts the user hasn't confirmed yet. */
  unconfirmedUpdateWarnings(ws: WizardStep): number {
    return (ws.entitySteps ?? []).filter(ep => this.entityPlans.get(ep)?.confirmed === false).length;
  }

  /** Update: true when the error is about the whole update, e.g. its check or the registration of the new version. */
  hasUpdateError(ws: WizardStep): boolean {
    return !!ws.progressError && !ws.entitySteps?.some(ep => ep.status === 'error');
  }

  /** Update: resolution is the user's choice for an entity whose name is taken, as at install. */
  confirmUpdateWarning(ep: EntityStepProgress, resolution?: string): void {
    const plan = this.entityPlans.get(ep);
    if (plan) {
      plan.resolution = resolution;
      plan.confirmed = true;
    }
  }

  /** A translated text with a link written as <a>…</a>, which the translation may put anywhere in it, split around the link. */
  textWithLink(key: string): { before: string; link: string; after: string } {
    const text: string = this.translate.instant(key);
    const match = /^([\s\S]*)<a>([\s\S]*)<\/a>([\s\S]*)$/.exec(text);
    return match ? { before: match[1], link: match[2], after: match[3] } : { before: text, link: '', after: '' };
  }

  /** Update: saves the entity the user changed, as it is before the update overwrites it, to a JSON file. */
  downloadCurrentEntity(ep: EntityStepProgress): void {
    const current = this.entityPlans.get(ep)?.current;
    if (!current) {
      return;
    }
    const blob = new Blob([JSON.stringify(current, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${(ep.resolvedName || ep.step.name).replace(/[^\w\- ]+/g, '_')}.json`;
    link.click();
    // Revoked right away, the URL may be gone before the download starts
    setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  /**
   * Update: nothing was changed, so trying again starts from the check: the entities may have changed meanwhile
   * (e.g. someone saved one, which is why it failed), and the user confirms what the update overwrites again.
   */
  retryUpdate(ws: WizardStep): void {
    void this.checkUpdate(ws);
  }

  /**
   * Update: sends what the check planned to the server, which applies all of it in one transaction together with
   * the new version of the installed item, or nothing. The rows run together: the request is one.
   */
  async runUpdate(ws: WizardStep): Promise<void> {
    ws.reviewing = false;
    ws.updating = true;
    ws.progressError = null;
    for (const ep of ws.entitySteps) {
      ep.status = 'running';
      ep.errorMessage = null;
    }
    this.cdr.detectChanges();
    let result: DeviceUpdateResult;
    try {
      result = await firstValueFrom(this.iotHubApiService.updateDevicePackage(this.data.update.installedItemId, {
        versionId: this.data.item.id as string,
        fromVersionId: this.data.update.fromVersionId,
        selectedInstallMethod: this.selectedInstallMethod,
        installState: this.buildFormState(),
        steps: this.buildUpdateSteps(ws)
      }, { ignoreLoading: true, ignoreErrors: true }));
    } catch (e: any) {
      result = { success: false, errorMessage: this.failureMessage(e) };
    }
    ws.updating = false;
    if (!result.success) {
      // Nothing was changed: the transaction was rolled back
      const message = readableMessage(result.errorMessage) ?? this.translate.instant('iot-hub.device-install-unexpected-error');
      for (const ep of ws.entitySteps) {
        const failed = ep.step.name === result.failedStep;
        ep.status = failed ? 'error' : 'skipped';
        ep.errorMessage = failed ? message : null;
      }
      ws.progressError = message;
      // The steps refer to the installed entities again
      this.restoreInstallState(this.data.update.descriptor.installState ?? {}, false);
      this.cdr.detectChanges();
      return;
    }
    for (const ep of ws.entitySteps) {
      const output = result.outputs?.[ep.step.name];
      const plan = this.entityPlans.get(ep);
      const resolution = plan?.previous?.resolution ?? plan?.resolution ?? 'created';
      if (output) {
        const entityOutput = this.withEntityUrl(ep.step, output);
        if (ep.step.type === InstallStepType.GATEWAY) {
          this.setGatewayOutput(ep.step, entityOutput);
        }
        this.markEntityStepSuccess(ws, ep, entityOutput, resolution);
      } else {
        ep.status = 'success';
      }
    }
    this.installRegistered = true;
    ws.progressDone = true;
    ws.completed = true;
    this.cdr.detectChanges();
  }

  /** Update: the values of the forms, which the install state keeps for the next update. */
  private buildFormState(): Record<string, any> {
    const state: Record<string, any> = {};
    for (const ws of this.wizardSteps) {
      if (ws.type === 'form' && ws.formGroup) {
        state[ws.stateKey ?? ws.label] = { formValues: ws.formGroup.getRawValue() };
      }
    }
    return state;
  }

  /** Update: the steps the server applies, each with its entity resolved from the template of the new version. */
  private buildUpdateSteps(ws: WizardStep): DeviceUpdateStep[] {
    for (const ep of ws.entitySteps) {
      const plan = this.entityPlans.get(ep);
      const alias = stepTypeAliasMap[ep.step.type];
      if (plan.action !== 'create') {
        continue;
      }
      if (plan.resolution === 'use-existing' || plan.resolution === 'overwrite') {
        // Not the package's entity deleted since, which the install state still names
        this.entityOutputs.set(alias, plan.existing);
      } else {
        // The server fills in the ids of the entities it creates: their placeholders stay for it
        this.entityOutputs.delete(alias);
      }
    }
    return ws.entitySteps.map(ep => this.buildUpdateStep(ws, ep.step, this.entityPlans.get(ep)));
  }

  private buildUpdateStep(ws: WizardStep, step: DeviceInstallStep, plan: EntityStepPlan): DeviceUpdateStep {
    const base: DeviceUpdateStep = {
      name: step.name,
      type: step.type,
      action: 'KEEP',
      resolution: plan.previous?.resolution ?? plan.resolution ?? 'created'
    };
    if (step.type === InstallStepType.GATEWAY_CONNECTOR) {
      return plan.action === 'keep' ? base : { ...base, action: 'OVERWRITE', entity: this.resolveTemplateJson(this.zipFiles.get(step.template)) };
    }
    if (plan.action === 'keep') {
      // A renamed step: the server takes the kept entity's checksum from the installed step's entry
      const previousStepName = plan.previous.stateKey !== step.name ? plan.previous.stateKey : undefined;
      return { ...base, entityId: plan.previous.output.id, previousStepName };
    }
    if (plan.resolution === 'use-existing') {
      // The package's own entity, under a step that wasn't matched to its installed one, stays the package's
      const resolution = this.isPackageEntity(plan.existing.id) ? 'created' : base.resolution;
      return { ...base, resolution, entityId: plan.existing.id, confirmed: true };
    }
    const overwritten = plan.action === 'overwrite' ? plan.previous.output : plan.resolution === 'overwrite' ? plan.existing : null;
    const entity = this.buildEntityBody(ws, step, overwritten);
    // As at install, the entity gets the step's attributes
    const attributes = {
      serverAttributes: this.resolvePackageFile(this.zipFiles, step.serverAttributes, this.formValues),
      sharedAttributes: this.resolvePackageFile(this.zipFiles, step.sharedAttributes, this.formValues)
    };
    if (overwritten) {
      return { ...base, action: 'OVERWRITE', entityId: overwritten.id, expectedVersion: plan.version, confirmed: true, entity, ...attributes };
    }
    return {
      ...base,
      action: 'CREATE',
      entity,
      credentials: this.resolvePackageFile(this.zipFiles, step.credentials, this.formValues),
      ...attributes
    };
  }

  /**
   * Update: the entity of a step resolved from the template of the new version, as the server saves it. The server
   * keeps what the entity it overwrites must keep: its id, name, owner and an integration's routing key.
   */
  private buildEntityBody(ws: WizardStep, step: DeviceInstallStep, overwritten: EntityStepOutput | null): any {
    const raw = this.zipFiles.get(step.template);
    if (!raw) {
      throw new Error(`Template file not found: ${step.template}`);
    }
    if (step.type === InstallStepType.INTEGRATION) {
      // ${routingKey} in the configuration, e.g. in an httpEndpoint
      this.pendingRoutingKey = overwritten?.routingKey ?? this.generateUuid();
    }
    try {
      const entity = this.resolveTemplateJson(raw);
      delete entity.id;
      delete entity.version;
      delete entity.ruleChain?.version;
      if (step.type === InstallStepType.INTEGRATION) {
        this.attachConverterReferences(entity);
        // A converter this update creates gets its id on the server
        const steps = ws.rawSteps.map(s => s.type);
        if (entity.defaultConverterId == null && steps.includes(InstallStepType.UPLINK_CONVERTER)) {
          entity.defaultConverterId = { entityType: 'CONVERTER', id: '${uplinkConverter.id}' };
        }
        if (entity.downlinkConverterId == null && steps.includes(InstallStepType.DOWNLINK_CONVERTER)) {
          entity.downlinkConverterId = { entityType: 'CONVERTER', id: '${downlinkConverter.id}' };
        }
        if (!entity.routingKey) {
          entity.routingKey = this.pendingRoutingKey;
        }
        if (!entity.configuration) {
          entity.configuration = {};
        }
        if (entity.configuration.metadata == null) {
          entity.configuration.metadata = {};
        }
      }
      return entity;
    } finally {
      this.pendingRoutingKey = undefined;
    }
  }

  /** Update: a step output from the server, with the URL the wizard opens it at. */
  private withEntityUrl(step: DeviceInstallStep, output: EntityStepOutput): EntityStepOutput {
    const urls: Partial<Record<InstallStepType, string>> = {
      [InstallStepType.DEVICE_PROFILE]: `/profiles/deviceProfiles/${output.id}`,
      [InstallStepType.DEVICE]: `/entities/devices/all/${output.id}`,
      [InstallStepType.GATEWAY]: '/entities/gateways',
      [InstallStepType.DASHBOARD]: `/dashboards/all/${output.id}`,
      [InstallStepType.RULE_CHAIN]: `/ruleChains/${output.id}`,
      [InstallStepType.UPLINK_CONVERTER]: `/converters/${output.id}`,
      [InstallStepType.DOWNLINK_CONVERTER]: `/converters/${output.id}`,
      [InstallStepType.INTEGRATION]: `/integrations/${output.id}`
    };
    return { ...output, url: urls[step.type] };
  }

  private async loadPackageFiles(versionId: string, files: Map<string, string>, images?: Map<string, string>): Promise<void> {
    const zipData = await firstValueFrom(this.iotHubApiService.getVersionFileData(versionId, { ignoreLoading: true }).pipe(
      mergeMap((blob: Blob) => blob.arrayBuffer())
    ));
    const JSZip = (await import('jszip')).default;
    const zip = await JSZip.loadAsync(zipData);
    const imageExtensions = new Set(['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp']);
    for (const [path, entry] of Object.entries(zip.files) as [string, any][]) {
      if (!entry.dir) {
        const ext = path.split('.').pop()?.toLowerCase();
        if (imageExtensions.has(ext)) {
          // Not loaded for the installed version, which is only compared
          if (images) {
            const blob = await entry.async('blob');
            images.set(path, URL.createObjectURL(blob));
          }
        } else {
          files.set(path, await entry.async('string'));
        }
      }
    }
  }

  /** Update: loads the installed version. Without it, every form and entity of the new version counts as changed. */
  private async loadPreviousVersion(): Promise<void> {
    try {
      await this.loadPackageFiles(this.data.update.fromVersionId, this.previousFiles);
      this.previousPackageInfo = JSON.parse(this.previousFiles.get('device-info.json'));
    } catch (e) {
      console.error('Failed to load the installed version of the device package', e);
    }
  }

  /**
   * Update: true when the new version changes no form and no entity, only e.g. the instructions, the readme
   * or the images. The installed item then takes the new version without the wizard.
   */
  private isTextOnlyUpdate(): boolean {
    if (!this.previousPackageInfo) {
      return false;
    }
    this.restoreInstallState(this.data.update.descriptor.installState ?? {});
    try {
      return (this.packageInfo.installSteps[this.selectedInstallMethod] ?? []).every(step => {
        if (step.type === InstallStepType.SHOW_FORM) {
          return !this.formStepChanged(step);
        }
        // The device and the gateway never change on update
        if (step.type === InstallStepType.DEVICE || step.type === InstallStepType.GATEWAY || !ENTITY_STEP_TYPES.has(step.type)) {
          return true;
        }
        return !this.entityStepChanged(step);
      });
    } finally {
      this.formValues = {};
      this.entityOutputs.clear();
    }
  }

  /**
   * Update: records the new version with every entity kept as it is. When the server can't (e.g. an entity
   * was deleted since), the wizard opens instead, so the user sees what the update does.
   */
  private async registerTextOnlyUpdate(): Promise<boolean> {
    const installState = this.data.update.descriptor.installState ?? {};
    const steps: DeviceUpdateStep[] = [];
    for (const step of this.packageInfo.installSteps[this.selectedInstallMethod] ?? []) {
      if (!ENTITY_STEP_TYPES.has(step.type)) {
        continue;
      }
      const stored = this.storedStateOf(step)?.state;
      if (step.type !== InstallStepType.GATEWAY_CONNECTOR && !stored?.entityOutput?.id) {
        return false;
      }
      steps.push({ name: step.name, type: step.type, action: 'KEEP', entityId: stored?.entityOutput?.id,
        resolution: stored?.resolution ?? 'created', confirmed: true });
    }
    // Under the new version's names, as the wizard would store them
    const formState: Record<string, any> = {};
    if (installState[TRANSPORT_STEP_STATE_KEY]?.formValues) {
      formState[TRANSPORT_STEP_STATE_KEY] = { formValues: installState[TRANSPORT_STEP_STATE_KEY].formValues };
    }
    for (const step of this.packageInfo.installSteps[this.selectedInstallMethod] ?? []) {
      const formValues = step.type === InstallStepType.SHOW_FORM ? this.storedStateOf(step)?.state.formValues : undefined;
      if (formValues) {
        formState[step.name] = { formValues };
      }
    }
    try {
      const result = await firstValueFrom(this.iotHubApiService.updateDevicePackage(this.data.update.installedItemId,
        { versionId: this.data.item.id as string, fromVersionId: this.data.update.fromVersionId,
          selectedInstallMethod: this.selectedInstallMethod, installState: formState, steps },
        { ignoreLoading: true, ignoreErrors: true }));
      if (result.success) {
        this.dialogRef.close('installed');
        return true;
      }
      console.warn('Text-only update was rejected, opening the wizard', result.errorMessage);
    } catch (e) {
      console.warn('Text-only update failed, opening the wizard', e);
    }
    return false;
  }

  /**
   * Update: the user doesn't go through the steps again. The update opens Configuration when a form changed,
   * since there may be something new to fill in, and otherwise the Update step. The steps before it count as done.
   */
  private goToUpdateStart(): void {
    const changedForm = this.wizardSteps.findIndex(ws => ws.type === 'form' && ws.rawSteps[0] && this.formStepChanged(ws.rawSteps[0]));
    const update = this.wizardSteps.findIndex(ws => ws.type === 'progress');
    // Never past the Update step: a form after it doesn't feed the entities
    const start = changedForm >= 0 && (update < 0 || changedForm < update) ? changedForm : update;
    if (start > 0) {
      for (const ws of this.wizardSteps.slice(0, start)) {
        if (ws.type === 'instruction') {
          // Back shows it without activating it
          ws.markdown = this.zipFiles.get(ws.rawSteps[0].file) || '';
        }
        ws.completed = true;
      }
      this.cdr.detectChanges();
      // Emits selectionChange, which commits the forms passed over and activates the step
      this.stepper.selectedIndex = start;
    }
    if (start <= 0 || this.stepper.selectedIndex !== start) {
      // Already on it, or the stepper refused to move there
      this.onStepActivated();
    }
  }

  /**
   * Update: the step of the installed version that a step of the new one replaces: same type and name, or, when the
   * new version renamed it, the only installed step of its type that no step of the new version has the name of.
   */
  private findPreviousStep(step: DeviceInstallStep): DeviceInstallStep | undefined {
    const previousSteps = (this.previousPackageInfo?.installSteps[this.selectedInstallMethod] ?? []).filter(s => s.type === step.type);
    const steps = (this.packageInfo.installSteps[this.selectedInstallMethod] ?? []).filter(s => s.type === step.type);
    const sameName = previousSteps.find(s => s.name === step.name);
    if (sameName) {
      return sameName;
    }
    const unmatchedPrevious = previousSteps.filter(p => !steps.some(s => s.name === p.name));
    const unmatched = steps.filter(s => !previousSteps.some(p => p.name === s.name));
    return unmatchedPrevious.length === 1 && unmatched.length === 1 ? unmatchedPrevious[0] : undefined;
  }

  /** Update: true when the entity is one the installed package created. */
  private isPackageEntity(id: string): boolean {
    return !!this.data.update?.descriptor.createdEntityIds?.some(created => created.id === id);
  }

  /** Update: the install state of a step of the new version, under its name or under the installed step it replaces. */
  private storedStateOf(step: DeviceInstallStep): { key: string; state: any } | undefined {
    const installState = this.data.update?.descriptor.installState ?? {};
    const key = [step.name, this.resolveVariables(step.name), this.findPreviousStep(step)?.name].find(name => name && installState[name]);
    return key ? { key, state: installState[key] } : undefined;
  }

  private formStepChanged(step: DeviceInstallStep): boolean {
    const previousStep = this.findPreviousStep(step);
    return !previousStep || this.zipFiles.get(step.file) !== this.previousFiles.get(previousStep.file);
  }

  /** Update: true when the new version's files give the entity something else than the installed version's gave it. */
  private entityStepChanged(step: DeviceInstallStep): boolean {
    const previousStep = this.findPreviousStep(step);
    if (!previousStep) {
      return true;
    }
    const previousFormValues = this.previousFormValues();
    const files: (keyof DeviceInstallStep)[] = ['template', 'serverAttributes', 'sharedAttributes', 'credentials'];
    return files.some(file => !isEqual(
      this.resolvePackageFile(this.zipFiles, step[file], this.formValues),
      this.resolvePackageFile(this.previousFiles, previousStep[file], previousFormValues)
    ));
  }

  /** Update: the form values the installed version was installed with. */
  private previousFormValues(): Record<string, any> {
    const values: Record<string, any> = {};
    for (const state of Object.values(this.data.update.descriptor.installState ?? {})) {
      Object.assign(values, state?.formValues);
    }
    return values;
  }

  /** A JSON file of a package version, resolved with the given form values; undefined when there is no such file. */
  private resolvePackageFile(files: Map<string, string>, path: string | undefined, formValues: Record<string, any>,
                             routingKey?: string): any {
    const raw = path ? files.get(path) : undefined;
    if (raw === undefined) {
      return undefined;
    }
    const committedValues = this.formValues;
    this.formValues = formValues;
    this.pendingRoutingKey = routingKey;
    try {
      return this.resolveTemplateJson(raw);
    } finally {
      this.formValues = committedValues;
      this.pendingRoutingKey = undefined;
    }
  }

  /** Update: finds out what the update changes and what the user has to confirm, before anything changes. */
  private async checkUpdate(ws: WizardStep): Promise<void> {
    // Templates refer to the package's entities, e.g. ${deviceProfile.id}: until the steps run, the installed ones
    this.restoreInstallState(this.data.update.descriptor.installState ?? {}, false);
    ws.entitySteps = ws.rawSteps.map(s => ({
      step: s,
      status: 'pending' as const,
      resolvedName: this.resolveVariables(s.name)
    }));
    ws.reviewing = true;
    ws.checking = true;
    ws.progressError = null;
    this.cdr.detectChanges();
    try {
      const states = await firstValueFrom(this.iotHubApiService.checkDeviceUpdate(this.data.update.installedItemId, { ignoreLoading: true }));
      this.updateStates = new Map(states.map(state => [state.stepName, state]));
      const plans = await Promise.all(ws.entitySteps.map(ep => this.planEntityStep(ep)));
      ws.entitySteps.forEach((ep, i) => this.entityPlans.set(ep, plans[i]));
      this.overwriteReferencesToCreated(ws);
    } catch (e: any) {
      ws.progressError = this.failureMessage(e);
    }
    ws.checking = false;
    this.cdr.detectChanges();
  }

  private async planEntityStep(ep: EntityStepProgress): Promise<EntityStepPlan> {
    const step = ep.step;
    if (step.type === InstallStepType.GATEWAY_CONNECTOR) {
      // A connector lives in the gateway's attributes, which the step sets again when it changed
      return { action: this.entityStepChanged(step) ? 'overwrite' : 'keep', confirmed: true };
    }
    const previous = await this.findPreviousEntity(step);
    if (previous) {
      // Its current name: the user may have renamed it
      ep.resolvedName = previous.output.name;
      // The device and its credentials, which the firmware uses, never change on update
      if (previous.useExisting || step.type === InstallStepType.DEVICE || step.type === InstallStepType.GATEWAY) {
        return { action: 'keep', previous, confirmed: true };
      }
      // The update brings the entity to the new version even when only someone else changed it
      const state = previous.state;
      const current = await this.loadEntitySnapshot(step, previous.output.id);
      const modified = state?.modified ?? this.differsFromTemplate(step, previous, current);
      const version = state?.version ?? current.version ?? current.ruleChain?.version;
      if (!modified && !this.entityStepChanged(step)) {
        // With the version, in case it refers to an entity the update creates again
        return { action: 'keep', previous, confirmed: true, version };
      }
      return {
        // A shared entity may have been changed by the other package: the shared warning says what matters
        action: 'overwrite', previous, shared: previous.shared, modified: modified && !previous.shared,
        confirmed: !previous.shared && !modified,
        version,
        current: modified && !previous.shared ? current : undefined
      };
    }
    // New in this version, or deleted since: created as at install, with the same name conflict check
    const existing = await this.preCheckEntity(step);
    return { action: 'create', existing, confirmed: !existing };
  }

  /**
   * Update: an entity kept as it is whose files refer to an entity the update creates again, e.g. the dashboard of
   * a device deleted since, is overwritten, so that it refers to the new entity and not to the deleted one.
   */
  private overwriteReferencesToCreated(ws: WizardStep): void {
    const createdAliases = ws.entitySteps
      .filter(ep => this.entityPlans.get(ep)?.action === 'create')
      .map(ep => stepTypeAliasMap[ep.step.type])
      .filter(alias => !!alias);
    if (!createdAliases.length) {
      return;
    }
    for (const ep of ws.entitySteps) {
      const plan = this.entityPlans.get(ep);
      const step = ep.step;
      if (plan?.action !== 'keep' || plan.previous?.useExisting ||
        step.type === InstallStepType.DEVICE || step.type === InstallStepType.GATEWAY) {
        continue;
      }
      const files = [step.template, step.serverAttributes, step.sharedAttributes].map(file => file && this.zipFiles.get(file));
      if (createdAliases.some(alias => files.some(raw => raw?.includes(`\${${alias}.`)))) {
        plan.action = 'overwrite';
        plan.shared = plan.previous?.shared;
        plan.confirmed = !plan.shared;
      }
    }
  }

  /**
   * Update, for packages installed before the server kept checksums: true when the entity differs from what the
   * installed version's template gave it, i.e. someone changed it since. Without that template, e.g. when the
   * installed version failed to load, it counts as changed: the user confirms before it is overwritten.
   */
  private differsFromTemplate(step: DeviceInstallStep, previous: PreviousEntity, current: any): boolean {
    const expected = this.resolvePackageFile(this.previousFiles, this.findPreviousStep(step)?.template,
      this.previousFormValues(), previous.output.routingKey);
    if (!expected) {
      return true;
    }
    switch (step.type) {
      case InstallStepType.RULE_CHAIN:
        return !matchesTemplate(expected.ruleChain || expected, current.ruleChain, RULE_CHAIN_SERVER_SET_KEYS) ||
          (!!expected.metadata && !matchesRuleChainMetadata(expected.metadata, current.metadata));
      case InstallStepType.DASHBOARD: {
        // The server keeps a dashboard's configuration as it is, but for its images and resources:
        // a widget added or resized counts too
        const { configuration, ...dashboard } = expected;
        return !isEqual(withoutImageLinks(configuration ?? null), withoutImageLinks(current.configuration ?? null)) ||
          !matchesTemplate(dashboard, current);
      }
      default:
        return !matchesTemplate(expected, current);
    }
  }

  /** Update: the whole entity as it is now, which the user can download before the update overwrites it. */
  private async loadEntitySnapshot(step: DeviceInstallStep, id: string): Promise<any> {
    const config = {ignoreErrors: true};
    switch (step.type) {
      case InstallStepType.DEVICE_PROFILE:
        return firstValueFrom(this.deviceProfileService.getDeviceProfile(id, config));
      case InstallStepType.DEVICE:
      case InstallStepType.GATEWAY:
        return firstValueFrom(this.deviceService.getDevice(id, config));
      case InstallStepType.DASHBOARD:
        return firstValueFrom(this.dashboardService.getDashboard(id, config));
      case InstallStepType.RULE_CHAIN:
        return {
          ruleChain: await firstValueFrom(this.ruleChainService.getRuleChain(id, config)),
          metadata: await firstValueFrom(this.ruleChainService.getRuleChainMetadata(id, config))
        };
      case InstallStepType.UPLINK_CONVERTER:
      case InstallStepType.DOWNLINK_CONVERTER:
        return firstValueFrom(this.converterService.getConverter(id, config));
      case InstallStepType.INTEGRATION:
        return firstValueFrom(this.integrationService.getIntegration(id, config));
      default:
        throw new Error(`Unsupported update for step type: ${step.type}`);
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
      case InstallStepType.UPLINK_CONVERTER:
        return this.findConverterByNameAndType(template.name, ConverterType.UPLINK);
      case InstallStepType.DOWNLINK_CONVERTER:
        return this.findConverterByNameAndType(template.name, ConverterType.DOWNLINK);
      case InstallStepType.INTEGRATION:
        return this.findIntegrationByName(template.name);
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
        state[ws.stateKey ?? ws.label] = { formValues: ws.formGroup.getRawValue() };
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
      case InstallStepType.UPLINK_CONVERTER: return 'CONVERTER';
      case InstallStepType.DOWNLINK_CONVERTER: return 'CONVERTER';
      case InstallStepType.INTEGRATION: return 'INTEGRATION';
      default: return null;
    }
  }

  private generateUuid(): string {
    const c: any = typeof crypto !== 'undefined' ? crypto : null;
    if (c?.randomUUID) return c.randomUUID() as string;
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, ch => {
      const r = (Math.random() * 16) | 0;
      const v = ch === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
