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

import {Component, OnInit} from '@angular/core';
import {UserService} from '@core/http/user.service';
import {User} from '@shared/models/user.model';
import {Authority} from '@shared/models/authority.enum';
import {PageComponent} from '@shared/components/page.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {HasConfirmForm} from '@core/guards/confirm-on-exit.guard';
import {ActionAuthUpdateUserDetails} from '@core/auth/auth.actions';
import {environment as env} from '@env/environment';
import {TranslateService} from '@ngx-translate/core';
import {ActionSettingsChangeLanguage} from '@core/settings/settings.actions';
import {ChangePasswordDialogComponent} from '@modules/home/pages/profile/change-password-dialog.component';
import {MatDialog} from '@angular/material';
import {DialogService} from '@core/services/dialog.service';
import {AuthService} from '@core/auth/auth.service';
import {ActivatedRoute} from '@angular/router';
import { Dashboard } from '@shared/models/dashboard.models';
import { RuleChain } from '@shared/models/rule-chain.models';

@Component({
  selector: 'tb-rulechain-page',
  templateUrl: './rulechain-page.component.html',
  styleUrls: []
})
export class RuleChainPageComponent extends PageComponent implements OnInit {

  ruleChain: RuleChain;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private userService: UserService,
              private authService: AuthService,
              private translate: TranslateService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.ruleChain = this.route.snapshot.data.ruleChain;
  }

}
