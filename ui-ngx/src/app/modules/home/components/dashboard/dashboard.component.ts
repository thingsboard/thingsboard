///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import {
  AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef,
  Component,
  DoCheck,
  Input,
  IterableDiffers,
  KeyValueDiffers,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Timewindow, toHistoryTimewindow } from '@shared/models/time/time.models';
import { TimeService } from '@core/services/time.service';
import { GridsterComponent, GridsterConfig, GridType } from 'angular-gridster2';
import {
  DashboardCallbacks,
  DashboardWidget,
  DashboardWidgets,
  IDashboardComponent
} from '../../models/dashboard-component.models';
import { ReplaySubject, Subject, Subscription } from 'rxjs';
import { WidgetLayout, WidgetLayouts } from '@shared/models/dashboard.models';
import { DialogService } from '@core/services/dialog.service';
import { animatedScroll, deepClone, isDefined } from '@app/core/utils';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { IAliasController, IStateController } from '@app/core/api/widget-api.models';
import { Widget, WidgetPosition } from '@app/shared/models/widget.models';
import { MatMenuTrigger } from '@angular/material/menu';
import { SafeStyle } from '@angular/platform-browser';
import { distinct } from 'rxjs/operators';
import { ResizeObserver } from '@juggle/resize-observer';
import { UtilsService } from '@core/services/utils.service';
import { WidgetComponentAction, WidgetComponentActionType } from '@home/components/widget/widget-container.component';

