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

import { ChangeDetectorRef, Component, DestroyRef, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import {
  RepositorySettings,
  RepositoryAuthMethod,
  repositoryAuthMethodTranslationMap
} from '@shared/models/settings.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { isNotEmptyStr } from '@core/utils';
import { DialogService } from '@core/services/dialog.service';
import { ActionAuthUpdateHasRepository } from '@core/auth/auth.actions';
import { selectHasRepository } from '@core/auth/auth.selectors';
import { catchError, mergeMap, take } from 'rxjs/operators';
import { of } from 'rxjs';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-repository-settings',
  templateUrl: './repository-settings.component.html',
  styleUrls: ['./repository-settings.component.scss', './../../pages/admin/settings-card.scss']
})
export class RepositorySettingsComponent extends PageComponent implements OnInit {

  @Input()
  detailsMode = false;

  @Input()
  popoverComponent: TbPopoverComponent;

  @Input()
  @coerceBoolean()
  hideLoadingBar = false;

  repositorySettingsForm: UntypedFormGroup;
  settings: RepositorySettings = null;

  repositoryAuthMethod = RepositoryAuthMethod;
  repositoryAuthMethods = Object.values(RepositoryAuthMethod);
  repositoryAuthMethodTranslations = repositoryAuthMethodTranslationMap;

  showChangePassword = false;
  changePassword = false;

  showChangePrivateKeyPassword = false;
  changePrivateKeyPassword = false;

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private cd: ChangeDetectorRef,
              public fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit() {
    this.repositorySettingsForm = this.fb.group({
      repositoryUri: [null, [Validators.required]],
      defaultBranch: ['main', []],
      readOnly: [false, []],
      showMergeCommits: [false, []],
      authMethod: [RepositoryAuthMethod.USERNAME_PASSWORD, [Validators.required]],
      username: [null, []],
      password: [null, []],
      privateKeyFileName: [null, [Validators.required]],
      privateKey: [null, []],
      privateKeyPassword: [null, []]
    });
    this.updateValidators(false);
    this.repositorySettingsForm.get('authMethod').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
    });
    this.repositorySettingsForm.get('privateKeyFileName').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(false);
    });
    this.store.pipe(
      select(selectHasRepository),
      take(1),
      mergeMap((hasRepository) => {
        if (hasRepository) {
          return this.adminService.getRepositorySettings({ignoreErrors: true}).pipe(
            catchError(() => of(null))
          );
        } else {
          return of(null);
        }
      })
    ).subscribe(
      (settings) => {
        this.settings = settings;
        if (this.settings != null) {
          if (this.settings.authMethod === RepositoryAuthMethod.USERNAME_PASSWORD) {
            this.showChangePassword = true;
          } else {
            this.showChangePrivateKeyPassword = true;
          }
          this.repositorySettingsForm.reset(this.settings);
          this.updateValidators(false);
        }
    });
  }

  checkAccess(): void {
    const settings: RepositorySettings = this.repositorySettingsForm.value;
    this.adminService.checkRepositoryAccess(settings).subscribe(() => {
      this.store.dispatch(new ActionNotificationShow({ message: this.translate.instant('admin.check-repository-access-success'),
        type: 'success' }));
    });
  }

  save(): void {
    const settings: RepositorySettings = this.repositorySettingsForm.value;
    this.adminService.saveRepositorySettings(settings).subscribe(
      (savedSettings) => {
        this.settings = savedSettings;
        if (this.settings.authMethod === RepositoryAuthMethod.USERNAME_PASSWORD) {
          this.showChangePassword = true;
          this.changePassword = false;
        } else {
          this.showChangePrivateKeyPassword = true;
          this.changePrivateKeyPassword = false;
        }
        this.repositorySettingsForm.reset(this.settings);
        this.updateValidators(false);
        this.store.dispatch(new ActionAuthUpdateHasRepository({ hasRepository: true }));
      }
    );
  }

  delete(formDirective: FormGroupDirective): void {
    this.dialogService.confirm(
      this.translate.instant('admin.delete-repository-settings-title', ),
      this.translate.instant('admin.delete-repository-settings-text'), null,
      this.translate.instant('action.delete')
    ).subscribe((data) => {
      if (data) {
        this.adminService.deleteRepositorySettings().subscribe(
          () => {
            this.settings = null;
            this.showChangePassword = false;
            this.changePassword = false;
            this.showChangePrivateKeyPassword = false;
            this.changePrivateKeyPassword = false;
            formDirective.resetForm();
            this.repositorySettingsForm.reset({ defaultBranch: 'main', authMethod: RepositoryAuthMethod.USERNAME_PASSWORD });
            this.updateValidators(false);
            this.store.dispatch(new ActionAuthUpdateHasRepository({ hasRepository: false }));
          }
        );
      }
    });
  }

  changePasswordChanged() {
    if (this.changePassword) {
      this.repositorySettingsForm.get('password').patchValue('');
      this.repositorySettingsForm.get('password').markAsDirty();
    }
    this.updateValidators(false);
  }

  changePrivateKeyPasswordChanged() {
    if (this.changePrivateKeyPassword) {
      this.repositorySettingsForm.get('privateKeyPassword').patchValue('');
      this.repositorySettingsForm.get('privateKeyPassword').markAsDirty();
    }
    this.updateValidators(false);
  }

  updateValidators(emitEvent?: boolean): void {
    const authMethod: RepositoryAuthMethod = this.repositorySettingsForm.get('authMethod').value;
    const privateKeyFileName: string = this.repositorySettingsForm.get('privateKeyFileName').value;
    if (authMethod === RepositoryAuthMethod.USERNAME_PASSWORD) {
      this.repositorySettingsForm.get('username').enable({emitEvent});
      if (this.changePassword || !this.showChangePassword) {
        this.repositorySettingsForm.get('password').enable({emitEvent});
      } else {
        this.repositorySettingsForm.get('password').disable({emitEvent});
      }
      this.repositorySettingsForm.get('privateKeyFileName').disable({emitEvent});
      this.repositorySettingsForm.get('privateKey').disable({emitEvent});
      this.repositorySettingsForm.get('privateKeyPassword').disable({emitEvent});
    } else {
      this.repositorySettingsForm.get('username').disable({emitEvent});
      this.repositorySettingsForm.get('password').disable({emitEvent});
      this.repositorySettingsForm.get('privateKeyFileName').enable({emitEvent});
      this.repositorySettingsForm.get('privateKey').enable({emitEvent});
      if (this.changePrivateKeyPassword || !this.showChangePrivateKeyPassword) {
        this.repositorySettingsForm.get('privateKeyPassword').enable({emitEvent});
      } else {
        this.repositorySettingsForm.get('privateKeyPassword').disable({emitEvent});
      }
      if (isNotEmptyStr(privateKeyFileName)) {
        this.repositorySettingsForm.get('privateKey').clearValidators();
      } else {
        this.repositorySettingsForm.get('privateKey').setValidators([Validators.required]);
      }
    }
    this.repositorySettingsForm.get('username').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('password').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKeyFileName').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKey').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKeyPassword').updateValueAndValidity({emitEvent: false});
  }

}
