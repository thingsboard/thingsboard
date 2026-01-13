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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { DashboardInfo } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { getCurrentAuthUser } from '@app/core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { TranslateService } from '@ngx-translate/core';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { getEntityDetailsPageURL } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import { AuthUser } from '@shared/models/user.model';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';

@Component({
  selector: 'tb-dashboard-autocomplete',
  templateUrl: './dashboard-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DashboardAutocompleteComponent),
    multi: true
  }]
})
export class DashboardAutocompleteComponent implements ControlValueAccessor, OnInit {

  private dirty = false;

  selectDashboardFormGroup: UntypedFormGroup;

  modelValue: DashboardInfo | string | null;

  @Input()
  useIdValue = true;

  @Input()
  selectFirstDashboard = false;

  @Input()
  label = this.translate.instant('dashboard.dashboard');

  @Input()
  placeholder: string;

  @Input()
  dashboardsScope: 'customer' | 'tenant';

  @Input()
  tenantId: string;

  @Input()
  customerId: string;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @Input()
  requiredText: string;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @ViewChild('dashboardInput', {static: true}) dashboardInput: ElementRef;
  @ViewChild('dashboardInput', {read: MatAutocompleteTrigger, static: true}) dashboardAutocomplete: MatAutocompleteTrigger;

  filteredDashboards: Observable<Array<DashboardInfo>>;

  searchText = '';

  dashboardURL: string;

  useDashboardLink = true;

  private authUser: AuthUser;

  private propagateChange = (_v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private dashboardService: DashboardService,
              private fb: UntypedFormBuilder) {
    this.authUser = getCurrentAuthUser(this.store);
    if (this.authUser.authority === Authority.SYS_ADMIN) {
      this.useDashboardLink = false;
    }

    this.selectDashboardFormGroup = this.fb.group({
      dashboard: [null, this.required ? [Validators.required] : []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredDashboards = this.selectDashboardFormGroup.get('dashboard').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue: string | DashboardInfo;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = this.useIdValue ? value.id.id : value;
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        switchMap(name => this.fetchDashboards(name) ),
        share()
      );
  }

  selectFirstDashboardIfNeeded(): void {
    if (this.selectFirstDashboard && !this.modelValue) {
      this.getDashboards(new PageLink(1, 0, null, {
        property: 'title',
        direction: Direction.ASC
      })).subscribe(
        (data) => {
          if (data.data.length) {
            const dashboard = data.data[0];
            this.modelValue = this.useIdValue ? dashboard.id.id : dashboard;
            this.selectDashboardFormGroup.get('dashboard').patchValue(dashboard, {emitEvent: false});
            this.propagateChange(this.modelValue);
          }
        }
      );
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectDashboardFormGroup.disable({emitEvent: false});
      this.dashboardAutocomplete.closePanel();
    } else {
      this.selectDashboardFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DashboardInfo | string | null): void {
    this.searchText = '';
    if (value != null) {
      if (typeof value === 'string') {
        this.dashboardService.getDashboardInfo(value, {ignoreLoading: true, ignoreErrors: true}).subscribe({
          next: (dashboard) => {
            this.modelValue = this.useIdValue ? dashboard.id.id : dashboard;
            if (this.useDashboardLink) {
              this.dashboardURL = getEntityDetailsPageURL(this.modelValue as string, EntityType.DASHBOARD);
            }
            this.selectDashboardFormGroup.get('dashboard').patchValue(dashboard, {emitEvent: false});
          },
          error: () => {
            this.modelValue = null;
            this.selectDashboardFormGroup.get('dashboard').patchValue('', {emitEvent: false});
            if (this.required) {
              this.propagateChange(this.modelValue);
            }
          }
        });
      } else {
        this.modelValue = this.useIdValue ? value.id.id : value;
        this.selectDashboardFormGroup.get('dashboard').patchValue(value, {emitEvent: false});
      }
    } else {
      this.modelValue = null;
      this.selectDashboardFormGroup.get('dashboard').patchValue('', {emitEvent: false});
      this.selectFirstDashboardIfNeeded();
    }
    this.dirty = true;
  }

  updateView(value: DashboardInfo | string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayDashboardFn(dashboard?: DashboardInfo): string | undefined {
    return dashboard ? dashboard.title : undefined;
  }

  fetchDashboards(searchText?: string): Observable<Array<DashboardInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(25, 0, searchText, {
      property: 'title',
      direction: Direction.ASC
    });
    return this.getDashboards(pageLink).pipe(
      catchError(() => of(emptyPageData<DashboardInfo>())),
      map(pageData => pageData.data)
    );
  }

  getDashboards(pageLink: PageLink): Observable<PageData<DashboardInfo>> {
    let dashboardsObservable: Observable<PageData<DashboardInfo>>;
    if (this.dashboardsScope === 'customer' || this.authUser.authority === Authority.CUSTOMER_USER) {
      if (this.customerId) {
        dashboardsObservable = this.dashboardService.getCustomerDashboards(this.customerId, pageLink,
          {ignoreLoading: true});
      } else {
        dashboardsObservable = of(emptyPageData());
      }
    } else {
      if (this.authUser.authority === Authority.SYS_ADMIN) {
        if (this.tenantId) {
          dashboardsObservable = this.dashboardService.getTenantDashboardsByTenantId(this.tenantId, pageLink,
            {ignoreLoading: true});
        } else {
          dashboardsObservable = of(emptyPageData());
        }
      } else {
        dashboardsObservable = this.dashboardService.getTenantDashboards(pageLink,
          {ignoreLoading: true});
      }
    }
    return dashboardsObservable;
  }

  onFocus() {
    if (this.dirty) {
      this.selectDashboardFormGroup.get('dashboard').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear() {
    this.selectDashboardFormGroup.get('dashboard').patchValue('');
    setTimeout(() => {
      this.dashboardInput.nativeElement.blur();
      this.dashboardInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }
}
