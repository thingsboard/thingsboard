// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import {
  NonConfirmedNotificationEscalation,
  NotificationTarget,
  NotificationType
} from '@shared/models/notification.models';
import { EntityType } from '@shared/models/entity-type.models';
import { takeUntil } from 'rxjs/operators';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatButton } from '@angular/material/button';

@Component({
    selector: 'tb-escalation-form',
    templateUrl: './escalation-form.component.html',
    styleUrls: ['./escalation-form.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EscalationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => EscalationFormComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class EscalationFormComponent implements ControlValueAccessor, OnInit, OnDestroy, Validator {

  @Input()
  disabled: boolean;

  @Input()
  systemEscalation = false;

  escalationFormGroup: FormGroup;

  entityType = EntityType;
  notificationType = NotificationType;

  private modelValue;
  private propagateChange = null;
  private propagateChangePending = false;
  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private dialog: MatDialog) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.propagateChange(this.modelValue);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.escalationFormGroup = this.fb.group(
      {
        delayInSec: [0],
        targets: [null, Validators.required],
      });
    this.escalationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateModel());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.escalationFormGroup.disable({emitEvent: false});
    } else {
      this.escalationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: NonConfirmedNotificationEscalation): void {
    this.propagateChangePending = false;
    this.modelValue = value;
    this.modelValue.delayInSec = +value.delayInSec;
    if (isDefinedAndNotNull(this.modelValue)) {
      this.escalationFormGroup.patchValue(this.modelValue, {emitEvent: false});
    }
    if (!this.disabled && !this.escalationFormGroup.valid) {
      this.updateModel();
    }
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
          let formValue: string[] = this.escalationFormGroup.get('targets').value;
          if (!formValue) {
            formValue = [];
          }
          formValue.push(res.id.id);
          this.escalationFormGroup.get('targets').patchValue(formValue);
        }
      });
  }

  public validate(c: FormControl) {
    return (this.escalationFormGroup.valid) ? null : {
      escalation: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value = this.escalationFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    if (this.propagateChange) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChangePending = true;
    }
  }
}
