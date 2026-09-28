// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Renderer2,
  SimpleChanges, Type,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetContext } from '@home/models/widget-component.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import {
  CalendarQueryParam,
  SchedulerEvent,
  SchedulerEventMode,
  SchedulerEventWithCustomerInfo,
  SchedulerRepeatType,
  schedulerTimeUnitRepeatTranslationMap
} from '@shared/models/scheduler-event.models';
import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, forkJoin, merge, Observable, of, shareReplay } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { catchError, debounceTime, distinctUntilChanged, filter, map, share, skip, take, tap } from 'rxjs/operators';
import { PageLink, PageQueryParam } from '@shared/models/page/page-link';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort, SortDirection } from '@angular/material/sort';
import { Direction, SortOrder, sortOrderFromString } from '@shared/models/page/sort-order';
import { TranslateService } from '@ngx-translate/core';
import { deepClone, isDefined, isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  SchedulerEventDialogComponent,
  SchedulerEventDialogData
} from '@home/components/scheduler/scheduler-event-dialog.component';
import {
  defaultSchedulerEventConfigTypes,
  reportSchedulerEventTypes,
  SchedulerEventConfigType
} from '@home/components/scheduler/scheduler-event-config.models';
import { DialogService } from '@core/services/dialog.service';
import dayGridPlugin from '@fullcalendar/daygrid';
import listPlugin from '@fullcalendar/list';
import timeGridPlugin from '@fullcalendar/timegrid';
import momentPlugin, { toMoment } from '@fullcalendar/moment';
import interactionPlugin, { DateClickArg } from '@fullcalendar/interaction';
import { FullCalendarComponent } from '@fullcalendar/angular';
import {
  scheduleInfo,
  schedulerCalendarView,
  schedulerCalendarViewTranslationMap,
  schedulerCalendarViewValueMap,
  SchedulerEventsWidgetSettings
} from '@home/components/scheduler/scheduler-events.models';
import { Calendar, CalendarOptions, Duration, EventClickArg, EventDropArg, EventInput, } from '@fullcalendar/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { ActivatedRoute, QueryParamsHandling, Router } from '@angular/router';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';
import { EntityType } from '@shared/models/entity-type.models';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { asRoughMs, rangeContainsMarker } from '@fullcalendar/core/internal';
import _moment from 'moment';
import { FormBuilder } from '@angular/forms';
import { isValidPageStepCount, isValidPageStepIncrement } from '@home/components/widget/lib/table-widget.models';
import { VersionControlComponent } from '@home/components/vc/version-control.component';
import { MatIconButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { DomSanitizer } from "@angular/platform-browser";
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';

@Component({
    selector: 'tb-scheduler-events',
    templateUrl: './scheduler-events.component.html',
    styleUrls: ['./scheduler-events.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SchedulerEventsComponent extends PageComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {

  replaceComponent: Type<any>;

  @ViewChild('replaceComponentAnchor', {static: true}) replaceComponentAnchor: TbAnchorComponent;
  @ViewChild('schedulerEventWidgetContainer', {static: true}) schedulerEventWidgetContainerRef: ElementRef;
  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild('calendarContainer') calendarContainer: ElementRef<HTMLElement>;
  @ViewChild('calendar')
  set calendarComponent(comp: FullCalendarComponent) {
    if (comp) {
      this.calendarApi = comp.getApi();
      this.calendarApi.render();
      this.isCalendarInitialized.next(true);
      this.cd.detectChanges();
    }
  }
  @ViewChild('schedulerEventMenuTrigger') schedulerEventMenuTrigger: MatMenuTrigger;

  @Input() widgetMode: boolean;
  @Input() ctx: WidgetContext;
  @Input() edgeId: string = this.route.snapshot.params.edgeId;

  authUser = getCurrentAuthUser(this.store);
  reportingEnabled = getCurrentAuthState(this.store).reportingEnabled;
  editEnabled = this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.WRITE);
  vcEnabled = this.userPermissionsService.hasGenericPermission(Resource.VERSION_CONTROL, Operation.READ) &&
    this.authUser.authority === Authority.TENANT_ADMIN;
  addEnabled = this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.CREATE);
  deleteEnabled = this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.DELETE);
  showData = (this.authUser.authority === Authority.TENANT_ADMIN ||
      this.authUser.authority === Authority.CUSTOMER_USER) &&
    this.userPermissionsService.hasGenericPermission(Resource.SCHEDULER_EVENT, Operation.READ);

  enabledViews: 'both' | 'list' | 'calendar' = 'both'
  mode: SchedulerEventMode = 'list';
  displayPagination = true;
  pageSizeOptions: Array<number> = [];
  defaultPageSize: number;
  defaultSortOrder = 'createdTime';
  defaultEventType: string;
  hidePageSize = false;
  noDataDisplayMessageText = this.translate.instant('scheduler.no-scheduler-events');
  displayedColumns: string[];
  pageLink: PageLink;
  textSearchMode = false;
  assignEnabled = false;
  dataSource: SchedulerEventsDatasource;
  isCalendarInitialized = new BehaviorSubject<boolean>(false);
  currentCalendarView = schedulerCalendarView.month;
  schedulerCalendarViews = Object.keys(schedulerCalendarView) as schedulerCalendarView[];
  schedulerCalendarViewTranslations = schedulerCalendarViewTranslationMap;
  schedulerEventMenuPosition = {x: '0px', y: '0px'};
  schedulerContextMenuEvent: MouseEvent;
  calendarOptions: CalendarOptions;
  textSearch = this.fb.control('', {nonNullable: true});
  currentCalendarViewValue = schedulerCalendarViewValueMap.get(this.currentCalendarView);
  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};
  initialCalendarDate: Date | null = null;

  private calendarApi: Calendar;
  private schedulerEvents: Array<SchedulerEventWithCustomerInfo> = [];
  private componentResize$: ResizeObserver;
  private backNavigationCommands? = this.route.snapshot.data.backNavigationCommands;
  private modeHandler: SchedulerModeHandler;

  constructor(
    protected store: Store<AppState>,
    private customTranslatePipe: CustomTranslatePipe,
    private translate: TranslateService,
    private schedulerEventService: SchedulerEventService,
    private userPermissionsService: UserPermissionsService,
    private dialogService: DialogService,
    private dialog: MatDialog,
    private router: Router,
    private route: ActivatedRoute,
    private cd: ChangeDetectorRef,
    private fb: FormBuilder,
    private zone: NgZone,
    private renderer: Renderer2,
    private popoverService: TbPopoverService,
    private viewContainerRef: ViewContainerRef,
    private destroyRef: DestroyRef,
    private sanitizer: DomSanitizer
  ) {
    super();
  }

  ngOnInit(): void {

    if (this.route.snapshot.data.replaceComponent) {
      this.replaceComponent = this.route.snapshot.data.replaceComponent(this.store);
    }

    const viewContainerRef = this.replaceComponentAnchor.viewContainerRef;
    viewContainerRef.clear();
    if (this.replaceComponent) {
      viewContainerRef.createComponent(this.replaceComponent);
      this.showData = false;
    } else {
      this.modeHandler = SchedulerModeHandler.create(this, this.router, this.route);
      this.modeHandler.initialize(this.schedulerEventService, this.userPermissionsService, this.customTranslatePipe);

      if (this.displayPagination) {
        this.setupResizeObserver();
      }

      this.setupCalendarOptions();
    }
  }

  private setupResizeObserver(): void {
    this.componentResize$ = new ResizeObserver(() => {
      this.zone.run(() => {
        const showHidePageSize = this.schedulerEventWidgetContainerRef.nativeElement.offsetWidth < hidePageSizePixelValue;
        if (showHidePageSize !== this.hidePageSize) {
          this.hidePageSize = showHidePageSize;
          this.cd.markForCheck();
        }
      });
    });
    this.componentResize$.observe(this.schedulerEventWidgetContainerRef.nativeElement);
  }

  private setupCalendarOptions(): void {
    this.calendarOptions = {
      plugins: [interactionPlugin, momentPlugin, dayGridPlugin, listPlugin, timeGridPlugin],
      height: '100%',
      fixedWeekCount: false,
      initialView: this.currentCalendarViewValue,
      initialDate: this.initialCalendarDate,
      allDaySlot: false,
      editable: this.editEnabled,
      headerToolbar: false,
      selectable: false,
      eventDisplay: 'block',
      eventDurationEditable: false,
      lazyFetching: false,
      events: this.eventSourceFunction.bind(this),
      eventClick: this.onEventClick.bind(this),
      dateClick: this.onDayClick.bind(this),
      eventDrop: this.onEventDrop.bind(this),
      eventDidMount: this.onEventDidMount.bind(this)
    };
  }

  ngOnDestroy(): void {
    if (this.componentResize$) {
      this.componentResize$.disconnect();
    }
    if (this.calendarApi) {
      this.calendarApi.destroy();
      this.isCalendarInitialized.complete();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.previousValue && change.currentValue !== change.previousValue) {
        if (propName === 'edgeId') {
          this.reloadSchedulerEvents();
        }
      }
    }
  }

  ngAfterViewInit(): void {
    if (!this.showData) return;

    this.setupTextSearchSubscription();
    this.setupSortAndPaginatorSubscriptions();

    this.modeHandler.setupAfterViewInitSubscriptions(this.destroyRef);

    this.updateData();
  }

  private setupTextSearchSubscription(): void {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((_prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      this.modeHandler.handleTextSearchChange(value);
      if (this.mode === 'calendar') {
        this.calendarApi.refetchEvents();
      }
    });
  }

  private setupSortAndPaginatorSubscriptions(): void {
    const sortSubscription$ = this.sort.sortChange.asObservable().pipe(
      map(data => {
        const direction = data.direction.toUpperCase();
        const queryParams: PageQueryParam = {
          direction: Direction.DESC === direction ? null : direction as Direction,
          property: this.defaultSortOrder === data.active ? null : data.active,
          page: null
        };
        if (this.displayPagination) {
          this.paginator.pageIndex = 0;
        }
        return queryParams;
      })
    );

    let paginatorSubscription$: Observable<object>;
    if (this.displayPagination) {
      paginatorSubscription$ = this.paginator.page.asObservable().pipe(
        map(data => ({
          page: data.pageIndex === 0 ? null : data.pageIndex,
          pageSize: data.pageSize === this.defaultPageSize ? null : data.pageSize
        }))
      );
    }

    (this.displayPagination ? merge(sortSubscription$, paginatorSubscription$) : sortSubscription$).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(queryParams => {
      this.modeHandler.handleSortOrPageChange(queryParams as PageQueryParam);
    });
  }

  onEditModeChanged(): void {
    if (this.textSearchMode) {
      this.ctx.hideTitlePanel = !this.ctx.isEdit;
      this.ctx.detectChanges(true);
    }
  }

  displayBackButton(): boolean {
    return isDefinedAndNotNull(this.backNavigationCommands);
  }

  goBack(): void {
    this.router.navigate(this.backNavigationCommands, { relativeTo: this.route }).then(() => {});
  }

  resize(): void {
    if (this.mode === 'calendar' && this.calendarApi) {
      this.calendarApi.updateSize();
    }
  }

  updateMode(mode: SchedulerEventMode, updateRouterQueryParams: boolean = true): void {
    this.mode = mode;
    const skipUpdateData = this.modeHandler.handleUpdateMode(mode, updateRouterQueryParams);
    if (mode === 'calendar') {
      this.dataSource?.selection.clear();
      if (this.isCalendarInitialized.value) {
        this.calendarApi.refetchEvents();
        if (this.widgetMode) {
          this.calendarApi.updateSize();
        }
      }
    } else if (!skipUpdateData) {
      this.updateData();
    }
  }

  updateData(): void {
    if (this.mode === 'calendar') {
      this.isCalendarInitialized.pipe(
        filter(isInitialized => isInitialized),
        take(1)
      ).subscribe(() => {
        this.resize();
      });
    } else {
      if (this.displayPagination) {
        this.pageLink.page = this.paginator.pageIndex;
        this.pageLink.pageSize = this.paginator.pageSize;
      } else {
        this.pageLink.page = 0;
      }
      this.pageLink.sortOrder.property = this.sort.active;
      this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
      this.dataSource.edgeId = this.edgeId;
      this.dataSource.loadEntities(this.pageLink, this.defaultEventType);
    }
    if (this.widgetMode) {
      this.ctx.detectChanges();
    }
  }

  enterFilterMode(): void {
    this.textSearchMode = true;
    if (this.widgetMode) {
      this.ctx.hideTitlePanel = true;
      this.ctx.detectChanges(true);
    }
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode(): void {
    this.textSearchMode = false;
    this.textSearch.reset();
    if (this.widgetMode) {
      this.ctx.hideTitlePanel = false;
      this.ctx.detectChanges(true);
    }
  }

  reloadSchedulerEvents(): void {
    if (this.mode === 'calendar') {
      this.calendarApi.refetchEvents();
    } else {
      this.updateData();
    }
  }

  deleteSchedulerEvent($event: Event, schedulerEvent: SchedulerEventWithCustomerInfo): void {
    $event?.stopPropagation();
    const title = this.translate.instant('scheduler.delete-scheduler-event-title', {schedulerEventName: schedulerEvent.name});
    const content = this.translate.instant('scheduler.delete-scheduler-event-text');
    this.dialogService.confirm(title, content, this.translate.instant('action.no'), this.translate.instant('action.yes')).subscribe(result => {
      if (result) {
        this.schedulerEventService.deleteSchedulerEvent(schedulerEvent.id.id).subscribe(() => this.reloadSchedulerEvents());
      }
    });
  }

  deleteSchedulerEvents($event: Event): void {
    $event?.stopPropagation();
    const selectedSchedulerEvents = this.dataSource.selection.selected;
    if (selectedSchedulerEvents?.length) {
      const title = this.translate.instant('scheduler.delete-scheduler-events-title', {count: selectedSchedulerEvents.length});
      const content = this.translate.instant('scheduler.delete-scheduler-events-text');
      this.dialogService.confirm(title, content, this.translate.instant('action.no'), this.translate.instant('action.yes')).subscribe(result => {
        if (result) {
          const tasks = selectedSchedulerEvents.map(event => this.schedulerEventService.deleteSchedulerEvent(event.id.id));
          forkJoin(tasks).subscribe(() => this.reloadSchedulerEvents());
        }
      });
    }
  }

  addSchedulerEvent($event: Event): void {
    this.openSchedulerEventDialog($event);
  }

  assignToEdgeSchedulerEvent($event: Event): void {
    this.openAssignSchedulerEventToEdgeDialog($event);
  }

  editSchedulerEvent($event: Event, schedulerEventWithCustomerInfo: SchedulerEventWithCustomerInfo): void {
    $event?.stopPropagation();
    this.schedulerEventService.getSchedulerEvent(schedulerEventWithCustomerInfo.id.id).subscribe(event => {
      this.openSchedulerEventDialog($event, event);
    });
  }

  viewSchedulerEvent($event: Event, schedulerEventWithCustomerInfo: SchedulerEventWithCustomerInfo): void {
    $event?.stopPropagation();
    this.schedulerEventService.getSchedulerEvent(schedulerEventWithCustomerInfo.id.id).subscribe(event => {
      this.openSchedulerEventDialog($event, event, true);
    });
  }

  private openSchedulerEventDialog($event: Event, schedulerEvent?: SchedulerEvent, readonly = false): void {
    $event?.stopPropagation();
    let isAdd = false;
    if (!schedulerEvent || !schedulerEvent.id) {
      isAdd = true;
      schedulerEvent = schedulerEvent || {
        name: null,
        type: null,
        schedule: null,
        configuration: {
          originatorId: null,
          msgType: null,
          msgBody: {},
          metadata: {}
        }
      };
    }
    this.dialog.open<SchedulerEventDialogComponent, SchedulerEventDialogData, boolean>(SchedulerEventDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        schedulerEventConfigTypes: this.schedulerEventConfigTypes,
        isAdd,
        readonly,
        schedulerEvent,
        defaultEventType: this.defaultEventType
      }
    }).afterClosed().subscribe(res => {
      if (res) this.reloadSchedulerEvents();
    });
  }

  private openAssignSchedulerEventToEdgeDialog($event: Event): void {
    $event?.stopPropagation();
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData, Array<string>>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.edgeId,
        entityType: EntityType.SCHEDULER_EVENT
      }
    }).afterClosed().subscribe(res => {
      if (res) this.reloadSchedulerEvents();
    });
  }

  changeCalendarView(calendarView?: schedulerCalendarView, updateRouterQueryParams: boolean = true): void {
    if (calendarView) {
      this.currentCalendarView = calendarView;
    }
    this.currentCalendarViewValue = schedulerCalendarViewValueMap.get(this.currentCalendarView);
    this.calendarApi.changeView(this.currentCalendarViewValue);
    this.modeHandler.handleChangeCalendarView(updateRouterQueryParams);
  }

  private updateCalendarDate(): void {
    const today = this.isCalendarToday();
    this.modeHandler.handleUpdateCalendarDate(today ? null : this.calendarApi.view.currentStart.valueOf());
  }

  calendarViewTitle(): string {
    return this.calendarApi?.view.title ?? '';
  }

  gotoCalendarToday(): void {
    this.calendarApi.today();
    this.updateCalendarDate();
  }

  isCalendarToday(): boolean {
    return this.isDateInView(Date.now().valueOf())
  }

  gotoCalendarPrev(): void {
    this.calendarApi.prev();
    this.updateCalendarDate();
  }

  gotoCalendarNext(): void {
    this.calendarApi.next();
    this.updateCalendarDate();
  }

  gotoCalendarDate(date: number): void {
    if (!this.isDateInView(date) && this.calendarApi) {
      this.calendarApi.gotoDate(date);
    }
  }

  private isDateInView(targetDate: number): boolean {
    if (!this.calendarApi) return false;
    const view = this.calendarApi.view;
    return rangeContainsMarker({start: view.currentStart, end: view.currentEnd}, new Date(targetDate));
  }

  private onEventClick(arg: EventClickArg): void {
    const schedulerEvent = this.schedulerEvents.find(event => event.id.id === arg.event.id);
    if (schedulerEvent) {
      if (!schedulerEvent.typeName) {
        schedulerEvent.typeName = arg.event.extendedProps?.type;
      }
      this.openSchedulerEventContextMenu(arg.jsEvent, schedulerEvent);
    }
  }

  private openSchedulerEventContextMenu($event: MouseEvent, schedulerEvent: SchedulerEventWithCustomerInfo): void {
    $event.preventDefault();
    $event.stopPropagation();
    const $element = $(this.calendarContainer.nativeElement);
    const offset = $element.offset();
    const x = $event.pageX - offset.left;
    const y = $event.pageY - offset.top;
    this.schedulerContextMenuEvent = $event;
    this.schedulerEventMenuPosition.x = x + 'px';
    this.schedulerEventMenuPosition.y = y + 'px';
    this.schedulerEventMenuTrigger.menuData = {schedulerEvent};
    this.schedulerEventMenuTrigger.openMenu();
  }

  onSchedulerEventContextMenuMouseLeave(): void {
    this.schedulerEventMenuTrigger.closeMenu();
  }

  private onDayClick(event: DateClickArg): void {
    if (this.addEnabled) {
      const schedulerEvent = {
        schedule: { startTime: event.date.getTime() },
        configuration: {
          originatorId: null,
          msgType: null,
          msgBody: {},
          metadata: {}
        }
      } as SchedulerEvent;
      this.openSchedulerEventDialog(event.jsEvent, schedulerEvent);
    }
  }

  private onEventDrop(arg: EventDropArg): void {
    const schedulerEvent = this.schedulerEvents.find(event => event.id.id === arg.event.id);
    if (schedulerEvent) {
      this.moveEvent(schedulerEvent, arg.delta, arg.revert);
    }
  }

  private onEventDidMount(event: any): void {
    const props: { name: string; type: string; info: string; repeatInterval: string } = event.event.extendedProps;
    const element = $(event.el);
    import('tooltipster').then(() => {
      element.tooltipster({
        theme: 'tooltipster-shadow',
        delay: 100,
        trigger: 'hover',
        triggerOpen: { click: false, tap: false },
        triggerClose: { click: true, tap: true, scroll: true },
        side: 'top',
        trackOrigin: true
      });
      const tooltip = element.tooltipster('instance');
      tooltip.content($(
        `<div class="tb-scheduler-tooltip-title">${props.name}</div>` +
        `<div class="tb-scheduler-tooltip-content"><b>${this.translate.instant('scheduler.event-type')}:&nbsp;</b>${props.type}</div>` +
        `<div class="tb-scheduler-tooltip-content">${props.info}</div>`
      ));
    });
  }

  private moveEvent(event: SchedulerEventWithCustomerInfo, delta: Duration, revertFunc: () => void): void {
    this.schedulerEventService.getSchedulerEvent(event.id.id).subscribe({
      next: schedulerEvent => {
        schedulerEvent.schedule.startTime += asRoughMs(delta);
        this.schedulerEventService.saveSchedulerEvent(schedulerEvent).subscribe({
          next: () => this.reloadSchedulerEvents(),
          error: () => revertFunc()
        });
      },
      error: () => revertFunc()
    });
  }

  private eventSourceFunction(
    arg: { start: Date; end: Date; timeZone: string },
    successCallback: (events: EventInput[]) => void,
    failureCallback: (error: Error) => void
  ): void {
    const eventType = this.defaultEventType || '';
    const textSearch = this.pageLink.textSearch || '';

    this.schedulerEventService.getCalendarSchedulerEvents(
      eventType,
      arg.start.getTime(),
      arg.end.getTime(),
      textSearch,
      this.edgeId
    ).pipe(
      map(schedulerEvents => {
        this.schedulerEvents = schedulerEvents;
        const events: EventInput[] = [];
        if (this.schedulerEvents.length && this.calendarApi) {
          const start = toMoment(arg.start, this.calendarApi);
          const end = toMoment(arg.end, this.calendarApi);
          const rangeStart = start.local();
          const rangeEnd = end.local();
          this.schedulerEvents.forEach(event => {
            const eventStart = _moment(event.schedule.startTime);
            let calendarEvent: EventInput;
            if (rangeEnd.isSameOrAfter(eventStart)) {
              if (event.schedule.repeat) {
                const repeatEndsOn = _moment(event.schedule.repeat.endsOn);
                if (event.schedule.repeat.type === SchedulerRepeatType.TIMER && !event.timestamps?.length) {
                  calendarEvent = this.toCalendarEvent(event, eventStart, repeatEndsOn);
                  events.push(calendarEvent);
                } else {
                  event.timestamps.forEach(ts => {
                    calendarEvent = this.toCalendarEvent(event, _moment(ts));
                    events.push(calendarEvent);
                  });
                }
              } else if (rangeStart.isSameOrBefore(eventStart)) {
                calendarEvent = this.toCalendarEvent(event, eventStart);
                events.push(calendarEvent);
              }
            }
          });
        }
        return events;
      })
    ).subscribe({
      next: events => successCallback(events),
      error: error => failureCallback(error)
    });
  }

  private toCalendarEvent(event: SchedulerEventWithCustomerInfo, start: _moment.Moment, end?: _moment.Moment): EventInput {
    let typeName = event.type;
    let repeatInterval: string;
    if (this.schedulerEventConfigTypes[typeName]) {
      typeName = this.schedulerEventConfigTypes[typeName].name;
    }
    typeName = this.sanitizer.sanitize(1, typeName)
    const name = this.sanitizer.sanitize(1, event.name);
    const title = `${name} - ${typeName}`;
    if (event.schedule.repeat && event.schedule.repeat.type === SchedulerRepeatType.TIMER) {
      repeatInterval = this.translate.instant(schedulerTimeUnitRepeatTranslationMap.get(event.schedule.repeat.timeUnit),
        {count: event.schedule.repeat.repeatInterval});
    }
    return {
      id: event.id.id,
      title,
      name,
      type: typeName,
      info: this.eventInfo(event, start),
      start: start.toDate(),
      end: end ? end.toDate() : null,
      repeatInterval
    };
  }

  eventInfo(event: SchedulerEventWithCustomerInfo, startTime?: _moment.Moment): string {
    return scheduleInfo(event.schedule, this.translate, startTime);
  }

  unassignFromEdge($event: Event, schedulerEvent: SchedulerEventWithCustomerInfo): void {
    $event?.stopPropagation();
    const title = this.translate.instant('edge.unassign-scheduler-event-from-edge-title', {schedulerEventName: schedulerEvent.name});
    const content = this.translate.instant('edge.unassign-scheduler-event-from-edge-text');
    this.dialogService.confirm(title, content, this.translate.instant('action.no'), this.translate.instant('action.yes')).subscribe(result => {
      if (result) {
        this.schedulerEventService.unassignSchedulerEventFromEdge(this.edgeId, schedulerEvent.id.id).subscribe(() => this.reloadSchedulerEvents());
      }
    });
  }

  unassignFromEdgeSchedulerEvents($event: Event): void {
    $event?.stopPropagation();
    const selectedSchedulerEvents = this.dataSource.selection.selected;
    if (selectedSchedulerEvents?.length) {
      const title = this.translate.instant('edge.unassign-scheduler-events-from-edge-title', {count: selectedSchedulerEvents.length});
      const content = this.translate.instant('edge.unassign-scheduler-events-from-edge-text');
      this.dialogService.confirm(title, content, this.translate.instant('action.no'), this.translate.instant('action.yes')).subscribe(result => {
        if (result) {
          const tasks = selectedSchedulerEvents.map(event => this.schedulerEventService.unassignSchedulerEventFromEdge(this.edgeId, event.id.id));
          forkJoin(tasks).subscribe(() => this.reloadSchedulerEvents());
        }
      });
    }
  }

  isEnabled(schedulerEventWithCustomerInfo: SchedulerEventWithCustomerInfo): boolean {
    return isDefinedAndNotNull(schedulerEventWithCustomerInfo.enabled) ? schedulerEventWithCustomerInfo.enabled : true;
  }

  enableSchedulerEvent($event: Event, schedulerEvent: SchedulerEventWithCustomerInfo): void {
    $event?.stopPropagation();
    schedulerEvent.enabled = !this.isEnabled(schedulerEvent);
    this.schedulerEventService.updateSchedulerStatus(schedulerEvent.id.id, schedulerEvent.enabled, {ignoreLoading: true})
      .subscribe(() => this.cd.detectChanges());
  }

  public toggleVersionControl($event: Event, scheduled: SchedulerEventWithCustomerInfo, versionControlButton: MatIconButton): void {
    $event?.stopPropagation();
    const trigger = versionControlButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const versionControlPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: VersionControlComponent,
        preferredPlacement: ['left', 'leftTop', 'leftBottom'],
        context: {
          detailsMode: true,
          active: true,
          singleEntityMode: true,
          externalEntityId: scheduled.externalId || scheduled.id,
          entityId: scheduled.id,
          entityName: scheduled.name
        }
      });
      versionControlPopover.tbComponentRef.instance.popoverComponent = versionControlPopover;
      versionControlPopover.tbComponentRef.instance.versionRestored.subscribe(() => {
        versionControlPopover.hide();
        this.reloadSchedulerEvents();
      });
    }
  }
}

