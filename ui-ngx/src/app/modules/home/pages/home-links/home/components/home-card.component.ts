// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { Store } from '@ngrx/store';
import { ToastNotificationService } from '@core/services/toast-notification.service';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { ActionPreferencesUpdateHomeCards } from '@core/auth/auth.actions';
import { HomeCardDefinition } from '@home/pages/home-links/home/home-cards.models';

const UNDO_MS = 6000;

@Component({
  selector: 'tb-home-card',
  template: `
    <section class="tb-home-card" [class.hideable]="hideable()" [attr.data-home-card]="card().id" [style.min-height.px]="card().height"
             [attr.aria-label]="card().name | translate">
      <div class="tb-home-card-header" [class.expanded]="expanded()">
        <div class="tb-home-card-title-group">
          @if (card().link) {
            <a class="tb-home-card-title link" [routerLink]="card().link">
              <mat-icon class="tb-home-card-icon">{{ card().icon }}</mat-icon>
              <span>{{ card().name | translate }}</span>
              @if (count()) {
                <span class="tb-home-card-count">{{ count() | shortNumber }}</span>
              }
            </a>
          } @else {
            <span class="tb-home-card-title">
              <mat-icon class="tb-home-card-icon">{{ card().icon }}</mat-icon>
              <span>{{ card().name | translate }}</span>
            </span>
          }
          <ng-content select="[homeCardTitleExtra]"></ng-content>
        </div>
        <div class="tb-home-card-actions">
          @if (hideable() && !expanded()) {
            <button mat-icon-button type="button" class="tb-home-card-hide"
                    [matTooltip]="'home.customize.hide' | translate: {name: (card().name | translate)}" matTooltipPosition="above"
                    [attr.aria-label]="'home.customize.hide' | translate: {name: (card().name | translate)}" (click)="hide()">
              <mat-icon>visibility_off</mat-icon>
            </button>
          }
          <ng-content select="[homeCardActions]"></ng-content>
        </div>
      </div>
      <ng-content></ng-content>
    </section>
  `,
  styleUrls: ['./home-card.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeCardComponent {

  card = input.required<HomeCardDefinition>();

  hideable = input(false);

  expanded = input(false);

  count = input<number | null>(null);

  private store = inject(Store<AppState>);
  private translate = inject(TranslateService);
  private toast = inject(ToastNotificationService);

  hide(): void {
    const id = this.card().id;
    const name = this.translate.instant(this.card().name);
    this.store.dispatch(new ActionPreferencesUpdateHomeCards({[id]: false}));
    this.toast.dispatchNotification({
      message: this.translate.instant('home.customize.hidden', {name}),
      type: 'info',
      duration: UNDO_MS,
      horizontalPosition: 'right',
      verticalPosition: 'bottom',
      panelClass: 'tb-home-toast',
      actionLabel: this.translate.instant('home.customize.undo'),
      onAction: () => this.store.dispatch(new ActionPreferencesUpdateHomeCards({[id]: true}))
    });
  }
}
