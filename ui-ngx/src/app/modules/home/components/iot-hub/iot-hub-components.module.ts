// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { TbIotHubItemDetailDialogComponent } from './iot-hub-item-detail-dialog.component';
import { TbIotHubBrowseComponent } from './iot-hub-browse.component';
import { TbIotHubItemCardComponent } from './iot-hub-item-card.component';
import { TbIotHubAddItemDialogComponent } from './iot-hub-add-item-dialog.component';
import { TbIotHubInstallDialogComponent } from './iot-hub-install-dialog.component';
import { TbIotHubUpdateDialogComponent } from './iot-hub-update-dialog.component';
import { TbIotHubDeleteDialogComponent } from './iot-hub-delete-dialog.component';
import { TbIotHubUnpublishedWarningDialogComponent } from './iot-hub-unpublished-warning-dialog.component';
import { TbDeviceInstallDialogComponent } from './device-install-dialog/device-install-dialog.component';
import { InstallFormRendererComponent } from './device-install-dialog/install-form-renderer/install-form-renderer.component';
import { TbIotHubSearchComponent } from './iot-hub-search.component';
import { TbIotHubFacetListComponent } from './iot-hub-facet-list.component';
import { TbIotHubInstalledItemsTableComponent } from './iot-hub-installed-items-table.component';
import { TbIotHubInstalledItemsDialogComponent } from './iot-hub-installed-items-dialog.component';
import { TbIotHubSelectCfEntityDialogComponent } from './iot-hub-select-cf-entity-dialog.component';
import { TbIotHubUpgradeRequiredDialogComponent } from './iot-hub-upgrade-required-dialog.component';
import { TbIotHubMarkdownComponent } from './iot-hub-markdown.component';
import { SolutionInstallDialogComponent } from './solution-install-dialog.component';
import { IotHubActionsService } from './iot-hub-actions.service';
import { IotHubItemLinkModule } from './iot-hub-item-link-card/iot-hub-item-link.module';

@NgModule({
  declarations: [
    TbIotHubItemDetailDialogComponent,
    TbIotHubBrowseComponent,
    TbIotHubItemCardComponent,
    TbIotHubAddItemDialogComponent,
    TbIotHubInstallDialogComponent,
    TbIotHubUpdateDialogComponent,
    TbIotHubDeleteDialogComponent,
    TbIotHubUnpublishedWarningDialogComponent,
    TbDeviceInstallDialogComponent,
    TbIotHubSearchComponent,
    TbIotHubFacetListComponent,
    TbIotHubInstalledItemsTableComponent,
    TbIotHubInstalledItemsDialogComponent,
    TbIotHubSelectCfEntityDialogComponent,
    TbIotHubMarkdownComponent,
    SolutionInstallDialogComponent,
    InstallFormRendererComponent,
    TbIotHubUpgradeRequiredDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    IotHubItemLinkModule
  ],
  providers: [
    IotHubActionsService
  ],
  exports: [
    TbIotHubItemDetailDialogComponent,
    TbIotHubBrowseComponent,
    TbIotHubItemCardComponent,
    TbIotHubAddItemDialogComponent,
    TbIotHubInstallDialogComponent,
    TbIotHubUpdateDialogComponent,
    TbIotHubDeleteDialogComponent,
    TbIotHubUnpublishedWarningDialogComponent,
    TbDeviceInstallDialogComponent,
    TbIotHubSearchComponent,
    TbIotHubFacetListComponent,
    TbIotHubInstalledItemsTableComponent,
    TbIotHubInstalledItemsDialogComponent,
    TbIotHubSelectCfEntityDialogComponent,
    TbIotHubMarkdownComponent,
    SolutionInstallDialogComponent,
    TbIotHubUpgradeRequiredDialogComponent
  ]
})
export class IotHubComponentsModule { }
