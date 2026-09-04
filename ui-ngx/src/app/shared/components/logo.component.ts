// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';

@Component({
    selector: 'tb-logo',
    templateUrl: './logo.component.html',
    styleUrls: ['./logo.component.scss'],
    standalone: false
})
export class LogoComponent {

  logo = 'assets/logo_title_white.svg';

  gotoThingsboard(): void {
    window.open('https://thingsboard.io', '_blank');
  }

}
