// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PePackOffer, pePackOfferInfoMap, pePackOffers } from '@shared/models/subscription.models';

/**
 * Card row used by the Community-Grant flows to let the viewer sample what the Professional Pack adds:
 * White-labeling, Integrations, the Scheduler and Reports. Cards are opt-in per input so the caller can
 * hide the one it is currently offering (e.g. the Integrations screen hides its own card). The grid width
 * follows the number of visible cards up to three columns, and collapses to one on the smallest screens.
 * Set disableNavigation when the button should render without hover affordance and swallow clicks (used
 * for the sysadmin view of the White-labeling dialog).
 */
@Component({
  selector: 'tb-pe-pack-offer-cards',
  templateUrl: './pe-pack-offer-cards.component.html',
  styleUrls: ['./pe-pack-offer-cards.component.scss'],
  standalone: false
})
export class PePackOfferCardsComponent {

  @Input() showWhiteLabeling = false;
  @Input() showIntegrations = false;
  @Input() showScheduler = false;
  @Input() showReports = false;
  @Input() disableNavigation = false;

  @Output() cardClick = new EventEmitter<string>();

  PePackOffer = PePackOffer;
  pePackOfferInfoMap = pePackOfferInfoMap;

  get visibleOffers(): PePackOffer[] {
    return pePackOffers.filter((offer) => this.isOfferVisible(offer));
  }

  onCard($event: Event, offer: PePackOffer): void {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.disableNavigation) {
      return;
    }
    const page = pePackOfferInfoMap.get(offer)?.page;
    if (page) {
      this.cardClick.emit(page);
    }
  }

  private isOfferVisible(offer: PePackOffer): boolean {
    switch (offer) {
      case PePackOffer.WHITE_LABELING:
        return this.showWhiteLabeling;
      case PePackOffer.INTEGRATIONS:
        return this.showIntegrations;
      case PePackOffer.SCHEDULER:
        return this.showScheduler;
      case PePackOffer.REPORTS:
        return this.showReports;
    }
  }
}
