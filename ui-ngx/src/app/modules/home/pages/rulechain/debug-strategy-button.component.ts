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
  forwardRef,
  Renderer2,
  ViewContainerRef,
  DestroyRef,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, UntypedFormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DebugStrategy } from '@shared/models/rule-node.models';
import { DebugDurationLeftPipe } from '@home/pages/rulechain/debug-duration-left.pipe';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TbPopoverService } from '@shared/components/popover.service';
import { MatButton } from '@angular/material/button';
import { DebugStrategyPanelComponent } from '@home/pages/rulechain/debug-strategy-panel.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';

@Component({
  selector: 'tb-debug-strategy-button',
  templateUrl: './debug-strategy-button.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DebugStrategyButtonComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    DebugDurationLeftPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DebugStrategyButtonComponent implements ControlValueAccessor {

  @Input() disabled = false;
  @Input() lastUpdateTs: number;

  debugStrategyFormControl: FormControl<DebugStrategy>;

  private onChange: (debugStrategy: DebugStrategy) => void;

  readonly maxRuleNodeDebugDurationMinutes = getCurrentAuthState(this.store).maxRuleNodeDebugDurationMinutes;
  readonly DebugStrategy = DebugStrategy;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
              private cdr: ChangeDetectorRef
  ) {
    this.debugStrategyFormControl = this.fb.control(DebugStrategy.DISABLED);
    this.debugStrategyFormControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => this.onChange(value));
    interval(0.5 * MINUTE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cdr.markForCheck());
  }

  openDebugStrategyPanel($event: Event, matButton: MatButton): void {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    const debugStrategy = this.debugStrategyFormControl.value;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const debugStrategyPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, DebugStrategyPanelComponent, 'bottom', true, null,
        { debugStrategy },
        {},
        {}, {}, true);
      debugStrategyPopover.tbComponentRef.instance.popover = debugStrategyPopover;
      debugStrategyPopover.tbComponentRef.instance.onStrategyApplied.subscribe((strategy: DebugStrategy) => {
        this.debugStrategyFormControl.patchValue(strategy);
        this.cdr.markForCheck();
        debugStrategyPopover.hide();
      });
    }
  }

  registerOnChange(onChange: (debugStrategy: DebugStrategy) => void): void {
    this.onChange = onChange;
  }

  registerOnTouched(_: () => {}): void {}

  writeValue(value: DebugStrategy): void {
    this.debugStrategyFormControl.patchValue(value, { emitEvent: false });
    this.cdr.markForCheck();
  }
}
