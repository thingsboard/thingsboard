///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn,
  EntityChipsEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { MobileAppBundleInfo } from '@shared/models/mobile-app.models';
import { ActivatedRouteSnapshot } from '@angular/router';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { MobileBundleTableHeaderComponent } from '@home/pages/mobile/bundes/mobile-bundle-table-header.component';
import { DatePipe } from '@angular/common';
import { MobileAppService } from '@core/http/mobile-app.service';
import { map, take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import {
  MobileBundleDialogComponent,
  MobileBundleDialogData
} from '@home/pages/mobile/bundes/mobile-bundle-dialog.component';
import {
  MobileAppConfigurationDialogComponent,
  MobileAppConfigurationDialogData
} from '@home/pages/mobile/bundes/mobile-app-configuration-dialog.component';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { forkJoin, of } from 'rxjs';

@Injectable()
export class MobileBundleTableConfigResolver {

  private readonly config: EntityTableConfig<MobileAppBundleInfo> = new EntityTableConfig<MobileAppBundleInfo>();

  constructor(
    private datePipe: DatePipe,
    private mobileAppService: MobileAppService,
    private translate : TranslateService,
    private dialog: MatDialog,
    private store: Store<AppState>
  ) {
    this.config.selectionEnabled = false;
    this.config.entityType = EntityType.MOBILE_APP_BUNDLE;
    this.config.addEnabled = false;
    this.config.rowPointer = true;
    this.config.detailsPanelEnabled = false;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.MOBILE_APP_BUNDLE);
    this.config.entityResources = entityTypeResources.get(EntityType.MOBILE_APP_BUNDLE);
    this.config.headerComponent = MobileBundleTableHeaderComponent;
    this.config.onEntityAction = action => this.onBundleAction(action);
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<MobileAppBundleInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<MobileAppBundleInfo>('title', 'mobile.title', '25%'),
      new EntityChipsEntityTableColumn<MobileAppBundleInfo>('oauth2ClientInfos', 'mobile.oauth-clients', '35%'),
      new EntityChipsEntityTableColumn<MobileAppBundleInfo>('androidPkg', 'mobile.android-app', '20%'),
      new EntityChipsEntityTableColumn<MobileAppBundleInfo>('iosPkg', 'mobile.ios-app', '20%'),
      new EntityTableColumn<MobileAppBundleInfo>('oauth2Enabled', 'mobile.enable-oauth', '140px',
        entity => checkBoxCell(entity.oauth2Enabled))
    )

    this.config.deleteEnabled = bundle => !(bundle.iosAppId || bundle.androidAppId);
    this.config.deleteEntityTitle = (bundle) => this.translate.instant('mobile.delete-applications-bundle-title', {bundleName: bundle.name});
    this.config.deleteEntityContent = () => this.translate.instant('mobile.delete-applications-bundle-text');
    this.config.deleteEntity = id => this.mobileAppService.deleteMobileAppBundle(id.id);

    this.config.entitiesFetchFunction = pageLink => this.mobileAppService.getTenantMobileAppBundleInfos(pageLink).pipe(
      map(bundles => {
        bundles.data.map(data => {
          if (data.androidPkgName) {
            data.androidPkg = {
              id: data.androidAppId,
              name: data.androidPkgName
            }
          }
          if (data.iosPkgName) {
            data.iosPkg = {
              id: data.iosAppId,
              name: data.iosPkgName
            }
          }
        })
        return bundles;
      })
    );

    this.config.handleRowClick = ($event, bundle) => {
      $event?.stopPropagation();
      this.mobileAppService.getMobileAppBundleInfoById(bundle.id.id).subscribe(appBundleInfo => {
        this.editBundle($event, appBundleInfo);
      })
      return true;
    };

    this.config.cellActionDescriptors = this.configureCellActions();
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<MobileAppBundleInfo> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<MobileAppBundleInfo>> {
    return [
      {
        name: this.translate.instant('mobile.configuration-app'),
        icon: 'code',
        isEnabled: () => true,
        onAction: ($event, entity) => this.configurationApp($event, entity)
      }
    ];
  }

  private editBundle($event: Event, bundle: MobileAppBundleInfo, isAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<MobileBundleDialogComponent, MobileBundleDialogData,
      MobileAppBundleInfo>(MobileBundleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        bundle
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res && !isAdd) {
          this.config.updateData();
        } else {
          this.store.pipe(select(selectUserSettingsProperty('notDisplayConfigurationAfterAddMobileBundle'))).pipe(
            take(1)
          ).subscribe((settings: boolean) => {
            if (!settings) {
              this.configurationApp(null, res, true);
            } else {
              this.config.updateData();
            }
          });
        }
      });
  }

  private onBundleAction(action: EntityAction<MobileAppBundleInfo>): boolean {
    switch (action.action) {
      case 'add':
        this.editBundle(action.event, action.entity, true);
        return true;
    }
    return false;
  }

  private configurationApp($event: Event, entity: MobileAppBundleInfo, afterAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    const task = {
      androidApp: entity.androidAppId ? this.mobileAppService.getMobileAppInfoById(entity.androidAppId.id) : of(null),
      iosApp: entity.iosAppId ? this.mobileAppService.getMobileAppInfoById(entity.iosAppId.id) : of(null)
    };
    forkJoin(task).subscribe(data => {
      this.dialog.open<MobileAppConfigurationDialogComponent, MobileAppConfigurationDialogData,
        MobileAppBundleInfo>(MobileAppConfigurationDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          afterAdd,
          appSecret: data.androidApp?.appSecret || data.iosApp?.appSecret
        }
      }).afterClosed()
        .subscribe(() => {
          if (afterAdd) {
            this.config.updateData();
          }
        });
    })
  }
}
