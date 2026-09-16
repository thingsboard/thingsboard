// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  ChangeDetectionStrategy,
  Component, EventEmitter,
  Input,
  OnDestroy,
  OnInit, Output,
  ViewEncapsulation
} from '@angular/core';
import { User } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectAuthUser, selectUserDetails } from '@core/auth/auth.selectors';
import { map } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { Router } from '@angular/router';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-user-menu',
    templateUrl: './user-menu.component.html',
    styleUrls: ['./user-menu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class UserMenuComponent implements OnInit, OnDestroy {

  @Input()
  @coerceBoolean()
  collapsed = false;

  @Output()
  menuClicked = new EventEmitter();

  authorities = Authority;

  authority$ = this.store.pipe(
    select(selectAuthUser),
    map((authUser) => authUser ? authUser.authority : Authority.ANONYMOUS)
  );

  authorityName$ = this.store.pipe(
    select(selectUserDetails),
    map((user) => this.getAuthorityName(user))
  );

  userDisplayName$ = this.store.pipe(
    select(selectUserDetails),
    map((user) => this.getUserDisplayName(user))
  );

  userEmail$ = this.store.pipe(
    select(selectUserDetails),
    map((user) => user?.email)
  );

  constructor(private store: Store<AppState>,
              private router: Router,
              private authService: AuthService) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  getAuthorityName(user: User): string {
    let name = null;
    if (user) {
      const authority = user.authority;
      switch (authority) {
        case Authority.SYS_ADMIN:
          name = 'user.sys-admin';
          break;
        case Authority.TENANT_ADMIN:
          name = 'user.tenant-admin';
          break;
        case Authority.CUSTOMER_USER:
          name = 'user.customer';
          break;
      }
    }
    return name;
  }

  getUserDisplayName(user: User): string {
    let name = '';
    if (user) {
      if ((user.firstName && user.firstName.length > 0) ||
        (user.lastName && user.lastName.length > 0)) {
        if (user.firstName) {
          name += user.firstName;
        }
        if (user.lastName) {
          if (name.length > 0) {
            name += ' ';
          }
          name += user.lastName;
        }
      } else {
        name = user.email;
      }
    }
    return name;
  }

  openAccount(): void {
    this.menuClicked.emit();
    this.router.navigate(['account']);
  }

  logout(): void {
    this.menuClicked.emit();
    this.authService.logout();
  }

}
