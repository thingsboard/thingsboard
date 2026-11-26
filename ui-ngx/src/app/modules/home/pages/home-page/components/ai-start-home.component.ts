// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, NgZone, signal } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
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
    @if (!resolved()) {
      @if (reserved) {
        <div class="tb-ai-start-block reserve" aria-hidden="true">
          <div class="tb-ai-start-h">{{ 'home.ai.start-title' | translate }}</div>
          <div class="tb-ai-starts">
            @for (n of [0, 1, 2]; track n) {
              <span class="tb-ai-start placeholder">
                <span class="tb-ai-start-ic tb-ai-start-skeleton"></span>
                <span class="tb-ai-start-line tb-ai-start-skeleton"></span>
              </span>
            }
          </div>
        </div>
      }
    } @else if (show()) {
      <div class="tb-ai-start-block" [class.appear]="!reserved">
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
          <a class="tb-ai-start cli" routerLink="/ai-assistant" [queryParams]="{cli: true, chats: 'closed'}"
             [matTooltip]="'home.ai.start-cli-tip' | translate" matTooltipPosition="above">
            <span class="tb-ai-start-ic" aria-hidden="true"><mat-icon fontSet="material-icons-round">terminal</mat-icon></span>
            <span class="tb-ai-start-t">{{ 'home.ai.start-cli' | translate }}</span>
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

  readonly resolved = computed(() => this.chats.loaded() && this.solutions.loaded() && this.devices() !== null);

  readonly show = computed(() => this.resolved() && this.devices() === 0
    && !this.chats.chats().length && !this.solutions.solutions().length);

  private readonly shownKey = `tb-home-ai-start:${getCurrentAuthUser(inject(Store<AppState>))?.userId}`;

  readonly reserved = this.readShown();

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
    effect(() => {
      if (this.resolved()) {
        this.writeShown(this.show());
      }
    });
  }

  private readShown(): boolean {
    try {
      return localStorage.getItem(this.shownKey) === '1';
    } catch {
      return false;
    }
  }

  private writeShown(shown: boolean): void {
    try {
      localStorage.setItem(this.shownKey, shown ? '1' : '0');
    } catch {
      return;
    }
  }
}
