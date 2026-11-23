// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-links/home/home-cards.models';

@Component({
  selector: 'tb-get-started-home',
  template: `<tb-home-card [card]="card"></tb-home-card>`,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GetStartedHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.GET_STARTED];
}
