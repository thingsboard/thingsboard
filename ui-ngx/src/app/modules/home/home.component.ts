///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { map, mergeMap, take } from 'rxjs/operators';

import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { User } from '@shared/models/user.model';
import { PageComponent } from '@shared/components/page.component';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { UserService } from '@core/http/user.service';
import { MenuService } from '@core/services/menu.service';
import { getCurrentAuthState, selectAuthUser, selectUserDetails } from '@core/auth/auth.selectors';
import { MediaBreakpoints } from '@shared/models/constants';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Router } from '@angular/router';
import * as screenfull from 'screenfull';
import { MatSidenav } from '@angular/material';
import { AuthState } from '@core/auth/auth.models';
import { WINDOW } from '@core/services/window.service';

@Component({
  selector: 'tb-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent extends PageComponent implements OnInit {

  authState: AuthState = getCurrentAuthState(this.store);

  forceFullscreen = this.authState.forceFullscreen;

  activeComponent: any;

  sidenavMode = 'side';
  sidenavOpened = true;

  logo = require('../../../assets/logo_title_white.svg');

  @ViewChild('sidenav', {static: false})
  sidenav: MatSidenav;

  // @ts-ignore
  fullscreenEnabled = screenfull.enabled;

  authUser$: Observable<any>;
  userDetails$: Observable<User>;
  userDetailsString: Observable<string>;

  constructor(protected store: Store<AppState>,
              @Inject(WINDOW) private window: Window,
              private authService: AuthService,
              private router: Router,
              private userService: UserService, private menuService: MenuService,
              public breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {

    this.authUser$ = this.store.pipe(select(selectAuthUser));
    this.userDetails$ = this.store.pipe(select(selectUserDetails));
    this.userDetailsString = this.userDetails$.pipe(map((user: User) => {
      return JSON.stringify(user);
    }));

    const isGtSm = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
    this.sidenavMode = isGtSm ? 'side' : 'over';
    this.sidenavOpened = isGtSm;

    this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm'])
      .subscribe((state: BreakpointState) => {
          if (state.matches) {
            this.sidenavMode = 'side';
            this.sidenavOpened = true;
          } else {
            this.sidenavMode = 'over';
            this.sidenavOpened = false;
          }
        }
      );
  }

  sidenavClicked() {
    if (this.sidenavMode === 'over') {
      this.sidenav.toggle();
    }
  }

  toggleFullscreen() {
    // @ts-ignore
    if (screenfull.enabled) {
      // @ts-ignore
      screenfull.toggle();
    }
  }

  isFullscreen() {
    // @ts-ignore
    return screenfull.isFullscreen;
  }

  goBack() {
    this.window.history.back();
  }
}
