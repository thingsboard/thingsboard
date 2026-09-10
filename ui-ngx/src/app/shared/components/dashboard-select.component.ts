// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  Component,
  ElementRef,
  forwardRef,
  Inject,
  Injector,
  Input,
  OnInit,
  StaticProvider,
  ViewChild,
  ViewContainerRef,
  DOCUMENT
} from '@angular/core';
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

import { WINDOW } from '@core/services/window.service';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  DASHBOARD_SELECT_PANEL_DATA,
  DashboardSelectPanelComponent
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
        }],
    standalone: false
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
              private nativeElement: ElementRef,
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
    if (!this.disabled) {
      const config = new OverlayConfig({
        panelClass: 'tb-dashboard-select-panel',
        backdropClass: 'cdk-overlay-transparent-backdrop',
        hasBackdrop: true
      });

      const connectedPosition: ConnectedPosition = {
        originX: 'start',
        originY: 'bottom',
        overlayX: 'start',
        overlayY: 'top'
      };

      config.positionStrategy = this.overlay.position().flexibleConnectedTo(this.nativeElement)
        .withPositions([connectedPosition]);
      const overlayRef = this.overlay.create(config);
      overlayRef.backdropClick().subscribe(() => {
        overlayRef.dispose();
      });

      const providers: StaticProvider[] = [
        {
          provide: DASHBOARD_SELECT_PANEL_DATA,
          useValue: {
            dashboards$: this.dashboards$,
            dashboardId: this.dashboardId,
            onDashboardSelected: (dashboardId) => {
              overlayRef.dispose();
              this.dashboardId = dashboardId;
              this.updateView();
            }
          }
        },
        {
          provide: OverlayRef,
          useValue: overlayRef
        }
      ];
      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      overlayRef.attach(new ComponentPortal(DashboardSelectPanelComponent, this.viewContainerRef, injector));
    }
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
