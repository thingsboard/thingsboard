// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  booleanAttribute,
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
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { aiCreditsResetDate, watchAiCredits } from '@home/components/ai/ai-credits.models';
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
  composer = input(false, {transform: booleanAttribute});
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

  over = computed(() => this.status() === ApiUsageStateValue.DISABLED || this.usagePercent() >= 100);

  near = computed(() => !this.over() && (this.status() === ApiUsageStateValue.WARNING || this.usagePercent() >= 80));

  arcDash = computed(() => {
    const circumference = 2 * Math.PI * 8;
    const share = Math.max(this.usagePercent() / 100, this.aiCreditsCount() > 0 ? 0.02 : 0);
    return `${(share * circumference).toFixed(2)} ${circumference.toFixed(2)}`;
  });

  private shortNumber = inject(ShortNumberPipe);

  usedLabel = computed(() => this.shortNumber.transform(this.aiCreditsCount(), {roundDown: true}));
  maxLabel = computed(() => this.shortNumber.transform(this.aiCreditsLimit()));
  leftLabel = computed(() => this.shortNumber.transform(Math.max(0, this.aiCreditsLimit() - this.aiCreditsCount()), {roundDown: true}));

  resetDate = computed(() => aiCreditsResetDate());

  private telemetryWsService = inject(TelemetryWebsocketService);
  private zone = inject(NgZone);
  private popoverService = inject(TbPopoverService);
  private renderer = inject(Renderer2);
  private viewContainerRef = inject(ViewContainerRef);

  private telemetrySubscriber: TelemetrySubscriber;

  private activeEffect = effect(() => {
    if (this.active()) {
      this.subscribeToCredits();
    } else {
      this.unsubscribeFromCredits();
    }
  });

  ngOnDestroy(): void {
    this.unsubscribeFromCredits();
  }

  private unsubscribeFromCredits(): void {
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
    this.telemetrySubscriber = watchAiCredits(this.telemetryWsService, this.zone, update => {
      if (update.count !== undefined) {
        this.aiCreditsCount.set(update.count);
      }
      if (update.limit !== undefined) {
        this.aiCreditsLimit.set(update.limit);
      }
      if (update.state) {
        this.aiApiState.set(update.state);
      }
    });
  }
}
