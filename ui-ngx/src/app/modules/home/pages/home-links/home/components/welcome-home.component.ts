// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-welcome-home',
  template: `
    <div class="tb-home-kicker">{{ today | date: 'EEEE d MMMM' }}</div>
    <h1 class="tb-greeting" [class.with-spark]="spark()">
      @if (spark()) {
        <tb-ai-mark class="tb-greet-spark" size="44"></tb-ai-mark>
      }
      <span>{{ (name ? 'home.greeting.' + daypart + '-name' : 'home.greeting.' + daypart) | translate: {name} }}</span>
    </h1>
  `,
  styleUrls: ['./welcome-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WelcomeHomeComponent {

  spark = input(false);

  readonly today = new Date();

  readonly daypart = this.today.getHours() < 12 ? 'morning' : this.today.getHours() < 18 ? 'afternoon' : 'evening';

  readonly name = (() => {
    const auth = getCurrentAuthState(inject(Store<AppState>));
    return (auth?.userDetails?.firstName || auth?.authUser?.firstName || '').trim().split(/\s+/)[0] || null;
  })();
}
