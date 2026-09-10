// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'tb-link-expired',
    templateUrl: './link-expired.component.html',
    styleUrls: ['./link-expired.component.scss'],
    standalone: false
})
export class LinkExpiredComponent {

  isPasswordLinkExpired: boolean;
  title: string;
  message: string;

  constructor(private route: ActivatedRoute) {
    this.isPasswordLinkExpired = this.route.snapshot.data.passwordLinkExpired;
    this.title = this.isPasswordLinkExpired ? 'login.reset-password-link-expired' : 'login.activation-link-expired';
    this.message = this.isPasswordLinkExpired ? 'login.reset-password-link-expired-message' :
      'login.activation-link-expired-message';
  }
}
