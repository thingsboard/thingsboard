// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validators,
  NG_VALIDATORS,
  Validator,
  ValidationErrors
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { DAY, getDefaultTimezone, historyInterval } from '@shared/models/time/time.models';
import { DashboardReportConfig, dashboardReportTypeNamesMap, dashboardReportTypes } from '@shared/models/dashboard-report.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { UtilsService } from '@core/services/utils.service';
import { EntityType } from '@shared/models/entity-type.models';
import { MatDialog } from '@angular/material/dialog';
import {
  SelectDashboardStateDialogComponent,
  SelectDashboardStateDialogData
} from '@home/components/scheduler/config/select-dashboard-state-dialog.component';
import { PageComponent } from '@shared/components/page.component';
import { DashboardReportService } from '@core/http/dashboard-report.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { safeMerge } from '@home/components/scheduler/config/config.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-dashboard-report-config',
    templateUrl: './dashboard-report-config.component.html',
    styleUrls: ['./dashboard-report-config.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DashboardReportConfigComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DashboardReportConfigComponent),
            multi: true
        }],
    standalone: false
})
export class DashboardReportConfigComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy, Validator {

  private modelValue: DashboardReportConfig | null;

  reportConfigFormGroup: UntypedFormGroup;

  @Input()
  reportsServerEndpointUrl: string;

  @Input()
  @coerceBoolean()
  pdfReportMode = false;

  @Input()
  disabled: boolean;

  authUser = getCurrentAuthUser(this.store);

  isTenantAdmin = this.authUser.authority === Authority.TENANT_ADMIN;

  entityType = EntityType;

  reportTypesList = dashboardReportTypes;

  reportTypeNames = dashboardReportTypeNamesMap;

  private destroy$ = new Subject<void>();

  private propagateChange = (v: any) => { };

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private reportService: DashboardReportService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  selectDashboardState() {
    this.dialog.open<SelectDashboardStateDialogComponent, SelectDashboardStateDialogData, string>(SelectDashboardStateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
      data: {
        dashboardId: this.reportConfigFormGroup.get('dashboardId').value,
        state: this.reportConfigFormGroup.get('state').value
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res !== null) {
          this.reportConfigFormGroup.get('state').patchValue(res, {emitEvent: true});
        }
      }
    );
  }

  generateTestReport() {
    const progressText = this.translate.instant('dashboard.download-dashboard-progress', {reportType: this.modelValue.type});
    let config = this.modelValue;
    if (this.pdfReportMode) {
      config = {...config, userId: this.authUser.userId};
    }
    this.dialogService.progress(
      this.reportService.downloadTestReport(config, this.reportsServerEndpointUrl), progressText).subscribe();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.reportConfigFormGroup = this.fb.group({
      baseUrl: [null, this.pdfReportMode ? [] : [Validators.required]],
      dashboardId: [null, [Validators.required]],
      state: [null, []],
      timezone: [null, this.pdfReportMode ? [] : [Validators.required]],
      useDashboardTimewindow: [true, []],
      timewindow: [null, this.pdfReportMode ? [] : [Validators.required]]
    });

    if (!this.pdfReportMode) {
      this.reportConfigFormGroup.addControl('useCurrentUserCredentials', this.fb.control(true));
      this.reportConfigFormGroup.addControl('userId', this.fb.control(null, [Validators.required]));
      this.reportConfigFormGroup.addControl('namePattern', this.fb.control(null, [Validators.required]));
      this.reportConfigFormGroup.addControl('type', this.fb.control(null, [Validators.required]));
    }

    this.reportConfigFormGroup.get('useDashboardTimewindow').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateEnabledState();
    });

    if (!this.pdfReportMode) {
      this.reportConfigFormGroup.get('useCurrentUserCredentials').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe((useCurrentUserCredentials: boolean) => {
        if (useCurrentUserCredentials) {
          this.reportConfigFormGroup.get('userId').patchValue(this.authUser.userId, {emitEvent: false});
        } else {
          this.reportConfigFormGroup.get('userId').patchValue(null, {emitEvent: false});
        }
        this.updateEnabledState();
      });
    }

    this.reportConfigFormGroup.get('dashboardId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.reportConfigFormGroup.get('state').patchValue('', {emitEvent: false});
    });

    this.reportConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngAfterViewInit(): void {
    if (!this.reportConfigFormGroup.valid) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateEnabledState();
  }

  writeValue(value: DashboardReportConfig | null): void {
    this.modelValue = safeMerge<DashboardReportConfig>(this.createDefaultReportConfig(), value);
    this.reportConfigFormGroup.reset(this.modelValue, {emitEvent: false});
    this.updateEnabledState();
  }

  validate(): ValidationErrors | null {
    if (!this.reportConfigFormGroup.valid) {
      return {
        reportConfigForm: {
          valid: false
        }
      };
    }

    return null;
  }

  private updateEnabledState() {
    if (this.disabled) {
      this.reportConfigFormGroup.disable({emitEvent: false});
    } else {
      this.reportConfigFormGroup.enable({emitEvent: false});
      const useDashboardTimewindow: boolean = this.reportConfigFormGroup.get('useDashboardTimewindow').value;
      if (useDashboardTimewindow) {
        this.reportConfigFormGroup.get('timewindow').disable({emitEvent: false});
      } else {
        this.reportConfigFormGroup.get('timewindow').enable({emitEvent: false});
      }
      if (!this.pdfReportMode) {
        const useCurrentUserCredentials: boolean = this.reportConfigFormGroup.get('useCurrentUserCredentials').value;
        if (useCurrentUserCredentials) {
          this.reportConfigFormGroup.get('userId').disable({emitEvent: false});
        } else {
          this.reportConfigFormGroup.get('userId').enable({emitEvent: false});
        }
      }
    }
  }

  private createDefaultReportConfig(): Partial<DashboardReportConfig> {
    const config: Partial<DashboardReportConfig> =  {
      baseUrl: this.utils.baseUrl(),
      useDashboardTimewindow: true,
      timewindow: historyInterval(DAY),
      type: this.pdfReportMode ? 'png' : 'pdf',
      timezone: getDefaultTimezone(),
      dashboardId: null,
      state: ''
    };
    if (!this.pdfReportMode) {
      config.namePattern = 'report-%d{yyyy-MM-dd_HH:mm:ss}';
      config.useCurrentUserCredentials = true;
      config.userId = this.authUser.userId;
    }
    return config;
  }

  private updateModel() {
    const value = this.reportConfigFormGroup.getRawValue() as DashboardReportConfig;
    this.modelValue = {...this.modelValue, ...value};
    this.propagateChange(this.modelValue);
  }

}
