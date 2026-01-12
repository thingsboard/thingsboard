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

import { Injectable, Optional, Renderer2, ViewContainerRef } from '@angular/core';
import { EntityDebugSettingsPanelComponent } from '@home/components/entity/debug/entity-debug-settings-panel.component';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { TranslateService } from '@ngx-translate/core';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { EntityDebugSettingPanelConfig } from '@home/components/entity/debug/entity-debug-settings.model';

@Injectable()
export class EntityDebugSettingsService {

  constructor(
    private popoverService: TbPopoverService,
    @Optional() public renderer: Renderer2,
    @Optional() public viewContainerRef: ViewContainerRef,
    private translate: TranslateService,
    private durationLeft: DurationLeftPipe,
  ) {}

  openDebugStrategyPanel(panelConfig: EntityDebugSettingPanelConfig, trigger: Element): void {
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const debugStrategyPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: EntityDebugSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'bottom',
        context: {
          ...panelConfig.debugSettings,
          ...panelConfig.debugConfig,
        },
        isModal: true,
      });
      debugStrategyPopover.tbComponentRef.instance.onSettingsApplied.subscribe(settings => {
        panelConfig.onSettingsAppliedFn(settings);
        debugStrategyPopover.hide();
      });
    }
  }


  getDebugConfigLabel(debugSettings: EntityDebugSettings): string {
    const isDebugActive = this.isDebugActive(debugSettings?.allEnabledUntil);

    if (!isDebugActive) {
      return debugSettings?.failuresEnabled ? this.translate.instant('debug-settings.failures') : this.translate.instant('common.disabled');
    } else {
      return this.durationLeft.transform(debugSettings?.allEnabledUntil);
    }
  }

  isDebugActive(allEnabledUntil: number): boolean {
    return allEnabledUntil > new Date().getTime();
  }
}