abstract class SchedulerModeHandler {
  protected component: SchedulerEventsComponent;
  protected router: Router;
  protected route: ActivatedRoute;

  protected _displayedColumns: string[];
  protected _defaultPageSize: number;
  protected _pageSizeOptions: Array<number>;
  protected _defaultSortOrder: string;
  protected _defaultEventType: string;
  protected _pageLink: PageLink;

  constructor(component: SchedulerEventsComponent, router: Router, route: ActivatedRoute) {
    this.component = component;
    this.router = router;
    this.route = route;
  }

  static create(component: SchedulerEventsComponent, router: Router, route: ActivatedRoute): SchedulerModeHandler {
    if (component.widgetMode) {
      return new WidgetSchedulerModeHandler(component, router, route);
    } else if (component.edgeId) {
      return new EdgeStandaloneSchedulerModeHandler(component, router, route);
    } else {
      return new StandaloneSchedulerModeHandler(component, router, route);
    }
  }

  setupSchedulerEventConfigTypes(): void  {
    const configTypes = deepClone(defaultSchedulerEventConfigTypes);
    if (!this.component.reportingEnabled) {
      // A report-producing scheduler event keeps generating reports once saved, so the server refuses to
      // save one while reporting is withheld. Hiding the types keeps the Scheduler from offering an option
      // that can only end in a refusal.
      reportSchedulerEventTypes.forEach((eventType) => delete configTypes[eventType]);
    }
    this.component.schedulerEventConfigTypes = configTypes;
  }