@Component({
  selector: 'tb-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent extends PageComponent implements IDashboardComponent, DoCheck, OnInit, OnDestroy, AfterViewInit, OnChanges {

  authUser: AuthUser;

  @Input()
  widgets: Iterable<Widget>;

  @Input()
  widgetLayouts: WidgetLayouts;

  @Input()
  callbacks: DashboardCallbacks;

  @Input()
  aliasController: IAliasController;

  @Input()
  stateController: IStateController;

  @Input()
  columns: number;

  @Input()
  margin: number;

  @Input()
  isEdit: boolean;

  @Input()
  autofillHeight: boolean;

  @Input()
  mobileAutofillHeight: boolean;

  @Input()
  mobileRowHeight: number;

  @Input()
  isMobile: boolean;

  @Input()
  isMobileDisabled: boolean;

  @Input()
  isEditActionEnabled: boolean;

  @Input()
  isExportActionEnabled: boolean;

  @Input()
  isRemoveActionEnabled: boolean;

  @Input()
  disableWidgetInteraction = false;

  @Input()
  dashboardStyle: {[klass: string]: any};

  @Input()
  backgroundImage: SafeStyle | string;

  @Input()
  dashboardClass: string;

  @Input()
  ignoreLoading = true;

  @Input()
  dashboardTimewindow: Timewindow;

  @Input()
  parentDashboard?: IDashboardComponent = null;

  dashboardTimewindowChangedSubject: Subject<Timewindow> = new ReplaySubject<Timewindow>();

  dashboardTimewindowChanged = this.dashboardTimewindowChangedSubject.asObservable().pipe(
    distinct()
  );

  originalDashboardTimewindow: Timewindow;

  gridsterOpts: GridsterConfig;

  isWidgetExpanded = false;
  isMobileSize = false;

  @ViewChild('gridster', {static: true}) gridster: GridsterComponent;

  @ViewChild('dashboardMenuTrigger', {static: true}) dashboardMenuTrigger: MatMenuTrigger;

  dashboardMenuPosition = { x: '0px', y: '0px' };

  dashboardContextMenuEvent: MouseEvent;

  @ViewChild('widgetMenuTrigger', {static: true}) widgetMenuTrigger: MatMenuTrigger;

  widgetMenuPosition = { x: '0px', y: '0px' };

  widgetContextMenuEvent: MouseEvent;

  dashboardWidgets = new DashboardWidgets(this,
    this.differs.find([]).create<Widget>((index, item) => {
      return item;
    }),
    this.kvDiffers.find([]).create<string, WidgetLayout>()
  );

  breakpointObserverSubscription: Subscription;

  private optionsChangeNotificationsPaused = false;

  private gridsterResize$: ResizeObserver;

  constructor(protected store: Store<AppState>,
              public utils: UtilsService,
              private timeService: TimeService,
              private dialogService: DialogService,
              private breakpointObserver: BreakpointObserver,
              private differs: IterableDiffers,
              private kvDiffers: KeyValueDiffers,
              private cd: ChangeDetectorRef,
              private ngZone: NgZone) {
    super(store);
    this.authUser = getCurrentAuthUser(store);
  }

  ngOnInit(): void {
    this.dashboardWidgets.parentDashboard = this.parentDashboard;
    if (!this.dashboardTimewindow) {
      this.dashboardTimewindow = this.timeService.defaultTimewindow();
    }
    this.gridsterOpts = {
      gridType: GridType.ScrollVertical,
      keepFixedHeightInMobile: true,
      disableWarnings: false,
      disableAutoPositionOnConflict: false,
      pushItems: false,
      swap: false,
      maxRows: 100,
      minCols: this.columns ? this.columns : 24,
      maxCols: 3000,
      maxItemCols: 1000,
      maxItemRows: 1000,
      maxItemArea: 1000000,
      outerMargin: true,
      margin: isDefined(this.margin) ? this.margin : 10,
      minItemCols: 1,
      minItemRows: 1,
      defaultItemCols: 8,
      defaultItemRows: 6,
      resizable: {enabled: this.isEdit},
      draggable: {enabled: this.isEdit},
      itemChangeCallback: item => this.dashboardWidgets.sortWidgets(),
      itemInitCallback: (item, itemComponent) => {
        (itemComponent.item as DashboardWidget).gridsterItemComponent = itemComponent;
      }
    };

    this.updateMobileOpts();

    this.breakpointObserverSubscription = this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm']).subscribe(
      () => {
        this.updateMobileOpts();
        this.notifyGridsterOptionsChanged();
      }
    );

    this.updateWidgets();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.gridsterResize$) {
      this.gridsterResize$.disconnect();
    }
    if (this.breakpointObserverSubscription) {
      this.breakpointObserverSubscription.unsubscribe();
    }
    this.dashboardTimewindowChangedSubject.complete();
    this.gridster = null;
  }

  ngDoCheck() {
    if (!this.optionsChangeNotificationsPaused) {
      this.dashboardWidgets.doCheck();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    let updateMobileOpts = false;
    let updateLayoutOpts = false;
    let updateEditingOpts = false;
    let updateWidgets = false;
    let updateDashboardTimewindow = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['isMobile', 'isMobileDisabled', 'autofillHeight', 'mobileAutofillHeight', 'mobileRowHeight'].includes(propName)) {
          updateMobileOpts = true;
        } else if (['margin', 'columns'].includes(propName)) {
          updateLayoutOpts = true;
        } else if (propName === 'isEdit') {
          updateEditingOpts = true;
        } else if (['widgets', 'widgetLayouts'].includes(propName)) {
          updateWidgets = true;
        } else if (propName === 'dashboardTimewindow') {
          updateDashboardTimewindow = true;
        }
      }
    }
    if (updateWidgets) {
      this.updateWidgets();
    } else if (updateDashboardTimewindow) {
      this.dashboardTimewindowChangedSubject.next(this.dashboardTimewindow);
    }

    if (updateMobileOpts) {
      this.updateMobileOpts();
    }
    if (updateLayoutOpts) {
      this.updateLayoutOpts();
    }
    if (updateEditingOpts) {
      this.updateEditingOpts();
    }
    if (updateMobileOpts || updateLayoutOpts || updateEditingOpts) {
      this.notifyGridsterOptionsChanged();
    }
  }

  private updateWidgets() {
    this.dashboardWidgets.setWidgets(this.widgets, this.widgetLayouts);
    this.dashboardWidgets.doCheck();
  }

  private updateWidgetLayouts() {
    this.dashboardWidgets.widgetLayoutsUpdated();
  }

  ngAfterViewInit(): void {
    this.gridsterResize$ = new ResizeObserver(() => {
      this.onGridsterParentResize();
    });
    this.gridsterResize$.observe(this.gridster.el);
  }

  onUpdateTimewindow(startTimeMs: number, endTimeMs: number, interval?: number, persist?: boolean): void {
    this.ngZone.run(() => {
      if (!this.originalDashboardTimewindow && !persist) {
        this.originalDashboardTimewindow = deepClone(this.dashboardTimewindow);
      }
      this.dashboardTimewindow = toHistoryTimewindow(this.dashboardTimewindow,
        startTimeMs, endTimeMs, interval, this.timeService);
      this.dashboardTimewindowChangedSubject.next(this.dashboardTimewindow);
    });
  }

  onResetTimewindow(): void {
    this.ngZone.run(() => {
      if (this.originalDashboardTimewindow) {
        this.dashboardTimewindow = deepClone(this.originalDashboardTimewindow);
        this.originalDashboardTimewindow = null;
        this.dashboardTimewindowChangedSubject.next(this.dashboardTimewindow);
      }
    });
  }

  isAutofillHeight(): boolean {
    if (this.isMobileSize) {
      return isDefined(this.mobileAutofillHeight) ? this.mobileAutofillHeight : false;
    } else {
      return isDefined(this.autofillHeight) ? this.autofillHeight : false;
    }
  }

  openDashboardContextMenu($event: MouseEvent) {
    if (this.callbacks && this.callbacks.prepareDashboardContextMenu) {
      const items = this.callbacks.prepareDashboardContextMenu($event);
      if (items && items.length) {
        $event.preventDefault();
        $event.stopPropagation();
        this.dashboardContextMenuEvent = $event;
        this.dashboardMenuPosition.x = $event.clientX + 'px';
        this.dashboardMenuPosition.y = $event.clientY + 'px';
        this.dashboardMenuTrigger.menuData = { items };
        this.dashboardMenuTrigger.openMenu();
      }
    }
  }

  private openWidgetContextMenu($event: MouseEvent, widget: DashboardWidget) {
    if (this.callbacks && this.callbacks.prepareWidgetContextMenu) {
      const items = this.callbacks.prepareWidgetContextMenu($event, widget.widget);
      if (items && items.length) {
        $event.preventDefault();
        $event.stopPropagation();
        this.widgetContextMenuEvent = $event;
        this.widgetMenuPosition.x = $event.clientX + 'px';
        this.widgetMenuPosition.y = $event.clientY + 'px';
        this.widgetMenuTrigger.menuData = { items, widget: widget.widget };
        this.widgetMenuTrigger.openMenu();
      }
    }
  }

  onWidgetFullscreenChanged(expanded: boolean) {
    this.isWidgetExpanded = expanded;
  }

  onWidgetComponentAction(action: WidgetComponentAction, widget: DashboardWidget) {
    const $event = action.event;
    switch (action.actionType) {
      case WidgetComponentActionType.MOUSE_DOWN:
        this.widgetMouseDown($event, widget);
        break;
      case WidgetComponentActionType.CLICKED:
        this.widgetClicked($event, widget);
        break;
      case WidgetComponentActionType.CONTEXT_MENU:
        this.openWidgetContextMenu($event, widget);
        break;
      case WidgetComponentActionType.EDIT:
        this.editWidget($event, widget);
        break;
      case WidgetComponentActionType.EXPORT:
        this.exportWidget($event, widget);
        break;
      case WidgetComponentActionType.REMOVE:
        this.removeWidget($event, widget);
        break;
    }
  }

  private widgetMouseDown($event: Event, widget: DashboardWidget) {
    if (this.callbacks && this.callbacks.onWidgetMouseDown) {
      this.callbacks.onWidgetMouseDown($event, widget.widget);
    }
  }

  private widgetClicked($event: Event, widget: DashboardWidget) {
    if (this.callbacks && this.callbacks.onWidgetClicked) {
      this.callbacks.onWidgetClicked($event, widget.widget);
    }
  }

  private editWidget($event: Event, widget: DashboardWidget) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isEditActionEnabled && this.callbacks && this.callbacks.onEditWidget) {
      this.callbacks.onEditWidget($event, widget.widget);
    }
  }

  private exportWidget($event: Event, widget: DashboardWidget) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isExportActionEnabled && this.callbacks && this.callbacks.onExportWidget) {
      this.callbacks.onExportWidget($event, widget.widget);
    }
  }

  private removeWidget($event: Event, widget: DashboardWidget) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.isRemoveActionEnabled && this.callbacks && this.callbacks.onRemoveWidget) {
      this.callbacks.onRemoveWidget($event, widget.widget);
    }
  }

  highlightWidget(widgetId: string, delay?: number) {
    const highlighted = this.dashboardWidgets.highlightWidget(widgetId);
    if (highlighted) {
      this.scrollToWidget(highlighted, delay);
    }
  }

  selectWidget(widgetId: string, delay?: number) {
    const selected = this.dashboardWidgets.selectWidget(widgetId);
    if (selected) {
      this.scrollToWidget(selected, delay);
    }
  }

  getSelectedWidget(): Widget {
    const dashboardWidget = this.dashboardWidgets.getSelectedWidget();
    return dashboardWidget ? dashboardWidget.widget : null;
  }

  getEventGridPosition(event: Event): WidgetPosition {
    const pos: WidgetPosition = {
      row: 0,
      column: 0
    };
    const parentElement = $(this.gridster.el);
    let pageX = 0;
    let pageY = 0;
    if (event instanceof MouseEvent) {
      pageX = event.pageX;
      pageY = event.pageY;
    }
    const offset = parentElement.offset();
    const x = pageX - offset.left + parentElement.scrollLeft();
    const y = pageY - offset.top + parentElement.scrollTop();
    pos.row = this.gridster.pixelsToPositionY(y, Math.floor);
    pos.column = this.gridster.pixelsToPositionX(x, Math.floor);
    return pos;
  }

  resetHighlight() {
    const highlighted = this.dashboardWidgets.resetHighlight();
    if (highlighted) {
      setTimeout(() => {
        this.scrollToWidget(highlighted, 0);
      }, 0);
    }
  }

  private scrollToWidget(widget: DashboardWidget, delay?: number) {
    const parentElement = this.gridster.el as HTMLElement;
    widget.gridsterItemComponent$().subscribe((gridsterItem) => {
      const gridsterItemElement = gridsterItem.el as HTMLElement;
      const offset = (parentElement.clientHeight - gridsterItemElement.clientHeight) / 2;
      let scrollTop;
      if (this.isMobileSize) {
        scrollTop = gridsterItemElement.offsetTop;
      } else {
        scrollTop = scrollTop = gridsterItem.top;
      }
      if (offset > 0) {
        scrollTop -= offset;
      }
      animatedScroll(parentElement, scrollTop, delay);
    });
  }

  private updateMobileOpts(parentHeight?: number) {
    this.isMobileSize = this.checkIsMobileSize();
    const autofillHeight = this.isAutofillHeight();
    if (autofillHeight) {
      this.gridsterOpts.gridType = this.isMobileSize ? GridType.Fixed : GridType.Fit;
    } else {
      this.gridsterOpts.gridType = this.isMobileSize ? GridType.Fixed : GridType.ScrollVertical;
    }
    const mobileBreakPoint = this.isMobileSize ? 20000 : 0;
    this.gridsterOpts.mobileBreakpoint = mobileBreakPoint;
    const rowSize = this.detectRowSize(this.isMobileSize, autofillHeight, parentHeight);
    if (this.gridsterOpts.fixedRowHeight !== rowSize) {
      this.gridsterOpts.fixedRowHeight = rowSize;
    }
  }

  private onGridsterParentResize() {
    const parentHeight = this.gridster.el.offsetHeight;
    if (this.isMobileSize && this.mobileAutofillHeight && parentHeight) {
      this.updateMobileOpts(parentHeight);
    }
    this.notifyGridsterOptionsChanged();
  }

  private updateLayoutOpts() {
    this.gridsterOpts.minCols = this.columns ? this.columns : 24;
    this.gridsterOpts.margin = isDefined(this.margin) ? this.margin : 10;
  }

  private updateEditingOpts() {
    this.gridsterOpts.resizable.enabled = this.isEdit;
    this.gridsterOpts.draggable.enabled = this.isEdit;
  }

  public notifyGridsterOptionsChanged() {
    if (!this.optionsChangeNotificationsPaused) {
      if (this.gridster && this.gridster.options) {
        this.gridster.optionsChanged();
      }
    }
  }

  public pauseChangeNotifications() {
    this.optionsChangeNotificationsPaused = true;
  }

  public resumeChangeNotifications() {
    this.optionsChangeNotificationsPaused = false;
  }

  public notifyLayoutUpdated() {
    this.updateWidgetLayouts();
  }

  private detectRowSize(isMobile: boolean, autofillHeight: boolean, parentHeight?: number): number | null {
    let rowHeight = null;
    if (!autofillHeight) {
      if (isMobile) {
        rowHeight = isDefined(this.mobileRowHeight) ? this.mobileRowHeight : 70;
      }
    } else if (autofillHeight && isMobile) {
      if (!parentHeight) {
        parentHeight = this.gridster.el.offsetHeight;
      }
      if (parentHeight) {
        let totalRows = 0;
        for (const widget of this.dashboardWidgets.dashboardWidgets) {
          totalRows += widget.rows;
        }
        rowHeight = (parentHeight - this.gridsterOpts.margin * (this.dashboardWidgets.dashboardWidgets.length + 2)) / totalRows;
      }
    }
    return rowHeight;
  }

  private checkIsMobileSize(): boolean {
    const isMobileDisabled = this.isMobileDisabled === true;
    let isMobileSize = this.isMobile === true && !isMobileDisabled;
    if (!isMobileSize && !isMobileDisabled) {
      isMobileSize = !this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
    }
    return isMobileSize;
  }

}
