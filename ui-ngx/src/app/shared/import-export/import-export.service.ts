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

import { Inject, Injectable, DOCUMENT } from '@angular/core';
import { DashboardService } from '@core/http/dashboard.service';
import { TranslateService } from '@ngx-translate/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { BreakpointId, Dashboard, DashboardLayoutId } from '@shared/models/dashboard.models';
import { deepClone, guid, isDefined, isNotEmptyStr, isObject, isString, isUndefined } from '@core/utils';

import {
  AliasesInfo,
  AliasFilterType,
  EntityAlias,
  EntityAliases,
  EntityAliasFilter,
  EntityAliasInfo
} from '@shared/models/alias.models';
import { MatDialog } from '@angular/material/dialog';
import { ImportDialogComponent, ImportDialogData } from '@shared/import-export/import-dialog.component';
import { forkJoin, Observable, of, Subject } from 'rxjs';
import { catchError, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { EntityService } from '@core/http/entity.service';
import { Widget, WidgetSize, WidgetTypeDetails } from '@shared/models/widget.models';
import { ItemBufferService, WidgetItem } from '@core/services/item-buffer.service';
import {
  BulkImportRequest,
  BulkImportResult,
  CSV_TYPE,
  FileType,
  ImportWidgetResult,
  JSON_TYPE,
  TEXT_TYPE,
  WidgetsBundleItem,
  ZIP_TYPE
} from './import-export.models';
import { EntityType } from '@shared/models/entity-type.models';
import { UtilsService } from '@core/services/utils.service';
import { WidgetService } from '@core/http/widget.service';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import {
  EntityInfoData,
  ImportEntitiesResultInfo,
  ImportEntityData,
  VersionedEntity
} from '@shared/models/entity.models';
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
import { RuleNode } from '@shared/models/rule-node.models';
import { AssetProfileService } from '@core/http/asset-profile.service';
import { AssetProfile } from '@shared/models/asset.models';
import { ImageService } from '@core/http/image.service';
import { ImageExportData, ImageResourceInfo, ImageResourceType } from '@shared/models/resource.models';
import { selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';
import { ExportableEntity } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { Customer } from '@shared/models/customer.model';
import {
  ExportResourceDialogComponent,
  ExportResourceDialogData,
  ExportResourceDialogDialogResult
} from '@shared/import-export/export-resource-dialog.component';
import { FormProperty, propertyValid } from '@shared/models/dynamic-form.models';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { CalculatedField } from '@shared/models/calculated-field.models';

export type editMissingAliasesFunction = (widgets: Array<Widget>, isSingleWidget: boolean,
                                          customTitle: string, missingEntityAliases: EntityAliases) => Observable<EntityAliases>;

type SupportEntityResources = 'includeResourcesInExportWidgetTypes' | 'includeResourcesInExportDashboard' | 'includeBundleWidgetsInExport';

// @dynamic
@Injectable()
export class ImportExportService {

  constructor(@Inject(DOCUMENT) private document: Document,
              private store: Store<AppState>,
              private translate: TranslateService,
              private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService,
              private widgetService: WidgetService,
              private deviceProfileService: DeviceProfileService,
              private assetProfileService: AssetProfileService,
              private tenantProfileService: TenantProfileService,
              private entityService: EntityService,
              private ruleChainService: RuleChainService,
              private deviceService: DeviceService,
              private assetService: AssetService,
              private edgeService: EdgeService,
              private imageService: ImageService,
              private utils: UtilsService,
              private itembuffer: ItemBufferService,
              private calculatedFieldsService: CalculatedFieldsService,
              private dialog: MatDialog) {

  }

  public exportFormProperties(properties: FormProperty[], fileName: string) {
    this.exportToPc(properties, fileName);
  }

  public importFormProperties(): Observable<FormProperty[]> {
    return this.openImportDialog('dynamic-form.import-form',
      'dynamic-form.json-file', true, 'dynamic-form.json-content').pipe(
      map((properties: FormProperty[]) => {
        if (!this.validateImportedFormProperties(properties)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('dynamic-form.invalid-form-json-file-error'),
              type: 'error'}));
          throw new Error('Invalid form JSON file');
        } else {
          return properties;
        }
      }),
      catchError(() => of(null))
    );
  }

  public exportImage(type: ImageResourceType, key: string) {
    this.imageService.exportImage(type, key).subscribe(
      {
        next: (imageData) => {
          const name = imageData.fileName.split('.')[0];
          this.exportToPc(imageData, name);
        },
        error: (e) => {
          this.handleExportError(e, 'image.export-failed-error');
        }
      }
    );
  }

  public importImage(): Observable<ImageResourceInfo> {
    return this.openImportDialog('image.import-image', 'image.image-json-file').pipe(
      mergeMap((imageData: ImageExportData) => {
        if (!this.validateImportedImage(imageData)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('image.invalid-image-json-file-error'),
              type: 'error'}));
          throw new Error('Invalid image JSON file');
        } else {
          return this.imageService.importImage(imageData);
        }
      }),
      catchError(() => of(null))
    );
  }

  public exportCalculatedField(calculatedFieldId: string): void {
    this.calculatedFieldsService.getCalculatedFieldById(calculatedFieldId).subscribe({
      next: (calculatedField) => {
        this.exportToPc(this.prepareCalculatedFieldExport(calculatedField), calculatedField.name, true);
      },
      error: (e) => {
        this.handleExportError(e, 'calculated-fields.export-failed-error');
      }
    });
  }

  public openCalculatedFieldImportDialog(): Observable<CalculatedField> {
    return this.openImportDialog('calculated-fields.import', 'calculated-fields.file').pipe(
      catchError(() => of(null)),
    );
  }

  public exportDashboard(dashboardId: string) {
    this.getIncludeResourcesPreference('includeResourcesInExportDashboard').subscribe(includeResources => {
      this.openExportDialog('dashboard.export', 'dashboard.export-prompt', includeResources).subscribe(result => {
        if (result) {
          this.updateUserSettingsIncludeResourcesIfNeeded(includeResources, result.include, 'includeResourcesInExportDashboard');
          this.dashboardService.exportDashboard(dashboardId, result.include).subscribe({
            next: (dashboard) => {
              this.exportToPc(this.prepareDashboardExport(dashboard), dashboard.title, true);
            },
            error: (e) => {
              this.handleExportError(e, 'dashboard.export-failed-error');
            }
          });
        }
      })
    })
  }

  public importDashboard(onEditMissingAliases: editMissingAliasesFunction): Observable<Dashboard> {
    return this.openImportDialog('dashboard.import', 'dashboard.dashboard-file').pipe(
      mergeMap((dashboard: Dashboard) => {
        if (!this.validateImportedDashboard(dashboard)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('dashboard.invalid-dashboard-file-error'),
              type: 'error'}));
          throw new Error('Invalid dashboard file');
        } else {
          dashboard = this.prepareImport(dashboard);
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
                  return onEditMissingAliases(this.dashboardUtils.getWidgetsArray(dashboard),
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
      catchError(() => of(null))
    );
  }

  public exportWidget(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId, widget: Widget,
                      widgetTitle: string, breakpoint: BreakpointId) {
    const widgetItem = this.itembuffer.prepareWidgetItem(dashboard, sourceState, sourceLayout, widget, breakpoint);
    const widgetDefaultName = this.widgetService.getWidgetInfoFromCache(widget.typeFullFqn).widgetName;
    const fileName = widgetDefaultName + (isNotEmptyStr(widgetTitle) ? `_${widgetTitle}` : '');
    this.exportToPc(this.prepareExport(widgetItem), fileName, true);
  }

  public importWidget(dashboard: Dashboard, targetState: string,
                      onEditMissingAliases: editMissingAliasesFunction,
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
          let widget = this.prepareImport(widgetItem.widget);
          widget = this.dashboardUtils.validateAndUpdateWidget(widget);
          widget.id = guid();
          const aliasesInfo = this.prepareAliasesInfo(widgetItem.aliasesInfo);
          const filtersInfo: FiltersInfo = widgetItem.filtersInfo || {
            datasourceFilters: {}
          };
          const originalColumns = widgetItem.originalColumns;
          const originalSize = widgetItem.originalSize;

          const datasourceAliases = aliasesInfo.datasourceAliases;
          if (datasourceAliases || aliasesInfo.targetDeviceAlias) {
            const entityAliases: EntityAliases = {};
            const datasourceAliasesMap: {[aliasId: string]: number} = {};
            let targetDeviceAliasId: string;
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
            if (aliasesInfo.targetDeviceAlias) {
              aliasId = this.utils.guid();
              targetDeviceAliasId = aliasId;
              entityAliases[aliasId] = {id: aliasId, ...aliasesInfo.targetDeviceAlias};
            }
            const aliasIds = Object.keys(entityAliases);
            if (aliasIds.length > 0) {
              return this.processEntityAliases(entityAliases, aliasIds).pipe(
                mergeMap((missingEntityAliases) => {
                    if (Object.keys(missingEntityAliases).length > 0) {
                      onEditMissingAliases([widget],
                        false, 'dashboard.widget-import-missing-aliases-title',
                        missingEntityAliases).pipe(
                        mergeMap((updatedEntityAliases) => {
                          for (const id of Object.keys(updatedEntityAliases)) {
                            const entityAlias = updatedEntityAliases[id];
                            let index: number;
                            if (isDefined(datasourceAliasesMap[id])) {
                              index = datasourceAliasesMap[id];
                              datasourceAliases[index] = entityAlias;
                            } else if (targetDeviceAliasId === id) {
                              aliasesInfo.targetDeviceAlias = entityAlias;
                            }
                          }
                          return this.addImportedWidget(dashboard, targetState, targetLayoutFunction, widget,
                            aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction, originalColumns, originalSize, widgetItem.widgetExportInfo);
                        }
                      ));
                    } else {
                      return this.addImportedWidget(dashboard, targetState, targetLayoutFunction, widget,
                        aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction, originalColumns, originalSize, widgetItem.widgetExportInfo);
                    }
                  }
                )
              );
            } else {
              return this.addImportedWidget(dashboard, targetState, targetLayoutFunction, widget,
                aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction, originalColumns, originalSize, widgetItem.widgetExportInfo);
            }
          } else {
            return this.addImportedWidget(dashboard, targetState, targetLayoutFunction, widget,
              aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction, originalColumns, originalSize, widgetItem.widgetExportInfo);
          }
        }
      }),
      catchError(() => of(null))
    );
  }

  public exportWidgetType(widgetTypeId: string) {
    this.getIncludeResourcesPreference('includeResourcesInExportWidgetTypes').subscribe(includeResources => {
      this.openExportDialog('widget.export', 'widget.export-prompt', includeResources).subscribe(result => {
        if (result) {
          this.updateUserSettingsIncludeResourcesIfNeeded(includeResources, result.include, 'includeResourcesInExportWidgetTypes');
          this.widgetService.exportWidgetType(widgetTypeId, result.include).subscribe({
            next: (widgetTypeDetails) => {
              this.exportToPc(this.prepareExport(widgetTypeDetails), widgetTypeDetails.name, true);
            },
            error: (e) => {
              this.handleExportError(e, 'widget-type.export-failed-error');
            }
          });
        }
      })
    });
  }

  public exportWidgetTypes(widgetTypeIds: string[]): Observable<void> {
    return this.getIncludeResourcesPreference('includeResourcesInExportWidgetTypes').pipe(
      mergeMap(includeResources =>
        this.openExportDialog('widget.export-widgets', 'widget.export-widgets-prompt', includeResources).pipe(
          mergeMap(result => {
            if (result) {
              this.updateUserSettingsIncludeResourcesIfNeeded(includeResources, result.include, 'includeResourcesInExportWidgetTypes');
              const widgetTypesObservables: Array<Observable<WidgetTypeDetails>> = [];
              for (const id of widgetTypeIds) {
                widgetTypesObservables.push(this.widgetService.exportWidgetType(id, result.include));
              }
              return forkJoin(widgetTypesObservables).pipe(
                map((widgetTypes) =>
                  Object.fromEntries(widgetTypes.map(wt => {
                    let name = wt.name;
                    name = name.toLowerCase().replace(/\W/g, '_') + `.${JSON_TYPE.extension}`;
                    const data = JSON.stringify(this.prepareExport(wt), null, 2);
                    return [name, data];
                  }))),
                mergeMap(widgetTypeFiles => this.exportJSZip(widgetTypeFiles, 'widget_types')),
                catchError(e => {
                  this.handleExportError(e, 'widget-type.export-failed-error');
                  throw e;
                })
              );
            }
          })
        )
      )
    );
  }

  public importWidgetType(): Observable<WidgetTypeDetails> {
    return this.openImportDialog('widget.import', 'widget-type.widget-file').pipe(
      mergeMap((widgetTypeDetails: WidgetTypeDetails) => {
        if (!this.validateImportedWidgetTypeDetails(widgetTypeDetails)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('widget-type.invalid-widget-file-error'),
              type: 'error'}));
          throw new Error('Invalid widget file');
        } else {
          return this.widgetService.saveImportedWidgetTypeDetails(this.prepareImport(widgetTypeDetails));
        }
      }),
      catchError(() => of(null))
    );
  }

  public exportWidgetsBundle(widgetsBundleId: string) {
    const tasks = {
      includeBundleWidgetsInExport: this.store.pipe(select(selectUserSettingsProperty( 'includeBundleWidgetsInExport'))).pipe(take(1)),
      widgetsBundle: this.widgetService.exportWidgetsBundle(widgetsBundleId)
    };

    forkJoin(tasks).subscribe({
      next: ({includeBundleWidgetsInExport, widgetsBundle}) => {
        this.handleExportWidgetsBundle(widgetsBundle, includeBundleWidgetsInExport);
      },
      error: (e) => {
        this.handleExportError(e, 'widgets-bundle.export-failed-error');
      }
    });
  }

  public exportEntity(entityData: VersionedEntity): void {
    const id = (entityData as EntityInfoData).id ?? (entityData as RuleChainMetaData).ruleChainId;
    let fileName = (entityData as EntityInfoData).name;
    let preparedData: any;
    switch (id.entityType) {
      case EntityType.DEVICE_PROFILE:
      case EntityType.ASSET_PROFILE:
        preparedData = this.prepareProfileExport(entityData as DeviceProfile | AssetProfile);
        break;
      case EntityType.RULE_CHAIN:
        forkJoin([this.ruleChainService.getRuleChainMetadata(id.id), this.ruleChainService.getRuleChain(id.id)])
          .pipe(
            take(1),
            map(([ruleChainMetaData, ruleChain]) => {
              const ruleChainExport: RuleChainImport = {
                ruleChain: this.prepareRuleChain(ruleChain),
                metadata: this.prepareRuleChainMetaData(ruleChainMetaData)
              };
              return ruleChainExport;
            }))
          .subscribe(this.onRuleChainExported());
        return;
      case EntityType.WIDGETS_BUNDLE:
        this.exportSelectedWidgetsBundle(entityData as WidgetsBundle);
        return;
      case EntityType.DASHBOARD:
        preparedData = this.prepareDashboardExport(entityData as Dashboard);
        break;
      case EntityType.CUSTOMER:
        fileName = (entityData as Customer).title;
        preparedData = this.prepareExport(entityData);
        break;
      default:
        preparedData = this.prepareExport(entityData);
    }
    this.exportToPc(preparedData, fileName);
  }

  private exportSelectedWidgetsBundle(widgetsBundle: WidgetsBundle): void {
    this.store.pipe(select(selectUserSettingsProperty( 'includeBundleWidgetsInExport'))).pipe(take(1)).subscribe({
      next: (includeBundleWidgetsInExport) => {
        this.handleExportWidgetsBundle(widgetsBundle, includeBundleWidgetsInExport, true);
      },
      error: (e) => {
        this.handleExportError(e, 'widgets-bundle.export-failed-error');
      }
    });
  }

  private handleExportWidgetsBundle(widgetsBundle: WidgetsBundle, includeBundleWidgetsInExport: boolean, ignoreLoading?: boolean): void {
    this.openExportDialog('widgets-bundle.export', 'widgets-bundle.export-widgets-bundle-widgets-prompt',
      includeBundleWidgetsInExport, ignoreLoading).subscribe((result) => {
        if (result) {
          this.updateUserSettingsIncludeResourcesIfNeeded(includeBundleWidgetsInExport, result.include, 'includeBundleWidgetsInExport');
          if (result.include) {
            this.exportWidgetsBundleWithWidgetTypes(widgetsBundle);
          } else {
            this.exportWidgetsBundleWithWidgetTypeFqns(widgetsBundle);
          }
        }
      }
    );
  }

  private exportWidgetsBundleWithWidgetTypes(widgetsBundle: WidgetsBundle) {
    this.widgetService.exportBundleWidgetTypesDetails(widgetsBundle.id.id).subscribe({
      next: (widgetTypesDetails) => {
        const widgetsBundleItem: WidgetsBundleItem = {
          widgetsBundle: this.prepareExport(widgetsBundle),
          widgetTypes: []
        };
        for (const widgetTypeDetails of widgetTypesDetails) {
          widgetsBundleItem.widgetTypes.push(this.prepareExport(widgetTypeDetails));
        }
        this.exportToPc(widgetsBundleItem, widgetsBundle.title, true);
      },
      error: (e) => {
        this.handleExportError(e, 'widgets-bundle.export-failed-error');
      }
    });
  }

  private exportWidgetsBundleWithWidgetTypeFqns(widgetsBundle: WidgetsBundle) {
    this.widgetService.getBundleWidgetTypeFqns(widgetsBundle.id.id).subscribe({
      next: (widgetTypeFqns) => {
        const widgetsBundleItem: WidgetsBundleItem = {
          widgetsBundle: this.prepareExport(widgetsBundle),
          widgetTypeFqns
        };
        this.exportToPc(widgetsBundleItem, widgetsBundle.title, true);
      },
      error: (e) => {
        this.handleExportError(e, 'widgets-bundle.export-failed-error');
      }
    });
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
          const widgetsBundle = this.prepareImport(widgetsBundleItem.widgetsBundle);
          return this.widgetService.saveWidgetsBundle(widgetsBundle).pipe(
            mergeMap((savedWidgetsBundle) => {
              if (widgetsBundleItem.widgetTypes?.length || widgetsBundleItem.widgetTypeFqns?.length) {
                let widgetTypesObservable: Observable<Array<WidgetTypeDetails>>;
                if (widgetsBundleItem.widgetTypes?.length) {
                  const widgetTypesDetails = widgetsBundleItem.widgetTypes;
                  const saveWidgetTypesObservables: Array<Observable<WidgetTypeDetails>> = [];
                  for (const widgetTypeDetails of widgetTypesDetails) {
                    saveWidgetTypesObservables.push(
                      this.widgetService.saveImportedWidgetTypeDetails(this.prepareWidgetType(widgetTypeDetails, savedWidgetsBundle))
                    );
                  }
                  widgetTypesObservable = forkJoin(saveWidgetTypesObservables);
                } else {
                  widgetTypesObservable = of([]);
                }
                return widgetTypesObservable.pipe(
                  switchMap((widgetTypes) => {
                    let widgetTypeFqns = widgetTypes.map(w => w.fqn);
                    if (widgetsBundleItem.widgetTypeFqns?.length) {
                      widgetTypeFqns = widgetTypeFqns.concat(widgetsBundleItem.widgetTypeFqns);
                    }
                    if (widgetTypeFqns.length) {
                      return this.widgetService.updateWidgetsBundleWidgetFqns(savedWidgetsBundle.id.id, widgetTypeFqns).pipe(
                        map(() => savedWidgetsBundle)
                      );
                    } else {
                      return of(savedWidgetsBundle);
                    }
                  })
                );
              } else {
                return of(savedWidgetsBundle);
              }
            }
          ));
        }
      }),
      catchError(() => of(null))
    );
  }

  private prepareWidgetType(widgetType: WidgetTypeDetails & {alias?: string}, widgetsBundle: WidgetsBundle): WidgetTypeDetails {
    if (!widgetType.fqn) {
      widgetType.fqn = `${widgetsBundle.alias}.${widgetType.alias
                                                  ? widgetType.alias
                                                  : widgetType.name.toLowerCase().replace(/\W/g, '_')}`;
    }
    return widgetType;
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
      const saveEntityPromise = this.entityService.saveEntityParameters(entityType, entitiesData[i], updateData, config);
      const importEntityPromise = saveEntityPromise.pipe(
          tap(() => {
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
            map((response) => this.sumObject(statisticalInfo, response))
          );
        } else {
          return of(statisticalInfo);
        }
      })
    );
  }

  public exportRuleChain(ruleChainId: string) {
    this.ruleChainService.getRuleChain(ruleChainId).pipe(
      mergeMap(ruleChain => this.ruleChainService.getRuleChainMetadata(ruleChainId).pipe(
        map((ruleChainMetaData) => {
          const ruleChainExport: RuleChainImport = {
            ruleChain: this.prepareRuleChain(ruleChain),
            metadata: this.prepareRuleChainMetaData(ruleChainMetaData)
          };
          return ruleChainExport;
        })
      ))
    ).subscribe(this.onRuleChainExported());
  }

  private onRuleChainExported() {
    return {
      next: (ruleChainExport: RuleChainImport) => {
        this.exportToPc(ruleChainExport, ruleChainExport.ruleChain.name, true);
      },
      error: (e: any) => {
        this.handleExportError(e, 'rulechain.export-failed-error');
      }
    };
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
          return this.processOldRuleChainConnections(ruleChainImport);
        }
      }),
      catchError(() => of(null))
    );
  }

  private processOldRuleChainConnections({ruleChain, metadata}: RuleChainImport): Observable<RuleChainImport> {
    ruleChain = this.prepareImport(ruleChain);
    metadata = {
      ...metadata,
      nodes: metadata.nodes.map(({ debugMode, ...node }: RuleNode & { debugMode: boolean }) => {
        return debugMode ? { ...node, debugSettings: { failuresEnabled: true, allEnabled: true} } : node
      })
    };
    if ((metadata as any).ruleChainConnections) {
      const ruleChainNameResolveObservables: Observable<void>[] = [];
      for (const ruleChainConnection of (metadata as any).ruleChainConnections) {
        if (ruleChainConnection.targetRuleChainId && ruleChainConnection.targetRuleChainId.id) {
          const ruleChainNode: RuleNode = {
            name: '',
            singletonMode: false,
            type: 'org.thingsboard.rule.engine.flow.TbRuleChainInputNode',
            configuration: {
              ruleChainId: ruleChainConnection.targetRuleChainId.id
            },
            additionalInfo: ruleChainConnection.additionalInfo
          };
          ruleChainNameResolveObservables.push(this.ruleChainService.getRuleChain(ruleChainNode.configuration.ruleChainId,
            {ignoreErrors: true, ignoreLoading: true}).pipe(
              catchError(() => of({name: 'Rule Chain Input'} as RuleChain)),
              map((ruleChain => {
                ruleChainNode.name = ruleChain.name;
                return null;
              })
            )
          ));
          const toIndex = metadata.nodes.length;
          metadata.nodes.push(ruleChainNode);
          metadata.connections.push({
            toIndex,
            fromIndex: ruleChainConnection.fromIndex,
            type: ruleChainConnection.type
          });
        }
      }
      if (ruleChainNameResolveObservables.length) {
        return forkJoin(ruleChainNameResolveObservables).pipe(
           map(() => ({ruleChain, metadata}))
        );
      } else {
        return of({ruleChain, metadata});
      }
    } else {
      return of({ruleChain, metadata});
    }
  }

  public exportDeviceProfile(deviceProfileId: string) {
    this.deviceProfileService.exportDeviceProfile(deviceProfileId).subscribe({
      next: (deviceProfile) => {
        this.exportToPc(this.prepareProfileExport(deviceProfile), deviceProfile.name, true);
      },
      error: (e) => {
        this.handleExportError(e, 'device-profile.export-failed-error');
      }
    });
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
            return this.deviceProfileService.saveDeviceProfile(this.prepareImport(deviceProfile));
        }
      }),
      catchError(() => of(null))
    );
  }

  public exportAssetProfile(assetProfileId: string) {
    this.assetProfileService.exportAssetProfile(assetProfileId).subscribe({
      next: (assetProfile) => {
        this.exportToPc(this.prepareProfileExport(assetProfile), assetProfile.name, true);
      },
      error: (e) => {
        this.handleExportError(e, 'asset-profile.export-failed-error');
      }
    });
  }

  public importAssetProfile(): Observable<AssetProfile> {
    return this.openImportDialog('asset-profile.import', 'asset-profile.asset-profile-file').pipe(
      mergeMap((assetProfile: AssetProfile) => {
        if (!this.validateImportedAssetProfile(assetProfile)) {
          this.store.dispatch(new ActionNotificationShow(
            {message: this.translate.instant('asset-profile.invalid-asset-profile-file-error'),
              type: 'error'}));
          throw new Error('Invalid asset profile file');
        } else {
          return this.assetProfileService.saveAssetProfile(this.prepareImport(assetProfile));
        }
      }),
      catchError(() => of(null))
    );
  }

  public exportTenantProfile(tenantProfileId: string) {
    this.tenantProfileService.getTenantProfile(tenantProfileId).subscribe({
      next: (tenantProfile) => {
        this.exportToPc(this.prepareProfileExport(tenantProfile), tenantProfile.name, true);
      },
      error: (e) => {
        this.handleExportError(e, 'tenant-profile.export-failed-error');
      }
    });
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
          return this.tenantProfileService.saveTenantProfile(this.prepareImport(tenantProfile));
        }
      }),
      catchError(() => of(null))
    );
  }

  private processCSVCell(cellData: any): any {
    if (isString(cellData)) {
      let result = cellData.replace(/"/g, '""');
      if (result.search(/([",;\n])/g) >= 0) {
        result = `"${result}"`;
      }
      return result;
    }
    return cellData;
  }

  public exportCsv(data: {[key: string]: any}[], filename: string, normalizeFileName = false) {
    let colsHead: string;
    let colsData: string;
    if (data && data.length) {
      colsHead = Object.keys(data[0]).map(key => [this.processCSVCell(key)]).join(';');
      colsData = data.map(obj => [
        Object.keys(obj).map(col => [
          this.processCSVCell(obj[col])
        ]).join(';')
      ]).join('\n');
    } else {
      colsHead = '';
      colsData = '';
    }
    const csvData = `${colsHead}\n${colsData}`;
    this.downloadFile(csvData, filename, CSV_TYPE, normalizeFileName);
  }

  public exportText(data: string | Array<string>, filename: string, normalizeFileName = false) {
    let content = data;
    if (Array.isArray(data)) {
      content = data.join('\n');
    }
    this.downloadFile(content, filename, TEXT_TYPE, normalizeFileName);
  }

  public exportJSZip(data: object, filename: string, normalizeFileName = false): Observable<void> {
    const exportJsSubjectSubject = new Subject<void>();
    import('jszip').then((JSZip) => {
      try {
        const jsZip = new JSZip.default();
        for (const keyName in data) {
          if (data.hasOwnProperty(keyName)) {
            const valueData = data[keyName];
            jsZip.file(keyName, valueData);
          }
        }
        jsZip.generateAsync({type: 'blob'}).then(content => {
          this.downloadFile(content, filename, ZIP_TYPE, normalizeFileName);
          exportJsSubjectSubject.next(null);
        }).catch((e: any) => {
            exportJsSubjectSubject.error(e);
        });
      } catch (e) {
          exportJsSubjectSubject.error(e);
      }
    });
    return exportJsSubjectSubject.asObservable();
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

  private validateImportedAssetProfile(assetProfile: AssetProfile): boolean {
    if (isUndefined(assetProfile.name)) {
      return false;
    }
    return true;
  }

  private validateImportedTenantProfile(tenantProfile: TenantProfile): boolean {
    return isDefined(tenantProfile.name)
      && isDefined(tenantProfile.profileData)
      && isDefined(tenantProfile.isolatedTbRuleEngine);
  }

  private sumObject<T>(obj1: T, obj2: T): T {
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

  private validateImportedFormProperties(properties: FormProperty[]): boolean {
    if (!properties.length) {
      return false;
    } else {
      return !properties.some(p => !propertyValid(p));
    }
  }

  private validateImportedImage(image: ImageExportData): boolean {
    return !(!isNotEmptyStr(image.data)
      || !isNotEmptyStr(image.title)
      || !isNotEmptyStr(image.fileName)
      || !isNotEmptyStr(image.mediaType)
      || !isNotEmptyStr(image.resourceKey));
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
    if ((isUndefined(widget.typeFullFqn) && (isUndefined((widget as any).isSystemType) ||
                                             isUndefined((widget as any).bundleAlias) ||
                                             isUndefined((widget as any).typeAlias))) ||
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
    if (isUndefined(widgetsBundleItem.widgetTypes) && isUndefined(widgetsBundleItem.widgetTypeFqns)) {
      return false;
    }
    const widgetsBundle = widgetsBundleItem.widgetsBundle;
    if (isUndefined(widgetsBundle.title)) {
      return false;
    }
    if (isDefined(widgetsBundleItem.widgetTypes)) {
      const widgetTypesDetails = widgetsBundleItem.widgetTypes;
      for (const widgetTypeDetails of widgetTypesDetails) {
        if (!this.validateImportedWidgetTypeDetails(widgetTypeDetails)) {
          return false;
        }
      }
    }
    if (isDefined(widgetsBundleItem.widgetTypeFqns)) {
      if (!Array.isArray(widgetsBundleItem.widgetTypeFqns)) {
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
                            originalColumns: number, originalSize: WidgetSize, widgetExportInfo: any): Observable<ImportWidgetResult> {
    return targetLayoutFunction().pipe(
      mergeMap((targetLayout) => this.itembuffer.addWidgetToDashboard(dashboard, targetState, targetLayout,
          widget, aliasesInfo, filtersInfo, onAliasesUpdateFunction, onFiltersUpdateFunction,
          originalColumns, originalSize, -1, -1, 'default', widgetExportInfo).pipe(
          map(() => ({widget, layoutId: targetLayout} as ImportWidgetResult))
        )
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

  private prepareAliasesInfo(aliasesInfo: AliasesInfo): AliasesInfo {
    const datasourceAliases = aliasesInfo.datasourceAliases;
    if (datasourceAliases || aliasesInfo.targetDeviceAlias) {
      if (datasourceAliases) {
        for (const strIndex of Object.keys(datasourceAliases)) {
          const datasourceIndex = Number(strIndex);
          datasourceAliases[datasourceIndex] = this.prepareEntityAlias(datasourceAliases[datasourceIndex]);
        }
      }
      if (aliasesInfo.targetDeviceAlias) {
        aliasesInfo.targetDeviceAlias = this.prepareEntityAlias(aliasesInfo.targetDeviceAlias);
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

  private openImportDialog(importTitle: string, importFileLabel: string,
                           enableImportFromContent = false, importContentLabel?: string): Observable<any> {
    return this.dialog.open<ImportDialogComponent, ImportDialogData,
      any>(ImportDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        importTitle,
        importFileLabel,
        enableImportFromContent,
        importContentLabel
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

  private exportToPc(data: any, filename: string, normalizeFileName = false) {
    if (!data) {
      console.error('No data');
      return;
    }
    this.exportJson(data, filename, normalizeFileName);
  }

  public exportJson(data: any, filename: string, normalizeFileName = false) {
    if (isObject(data)) {
      data = JSON.stringify(data, null,  2);
    }
    this.downloadFile(data, filename, JSON_TYPE, normalizeFileName);
  }

  private prepareFilename(filename: string, extension: string, normalizeFileName = false): string {
    if (normalizeFileName) {
      filename = filename.toLowerCase().replace(/\s/g, '_');
    }
    filename = filename.replace(/[\\/<>:"|?*\s]/g, '_');
    return `${filename}.${extension}`;
  }

  private downloadFile(data: any, filename = 'download', fileType: FileType, normalizeFileName: boolean) {
    filename = this.prepareFilename(filename, fileType.extension, normalizeFileName);
    const blob = new Blob([data], {type: fileType.mimeType});
    const url = URL.createObjectURL(blob);

    const a = this.document.createElement('a');
    a.href = url;
    a.download = filename;
    a.dataset.downloadurl = [fileType.mimeType, a.download, a.href].join(':');
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  private prepareDashboardExport(dashboard: Dashboard): Dashboard {
    dashboard = this.dashboardUtils.validateAndUpdateDashboard(dashboard);
    dashboard = this.prepareExport(dashboard);
    delete dashboard.assignedCustomers;
    return dashboard;
  }

  private prepareProfileExport<T extends DeviceProfile|AssetProfile|TenantProfile>(profile: T): T {
    profile = this.prepareExport(profile);
    profile.default = false;
    return profile;
  }

  private prepareCalculatedFieldExport(calculatedField: CalculatedField): CalculatedField {
    delete calculatedField.entityId;
    return this.prepareExport(calculatedField);
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
    if (isDefined(exportedData.externalId)) {
      delete exportedData.externalId;
    }
    if (isDefined(exportedData.version)) {
      delete exportedData.version;
    }
    return exportedData;
  }

  private prepareImport<T extends ExportableEntity<EntityId>>(data: T): T{
    const importedData = deepClone(data);
    if (isDefined(importedData.externalId)) {
      delete importedData.externalId;
    }
    return importedData;
  }

  private getIncludeResourcesPreference(key: SupportEntityResources): Observable<boolean> {
    return this.store.pipe(
      select(selectUserSettingsProperty(key)),
      take(1)
    );
  }

  private openExportDialog(title: string, prompt: string, includeResources: boolean, ignoreLoading?: boolean) {
    return this.dialog.open<ExportResourceDialogComponent, ExportResourceDialogData, ExportResourceDialogDialogResult>(
      ExportResourceDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { title, prompt, include: includeResources, ignoreLoading }
      }
    ).afterClosed();
  }

  private updateUserSettingsIncludeResourcesIfNeeded(currentValue: boolean, newValue: boolean, key: SupportEntityResources) {
    if (currentValue !== newValue) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({[key]: newValue }));
    }
  }

}
