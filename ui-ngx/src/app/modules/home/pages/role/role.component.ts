// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, DestroyRef, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatDialog } from '@angular/material/dialog';
import { Role } from '@shared/models/role.models';
import { RoleType, roleTypeTranslationMap } from '@shared/models/security.models';
import { isEqual } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.scss'],
  standalone: false
})
export class RoleComponent extends EntityComponent<Role> {

  roleType = RoleType;

  roleTypes = Object.values(RoleType);

  roleTypeTranslations = roleTypeTranslationMap;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private dialog: MatDialog,
              @Inject('entity') protected entityValue: Role,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Role>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Role): UntypedFormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        type: [entity ? entity.type : null, [Validators.required]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        ),
        genericPermissions: [entity && entity.type === RoleType.GENERIC ?
          {permissions: entity.permissions, excludedPermissions: entity.excludedPermissions} : null, []],
        groupPermissions: [entity && entity.type === RoleType.GROUP ? entity.permissions : null, []]
      }
    );
    this.togglePermissionControls(form);
    form.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((newVal) => {
      this.roleTypeChanged(form, newVal);
    });
    return form;
  }

  private roleTypeChanged(form: UntypedFormGroup, newVal: RoleType) {
    if (this.isEdit) {
      const prevVal: RoleType = form.value.type;
      if (!isEqual(newVal, prevVal)) {
          form.get('genericPermissions').patchValue({permissions: {}, excludedPermissions: {}}, {emitEvent: false});
          form.get('groupPermissions').patchValue([], {emitEvent: false});
          this.togglePermissionControls(form);
      }
    }
  }

  private togglePermissionControls(form: UntypedFormGroup) {
    const roleType: RoleType = form.get('type').value;
    if (roleType === RoleType.GENERIC) {
      if (this.isEdit) {
        form.get('genericPermissions').enable({emitEvent: false});
      }
      form.get('groupPermissions').disable({emitEvent: false});
    } else if (roleType === RoleType.GROUP) {
      form.get('genericPermissions').disable({emitEvent: false});
      if (this.isEdit) {
        form.get('groupPermissions').enable({emitEvent: false});
      }
    } else {
      form.get('genericPermissions').disable({emitEvent: false});
      form.get('groupPermissions').disable({emitEvent: false});
    }
  }

  updateForm(entity: Role) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type}, {emitEvent: false});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
    this.entityForm.patchValue({genericPermissions: entity.type === RoleType.GENERIC ?
      {permissions: entity.permissions, excludedPermissions: entity.excludedPermissions} : null});
    this.entityForm.patchValue({groupPermissions: entity.type === RoleType.GROUP ? entity.permissions : null});
    this.togglePermissionControls(this.entityForm);
  }

  prepareFormValue(formValue: any): any {
    const roleType: RoleType = formValue.type;
    if (roleType === RoleType.GENERIC) {
      formValue.permissions = formValue.genericPermissions?.permissions;
      formValue.excludedPermissions = formValue.genericPermissions?.excludedPermissions;
    } else if (roleType === RoleType.GROUP) {
      formValue.permissions = formValue.groupPermissions;
      formValue.excludedPermissions = null;
    }
    delete formValue.genericPermissions;
    delete formValue.groupPermissions;
    return super.prepareFormValue(formValue);
  }

  onRoleIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('role.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
