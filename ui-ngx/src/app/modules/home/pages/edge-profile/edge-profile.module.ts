// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { EdgeProfileComponent } from '@home/pages/edge-profile/edge-profile.component';
import { EdgeProfileTabsComponent } from '@home/pages/edge-profile/edge-profile-tabs.component';
import { EdgeProfileDialogComponent } from '@home/pages/edge-profile/edge-profile-dialog.component';
import { EdgeProfileAutocompleteComponent } from '@home/pages/edge-profile/edge-profile-autocomplete.component';
import { EdgeProfilesTableConfigResolver } from '@home/pages/edge-profile/edge-profiles-table-config.resolver';

@NgModule({
  declarations: [
    EdgeProfileComponent,
    EdgeProfileTabsComponent,
    EdgeProfileDialogComponent,
    EdgeProfileAutocompleteComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule
  ],
  exports: [
    EdgeProfileAutocompleteComponent
  ],
  providers: [
    EdgeProfilesTableConfigResolver
  ]
})
export class EdgeProfileModule { }
