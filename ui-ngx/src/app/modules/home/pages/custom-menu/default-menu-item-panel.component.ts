// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import {
  CMScope,
  DefaultMenuItem,
  HomeMenuItem,
  HomeMenuItemType,
  homeMenuItemTypes,
  homeMenuItemTypeTranslations,
  isHomeMenuItem
} from '@shared/models/custom-menu.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MenuSection, menuSectionMap } from '@core/services/menu.models';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-default-menu-item-panel',
    templateUrl: './default-menu-item-panel.component.html',
    styleUrls: ['./default-menu-item-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DefaultMenuItemPanelComponent implements OnInit {

  HomeMenuItemType = HomeMenuItemType;

  homeMenuItemTypes = homeMenuItemTypes;

  homeMenuItemTypeTranslations = homeMenuItemTypeTranslations;

  @Input()
  disabled: boolean;

  @Input()
  scope: CMScope;

  @Input()
  menuItem: DefaultMenuItem;

  @Input()
  popover: TbPopoverComponent<DefaultMenuItemPanelComponent>;

  @Output()
  defaultMenuItemApplied = new EventEmitter<DefaultMenuItem>();

  menuItemFormGroup: UntypedFormGroup;

  defaultItemName: string;

  isHomeMenuItem = false;

  isHomeTypeEditable = false;

  isCleanupEnabled = false;

  private defaultMenuSection: MenuSection;

  constructor(private fb: UntypedFormBuilder,
              private store: Store<AppState>,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    if (isHomeMenuItem(this.menuItem)) {
      this.isHomeMenuItem = true;
    }
    this.defaultMenuSection = menuSectionMap.get(this.menuItem.id);
    this.defaultItemName = this.translate.instant(this.defaultMenuSection.name);

    this.menuItemFormGroup = this.fb.group({
      visible: [this.menuItem.visible, []],
      icon: [this.menuItem.icon ? this.menuItem.icon : this.defaultMenuSection.icon, []],
      name: [this.menuItem.name, []],
    });

    if (this.isHomeMenuItem) {
      const authUser = getCurrentAuthUser(this.store);
      this.isHomeTypeEditable = authUser.authority !== Authority.SYS_ADMIN && this.scope !== CMScope.SYSTEM;

      this.menuItemFormGroup.get('visible').disable({emitEvent: false});
      const homeMenuItem = this.menuItem as HomeMenuItem;
      this.menuItemFormGroup.setControl('homeType',
        this.fb.control(this.isHomeTypeEditable ? homeMenuItem.homeType : HomeMenuItemType.DEFAULT), {emitEvent: false});
      this.menuItemFormGroup.setControl('dashboardId',
        this.fb.control(homeMenuItem.dashboardId, [Validators.required]), {emitEvent: false});
      this.menuItemFormGroup.setControl('hideDashboardToolbar',
        this.fb.control(isDefinedAndNotNull(homeMenuItem.hideDashboardToolbar) ?
          homeMenuItem.hideDashboardToolbar : true), {emitEvent: false});
      this.homeTypeChanged();
      if (!this.disabled) {
        if (this.isHomeTypeEditable) {
          this.menuItemFormGroup.get('homeType').valueChanges.pipe(
            takeUntilDestroyed(this.destroyRef)
          ).subscribe(() => {
            this.homeTypeChanged();
          });
        } else {
          this.menuItemFormGroup.get('homeType').disable({emitEvent: false});
        }
      }
    }

    if (this.disabled) {
      this.menuItemFormGroup.disable({emitEvent: false});
    } else {
      this.menuItemFormGroup.valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateModel();
      });
      this.updateCleanupState();
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    this.defaultMenuItemApplied.emit(this.menuItem);
  }

  cleanup() {
    this.menuItemFormGroup.patchValue(
      {
        visible: true,
        icon: this.defaultMenuSection.icon,
        name: null
      }, {emitEvent: false}
    );
    if (this.isHomeMenuItem) {
      this.menuItemFormGroup.patchValue(
        {
          homeType: HomeMenuItemType.DEFAULT,
          dashboardId: null,
          hideDashboardToolbar: true
        }, {emitEvent: false}
      );
      this.homeTypeChanged();
    }
    this.menuItemFormGroup.markAsDirty();
    this.updateModel();
  }

  private updateModel() {
    if (!this.isHomeMenuItem) {
      this.menuItem.visible = this.menuItemFormGroup.get('visible').value;
    }
    const name = this.menuItemFormGroup.get('name').value;
    if (name) {
      this.menuItem.name = name;
    } else {
      delete this.menuItem.name;
    }
    const icon = this.menuItemFormGroup.get('icon').value;
    if (icon !== this.defaultMenuSection.icon) {
      this.menuItem.icon = icon;
    } else {
      delete this.menuItem.icon;
    }

    if (this.isHomeMenuItem) {
      const homeMenuItem = this.menuItem as HomeMenuItem;
      homeMenuItem.homeType = this.menuItemFormGroup.get('homeType').value;
      if (homeMenuItem.homeType === HomeMenuItemType.DEFAULT) {
        delete homeMenuItem.dashboardId;
        delete homeMenuItem.hideDashboardToolbar;
      } else {
        homeMenuItem.dashboardId = this.menuItemFormGroup.get('dashboardId').value;
        homeMenuItem.hideDashboardToolbar = this.menuItemFormGroup.get('hideDashboardToolbar').value;
      }
    }
    this.updateCleanupState();
  }

  private updateCleanupState() {
    this.isCleanupEnabled = !this.menuItem.visible ||
      !!this.menuItem.name ||
      !!this.menuItem.icon ||
      (this.isHomeMenuItem && (this.menuItem as HomeMenuItem).homeType !== HomeMenuItemType.DEFAULT);
  }

  private homeTypeChanged() {
    const homeType: HomeMenuItemType = this.menuItemFormGroup.get('homeType').value;
    if (homeType === HomeMenuItemType.DEFAULT) {
      this.menuItemFormGroup.get('dashboardId').disable({emitEvent: false});
      this.menuItemFormGroup.get('hideDashboardToolbar').disable({emitEvent: false});
    } else {
      this.menuItemFormGroup.get('dashboardId').enable({emitEvent: false});
      this.menuItemFormGroup.get('hideDashboardToolbar').enable({emitEvent: false});
    }
  }
}
