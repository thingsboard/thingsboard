// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { CustomMenuRoutingModule } from '@home/pages/custom-menu/custom-menu-routing.module';
import { CustomMenuTableHeaderComponent } from '@home/pages/custom-menu/custom-menu-table-header.component';
import { CustomMenuConfigComponent } from '@home/pages/custom-menu/custom-menu-config.component';
import { CustomMenuTableComponent } from '@home/pages/custom-menu/custom-menu-table.component';
import { ManageCustomMenuDialogComponent } from '@home/pages/custom-menu/manage-custom-menu-dialog.component';
import { EditCustomMenuNamePanelComponent } from '@home/pages/custom-menu/edit-custom-menu-name-panel.component';
import { CustomMenuIsAssignedDialogComponent } from '@home/pages/custom-menu/custom-menu-is-assigned.dialog.component';
import { CustomMenuItemRowComponent } from '@home/pages/custom-menu/custom-menu-item-row.component';
import { DefaultMenuItemPanelComponent } from '@home/pages/custom-menu/default-menu-item-panel.component';
import { CustomMenuItemComponent } from '@home/pages/custom-menu/custom-menu-item.component';
import { AddCustomMenuItemDialogComponent } from '@home/pages/custom-menu/add-custom-menu-item.dialog.component';
import { CustomMenuItemPanelComponent } from '@home/pages/custom-menu/custom-menu-item-panel.component';

@NgModule({
  declarations: [
    CustomMenuTableHeaderComponent,
    CustomMenuTableComponent,
    ManageCustomMenuDialogComponent,
    CustomMenuIsAssignedDialogComponent,
    EditCustomMenuNamePanelComponent,
    CustomMenuConfigComponent,
    CustomMenuItemRowComponent,
    DefaultMenuItemPanelComponent,
    CustomMenuItemComponent,
    AddCustomMenuItemDialogComponent,
    CustomMenuItemPanelComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    CustomMenuRoutingModule
  ]
})
export class CustomMenuModule { }
