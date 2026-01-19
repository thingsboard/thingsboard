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
  booleanAttribute,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { MINUTE, SECOND } from '@shared/models/time/time.models';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { of, shareReplay, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { distinctUntilChanged, map, startWith, switchMap, takeWhile } from 'rxjs/operators';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-entity-debug-settings-panel',
  templateUrl: './entity-debug-settings-panel.component.html',
  standalone: true,
  imports: [
    SharedModule,
    CommonModule,
    DurationLeftPipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDebugSettingsPanelComponent extends PageComponent implements OnInit {

  @Input({ transform: booleanAttribute }) failuresEnabled = false;
  @Input({ transform: booleanAttribute }) allEnabled = false;
  @Input() entityLabel = '';
  @Input() entityType: EntityType;
  @Input() allEnabledUntil = 0;
  @Input() maxDebugModeDuration = getCurrentAuthState(this.store).maxDebugModeDurationMinutes * MINUTE;
  @Input() additionalActionConfig: AdditionalDebugActionConfig;

  onFailuresControl = this.fb.control(false);
  debugAllControl = this.fb.control(false);

  maxMessagesCount: string;
  maxTimeFrameDuration: number;
  initialAllEnabled: boolean;

  isDebugAllActive$ = this.debugAllControl.valueChanges.pipe(
    startWith(this.debugAllControl.value),
    switchMap(value => {
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
    shareReplay(1),
  );

  onSettingsApplied = new EventEmitter<EntityDebugSettings>();

  constructor(private fb: FormBuilder,
              private cd: ChangeDetectorRef,
              private popover: TbPopoverComponent<EntityDebugSettingsPanelComponent>) {
    super();

    this.debugAllControl.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.allEnabled = value;
      this.cd.markForCheck();
    });

    this.isDebugAllActive$.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(isDebugOn => this.debugAllControl.patchValue(isDebugOn, {emitEvent: false}))
  }

  ngOnInit(): void {
    const debugLimitsConfiguration = this.getByEntityTypeDebugLimit(this.entityType);
    this.maxMessagesCount = debugLimitsConfiguration?.split(':')[0];
    this.maxTimeFrameDuration = parseInt(debugLimitsConfiguration?.split(':')[1]) * SECOND;
    this.onFailuresControl.patchValue(this.failuresEnabled);
    this.debugAllControl.patchValue(this.allEnabled);
    this.initialAllEnabled = this.allEnabled || this.allEnabledUntil > new Date().getTime();
    if (!this.entityLabel) {
      this.entityLabel = entityTypeTranslations.has(this.entityType) ? entityTypeTranslations.get(this.entityType).type : 'debug-settings.entity';
    }
  }

  onCancel(): void {
    this.popover.hide();
  }

  onApply(): void {
    const isDebugAllChanged = this.initialAllEnabled !== this.debugAllControl.value || this.initialAllEnabled !== this.allEnabledUntil > new Date().getTime();
    if (isDebugAllChanged) {
      this.onSettingsApplied.emit({
        allEnabled: this.allEnabled,
        failuresEnabled: this.onFailuresControl.value,
        allEnabledUntil: 0,
      });
    } else {
      this.onSettingsApplied.emit({
        allEnabled: false,
        failuresEnabled: this.onFailuresControl.value,
        allEnabledUntil: this.allEnabledUntil,
      });
    }
  }

  onReset(): void {
    this.debugAllControl.patchValue(true);
    this.debugAllControl.markAsDirty();
    this.allEnabledUntil = 0;
    this.cd.markForCheck();
  }

  private getByEntityTypeDebugLimit(entityType: EntityType): string {
    switch (entityType) {
      case EntityType.RULE_NODE:
        return getCurrentAuthState(this.store).ruleChainDebugPerTenantLimitsConfiguration;
      case EntityType.CALCULATED_FIELD:
        return getCurrentAuthState(this.store).calculatedFieldDebugPerTenantLimitsConfiguration;
    }
  }
}
