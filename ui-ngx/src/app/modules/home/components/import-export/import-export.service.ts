///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Inject, Injectable } from '@angular/core';
import { DashboardService } from '@core/http/dashboard.service';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Dashboard, DashboardLayoutId } from '@shared/models/dashboard.models';
import { deepClone, isDefined, isObject, isString, isUndefined } from '@core/utils';
import { WINDOW } from '@core/services/window.service';
import { DOCUMENT } from '@angular/common';
import {
  AliasesInfo,
  AliasFilterType,
  EntityAlias,
  EntityAliases,
  EntityAliasFilter,
  EntityAliasInfo
} from '@shared/models/alias.models';
import { MatDialog } from '@angular/material/dialog';
import { ImportDialogComponent, ImportDialogData } from '@home/components/import-export/import-dialog.component';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { EntityService } from '@core/http/entity.service';
import { Widget, WidgetSize, WidgetType, WidgetTypeDetails } from '@shared/models/widget.models';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';
import { ItemBufferService, WidgetItem } from '@core/services/item-buffer.service';
import { FileType, ImportWidgetResult, JSON_TYPE, WidgetsBundleItem, ZIP_TYPE, BulkImportRequest, BulkImportResult } from './import-export.models';
import { EntityType } from '@shared/models/entity-type.models';
import { UtilsService } from '@core/services/utils.service';
import { WidgetService } from '@core/http/widget.service';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { ImportEntitiesResultInfo, ImportEntityData } from '@shared/models/entity.models';
import { RequestConfig } from '@core/http/http-utils';
import { RuleChain, RuleChainImport, RuleChainMetaData, RuleChainType } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { FiltersInfo } from '@shared/models/query/query.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { DeviceProfile } from '@shared/models/device.models';
import { TenantProfile } from '@shared/models/tenant.model';
import { TenantProfileService } from '@core/http/tenant-profile.service';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { EdgeService } from '@core/http/edge.service';

// @dynamic
@Injectable()
export class ImportExportService {

  constructor(@Inject(WINDOW) private window: Window,
              @Inject(DOCUMENT) private document: Document,
              private store: Store<AppState>,
              private translate: TranslateService,
              private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService,
              private widgetService: WidgetService,
              private deviceProfileService: DeviceProfileService,
              private tenantProfileService: TenantProfileService,
              private entityService: EntityService,
              private ruleChainService: RuleChainService,
              private deviceService: DeviceService,
              private assetService: AssetService,
              private edgeService: EdgeService,
              private utils: UtilsService,
              private itembuffer: ItemBufferService,
              private dialog: MatDialog) {

  }

  public exportDashboard(dashboardId: string) {
    this.dashboardService.getDashboard(dashboardId).subscribe(
      (dashboard) => {
        let name = dashboard.title;
        name = name.toLowerCase().replace(/\W/g, '_');
        this.exportToPc(this.prepareDashboardExport(dashboard), name);
      },
      (e) => {
        this.handleExportError(e, 'dashboard.export-failed-error');
      }
    );
  }

