// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';
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
export class UserMenuComponent {

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

  userFullName$ = this.store.pipe(
    select(selectUserDetails),
    map((user) => this.getUserFullName(user))
  );

  userInitials$ = this.store.pipe(
    select(selectUserDetails),
    map((user) => this.getUserInitials(user))
  );

  userEmail$ = this.store.pipe(
    select(selectUserDetails),
    map((user) => user?.email)
  );

  constructor(private store: Store<AppState>,
              private router: Router,
              private authService: AuthService) {
  }

  private getAuthorityName(user: User): string {
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

  private getUserDisplayName(user: User): string {
    if (user) {
      return this.getUserFullName(user) || user.email;
    } else {
      return '';
    }
  }

  private getUserFullName(user: User): string {
    if (user) {
      return [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    } else {
      return '';
    }
  }

  private getUserInitials(user: User): string {
    if (user) {
      const first = (user.firstName || "").trim()[0] || "";
      const last = (user.lastName || "").trim()[0] || "";
      const nameInitials = (first + last).toUpperCase();
      if (nameInitials) {
        return nameInitials;
      }
      const [local, domain] = (user.email || "").split("@");
      const localInitial = (local || "")[0] || "";
      const domainInitial = (domain || "")[0] || "";
      return (localInitial + domainInitial).toUpperCase() || "?";
    } else {
      return '?';
    }
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
