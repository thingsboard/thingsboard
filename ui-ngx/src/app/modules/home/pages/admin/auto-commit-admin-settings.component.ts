///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormGroup } from '@angular/forms';
import { AutoCommitSettingsComponent } from '@home/components/vc/auto-commit-settings.component';
import { selectHasRepository } from '@core/auth/auth.selectors';
import { RepositorySettingsComponent } from '@home/components/vc/repository-settings.component';

@Component({
  selector: 'tb-auto-commit-admin-settings',
  templateUrl: './auto-commit-admin-settings.component.html',
  styleUrls: []
})
export class AutoCommitAdminSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  @ViewChild('repositorySettingsComponent', {static: false}) repositorySettingsComponent: RepositorySettingsComponent;
  @ViewChild('autoCommitSettingsComponent', {static: false}) autoCommitSettingsComponent: AutoCommitSettingsComponent;

  hasRepository$ = this.store.pipe(select(selectHasRepository));

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
  }

  confirmForm(): UntypedFormGroup {
    return this.repositorySettingsComponent ?
      this.repositorySettingsComponent?.repositorySettingsForm :
      this.autoCommitSettingsComponent?.autoCommitSettingsForm;
  }
}
