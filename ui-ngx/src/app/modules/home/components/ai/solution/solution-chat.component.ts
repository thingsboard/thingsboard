// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, DestroyRef, ElementRef, EventEmitter, OnDestroy, OnInit, Output, signal, ViewChild } from '@angular/core';
import { AiChatMessagesComponent } from '@home/components/ai/ai-chat-messages.component';
import { PageComponent } from '@shared/components/page.component';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import {
  ChatMessage,
  DashboardsOverview,
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
import { EMPTY, Observable, of, Subject } from 'rxjs';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { AiSolutionsService } from '@core/services/ai-solutions.service';
import {
  SolutionInfoDialogComponent,
  SolutionInfoDialogData
} from '@home/components/ai/solution/solution-info/solution-info-dialog.component';
import { baseDetailsPageByEntityType, EntityType } from '@shared/models/entity-type.models';
import { deepClone, isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { FormControl } from '@angular/forms';
import { AiRenameDialogComponent, AiRenameDialogData } from '@home/components/ai/ai-rename-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';

export type SolutionView = 'overview' | 'dashboards' | 'solution';

@Component({
  selector: 'tb-solution-chat',
  templateUrl: './solution-chat.component.html',
  styleUrls: ['./solution-chat.component.scss'],
  standalone: false
})
export class SolutionChatComponent extends PageComponent implements OnInit, OnDestroy {

  private static readonly NARROW_WIDTH = 1100;
  private static readonly PANE_MIN = 360;
  private static readonly CHAT_MIN = 420;
  private static readonly PANE_WIDTH_KEY = 'tbSolutionPaneWidth';

  @ViewChild(AiPromptInputComponent) promptInput: AiPromptInputComponent;
  @ViewChild(AiChatMessagesComponent) chatMessages: AiChatMessagesComponent;
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

  work = signal<{words: string; kind: 'think' | 'gen' | 'install'; since: number} | null>(null);

  fullscreen = signal<boolean>(false);

  title = signal<string>('');

  built = signal<boolean>(false);

  installed = signal<boolean>(false);

  mainDashboard = signal<EntityId[]>([]);

  level = computed<'running' | 'needs' | null>(() => this.loading() || this.isGenerating()
    ? 'running'
    : this.isReadyStep() && this.creatorStep() === 'chat' && !this.built() ? 'needs' : null);

  info = signal<SolutionCreatorInfo | null>(null);

  making = signal<SolutionView | null>(null);

  pickedView = signal<{view: SolutionView; phase: string} | null>(null);

  json = signal(false);

  jsonControl = new FormControl<unknown>(null);

  paneOpen = signal(typeof window === 'undefined' || window.innerWidth >= SolutionChatComponent.NARROW_WIDTH);

  paneWidth = signal<number | null>(this.storedPaneWidth());

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
              private aiSolutions: AiSolutionsService) {
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

  renameSolution(): void {
    if (!this.solutionId) {
      return;
    }
    this.dialogService.dialog.open<AiRenameDialogComponent, AiRenameDialogData, string>(AiRenameDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: this.translate.instant('solution-creator.rename-solution-title'),
        value: this.title() || this.translate.instant('solution-creator.new-solution')
      }
    }).afterClosed().subscribe(newTitle => {
      const title = newTitle?.trim();
      if (title && title !== this.title()) {
        this.aiSolutions.rename(this.aiSolutions.find(this.solutionId) ?? {id: this.solutionId}, title).subscribe({
          next: value => this.updatedSolution(value),
          error: err => this.showErrorMessage(err)
        });
      }
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
          const dialogRef = this.initGenerateState();
          const uninstallOrContinue$ = !this.solutionInfo.metadata?.installed
            ? of(null)
            : this.solutionCreator.uninstallSolution(this.solutionId, {ignoreLoading: true, ignoreErrors: true});
          uninstallOrContinue$.pipe(
            switchMap(() => this.solutionCreator.createSolution(this.solutionId, {
              ignoreLoading: true,
              ignoreErrors: true
            })),
            tap(() => this.creatorStep.set('result')),
            switchMap((solution) =>
              this.solutionCreator.installSolution(this.solutionId, {ignoreLoading: true, ignoreErrors: true})
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
                if (value.metadata.installResult.mainDashboardId) {
                  this.gotoMainDashboard(true);
                } else {
                  this.openInfo();
                }
              },
              error: (err) => {
                this.showErrorMessage(err);
              }
            });
        }
      }
    }
  }

  installSolution() {
    const progressSubject = new Subject<void>();
    this.dialogService.progress(progressSubject.asObservable(), this.translate.instant('solution-creator.installing'));
    this.solutionCreator.installSolution(this.solutionId, {ignoreLoading: true, ignoreErrors: true}).pipe(
      takeUntil(this.cancelPending$)
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
          this.solutionInfo = this.solutionInfo;
          this.aiSolutions.update(this.solutionId, {installed: true});
          progressSubject.next();
          progressSubject.complete();
          if (value.mainDashboardId) {
            this.gotoMainDashboard(true);
          } else {
            this.openInfo();
          }
        },
        error: (err) => {
          progressSubject.next();
          progressSubject.complete();
          this.showErrorMessage(err);
        }
      });
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
      }),
      catchError(err => {
        this.showErrorMessage(err);
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
    const dialogRef = this.initGenerateState();
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
    this.messages.set(steps.flatMap(step => this.solutionInfo.states[step]?.messages ?? [])
      .filter(msg => msg.from === 'AI' || (msg.from === 'USER' && !/^<[a-z-]+\/>$/.test((msg.content ?? '').trim()))));
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
    this.cancelPending$.next();
    this.isReadyStep.set(false);
    this.skipInterviewAllowed.set(false);
    this.solutionStep.set(SolutionStep.INITIAL_CONFIGURATION);
    this.messages.set([]);
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
    if (this.solutionStep() === SolutionStep.DASHBOARDS_CONFIGURATION && dashboardState?.pendingChanges?.length) {
      this.sendDashboardConfigurationMsg('<sync/>');
    }
  }

  private initGenerateState() {
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
      if (!this.messages().length) {
        estimateWaitTime = 30000;
        messages = [
          { text: 'solution-creator.step-loading-designing-dashboards-structure', delay: 0 },
          { text: 'solution-creator.step-loading-designing-dashboards-states', delay: 10000 },
          { text: 'solution-creator.step-loading-designing-widgets', delay: 10000 },
          { text: 'solution-creator.step-loading-adding-finishing-touches', delay: 10000 },
        ];
      } else if (this.solutionInfo.states[SolutionStep.DASHBOARDS_CONFIGURATION]?.pendingChanges?.length) {
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
    this.clearWork();
    const since = Date.now();
    let at = 0;
    messages.forEach(message => {
      at += message.delay;
      this.workTimers.push(setTimeout(() => this.work.set({words: this.translate.instant(message.text), kind, since}), at));
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
    const on = !this.json();
    if (on) {
      const data = this.currentView() === 'dashboards' ? this.info()?.data?.dashboardsOverview : this.info()?.data?.solutionDescriptor;
      this.jsonControl.setValue(deepClone(data));
      this.jsonControl.markAsPristine();
    }
    this.json.set(on);
  }

  applyJson(): void {
    const value = this.jsonControl.value;
    this.jsonControl.markAsPristine();
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
  }

  startResize(event: PointerEvent): void {
    event.preventDefault();
    const edge = event.target as HTMLElement;
    const cols = this.elementRef.nativeElement.querySelector('.sol-cols') as HTMLElement;
    const pane = this.elementRef.nativeElement.querySelector('.sol-pane') as HTMLElement;
    if (!cols || !pane) {
      return;
    }
    const startX = event.clientX;
    const startWidth = pane.getBoundingClientRect().width;
    const max = cols.getBoundingClientRect().width - SolutionChatComponent.CHAT_MIN;
    edge.setPointerCapture(event.pointerId);
    edge.classList.add('held');
    let closing = false;
    const move = (e: PointerEvent) => {
      const width = startWidth + (startX - e.clientX);
      closing = width < SolutionChatComponent.PANE_MIN - 72;
      pane.classList.toggle('closing', closing);
      this.paneWidth.set(Math.round(Math.min(max, Math.max(SolutionChatComponent.PANE_MIN, width))));
    };
    const up = () => {
      edge.removeEventListener('pointermove', move);
      edge.removeEventListener('pointerup', up);
      edge.removeEventListener('pointercancel', up);
      edge.classList.remove('held');
      pane.classList.remove('closing');
      if (closing) {
        this.paneWidth.set(startWidth);
        this.closePane();
      } else {
        this.storePaneWidth();
      }
    };
    edge.addEventListener('pointermove', move);
    edge.addEventListener('pointerup', up);
    edge.addEventListener('pointercancel', up);
  }

  resetPaneWidth(): void {
    this.paneWidth.set(null);
    this.storePaneWidth();
  }

  onEdgeKey(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.resetPaneWidth();
      return;
    }
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') {
      return;
    }
    event.preventDefault();
    const pane = this.elementRef.nativeElement.querySelector('.sol-pane') as HTMLElement;
    const cols = this.elementRef.nativeElement.querySelector('.sol-cols') as HTMLElement;
    if (!pane || !cols) {
      return;
    }
    const step = event.shiftKey ? 64 : 16;
    const width = pane.getBoundingClientRect().width + (event.key === 'ArrowLeft' ? step : -step);
    const max = cols.getBoundingClientRect().width - SolutionChatComponent.CHAT_MIN;
    this.paneWidth.set(Math.round(Math.min(max, Math.max(SolutionChatComponent.PANE_MIN, width))));
    this.storePaneWidth();
  }

  private storedPaneWidth(): number | null {
    try {
      return Number(localStorage.getItem(SolutionChatComponent.PANE_WIDTH_KEY)) || null;
    } catch {
      return null;
    }
  }

  private storePaneWidth(): void {
    try {
      const width = this.paneWidth();
      if (width) {
        localStorage.setItem(SolutionChatComponent.PANE_WIDTH_KEY, String(width));
      } else {
        localStorage.removeItem(SolutionChatComponent.PANE_WIDTH_KEY);
      }
    } catch {
      return;
    }
  }

  toggleMoreExamples(): void {
    this.showMoreExamples.update(v => !v);
  }

  skipInterview(): void {
    this.sendMessage('<skip-interview/>').subscribe(() => {});
  }

  sendMsgFromUser(prompt: AiPromptMessage): void {
    this.messages.update(list => [...list, prompt.resources.length
      ? {from: 'USER', content: prompt.message, resources: prompt.resources}
      : {from: 'USER', content: prompt.message}]);
    this.sendMessage(prompt.message, prompt.resources.map(resource => resource.id)).subscribe(() => {});
  }

  private initLoadingState(): void {
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