  abstract initialize(schedulerEventService: SchedulerEventService, userPermissionsService: UserPermissionsService,
                      customTranslatePipe: CustomTranslatePipe): void;

  abstract setupAfterViewInitSubscriptions(destroyRef: DestroyRef): void;

  abstract handleTextSearchChange(value: string): void;

  abstract handleSortOrPageChange(queryParams: PageQueryParam): void;

  abstract handleQueryParams(params: PageQueryParam): void;

  abstract updateRouterQueryParams(queryParams: object, queryParamsHandling: QueryParamsHandling): void;

  abstract handleUpdateMode(mode: SchedulerEventMode, updateRouterQueryParams: boolean): boolean;

  abstract handleChangeCalendarView(updateRouterQueryParams: boolean): void;

  abstract handleUpdateCalendarDate(startTs: number): void;
}

class WidgetSchedulerModeHandler extends SchedulerModeHandler {

  settings: SchedulerEventsWidgetSettings;

  setupSchedulerEventConfigTypes(): void {
    super.setupSchedulerEventConfigTypes();
    if (this.settings.customEventTypes?.length) {
      this.settings.customEventTypes.forEach((customEventType) => {
        this.component.schedulerEventConfigTypes[customEventType.value] = customEventType;
      });
    }
  }

  initialize(schedulerEventService: SchedulerEventService, _userPermissionsService: UserPermissionsService,
             customTranslatePipe: CustomTranslatePipe): void {
    this.component.ctx.$scope.schedulerEventsWidget = this.component;
    this.component.vcEnabled = false;
    if (this.component.showData) {
      this.settings = this.component.ctx.settings;
      this.initializeWidgetConfig(schedulerEventService, customTranslatePipe);
      this.component.ctx.updateWidgetParams();
    }
  }

