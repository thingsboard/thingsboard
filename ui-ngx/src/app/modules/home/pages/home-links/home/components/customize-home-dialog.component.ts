// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject, ViewEncapsulation } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectHomeCards } from '@core/auth/auth.selectors';
import { ActionPreferencesUpdateHomeCards } from '@core/auth/auth.actions';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS, isHomeCardShown } from '@home/pages/home-links/home/home-cards.models';

export interface CustomizeHomeDialogData {
  cards: HomeCardId[];
}

@Component({
  selector: 'tb-customize-home-dialog',
  template: `
    <div class="tb-customize-home" role="dialog" aria-labelledby="tb-customize-home-title">
      <h2 id="tb-customize-home-title">{{ 'home.customize.title' | translate }}</h2>
      <div class="tb-sections" role="group" [attr.aria-label]="'home.customize.cards' | translate">
        @for (row of rows(); track row.card.id) {
          <label class="tb-section-row">
            <input type="checkbox" [checked]="row.shown" (change)="toggle(row.card.id, $any($event.target).checked)">
            <mat-icon aria-hidden="true">{{ row.card.icon }}</mat-icon>
            <span class="tb-section-name">{{ row.card.name | translate }}</span>
          </label>
        }
      </div>
      <div class="tb-customize-actions">
        @if (anyHidden()) {
          <button mat-button type="button" (click)="showAll()">{{ 'home.customize.show-all' | translate }}</button>
        }
        <span class="flex-1"></span>
        <button mat-flat-button color="primary" type="button" (click)="dialogRef.close()">{{ 'action.done' | translate }}</button>
      </div>
    </div>
  `,
  styleUrls: ['./customize-home-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomizeHomeDialogComponent {

  readonly dialogRef = inject(MatDialogRef<CustomizeHomeDialogComponent>);

  private data = inject<CustomizeHomeDialogData>(MAT_DIALOG_DATA);
  private store = inject(Store<AppState>);

  private settings = toSignal(this.store.select(selectHomeCards), {initialValue: {}});

  readonly rows = computed(() => this.data.cards.map(id => ({card: HOME_CARDS[id], shown: isHomeCardShown(this.settings(), id)})));

  readonly anyHidden = computed(() => this.rows().some(row => !row.shown));

  toggle(id: HomeCardId, shown: boolean): void {
    this.store.dispatch(new ActionPreferencesUpdateHomeCards({[id]: shown}));
  }

  showAll(): void {
    this.store.dispatch(new ActionPreferencesUpdateHomeCards(Object.fromEntries(this.data.cards.map(id => [id, true]))));
  }
}
