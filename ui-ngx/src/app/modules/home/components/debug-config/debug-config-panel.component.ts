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
  EventEmitter,
  Input,
  OnInit
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { MINUTE, SECOND } from '@shared/models/time/time.models';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { shareReplay, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HasDebugConfig } from '@shared/models/entity.models';
import { distinctUntilChanged, map, tap } from 'rxjs/operators';

@Component({
  selector: 'tb-debug-config-panel',
  templateUrl: './debug-config-panel.component.html',
  standalone: true,
  imports: [
    SharedModule,
    CommonModule,
    DurationLeftPipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DebugConfigPanelComponent extends PageComponent implements OnInit {

  @Input() popover: TbPopoverComponent<DebugConfigPanelComponent>;
  @Input() debugFailures = false;
  @Input() debugAll = false;
  @Input() debugAllUntil = 0;
  @Input() maxDebugModeDurationMinutes: number;
  @Input() debugLimitsConfiguration: string;

  onFailuresControl = this.fb.control(false);
  debugAllControl = this.fb.control(false);

  maxMessagesCount: string;
  maxTimeFrameSec: string;

  isDebugAllActive$ = timer(0, SECOND).pipe(
    map(() => {
      this.cd.markForCheck();
      return this.debugAllUntil > new Date().getTime();
    }),
    distinctUntilChanged(),
    tap(isDebugOn => this.debugAllControl.patchValue(isDebugOn, { emitEvent: false })),
    shareReplay(1),
  );

  onConfigApplied = new EventEmitter<HasDebugConfig>();

  constructor(private fb: UntypedFormBuilder, private cd: ChangeDetectorRef) {
    super();

    this.observeDebugAllChange();
  }

  ngOnInit(): void {
    this.maxMessagesCount = this.debugLimitsConfiguration?.split(':')[0];
    this.maxTimeFrameSec = this.debugLimitsConfiguration?.split(':')[1];
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
    this.debugAllUntil = new Date().getTime() + this.maxDebugModeDurationMinutes * MINUTE;
  }

  private observeDebugAllChange(): void {
    this.debugAllControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(value => {
      this.debugAllUntil = value? new Date().getTime() + this.maxDebugModeDurationMinutes * MINUTE : 0;
      this.debugAll = value;
    });
  }
}