  private initializeWidgetConfig(schedulerEventService: SchedulerEventService, customTranslatePipe: CustomTranslatePipe): void {
    this.component.ctx.widgetConfig.showTitle = false;
    this.component.ctx.widgetTitle = this.settings.title;
    const displayCreatedTime = isDefined(this.settings.displayCreatedTime) ? this.settings.displayCreatedTime : true;
    const displayType = isDefined(this.settings.displayType) ? this.settings.displayType : true;
    const displayCustomer = isDefined(this.settings.displayCustomer) ? this.settings.displayCustomer : true;

    this._displayedColumns = [];
    if (this.component.deleteEnabled) {
      this._displayedColumns.push('select');
    }
    if (displayCreatedTime) {
      this._displayedColumns.push('createdTime');
    }
    this._displayedColumns.push('name');
    if (displayType) {
      this._displayedColumns.push('type');
    }
    if (displayCustomer) {
      this._displayedColumns.push('customerTitle');
    }
    if (this.settings.displaySchedule ?? false) {
      this._displayedColumns.push('schedule');
    }
    this._displayedColumns.push('actions');
    this.component.displayedColumns = this._displayedColumns;

    this.component.displayPagination = this.settings.displayPagination ?? true;

    const pageSize = this.settings.defaultPageSize;
    let pageStepIncrement = isValidPageStepIncrement(this.settings.pageStepIncrement) ? this.settings.pageStepIncrement : null;
    let pageStepCount = isValidPageStepCount(this.settings.pageStepCount) ? this.settings.pageStepCount : null;

    if (Number.isInteger(pageSize) && pageSize > 0) {
      this._defaultPageSize = pageSize;
    }

    if (!this._defaultPageSize) {
      this._defaultPageSize = pageStepIncrement ?? 10;
    }
    this.component.defaultPageSize = this._defaultPageSize;

    if (!isDefinedAndNotNull(pageStepIncrement) || !isDefinedAndNotNull(pageStepCount)) {
      pageStepIncrement = this._defaultPageSize;
      pageStepCount = 3;
    }

    this._pageSizeOptions = [];
    for (let i = 1; i <= pageStepCount; i++) {
      this._pageSizeOptions.push(pageStepIncrement * i);
    }
    this.component.pageSizeOptions = this._pageSizeOptions;

    if (this.settings.defaultSortOrder && this.settings.defaultSortOrder.length) {
      this._defaultSortOrder = this.settings.defaultSortOrder;
    }
    this.component.defaultSortOrder = this._defaultSortOrder;

    const noDataDisplayMessage = this.settings.noDataDisplayMessage;
    if (isNotEmptyStr(noDataDisplayMessage)) {
      this.component.noDataDisplayMessageText = customTranslatePipe.transform(noDataDisplayMessage);
    }

    const sortOrder: SortOrder = sortOrderFromString(this._defaultSortOrder);
    if (sortOrder.property === 'customer') {
      sortOrder.property = 'customerTitle';
    }
    this._pageLink = new PageLink(this._defaultPageSize, 0, null, sortOrder);
    this.component.pageLink = this._pageLink;

    this.setupSchedulerEventConfigTypes();

    if (this.settings.forceDefaultEventType && this.settings.forceDefaultEventType.length) {
      this._defaultEventType = this.settings.forceDefaultEventType;
    }
    this.component.defaultEventType = this._defaultEventType;

    this.component.enabledViews = this.settings.enabledViews;
    if (this.settings.enabledViews !== 'both') {
      this.component.mode = this.settings.enabledViews;
    }

    this.component.ctx.widgetActions = [
      {
        name: 'scheduler.add-scheduler-event',
        show: this.component.addEnabled,
        icon: 'add',
        onAction: ($event) => this.component.addSchedulerEvent($event)
      },
      {
        name: 'action.search',
        show: true,
        icon: 'search',
        onAction: () => this.component.enterFilterMode()
      },
      {
        name: 'action.refresh',
        show: true,
        icon: 'refresh',
        onAction: () => this.component.reloadSchedulerEvents()
      }
    ];

    this.component.dataSource = new SchedulerEventsDatasource(schedulerEventService, this.component.schedulerEventConfigTypes);
    this.component.dataSource.selection.changed.subscribe(() => {
      const hideTitlePanel = !this.component.dataSource.selection.isEmpty() || this.component.textSearchMode;
      if (this.component.ctx.hideTitlePanel !== hideTitlePanel) {
        this.component.ctx.hideTitlePanel = hideTitlePanel;
        this.component.ctx.detectChanges(true);
      } else {
        this.component.ctx.detectChanges();
      }
    });
  }

