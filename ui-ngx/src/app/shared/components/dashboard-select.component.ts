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

import { Component, forwardRef, Inject, Input, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { map, share } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { DashboardInfo } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { getCurrentAuthUser } from '@app/core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TooltipPosition } from '@angular/material/tooltip';
import { CdkOverlayOrigin, ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { BreakpointObserver } from '@angular/cdk/layout';
import { DOCUMENT } from '@angular/common';
import { WINDOW } from '@core/services/window.service';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';
import {
  DASHBOARD_SELECT_PANEL_DATA,
  DashboardSelectPanelComponent,
  DashboardSelectPanelData
} from './dashboard-select-panel.component';
import { NULL_UUID } from '@shared/models/id/has-uuid';

// @dynamic
@Component({
  selector: 'tb-dashboard-select',
  templateUrl: './dashboard-select.component.html',
  styleUrls: ['./dashboard-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DashboardSelectComponent),
    multi: true
  }]
})
export class DashboardSelectComponent implements ControlValueAccessor, OnInit {

  @Input()
  dashboardsScope: 'customer' | 'tenant';

  @Input()
  customerId: string;

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  dashboards$: Observable<Array<DashboardInfo>>;

  dashboardId: string | null;

  @ViewChild('dashboardSelectPanelOrigin') dashboardSelectPanelOrigin: CdkOverlayOrigin;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private dashboardService: DashboardService,
              private overlay: Overlay,
              private breakpointObserver: BreakpointObserver,
              private viewContainerRef: ViewContainerRef,
              @Inject(DOCUMENT) private document: Document,
              @Inject(WINDOW) private window: Window) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    const pageLink = new PageLink(100);

    this.dashboards$ = this.getDashboards(pageLink).pipe(
      map((pageData) => pageData.data),
      share()
    );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string | null): void {
    this.dashboardId = value;
  }

  dashboardIdChanged() {
    this.updateView();
  }

  openDashboardSelectPanel() {
    if (this.disabled) {
      return;
    }
    const panelHeight = this.breakpointObserver.isMatched('min-height: 350px') ? 250 : 150;
    const panelWidth = 300;
    const position = this.overlay.position();
    const config = new OverlayConfig({
      panelClass: 'tb-dashboard-select-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
    });
    const el = this.dashboardSelectPanelOrigin.elementRef.nativeElement;
    const offset = el.getBoundingClientRect();
    const scrollTop = this.window.pageYOffset || this.document.documentElement.scrollTop || this.document.body.scrollTop || 0;
    const scrollLeft = this.window.pageXOffset || this.document.documentElement.scrollLeft || this.document.body.scrollLeft || 0;
    const bottomY = offset.bottom - scrollTop;
    const leftX = offset.left - scrollLeft;
    let originX;
    let originY;
    let overlayX;
    let overlayY;
    const wHeight = this.document.documentElement.clientHeight;
    const wWidth = this.document.documentElement.clientWidth;
    if (bottomY + panelHeight > wHeight) {
      originY = 'top';
      overlayY = 'bottom';
    } else {
      originY = 'bottom';
      overlayY = 'top';
    }
    if (leftX + panelWidth > wWidth) {
      originX = 'end';
      overlayX = 'end';
    } else {
      originX = 'start';
      overlayX = 'start';
    }
    const connectedPosition: ConnectedPosition = {
      originX,
      originY,
      overlayX,
      overlayY
    };
    config.positionStrategy = position.flexibleConnectedTo(this.dashboardSelectPanelOrigin.elementRef)
      .withPositions([connectedPosition]);
    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });

    const injector = this._createDashboardSelectPanelInjector(
      overlayRef,
      {
        dashboards$: this.dashboards$,
        dashboardId: this.dashboardId,
        onDashboardSelected: (dashboardId) => {
          overlayRef.dispose();
          this.dashboardId = dashboardId;
          this.updateView();
        }
      }
    );
    overlayRef.attach(new ComponentPortal(DashboardSelectPanelComponent, this.viewContainerRef, injector));
  }

  private _createDashboardSelectPanelInjector(overlayRef: OverlayRef, data: DashboardSelectPanelData): PortalInjector {
    const injectionTokens = new WeakMap<any, any>([
      [DASHBOARD_SELECT_PANEL_DATA, data],
      [OverlayRef, overlayRef]
    ]);
    return new PortalInjector(this.viewContainerRef.injector, injectionTokens);
  }

  private updateView() {
    this.propagateChange(this.dashboardId);
  }

  private getDashboards(pageLink: PageLink): Observable<PageData<DashboardInfo>> {
    let dashboardsObservable: Observable<PageData<DashboardInfo>>;
    const authUser = getCurrentAuthUser(this.store);
    if (this.dashboardsScope === 'customer' || authUser.authority === Authority.CUSTOMER_USER) {
      if (this.customerId && this.customerId !== NULL_UUID) {
        dashboardsObservable = this.dashboardService.getCustomerDashboards(this.customerId, pageLink,
          {ignoreLoading: true});
      } else {
        dashboardsObservable = of(emptyPageData());
      }
    } else {
      dashboardsObservable = this.dashboardService.getTenantDashboards(pageLink, {ignoreLoading: true});
    }
    return dashboardsObservable;
  }

}
