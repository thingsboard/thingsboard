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
  DevicePackageRequest,
  DevicePackageResult,
  DevicePackageStep
} from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { DeviceService } from '@core/http/device.service';
import { DashboardService } from '@core/http/dashboard.service';
import { RuleChainService } from '@core/http/rule-chain.service';
import { generateSecret, isEqual } from '@core/utils';
import { ConverterService } from '@core/http/converter.service';
import { IntegrationService } from '@core/http/integration.service';
import { DialogService } from '@core/services/dialog.service';
import { Converter, ConverterType } from '@shared/models/converter.models';
import { AiDashboardGenerationService } from '@home/components/ai/ai-dashboard-generation.service';
import { ResourcesService } from '@core/services/resources.service';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { Integration, IntegrationType } from '@shared/models/integration.models';
import { HasVersion } from '@shared/models/entity.models';
import { loadDefaultUplinkConverter } from '@home/components/iot-hub/iot-hub-default-converter';
import { downloadEntityJson, readableMessage, requestFailureMessage, splitTextWithLink } from '@home/components/iot-hub/iot-hub-utils';
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
  // Review mode: the installed package, whose install state the wizard shows
  reviewed?: DeviceInstalledItemDescriptor;
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
  // What the server's check found for it
  state?: DevicePackageEntityState;
}

/** What an update or install does with the entity of a step, decided on its progress step before anything changes. */
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
  // Overwrite: the version the user reviewed; the server rejects the request if someone changed the entity since
  version?: number;
  // Modified: the entity as the check found it, which the user can download before it is overwritten
  current?: any;
}

// Install: the conflict choice a Retry keeps while the existing entity is the same one
interface InstallConflictChoice {
  existingId: string;
  resolution: string;
}

/** Install: an entity found under the name a step's template gives. */
interface ExistingEntity {
  output: EntityStepOutput;
  // Sent with an overwrite
  version?: number;
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
  // Form: the key of its values in the install state
  stateKey?: string;
  // Update: the keys of the form's fields the installed version didn't have
  newFieldKeys?: Set<string>;
  formGroup?: UntypedFormGroup;
  // Connection check of the integration the form configures; null when the form configures none
  connectionCheck?: ConnectionCheck | null;
  // Progress
  entitySteps?: EntityStepProgress[];
  progressError?: string;
  progressDone?: boolean;
  // The user reviews what the install or update does and confirms the warnings (checking: still finding it out)
  reviewing?: boolean;
  checking?: boolean;
  // The server is applying the plan
  applying?: boolean;
}

export interface ConnectionCheck {
  // The INTEGRATION step template that the form values fill
  template: string;
  status: 'idle' | 'checking' | 'success' | 'error';
  // The values the status is for, as JSON: only the fields the integration template uses
  checkedValues?: string;
}

// The rows show the outcome of the one request one after another, this far apart
const REVEAL_INTERVAL = 150;
// Where the wizard keeps the converter it builds for a step with "default": true, as if the package had that file
const DEFAULT_CONVERTER_PATH = 'tb-default-converter/';
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

const DIRECT_MQTT_TRANSPORT: HostFieldTransport = {
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
};

// A gateway runs in Docker and puts ${mqtt.host} / ${mqtt.port} into docker-compose: the host must be
// reachable from the machine the gateway runs on, so the wording names the gateway, not the device.
const GATEWAY_MQTT_TRANSPORT: HostFieldTransport = {
  ...DIRECT_MQTT_TRANSPORT,
  hostHints: {
    REQUEST: 'iot-hub.device-install-gateway-mqtt-host-request-hint',
    BASE_URL: 'iot-hub.device-install-gateway-mqtt-host-base-url-hint'
  },
  loopbackHints: {
    REQUEST: 'iot-hub.device-install-gateway-mqtt-host-opened-via-loopback-hint',
    BASE_URL: 'iot-hub.device-install-gateway-mqtt-host-base-url-loopback-hint'
  },
  loopbackHint: 'iot-hub.device-install-gateway-mqtt-host-loopback-hint',
  reviewHostHint: 'iot-hub.device-install-gateway-mqtt-host-hint',
  configuredHint: 'iot-hub.device-install-gateway-mqtt-host-configured-hint',
  hostError: 'iot-hub.device-install-gateway-transport-host-error',
  helpPopupId: 'iot-hub/gateway-connection-host'
};