  setupAfterViewInitSubscriptions(): void { }

  handleTextSearchChange(value: string): void {
    if (this.component.displayPagination) {
      this.component.paginator.pageIndex = 0;
    }
    this.component.pageLink.textSearch = value.trim();
    this.component.updateData();
  }

  handleSortOrPageChange(_queryParams: PageQueryParam): void {
    this.component.updateData();
  }

  handleQueryParams(_params: PageQueryParam): void { }

  updateRouterQueryParams(_queryParams: object, _queryParamsHandling: QueryParamsHandling = 'merge'): void { }

  handleUpdateMode(_mode: SchedulerEventMode, _updateRouterQueryParams: boolean): boolean {
    return false;
  }

  handleChangeCalendarView(_updateRouterQueryParams: boolean): void { }

  handleUpdateCalendarDate(): void { }
}

class StandaloneSchedulerModeHandler extends SchedulerModeHandler {

  initialize(schedulerEventService: SchedulerEventService, _userPermissionsService: UserPermissionsService): void {
    this._displayedColumns = ['createdTime', 'name', 'type', 'customerTitle', 'schedule', 'actions'];
    if (this.component.deleteEnabled) {
      this._displayedColumns.unshift('select');
    }
    this.component.displayedColumns = this._displayedColumns;

    const routerQueryParams: CalendarQueryParam = this.route.snapshot.queryParams;
    const sortOrder: SortOrder = {
      property: routerQueryParams?.property || this.component.defaultSortOrder,
      direction: routerQueryParams?.direction || Direction.DESC
    };
    this._defaultPageSize = 10;
    this.component.defaultPageSize = this._defaultPageSize;
    this._pageSizeOptions = [this._defaultPageSize, this._defaultPageSize * 2, this._defaultPageSize * 3];
    this.component.pageSizeOptions = this._pageSizeOptions;
    this._pageLink = new PageLink(this._defaultPageSize, 0, null, sortOrder);
    this.component.pageLink = this._pageLink;
    if (routerQueryParams.hasOwnProperty('page')) {
      this._pageLink.page = Number(routerQueryParams.page);
    }
    if (routerQueryParams.hasOwnProperty('pageSize')) {
      this._pageLink.pageSize = Number(routerQueryParams.pageSize);
    }
    if (routerQueryParams.hasOwnProperty('mode')) {
      this.component.mode = 'calendar';
    }
    const textSearchParam = routerQueryParams.textSearch;
    if (isNotEmptyStr(textSearchParam)) {
      this.component.textSearchMode = true;
      const decodedTextSearch = decodeURI(textSearchParam);
      this._pageLink.textSearch = decodedTextSearch.trim();
      this.component.textSearch.setValue(decodedTextSearch, {emitEvent: false});
    }
    if (this.component.mode === 'calendar') {
      if (routerQueryParams.hasOwnProperty('calendarView')) {
        this.component.currentCalendarView = schedulerCalendarView[routerQueryParams.calendarView];
        this.component.currentCalendarViewValue = schedulerCalendarViewValueMap.get(this.component.currentCalendarView);
      }
      if (routerQueryParams.hasOwnProperty('calendarStart')) {
        this.component.initialCalendarDate = new Date(+routerQueryParams.calendarStart);
      }
    }
    this.setupSchedulerEventConfigTypes();
    this.component.dataSource = new SchedulerEventsDatasource(schedulerEventService, this.component.schedulerEventConfigTypes);
  }

