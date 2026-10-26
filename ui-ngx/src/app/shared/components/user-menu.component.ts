// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  ViewChild,
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
export class UserMenuComponent implements OnDestroy {

  @Input()
  @coerceBoolean()
  collapsed = false;

  @Output()
  menuClicked = new EventEmitter();

  @ViewChild('emailLocal')
  set emailLocal(emailLocal: ElementRef<HTMLElement>) {
    this.emailResize$?.disconnect();
    const el = emailLocal?.nativeElement;
    const text = el?.firstChild;
    if (text) {
      const local = text.nodeValue;
      const range = document.createRange();
      range.selectNodeContents(el);
      const overflows = () => range.getBoundingClientRect().width > el.getBoundingClientRect().width;
      this.emailResize$ = new ResizeObserver(() => {
        text.nodeValue = local;
        for (let length = local.length - 1; length >= 0 && overflows(); length--) {
          text.nodeValue = local.substring(0, length) + '…';
        }
      });
      this.emailResize$.observe(el);
    }
  }

  private emailResize$: ResizeObserver;

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
    map((user) => this.splitEmail('tenanttenanttenanttenantnanttenantnanttenant@thingsboard.io'))
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

  private splitEmail(email: string): {full: string; local: string; domain: string} {
    if (!email) {
      return null;
    }
    const atIndex = email.lastIndexOf('@');
    if (atIndex <= 0) {
      return {full: email, local: email, domain: ''};
    }
    return {full: email, local: email.substring(0, atIndex), domain: email.substring(atIndex)};
  }

  ngOnDestroy(): void {
    this.emailResize$?.disconnect();
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