  public importDashboard(): Observable<Dashboard> {
    return this.openImportDialog('dashboard.import', 'dashboard.dashboard-file').pipe(
      mergeMap((dashboard: Dashboard) => {
        if (!this.validateImportedDashboard(dashboard)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('dashboard.invalid-dashboard-file-error'),
              type: 'error'}));
          throw new Error('Invalid dashboard file');
        } else {
          dashboard = this.dashboardUtils.validateAndUpdateDashboard(dashboard);
          let aliasIds = null;
          const entityAliases = dashboard.configuration.entityAliases;
          if (entityAliases) {
            aliasIds = Object.keys(entityAliases);
          }
          if (aliasIds && aliasIds.length > 0) {
            return this.processEntityAliases(entityAliases, aliasIds).pipe(
              mergeMap((missingEntityAliases) => {
                if (Object.keys(missingEntityAliases).length > 0) {
                  return this.editMissingAliases(this.dashboardUtils.getWidgetsArray(dashboard),
                    false, 'dashboard.dashboard-import-missing-aliases-title',
                    missingEntityAliases).pipe(
                    mergeMap((updatedEntityAliases) => {
                      for (const aliasId of Object.keys(updatedEntityAliases)) {
                        entityAliases[aliasId] = updatedEntityAliases[aliasId];
                      }
                      return this.saveImportedDashboard(dashboard);
                    })
                  );
                } else {
                  return this.saveImportedDashboard(dashboard);
                }
              }
             ));
          } else {
            return this.saveImportedDashboard(dashboard);
          }
        }
      }),
      catchError((err) => {
        return of(null);
      })
    );
  }

  public exportWidget(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId, widget: Widget) {
    const widgetItem = this.itembuffer.prepareWidgetItem(dashboard, sourceState, sourceLayout, widget);
    let name = widgetItem.widget.config.title;
    name = name.toLowerCase().replace(/\W/g, '_');
    this.exportToPc(this.prepareExport(widgetItem), name);
  }

  public importWidget(dashboard: Dashboard, targetState: string,
                      targetLayoutFunction: () => Observable<DashboardLayoutId>,
                      onAliasesUpdateFunction: () => void,
                      onFiltersUpdateFunction: () => void): Observable<ImportWidgetResult> {
    return this.openImportDialog('dashboard.import-widget', 'dashboard.widget-file').pipe(
      mergeMap((widgetItem: WidgetItem) => {
        if (!this.validateImportedWidget(widgetItem)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('dashboard.invalid-widget-file-error'),
              type: 'error'}));
          throw new Error('Invalid widget file');
        } else {
          let widget = widgetItem.widget;
          widget = this.dashboardUtils.validateAndUpdateWidget(widget);
          const aliasesInfo = this.prepareAliasesInfo(widgetItem.aliasesInfo);
          const filtersInfo: FiltersInfo = widgetItem.filtersInfo || {
            datasourceFilters: {}
          };
          const originalColumns = widgetItem.originalColumns;
          const originalSize = widgetItem.originalSize;

          const datasourceAliases = aliasesInfo.datasourceAliases;
          const targetDeviceAliases = aliasesInfo.targetDeviceAliases;
          if (datasourceAliases || targetDeviceAliases) {
            const entityAliases: EntityAliases = {};
            const datasourceAliasesMap: {[aliasId: string]: number} = {};
            const targetDeviceAliasesMap: {[aliasId: string]: number} = {};
            let aliasId: string;
            let datasourceIndex: number;
            if (datasourceAliases) {
              for (const strIndex of Object.keys(datasourceAliases)) {
                datasourceIndex = Number(strIndex);
                aliasId = this.utils.guid();
                datasourceAliasesMap[aliasId] = datasourceIndex;
                entityAliases[aliasId] = {id: aliasId, ...datasourceAliases[datasourceIndex]};
              }
            }
            if (targetDeviceAliases) {
              for (const strIndex of Object.keys(targetDeviceAliases)) {
                datasourceIndex = Number(strIndex);
                aliasId = this.utils.guid();
                targetDeviceAliasesMap[aliasId] = datasourceIndex;
                entityAliases[aliasId] = {id: aliasId, ...targetDeviceAliases[datasourceIndex]};
              }
            }
            const aliasIds = Object.keys(entityAliases);
            if (aliasIds.length > 0) {
              return this.processEntityAliases(entityAliases, aliasIds).pipe(
                mergeMap((missingEntityAliases) => {
                    if (Object.keys(missingEntityAliases).length > 0) {
                      return this.editMissingAliases([widget],
                        false, 'dashboard.widget-import-missing-aliases-title',
                        missingEntityAliases).pipe(
                        mergeMap((updatedEntityAliases) => {
                          for (const id of Object.keys(updatedEntityAliases)) {
                            const entityAlias = updatedEntityAliases[id];
                            let index;
                            if (isDefined(datasourceAliasesMap[id])) {
                              index = datasourceAliasesMap[id];
                              datasourceAliases[index] = entityAlias;
                            } else if (isDefined(targetDeviceAliasesMap[id])) {
                              index = targetDeviceAliasesMap[id];
                              targetDeviceAliases[index] = entityAlias;
                            }
                          }
                          return this.addImportedWidget(dashboard, targetState, targetLayoutFunction, widget,
                            aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction, originalColumns, originalSize);
                        }
                      ));
                    } else {
                      return this.addImportedWidget(dashboard, targetState, targetLayoutFunction, widget,
                        aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction, originalColumns, originalSize);
                    }
                  }
                )
              );
            } else {
              return this.addImportedWidget(dashboard, targetState, targetLayoutFunction, widget,
                aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction, originalColumns, originalSize);
            }
          } else {
            return this.addImportedWidget(dashboard, targetState, targetLayoutFunction, widget,
              aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction, originalColumns, originalSize);
          }
        }
      }),
      catchError((err) => {
        return of(null);
      })
    );
  }

  public exportWidgetType(widgetTypeId: string) {
    this.widgetService.getWidgetTypeById(widgetTypeId).subscribe(
      (widgetTypeDetails) => {
        if (isDefined(widgetTypeDetails.bundleAlias)) {
          delete widgetTypeDetails.bundleAlias;
        }
        let name = widgetTypeDetails.name;
        name = name.toLowerCase().replace(/\W/g, '_');
        this.exportToPc(this.prepareExport(widgetTypeDetails), name);
      },
      (e) => {
        this.handleExportError(e, 'widget-type.export-failed-error');
      }
    );
  }

  public importWidgetType(bundleAlias: string): Observable<WidgetType> {
    return this.openImportDialog('widget-type.import', 'widget-type.widget-type-file').pipe(
      mergeMap((widgetTypeDetails: WidgetTypeDetails) => {
        if (!this.validateImportedWidgetTypeDetails(widgetTypeDetails)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('widget-type.invalid-widget-type-file-error'),
              type: 'error'}));
          throw new Error('Invalid widget type file');
        } else {
          widgetTypeDetails.bundleAlias = bundleAlias;
          return this.widgetService.saveImportedWidgetTypeDetails(widgetTypeDetails);
        }
      }),
      catchError((err) => {
        return of(null);
      })
    );
  }

  public exportWidgetsBundle(widgetsBundleId: string) {
    this.widgetService.getWidgetsBundle(widgetsBundleId).subscribe(
      (widgetsBundle) => {
        const bundleAlias = widgetsBundle.alias;
        const isSystem = widgetsBundle.tenantId.id === NULL_UUID;
        this.widgetService.getBundleWidgetTypesDetails(bundleAlias, isSystem).subscribe(
          (widgetTypesDetails) => {
            widgetTypesDetails = widgetTypesDetails.sort((a, b) => a.createdTime - b.createdTime);
            const widgetsBundleItem: WidgetsBundleItem = {
              widgetsBundle: this.prepareExport(widgetsBundle),
              widgetTypes: []
            };
            for (const widgetTypeDetails of widgetTypesDetails) {
              if (isDefined(widgetTypeDetails.bundleAlias)) {
                delete widgetTypeDetails.bundleAlias;
              }
              widgetsBundleItem.widgetTypes.push(this.prepareExport(widgetTypeDetails));
            }
            let name = widgetsBundle.title;
            name = name.toLowerCase().replace(/\W/g, '_');
            this.exportToPc(widgetsBundleItem, name);
          },
          (e) => {
            this.handleExportError(e, 'widgets-bundle.export-failed-error');
          }
        );
      },
      (e) => {
        this.handleExportError(e, 'widgets-bundle.export-failed-error');
      }
    );
  }

  public importWidgetsBundle(): Observable<WidgetsBundle> {
    return this.openImportDialog('widgets-bundle.import', 'widgets-bundle.widgets-bundle-file').pipe(
      mergeMap((widgetsBundleItem: WidgetsBundleItem) => {
        if (!this.validateImportedWidgetsBundle(widgetsBundleItem)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('widgets-bundle.invalid-widgets-bundle-file-error'),
              type: 'error'}));
          throw new Error('Invalid widgets bundle file');
        } else {
          const widgetsBundle = widgetsBundleItem.widgetsBundle;
          return this.widgetService.saveWidgetsBundle(widgetsBundle).pipe(
            mergeMap((savedWidgetsBundle) => {
              const bundleAlias = savedWidgetsBundle.alias;
              const widgetTypesDetails = widgetsBundleItem.widgetTypes;
              if (widgetTypesDetails.length) {
                const saveWidgetTypesObservables: Array<Observable<WidgetType>> = [];
                for (const widgetTypeDetails of widgetTypesDetails) {
                  widgetTypeDetails.bundleAlias = bundleAlias;
                  saveWidgetTypesObservables.push(this.widgetService.saveImportedWidgetTypeDetails(widgetTypeDetails));
                }
                return forkJoin(saveWidgetTypesObservables).pipe(
                  map(() => savedWidgetsBundle)
                );
              } else {
                return of(savedWidgetsBundle);
              }
            }
          ));
        }
      }),
      catchError((err) => {
        return of(null);
      })
    );
  }

  public bulkImportEntities(entitiesData: BulkImportRequest, entityType: EntityType, config?: RequestConfig): Observable<BulkImportResult> {
    switch (entityType) {
      case EntityType.DEVICE:
        return this.deviceService.bulkImportDevices(entitiesData, config);
      case EntityType.ASSET:
        return this.assetService.bulkImportAssets(entitiesData, config);
      case EntityType.EDGE:
        return this.edgeService.bulkImportEdges(entitiesData, config);
    }
  }

  public importEntities(entitiesData: ImportEntityData[], entityType: EntityType, updateData: boolean,
                        importEntityCompleted?: () => void, config?: RequestConfig): Observable<ImportEntitiesResultInfo> {
    let partSize = 100;
    partSize = entitiesData.length > partSize ? partSize : entitiesData.length;

    let statisticalInfo: ImportEntitiesResultInfo = {};
    const importEntitiesObservables: Observable<ImportEntitiesResultInfo>[] = [];
    for (let i = 0; i < partSize; i++) {
      let saveEntityPromise: Observable<ImportEntitiesResultInfo>;
      saveEntityPromise = this.entityService.saveEntityParameters(entityType, entitiesData[i], updateData, config);
      const importEntityPromise = saveEntityPromise.pipe(
          tap((res) => {
            if (importEntityCompleted) {
              importEntityCompleted();
            }
          })
        );
      importEntitiesObservables.push(importEntityPromise);
    }
    return forkJoin(importEntitiesObservables).pipe(
      mergeMap((responses) => {
        for (const response of responses) {
          statisticalInfo = this.sumObject(statisticalInfo, response);
        }
        entitiesData.splice(0, partSize);
        if (entitiesData.length) {
          return this.importEntities(entitiesData, entityType, updateData, importEntityCompleted, config).pipe(
            map((response) => {
              return this.sumObject(statisticalInfo, response) as ImportEntitiesResultInfo;
            })
          );
        } else {
          return of(statisticalInfo);
        }
      })
    );
  }

  public exportRuleChain(ruleChainId: string) {
    this.ruleChainService.getRuleChain(ruleChainId).pipe(
      mergeMap(ruleChain => {
        return this.ruleChainService.getRuleChainMetadata(ruleChainId).pipe(
          map((ruleChainMetaData) => {
            const ruleChainExport: RuleChainImport = {
              ruleChain: this.prepareRuleChain(ruleChain),
              metadata: this.prepareRuleChainMetaData(ruleChainMetaData)
            };
            return ruleChainExport;
          })
        );
      })
    ).subscribe((ruleChainExport) => {
        let name = ruleChainExport.ruleChain.name;
        name = name.toLowerCase().replace(/\W/g, '_');
        this.exportToPc(ruleChainExport, name);
    },
      (e) => {
        this.handleExportError(e, 'rulechain.export-failed-error');
      }
    );
  }

  public importRuleChain(expectedRuleChainType: RuleChainType): Observable<RuleChainImport> {
    return this.openImportDialog('rulechain.import', 'rulechain.rulechain-file').pipe(
      mergeMap((ruleChainImport: RuleChainImport) => {
        if (!this.validateImportedRuleChain(ruleChainImport)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('rulechain.invalid-rulechain-file-error'),
              type: 'error'}));
          throw new Error('Invalid rule chain file');
        } else if (ruleChainImport.ruleChain.type !== expectedRuleChainType) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('rulechain.invalid-rulechain-type-error', {expectedRuleChainType}),
              type: 'error'}));
          throw new Error('Invalid rule chain type');
        } else {
          return this.ruleChainService.resolveRuleChainMetadata(ruleChainImport.metadata).pipe(
            map((resolvedMetadata) => {
              ruleChainImport.resolvedMetadata = resolvedMetadata;
              return ruleChainImport;
            })
          );
        }
      }),
      catchError((err) => {
        return of(null);
      })
    );
  }

  public exportDeviceProfile(deviceProfileId: string) {
    this.deviceProfileService.getDeviceProfile(deviceProfileId).subscribe(
      (deviceProfile) => {
          let name = deviceProfile.name;
          name = name.toLowerCase().replace(/\W/g, '_');
          this.exportToPc(this.prepareProfileExport(deviceProfile), name);
        },
        (e) => {
          this.handleExportError(e, 'device-profile.export-failed-error');
        }
      );
  }

  public importDeviceProfile(): Observable<DeviceProfile> {
    return this.openImportDialog('device-profile.import', 'device-profile.device-profile-file').pipe(
      mergeMap((deviceProfile: DeviceProfile) => {
        if (!this.validateImportedDeviceProfile(deviceProfile)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('device-profile.invalid-device-profile-file-error'),
              type: 'error'}));
          throw new Error('Invalid device profile file');
        } else {
            return this.deviceProfileService.saveDeviceProfile(deviceProfile);
        }
      }),
      catchError((err) => {
        return of(null);
      })
    );
  }

  public exportTenantProfile(tenantProfileId: string) {
    this.tenantProfileService.getTenantProfile(tenantProfileId).subscribe(
      (tenantProfile) => {
        let name = tenantProfile.name;
        name = name.toLowerCase().replace(/\W/g, '_');
        this.exportToPc(this.prepareProfileExport(tenantProfile), name);
      },
      (e) => {
        this.handleExportError(e, 'tenant-profile.export-failed-error');
      }
    );
  }

  public importTenantProfile(): Observable<TenantProfile> {
    return this.openImportDialog('tenant-profile.import', 'tenant-profile.tenant-profile-file').pipe(
      mergeMap((tenantProfile: TenantProfile) => {
        if (!this.validateImportedTenantProfile(tenantProfile)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('tenant-profile.invalid-tenant-profile-file-error'),
              type: 'error'}));
          throw new Error('Invalid tenant profile file');
        } else {
          return this.tenantProfileService.saveTenantProfile(tenantProfile);
        }
      }),
      catchError(() => {
        return of(null);
      })
    );
  }

  public exportJSZip(data: object, filename: string) {
    import('jszip').then((JSZip) => {
      const jsZip = new JSZip.default();
      for (const keyName in data) {
        if (data.hasOwnProperty(keyName)) {
          const valueData = data[keyName];
          jsZip.file(keyName, valueData);
        }
      }
      jsZip.generateAsync({type: 'blob'}).then(content => {
        this.downloadFile(content, filename, ZIP_TYPE);
      });
    });
  }

  private prepareRuleChain(ruleChain: RuleChain): RuleChain {
    ruleChain = this.prepareExport(ruleChain);
    if (ruleChain.firstRuleNodeId) {
      ruleChain.firstRuleNodeId = null;
    }
    ruleChain.root = false;
    return ruleChain;
  }

  private prepareRuleChainMetaData(ruleChainMetaData: RuleChainMetaData) {
    delete ruleChainMetaData.ruleChainId;
    for (let i = 0; i < ruleChainMetaData.nodes.length; i++) {
      const node = ruleChainMetaData.nodes[i];
      delete node.ruleChainId;
      ruleChainMetaData.nodes[i] = this.prepareExport(node);
    }
    return ruleChainMetaData;
  }

  private validateImportedRuleChain(ruleChainImport: RuleChainImport): boolean {
    if (isUndefined(ruleChainImport.ruleChain)
      || isUndefined(ruleChainImport.metadata)
      || isUndefined(ruleChainImport.ruleChain.name)) {
      return false;
    }
    if (isUndefined(ruleChainImport.ruleChain.type)) {
      ruleChainImport.ruleChain.type = RuleChainType.CORE;
    }
    return true;
  }

  private validateImportedDeviceProfile(deviceProfile: DeviceProfile): boolean {
    if (isUndefined(deviceProfile.name)
      || isUndefined(deviceProfile.type)
      || isUndefined(deviceProfile.transportType)
      || isUndefined(deviceProfile.provisionType)
      || isUndefined(deviceProfile.profileData)) {
      return false;
    }
    return true;
  }

  private validateImportedTenantProfile(tenantProfile: TenantProfile): boolean {
    return isDefined(tenantProfile.name)
      && isDefined(tenantProfile.profileData)
      && isDefined(tenantProfile.isolatedTbCore)
      && isDefined(tenantProfile.isolatedTbRuleEngine);
  }

  private sumObject(obj1: any, obj2: any): any {
    Object.keys(obj2).map((key) => {
      if (isObject(obj2[key])) {
        obj1[key] = obj1[key] || {};
        obj1[key] = {...obj1[key], ...this.sumObject(obj1[key], obj2[key])};
      } else if (isString(obj2[key])) {
        obj1[key] = (obj1[key] || '') + `${obj2[key]}\n`;
      } else {
        obj1[key] = (obj1[key] || 0) + obj2[key];
      }
    });
    return obj1;
  }

  private handleExportError(e: any, errorDetailsMessageId: string) {
    let message = e;
    if (!message) {
      message = this.translate.instant('error.unknown-error');
    }
    this.store.dispatch(new ActionNotificationShow(
      {message: this.translate.instant(errorDetailsMessageId, {error: message}),
        type: 'error'}));
  }

  private validateImportedDashboard(dashboard: Dashboard): boolean {
    if (isUndefined(dashboard.title) || isUndefined(dashboard.configuration)) {
      return false;
    }
    return true;
  }

  private validateImportedWidget(widgetItem: WidgetItem): boolean {
    if (isUndefined(widgetItem.widget)
      || isUndefined(widgetItem.aliasesInfo)
      || isUndefined(widgetItem.originalColumns)) {
      return false;
    }
    const widget = widgetItem.widget;
    if (isUndefined(widget.isSystemType) ||
      isUndefined(widget.bundleAlias) ||
      isUndefined(widget.typeAlias) ||
      isUndefined(widget.type)) {
      return false;
    }
    return true;
  }

  private validateImportedWidgetTypeDetails(widgetTypeDetails: WidgetTypeDetails): boolean {
    if (isUndefined(widgetTypeDetails.name)
      || isUndefined(widgetTypeDetails.descriptor)) {
      return false;
    }
    return true;
  }

  private validateImportedWidgetsBundle(widgetsBundleItem: WidgetsBundleItem): boolean {
    if (isUndefined(widgetsBundleItem.widgetsBundle)) {
      return false;
    }
    if (isUndefined(widgetsBundleItem.widgetTypes)) {
      return false;
    }
    const widgetsBundle = widgetsBundleItem.widgetsBundle;
    if (isUndefined(widgetsBundle.title)) {
      return false;
    }
    const widgetTypesDetails = widgetsBundleItem.widgetTypes;
    for (const widgetTypeDetails of widgetTypesDetails) {
      if (!this.validateImportedWidgetTypeDetails(widgetTypeDetails)) {
        return false;
      }
    }
    return true;
  }

  private saveImportedDashboard(dashboard: Dashboard): Observable<Dashboard> {
    return this.dashboardService.saveDashboard(dashboard);
  }

  private addImportedWidget(dashboard: Dashboard, targetState: string,
                            targetLayoutFunction: () => Observable<DashboardLayoutId>,
                            widget: Widget, aliasesInfo: AliasesInfo,
                            filtersInfo: FiltersInfo,
                            onAliasesUpdateFunction: () => void,
                            onFiltersUpdateFunction: () => void,
                            originalColumns: number, originalSize: WidgetSize): Observable<ImportWidgetResult> {
    return targetLayoutFunction().pipe(
      mergeMap((targetLayout) => {
        return this.itembuffer.addWidgetToDashboard(dashboard, targetState, targetLayout,
          widget, aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction,
          originalColumns, originalSize, -1, -1).pipe(
          map(() => ({widget, layoutId: targetLayout} as ImportWidgetResult))
        );
      }
    ));
  }

  private processEntityAliases(entityAliases: EntityAliases, aliasIds: string[]): Observable<EntityAliases> {
    const tasks: Observable<EntityAlias>[] = [];
    for (const aliasId of aliasIds) {
      const entityAlias = entityAliases[aliasId];
      tasks.push(
        this.entityService.checkEntityAlias(entityAlias).pipe(
          map((result) => {
            if (!result) {
              const missingEntityAlias = deepClone(entityAlias);
              missingEntityAlias.filter = null;
              return missingEntityAlias;
            }
            return null;
          }
          )
        )
      );
    }
    return forkJoin(tasks).pipe(
      map((missingAliasesArray) => {
          missingAliasesArray = missingAliasesArray.filter(alias => alias !== null);
          const missingEntityAliases: EntityAliases = {};
          for (const missingAlias of missingAliasesArray) {
            missingEntityAliases[missingAlias.id] = missingAlias;
          }
          return missingEntityAliases;
        }
      )
    );
  }

  private editMissingAliases(widgets: Array<Widget>, isSingleWidget: boolean,
                             customTitle: string, missingEntityAliases: EntityAliases): Observable<EntityAliases> {
    return this.dialog.open<EntityAliasesDialogComponent, EntityAliasesDialogData,
      EntityAliases>(EntityAliasesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityAliases: missingEntityAliases,
        widgets,
        customTitle,
        isSingleWidget,
        disableAdd: true
      }
    }).afterClosed().pipe(
      map((updatedEntityAliases) => {
        if (updatedEntityAliases) {
          return updatedEntityAliases;
        } else {
          throw new Error('Unable to resolve missing entity aliases!');
        }
      }
    ));
  }

  private prepareAliasesInfo(aliasesInfo: AliasesInfo): AliasesInfo {
    const datasourceAliases = aliasesInfo.datasourceAliases;
    const targetDeviceAliases = aliasesInfo.targetDeviceAliases;
    if (datasourceAliases || targetDeviceAliases) {
      if (datasourceAliases) {
        for (const strIndex of Object.keys(datasourceAliases)) {
          const datasourceIndex = Number(strIndex);
          datasourceAliases[datasourceIndex] = this.prepareEntityAlias(datasourceAliases[datasourceIndex]);
        }
      }
      if (targetDeviceAliases) {
        for (const strIndex of Object.keys(targetDeviceAliases)) {
          const datasourceIndex = Number(strIndex);
          targetDeviceAliases[datasourceIndex] = this.prepareEntityAlias(targetDeviceAliases[datasourceIndex]);
        }
      }
    }
    return aliasesInfo;
  }

  private prepareEntityAlias(aliasInfo: EntityAliasInfo): EntityAliasInfo {
    let alias: string;
    let filter: EntityAliasFilter;
    if (aliasInfo.deviceId) {
      alias = aliasInfo.aliasName;
      filter = {
        type: AliasFilterType.entityList,
        entityType: EntityType.DEVICE,
        entityList: [aliasInfo.deviceId],
        resolveMultiple: false
      };
    } else if (aliasInfo.deviceFilter) {
      alias = aliasInfo.aliasName;
      filter = {
        type: aliasInfo.deviceFilter.useFilter ? AliasFilterType.entityName : AliasFilterType.entityList,
        entityType: EntityType.DEVICE,
        resolveMultiple: false
      };
      if (filter.type === AliasFilterType.entityList) {
        filter.entityList = aliasInfo.deviceFilter.deviceList;
      } else {
        filter.entityNameFilter = aliasInfo.deviceFilter.deviceNameFilter;
      }
    } else if (aliasInfo.entityFilter) {
      alias = aliasInfo.aliasName;
      filter = {
        type: aliasInfo.entityFilter.useFilter ? AliasFilterType.entityName : AliasFilterType.entityList,
        entityType: aliasInfo.entityType,
        resolveMultiple: false
      };
      if (filter.type === AliasFilterType.entityList) {
        filter.entityList = aliasInfo.entityFilter.entityList;
      } else {
        filter.entityNameFilter = aliasInfo.entityFilter.entityNameFilter;
      }
    } else {
      alias = aliasInfo.alias;
      filter = aliasInfo.filter;
    }
    return {
      alias,
      filter
    };
  }

  private openImportDialog(importTitle: string, importFileLabel: string): Observable<any> {
    return this.dialog.open<ImportDialogComponent, ImportDialogData,
      any>(ImportDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        importTitle,
        importFileLabel
      }
    }).afterClosed().pipe(
      map((importedData) => {
        if (importedData) {
          return importedData;
        } else {
          throw new Error('No file selected!');
        }
      }
    ));
  }

  private exportToPc(data: any, filename: string) {
    if (!data) {
      console.error('No data');
      return;
    }
    this.exportJson(data, filename);
  }

  private exportJson(data: any, filename: string) {
    if (isObject(data)) {
      data = JSON.stringify(data, null,  2);
    }
    this.downloadFile(data, filename, JSON_TYPE);
  }

  private downloadFile(data: any, filename: string, fileType: FileType) {
    if (!filename) {
      filename = 'download';
    }
    filename += '.' + fileType.extension;
    const blob = new Blob([data], {type: fileType.mimeType});
    if (this.window.navigator && this.window.navigator.msSaveOrOpenBlob) {
      this.window.navigator.msSaveOrOpenBlob(blob, filename);
    } else {
      const e = this.document.createEvent('MouseEvents');
      const a = this.document.createElement('a');
      a.download = filename;
      a.href = URL.createObjectURL(blob);
      a.dataset.downloadurl = [fileType.mimeType, a.download, a.href].join(':');
      // @ts-ignore
      e.initEvent('click', true, false, this.window,
        0, 0, 0, 0, 0, false, false, false, false, 0, null);
      a.dispatchEvent(e);
    }
  }

  private prepareDashboardExport(dashboard: Dashboard): Dashboard {
    dashboard = this.prepareExport(dashboard);
    delete dashboard.assignedCustomers;
    return dashboard;
  }

  private prepareProfileExport<T extends DeviceProfile|TenantProfile>(profile: T): T {
    profile = this.prepareExport(profile);
    profile.default = false;
    return profile;
  }

  private prepareExport(data: any): any {
    const exportedData = deepClone(data);
    if (isDefined(exportedData.id)) {
      delete exportedData.id;
    }
    if (isDefined(exportedData.createdTime)) {
      delete exportedData.createdTime;
    }
    if (isDefined(exportedData.tenantId)) {
      delete exportedData.tenantId;
    }
    if (isDefined(exportedData.customerId)) {
      delete exportedData.customerId;
    }
    return exportedData;
  }

}