  setupAfterViewInitSubscriptions(destroyRef: DestroyRef): void {
    this.route.queryParams.pipe(
      skip(1),
      takeUntilDestroyed(destroyRef)
    ).subscribe((params: CalendarQueryParam) => {
      this.handleQueryParams(params);
    });
  }

  handleTextSearchChange(value: string): void {
    const queryParams: PageQueryParam = {
      textSearch: isNotEmptyStr(value) ? encodeURI(value) : null,
      page: null
    };
    this.updateRouterQueryParams(queryParams);
    this.component.pageLink.textSearch = value.trim();
  }

  handleSortOrPageChange(queryParams: PageQueryParam): void {
    this.updateRouterQueryParams(queryParams);
  }

  handleQueryParams(params: CalendarQueryParam): void {
    const newMode = params.mode ? 'calendar' : 'list';
    const modeChanged = newMode !== this.component.mode;
    if (modeChanged) {
      if (newMode === 'calendar') {
        const newView = params.calendarView ? schedulerCalendarView[params.calendarView] : schedulerCalendarView.month;
        this.component.currentCalendarView = newView;
        this.component.currentCalendarViewValue = schedulerCalendarViewValueMap.get(newView);
        this.component.initialCalendarDate = params.calendarStart ? new Date(+params.calendarStart) : null;
      }
      this.component.updateMode(newMode, false);
    }
    if (this.component.mode === 'calendar') {
      const newView = params.calendarView ? schedulerCalendarView[params.calendarView] : schedulerCalendarView.month;
      if (newView !== this.component.currentCalendarView) {
        this.component.changeCalendarView(newView, false);
      }
      this.component.gotoCalendarDate(params.calendarStart ? +params.calendarStart : Date.now().valueOf());
    }
    this.component.paginator.pageIndex = Number(params.page) || 0;
    this.component.paginator.pageSize = Number(params.pageSize) || this._defaultPageSize;
    this.component.sort.active = params.property || this.component.defaultSortOrder;
    this.component.sort.direction = (params.direction || Direction.DESC).toLowerCase() as SortDirection;
    const textSearchParam = params.textSearch;
    if (isNotEmptyStr(textSearchParam)) {
      this.component.textSearchMode = true;
      const decodedTextSearch = decodeURI(textSearchParam);
      this._pageLink.textSearch = decodedTextSearch.trim();
      this.component.textSearch.setValue(decodedTextSearch, {emitEvent: false});
    } else {
      this._pageLink.textSearch = null;
      this.component.textSearch.reset('', {emitEvent: false});
    }
    if (!modeChanged) {
      this.component.updateData();
    }
  }

