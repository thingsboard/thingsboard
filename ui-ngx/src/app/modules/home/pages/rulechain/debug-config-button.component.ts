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
  ChangeDetectorRef,
  EventEmitter,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DebugDurationLeftPipe } from '@home/pages/rulechain/debug-duration-left.pipe';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TbPopoverService } from '@shared/components/popover.service';
import { MatButton } from '@angular/material/button';
import { DebugConfigPanelComponent } from '@home/pages/rulechain/debug-config-panel.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { SECOND } from '@shared/models/time/time.models';
import { RuleNodeDebugConfig } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-debug-config-button',
  templateUrl: './debug-config-button.component.html',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    DebugDurationLeftPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DebugConfigButtonComponent {

  @Input() debugFailures = false;
  @Input() debugAll = false;
  @Input() debugAllUntil = 0;
  @Input() disabled = false;

  @Output() onDebugConfigChanged = new EventEmitter<RuleNodeDebugConfig>()

  constructor(protected store: Store<AppState>,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
              private cdr: ChangeDetectorRef
  ) {
    interval(SECOND)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cdr.markForCheck());
  }

  openDebugStrategyPanel($event: Event, matButton: MatButton): void {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const debugStrategyPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, DebugConfigPanelComponent, 'bottom', true, null,
        {
          debugFailures: this.debugFailures,
          debugAll: this.debugAll,
          debugAllUntil: this.debugAllUntil,
        },
        {},
        {}, {}, true);
      debugStrategyPopover.tbComponentRef.instance.popover = debugStrategyPopover;
      debugStrategyPopover.tbComponentRef.instance.onConfigApplied.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((config: RuleNodeDebugConfig) => {
        this.onDebugConfigChanged.emit(config);
        this.cdr.markForCheck();
        debugStrategyPopover.hide();
      });
    }
  }

  isDebugAllActive(): boolean {
    return this.debugAllUntil > new Date().getTime();
  }
}
