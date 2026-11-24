// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'tb-customer-user-home',
  template: `
    <div class="tb-home">
      <div class="tb-home-head">
        <tb-welcome-home></tb-welcome-home>
        <tb-status-line-home></tb-status-line-home>
      </div>
    </div>
  `,
  styleUrls: ['./tenant-admin-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CustomerUserHomeComponent {
}
