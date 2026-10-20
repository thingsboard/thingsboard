// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
  selector: 'tb-ai-credits-popup',
  templateUrl: './ai-credits-popup.component.html',
  styleUrls: ['./ai-credits-popup.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AiCreditsPopupComponent {

  usedLabel: string;
  maxLabel: string;
  resetDate: Date;

  updatePlan = output<void>();

  private popover = inject<TbPopoverComponent<AiCreditsPopupComponent>>(TbPopoverComponent);

  cancel(): void {
    this.popover.hide();
  }
}
