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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormControl, UntypedFormBuilder } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RuleNodeDebugConfig } from '@shared/models/rule-node.models';
import { MINUTE, SECOND } from '@shared/models/time/time.models';
import { DebugDurationLeftPipe } from '@home/pages/rulechain/debug-duration-left.pipe';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-debug-config-panel',
  templateUrl: './debug-config-panel.component.html',
  standalone: true,
  imports: [
    SharedModule,
    CommonModule,
    DebugDurationLeftPipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DebugConfigPanelComponent extends PageComponent implements OnInit {

  @Input() popover: TbPopoverComponent<DebugConfigPanelComponent>;
  @Input() debugFailures = false;
  @Input() debugAll = false;
  @Input() debugAllUntil = 0;

  onFailuresControl: FormControl<boolean>;
  debugAllControl: FormControl<boolean>;

  onConfigApplied = new EventEmitter<RuleNodeDebugConfig>()

  readonly maxRuleNodeDebugDurationMinutes = getCurrentAuthState(this.store).maxRuleNodeDebugDurationMinutes;
  readonly ruleChainDebugPerTenantLimitsConfiguration = getCurrentAuthState(this.store).ruleChainDebugPerTenantLimitsConfiguration;
  readonly maxMessagesCount = this.ruleChainDebugPerTenantLimitsConfiguration?.split(':')[0];
  readonly maxTimeFrameSec = this.ruleChainDebugPerTenantLimitsConfiguration?.split(':')[1];

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef,
              private cdr: ChangeDetectorRef,
              protected store: Store<AppState>) {
    super(store);

    this.onFailuresControl = this.fb.control(false);
    this.debugAllControl = this.fb.control(false);

    this.observeDebugAllChange();
  }

  ngOnInit(): void {
    this.debugAllControl.patchValue(this.isDebugAllOn(), { emitEvent: false });
    this.onFailuresControl.patchValue(this.debugFailures);
  }

  onCancel(): void {
    this.popover?.hide();
  }

  onApply(): void {
    this.onConfigApplied.emit({
      debugAll: this.debugAll,
      debugFailures: this.onFailuresControl.value,
      debugAllUntil: this.debugAllUntil
    });
  }

  onReset(): void {
    this.debugAll = true;
    this.debugAllUntil = new Date().getTime() + this.maxRuleNodeDebugDurationMinutes * MINUTE;
  }

  isDebugAllOn(): boolean {
    return this.debugAllUntil > new Date().getTime();
  }

  private observeDebugAllChange(): void {
    interval(SECOND)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.debugAllControl.patchValue(this.isDebugAllOn(), { emitEvent: false });
        this.cdr.markForCheck();
      });

    this.debugAllControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(value => {
      this.debugAllUntil = value? new Date().getTime() + this.maxRuleNodeDebugDurationMinutes * MINUTE : 0;
      this.debugAll = value;
      this.cdr.markForCheck();
    });
  }
}
