// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    styleUrls: [],
    standalone: false
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