// Install methods whose packages get the transport fields, when a step of the package uses the
// host (see addTransportFields). Other methods use the host differently (a ChirpStack package builds
// the ThingsBoard URL from it) and keep the resolved value until they are handled separately.
// Every gateway method is listed: packages are published apart from UI releases, so one that starts
// using ${mqtt.host} in its docker-compose gets the fields without a UI change.
const HOST_FIELD_TRANSPORTS = new Map<string, HostFieldTransport>([
  [InstallMethod.DIRECT_MQTT, DIRECT_MQTT_TRANSPORT],
  ...Object.values(InstallMethod)
    .filter(method => method.startsWith('GATEWAY_'))
    .map((method): [string, HostFieldTransport] => [method, GATEWAY_MQTT_TRANSPORT])
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
   * Set once the server has installed or updated the package. From that moment every way out of
   * the wizard — including the header X and the Cancel button on a still-pending later step —
   * has to report 'installed', or the caller keeps showing pre-install counts and badges.
   */
  private packageInstalled = false;
  // Update: the hint of an entity someone changed since install, with a link to save it
  readonly modifiedHintText = splitTextWithLink(this.translate.instant('iot-hub.device-update-modified'));
  // Set however the dialog closes: a check of the steps still running then installs nothing
  private closed = false;
  // Update: the package's entities as the server finds them, by install step
  private updateStates = new Map<string, DevicePackageEntityState>();
  // Update: the files of the installed version, which the new version is compared with
  private previousFiles = new Map<string, string>();
  private previousPackageInfo: DevicePackageInfo | null = null;
  private entityPlans = new WeakMap<EntityStepProgress, EntityStepPlan>();
  // The check of the plan in progress; a way back meanwhile starts a new one, which drops this one's result
  private checkRun = 0;

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
  // form-field key → definition. Its type lets resolveTemplateJson() emit BOOLEAN/INTEGER
  // placeholders as raw JSON values instead of quoted strings.
  private formFieldDefs = new Map<string, FormFieldDefinition>();
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
    private iotHubApiService: IotHubApiService,
    private converterService: ConverterService,
    private integrationService: IntegrationService,
    private aiDashboardGenerationService: AiDashboardGenerationService,
    private dialogService: DialogService,
    private translate: TranslateService,
    private resourcesService: ResourcesService
  ) {
    super(store, router, dialogRef);
  }

  async ngOnInit(): Promise<void> {
    this.dialogRef.beforeClosed().subscribe(() => this.closed = true);
    this.allowAiDashboardGenerate = this.aiDashboardGenerationService.isAllowedDashboardGenerate({ requireTelemetry: true });
    try {
      await this.loadPackageFiles(this.data.item.id as string, this.zipFiles, this.zipImages);
      this.packageInfo = JSON.parse(this.zipFiles.get('device-info.json'));
      await this.addDefaultConverters();
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
    if (this.data.reviewMode && this.data.reviewed?.installState && this.data.selectedInstallMethod) {
      this.reviewMode = true;
      this.selectedInstallMethod = this.data.selectedInstallMethod;
      this.restoreInstallState();
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

  /**
   * Coming back to the progress step checks the package again, e.g. with the configuration changed, so a plan made
   * before is never sent. A check still running is dropped when its result comes.
   */
  private resetPlan(ws: WizardStep): void {
    if (ws.type === 'progress' && !ws.progressDone && !ws.applying) {
      if (ws.checking) {
        this.checkRun++;
        ws.checking = false;
      }
      ws.entitySteps?.forEach(ep => this.entityPlans.delete(ep));
      ws.entitySteps = null;
      ws.reviewing = false;
      ws.progressError = null;
    }
  }

  previousStep(): void {
    if (!this.stepper || this.stepper.selectedIndex <= 0) {
      return;
    }
    const step = this.currentWizardStep;
    if (step?.type === 'progress' && (step.applying || step.checking)) {
      return;
    }
    // Install: a failed install goes back to the form, whose values it was planned with; the selection change resets the plan
    if (!this.data.update && step?.type === 'progress' && step.progressError) {
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
    if (this.needsConnectionCheck(step)) {
      if (this.connectionChecking(step) || !(await this.checkConnection(step))) {
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

  /** True when the step checks the connection and its current values haven't passed the check yet. */
  private needsConnectionCheck(ws: WizardStep): boolean {
    const check = this.connectionCheckOf(ws);
    return !!check && !(check.status === 'success' && check.checkedValues === this.checkedValuesOf(ws, check));
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
    } catch (e) {
      // A template that doesn't resolve fails the check, rather than leaving the form waiting for it
      console.error('Failed to build the integration for the connection check', e);
      body = null;
    } finally {
      this.formValues = committedValues;
    }
    if (body) {
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
    } else {
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
      // Back through the stepper header too, not only the Back button: the progress steps after it check again
      this.wizardSteps.slice(event.selectedIndex + 1).forEach(ws => this.resetPlan(ws));
      return;
    }
    // Forward moves can come from the stepper header, which bypasses nextStep(). A step whose connection isn't
    // checked for these values is left as Next leaves it: back to it, and Next checks the connection and moves on
    // only when it passes. The stepper sets the new index after this event, so the move back waits for it.
    for (let i = event.previouslySelectedIndex; i < event.selectedIndex; i++) {
      if (this.needsConnectionCheck(this.wizardSteps[i])) {
        queueMicrotask(() => {
          this.stepper.selectedIndex = i;
          void this.nextStep();
        });
        return;
      }
    }
    // Commit every step being passed over here
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
    // Update: the installed item keeps the old version until the update succeeds
    this.dialogRef.close(this.data.update && !this.packageInstalled ? false : 'installed');
  }

  cancel(): void {
    // The server is applying the package; closing now would hide whether it did
    if (this.isApplying) {
      return;
    }
    this.dialogRef.close(this.packageInstalled ? 'installed' : false);
  }

  get isApplying(): boolean {
    return this.wizardSteps.some(ws => ws.applying);
  }

  /** A check or an install is running: the stepper header must not leave the step and start a second one. */
  get isBusy(): boolean {
    return this.wizardSteps.some(ws => ws.applying || ws.checking);
  }

  /**
   * Nothing was changed, so trying again starts from the check: the entities may have changed meanwhile. An update
   * asks to confirm every warning again; an install keeps a conflict choice while the existing entity is the same one,
   * except on the step that failed: the choice may be why it failed, so the user chooses again.
   */
  retry(ws: WizardStep): void {
    let choices: Map<DeviceInstallStep, InstallConflictChoice> | undefined;
    if (!this.data.update) {
      choices = new Map();
      for (const ep of ws.entitySteps ?? []) {
        const plan = this.entityPlans.get(ep);
        if (plan?.existing && plan.resolution && ep.status !== 'error') {
          choices.set(ep.step, { existingId: plan.existing.id, resolution: plan.resolution });
        }
      }
    }
    void this.planPackage(ws, choices);
  }

  goBackToForm(): void {
    // Find the last form step before the current progress step
    const currentIdx = this.stepper.selectedIndex;
    for (let i = currentIdx - 1; i >= 0; i--) {
      if (this.wizardSteps[i].type === 'form') {
        // The plan was made with the form values before: the selection change below resets it, so the check runs
        // again with the new ones
        const progressStep = this.wizardSteps[currentIdx];
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

  /** escape: applied to each substituted value, e.g. to keep a JSON template valid. */
  resolveVariables(content: string, escape: (value: string) => string = value => value,
                   outputs: Map<string, EntityStepOutput> = this.entityOutputs): string {
    return content.replace(/\$\{([^}]+)}/g, (_match, key) => {
      const res = this.resolveVariable(key, outputs);
      if (this.isResolved(key, res)) {
        return escape(res);
      }
      return '${' + key + '}';
    });
  }

  resolveVariable(key: string, outputs: Map<string, EntityStepOutput> = this.entityOutputs): string | undefined {
    if (key in this.formValues) {
      return String(this.formValues[key] ?? (this.formFieldDefs.get(key)?.type === FormFieldType.BOOLEAN ? false : ''));
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
      const output = outputs.get(alias);
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
  resolveTemplateJson(raw: string, outputs: Map<string, EntityStepOutput> = this.entityOutputs): any {
    const typed = raw.replace(/"\$\{([^"{}]+)}"/g, (whole, key) => {
      const type = this.formFieldDefs.get(key)?.type;
      if (type !== FormFieldType.BOOLEAN && type !== FormFieldType.INTEGER) {
        return whole;
      }
      const res = this.resolveVariable(key, outputs);
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
    // Placeholders sit inside JSON strings (or are booleans and numbers, which escaping leaves as they are): a value
    // with a quote, a backslash or a line break, e.g. a pasted password, must not end the string
    return JSON.parse(this.resolveVariables(typed, value => JSON.stringify(value).slice(1, -1), outputs));
  }

  /**
   * An optional text field left empty is resolved, e.g. no Wi-Fi password for an open network. Any other empty value
   * keeps its placeholder, so a missing value stays visible.
   */
  private isResolved(key: string, res: string | undefined): boolean {
    if (res) {
      return true;
    }
    const field = this.formFieldDefs.get(key);
    return res === '' && !!field && !field.required && field.type !== FormFieldType.INTEGER && field.type !== FormFieldType.BOOLEAN;
  }

  private _resolveMarkdownVariable(key: string): string | undefined {
    const res = this.resolveVariable(key);
    if (this.isResolved(key, res)) {
      return res;
    }
    // Special action placeholders
    if (key === 'gateway.downloadButton') {
      return '<a href="#" data-action="download-gateway-docker-compose" mat-stroked-button color="primary">⬇ Download docker-compose.yml</a>';
    }
    return undefined;
  }

  // --- Private ---

  /**
   * Takes the installed package's form values and the outputs of its entity steps, by alias.
   * withFormValues false: only the entity outputs, e.g. to keep the values the user has just entered.
   */
  private restoreInstallState(withFormValues = true): void {
    if (withFormValues) {
      for (const state of Object.values(this.installedDescriptor?.installState ?? {})) {
        Object.assign(this.formValues, state?.formValues);
      }
    }
    this.installedEntityOutputs().forEach((output, alias) => this.entityOutputs.set(alias, output));
  }

  /** The outputs of the installed package's entity steps, by alias, as its install state records them. */
  private installedEntityOutputs(): Map<string, EntityStepOutput> {
    const outputs = new Map<string, EntityStepOutput>();
    for (const step of this.packageInfo.installSteps[this.selectedInstallMethod] || []) {
      const alias = stepTypeAliasMap[step.type];
      const output = alias ? this.storedStateOf(step)?.state.entityOutput : undefined;
      if (output) {
        outputs.set(alias, output);
      }
    }
    return outputs;
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
      this.restoreInstallState(false);
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
          stateKey: this.stateKeyOf(step),
          rawSteps: [step],
          completed: this.reviewMode
        });
        i++;
      } else if (ENTITY_STEP_TYPES.has(step.type) && !this.reviewMode) {
        // An install or update is one transaction: every entity step goes in one progress step, after the last form
        // the entities may use, i.e. where the last group of entity steps is
        if (!rawSteps.slice(i + 1).some(s => ENTITY_STEP_TYPES.has(s.type))) {
          this.wizardSteps.push({
            type: 'progress',
            label: this.translate.instant(this.data.update ? 'iot-hub.device-update-step' : 'iot-hub.step-provisioning'),
            rawSteps: rawSteps.filter(s => ENTITY_STEP_TYPES.has(s.type)),
            completed: false
          });
        }
        i++;
      } else if (ENTITY_STEP_TYPES.has(step.type)) {
        // Review mode: consecutive entity steps, as installs before the one-transaction install showed them
        const group: DeviceInstallStep[] = [];
        while (i < rawSteps.length && ENTITY_STEP_TYPES.has(rawSteps[i].type)) {
          group.push(rawSteps[i]);
          i++;
        }
        this.wizardSteps.push({
          type: 'progress',
          label: this.translate.instant('iot-hub.step-provisioning'),
          rawSteps: group,
          completed: true
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
      storedValues = this.installedDescriptor.installState[ws.stateKey]?.formValues;
    } else if (this.data.update && ws.stateKey !== TRANSPORT_STEP_STATE_KEY && ws.rawSteps[0]) {
      // Also when the new version renamed the step
      storedValues = this.storedStateOf(ws.rawSteps[0])?.state.formValues;
      ws.newFieldKeys = this.newFieldKeysOf(ws.rawSteps[0], packageFields);
      // E.g. the profile name the integration maps the devices to: the device profile keeps the name it was installed
      // with, so the field shows that one and can't be changed
      const locked = this.integrationMetadataFieldKeys();
      ws.formFields = ws.formFields.map(field => locked.has(field.key) ? { ...field, readOnly: true, resettable: false } : field);
    }
    for (const field of ws.formFields) {
      this.formFieldDefs.set(field.key, field);
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
      // A read-only transport field shows the current Device connectivity setting: the installed version's host and
      // port may be out of date, and the user couldn't correct them. The step of its own has no stored values anyway
      const currentSetting = !this.reviewMode && field.readOnly
        && (field.key === TRANSPORT_HOST_FIELD_KEY || field.key === TRANSPORT_PORT_FIELD_KEY);
      const initialValue = this.resolveInitialFieldValue(field, currentSetting ? undefined : storedValues?.[field.key]);
      controls[field.key] = new UntypedFormControl({ value: initialValue, disabled: !!field.readOnly }, validators);
    }
    ws.formGroup = new UntypedFormGroup(controls);
    if (this.reviewMode) {
      ws.formGroup.disable();
      ws.completed = true;
    } else {
      this.applyDefaultValueTemplates(ws, storedValues);
      if (this.data.update && storedValues) {
        this.markStoredConnectionChecked(ws, storedValues);
      }
    }
  }

  /**
   * Update: the connection the installed version was set up with counts as checked, so an update that changes none
   * of its fields goes to the Update step without checking it again. A changed or new connection field is checked.
   */
  private markStoredConnectionChecked(ws: WizardStep, storedValues: Record<string, any>): void {
    const check = this.connectionCheckOf(ws);
    if (!check) {
      return;
    }
    const template = this.zipFiles.get(check.template) ?? '';
    const allStored = ws.formFields
      .filter(field => template.includes(`\${${field.key}}`))
      .every(field => field.key in storedValues);
    if (allStored) {
      check.status = 'success';
      check.checkedValues = this.checkedValuesOf(ws, check);
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
    } else if (step.type === 'progress' && !step.progressDone && !step.applying && !step.checking) {
      if (this.reviewMode) {
        this.showCompletedEntitySteps(step);
      } else if (!step.entitySteps) {
        void this.planPackage(step);
      }
    }
  }

  private showCompletedEntitySteps(ws: WizardStep): void {
    ws.entitySteps = ws.rawSteps.map(s => {
      const storedState = this.storedStateOf(s)?.state;
      return {
        step: s,
        status: 'success' as const,
        resolvedName: this.resolveVariables(s.name),
        entityOutput: storedState?.entityOutput,
        resolution: storedState?.resolution || 'created'
      };
    });
    ws.progressDone = true;
    ws.completed = true;
  }

  private markEntityStepSuccess(ep: EntityStepProgress, output: EntityStepOutput, resolution: string): void {
    ep.entityOutput = output;
    ep.resolution = resolution;
    ep.status = 'success';
    const alias = stepTypeAliasMap[ep.step.type];
    if (alias) {
      this.entityOutputs.set(alias, output);
    }
    this.cdr.detectChanges();
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

  private async findDeviceProfileByName(name: string): Promise<ExistingEntity | null> {
    const profiles = await firstValueFrom(this.deviceProfileService.getDeviceProfileNames());
    const match = profiles.find(p => p.name === name);
    if (!match) {
      return null;
    }
    const output = { id: match.id.id, name: match.name, url: `/profiles/deviceProfiles/${match.id.id}` };
    try {
      // The names lookup carries no version; read the matched profile so an overwrite can send it
      const profile = await firstValueFrom(this.deviceProfileService.getDeviceProfile(match.id.id, {ignoreErrors: true}));
      return { output, version: profile.version };
    } catch (_e) {
      return { output };
    }
  }

  private async findRuleChainByName(name: string): Promise<ExistingEntity | null> {
    const page = await firstValueFrom(this.ruleChainService.getRuleChains(new PageLink(100, 0, name)));
    const match = page.data.find(rc => rc.name === name);
    return match ? { output: { id: match.id.id, name: match.name, url: `/ruleChains/${match.id.id}` }, version: match.version } : null;
  }

  /** Converters are unique by (tenant, name, type). Match both. */
  private async findConverterByNameAndType(name: string, type: ConverterType): Promise<ExistingEntity | null> {
    const page = await firstValueFrom(this.converterService.getConverters(new PageLink(100, 0, name), {ignoreErrors: true}));
    const match = page.data.find(c => c.name === name && c.type === type);
    return match ? {
      output: { id: match.id.id, name: match.name, url: `/converters/${match.id.id}` },
      version: (match as Converter & HasVersion).version
    } : null;
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
  private async findIntegrationByName(name: string): Promise<ExistingEntity | null> {
    const page = await firstValueFrom(this.integrationService.getIntegrations(new PageLink(100, 0, name), {ignoreErrors: true}));
    const match = page.data.find(i => i.name === name);
    return match ? {
      output: { id: match.id.id, name: match.name, url: `/integrations/${match.id.id}`, routingKey: match.routingKey },
      version: (match as Integration & HasVersion).version
    } : null;
  }

  private async findDeviceByName(name: string): Promise<ExistingEntity | null> {
    try {
      const device = await firstValueFrom(this.deviceService.findByName(name, {ignoreErrors: true}));
      if (device) {
        return { output: { id: device.id.id, name: device.name, url: `/entities/devices/all/${device.id.id}` }, version: device.version };
      }
    } catch (_e) {
      // 404 = not found
    }
    return null;
  }

  private async findDashboardByTitle(title: string): Promise<ExistingEntity | null> {
    const page = await firstValueFrom(this.dashboardService.getTenantDashboards(new PageLink(10, 0, title), {ignoreErrors: true}));
    const match = page.data.find(d => d.title === title);
    return match ? { output: { id: match.id.id, name: match.title, url: `/dashboards/${match.id.id}` }, version: match.version } : null;
  }

  /**
   * The integration's metadata with the INTEGRATION step's entries added. The entries it has stay, so the packages on
   * one integration keep each other's. An entry that maps a name to itself, as profile.<name>: <name> does when the
   * user kept the default profile name, isn't added, and an earlier value of it is removed: the name is the default again.
   */
  private withStepMetadata(step: DeviceInstallStep, metadata: Record<string, string> | null | undefined): Record<string, string> {
    const merged = { ...(metadata ?? {}) };
    for (const [key, value] of Object.entries(this.stepMetadataEntries(step))) {
      if (value === null) {
        delete merged[key];
      } else {
        merged[key] = value;
      }
    }
    return merged;
  }

  /** The metadata entries resolved with the form values the package was installed with, as the text-only update has no forms. */
  private metadataWithInstalledValues(step: DeviceInstallStep): Record<string, string | null> | undefined {
    this.restoreInstallState();
    try {
      return this.integrationMetadataOf(step);
    } finally {
      this.formValues = {};
      this.entityOutputs.clear();
    }
  }

  /** Update: the form fields the INTEGRATION steps' metadata entries are made of, e.g. ${profileName}. */
  private integrationMetadataFieldKeys(): Set<string> {
    const keys = new Set<string>();
    for (const step of this.packageInfo.installSteps[this.selectedInstallMethod] ?? []) {
      if (step.type !== InstallStepType.INTEGRATION) {
        continue;
      }
      for (const [key, value] of Object.entries(step.metadata ?? {})) {
        for (const match of `${key} ${value ?? ''}`.matchAll(/\$\{([^}.]+)}/g)) {
          keys.add(match[1]);
        }
      }
    }
    return keys;
  }

  private integrationMetadataOf(step: DeviceInstallStep): Record<string, string | null> | undefined {
    return step.type === InstallStepType.INTEGRATION && step.metadata ? this.stepMetadataEntries(step) : undefined;
  }

  /** The INTEGRATION step's metadata entries, resolved: null for a self-mapping, whose earlier value is removed. */
  private stepMetadataEntries(step: DeviceInstallStep): Record<string, string | null> {
    const entries: Record<string, string | null> = {};
    for (const [rawKey, rawValue] of Object.entries(step.metadata ?? {})) {
      const key = this.resolveVariables(rawKey);
      const value = this.resolveVariables(String(rawValue ?? ''));
      if (value && !value.includes('${')) {
        entries[key] = value === key.substring(key.indexOf('.') + 1) ? null : value;
      }
    }
    return entries;
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

  private failureMessage(error: any): string {
    return requestFailureMessage(error, this.translate);
  }

  // --- Plan, review and apply: install and update ---

  /** What the update or install does with the entity of a step, once its progress step has checked it. */
  planOf(ep: EntityStepProgress): EntityStepPlan | undefined {
    return this.entityPlans.get(ep);
  }

  /** The warnings and name conflicts the user hasn't confirmed yet. */
  unconfirmedWarnings(ws: WizardStep): number {
    return (ws.entitySteps ?? []).filter(ep => this.entityPlans.get(ep)?.confirmed === false).length;
  }

  /**
   * True when the banner shows the error: an update's error about the whole request, e.g. its check, as a step's
   * error shows on its row; every install error, as the banner also says that nothing was installed.
   */
  hasPackageError(ws: WizardStep): boolean {
    return !!ws.progressError && (!this.data.update || !ws.entitySteps?.some(ep => ep.status === 'error'));
  }

  /**
   * An install never overwrites an existing device or gateway: its credentials are what its firmware uses. The user
   * can use it as it is.
   */
  canOverwriteExisting(ep: EntityStepProgress): boolean {
    return !!this.data.update || (ep.step.type !== InstallStepType.DEVICE && ep.step.type !== InstallStepType.GATEWAY);
  }

  /** resolution is the user's choice for an entity whose name is taken. */
  confirmWarning(ep: EntityStepProgress, resolution?: string): void {
    const plan = this.entityPlans.get(ep);
    if (plan) {
      plan.resolution = resolution;
      plan.confirmed = true;
    }
  }

  /** What the plan will do with the entity of a row; the same as appliedOutcomeOf says once it succeeded. */
  plannedOutcomeOf(plan: EntityStepPlan): string {
    return { update: 'iot-hub.device-update-will-update', create: 'iot-hub.device-package-will-create',
      keep: 'iot-hub.device-update-no-changes' }[this.outcomeOf(plan)];
  }

  /**
   * What the request did with the entity of a row, once it succeeded. Review mode has no plan: the row is done.
   * An install names the resolution, as there is nothing older to compare with.
   */
  appliedOutcomeOf(plan: EntityStepPlan | undefined): string {
    if (!plan) {
      return 'iot-hub.done';
    }
    if (!this.data.update) {
      return `iot-hub.device-install-resolution-${plan.resolution ?? 'created'}`;
    }
    return { update: 'iot-hub.device-package-updated', create: 'iot-hub.device-package-created',
      keep: 'iot-hub.device-package-skipped' }[this.outcomeOf(plan)];
  }

  /**
   * An entity kept or used as it is counts as not changed, even an integration the server adds the step's
   * metadata entries to; one a name conflict resolves to overwrite counts as updated.
   */
  private outcomeOf(plan: EntityStepPlan | undefined): 'update' | 'create' | 'keep' {
    if (!plan || plan.action === 'keep' || plan.resolution === 'use-existing') {
      return 'keep';
    }
    return plan.action === 'create' && plan.resolution !== 'overwrite' ? 'create' : 'update';
  }

  /** What happens to the step's entity: on an update the resolution chosen at install, else the one chosen now. */
  private resolutionOf(plan: EntityStepPlan | undefined): string {
    return plan?.previous?.resolution ?? plan?.resolution ?? 'created';
  }

  /** Update: saves the entity the user changed, as it is before the update overwrites it, to a JSON file. */
  downloadCurrentEntity(ep: EntityStepProgress): void {
    const current = this.entityPlans.get(ep)?.current;
    if (!current) {
      return;
    }
    downloadEntityJson(current, ep.resolvedName || ep.step.name);
  }

  /** The label of the button that applies the plan: Install or Update. */
  get applyActionKey(): string {
    return this.data.update ? 'action.update' : 'iot-hub.install';
  }

  /**
   * Sends the plan in one request, which the server applies in one transaction together with the installed item, or
   * nothing. The rows run together: the request is one. A success shows row by row, and the request counts as running
   * until the last row shows it, so the dialog can't be closed meanwhile.
   */
  async apply(ws: WizardStep): Promise<void> {
    ws.reviewing = false;
    ws.applying = true;
    ws.progressError = null;
    for (const ep of ws.entitySteps) {
      ep.status = 'running';
      ep.errorMessage = null;
    }
    this.cdr.detectChanges();
    let result: DevicePackageResult;
    try {
      const request: DevicePackageRequest = {
        versionId: this.data.item.id as string,
        selectedInstallMethod: this.selectedInstallMethod,
        installState: this.buildFormState(),
        steps: await this.buildPackageSteps(ws)
      };
      const config = { ignoreLoading: true, ignoreErrors: true };
      result = await firstValueFrom(this.data.update ?
        this.iotHubApiService.updateDevicePackage(this.data.update.installedItemId,
          { ...request, fromVersionId: this.data.update.fromVersionId }, config) :
        this.iotHubApiService.installDevicePackage(
          { ...request, customerId: this.data.customerId, entityGroupId: this.data.entityGroupId }, config));
    } catch (e: any) {
      result = { success: false, errorMessage: this.failureMessage(e) };
    }
    if (!result.success) {
      ws.applying = false;
      // Nothing was changed: the transaction was rolled back
      const message = readableMessage(result.errorMessage) ?? this.translate.instant('iot-hub.device-install-unexpected-error');
      for (const ep of ws.entitySteps) {
        const failed = this.stateKeyOf(ep.step) === result.failedStep;
        ep.status = failed ? 'error' : 'skipped';
        ep.errorMessage = failed ? message : null;
      }
      if (this.data.update) {
        ws.progressError = message;
        // The steps refer to the installed entities again
        this.restoreInstallState(false);
      } else {
        ws.progressError = `${message} ${this.translate.instant('iot-hub.device-install-nothing-installed')}`;
      }
      this.cdr.detectChanges();
      return;
    }
    const interval = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 0 : REVEAL_INTERVAL;
    for (const [index, ep] of ws.entitySteps.entries()) {
      if (index > 0 && interval) {
        await this.delay(interval);
      }
      const output = result.outputs?.[this.stateKeyOf(ep.step)];
      const plan = this.entityPlans.get(ep);
      const resolution = this.resolutionOf(plan);
      if (output) {
        const entityOutput = this.withEntityUrl(ep.step, output);
        if (ep.step.type === InstallStepType.GATEWAY) {
          this.setGatewayOutput(ep.step, entityOutput);
        }
        this.markEntityStepSuccess(ep, entityOutput, resolution);
      } else {
        ep.status = 'success';
        this.cdr.detectChanges();
      }
    }
    ws.applying = false;
    this.packageInstalled = true;
    ws.progressDone = true;
    ws.completed = true;
    this.cdr.detectChanges();
  }

  /** The values of the forms, which the install state keeps for the next update. */
  private buildFormState(): Record<string, any> {
    const state: Record<string, any> = {};
    for (const ws of this.wizardSteps) {
      if (ws.type === 'form' && ws.formGroup) {
        state[ws.stateKey] = { formValues: ws.formGroup.getRawValue() };
      }
    }
    return state;
  }

  /** The steps the server applies, each with its entity resolved from the template of the version being installed. */
  private async buildPackageSteps(ws: WizardStep): Promise<DevicePackageStep[]> {
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
    const steps: DevicePackageStep[] = [];
    for (const ep of ws.entitySteps) {
      steps.push(await this.buildPackageStep(ws, ep.step, this.entityPlans.get(ep)));
    }
    return steps;
  }

  private async buildPackageStep(ws: WizardStep, step: DeviceInstallStep, plan: EntityStepPlan): Promise<DevicePackageStep> {
    const base: DevicePackageStep = {
      name: step.name,
      id: step.id,
      type: step.type,
      action: 'KEEP',
      resolution: this.resolutionOf(plan),
      defaultConverter: step.default || undefined,
      // A kept integration, e.g. one the package took as it was, gets them merged by the server: the converter reads them
      metadata: this.integrationMetadataOf(step)
    };
    if (step.type === InstallStepType.GATEWAY_CONNECTOR) {
      return plan.action === 'keep' ? base : { ...base, action: 'OVERWRITE', entity: this.resolveTemplateJson(this.zipFiles.get(step.template)) };
    }
    if (plan.action === 'keep') {
      return { ...base, entityId: plan.previous.output.id };
    }
    // The entity gets the step's attributes, at install also one the user chose to use as it is
    const attributes = {
      serverAttributes: this.resolvePackageFile(this.zipFiles, step.serverAttributes, this.formValues),
      sharedAttributes: this.resolvePackageFile(this.zipFiles, step.sharedAttributes, this.formValues)
    };
    if (plan.resolution === 'use-existing') {
      // The package's own entity, under a step not matched to its installed one (e.g. renamed without an id),
      // stays the package's, so later versions still update it
      const resolution = this.isPackageEntity(plan.existing.id) ? 'created' : base.resolution;
      return { ...base, resolution, entityId: plan.existing.id, confirmed: true, ...(this.data.update ? {} : attributes) };
    }
    const overwritten = plan.action === 'overwrite' ? plan.previous.output : plan.resolution === 'overwrite' ? plan.existing : null;
    const entity = this.buildEntityBody(ws, step, overwritten);
    if (step.type === InstallStepType.INTEGRATION) {
      // An overwritten integration keeps the metadata it has, e.g. other packages' profile names
      const current = overwritten ?
        await firstValueFrom(this.integrationService.getIntegration(overwritten.id, {ignoreErrors: true})) : null;
      entity.configuration.metadata = this.withStepMetadata(step, { ...current?.configuration?.metadata, ...entity.configuration.metadata });
    }
    if (overwritten) {
      // An overwrite keeps the entity's credentials: only an update overwrites a device, and never the package's own
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
   * The entity of a step resolved from the template of the version being installed, as the server saves it. The server
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
        // A converter the request creates gets its id on the server
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

  /** A step output from the server, with the URL the wizard opens it at. */
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

  /**
   * A converter step with "default": true takes the converter ThingsBoard creates for a new integration of the method's
   * type (INTEGRATION_TTN: TTN). It becomes the step's template, so it installs and overwrites like a file of the package; an update
   * counts it as changed only when the step switches to or from the default converter (see entityStepChanged).
   */
  private async addDefaultConverters(): Promise<void> {
    const scriptLang = getCurrentAuthState(this.store).tbelEnabled ? ScriptLanguage.TBEL : ScriptLanguage.JS;
    for (const [method, steps] of Object.entries(this.packageInfo.installSteps ?? {})) {
      for (const step of steps) {
        if (step.type !== InstallStepType.UPLINK_CONVERTER || !step.default) {
          continue;
        }
        const integrationType = method.replace(/^INTEGRATION_/, '') as IntegrationType;
        const converter = await loadDefaultUplinkConverter(this.resourcesService, integrationType, scriptLang, step.name);
        if (!converter) {
          throw new Error(`ThingsBoard has no default converter for ${integrationType}`);
        }
        step.template = `${DEFAULT_CONVERTER_PATH}${integrationType}.json`;
        this.zipFiles.set(step.template, JSON.stringify(converter));
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
    this.restoreInstallState();
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
    const steps: DevicePackageStep[] = [];
    for (const step of this.packageInfo.installSteps[this.selectedInstallMethod] ?? []) {
      if (!ENTITY_STEP_TYPES.has(step.type)) {
        continue;
      }
      const stored = this.storedStateOf(step)?.state;
      if (step.type !== InstallStepType.GATEWAY_CONNECTOR && !stored?.entityOutput?.id) {
        return false;
      }
      steps.push({ name: step.name, id: step.id, type: step.type, action: 'KEEP', entityId: stored?.entityOutput?.id,
        resolution: stored?.resolution ?? 'created', confirmed: true, defaultConverter: step.default || undefined,
        metadata: this.metadataWithInstalledValues(step) });
    }
    // Under the new version's keys, as the wizard would store them
    const formState: Record<string, any> = {};
    if (installState[TRANSPORT_STEP_STATE_KEY]?.formValues) {
      formState[TRANSPORT_STEP_STATE_KEY] = { formValues: installState[TRANSPORT_STEP_STATE_KEY].formValues };
    }
    for (const step of this.packageInfo.installSteps[this.selectedInstallMethod] ?? []) {
      const formValues = step.type === InstallStepType.SHOW_FORM ? this.storedStateOf(step)?.state.formValues : undefined;
      if (formValues) {
        formState[this.stateKeyOf(step)] = { formValues };
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
   * Update: the step of the installed version that a step of the new one replaces: of the same type, with the same id,
   * or with the same name when either has no id. A step renamed without an id isn't matched: it is a new step.
   */
  private findPreviousStep(step: DeviceInstallStep): DeviceInstallStep | undefined {
    return (this.previousPackageInfo?.installSteps[this.selectedInstallMethod] ?? [])
      .find(s => s.type === step.type && (s.id && step.id ? s.id === step.id : s.name === step.name));
  }

  /** Update: the fields of a form step the installed version's form didn't have; all of them for a new form step. */
  private newFieldKeysOf(step: DeviceInstallStep, fields: FormFieldDefinition[]): Set<string> {
    if (!this.previousPackageInfo) {
      return new Set();
    }
    const previousStep = this.findPreviousStep(step);
    let previousKeys = new Set<string>();
    try {
      const previousForm = previousStep ? JSON.parse(this.previousFiles.get(previousStep.file) || '[]') as FormFieldDefinition[] : [];
      previousKeys = new Set(previousForm.map(field => field.key));
    } catch {
      return new Set();
    }
    return new Set(fields.map(field => field.key).filter(key => !previousKeys.has(key)));
  }



  /**
   * Update: true when the entity is one the installed package owns: it created it, not the user chose to use it as it was.
   * The same rule as the server's: createdEntityIds without usedExistingEntityIds, which an install made before the
   * descriptor kept them has only in its install state.
   */
  private isPackageEntity(id: string): boolean {
    const descriptor = this.data.update?.descriptor;
    const usedExisting = descriptor?.usedExistingEntityIds?.some(used => used.id === id) ??
      Object.values(descriptor?.installState ?? {}).some(state => state?.entityOutput?.id === id && state.resolution === 'use-existing');
    return !!descriptor?.createdEntityIds?.some(created => created.id === id) && !usedExisting;
  }

  /** The installed package: the one the wizard updates, or the one review mode shows. */
  private get installedDescriptor(): DeviceInstalledItemDescriptor | undefined {
    return this.data.update?.descriptor ?? this.data.reviewed;
  }

  /**
   * The key of the step's install state: its id, or else its type and name, so that two steps of an install method
   * never share it. A form without an id keeps its name, which no entity step's type-and-name key equals; a step id
   * equal to a form's name is caught by the duplicate check before the plan is sent, and by the server.
   * The server keys the steps of a request the same way.
   */
  private stateKeyOf(step: DeviceInstallStep): string {
    return step.id ?? (step.type === InstallStepType.SHOW_FORM ? step.name : `${step.type}:${step.name}`);
  }

  /**
   * A key two steps of the install method share (forms included: a form's values are saved under its key too), so one's
   * install state would overwrite the other's; null when none.
   */
  private duplicateStateKey(): string | null {
    const keys = new Set<string>();
    for (const step of this.packageInfo.installSteps[this.selectedInstallMethod] ?? []) {
      if (step.type !== InstallStepType.SHOW_FORM && !ENTITY_STEP_TYPES.has(step.type)) {
        continue;
      }
      const key = this.stateKeyOf(step);
      if (keys.has(key)) {
        return key;
      }
      keys.add(key);
    }
    return null;
  }

  /**
   * The installed package's install state of a step; in an update, under the key of the installed step it replaces.
   * Without the installed version's steps (e.g. it can't be downloaded any more), a step that has gained an id
   * is also looked up as the installed version had it, without the id.
   */
  private storedStateOf(step: DeviceInstallStep): { key: string; state: any } | undefined {
    const installState = this.installedDescriptor?.installState ?? {};
    const steps = [this.findPreviousStep(step), step,
      this.data.update && !this.previousPackageInfo && step.id ? { ...step, id: undefined } : undefined]
      .filter((s): s is DeviceInstallStep => !!s);
    const key = steps.map(s => this.stateKeyOf(s)).find(k => installState[k]);
    if (key) {
      return { key, state: installState[key] };
    }
    // Legacy: an entity step without an id was keyed by its name, as written or resolved, so a later step of the same
    // name overwrote its state. Such a state is the step's only when it records an entity of the step's type.
    const entityType = this.stepTypeToEntityType(step.type);
    const legacyKey = steps.filter(s => !s.id && s.type !== InstallStepType.SHOW_FORM)
      .flatMap(s => [s.name, this.resolveVariables(s.name)])
      .find(k => installState[k] && this.installedDescriptor.createdEntityIds?.some(
        created => created.id === installState[k].entityOutput?.id && created.entityType === entityType));
    return legacyKey ? { key: legacyKey, state: installState[legacyKey] } : undefined;
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
    // The default converter's template isn't a file of the package: it changes only when the step switches to or from the default converter
    if (step.default) {
      return !previousStep.default;
    }
    const previousFormValues = this.previousFormValues();
    const files: ('template' | 'serverAttributes' | 'sharedAttributes' | 'credentials')[] = ['template', 'serverAttributes', 'sharedAttributes', 'credentials'];
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

  /**
   * Finds out what the install or update does and what the user has to confirm, before anything changes. An install
   * with nothing to confirm starts at once. choices: on an install's Retry, the conflict choices to keep.
   */
  private async planPackage(ws: WizardStep, choices?: Map<DeviceInstallStep, InstallConflictChoice>): Promise<void> {
    const run = ++this.checkRun;
    if (this.data.update) {
      // Templates refer to the package's entities, e.g. ${deviceProfile.id}: until the steps run, the installed ones
      this.restoreInstallState(false);
    }
    ws.entitySteps = ws.rawSteps.map(s => ({
      step: s,
      status: 'pending' as const,
      resolvedName: this.resolveVariables(s.name)
    }));
    ws.reviewing = true;
    ws.progressError = null;
    const duplicateKey = this.duplicateStateKey();
    if (duplicateKey) {
      ws.progressError = this.translate.instant('iot-hub.device-install-duplicate-step', { key: duplicateKey });
      this.cdr.detectChanges();
      return;
    }
    ws.checking = true;
    this.cdr.detectChanges();
    try {
      if (this.data.update) {
        const states = await firstValueFrom(this.iotHubApiService.checkDeviceUpdate(this.data.update.installedItemId, { ignoreLoading: true }));
        this.updateStates = new Map(states.map(state => [state.stepName, state]));
      }
      const checkTemplates = this.preCheckTemplates(ws);
      const plans = await Promise.all(ws.entitySteps.map(ep => this.planEntityStep(ep, checkTemplates, choices)));
      if (this.closed || run !== this.checkRun) {
        // The dialog closed or the user went back meanwhile: the plan is of the form values before
        return;
      }
      ws.entitySteps.forEach((ep, i) => this.entityPlans.set(ep, plans[i]));
      if (this.data.update) {
        this.overwriteReferencesToCreated(ws);
      }
    } catch (e: any) {
      if (this.closed || run !== this.checkRun) {
        return;
      }
      ws.progressError = this.failureMessage(e);
    }
    ws.checking = false;
    this.cdr.detectChanges();
    if (!this.data.update && !ws.progressError && this.unconfirmedWarnings(ws) === 0) {
      await this.apply(ws);
    }
  }

  private async planEntityStep(ep: EntityStepProgress, checkTemplates: Map<DeviceInstallStep, any>,
                               choices?: Map<DeviceInstallStep, InstallConflictChoice>): Promise<EntityStepPlan> {
    const step = ep.step;
    if (this.data.update && step.type === InstallStepType.GATEWAY_CONNECTOR) {
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
    // At install, or new in this version, or deleted since: created, unless the user chooses otherwise for an entity
    // that has its name. A kept choice counts as confirmed
    const found = await this.preCheckEntity(step, checkTemplates.get(step));
    const existing = found?.output;
    const choice = existing ? choices?.get(step) : undefined;
    const resolution = choice && choice.existingId === existing.id ? choice.resolution : undefined;
    return { action: 'create', existing, resolution, confirmed: !existing || !!resolution, version: found?.version };
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

  /**
   * Names that refer to earlier steps resolve to the names the plan gives those steps, so a retry checks the same names;
   * ids stay placeholders. On an update the installed entities are used.
   */
  private preCheckTemplates(ws: WizardStep): Map<DeviceInstallStep, any> {
    const installed = this.data.update ? this.installedEntityOutputs() : new Map<string, EntityStepOutput>();
    const outputs = new Map(installed);
    const templates = new Map<DeviceInstallStep, any>();
    for (const ep of ws.entitySteps) {
      const raw = this.zipFiles.get(ep.step.template);
      if (!raw) {
        continue;
      }
      const template = this.resolveTemplateJson(raw, outputs);
      templates.set(ep.step, template);
      const alias = stepTypeAliasMap[ep.step.type];
      const name = this.templateNameOf(ep.step, template);
      if (alias && name && !installed.has(alias)) {
        outputs.set(alias, { name } as EntityStepOutput);
      }
    }
    return templates;
  }

  private templateNameOf(step: DeviceInstallStep, template: any): string | undefined {
    switch (step.type) {
      case InstallStepType.RULE_CHAIN:
        return template.ruleChain?.name || template.name;
      case InstallStepType.DASHBOARD:
        return template.title;
      default:
        return template.name;
    }
  }

  private async preCheckEntity(step: DeviceInstallStep, template: any): Promise<ExistingEntity | null> {
    if (!template) return null;
    const name = this.templateNameOf(step, template);
    switch (step.type) {
      case InstallStepType.DEVICE_PROFILE:
        return this.findDeviceProfileByName(name);
      case InstallStepType.RULE_CHAIN:
        return this.findRuleChainByName(name);
      case InstallStepType.DEVICE:
      case InstallStepType.GATEWAY:
        return this.findDeviceByName(name);
      case InstallStepType.DASHBOARD:
        return this.findDashboardByTitle(name);
      case InstallStepType.UPLINK_CONVERTER:
        return this.findConverterByNameAndType(name, ConverterType.UPLINK);
      case InstallStepType.DOWNLINK_CONVERTER:
        return this.findConverterByNameAndType(name, ConverterType.DOWNLINK);
      case InstallStepType.INTEGRATION:
        return this.findIntegrationByName(name);
      default:
        return null;
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
