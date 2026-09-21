// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
