// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, DestroyRef, ElementRef, OnDestroy, OnInit, signal, ViewChild } from '@angular/core';
import { AiChatMessagesComponent } from '@home/components/ai/ai-chat-messages.component';
import { PageComponent } from '@shared/components/page.component';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import {
  ChatMessage,
  DashboardsOverview,
  SolutionCreatorInfo,
  SolutionDescriptor,
  SolutionInstallResult,
  SolutionMetadata,
  SolutionStep,
  SolutionStepStatus
} from '@shared/models/solution-creator.models';
import { AiPromptInputComponent } from '@home/components/ai/ai-prompt-input.component';
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
import { SolutionSideBarComponent } from '@home/pages/ai-solution-creator/side-bar/solution-side-bar.component';
import {
  SolutionInfoDialogComponent,
  SolutionInfoDialogData
} from '@home/pages/ai-solution-creator/solution-info/solution-info-dialog.component';
import { baseDetailsPageByEntityType, EntityType } from '@shared/models/entity-type.models';
import { isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { AiLoadingModalComponent } from '@home/components/ai/ai-loading-modal.component';
import { DynamicMatDialog } from '@shared/components/dialog/dynamic/dynamic-dialog';
import { generatedStateImages } from '@home/components/ai/ai.models';

@Component({
  selector: 'tb-solution-creator',
  templateUrl: './solution-creator.component.html',
  styleUrls: ['./solution-creator.component.scss'],
  standalone: false
})
export class SolutionCreatorComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild(AiPromptInputComponent) promptInput: AiPromptInputComponent;
  @ViewChild(AiChatMessagesComponent) chatMessages: AiChatMessagesComponent;
  @ViewChild(SolutionSideBarComponent, {static: true}) solutionSideBar: SolutionSideBarComponent;

  isReadyStep = signal<boolean>(false);
  skipInterviewAllowed = signal<boolean>(false);
  solutionStep = signal<SolutionStep>(SolutionStep.INITIAL_CONFIGURATION);
  creatorStep = signal<'chat' | 'result' | null>('chat');

  messages = signal<ChatMessage[]>([]);

  loading = signal<boolean>(false);
  loadingLongTime = signal<boolean>(false);
  isGenerating = signal<boolean>(false);

  showMoreExamples = signal(false);

  isInitialEmpty = computed(() =>
    this.creatorStep() === 'chat' &&
    this.solutionStep() === SolutionStep.INITIAL_CONFIGURATION &&
    !this.messages().length &&
    !this.loading()
  );

  activePlaceholder = computed(() =>
    this.isInitialEmpty() ? 'solution-creator.describe-solution-you-want' : 'solution-creator.enter-your-prompt'
  );

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
      icon: 'local_cafe',
      message: 'I want to monitor my coffee machine. It brews espresso, cappuccino, latte, and americano.\n\n' +
        'I need to track stock levels (beans, milk, water, cups), waste fullness, and temperatures. ' +
        'I also want to calculate daily totals for each drink type and daily revenue.\n\n' +
        'Alert me when stock runs low, waste is nearly full, or temperatures are out of range.'
    },
    {
      key: 'lorawanTracker',
      labelKey: 'solution-creator.lorawan-tracking',
      icon: 'sensors',
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
      icon: 'wb_cloudy',
      message: 'I want to monitor outdoor weather conditions across multiple locations using weather stations. ' +
        'Each station reports standard atmospheric data — temperature, humidity, pressure, wind speed and direction, and rainfall.\n\n' +
        'Show all stations on a map. Alert when conditions reach extreme levels such as frost, strong wind, or heavy rainfall.'
    },
    {
      key: 'fleetGeofencing',
      labelKey: 'solution-creator.fleet-geofencing',
      icon: 'directions_car',
      message: 'I want to build a fleet tracking solution for transport companies. Vehicles are tracked by GPS in real time and must operate within designated allowed zones while avoiding restricted areas.\n\n' +
        'Raise an alarm when a vehicle enters a restricted zone or leaves an allowed zone.'
    },
  ];

  SolutionStep = SolutionStep;
  solutionDescriptor: SolutionDescriptor;
  dashboardsOverview: DashboardsOverview[];
  solutionName: string;

  loadingImage: string;

  private solutionId: string;
  private _solutionInfo: SolutionCreatorInfo;

  private get solutionInfo(): SolutionCreatorInfo {
    return this._solutionInfo;
  }

  private set solutionInfo(value: SolutionCreatorInfo) {
    this._solutionInfo = value;
    this.updateStepStatuses();
  }
  private messages$ = toObservable(this.messages);
  private forceScroll = false;

  private cancelPending$ = new Subject<void>();
  private loadingTimer: NodeJS.Timeout;

  constructor(private route: ActivatedRoute,
              private solutionCreator: SolutionsCreatorService,
              private router: Router,
              private destroyRef: DestroyRef,
              private elementRef: ElementRef,
              private dialogService: DialogService,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private dynamicMatDialog: DynamicMatDialog) {
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
              this.chatMessages?.scrollToBottom('instant');
            }, { once: true });
          } else {
            this.chatMessages?.scrollToBottom('instant');
          }
        }, 0);
        this.forceScroll = false;
      } else if (lastMsg?.from === 'USER') {
        setTimeout(() => this.chatMessages?.scrollToBottom('smooth'), 0);
      } else if (lastMsg?.from === 'AI') {
        setTimeout(() => this.chatMessages?.scrollToLastUserMessage(), 0);
      }
    })
  }

  ngOnInit() {
    this.route.queryParams.pipe(
      filter(data => this.solutionId !== data.solutionId),
      tap(() => this.resetSolution()),
      switchMap(data => {
        if (data.solutionId) {
          return this.solutionCreator.getSolutionById(data.solutionId, {ignoreLoading: true, ignoreErrors: true}).pipe(
            catchError(err => {
              this.creatorStep.set('chat');
              this.showErrorMessage(err);
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
    });
  }

  ngOnDestroy(): void {
    this.cancelPending$.next();
    this.cancelPending$.complete();
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
          this.solutionSideBar.updateSolutionField({id: this.solutionId}, {installed: true});
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
          this.solutionSideBar.updateSolutionField({id: this.solutionId}, {installed: false});
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
      this.sendMsgFromUser(example.message);
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

  private sendMessage(msg: string): Observable<SolutionCreatorInfo> {
    this.initLoadingState();

    const startOrContinue$ = this.solutionId
      ? of(null)
      : this.solutionCreator.startSolution({ignoreLoading: true, ignoreErrors: true}).pipe(
        tap(solution => {
          this.solutionId = solution.id;
          this.solutionInfo = solution;
          this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { solutionId: this.solutionId },
            queryParamsHandling: 'merge',
          }).then(() => {});
        })
      );

    return startOrContinue$.pipe(
      switchMap(() =>
        this.solutionCreator.chatSolution(this.solutionId, this.solutionStep(), msg, {ignoreLoading: true, ignoreErrors: true})
      ),
      tap(solution => {
        this.solutionInfo = solution;
        this.solutionSideBar.addNewSolutionTemplate({id: this.solutionId, solutionTitle: this.solutionInfo?.data?.solutionTitle ?? '', built: this.solutionInfo?.metadata?.built, installed: this.solutionInfo?.metadata?.installed});
        this.updatedCurrentMsg();
      }),
      catchError(err => {
        this.showErrorMessage(err);
        this.solutionSideBar.addNewSolutionTemplate({id: this.solutionId, solutionTitle: this.solutionInfo?.data?.solutionTitle ?? '', built: this.solutionInfo?.metadata?.built, installed: this.solutionInfo?.metadata?.installed});
        return EMPTY;
      }),
      takeUntil(this.cancelPending$),
      finalize(() => this.stopLoadingState())
    );
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
    this.messages.set(this.solutionInfo.states[this.solutionStep()]?.messages?.filter(msg => msg.from === 'AI' || msg.from === 'USER') ?? []);
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

  private showErrorMessage(error: any): void {
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

    return this.dynamicMatDialog.open(AiLoadingModalComponent, {
      containerElement: this.elementRef.nativeElement,
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      backdropClass: 'ai-backdrop-filter',
      data: {
        messages,
        estimateWaitTime,
      }
    });
  }

  toggleMoreExamples(): void {
    this.showMoreExamples.update(v => !v);
  }

  skipInterview(): void {
    this.sendMessage('<skip-interview/>').subscribe(() => {});
  }

  sendMsgFromUser(msg: string): void {
    this.messages.update(list => [...list, {from: 'USER', content: msg}]);
    this.sendMessage(msg).subscribe(() => {});
  }

  private initLoadingState(): void {
    this.loading.set(true);
    this.loadingImage = generatedStateImages[Math.floor(Math.random() * generatedStateImages.length)];
    this.loadingTimer = setTimeout(() => {
      this.loadingLongTime.set(true);
    }, 5000);
  }

  private stopLoadingState(): void {
    this.loading.set(false);
    this.loadingLongTime.set(false);
    clearTimeout(this.loadingTimer);
  }
}
