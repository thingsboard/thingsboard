// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectHomeCards } from '@core/auth/auth.selectors';
import { ActionPreferencesUpdateHomeCards } from '@core/auth/auth.actions';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS, isHomeCardShown } from '@home/pages/home-links/home/home-cards.models';
import {
  CustomizeHomeDialogComponent,
  CustomizeHomeDialogData
} from '@home/pages/home-links/home/components/customize-home-dialog.component';

@Component({
  selector: 'tb-home-cards-footer',
  template: `
    <footer class="tb-home-foot">
      @if (hidden().length) {
        <div class="tb-hidden-cards" role="group" [attr.aria-label]="'home.customize.show-on-home' | translate">
          <span class="tb-hidden-lbl">{{ 'home.customize.show-on-home' | translate }}</span>
          @for (card of hidden(); track card.id) {
            <button type="button" class="tb-foot-btn" (click)="show(card.id)">
              <mat-icon aria-hidden="true">{{ card.icon }}</mat-icon>
              <span>{{ card.name | translate }}</span>
            </button>
          }
        </div>
      }
      <button type="button" class="tb-foot-btn tb-customize-btn" aria-haspopup="dialog" (click)="customize()">
        <mat-icon aria-hidden="true">settings</mat-icon>
        <span>{{ 'home.customize.title' | translate }}</span>
      </button>
    </footer>
  `,
  styleUrls: ['./home-cards-footer.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeCardsFooterComponent {

  cards = input.required<HomeCardId[]>();

  private store = inject(Store<AppState>);
  private dialog = inject(MatDialog);

  private settings = toSignal(this.store.select(selectHomeCards), {initialValue: {}});

  readonly hidden = computed(() => this.cards().filter(id => !isHomeCardShown(this.settings(), id)).map(id => HOME_CARDS[id]));

  show(id: HomeCardId): void {
    this.store.dispatch(new ActionPreferencesUpdateHomeCards({[id]: true}));
    setTimeout(() => document.querySelector(`[data-home-card="${id}"]`)?.scrollIntoView({behavior: 'smooth', block: 'center'}));
  }

  customize(): void {
    this.dialog.open<CustomizeHomeDialogComponent, CustomizeHomeDialogData>(CustomizeHomeDialogComponent, {
      panelClass: 'tb-customize-home-panel',
      autoFocus: 'first-tabbable',
      data: {cards: this.cards()}
    });
  }
}
