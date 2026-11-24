// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Params } from '@angular/router';
import { HOME_LIST_LIMIT, homeListFew, homeListShown } from '@home/pages/home-page/home-list.models';

@Component({
  selector: 'tb-home-list-footer',
  template: `
    @if (!few() || (total() && link())) {
      <div class="tb-home-list-footer">
        @if (!few()) {
          <button mat-button type="button" class="tb-home-list-toggle" (click)="expandedChange.emit(!expanded())">
            <mat-icon>{{ expanded() ? 'expand_less' : 'expand_more' }}</mat-icon>
            {{ toggleLabel() | translate }}
          </button>
        }
        <span class="tb-home-list-spacer"></span>
        @if (shown() < total()) {
          <span class="tb-home-list-count">{{ 'home.list.shown-of' | translate: {shown: shown(), total: (total() | shortNumber)} }}</span>
        }
        @if (total() && link()) {
          <a class="tb-home-list-view-all" [routerLink]="link()" [queryParams]="params()">{{ 'home.list.view-all' | translate }}<mat-icon>chevron_right</mat-icon></a>
        }
      </div>
    }
  `,
  styleUrls: ['./home-list-footer.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeListFooterComponent {

  total = input(0);

  expanded = input(false);

  link = input<string | null>(null);

  params = input<Params | null>(null);

  expandedChange = output<boolean>();

  readonly few = computed(() => homeListFew(this.total()));

  readonly shown = computed(() => homeListShown(this.total(), this.expanded()));

  readonly toggleLabel = computed(() => {
    if (this.expanded()) {
      return 'home.list.show-less';
    }
    return this.total() <= HOME_LIST_LIMIT ? 'home.list.show-all' : 'home.list.show-more';
  });
}