  updateRouterQueryParams(queryParams: object, queryParamsHandling: QueryParamsHandling = 'merge'): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling
    }).then(() => {});
  }

  handleUpdateMode(mode: SchedulerEventMode, updateRouterQueryParams: boolean): boolean {
    if (updateRouterQueryParams) {
      const queryParams = { mode: mode === 'calendar' ? mode : null };
      this.updateRouterQueryParams(queryParams, 'replace');
      return true;
    }
    return false;
  }

  handleChangeCalendarView(updateRouterQueryParams: boolean): void {
    if (updateRouterQueryParams) {
      const queryParams = { calendarView: this.component.currentCalendarView !== schedulerCalendarView.month ? this.component.currentCalendarView : null };
      this.updateRouterQueryParams(queryParams);
    }
  }

  handleUpdateCalendarDate(startTs: number): void {
    const queryParams = { calendarStart: startTs };
    this.updateRouterQueryParams(queryParams);
  }
}

class EdgeStandaloneSchedulerModeHandler extends StandaloneSchedulerModeHandler {
  initialize(schedulerEventService: SchedulerEventService, userPermissionsService: UserPermissionsService) {
    super.initialize(schedulerEventService, userPermissionsService);
    const isEdgeWriteAllowed = userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE);
    this.component.assignEnabled = isEdgeWriteAllowed;
    if (isEdgeWriteAllowed && !this.component.deleteEnabled) {
      this.component.displayedColumns.unshift('select');
    } else if (!isEdgeWriteAllowed && this.component.deleteEnabled) {
      this.component.displayedColumns.shift();
    }
    this.component.deleteEnabled = false;
    this.component.addEnabled = false;
    this.component.editEnabled = false;
    this.component.vcEnabled = false;
  }
}

