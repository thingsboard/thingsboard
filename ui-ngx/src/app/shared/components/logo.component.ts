// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Input, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { UrlTree } from '@angular/router';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { UrlHolder } from '@shared/pipe/image.pipe';

@Component({
    selector: 'tb-logo',
    templateUrl: './logo.component.html',
    styleUrls: ['./logo.component.scss'],
    standalone: false
})
export class LogoComponent implements OnInit {

  @Input()
  src: string | UrlHolder = 'assets/logo_title_white.svg';

  @Input()
  link: string | UrlTree;

  @Input()
  target: string = null;

  @Input()
  embed = false;

  isExternal = false;

  constructor(private authService: AuthService,
              private store: Store<AppState>) {
  }

  ngOnInit() {
    if (!this.link) {
      const authState = getCurrentAuthState(this.store);
      this.link = this.authService.defaultUrl(true, authState);
    }
    if (typeof this.link === 'string' && this.link.startsWith('http')) {
      this.isExternal = true;
    }
  }
}
