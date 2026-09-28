// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  NgZone,
  OnDestroy,
  Renderer2,
  signal,
  ViewContainerRef
} from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityKeyType } from '@shared/models/query/query.models';
import { ApiUsageStateValue } from '@shared/models/api-usage.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { PopoverPreferredPlacement } from '@shared/components/popover.models';
import { AiCreditsPopupComponent } from './ai-credits-popup.component';
import { ShortNumberPipe } from '@shared/pipe/short-number.pipe';

@Component({
  selector: 'tb-ai-credits',
  templateUrl: './ai-credits.component.html',
  styleUrls: ['./ai-credits.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class AiCreditsComponent implements OnDestroy {

  expanded = input<boolean>(false);
  asButton = input<boolean>(false);
  active = input<boolean>(true);
  popoverPlacement = input<PopoverPreferredPlacement>(null);
  tooltipPosition = input<'above' | 'below' | 'before' | 'after'>(null);

  resolvedTooltipPosition = computed(() => {
    return this.tooltipPosition() ?? (this.asButton() ? 'above' : 'after');
  });

  resolvedPopoverPlacement = computed<PopoverPreferredPlacement>(() => {
    return this.popoverPlacement() ?? (this.asButton() ? 'bottom' : (this.expanded() ? 'top' : 'right'));
  });

  aiCreditsCount = signal(0);
  aiCreditsLimit = signal(0);
  aiApiState = signal<string>(null);

  hasData = computed(() => this.aiCreditsLimit() > 0);

  usagePercent = computed(() => {
    const max = this.aiCreditsLimit();
    return max > 0 ? Math.min((this.aiCreditsCount() / max) * 100, 100) : 0;
  });

  status = computed(() => this.aiApiState() ?? ApiUsageStateValue.ENABLED);

  private shortNumber = inject(ShortNumberPipe);

  usedLabel = computed(() => this.shortNumber.transform(this.aiCreditsCount(), {roundDown: true}));
  maxLabel = computed(() => this.shortNumber.transform(this.aiCreditsLimit()));

  resetDate = computed(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth() + 1, 2);
  });

  private telemetryWsService = inject(TelemetryWebsocketService);
  private zone = inject(NgZone);
  private popoverService = inject(TbPopoverService);
  private renderer = inject(Renderer2);
  private viewContainerRef = inject(ViewContainerRef);

  private telemetrySubscriber: TelemetrySubscriber;
  private cancelSubscription$ = new Subject<void>();

  private activeEffect = effect(() => {
    if (this.active()) {
      this.subscribeToCredits();
    } else {
      this.unsubscribeFromCredits();
    }
  });

  ngOnDestroy(): void {
    this.unsubscribeFromCredits();
    this.cancelSubscription$.complete();
  }

  private unsubscribeFromCredits(): void {
    this.cancelSubscription$.next();
    if (this.telemetrySubscriber) {
      this.telemetrySubscriber.unsubscribe();
      this.telemetrySubscriber = null;
    }
  }

  togglePopup(event: Event, trigger: Element): void {
    event.stopPropagation();
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const popover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: AiCreditsPopupComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: this.resolvedPopoverPlacement(),
        hideOnClickOutside: true,
        showCloseButton: true,
        context: {
          usedLabel: this.usedLabel(),
          maxLabel: this.maxLabel(),
          resetDate: this.resetDate()
        }
      });
      popover.tbComponentRef.instance.updatePlan.subscribe(() => {
        this.onUpdatePlan();
      });
    }
  }

  onUpdatePlan($event?: Event): void {
    $event?.stopPropagation();
    alert('Update plan');
  }

  private subscribeToCredits(): void {
    if (this.telemetrySubscriber) {
      return;
    }
    const latestKeys = [
      { key: 'aiApiState', type: EntityKeyType.TIME_SERIES },
      { key: 'aiCreditsLimit', type: EntityKeyType.TIME_SERIES },
      { key: 'aiCreditsCount', type: EntityKeyType.TIME_SERIES }
    ];

    this.telemetrySubscriber = TelemetrySubscriber.createEntityFilterLatestSubscription(
      this.telemetryWsService,
      { type: AliasFilterType.apiUsageState },
      this.zone,
      latestKeys
    );

    this.telemetrySubscriber.subscribe();
    this.telemetrySubscriber.entityData$.pipe(
      takeUntil(this.cancelSubscription$)
    ).subscribe(update => {
      const entityData = update.data?.data ?? update.update;
      if (entityData?.length) {
        const latest = entityData[0].latest;
        if (latest?.[EntityKeyType.TIME_SERIES]) {
          const ts = latest[EntityKeyType.TIME_SERIES];
          if (ts.aiCreditsCount) {
            this.aiCreditsCount.set(Number(ts.aiCreditsCount.value) || 0);
          }
          if (ts.aiCreditsLimit) {
            this.aiCreditsLimit.set(Number(ts.aiCreditsLimit.value) || 0);
          }
          if (ts.aiApiState) {
            this.aiApiState.set(ts.aiApiState.value);
          }
        }
      }
    });
  }
}