class SchedulerEventsDatasource implements DataSource<SchedulerEventWithCustomerInfo> {

  private entitiesSubject = new BehaviorSubject<SchedulerEventWithCustomerInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<SchedulerEventWithCustomerInfo>>(emptyPageData<SchedulerEventWithCustomerInfo>());

  public pageData$ = this.pageDataSubject.asObservable();
  public selection = new SelectionModel<SchedulerEventWithCustomerInfo>(true, []);
  public dataLoading = true;
  public edgeId: string;

  constructor(
    private schedulerEventService: SchedulerEventService,
    private schedulerEventConfigTypes: { [eventType: string]: SchedulerEventConfigType }
  ) {}

  connect(_collectionViewer: CollectionViewer): Observable<SchedulerEventWithCustomerInfo[] | ReadonlyArray<SchedulerEventWithCustomerInfo>> {
    return this.entitiesSubject.asObservable();
  }

  disconnect(_collectionViewer: CollectionViewer): void {
    this.entitiesSubject.complete();
    this.pageDataSubject.complete();
  }

  reset(): void {
    const pageData = emptyPageData<SchedulerEventWithCustomerInfo>();
    this.entitiesSubject.next(pageData.data);
    this.pageDataSubject.next(pageData);
  }

  loadEntities(pageLink: PageLink, eventType: string): void {
    this.dataLoading = true;
    this.getEntities(eventType, pageLink).pipe(
      tap(() => this.selection.clear()),
      catchError(() => of(emptyPageData<SchedulerEventWithCustomerInfo>()))
    ).subscribe(pageData => {
      this.entitiesSubject.next(pageData.data);
      this.pageDataSubject.next(pageData);
      this.dataLoading = false;
    });
  }

  getEntities(eventType: string, pageLink: PageLink): Observable<PageData<SchedulerEventWithCustomerInfo>> {
    return this.schedulerEventService.getSchedulerEventsByPageLink(eventType, pageLink, this.edgeId).pipe(
      map(schedulerEvents => {
        schedulerEvents.data.forEach(schedulerEvent => {
          let typeName = schedulerEvent.type;
          if (this.schedulerEventConfigTypes[typeName]) {
            typeName = this.schedulerEventConfigTypes[typeName].name;
          }
          schedulerEvent.typeName = typeName;
        });
        return schedulerEvents;
      }),
      shareReplay(1)
    );
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.entitiesSubject.pipe(
      map(entities => numSelected === entities.length),
      share()
    );
  }

  isEmpty(): Observable<boolean> {
    return this.entitiesSubject.pipe(
      map(entities => !entities.length),
      share()
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map(pageData => pageData.totalElements),
      share()
    );
  }

  masterToggle(): void {
    const entities = this.entitiesSubject.getValue();
    const numSelected = this.selection.selected.length;
    if (numSelected === entities.length) {
      this.selection.clear();
    } else {
      entities.forEach(row => this.selection.select(row));
    }
  }
}
