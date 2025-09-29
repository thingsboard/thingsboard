///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Input, OnInit, HostBinding } from '@angular/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { AuthService } from '@core/auth/auth.service';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { UrlTree } from '@angular/router';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { UrlHolder } from '@shared/pipe/image.pipe';

@Component({
  selector: 'tb-logo',
  templateUrl: './logo.component.html',
  styleUrls: ['./logo.component.scss']
})
export class LogoComponent implements OnInit {

  @HostBinding('class.login-logo')
  get isLoginLogoClass() {
    return this.isLogin;
  }

  @Input()
  @coerceBoolean()
  isLogin: boolean = false;

  @Input()
  logoSrc: string | UrlHolder = 'assets/logo_title_white.svg';

  logoLink: UrlTree;

  constructor(private authService: AuthService,
              private store: Store<AppState>) {
  }

  ngOnInit() {
    if (!this.isLogin) {
      const authState = getCurrentAuthState(this.store);
      this.logoLink = this.authService.defaultUrl(true, authState);
    }
  }

  onLogoClick() {
    if (this.isLogin) {
      this.gotoThingsboard();
    }
  }

  private gotoThingsboard() {
    window.open('https://thingsboard.io', '_blank');
  }
}
