// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { getDefaultTimezone } from '@shared/models/time/time.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { PageComponent } from '@shared/components/page.component';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { safeMerge } from '@home/components/scheduler/config/config.models';
import { ReportConfig, ReportTemplateType } from '@shared/models/report.models';
import { UserId } from '@shared/models/id/user-id';
import { NotificationTarget, NotificationType } from '@shared/models/notification.models';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatButton } from '@angular/material/button';

@Component({
    selector: 'tb-report-config',
    templateUrl: './report-config.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ReportConfigComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => ReportConfigComponent),
            multi: true
        }],
    standalone: false
})
export class ReportConfigComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy, Validator {

  private modelValue: ReportConfig | null;

  reportConfigFormGroup: UntypedFormGroup;

  @Input()
  disabled: boolean;

  authUser = getCurrentAuthUser(this.store);

  showRecipients = this.authUser.authority !== Authority.CUSTOMER_USER;

  entityType = EntityType;

  ReportTemplateType = ReportTemplateType;

  NotificationType = NotificationType;

  private destroy$ = new Subject<void>();

  private propagateChange = (_v: any) => { };

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.reportConfigFormGroup = this.fb.group({
      reportTemplateId: [null, [Validators.required]],
      userId: [null, [Validators.required]],
      timezone: [null, [Validators.required]],
      targets: [null, []],
      notificationTemplateId: [null, []],
      makePublic: [false, null]
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

  writeValue(value: ReportConfig | null): void {
    this.modelValue = safeMerge<ReportConfig>(this.createDefaultReportConfig(), value);
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

  createTarget($event: Event, button: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    button._elementRef.nativeElement.blur();
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData,
      NotificationTarget>(RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
    .subscribe((res) => {
      if (res) {
        let formValue: string[] = this.reportConfigFormGroup.get('targets').value;
        if (!formValue) {
          formValue = [];
        }
        formValue.push(res.id.id);
        this.reportConfigFormGroup.get('targets').patchValue(formValue);
      }
    })
  }

  private updateEnabledState() {
    if (this.disabled) {
      this.reportConfigFormGroup.disable({emitEvent: false});
    } else {
      this.reportConfigFormGroup.enable({emitEvent: false});
    }
  }

  private createDefaultReportConfig(): ReportConfig {
    return {
      reportTemplateId: null,
      timezone: getDefaultTimezone(),
      userId: new UserId(this.authUser.userId),
      targets: [],
      notificationTemplateId: null,
      makePublic: false,
    };
  }

  private updateModel() {
    if (this.reportConfigFormGroup.valid) {
      const value = this.reportConfigFormGroup.getRawValue() as ReportConfig;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
