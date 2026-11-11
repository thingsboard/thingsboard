// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, DestroyRef, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { FormGroupDirective, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import {
  RepositoryAuthMethod,
  repositoryAuthMethodTranslationMap,
  RepositorySettings
} from '@shared/models/settings.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { ActionAuthUpdateHasRepository } from '@core/auth/auth.actions';
import { selectHasRepository } from '@core/auth/auth.selectors';
import { catchError, mergeMap, take } from 'rxjs/operators';
import { of } from 'rxjs';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-repository-settings',
    templateUrl: './repository-settings.component.html',
    styleUrls: ['./repository-settings.component.scss', './../../pages/admin/settings-card.scss'],
    standalone: false
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

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.VERSION_CONTROL, Operation.WRITE);
  allowDelete = this.userPermissionsService.hasGenericPermission(Resource.VERSION_CONTROL, Operation.DELETE);

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
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
      privateKeyFileName: [null, []],
      privateKey: [null, [Validators.required]],
      privateKeyPassword: [null, []]
    });
    this.updateValidators(false);
    this.repositorySettingsForm.get('authMethod').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(true);
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
          this.repositorySettingsForm.reset(this.settings);
          this.updateValidators(false);
        }
    });
    if (this.readonly) {
      this.repositorySettingsForm.disable({emitEvent: false});
    }
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
            formDirective.resetForm();
            this.repositorySettingsForm.reset({ defaultBranch: 'main', authMethod: RepositoryAuthMethod.USERNAME_PASSWORD });
            this.updateValidators(false);
            this.store.dispatch(new ActionAuthUpdateHasRepository({ hasRepository: false }));
          }
        );
      }
    });
  }

  updateValidators(emitEvent?: boolean): void {
    if (this.readonly) {
      return;
    }
    const authMethod: RepositoryAuthMethod = this.repositorySettingsForm.get('authMethod').value;
    if (authMethod === RepositoryAuthMethod.USERNAME_PASSWORD) {
      this.repositorySettingsForm.get('username').enable({emitEvent});
      this.repositorySettingsForm.get('password').enable({emitEvent});
      this.repositorySettingsForm.get('privateKeyFileName').disable({emitEvent});
      this.repositorySettingsForm.get('privateKey').disable({emitEvent});
      this.repositorySettingsForm.get('privateKeyPassword').disable({emitEvent});
    } else {
      this.repositorySettingsForm.get('username').disable({emitEvent});
      this.repositorySettingsForm.get('password').disable({emitEvent});
      this.repositorySettingsForm.get('privateKeyFileName').enable({emitEvent});
      this.repositorySettingsForm.get('privateKey').enable({emitEvent});
      this.repositorySettingsForm.get('privateKeyPassword').enable({emitEvent});
    }
    this.repositorySettingsForm.get('username').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('password').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKeyFileName').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKey').updateValueAndValidity({emitEvent: false});
    this.repositorySettingsForm.get('privateKeyPassword').updateValueAndValidity({emitEvent: false});
  }

}
