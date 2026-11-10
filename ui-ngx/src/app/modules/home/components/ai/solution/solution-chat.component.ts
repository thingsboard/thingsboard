// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AiNotice } from '@shared/models/ai-chat.models';
import { AiArtifactPaneComponent } from '@home/components/ai/ai-artifact-pane.component';
import {
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  EventEmitter,
  HostListener,
  OnDestroy,
  OnInit,
  Output,
  signal,
  untracked,
  ViewChild
} from '@angular/core';
import {
  AiChatMessagesComponent,
  AnswerRatingEvent,
  AnswerReportEvent
} from '@home/components/ai/ai-chat-messages.component';
import { PageComponent } from '@shared/components/page.component';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import {
  AiQuestion,
  AiQuestionAnswer,
  ChatMessage,
  EntityResult,
  DashboardsOverview,
  GeneralChatMessage,
  SolutionEstimate,
  SolutionInstallProgress,
  NEW_SOLUTION_ID,
  SolutionCreatorInfo,
  SolutionDescriptor,
  SolutionInstallResult,
  SolutionMetadata,
  SolutionStep,
  SolutionStepStatus
} from '@shared/models/solution-creator.models';
import { AiPromptInputComponent } from '@home/components/ai/ai-prompt-input.component';
import { AiPromptMessage } from '@shared/models/ai-chat.models';
import { SolutionsCreatorService } from '@core/http/solutions-creator.service';
import {
  catchError,
  filter,
  finalize,
  map,
  switchMap,
  takeUntil,
  tap
} from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { EMPTY, Observable, of, Subject, timer } from 'rxjs';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { AiSolutionsService } from '@core/services/ai-solutions.service';
import { AuthService } from '@core/auth/auth.service';
import {
  SolutionInfoDialogComponent,
  SolutionInfoDialogData
} from '@home/components/ai/solution/solution-info/solution-info-dialog.component';
import { baseDetailsPageByEntityType, EntityType } from '@shared/models/entity-type.models';
import { isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { aggregateByName, IssueStatus } from '@home/components/ai/solution/solution-info/issues-aggregation';
import { SolutionJsonComponent } from '@home/components/ai/solution/pane/solution-json.component';
import { AiChatTitleComponent } from '@home/components/ai/ai-chat-title.component';
import { AiSpinKind } from '@home/components/ai/ai-action.models';
import { aiAnswersOf, aiAnswersText } from '@home/components/ai/ai-question.component';
import { EntityId } from '@shared/models/id/entity-id';

export type SolutionView = 'overview' | 'dashboards' | 'solution';

function formatCredits(credits: number): string {
  if (credits >= 1000) {
    const thousands = credits / 1000;
    return (thousands >= 10 ? Math.round(thousands) : Math.round(thousands * 10) / 10) + 'k';
  }
  return String(credits);
}

@Component({
  selector: 'tb-solution-chat',
  templateUrl: './solution-chat.component.html',
  styleUrls: ['./solution-chat.component.scss'],
  standalone: false
})
export class SolutionChatComponent extends PageComponent implements OnInit, OnDestroy {

  private static readonly NARROW_WIDTH = 1100;
  private static readonly END_IN_VIEW = 160;
  private static readonly CONNECTIVITY_GUIDE = 'https://thingsboard.io/docs/getting-started-guides/connectivity/';

  @ViewChild(AiPromptInputComponent) promptInput: AiPromptInputComponent;
  @ViewChild(AiChatMessagesComponent) chatMessages: AiChatMessagesComponent;
  @ViewChild(AiChatTitleComponent) titleRef: AiChatTitleComponent;
  @ViewChild(AiArtifactPaneComponent) pane: AiArtifactPaneComponent;
  @Output() loaded = new EventEmitter<string>();

  get hostElement(): HTMLElement {
    return this.elementRef.nativeElement;
  }

  isReadyStep = signal<boolean>(false);
  skipInterviewAllowed = signal<boolean>(false);
  solutionStep = signal<SolutionStep>(SolutionStep.INITIAL_CONFIGURATION);
  creatorStep = signal<'chat' | 'result' | null>('chat');

  messages = signal<ChatMessage[]>([]);

  loading = signal<boolean>(false);
  isGenerating = signal<boolean>(false);

  work = signal<{words: string; kind: 'think' | 'gen' | 'install'; since: number; lines?: string[]} | null>(null);

  fullscreen = signal<boolean>(false);

  title = signal<string>('');

  built = signal<boolean>(false);

  installed = signal<boolean>(false);

  mainDashboard = signal<EntityId[]>([]);

  logScrolled = signal(false);
  private atEnd = signal(true);
  private newTurns = signal(0);
  private seenTurns = 0;

  pill = computed<{mode: 'work' | 'new'; kind: AiSpinKind; label: string; count: number} | null>(() => {
    if (this.atEnd()) {
      return null;
    }
    const work = this.work();
    if (work && (this.loading() || this.isGenerating())) {
      return {mode: 'work', kind: work.kind, label: work.words, count: 0};
    }
    const count = this.newTurns();
    return count ? {mode: 'new', kind: 'think', label: this.translate.instant('ai-assistant.new-count', {count}), count} : null;
  });

  private newTurnsEffect = effect(() => {
    const total = this.viewMessages().length;
    untracked(() => {
      if (this.atEnd() || total <= this.seenTurns) {
        this.seenTurns = total;
        return;
      }
      this.newTurns.update(count => count + total - this.seenTurns);
      this.seenTurns = total;
    });
  });

  onLogScroll(area: HTMLElement): void {
    const scrolled = area.scrollTop > 0;
    if (scrolled !== this.logScrolled()) {
      this.logScrolled.set(scrolled);
    }
    const atEnd = area.scrollHeight - area.scrollTop - area.clientHeight < SolutionChatComponent.END_IN_VIEW;
    if (atEnd !== this.atEnd()) {
      this.atEnd.set(atEnd);
    }
    if (atEnd) {
      this.newTurns.set(0);
      this.seenTurns = this.viewMessages().length;
    }
  }

  goToEnd(area: HTMLElement): void {
    const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
    area.scrollTo({top: area.scrollHeight, behavior: reduce ? 'auto' : 'smooth'});
  }

  statusTip = computed(() => {
    const level = this.level();
    if (!level) {
      return '';
    }
    return level === 'error' && this.failure() ? this.failure() : this.translate.instant('ai-assistant.status-tip-' + level);
  });

  level = computed<'running' | 'needs' | 'error' | null>(() => this.loading() || this.isGenerating()
    ? 'running'
    : this.failure() ? 'error'
      : this.pendingQuestions().length || (this.isReadyStep() && this.creatorStep() === 'chat' && !this.built()) ? 'needs' : null);

  info = signal<SolutionCreatorInfo | null>(null);

  making = signal<SolutionView | null>(null);

  pickedView = signal<{view: SolutionView; phase: string} | null>(null);

  json = signal(false);

  jsonEditing = signal(false);

  uninstalledAt = signal<number | null>(null);

  failure = signal<string | null>(null);
  failedAt = signal<'install' | 'step'>('step');
  installProgress = signal<SolutionInstallProgress | null>(null);
  makeStartedAt = signal(0);
  private installOnly = signal(false);
  private messageSteps: SolutionStep[] = [];

  stopping = signal(false);

  notice = signal<AiNotice | null>(null);

  private stoppedAction: (() => void) | null = null;

  progressInfo = computed<SolutionCreatorInfo | null>(() => {
    const progress = this.installProgress();
    const info = this.info();
    if (!progress || !info || (!progress.items?.length && !Object.values(progress.entityResults ?? {}).some(results => results?.length))) {
      return null;
    }
    return {...info, metadata: {built: true, installed: false, installResult: {createdEntities: [], entityResults: progress.entityResults as any}}};
  });

  createShare = computed(() => {
    if (this.installOnly()) {
      return 0;
    }
    const estimates = this.info()?.estimates;
    const create = estimates?.CREATE?.seconds;
    const install = estimates?.INSTALL?.seconds;
    return create > 0 && install > 0 ? Math.min(.95, Math.max(.5, create / (create + install))) : .85;
  });

  makeSeconds = computed(() => {
    const estimates = this.info()?.estimates;
    if (this.installOnly()) {
      return Math.max(5, estimates?.INSTALL?.seconds ?? 15);
    }
    const seconds = (estimates?.CREATE?.seconds ?? 0) + (estimates?.INSTALL?.seconds ?? 0);
    return estimates?.CREATE && estimates?.INSTALL ? Math.max(20, seconds) : 90;
  });

  private closingPatch = signal<Partial<GeneralChatMessage>>({});

  private stepActions(): ChatMessage[] {
    const info = this.info();
    const result = info?.metadata?.installResult;
    const actions: ChatMessage[] = [{from: 'ACTION', data: {executionId: 'solution-create', tool: 'create_solution', status: 'SUCCESS', startedAt: 0}}];
    if (this.installed() && result) {
      const entities = Object.entries(result.entityResults ?? {})
        .filter(([type]) => type !== EntityType.ENTITY_GROUP && type !== EntityType.RULE_CHAIN)
        .flatMap(([, entries]) => (entries ?? []).filter(entry => entry?.id && entry.status !== 'ERROR').map(entry => entry.id));
      actions.push({from: 'ACTION', data: {executionId: 'solution-install', tool: 'install_solution', status: 'SUCCESS',
        startedAt: result.installedTime ?? 0, affectedEntities: entities}});
    }
    return actions;
  }

  closing = computed<GeneralChatMessage | null>(() => {
    const info = this.info();
    if (this.creatorStep() !== 'result' || !this.built() || !info) {
      return null;
    }
    const installed = this.installed();
    const result = info.metadata?.installResult;
    const user = this.mainUser();
    const chips: GeneralChatMessage['chips'] = [];
    if (installed && user) {
      chips.push({id: 'view-as', icon: 'person', label: this.translate.instant('solution-creator.view-as-name', {name: user.name})});
    }
    chips.push({id: 'edit', icon: 'edit', label: this.translate.instant('ai-assistant.edit-design')});
    if (installed) {
      chips.push({id: 'connect', icon: 'memory', label: this.translate.instant('solution-creator.connect-devices')});
      chips.push({id: 'uninstall', icon: 'delete_sweep', label: this.translate.instant('solution-creator.uninstall-solution')});
    }
    return {
      from: 'AI',
      content: installed ? this.installedSummary(result?.entityResults) : this.translate.instant('solution-creator.closing-created'),
      time: installed ? result?.installedTime : undefined,
      entities: installed && this.mainDashboard().length ? this.mainDashboard() : undefined,
      entitiesMeta: installed ? this.mainDashboardMeta() : undefined,
      hint: installed ? {
        lead: this.translate.instant('solution-creator.closing-simulated-lead'),
        text: this.translate.instant('solution-creator.closing-simulated')
      } : undefined,
      chips,
      ...this.closingPatch()
    };
  });

  viewMessages = computed<ChatMessage[]>(() => {
    const messages = this.messages();
    const lastAi = messages.map(message => message.from).lastIndexOf('AI');
    const waiting = this.isReadyStep() && this.creatorStep() === 'chat' && !this.built() && !this.loading() && !this.isGenerating();
    return messages.map((message, index) => {
      if (message.from === 'AI') {
        const general = message as GeneralChatMessage;
        const patch: Partial<GeneralChatMessage> = {};
        if (/^\s*(✅|🛠️|🛠)/u.test(general.content ?? '')) {
          patch.done = true;
        } else if (/^\s*(🤔|💡|🛑)/u.test(general.content ?? '')) {
          patch.done = false;
        }
        if (general.questions?.length) {
          patch.lead = this.translate.instant('solution-creator.assumptions');
        }
        if (waiting && index === lastAi) {
          patch.note = this.translate.instant(this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION
            ? 'solution-creator.next-create' : 'solution-creator.next-dashboards-note');
        }
        return Object.keys(patch).length ? {...general, ...patch} : message;
      }
      if (message.from === 'USER') {
        const previous = messages[index - 1] as GeneralChatMessage;
        const answers = previous?.from === 'AI' ? aiAnswersOf((message as GeneralChatMessage).content, previous.questions) : null;
        return answers ? {...message, answers} as GeneralChatMessage : message;
      }
      return message;
    }).concat(this.closing() ? [...this.stepActions(), this.closing()] : []);
  });

  onChip(id: string): void {
    switch (id) {
      case 'view-as':
        this.viewAsMainUser();
        break;
      case 'edit':
        this.editDesign();
        break;
      case 'connect':
        this.connectDevices();
        break;
      case 'uninstall':
        this.uninstallSolution();
        break;
    }
  }

  private installedSummary(results: Partial<Record<EntityType, EntityResult[]>> | undefined): string {
    const count = (...types: EntityType[]) => types.reduce((sum, type) =>
      sum + aggregateByName(type, results?.[type]).filter(row => row.status !== IssueStatus.ERROR).length, 0);
    const parts = [
      ['device', count(EntityType.DEVICE)],
      ['asset', count(EntityType.ASSET)],
      ['profile', count(EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE)],
      ['calculated-field', count(EntityType.CALCULATED_FIELD)],
      ['alarm-rule', count(EntityType.ALARM)],
      ['role', count(EntityType.ROLE)],
      ['user', count(EntityType.USER)],
      ['dashboard', count(EntityType.DASHBOARD)]
    ].filter(([, n]) => (n as number) > 0)
      .map(([key, n]) => this.translate.instant('solution-creator.installed-made.' + key, {count: n}));
    if (!parts.length) {
      return this.translate.instant('solution-creator.closing-installed');
    }
    const items = parts.length === 1 ? parts[0]
      : parts.slice(0, -1).join(', ') + ' ' + this.translate.instant('solution-creator.installed-and') + ' ' + parts[parts.length - 1];
    return this.translate.instant('solution-creator.installed-summary', {items});
  }

  private mainDashboardMeta(): string | undefined {
    const result = this.info()?.metadata?.installResult;
    const main = result?.mainDashboardId?.id;
    const dashboard = main ? result?.entityResults?.[EntityType.DASHBOARD]?.find(entry => entry.id?.id === main) : undefined;
    if (!dashboard) {
      return undefined;
    }
    const role = this.info()?.data?.dashboardsOverview?.find(overview => overview.name === dashboard.additionalData?.name)?.assignedTo;
    const user = this.mainUser();
    return [
      this.translate.instant('entity.type-dashboard'),
      role,
      user ? this.translate.instant('solution-creator.shared-with', {name: user.name}) : null
    ].filter(Boolean).join(' · ');
  }

  pendingQuestions = computed<AiQuestion[]>(() => {
    const last = this.messages()[this.messages().length - 1] as GeneralChatMessage;
    if (!last || last.from !== 'AI' || !last.questions?.length || this.loading() || this.isGenerating() || this.isReadyStep()
      || this.solutionStep() !== SolutionStep.INITIAL_CONFIGURATION) {
      return [];
    }
    return last.questions;
  });

  readonly aboutFeatures = [
    {icon: 'account_tree', title: 'solution-creator.about-design-title', line: 'solution-creator.about-design'},
    {icon: 'fact_check', title: 'solution-creator.about-approve-title', line: 'solution-creator.about-approve'},
    {icon: 'dashboard', title: 'solution-creator.about-dashboards-title', line: 'solution-creator.about-dashboards'},
    {icon: 'layers', title: 'solution-creator.about-installed-title', line: 'solution-creator.about-installed'}
  ];

  aboutOpen = signal(false);

  issues = computed<Array<{name: string; status: string; message?: string}>>(() => {
    const results = this.info()?.metadata?.installResult?.entityResults;
    if (!results || !this.info()?.metadata?.installed) {
      return [];
    }
    return Object.values(results).flat()
      .filter(entry => entry?.status === 'ERROR' || entry?.status === 'WARN')
      .map(entry => ({name: entry.additionalData?.name ?? '', status: entry.status, message: entry.error?.message}));
  });

  private paneEdgeEffect = effect(() => {
    this.currentView();
    this.json();
    this.paneOpen();
    untracked(() => setTimeout(() => this.pane?.refreshEdges(), 50));
  });

  private failureEffect = effect(() => {
    const id = this.info()?.id;
    const failure = this.failure();
    untracked(() => this.aiSolutions.setFailed(id, failure));
  });

  private runningEffect = effect(() => {
    const id = this.info()?.id;
    const running = this.loading() || this.isGenerating();
    untracked(() => this.aiSolutions.setRunning(id, running));
  });

  mainUser = computed<{id: string; name: string} | null>(() => {
    const result = this.info()?.metadata?.installResult;
    const main = result?.mainDashboardId?.id;
    const dashboard = main ? result?.entityResults?.[EntityType.DASHBOARD]?.find(entry => entry.id?.id === main) : undefined;
    const userId = dashboard?.additionalData?.userId;
    if (!userId) {
      return null;
    }
    const user = result.entityResults?.[EntityType.USER]?.find(entry => entry.id?.id === userId);
    return {id: userId, name: user?.additionalData?.name ?? ''};
  });

  jsonValue = computed(() => this.currentView() === 'dashboards'
    ? this.info()?.data?.dashboardsOverview
    : this.info()?.data?.solutionDescriptor);

  @ViewChild('jsonView') jsonView: SolutionJsonComponent;

  paneOpen = signal(typeof window === 'undefined' || window.innerWidth >= SolutionChatComponent.NARROW_WIDTH);

  phase = computed(() => `${this.creatorStep()}:${this.solutionStep()}`);

  views = computed<Array<{id: SolutionView; icon: string; state: 'have' | 'working' | 'next'}>>(() => {
    const info = this.info();
    const states = info?.states;
    const have: Record<SolutionView, boolean> = {
      overview: !!info?.data?.solutionDescriptor && states?.[SolutionStep.INITIAL_CONFIGURATION]?.status === SolutionStepStatus.READY,
      dashboards: !!info?.data?.dashboardsOverview?.length,
      solution: !!info?.metadata?.built
    };
    const icons: Record<SolutionView, string> = {overview: 'account_tree', dashboards: 'dashboard', solution: 'layers'};
    return (['overview', 'dashboards', 'solution'] as SolutionView[]).map(id => ({
      id,
      icon: icons[id],
      state: this.making() === id ? 'working' : have[id] ? 'have' : 'next'
    }));
  });

  currentView = computed<SolutionView | null>(() => {
    const views = this.views();
    const usable = (view: SolutionView) => views.find(item => item.id === view)?.state !== 'next';
    const picked = this.pickedView();
    if (picked && picked.phase === this.phase() && usable(picked.view)) {
      return picked.view;
    }
    if (this.making()) {
      return this.making();
    }
    const preferred: SolutionView = this.creatorStep() === 'result' ? 'solution'
      : this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION ? 'dashboards' : 'overview';
    if (usable(preferred)) {
      return preferred;
    }
    return [...views].reverse().find(item => item.state !== 'next')?.id ?? null;
  });

  showPane = computed(() => !!this.currentView());

  jsonAvailable = computed(() => !this.making() && (this.currentView() === 'overview' || this.currentView() === 'dashboards'));

  trayNote = computed(() => {
    const making = this.making();
    if (making) {
      return this.translate.instant(making === 'solution' ? 'solution-creator.pane.creating' : 'solution-creator.pane.designing');
    }
    if (this.failure()) {
      return this.translate.instant(this.failedAt() === 'install' ? 'solution-creator.pane.install-failed' : 'solution-creator.pane.step-failed');
    }
    return this.translate.instant('solution-creator.pane.view-' + this.currentView());
  });

  showMoreExamples = signal(false);

  isInitialEmpty = computed(() =>
    this.creatorStep() === 'chat' &&
    this.solutionStep() === SolutionStep.INITIAL_CONFIGURATION &&
    !this.messages().length &&
    !this.loading()
  );

  activePlaceholder = computed(() => this.isInitialEmpty()
    ? 'solution-creator.describe-solution-you-want'
    : this.pendingQuestions().length ? 'solution-creator.type-the-answer'
      : this.creatorStep() === 'result' ? 'solution-creator.ask-about-solution' : 'solution-creator.say-what-to-change');

  activeStepIndex = computed(() => {
    if (!this.creatorStep()) {
      return -1;
    }
    if (this.creatorStep() === 'result') {
      return 2;
    }
    if (this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION) {
      return 1;
    }
    return 0;
  });

  stepperLabels: string[] = [
    'solution-creator.step-architecture-design',
    'solution-creator.step-dashboard-design',
    'solution-creator.step-solution-installation',
  ];

  stepStatuses = signal<Array<'ready' | 'active' | 'pending'>>(['pending', 'pending', 'pending']);

  goToStep(index: number): void {
    if (this.stepStatuses()[index] !== 'ready') {
      return;
    }
    this.forceScroll = true;
    if (index === 0) {
      this.creatorStep.set('chat');
      this.solutionStep.set(SolutionStep.INITIAL_CONFIGURATION);
      this.updatedCurrentMsg();
    } else if (index === 1) {
      this.creatorStep.set('chat');
      this.solutionStep.set(SolutionStep.DASHBOARDS_CONFIGURATION);
      this.updatedCurrentMsg();
    } else if (index === 2) {
      this.creatorStep.set('result');
    }
  }

  private updateStepStatuses(): void {
    const active = this.activeStepIndex();
    const statuses: Array<'ready' | 'active' | 'pending'> = [0, 1, 2].map(i => {
      if (i === active) {
        return 'active';
      }
      if (!this.solutionInfo) {
        return 'pending';
      }
      if (i === 0 && this.solutionInfo.states?.[SolutionStep.INITIAL_CONFIGURATION]?.status === SolutionStepStatus.READY) {
        return 'ready';
      }
      if (i === 1 && this.solutionInfo.states?.[SolutionStep.DASHBOARDS_CONFIGURATION]?.status === SolutionStepStatus.READY) {
        return 'ready';
      }
      if (i === 2 && this.solutionInfo.metadata?.built) {
        return 'ready';
      }
      return 'pending';
    });
    this.stepStatuses.set(statuses);
  }


  solutionExamples: Array<{ key: string; labelKey: string; icon: string; message: string }> = [
    {
      key: 'coffeeMonitor',
      labelKey: 'solution-creator.coffee-machine-monitoring',
      icon: 'coffee',
      message: 'I want to monitor my coffee machine. It brews espresso, cappuccino, latte, and americano.\n\n' +
        'I need to track stock levels (beans, milk, water, cups), waste fullness, and temperatures. ' +
        'I also want to calculate daily totals for each drink type and daily revenue.\n\n' +
        'Alert me when stock runs low, waste is nearly full, or temperatures are out of range.'
    },
    {
      key: 'lorawanTracker',
      labelKey: 'solution-creator.lorawan-tracking',
      icon: 'cell_tower',
      message: 'I need a simple device management dashboard for monitoring my LoRaWAN tracker devices and connectivity.\n' +
        'I want to assign a label to device and see them on the map.\n' +
        'I also want to browse the device telemetry related to signal strength and raise alerts when device is offline, low battery or low signal strength.'
    },
    {
      key: 'waterMetering',
      labelKey: 'solution-creator.water-metering',
      icon: 'water_drop',
      message: 'I\'m building a water metering solution for a utility company. We deploy pulse-based water meters at residential and commercial properties to monitor consumption and track water costs.\n\n' +
        'The meter reports a cumulative pulse counter; consumption in liters is derived from the delta between readings.\n\n' +
        'Residential and commercial properties are billed at different rates. The price per liter is fixed per property type and set by the utility — not configurable per individual property.\n\n' +
        'Each property owner should see only their own consumption and cost data. Alert when consumption is abnormally high or a possible leak is detected. Alert thresholds should be configurable per property.'
    },
    {
      key: 'smartParking',
      labelKey: 'solution-creator.smart-parking',
      icon: 'local_parking',
      message: 'I would like to create a simple smart parking solution to monitor single level indoor parking lots with magnetic sensors.\n\n' +
        'There will be three roles:\n' +
        ' * Solution Administrator will provision customers, sensors and users. He needs to see the alarms from devices.\n' +
        ' * Technician will oversee all sensor statuses. He is interested to track the alarms.\n' +
        ' * Parking attendant view parking status.\n\n' +
        'Parking lots are relatively small, no need to create Zones or support parking spaces types.\n\n' +
        'Alarms:\n' +
        'An alarm must be generated for a sensor offline, a low battery, or if a vehicle overstays a designated time limit.\n' +
        'The low battery and max stay duration must be configured on the customer level.'
    },
    {
      key: 'weatherMonitoring',
      labelKey: 'solution-creator.weather-monitoring',
      icon: 'thermostat',
      message: 'I want to monitor outdoor weather conditions across multiple locations using weather stations. ' +
        'Each station reports standard atmospheric data — temperature, humidity, pressure, wind speed and direction, and rainfall.\n\n' +
        'Show all stations on a map. Alert when conditions reach extreme levels such as frost, strong wind, or heavy rainfall.'
    },
    {
      key: 'fleetGeofencing',
      labelKey: 'solution-creator.fleet-geofencing',
      icon: 'local_shipping',
      message: 'I want to build a fleet tracking solution for transport companies. Vehicles are tracked by GPS in real time and must operate within designated allowed zones while avoiding restricted areas.\n\n' +
        'Raise an alarm when a vehicle enters a restricted zone or leaves an allowed zone.'
    },
  ];

  SolutionStep = SolutionStep;
  solutionDescriptor: SolutionDescriptor;
  dashboardsOverview: DashboardsOverview[];
  solutionName: string;


  private solutionId: string;
  private _solutionInfo: SolutionCreatorInfo;

  private get solutionInfo(): SolutionCreatorInfo {
    return this._solutionInfo;
  }

  private set solutionInfo(value: SolutionCreatorInfo) {
    this._solutionInfo = value;
    this.info.set(value ? {...value} : null);
    this.title.set(value?.data?.solutionTitle ?? '');
    this.built.set(!!value?.metadata?.built);
    this.installed.set(!!value?.metadata?.installed);
    const main = value?.metadata?.installResult?.mainDashboardId;
    this.mainDashboard.set(value?.metadata?.installed && main ? [main] : []);
    this.updateStepStatuses();
  }
  private messages$ = toObservable(this.messages);
  private forceScroll = false;

  private cancelPending$ = new Subject<void>();
  private workTimers: NodeJS.Timeout[] = [];

  constructor(private route: ActivatedRoute,
              private solutionCreator: SolutionsCreatorService,
              private router: Router,
              private destroyRef: DestroyRef,
              private elementRef: ElementRef,
              private dialogService: DialogService,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private aiSolutions: AiSolutionsService,
              private authService: AuthService) {
    super();

    toObservable(this.activeStepIndex).pipe(
      takeUntilDestroyed(),
    ).subscribe(() => this.updateStepStatuses());

    this.messages$.pipe(
      takeUntilDestroyed(),
    ).subscribe(value => {
      const lastMsg = value[value.length - 1];
      if (this.forceScroll) {
        setTimeout(() => {
          const animationPanel: Element = this.elementRef.nativeElement.querySelector('.overview');
          if (animationPanel) {
            animationPanel.addEventListener('animationend', () => {
              this.scrollLogToEnd();
            }, { once: true });
          } else {
            this.scrollLogToEnd();
          }
        }, 0);
        this.forceScroll = false;
      } else if (lastMsg?.from === 'USER') {
        setTimeout(() => this.scrollLogToEnd(true), 0);
      } else if (lastMsg?.from === 'AI') {
        setTimeout(() => this.scrollLastQuestionToTop(), 0);
      }
    })
  }

  ngOnInit() {
    this.route.queryParams.pipe(
      filter(data => this.solutionId !== data.solutionId),
      tap(() => this.resetSolution()),
      switchMap(data => {
        if (data.solutionId && data.solutionId !== NEW_SOLUTION_ID) {
          return this.solutionCreator.getSolutionById(data.solutionId, {ignoreLoading: true, ignoreErrors: true}).pipe(
            catchError(err => {
              this.creatorStep.set('chat');
              if (err?.status === 400 || err?.status === 404) {
                this.router.navigate([], {
                  relativeTo: this.route,
                  queryParams: {solutionId: NEW_SOLUTION_ID},
                  queryParamsHandling: 'merge',
                  replaceUrl: true
                });
              } else {
                this.showErrorMessage(err);
              }
              return EMPTY;
            })
          );
        } else {
          this.creatorStep.set('chat');
          return EMPTY;
        }
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(solution => {
      this.solutionId = solution.id;
      this.solutionInfo = solution;
      this.forceScroll = true;
      this.initPredefinedSolution();
      this.loaded.emit(solution.id);
      setTimeout(() => this.scrollLogToEnd(), 300);
    });
  }

  ngOnDestroy(): void {
    this.clearWork();
    this.cancelPending$.next();
    this.cancelPending$.complete();
  }

  toggleFullscreen(): void {
    this.fullscreen.update(value => !value);
  }

  renameTo(title: string): void {
    if (!this.solutionId || title === this.title()) {
      return;
    }
    this.aiSolutions.rename(this.aiSolutions.find(this.solutionId) ?? {id: this.solutionId}, title).subscribe({
      next: value => this.updatedSolution(value),
      error: err => this.showErrorMessage(err)
    });
  }

  deleteSolution(): void {
    if (!this.solutionId) {
      return;
    }
    this.aiSolutions.confirmAndDelete(this.aiSolutions.find(this.solutionId) ?? {id: this.solutionId, solutionTitle: this.title()}).subscribe({
      next: deleted => {
        if (deleted) {
          this.router.navigate([], {relativeTo: this.route, queryParams: {solutionId: NEW_SOLUTION_ID}, queryParamsHandling: 'merge'});
        }
      },
      error: err => this.showErrorMessage(err)
    });
  }

  @HostListener('keydown.escape')
  closeAbout(): void {
    this.aboutOpen.set(false);
  }

  stop(): void {
    if (!this.solutionId || this.stopping()) {
      return;
    }
    this.stopping.set(true);
    this.solutionCreator.stopSolution(this.solutionId, {ignoreLoading: true, ignoreErrors: true}).subscribe({
      next: stopped => {
        if (!stopped) {
          this.stopping.set(false);
        }
      },
      error: () => this.stopping.set(false)
    });
  }

  resume(): void {
    const action = this.stoppedAction;
    this.stoppedAction = null;
    this.messages.update(list => list.filter(message => (message as GeneralChatMessage).status !== 'STOPPED'));
    action?.();
  }

  private noticeFor(err: any): boolean {
    const status = err?.status;
    const message = (parseHttpErrorMessage(err, this.translate, isNotEmptyStr(err?.error) ? 'text' : undefined, this.sanitizer).message ?? '').toString();
    if (status === 0) {
      this.setNotice('warn', 'wifi_off', 'connection-failed');
    } else if (/out of ai credits|ai_credits limit exceeded/i.test(message)) {
      this.setNotice('block', 'block', 'credits-used-up');
    } else if (status === 429) {
      this.setNotice('warn', 'hourglass_top', 'rate-limited');
    } else {
      return false;
    }
    return true;
  }

  private setNotice(kind: AiNotice['kind'], icon: string, key: string): void {
    this.notice.set({kind, icon, text: this.translate.instant('ai-assistant.notice.' + key)});
  }

  private wasStopped(err: any, again: () => void): boolean {
    if (!this.stopping() || err?.status !== 409) {
      return false;
    }
    this.stopping.set(false);
    this.stoppedAction = again;
    this.messageSteps = [...this.messageSteps, this.solutionStep()];
    this.messages.update(list => [...list, {from: 'AI', content: this.translate.instant('solution-creator.stopped'), status: 'STOPPED', time: Date.now()}]);
    setTimeout(() => this.scrollLogToEnd(true));
    return true;
  }

  private reportFailure(err: any, at?: 'install' | 'step'): void {
    this.failedAt.set(at ?? (this.creatorStep() === 'result' ? 'install' : 'step'));
    const responseType = isNotEmptyStr(err?.error) ? 'text' : undefined;
    this.failure.set(parseHttpErrorMessage(err, this.translate, responseType, this.sanitizer).message);
    this.paneOpen.set(true);
    setTimeout(() => this.scrollLogToEnd(true));
  }

  private openPaneForStep(): void {
    if (window.innerWidth >= SolutionChatComponent.NARROW_WIDTH) {
      this.paneOpen.set(true);
    }
  }

  viewAsMainUser(): void {
    const user = this.mainUser();
    const main = this.info()?.metadata?.installResult?.mainDashboardId?.id;
    if (!user || !main) {
      return;
    }
    const tenantUser = !this.info()?.metadata?.installResult?.entityResults?.[EntityType.USER]
      ?.find(entry => entry.id?.id === user.id)?.additionalData?.customer;
    this.authService.redirectUrl = tenantUser ? `/dashboards/all/${main}` : `/dashboards/${main}`;
    this.authService.loginAsUser(user.id).subscribe(() => {});
  }

  connectDevices(): void {
    window.open(SolutionChatComponent.CONNECTIVITY_GUIDE, '_blank', 'noopener');
  }

  fillExample(key: string): void {
    const example = this.solutionExamples.find(ex => ex.key === key);
    if (example) {
      this.promptInput?.setValue(example.message);
    }
  }

  nextStep() {
    if(this.solutionInfo.states[this.solutionStep()]?.status === SolutionStepStatus.READY) {
      if (this.solutionStep() === SolutionStep.INITIAL_CONFIGURATION) {
        const dashboardState = this.solutionInfo.states[SolutionStep.DASHBOARDS_CONFIGURATION];
        if (!dashboardState) {
          this.solutionInfo.states[SolutionStep.DASHBOARDS_CONFIGURATION] = {
            messages: [],
            status: SolutionStepStatus.IN_PROGRESS,
            chatId: null
          }
          this.solutionStep.set(SolutionStep.DASHBOARDS_CONFIGURATION);
          this.updatedCurrentMsg();
          this.sendDashboardConfigurationMsg('<start/>');
        } else if (dashboardState.pendingChanges?.length) {
          this.solutionStep.set(SolutionStep.DASHBOARDS_CONFIGURATION);
          this.updatedCurrentMsg();
          this.sendDashboardConfigurationMsg('<sync/>');
        } else {
          this.solutionStep.set(SolutionStep.DASHBOARDS_CONFIGURATION);
          this.updatedCurrentMsg();
        }
      } else if (this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION) {
        if (this.solutionInfo.metadata?.built) {
          this.creatorStep.set('result');
        } else {
          this.installProgress.set(null);
          this.makeStartedAt.set(Date.now());
          this.installOnly.set(false);
          const dialogRef = this.initGenerateState('create');
          const uninstallOrContinue$ = !this.solutionInfo.metadata?.installed
            ? of(null)
            : this.solutionCreator.uninstallSolution(this.solutionId, {ignoreLoading: true, ignoreErrors: true});
          uninstallOrContinue$.pipe(
            switchMap(() => this.withProgress(this.solutionCreator.createSolution(this.solutionId, {
              ignoreLoading: true,
              ignoreErrors: true
            }))),
            tap(() => this.creatorStep.set('result')),
            switchMap((solution) =>
              this.installWithProgress()
                .pipe(map((installData) => {
                  if (!solution.metadata) {
                    solution.metadata = {} as SolutionMetadata;
                  }
                  solution.metadata.installed = true;
                  if (!solution.metadata.installResult) {
                    solution.metadata.installResult = {} as SolutionInstallResult;
                  }
                  Object.assign(solution.metadata.installResult, installData);
                  return solution;
                }))
            ),
            takeUntil(this.cancelPending$),
            finalize(() => {
              this.isGenerating.set(false);
              dialogRef.close();
            })
          )
            .subscribe({
              next: (value) => {
                this.solutionInfo = value;
                this.aiSolutions.upsert(value);
                this.showInstalled();
              },
              error: (err) => {
                if (!this.wasStopped(err, () => this.nextStep()) && !this.noticeFor(err)) {
                  this.reportFailure(err);
                }
              }
            });
        }
      }
    }
  }

  installSolution() {
    this.failure.set(null);
    this.installProgress.set(null);
    this.makeStartedAt.set(Date.now());
    this.installOnly.set(true);
    if (window.innerWidth >= SolutionChatComponent.NARROW_WIDTH) {
      this.paneOpen.set(true);
    }
    const work = this.startWork([{text: 'solution-creator.step-loading-creating-entities', delay: 0}], 'install');
    this.making.set('solution');
    this.installWithProgress().pipe(
      takeUntil(this.cancelPending$),
      finalize(() => work.close())
    ).subscribe({
        next: (value) => {
          if (!this.solutionInfo.metadata) {
            this.solutionInfo.metadata = {} as SolutionMetadata;
          }
          if (!this.solutionInfo.metadata.installResult) {
            this.solutionInfo.metadata.installResult = {} as SolutionInstallResult;
          }
          Object.assign(this.solutionInfo.metadata.installResult, value);
          this.solutionInfo.metadata.installed = true;
          this.solutionInfo = {...this.solutionInfo};
          this.aiSolutions.update(this.solutionId, {installed: true});
          this.showInstalled();
        },
        error: (err) => {
          if (!this.wasStopped(err, () => this.installSolution()) && !this.noticeFor(err)) {
            this.reportFailure(err, 'install');
          }
        }
      });
  }

  private installWithProgress(): Observable<SolutionInstallResult> {
    return this.withProgress(this.solutionCreator.installSolution(this.solutionId, {ignoreLoading: true, ignoreErrors: true}));
  }

  private withProgress<T>(operation: Observable<T>): Observable<T> {
    const solutionId = this.solutionId;
    const finished$ = new Subject<void>();
    timer(800, 1200).pipe(
      switchMap(() => this.aiSolutions.installProgress(solutionId).pipe(catchError(() => of(null)))),
      takeUntil(finished$),
      takeUntil(this.cancelPending$)
    ).subscribe(progress => {
      if (progress?.running) {
        this.installProgress.set(progress);
      }
    });
    return operation.pipe(
      finalize(() => {
        finished$.next();
        finished$.complete();
      })
    );
  }

  estimateOf(view: SolutionView): string {
    const estimates = this.info()?.estimates;
    const operations: Record<SolutionView, Array<keyof NonNullable<SolutionCreatorInfo['estimates']>>> = {
      overview: ['INITIAL_CONFIGURATION'],
      dashboards: ['DASHBOARDS_CONFIGURATION'],
      solution: ['CREATE', 'INSTALL']
    };
    const known = operations[view].map(operation => estimates?.[operation]).filter((estimate): estimate is SolutionEstimate => !!estimate);
    if (!known.length || known.length < operations[view].length) {
      return this.translate.instant('solution-creator.pane.estimate-' + view);
    }
    const seconds = known.reduce((sum, estimate) => sum + estimate.seconds, 0);
    const credits = known.reduce((sum, estimate) => sum + estimate.credits, 0);
    const time = seconds < 50
      ? this.translate.instant('solution-creator.pane.about-seconds', {count: Math.max(10, Math.round(seconds / 10) * 10)})
      : seconds < 90
        ? this.translate.instant('solution-creator.pane.estimate-solution')
        : this.translate.instant('solution-creator.pane.about-minutes', {count: Math.round(seconds / 60)});
    return credits > 0 ? time + ' · ' + this.translate.instant('solution-creator.pane.credits', {credits: formatCredits(credits)}) : time;
  }

  answerQuestions(answers: AiQuestionAnswer[]): void {
    this.sendMsgFromUser({message: aiAnswersText(answers), resources: []});
  }

  skipQuestions(answers: AiQuestionAnswer[]): void {
    if (this.skipInterviewAllowed()) {
      this.skipInterview();
    } else {
      this.answerQuestions(answers);
    }
  }

  onRate(event: AnswerRatingEvent): void {
    const target = this.answerAt(event.index);
    if (!target) {
      return;
    }
    const before = target.answer.rating;
    this.patchAnswer(event.index, {rating: event.rating === 'NONE' ? undefined : event.rating});
    this.aiSolutions.sendFeedback(this.solutionId, target.step, {answer: target.answer.content, rating: event.rating}).subscribe({
      error: () => {
        this.patchAnswer(event.index, {rating: before});
        this.showErrorMessage(this.translate.instant('ai-assistant.feedback.failed'));
      }
    });
  }

  onReport(event: AnswerReportEvent): void {
    const target = this.answerAt(event.index);
    if (!target) {
      return;
    }
    this.patchAnswer(event.index, {reported: true});
    this.aiSolutions.sendFeedback(this.solutionId, target.step, {answer: target.answer.content, problem: event.problem, comment: event.comment}).subscribe({
      next: () => this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('ai-assistant.feedback.thanks'), type: 'success', modern: true,
        target: 'solution-creator-chat', verticalPosition: 'bottom', horizontalPosition: 'center'
      })),
      error: () => {
        this.patchAnswer(event.index, {reported: false});
        this.showErrorMessage(this.translate.instant('ai-assistant.feedback.failed'));
      }
    });
  }

  private answerAt(index: number): {answer: GeneralChatMessage; step: SolutionStep} | null {
    const message = this.viewMessages()[index] as GeneralChatMessage;
    if (!this.solutionId || message?.from !== 'AI' || !message.content) {
      return null;
    }
    return {answer: message, step: this.messageSteps[index] ?? this.solutionStep()};
  }

  private patchAnswer(index: number, patch: Partial<GeneralChatMessage>): void {
    if (index >= this.messages().length) {
      this.closingPatch.update(current => ({...current, ...patch}));
      return;
    }
    this.messages.update(list => list.map((message, at) => at === index ? {...message, ...patch} as GeneralChatMessage : message));
  }

  uninstallSolution() {
    this.dialogService.confirm(
      this.translate.instant('solution-creator.uninstall-solution-dialog-title', {name: this.solutionInfo.data.solutionTitle}),
      this.translate.instant('solution-creator.uninstall-solution-dialog-text'),
      this.translate.instant('action.no'),
      this.translate.instant('solution-creator.uninstall-solution-dialog-confirm')
    ).subscribe((value) => {
      if (value) {
        this.solutionCreator.uninstallSolution(this.solutionId, {ignoreErrors: true}).pipe(
          takeUntil(this.cancelPending$)
        ).subscribe((value) => {
          this.solutionInfo = value;
          this.aiSolutions.update(this.solutionId, {installed: false});
          this.uninstalledAt.set(Date.now());
          if (this.creatorStep() === 'result') {
            this.back();
          }
        });
      }
    })
  }

  startOver() {
    this.initLoadingState();
    this.solutionCreator.clearStep(this.solutionId, this.solutionStep(), {ignoreLoading: true, ignoreErrors: true}).pipe(
      takeUntil(this.cancelPending$)
    ).subscribe({
      next: () => {
        this.messages.set([]);
        this.messageSteps = [];
        this.isReadyStep.set(false);
        if (this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION) {
          this.sendDashboardConfigurationMsg('<start/>');
        } else {
          this.stopLoadingState();
        }
      },
      error: (err) => {
        this.showErrorMessage(err);
        this.stopLoadingState();
      },
      complete: () => {
        this.stopLoadingState();
      }
    })
  }

  editDesign(): void {
    if (this.creatorStep() === 'result') {
      this.back();
    }
  }

  back(): void {
    this.forceScroll = true;
    if (this.creatorStep() === 'result') {
      this.solutionStep.set(SolutionStep.DASHBOARDS_CONFIGURATION);
      this.creatorStep.set('chat');
    } else if(this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION) {
      this.solutionStep.set(SolutionStep.INITIAL_CONFIGURATION);
    }
    this.updatedCurrentMsg();
  }

  updatedSolutionDescriptor(descriptor: SolutionDescriptor) {
    this.solutionCreator.updateSolutionData(this.solutionId, 'solutionDescriptor', descriptor, {ignoreLoading: true}).pipe(
      takeUntil(this.cancelPending$)
    ).subscribe(value => {
        this.solutionInfo = value;
        this.solutionDescriptor = this.solutionInfo.data.solutionDescriptor;
      });
  }

  updatedDashboardOverview(dashboards: DashboardsOverview[]) {
    this.solutionCreator.updateSolutionData(this.solutionId, 'dashboardsOverview', dashboards, {ignoreLoading: true}).pipe(
      takeUntil(this.cancelPending$)
    ).subscribe(value => {
      this.solutionInfo = value;
      this.dashboardsOverview = this.solutionInfo.data.dashboardsOverview;
    });
  }

  get solutionMetadata(): Partial<SolutionMetadata> {
    return this.solutionInfo?.metadata ?? {};
  }

  get nextButtonLabel(): string {
    if (this.solutionStep() === SolutionStep.INITIAL_CONFIGURATION) {
      return 'solution-creator.go-dashboard-manager';
    } else {
      if (this.solutionInfo.metadata?.built) {
        return 'solution-creator.finish-edit';
      }
      if (this.solutionInfo.data.entities || this.solutionInfo.data.dashboards) {
        return 'solution-creator.update-solution';
      }
      return 'solution-creator.create-solution';
    }
  }

  updatedSolution($event: SolutionCreatorInfo) {
    if ($event.id === this.solutionId) {
      this.solutionName = $event.data?.solutionTitle;
      this.solutionInfo = $event;
    }
  }

  onInstalledChanged($event: {solutionId: string, installed: boolean}) {
    if ($event.solutionId === this.solutionId && this.solutionInfo) {
      if (!this.solutionInfo.metadata) {
        this.solutionInfo.metadata = {} as SolutionMetadata;
      }
      this.solutionInfo.metadata.installed = $event.installed;
    }
  }

  startCreateSolution(key: string) {
    const example = this.solutionExamples.find(ex => ex.key === key);
    if (example) {
      this.sendMsgFromUser({message: example.message, resources: []});
    }
  }

  private showInstalled(): void {
    this.creatorStep.set('result');
    this.json.set(false);
    this.pickedView.set({view: 'solution', phase: this.phase()});
    if (window.innerWidth >= SolutionChatComponent.NARROW_WIDTH) {
      this.paneOpen.set(true);
    }
  }

  gotoMainDashboard(openInfoDialog = false): void {
    this.router.navigateByUrl(
      this.router.createUrlTree([baseDetailsPageByEntityType.get(EntityType.DASHBOARD), this.solutionMetadata.installResult.mainDashboardId.id])
    ).then(()=> {
      if (openInfoDialog) {
        this.openInfo();
      }
    });
  }

  openInfo(): void {
    this.dialogService.dialog.open<SolutionInfoDialogComponent, SolutionInfoDialogData, void>(SolutionInfoDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        solution: this.solutionInfo
      }
    }).afterClosed().subscribe(() => {});
  }

  private sendMessage(msg: string, resources: string[] = []): Observable<SolutionCreatorInfo> {
    this.initLoadingState();

    const startOrContinue$ = this.solutionId
      ? of(null)
      : this.solutionCreator.startSolution({ignoreLoading: true, ignoreErrors: true}).pipe(
        tap(solution => {
          this.solutionId = solution.id;
          this.solutionInfo = solution;
          this.rememberSolution();
          this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { solutionId: this.solutionId },
            queryParamsHandling: 'merge',
          }).then(() => {});
        })
      );

    return startOrContinue$.pipe(
      switchMap(() =>
        this.solutionCreator.chatSolution(this.solutionId, this.solutionStep(), msg, {ignoreLoading: true, ignoreErrors: true}, resources)
      ),
      tap(solution => {
        this.solutionInfo = solution;
        this.rememberSolution();
        this.updatedCurrentMsg();
        if (this.isReadyStep()) {
          this.openPaneForStep();
        }
      }),
      catchError(err => {
        if (!this.wasStopped(err, () => this.sendMessage(msg, resources).subscribe(() => {})) && !this.noticeFor(err)) {
          this.showErrorMessage(err);
        }
        this.rememberSolution();
        return EMPTY;
      }),
      takeUntil(this.cancelPending$),
      finalize(() => this.stopLoadingState())
    );
  }

  private scrollLogToEnd(smooth = false): void {
    const log = this.elementRef.nativeElement.querySelector('.sol-log') as HTMLElement;
    log?.scrollTo({top: log.scrollHeight, behavior: smooth ? 'smooth' : 'auto'});
  }

  private scrollLastQuestionToTop(): void {
    const log = this.elementRef.nativeElement.querySelector('.sol-log') as HTMLElement;
    const questions = log?.querySelectorAll<HTMLElement>('tb-ai-chat-messages .user');
    const last = questions?.length ? questions[questions.length - 1] : null;
    if (log && last) {
      const top = log.scrollTop + last.getBoundingClientRect().top - log.getBoundingClientRect().top - 12;
      log.scrollTo({top, behavior: 'smooth'});
    }
  }

  private rememberSolution(): void {
    if (this.solutionInfo) {
      this.aiSolutions.upsert(this.solutionInfo);
    }
  }

  private sendDashboardConfigurationMsg(message: string): void {
    const dialogRef = this.initGenerateState(message === '<sync/>' ? 'sync' : 'design');
    this.sendMessage(message).pipe(
      finalize(() => {
        this.isGenerating.set(false);
        dialogRef.close();
      }),
    ).subscribe(() => {});
  }

  private updatedCurrentMsg() {
    const steps = this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION
      ? [SolutionStep.INITIAL_CONFIGURATION, SolutionStep.DASHBOARDS_CONFIGURATION]
      : [SolutionStep.INITIAL_CONFIGURATION];
    const visible = (msg: ChatMessage) => msg.from === 'AI' || (msg.from === 'USER' && !/^<[a-z-]+\/>$/.test(((msg as GeneralChatMessage).content ?? '').trim()));
    const tagged = steps.flatMap(step => (this.solutionInfo.states[step]?.messages ?? []).filter(visible).map(message => ({message, step})));
    this.messageSteps = tagged.map(item => item.step);
    this.messages.set(tagged.map(item => item.message));
    const stepState = this.solutionInfo.states[this.solutionStep()];
    if (stepState?.status === SolutionStepStatus.READY) {
      this.solutionName = this.solutionInfo.data?.solutionTitle;
      if (this.solutionStep() === SolutionStep.INITIAL_CONFIGURATION) {
        this.solutionDescriptor = this.solutionInfo.data.solutionDescriptor;
      }
      if (this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION) {
        this.dashboardsOverview = this.solutionInfo.data.dashboardsOverview;
      }
      this.isReadyStep.set(true);
      this.skipInterviewAllowed.set(false);
    } else {
      this.isReadyStep.set(false);
      this.skipInterviewAllowed.set(!!stepState?.skipInterviewAllowed);
    }
  }

  showErrorMessage(error: any): void {
    const responseType = isNotEmptyStr(error?.error) ? 'text' : undefined;
    const message = parseHttpErrorMessage(error, this.translate, responseType, this.sanitizer).message;
    const showOnTop = this.creatorStep() === 'result' || this.isInitialEmpty();
    this.store.dispatch(new ActionNotificationShow({
      message,
      modern: true,
      type: 'error',
      target: showOnTop ? 'solution-creator' : 'solution-creator-chat',
      verticalPosition: showOnTop ? 'top' : 'bottom',
      horizontalPosition: 'center',
      panelClass: showOnTop ? ( this.isInitialEmpty() ? 'mt-4' : 'mt-12') : ''
    }));
  }

  private resetSolution(): void {
    this.clearWork();
    this.pickedView.set(null);
    this.json.set(false);
    this.jsonEditing.set(false);
    this.installProgress.set(null);
    this.failedAt.set('step');
    this.uninstalledAt.set(null);
    this.cancelPending$.next();
    this.isReadyStep.set(false);
    this.skipInterviewAllowed.set(false);
    this.solutionStep.set(SolutionStep.INITIAL_CONFIGURATION);
    this.messages.set([]);
    this.messageSteps = [];
    this.closingPatch.set({});
    this.stopLoadingState();
    this.creatorStep.set(null);
    this.promptInput?.reset();
    this.showMoreExamples.set(false);
    this.solutionDescriptor = undefined;
    this.solutionId = undefined;
    this.solutionInfo = undefined;
    this.solutionName = undefined;
  }

  private initPredefinedSolution(): void {
    const listed = this.aiSolutions.find(this.solutionInfo?.id);
    this.failure.set(listed?.status === 'FAILED' ? listed.statusReason ?? null : null);
    if (this.solutionInfo.metadata?.built) {
      if (this.solutionInfo.states[SolutionStep.DASHBOARDS_CONFIGURATION]) {
        this.solutionStep.set(SolutionStep.DASHBOARDS_CONFIGURATION);
      }
      this.updatedCurrentMsg();
      this.creatorStep.set('result');
      return;
    }
    this.creatorStep.set('chat');
    const initialStatus = this.solutionInfo.states[SolutionStep.INITIAL_CONFIGURATION]?.status;
    const dashboardState = this.solutionInfo.states[SolutionStep.DASHBOARDS_CONFIGURATION];
    if (initialStatus === SolutionStepStatus.READY && dashboardState) {
      this.solutionStep.set(SolutionStep.DASHBOARDS_CONFIGURATION);
    }
    this.updatedCurrentMsg();
    if (this.isReadyStep()) {
      this.paneOpen.set(true);
    }
    if (this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION && dashboardState?.pendingChanges?.length) {
      this.sendDashboardConfigurationMsg('<sync/>');
    }
  }

  private initGenerateState(kind: 'design' | 'sync' | 'create') {
    this.failure.set(null);
    this.isGenerating.set(true);
    let messages: Array<{text: string; delay: number}>;
    let estimateWaitTime: number;
    if (this.solutionStep() === SolutionStep.INITIAL_CONFIGURATION) {
      estimateWaitTime = 30000;
      messages = [
        { text: 'solution-creator.step-loading-updating-entities', delay: 0 },
        { text: 'solution-creator.step-loading-restructuring-roles', delay: 10000 },
        { text: 'solution-creator.step-loading-rebuilding-alarm-rules', delay: 10000 },
        { text: 'solution-creator.step-loading-reassembling-dashboards', delay: 10000 },
        { text: 'solution-creator.step-loading-updating-solution', delay: 10000 },
      ];
    } else if (this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION) {
      if (kind === 'design') {
        estimateWaitTime = 30000;
        messages = [
          { text: 'solution-creator.step-loading-designing-dashboards-structure', delay: 0 },
          { text: 'solution-creator.step-loading-designing-dashboards-states', delay: 10000 },
          { text: 'solution-creator.step-loading-designing-widgets', delay: 10000 },
          { text: 'solution-creator.step-loading-adding-finishing-touches', delay: 10000 },
        ];
      } else if (kind === 'sync') {
        estimateWaitTime = 20000;
        messages = [
          { text: 'solution-creator.step-loading-evaluating-solution-changes', delay: 0 },
          { text: 'solution-creator.step-loading-reevaluating-use-cases', delay: 5000 },
          { text: 'solution-creator.step-loading-finalizing', delay: 15000 },
        ];
      } else {
        estimateWaitTime = 90000;
        messages = [
          { text: 'solution-creator.step-loading-creating-entities', delay: 0 },
          { text: 'solution-creator.step-loading-configuring-devices', delay: 10000 },
          { text: 'solution-creator.step-loading-setting-up-relations', delay: 10000 },
          { text: 'solution-creator.step-loading-structuring-access-controls', delay: 10000 },
          { text: 'solution-creator.step-loading-building-calculated-fields', delay: 10000 },
          { text: 'solution-creator.step-loading-building-alarm-rules', delay: 10000 },
          { text: 'solution-creator.step-loading-assembling-dashboards', delay: 10000 },
          { text: 'solution-creator.step-loading-preparing-states', delay: 10000 },
          { text: 'solution-creator.step-loading-initializing-solution', delay: 10000 },
        ];
      }
    }

    const creating = messages[0]?.text === 'solution-creator.step-loading-creating-entities';
    this.making.set(creating ? 'solution' : this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION ? 'dashboards' : 'overview');
    this.json.set(false);
    if (window.innerWidth >= SolutionChatComponent.NARROW_WIDTH) {
      this.paneOpen.set(true);
    }
    return this.startWork(messages, creating ? 'install' : 'gen', estimateWaitTime);
  }

  private startWork(messages: Array<{text: string; delay: number}>, kind: 'think' | 'gen' | 'install', _estimate?: number): {close: () => void} {
    const making = this.making();
    this.clearWork();
    this.making.set(making);
    const since = Date.now();
    const lines = messages.map(message => this.translate.instant(message.text) as string);
    let at = 0;
    messages.forEach((message, index) => {
      at += message.delay;
      this.workTimers.push(setTimeout(() => this.work.set({words: lines[index], kind, since, lines}), at));
    });
    return {close: () => this.clearWork()};
  }

  private clearWork(): void {
    this.workTimers.forEach(timer => clearTimeout(timer));
    this.workTimers = [];
    this.work.set(null);
    this.making.set(null);
  }

  pickView(view: SolutionView): void {
    this.pickedView.set({view, phase: this.phase()});
    this.json.set(false);
  }

  toggleJson(): void {
    this.jsonEditing.set(false);
    this.json.set(!this.json());
  }

  applyJson(): void {
    const value = this.jsonView?.edited();
    this.jsonView?.done();
    if (this.currentView() === 'dashboards') {
      this.updatedDashboardOverview(value as DashboardsOverview[]);
    } else {
      this.updatedSolutionDescriptor(value as SolutionDescriptor);
    }
  }

  openPane(): void {
    this.paneOpen.set(true);
  }

  closePane(): void {
    this.paneOpen.set(false);
    this.json.set(false);
    this.jsonEditing.set(false);
  }

  toggleMoreExamples(): void {
    this.showMoreExamples.update(v => !v);
  }

  skipInterview(): void {
    this.sendMessage('<skip-interview/>').subscribe(() => {});
  }

  sendMsgFromUser(prompt: AiPromptMessage): void {
    const time = Date.now();
    this.messageSteps = [...this.messageSteps, this.solutionStep()];
    this.messages.update(list => [...list, prompt.resources.length
      ? {from: 'USER', content: prompt.message, resources: prompt.resources, time}
      : {from: 'USER', content: prompt.message, time}]);
    this.sendMessage(prompt.message, prompt.resources.map(resource => resource.id)).subscribe(() => {});
  }

  private initLoadingState(): void {
    this.failure.set(null);
    if (this.notice()?.kind !== 'block') {
      this.notice.set(null);
    }
    this.loading.set(true);
    if (!this.workTimers.length) {
      this.startWork([{text: this.messages().length ? 'solution-creator.work-thinking' : 'solution-creator.work-reading', delay: 0}], 'think');
    }
  }

  private stopLoadingState(): void {
    this.loading.set(false);
    if (!this.isGenerating()) {
      this.clearWork();
    }
  }
}
