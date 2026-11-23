// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, NgZone, signal } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AiChatsService } from '@core/services/ai-chats.service';
import { AiSolutionsService } from '@core/services/ai-solutions.service';
import { EntityCountCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { NEW_SOLUTION_ID } from '@shared/models/solution-creator.models';

@Component({
  selector: 'tb-ai-start-home',
  template: `
    @if (show()) {
      <div class="tb-ai-start-block">
        <div class="tb-ai-start-h" id="tb-ai-start-h">{{ 'home.ai.start-title' | translate }}</div>
        <div class="tb-ai-starts" role="group" aria-labelledby="tb-ai-start-h">
          <a class="tb-ai-start dev" routerLink="/ai-assistant" [queryParams]="{getStarted: true, focus: 'composer'}"
             [matTooltip]="'home.ai.start-device-tip' | translate" matTooltipPosition="above">
            <span class="tb-ai-start-ic" aria-hidden="true"><mat-icon>memory</mat-icon></span>
            <span class="tb-ai-start-t">{{ 'home.ai.start-device' | translate }}</span>
            <mat-icon class="tb-ai-start-go" aria-hidden="true">chevron_right</mat-icon>
          </a>
          <a class="tb-ai-start ex" routerLink="/ai-assistant" [queryParams]="{solutionId: newSolutionId, focus: 'composer'}"
             [matTooltip]="'home.ai.start-solution-tip' | translate" matTooltipPosition="above">
            <span class="tb-ai-start-ic" aria-hidden="true"><mat-icon>layers</mat-icon></span>
            <span class="tb-ai-start-t">{{ 'home.ai.start-solution' | translate }}</span>
            <mat-icon class="tb-ai-start-go" aria-hidden="true">chevron_right</mat-icon>
          </a>
        </div>
      </div>
    }
  `,
  styleUrls: ['./ai-start-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiStartHomeComponent {

  readonly newSolutionId = NEW_SOLUTION_ID;

  private chats = inject(AiChatsService);
  private solutions = inject(AiSolutionsService);

  private readonly devices = signal<number | null>(null);

  readonly show = computed(() => this.chats.loaded() && this.solutions.loaded() && this.devices() === 0
    && !this.chats.chats().length && !this.solutions.solutions().length);

  constructor() {
    this.chats.ensureLoaded();
    this.solutions.ensureLoaded();
    const cmd = new EntityCountCmd();
    cmd.query = {entityFilter: {type: AliasFilterType.entityType, entityType: EntityType.DEVICE}};
    const subscriber = new TelemetrySubscriber(inject(TelemetryWebsocketService), inject(NgZone));
    subscriber.subscriptionCommands.push(cmd);
    subscriber.entityCount$.subscribe(update => this.devices.set(update.count ?? 0));
    subscriber.subscribe();
    inject(DestroyRef).onDestroy(() => subscriber.unsubscribe());
  }
}
