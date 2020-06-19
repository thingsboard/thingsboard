///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { map, mergeMap, startWith, tap } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { DashboardInfo } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { getCurrentAuthUser } from '@app/core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

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
export class DashboardAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  selectDashboardFormGroup: FormGroup;

  modelValue: DashboardInfo | string | null;

  @Input()
  useIdValue = true;

  @Input()
  selectFirstDashboard = false;

  @Input()
  placeholder: string;

  @Input()
  dashboardsScope: 'customer' | 'tenant';

  @Input()
  tenantId: string;

  @Input()
  customerId: string;

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

  @ViewChild('dashboardInput', {static: true}) dashboardInput: ElementRef;

  filteredDashboards: Observable<Array<DashboardInfo>>;

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private dashboardService: DashboardService,
              private fb: FormBuilder) {
    this.selectDashboardFormGroup = this.fb.group({
      dashboard: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredDashboards = this.selectDashboardFormGroup.get('dashboard').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = this.useIdValue ? value.id.id : value;
          }
          this.updateView(modelValue);
        }),
        startWith<string | DashboardInfo>(''),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchDashboards(name) )
      );
  }

  ngAfterViewInit(): void {
    // this.selectFirstDashboardIfNeeded();
  }

  selectFirstDashboardIfNeeded(): void {
    if (this.selectFirstDashboard && !this.modelValue) {
      this.getDashboards(new PageLink(1)).subscribe(
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
  }

  writeValue(value: DashboardInfo | string | null): void {
    this.searchText = '';
    if (value != null) {
      if (typeof value === 'string') {
        this.dashboardService.getDashboardInfo(value).subscribe(
          (dashboard) => {
            this.modelValue = this.useIdValue ? dashboard.id.id : dashboard;
            this.selectDashboardFormGroup.get('dashboard').patchValue(dashboard, {emitEvent: true});
          }
        );
      } else {
        this.modelValue = this.useIdValue ? value.id.id : value;
        this.selectDashboardFormGroup.get('dashboard').patchValue(value, {emitEvent: true});
      }
    } else {
      this.modelValue = null;
      this.selectDashboardFormGroup.get('dashboard').patchValue(null, {emitEvent: true});
      this.selectFirstDashboardIfNeeded();
    }
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
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'title',
      direction: Direction.ASC
    });
    return this.getDashboards(pageLink).pipe(
      map(pageData => {
        return pageData.data;
      })
    );
  }

  getDashboards(pageLink: PageLink): Observable<PageData<DashboardInfo>> {
    let dashboardsObservable: Observable<PageData<DashboardInfo>>;
    const authUser = getCurrentAuthUser(this.store);
    if (this.dashboardsScope === 'customer' || authUser.authority === Authority.CUSTOMER_USER) {
      if (this.customerId) {
        dashboardsObservable = this.dashboardService.getCustomerDashboards(this.customerId, pageLink,
          {ignoreLoading: true});
      } else {
        dashboardsObservable = of(emptyPageData());
      }
    } else {
      if (authUser.authority === Authority.SYS_ADMIN) {
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

  clear() {
    this.selectDashboardFormGroup.get('dashboard').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.dashboardInput.nativeElement.blur();
      this.dashboardInput.nativeElement.focus();
    }, 0);
  }

}
