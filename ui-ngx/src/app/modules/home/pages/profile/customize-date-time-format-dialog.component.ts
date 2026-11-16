// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  DateTimeFormatPickerState,
  dateOrders,
  dateOrderTranslations,
  dateSeparators,
  timeSeparators
} from '@shared/models/date-time-format.models';
import { DateTimeFormatService } from '@core/services/date-time-format.service';

@Component({
    selector: 'tb-customize-date-time-format-dialog',
    templateUrl: './customize-date-time-format-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class CustomizeDateTimeFormatDialogComponent
  extends DialogComponent<CustomizeDateTimeFormatDialogComponent, DateTimeFormatPickerState> {

  formGroup: UntypedFormGroup;

  readonly dateOrders = dateOrders;
  readonly dateOrderTranslations = dateOrderTranslations;
  readonly dateSeparators = dateSeparators;
  readonly timeSeparators = timeSeparators;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: DateTimeFormatPickerState,
              protected dialogRef: MatDialogRef<CustomizeDateTimeFormatDialogComponent, DateTimeFormatPickerState>,
              private fb: UntypedFormBuilder,
              private dateTimeFormatService: DateTimeFormatService) {
    super(store, router, dialogRef);
    const state = this.dateTimeFormatService.resolvePickerState(this.data);
    this.formGroup = this.fb.group({
      auto: [state.auto],
      dateOrder: [state.dateOrder],
      dateSeparator: [state.dateSeparator],
      timeClock: [state.timeClock],
      timeSeparator: [state.timeSeparator],
      showSeconds: [state.showSeconds],
      showMilliseconds: [state.showMilliseconds]
    });
    this.updateControlsEnabledState();
    this.formGroup.valueChanges.subscribe(() => this.updateControlsEnabledState());
  }

  private updateControlsEnabledState(): void {
    const auto = this.formGroup.get('auto').value;
    const order = this.formGroup.get('dateOrder').value;
    const showSeconds = this.formGroup.get('showSeconds').value;
    this.setControlEnabled('dateOrder', !auto);
    this.setControlEnabled('dateSeparator', !auto && order !== 'month');
    this.setControlEnabled('timeClock', !auto);
    this.setControlEnabled('timeSeparator', !auto);
    this.setControlEnabled('showMilliseconds', showSeconds);
    if (!showSeconds) {
      this.formGroup.get('showMilliseconds').setValue(false, { emitEvent: false });
    }
  }

  private setControlEnabled(name: string, enabled: boolean): void {
    const control = this.formGroup.get(name);
    if (enabled && control.disabled) {
      control.enable({emitEvent: false});
    } else if (!enabled && control.enabled) {
      control.disable({emitEvent: false});
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.formGroup.getRawValue());
  }

}
