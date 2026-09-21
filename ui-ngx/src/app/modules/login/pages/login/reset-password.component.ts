// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

@Component({
    selector: 'tb-reset-password',
    templateUrl: './reset-password.component.html',
    styleUrls: ['./reset-password.component.scss'],
    standalone: false
})
export class ResetPasswordComponent extends PageComponent implements OnInit, OnDestroy {

  isExpiredPassword: boolean;

  resetToken = '';
  sub: Subscription;

  resetPassword = this.fb.group({
    newPassword: [''],
    newPassword2: ['']
  });

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private authService: AuthService,
              private translate: TranslateService,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.isExpiredPassword = this.route.snapshot.data.expiredPassword;
    this.sub = this.route
      .queryParams
      .subscribe(params => {
        this.resetToken = params.resetToken || '';
      });
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.sub.unsubscribe();
  }

  onResetPassword() {
    if (this.resetPassword.get('newPassword').value !== this.resetPassword.get('newPassword2').value) {
      this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('login.passwords-mismatch-error'),
        type: 'error' }));
    } else {
      this.authService.resetPassword(
        this.resetToken,
        this.resetPassword.get('newPassword').value).subscribe(
        () => this.router.navigateByUrl('login')
      );
    }
  }
}
