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
  EventEmitter,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { TbPopoverService } from '@shared/components/popover.service';
import { MatButton } from '@angular/material/button';
import { DebugConfigPanelComponent } from './debug-config-panel.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { shareReplay, timer } from 'rxjs';
import { SECOND } from '@shared/models/time/time.models';
import { HasDebugConfig } from '@shared/models/entity.models';
import { map } from 'rxjs/operators';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';

@Component({
  selector: 'tb-debug-config-button',
  templateUrl: './debug-config-button.component.html',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    DurationLeftPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DebugConfigButtonComponent {

  @Input() debugFailures = false;
  @Input() debugAll = false;
  @Input() debugAllUntil = 0;
  @Input() disabled = false;
  @Input() debugLimitsConfiguration: string;

  @Output() onDebugConfigChanged = new EventEmitter<HasDebugConfig>();

  isDebugAllActive$ = timer(0, SECOND).pipe(map(() => this.debugAllUntil > new Date().getTime()), shareReplay(1));

  readonly maxDebugModeDurationMinutes = getCurrentAuthState(this.store).maxDebugModeDurationMinutes;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private store: Store<AppState>,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
  ) {}

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
          maxDebugModeDurationMinutes: this.maxDebugModeDurationMinutes,
          debugLimitsConfiguration: this.debugLimitsConfiguration
        },
        {},
        {}, {}, true);
      debugStrategyPopover.tbComponentRef.instance.popover = debugStrategyPopover;
      debugStrategyPopover.tbComponentRef.instance.onConfigApplied.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((config: HasDebugConfig) => {
        this.onDebugConfigChanged.emit(config);
        debugStrategyPopover.hide();
      });
    }
  }
}
