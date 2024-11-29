///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  Component,
  Input,
  Renderer2,
  ViewContainerRef,
  DestroyRef,
  ChangeDetectionStrategy,
  forwardRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { TbPopoverService } from '@shared/components/popover.service';
import { MatButton } from '@angular/material/button';
import { DebugSettingsPanelComponent } from './debug-settings-panel.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { shareReplay, timer } from 'rxjs';
import { SECOND } from '@shared/models/time/time.models';
import { DebugSettings } from '@shared/models/entity.models';
import { map } from 'rxjs/operators';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
} from '@angular/forms';

@Component({
  selector: 'tb-debug-settings-button',
  templateUrl: './debug-settings-button.component.html',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    DurationLeftPipe,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DebugSettingsButtonComponent),
      multi: true
    },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DebugSettingsButtonComponent implements ControlValueAccessor {

  @Input() debugLimitsConfiguration: string;

  debugSettingsFormGroup: UntypedFormGroup;
  disabled = false;
  isDebugAllActive$ = timer(0, SECOND).pipe(map(() => this.allEnabledUntil > new Date().getTime() || this.allEnabled), shareReplay(1));

  readonly maxDebugModeDurationMinutes = getCurrentAuthState(this.store).maxDebugModeDurationMinutes;

  private onChange: (settings: DebugSettings) => void;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private store: Store<AppState>,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
              private fb: FormBuilder,
  ) {
    this.debugSettingsFormGroup = this.fb.group({
      failuresEnabled: [false],
      allEnabled: [false],
      allEnabledUntil: []
    });

    this.debugSettingsFormGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      this.onChange(value);
    })
  }

  get failuresEnabled(): boolean {
    return this.debugSettingsFormGroup.get('failuresEnabled').value;
  }

  get allEnabled(): boolean {
    return this.debugSettingsFormGroup.get('allEnabled').value;
  }

  get allEnabledUntil(): number {
    return this.debugSettingsFormGroup.get('allEnabledUntil').value;
  }

  openDebugStrategyPanel($event: Event, matButton: MatButton): void {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    const debugSettings = this.debugSettingsFormGroup.value;

    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const debugStrategyPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, DebugSettingsPanelComponent, 'bottom', true, null,
        {
          ...debugSettings,
          maxDebugModeDurationMinutes: this.maxDebugModeDurationMinutes,
          debugLimitsConfiguration: this.debugLimitsConfiguration
        },
        {},
        {}, {}, true);
      debugStrategyPopover.tbComponentRef.instance.popover = debugStrategyPopover;
      debugStrategyPopover.tbComponentRef.instance.onConfigApplied.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((settings: DebugSettings) => {
        this.debugSettingsFormGroup.patchValue(settings);
        debugStrategyPopover.hide();
      });
    }
  }

  registerOnChange(fn: (settings: DebugSettings) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(_: () => void): void {}

  writeValue(settings: DebugSettings): void {
    this.debugSettingsFormGroup.patchValue(settings, {emitEvent: false});
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.debugSettingsFormGroup[isDisabled ? 'disable' : 'enable']({emitEvent: false});
  }
}
