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

import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ILayoutController } from '@home/components/dashboard-page/layout/layout.models';
import { DashboardContext, DashboardPageLayoutContext } from '@home/components/dashboard-page/dashboard-page.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Widget } from '@shared/models/widget.models';
import {
  DashboardCallbacks,
  DashboardContextMenuItem,
  IDashboardComponent,
  WidgetContextMenuItem
} from '@home/models/dashboard-component.models';
import { Observable, of, Subscription } from 'rxjs';
import { Hotkey } from 'angular2-hotkeys';
import { TranslateService } from '@ngx-translate/core';
import { ItemBufferService } from '@app/core/services/item-buffer.service';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { TbCheatSheetComponent } from '@shared/components/cheatsheet.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { map } from 'rxjs/operators';
import { displayGrids } from 'angular-gridster2/lib/gridsterConfig.interface';
import { BreakpointId, LayoutType, ViewFormatType } from '@shared/models/dashboard.models';
import { isNotEmptyStr } from '@core/utils';
import { TbContextMenuEvent } from '@shared/models/jquery-event.models';

@Component({
  selector: 'tb-dashboard-layout',
  templateUrl: './dashboard-layout.component.html',
  styleUrls: ['./dashboard-layout.component.scss']
})
export class DashboardLayoutComponent extends PageComponent implements ILayoutController, DashboardCallbacks, OnInit, OnDestroy {

  layoutCtxValue: DashboardPageLayoutContext;
  dashboardStyle: {[klass: string]: any} = null;
  backgroundImage$: Observable<SafeStyle | string>;

  hotKeys: Hotkey[] = [];

  @Input() dashboardCheatSheet: TbCheatSheetComponent;

  @Input()
  set layoutCtx(val: DashboardPageLayoutContext) {
    this.layoutCtxValue = val;
    if (this.layoutCtxValue) {
      this.layoutCtxValue.ctrl = this;
      if (this.dashboardStyle == null) {
        this.loadDashboardStyle();
      }
    }
  }
  get layoutCtx(): DashboardPageLayoutContext {
    return this.layoutCtxValue;
  }

  get isScada(): boolean {
    return this.layoutCtx.gridSettings.layoutType === LayoutType.scada;
  }

  get outerMargin(): boolean {
    return this.isScada ? false : this.layoutCtx.gridSettings.outerMargin;
  }

  get margin(): number {
    return this.isScada ? 0 : this.layoutCtx.gridSettings.margin;
  }

  get autoFillHeight(): boolean {
    return (this.isEdit || this.isScada) ? false : this.layoutCtx.gridSettings.autoFillHeight;
  }

  get mobileAutoFillHeight(): boolean {
    if (this.isEdit || this.isScada) {
      return false;
    } else if (this.layoutCtx.breakpoint !== 'default' && this.layoutCtx.gridSettings.viewFormat === ViewFormatType.list) {
      return this.layoutCtx.gridSettings.autoFillHeight;
    }
    return this.layoutCtx.gridSettings.mobileAutoFillHeight;
  }

  get isMobileValue(): boolean {
    return this.isMobile || (this.layoutCtx.breakpoint !== 'default' && this.layoutCtx.gridSettings.viewFormat === ViewFormatType.list);
  }

  get isMobileDisabled(): boolean {
    return this.widgetEditMode || this.isScada || (this.layoutCtx.breakpoint !== 'default' && !this.isMobileValue);
  }

  get mobielRowHeigth(): number {
    if (this.layoutCtx.breakpoint !== 'default' && this.layoutCtx.gridSettings.viewFormat === ViewFormatType.list) {
      return this.layoutCtx.gridSettings.rowHeight;
    }
    return this.layoutCtx.gridSettings.mobileRowHeight;
  }

  get columns(): number {
    return this.layoutCtx.gridSettings.minColumns || this.layoutCtx.gridSettings.columns || 24;
  }

  get displayGrid(): displayGrids {
    return this.layoutCtx.displayGrid || 'onDrag&Resize';
  }

  @Input()
  dashboardCtx: DashboardContext;

  @Input()
  isEdit: boolean;

  @Input()
  isEditingWidget: boolean;

  @Input()
  isMobile: boolean;

  @Input()
  widgetEditMode: boolean;

  @Input()
  parentDashboard?: IDashboardComponent = null;

  @Input()
  popoverComponent?: TbPopoverComponent = null;

  @ViewChild('dashboard', {static: true}) dashboard: IDashboardComponent;

  private rxSubscriptions = new Array<Subscription>();

  constructor(
    protected store: Store<AppState>,
    private translate: TranslateService,
    private itembuffer: ItemBufferService,
    private imagePipe: ImagePipe,
    private sanitizer: DomSanitizer,
  ) {
    super(store);
    this.initHotKeys();
  }

