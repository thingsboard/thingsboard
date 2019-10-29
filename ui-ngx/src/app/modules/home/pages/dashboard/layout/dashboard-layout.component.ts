///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Component, OnDestroy, OnInit, Input, ChangeDetectorRef, ViewChild } from '@angular/core';
import { StateControllerComponent } from '@home/pages/dashboard/states/state-controller.component';
import { ILayoutController } from '@home/pages/dashboard/layout/layout.models';
import { DashboardContext, DashboardPageLayoutContext } from '@home/pages/dashboard/dashboard-page.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Widget } from '@shared/models/widget.models';
import { WidgetLayout, WidgetLayouts } from '@shared/models/dashboard.models';
import { GridsterComponent } from 'angular-gridster2';
import {
  DashboardCallbacks,
  DashboardContextMenuItem,
  IDashboardComponent, WidgetContextMenuItem
} from '@home/models/dashboard-component.models';
import { Observable, of, Subscription } from 'rxjs';
import { Hotkey, HotkeysService } from 'angular2-hotkeys';
import { getCurrentIsLoading } from '@core/interceptors/load.selectors';
import { TranslateService } from '@ngx-translate/core';
import { ItemBufferService } from '@app/core/services/item-buffer.service';

@Component({
  selector: 'tb-dashboard-layout',
  templateUrl: './dashboard-layout.component.html',
  styleUrls: ['./dashboard-layout.component.scss']
})
export class DashboardLayoutComponent extends PageComponent implements ILayoutController, DashboardCallbacks, OnInit, OnDestroy {

  layoutCtxValue: DashboardPageLayoutContext;

  @Input()
  set layoutCtx(val: DashboardPageLayoutContext) {
    this.layoutCtxValue = val;
    if (this.layoutCtxValue) {
      this.layoutCtxValue.ctrl = this;
    }
  }
  get layoutCtx(): DashboardPageLayoutContext {
    return this.layoutCtxValue;
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

  @ViewChild('dashboard', {static: true}) dashboard: IDashboardComponent;

  private rxSubscriptions = new Array<Subscription>();

  constructor(protected store: Store<AppState>,
              private hotkeysService: HotkeysService,
              private translate: TranslateService,
              private itembuffer: ItemBufferService) {
    super(store);
  }

  ngOnInit(): void {
    this.rxSubscriptions.push(this.dashboard.dashboardTimewindowChanged.subscribe(
      (dashboardTimewindow) => {
        this.dashboardCtx.dashboardTimewindow = dashboardTimewindow;
        this.dashboardCtx.runChangeDetection();
      })
    );
    this.initHotKeys();
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  private initHotKeys(): void {
    this.hotkeysService.add(
      new Hotkey('ctrl+c', (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            const widget = this.dashboard.getSelectedWidget();
            if (widget) {
              event.preventDefault();
              this.copyWidget(event, widget);
            }
          }
          return false;
        }, null,
        this.translate.instant('action.copy'))
    );
    this.hotkeysService.add(
      new Hotkey('ctrl+r', (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            const widget = this.dashboard.getSelectedWidget();
            if (widget) {
              event.preventDefault();
              this.copyWidgetReference(event, widget);
            }
          }
          return false;
        }, null,
        this.translate.instant('action.copy-reference'))
    );
    this.hotkeysService.add(
      new Hotkey('ctrl+v', (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            if (this.itembuffer.hasWidget()) {
              event.preventDefault();
              this.pasteWidget(event);
            }
          }
          return false;
        }, null,
        this.translate.instant('action.paste'))
    );
    this.hotkeysService.add(
      new Hotkey('ctrl+i', (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            if (this.itembuffer.canPasteWidgetReference(this.dashboardCtx.dashboard,
              this.dashboardCtx.state, this.layoutCtx.id)) {
              event.preventDefault();
              this.pasteWidgetReference(event);
            }
          }
          return false;
        }, null,
        this.translate.instant('action.paste-reference'))
    );
    this.hotkeysService.add(
      new Hotkey('ctrl+x', (event: KeyboardEvent) => {
          if (this.isEdit && !this.isEditingWidget && !this.widgetEditMode) {
            const widget = this.dashboard.getSelectedWidget();
            if (widget) {
              event.preventDefault();
              this.layoutCtx.dashboardCtrl.removeWidget(event, this.layoutCtx, widget);
            }
          }
          return false;
        }, null,
        this.translate.instant('action.delete'))
    );
  }

  reload() {
  }

  setResizing(layoutVisibilityChanged: boolean) {
  }

  resetHighlight() {
    this.dashboard.resetHighlight();
  }

  highlightWidget(index: number, delay?: number) {
    this.dashboard.highlightWidget(index, delay);
  }

  selectWidget(index: number, delay?: number) {
    this.dashboard.selectWidget(index, delay);
  }

  addWidget($event: Event) {
    this.layoutCtx.dashboardCtrl.addWidget($event, this.layoutCtx);
  }

  onEditWidget($event: Event, widget: Widget, index: number): void {
    this.layoutCtx.dashboardCtrl.editWidget($event, this.layoutCtx, widget, index);
  }

  onExportWidget($event: Event, widget: Widget, index: number): void {
    this.layoutCtx.dashboardCtrl.exportWidget($event, this.layoutCtx, widget, index);
  }

  onRemoveWidget($event: Event, widget: Widget, index: number): void {
    return this.layoutCtx.dashboardCtrl.removeWidget($event, this.layoutCtx, widget);
  }

  onWidgetMouseDown($event: Event, widget: Widget, index: number): void {
    this.layoutCtx.dashboardCtrl.widgetMouseDown($event, this.layoutCtx, widget, index);
  }

  onWidgetClicked($event: Event, widget: Widget, index: number): void {
    this.layoutCtx.dashboardCtrl.widgetClicked($event, this.layoutCtx, widget, index);
  }

  prepareDashboardContextMenu($event: Event): Array<DashboardContextMenuItem> {
    return this.layoutCtx.dashboardCtrl.prepareDashboardContextMenu(this.layoutCtx);
  }

  prepareWidgetContextMenu($event: Event, widget: Widget, index: number): Array<WidgetContextMenuItem> {
    return this.layoutCtx.dashboardCtrl.prepareWidgetContextMenu(this.layoutCtx, widget, index);
  }

  copyWidget($event: Event, widget: Widget) {
    this.layoutCtx.dashboardCtrl.copyWidget($event, this.layoutCtx, widget);
  }

  copyWidgetReference($event: Event, widget: Widget) {
    this.layoutCtx.dashboardCtrl.copyWidgetReference($event, this.layoutCtx, widget);
  }

  pasteWidget($event: Event) {
    const pos = this.dashboard.getEventGridPosition($event);
    this.layoutCtx.dashboardCtrl.pasteWidget($event, this.layoutCtx, pos);
  }

  pasteWidgetReference($event: Event) {
    const pos = this.dashboard.getEventGridPosition($event);
    this.layoutCtx.dashboardCtrl.pasteWidgetReference($event, this.layoutCtx, pos);
  }

}
