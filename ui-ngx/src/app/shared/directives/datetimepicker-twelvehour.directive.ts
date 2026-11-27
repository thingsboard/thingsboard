// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DestroyRef, Directive, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDatetimepickerComponent } from '@mat-datetimepicker/core';
import { DateTimeFormatService } from '@core/services/date-time-format.service';

@Directive({
  selector: 'mat-datetimepicker:not([twelvehour])',
  standalone: false
})
export class DatetimepickerTwelveHourDirective implements OnInit {

  constructor(private picker: MatDatetimepickerComponent<any>,
              private dateTimeFormatService: DateTimeFormatService,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.applyClock();
    this.dateTimeFormatService.changed$.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.applyClock());
  }

  private applyClock(): void {
    this.picker.twelvehour = this.dateTimeFormatService.isTwelveHourFormat();
  }

}