  ngOnInit(): void {
    const dashboardTimewindowChanged = this.parentDashboard ?
      this.parentDashboard.dashboardTimewindowChanged : this.dashboard.dashboardTimewindowChanged;
    this.rxSubscriptions.push(dashboardTimewindowChanged.subscribe(
      (dashboardTimewindow) => {
        this.dashboardCtx.dashboardTimewindow = dashboardTimewindow;
        this.dashboardCtx.runChangeDetection();
      })
    );
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  private initHotKeys(): void {
    this.hotKeys.push(
      new Hotkey(['ctrl+c', 'meta+c'], (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            const widget = this.dashboard.getSelectedWidget();
            if (widget) {
              event.preventDefault();
              this.copyWidget(event, widget);
            }
            return false;
          }
          return true;
        }, null,
        this.translate.instant('action.copy'))
    );
    this.hotKeys.push(
      new Hotkey(['ctrl+r', 'meta+r'], (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            const widget = this.dashboard.getSelectedWidget();
            if (widget) {
              event.preventDefault();
              this.copyWidgetReference(event, widget);
            }
            return false;
          }
          return true;
        }, null,
        this.translate.instant('action.copy-reference'))
    );
    this.hotKeys.push(
      new Hotkey(['ctrl+v', 'meta+v'], (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            if (this.itembuffer.hasWidget()) {
              event.preventDefault();
              this.pasteWidget(event);
            }
            return false;
          }
          return true;
        }, null,
        this.translate.instant('action.paste'))
    );
    this.hotKeys.push(
      new Hotkey(['ctrl+i', 'meta+i'], (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            if (this.itembuffer.canPasteWidgetReference(this.dashboardCtx.getDashboard(),
              this.dashboardCtx.state, this.layoutCtx.id, this.layoutCtx.breakpoint)) {
              event.preventDefault();
              this.pasteWidgetReference(event);
            }
            return false;
          }
          return true;
        }, null,
        this.translate.instant('action.paste-reference'))
    );
    this.hotKeys.push(
      new Hotkey(['ctrl+x', 'meta+x'], (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            const widget = this.dashboard.getSelectedWidget();
            if (widget) {
              event.preventDefault();
              this.layoutCtx.dashboardCtrl.removeWidget(event, this.layoutCtx, widget);
            }
            return false;
          }
          return true;
        }, null,
        this.translate.instant('action.delete'))
    );
  }

  private loadDashboardStyle() {
    this.dashboardStyle = {'background-color': this.layoutCtx.gridSettings.backgroundColor,
      'background-repeat': 'no-repeat',
      'background-attachment': 'scroll',
      'background-size': this.layoutCtx.gridSettings.backgroundSizeMode || '100%',
      'background-position': '0% 0%'};
    this.backgroundImage$ = this.layoutCtx.gridSettings.backgroundImageUrl ?
      this.imagePipe.transform(this.layoutCtx.gridSettings.backgroundImageUrl, {asString: true, ignoreLoadingImage: true}).pipe(
        map((imageUrl) => this.sanitizer.bypassSecurityTrustStyle('url(' + imageUrl + ')'))
      ) : of('none');
  }

  reload() {
    this.loadDashboardStyle();
    this.dashboard.pauseChangeNotifications();
    setTimeout(() => {
       this.dashboard.resumeChangeNotifications();
       this.dashboard.notifyLayoutUpdated();
    }, 0);
  }

  resetHighlight() {
    this.dashboard.resetHighlight();
  }

  highlightWidget(widgetId: string, delay?: number) {
    this.dashboard.highlightWidget(widgetId, delay);
  }

  selectWidget(widgetId: string, delay?: number) {
    this.dashboard.selectWidget(widgetId, delay);
  }

  addWidget($event: Event) {
    this.layoutCtx.dashboardCtrl.addWidget($event, this.layoutCtx);
  }

  onEditWidget($event: Event, widget: Widget): void {
    this.layoutCtx.dashboardCtrl.editWidget($event, this.layoutCtx, widget);
  }

  replaceReferenceWithWidgetCopy($event: Event, widget: Widget): void {
    this.layoutCtx.dashboardCtrl.replaceReferenceWithWidgetCopy($event, this.layoutCtx, widget);
  }

  onExportWidget($event: Event, widget: Widget, widgetTitle: string): void {
    this.layoutCtx.dashboardCtrl.exportWidget($event, this.layoutCtx, widget, widgetTitle);
  }

  onRemoveWidget($event: Event, widget: Widget): void {
    return this.layoutCtx.dashboardCtrl.removeWidget($event, this.layoutCtx, widget);
  }

  onWidgetMouseDown($event: Event, widget: Widget): void {
    this.layoutCtx.dashboardCtrl.widgetMouseDown($event, this.layoutCtx, widget);
  }

  onDashboardMouseDown($event: Event): void {
    this.layoutCtx.dashboardCtrl.dashboardMouseDown($event, this.layoutCtx);
  }

  onWidgetClicked($event: Event, widget: Widget): void {
    this.layoutCtx.dashboardCtrl.widgetClicked($event, this.layoutCtx, widget);
  }

  prepareDashboardContextMenu(_: Event): Array<DashboardContextMenuItem> {
    return this.layoutCtx.dashboardCtrl.prepareDashboardContextMenu(this.layoutCtx);
  }

  prepareWidgetContextMenu(_: Event, widget: Widget, isReference: boolean): Array<WidgetContextMenuItem> {
    return this.layoutCtx.dashboardCtrl.prepareWidgetContextMenu(this.layoutCtx, widget, isReference);
  }

  copyWidget($event: Event, widget: Widget) {
    this.layoutCtx.dashboardCtrl.copyWidget($event, this.layoutCtx, widget);
  }

  copyWidgetReference($event: Event, widget: Widget) {
    this.layoutCtx.dashboardCtrl.copyWidgetReference($event, this.layoutCtx, widget);
  }

  pasteWidget($event: TbContextMenuEvent | KeyboardEvent) {
    const pos = this.dashboard.getEventGridPosition($event);
    this.layoutCtx.dashboardCtrl.pasteWidget($event, this.layoutCtx, pos);
  }

  pasteWidgetReference($event: TbContextMenuEvent | KeyboardEvent) {
    const pos = this.dashboard.getEventGridPosition($event);
    this.layoutCtx.dashboardCtrl.pasteWidgetReference($event, this.layoutCtx, pos);
  }
}
