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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { MatButton } from '@angular/material/button';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, of, shareReplay, timer } from 'rxjs';
import { SECOND, MINUTE } from '@shared/models/time/time.models';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { map, switchMap, takeWhile } from 'rxjs/operators';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
    selector: 'tb-entity-debug-settings-button',
    templateUrl: './entity-debug-settings-button.component.html',
    imports: [
        CommonModule,
        SharedModule,
        DurationLeftPipe,
    ],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityDebugSettingsButtonComponent),
            multi: true
        },
        EntityDebugSettingsService
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDebugSettingsButtonComponent implements ControlValueAccessor {

  @Input() additionalActionConfig: AdditionalDebugActionConfig;
  @Input({required: true}) entityType: EntityType;

  debugSettingsFormGroup = this.fb.group({
    failuresEnabled: [false],
    allEnabled: [false],
    allEnabledUntil: []
  });

  disabled = false;
  private allEnabledSubject = new BehaviorSubject(false);
  allEnabled$ = this.allEnabledSubject.asObservable();

  isDebugAllActive$ = this.allEnabled$.pipe(
    switchMap((value) => {
      if (value) {
        return of(true);
      } else {
        return timer(0, SECOND).pipe(
          map(() => this.allEnabledUntil > new Date().getTime()),
          takeWhile(value => value, true)
        );
      }
    }),
    takeUntilDestroyed(),
    shareReplay(1)
  );

  readonly maxDebugModeDuration = getCurrentAuthState(this.store).maxDebugModeDurationMinutes * MINUTE;

  private propagateChange: (settings: EntityDebugSettings) => void = () => {};

  constructor(private store: Store<AppState>,
              private fb: FormBuilder,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private cd : ChangeDetectorRef,
  ) {
    this.debugSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.propagateChange(value);
    });

    this.debugSettingsFormGroup.get('allEnabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => this.allEnabledSubject.next(value));
  }

  get failuresEnabled(): boolean {
    return this.debugSettingsFormGroup.get('failuresEnabled').value;
  }

  get allEnabledUntil(): number {
    return this.debugSettingsFormGroup.get('allEnabledUntil').value;
  }

  onOpenDebugStrategyPanel($event: Event, matButton: MatButton): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.entityDebugSettingsService.openDebugStrategyPanel({
      debugSettings: this.debugSettingsFormGroup.value,
      debugConfig: {
        maxDebugModeDuration: this.maxDebugModeDuration,
        entityType: this.entityType,
        additionalActionConfig: this.additionalActionConfig,
      },
      onSettingsAppliedFn: settings => {
        this.debugSettingsFormGroup.patchValue(settings);
        this.cd.markForCheck();
      }
    }, matButton._elementRef.nativeElement);
  }

  registerOnChange(fn: (settings: EntityDebugSettings) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: () => void): void {}

  writeValue(settings: EntityDebugSettings): void {
    this.debugSettingsFormGroup.patchValue(settings, {emitEvent: false});
    this.allEnabledSubject.next(settings?.allEnabled);
    this.debugSettingsFormGroup.get('allEnabled').updateValueAndValidity({onlySelf: true});
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.debugSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.debugSettingsFormGroup.enable({emitEvent: false});
    }
    this.cd.markForCheck();
  }
}
