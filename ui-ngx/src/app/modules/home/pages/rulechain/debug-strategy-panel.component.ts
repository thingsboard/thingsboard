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

import { Component, EventEmitter, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { DebugStrategy } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-debug-strategy-panel',
  templateUrl: './debug-strategy-panel.component.html',
  standalone: true,
  imports: [
    SharedModule,
    CommonModule
  ]
})
export class DebugStrategyPanelComponent extends PageComponent implements OnInit {

  @Input() popover: TbPopoverComponent<DebugStrategyPanelComponent>;
  @Input() debugStrategy: DebugStrategy;

  debugStrategyFormGroup: UntypedFormGroup;

  onStrategyApplied = new EventEmitter<DebugStrategy>()

  readonly maxRuleNodeDebugDurationMinutes = getCurrentAuthState(this.store).maxRuleNodeDebugDurationMinutes;
  readonly ruleChainDebugPerTenantLimitsConfiguration = getCurrentAuthState(this.store).ruleChainDebugPerTenantLimitsConfiguration;
  readonly maxMessagesCount = this.ruleChainDebugPerTenantLimitsConfiguration?.split(':')[0];
  readonly maxTimeFrameSec = this.ruleChainDebugPerTenantLimitsConfiguration?.split(':')[1];

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);

    this.debugStrategyFormGroup = this.fb.group({
      allMessages: [false],
      onFailure: [false]
    });
  }

  ngOnInit(): void {
    this.updatePanelStrategy();
  }

  onCancel() {
    this.popover?.hide();
  }

  onApply(): void {
    const allMessages = this.debugStrategyFormGroup.get('allMessages').value;
    const onFailure = this.debugStrategyFormGroup.get('onFailure').value;
    if (allMessages && onFailure) {
      this.onStrategyApplied.emit(DebugStrategy.ALL_THEN_ONLY_FAILURE_EVENTS);
    } else if (allMessages) {
      this.onStrategyApplied.emit(DebugStrategy.ALL_EVENTS);
    } else if (onFailure) {
      this.onStrategyApplied.emit(DebugStrategy.ONLY_FAILURE_EVENTS);
    } else {
      this.onStrategyApplied.emit(DebugStrategy.DISABLED);
    }
  }

  private updatePanelStrategy(): void {
    switch (this.debugStrategy) {
      case DebugStrategy.ALL_THEN_ONLY_FAILURE_EVENTS:
        this.debugStrategyFormGroup.get('allMessages').patchValue(true, { emitEvent: false });
        this.debugStrategyFormGroup.get('onFailure').patchValue(true, { emitEvent: false });
        break;
      case DebugStrategy.ONLY_FAILURE_EVENTS:
        this.debugStrategyFormGroup.get('onFailure').patchValue(true, { emitEvent: false });
        break;
      case DebugStrategy.ALL_EVENTS:
        this.debugStrategyFormGroup.get('allMessages').patchValue(true, { emitEvent: false });
        break;
    }
  }
}
