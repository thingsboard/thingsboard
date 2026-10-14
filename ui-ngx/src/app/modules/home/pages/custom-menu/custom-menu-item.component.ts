// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import {
  CMItemLinkType,
  CMItemType,
  cmItemTypes,
  cmItemTypeTranslations,
  cmLinkTypes,
  cmLinkTypeTranslations,
  CMScope,
  CustomMenuItem
} from '@shared/models/custom-menu.models';
import { merge } from 'rxjs';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-custom-menu-item',
    templateUrl: './custom-menu-item.component.html',
    styleUrls: ['./custom-menu-item.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CustomMenuItemComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CustomMenuItemComponent),
            multi: true
        }
    ],
    standalone: false
})
export class CustomMenuItemComponent implements ControlValueAccessor, OnInit, Validator {

  CMItemType = CMItemType;

  CMItemLinkType = CMItemLinkType;

  cmItemTypes = cmItemTypes;

  cmItemTypeTranslations = cmItemTypeTranslations;

  cmLinkTypes = cmLinkTypes;

  cmLinkTypeTranslations = cmLinkTypeTranslations;

  @Input()
  disabled: boolean;

  @Input()
  scope: CMScope;

  @Input()
  @coerceBoolean()
  menuItemTypeEditable = true;

  linkTypeEditable = false;

  modelValue: CustomMenuItem;

  menuItemFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private store: Store<AppState>,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.menuItemFormGroup = this.fb.group({
      visible: [true, []],
      icon: [null, [Validators.required]],
      name: [null, [Validators.required]],
      menuItemType: [null, [Validators.required]],
      linkType: [null, [Validators.required]],
      url: [null, [Validators.required]],
      setAccessToken: [false, []],
      dashboardId: [null, [Validators.required]],
      hideDashboardToolbar: [true, []]
    });

    const authUser = getCurrentAuthUser(this.store);
    this.linkTypeEditable = authUser.authority !== Authority.SYS_ADMIN && this.scope !== CMScope.SYSTEM;

    merge(this.menuItemFormGroup.get('menuItemType').valueChanges,
      this.menuItemFormGroup.get('linkType').valueChanges
    ).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
    });
    this.menuItemFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.menuItemFormGroup.disable({emitEvent: false});
    } else {
      this.menuItemFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: CustomMenuItem): void {
    this.modelValue = value;
    this.menuItemFormGroup.patchValue(
      {
        visible: value.visible,
        icon: value.icon,
        name: value.name,
        menuItemType: value.menuItemType,
        linkType: value.linkType,
        url: value.url,
        setAccessToken: value.setAccessToken,
        dashboardId: value.dashboardId,
        hideDashboardToolbar: value.hideDashboardToolbar
      }, {emitEvent: false}
    );
    this.updateValidators();
  }

  public validate(_c: UntypedFormControl) {
    if (!this.menuItemFormGroup.valid) {
      return {
        invalidCustomMenuItem: true
      };
    }
    return null;
  }

  private updateValidators() {
    const menuItemType: CMItemType = this.menuItemFormGroup.get('menuItemType').value;
    if (menuItemType === CMItemType.SECTION) {
      this.menuItemFormGroup.get('linkType').disable({emitEvent: false});
      this.menuItemFormGroup.get('url').disable({emitEvent: false});
      this.menuItemFormGroup.get('setAccessToken').disable({emitEvent: false});
      this.menuItemFormGroup.get('dashboardId').disable({emitEvent: false});
      this.menuItemFormGroup.get('hideDashboardToolbar').disable({emitEvent: false});
    } else {
      this.menuItemFormGroup.get('linkType').enable({emitEvent: false});
      const linkType: CMItemLinkType = this.menuItemFormGroup.get('linkType').value;
      if (linkType === CMItemLinkType.URL) {
        this.menuItemFormGroup.get('url').enable({emitEvent: false});
        this.menuItemFormGroup.get('setAccessToken').enable({emitEvent: false});
        this.menuItemFormGroup.get('dashboardId').disable({emitEvent: false});
        this.menuItemFormGroup.get('hideDashboardToolbar').disable({emitEvent: false});
      } else {
        this.menuItemFormGroup.get('dashboardId').enable({emitEvent: false});
        this.menuItemFormGroup.get('hideDashboardToolbar').enable({emitEvent: false});
        this.menuItemFormGroup.get('url').disable({emitEvent: false});
        this.menuItemFormGroup.get('setAccessToken').disable({emitEvent: false});
      }
    }
  }

  private updateModel() {
    this.modelValue.visible = this.menuItemFormGroup.get('visible').value;
    this.modelValue.icon = this.menuItemFormGroup.get('icon').value;
    this.modelValue.name = this.menuItemFormGroup.get('name').value;
    this.modelValue.menuItemType = this.menuItemFormGroup.get('menuItemType').value;
    if (this.modelValue.menuItemType === CMItemType.SECTION) {
      delete this.modelValue.linkType;
      delete this.modelValue.url;
      delete this.modelValue.setAccessToken;
      delete this.modelValue.dashboardId;
      delete this.modelValue.hideDashboardToolbar;
    } else {
      this.modelValue.linkType = this.menuItemFormGroup.get('linkType').value;
      if (this.modelValue.linkType === CMItemLinkType.URL) {
        this.modelValue.url = this.menuItemFormGroup.get('url').value;
        this.modelValue.setAccessToken = this.menuItemFormGroup.get('setAccessToken').value;
        delete this.modelValue.dashboardId;
        delete this.modelValue.hideDashboardToolbar;
      } else {
        this.modelValue.dashboardId = this.menuItemFormGroup.get('dashboardId').value;
        this.modelValue.hideDashboardToolbar = this.menuItemFormGroup.get('hideDashboardToolbar').value;
        delete this.modelValue.url;
        delete this.modelValue.setAccessToken;
      }
    }
    this.propagateChange(this.modelValue);
  }
}
