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
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';

@Component({
    selector: 'tb-create-password',
    templateUrl: './create-password.component.html',
    styleUrls: ['./create-password.component.scss'],
    standalone: false
})
export class CreatePasswordComponent extends PageComponent implements OnInit, OnDestroy {

  activateToken = '';
  sub: Subscription;

  createPassword = this.fb.group({
    password: [''],
    password2: ['']
  });

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private authService: AuthService,
              private translate: TranslateService,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.sub = this.route
      .queryParams
      .subscribe(params => {
        this.activateToken = params.activateToken || '';
      });
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.sub.unsubscribe();
  }

  onCreatePassword() {
    if (this.createPassword.get('password').value !== this.createPassword.get('password2').value) {
      this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('login.passwords-mismatch-error'),
        type: 'error' }));
    } else {
      this.authService.activate(
        this.activateToken,
        this.createPassword.get('password').value, true).subscribe();
    }
  }
}
