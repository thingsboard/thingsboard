// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DestroyRef, Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  Converter,
  converterTypeTranslationMap,
  getConverterHelpLink,
  resolveConverterParams
} from '@shared/models/converter.models';
import { ConverterService } from '@core/http/converter.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { ConverterTabsComponent } from '@home/pages/converter/converter-tabs.component';
import { Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { isUndefined } from '@core/utils';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { integrationTypeInfoMap } from '@shared/models/integration.models';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { catchError, first, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PageLink } from '@shared/models/page/page-link';

@Injectable()
export class ConvertersTableConfigResolver  {

  private readonly config: EntityTableConfig<Converter> = new EntityTableConfig<Converter>();

  constructor(private converterService: ConverterService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private importExport: ImportExportService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private destroyRef: DestroyRef,
              private customTranslate: CustomTranslatePipe) {

    this.config.entityType = EntityType.CONVERTER;
    this.config.entityComponent = ConverterComponent;
    this.config.entityTabsComponent = ConverterTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.CONVERTER);
    this.config.entityResources = {
      helpLinkId: null,
      helpLinkIdForEntity(entity: Converter): string {
        return getConverterHelpLink(entity);
      }
    };
    this.config.addDialogStyle = {width: '750px'};

    this.config.entityTitle = (converter) => converter ?
      this.utils.customTranslation(converter.name, converter.name) : '';

    this.config.columns.push(
      new DateEntityTableColumn<Converter>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Converter>('name', 'converter.name', '35%', this.config.entityTitle),
      new EntityTableColumn<Converter>('type', 'converter.type', '20%', (converter) => {
        return this.translate.instant(converterTypeTranslationMap.get(converter.type));
      }),
      new EntityTableColumn<Converter>('integrationType', 'converter.integration-type', '20%', (converter) => {
        if (integrationTypeInfoMap.has(converter.integrationType)) {
          return this.translate.instant(integrationTypeInfoMap.get(converter.integrationType).name);
        }
        return '';
      }),
      new EntityTableColumn<Converter>('description', 'converter.description', '25%',
        (converter) => this.customTranslate.transform(converter.additionalInfo?.description || ''),
        () => ({}), false),
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('converter.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportConverter($event, entity)
      },
      {
        name: '',
        nameFunction: (entity) => this.entityDebugSettingsService.getDebugConfigLabel(entity?.debugSettings),
        icon: 'mdi:bug',
        isEnabled: () => true,
        iconFunction: ({ debugSettings }) => this.entityDebugSettingsService.isDebugActive(debugSettings?.allEnabledUntil) || debugSettings?.failuresEnabled ? 'mdi:bug' : 'mdi:bug-outline',
        onAction: ($event, entity) => this.onOpenDebugConfig($event, entity),
      }
    );

    this.config.addActionDescriptors.push(
      {
        name: this.translate.instant('converter.create-new-converter'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.config.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('converter.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importConverter($event)
      }
    );

    this.config.deleteEntityTitle = converter =>
      this.translate.instant('converter.delete-converter-title', { converterName: converter.name });
    this.config.deleteEntityContent = () => this.translate.instant('converter.delete-converter-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('converter.delete-converters-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('converter.delete-converters-text');
    this.config.loadEntity = id => this.converterService.getConverter(id.id);
    this.config.saveEntity = converter => this.saveConverter(converter);
    this.config.deleteEntity = id => this.converterService.deleteConverter(id.id);

    this.config.onEntityAction = action => this.onConverterAction(action);
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<Converter> {
    this.config.componentsData = resolveConverterParams(route);
    this.config.tableTitle = this.configureTableTitle(this.config.componentsData.converterScope);

    this.config.entitiesFetchFunction = this.configureEntityFunctions(this.config.componentsData.converterScope);

    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  private configureEntityFunctions(converterScope: string): (pageLink: PageLink) => Observable<PageData<Converter>> {
    if (converterScope === 'tenant') {
      return pageLink => this.converterService.getConverters(pageLink);
    } else if (converterScope === 'edges') {
      return pageLink => this.converterService.getConvertersByEdgeTemplate(pageLink, true);
    }
  }

  private saveConverter(converter: Converter): Observable<Converter> {
    if (isUndefined(converter.edgeTemplate)) {
      if (this.config.componentsData.converterScope === 'tenant') {
        converter.edgeTemplate = false;
      } else if (this.config.componentsData.converterScope === 'edges') {
        converter.edgeTemplate = true;
      } else {
        // safe fallback to default
        converter.edgeTemplate = false;
      }
    }
    return this.converterService.saveConverter(converter);
  }

  openConverter($event: Event, converter: Converter) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.config.componentsData.converterScope === 'edges') {
      this.router.navigateByUrl(`edgeManagement/templates/converters/${converter.id.id}`).then(() => {});
    } else {
      this.router.navigateByUrl(`converters/${converter.id.id}`).then(() => {});
    }
  }

  onOpenDebugConfig($event: Event, converter: Converter): void {
    if ($event) {
      $event.stopPropagation();
    }

    const additionalActionConfig = {
      title: this.translate.instant('action.see-debug-events'),
      action: () => this.openDebugEventDetails($event, converter)
    };

    const { viewContainerRef, renderer } = this.config.getTable();
    this.entityDebugSettingsService.viewContainerRef = viewContainerRef;
    this.entityDebugSettingsService.renderer = renderer;

    this.entityDebugSettingsService.openDebugStrategyPanel({
      debugSettings: converter.debugSettings || {},
      debugConfig: {
        entityType: EntityType.CONVERTER,
        additionalActionConfig
      },
      onSettingsAppliedFn: settings => this.onDebugConfigChanged(converter.id.id, settings)
    }, $event.target as Element);
  }

  private openDebugEventDetails($event: Event, entity: Converter): void {
    const table = this.config.getTable();
    if (!table.isDetailsOpen) {
      table.toggleEntityDetails($event, entity);
      if (table.entityDetailsPanel.matTabGroup._tabs.length > 1) {
        table.entityDetailsPanel.matTabGroup.selectedIndex = 4;
      } else {
        table.entityDetailsPanel.matTabGroup._tabs.changes.pipe(
          first()
        ).subscribe(() => {
          table.entityDetailsPanel.matTabGroup.selectedIndex = 4;
        })
      }
    }
    table.detectChanges();
  }

  exportConverter($event: Event, converter: Converter) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportConverter(converter.id.id);
  }

  importConverter(_$event: Event) {
    this.importExport.importConverter().subscribe(
      (converter) => {
        if (converter) {
          this.config.updateData();
        }
      });
  }

  onConverterAction(action: EntityAction<Converter>): boolean {
    switch (action.action) {
      case 'open':
        this.openConverter(action.event, action.entity);
        return true;
      case 'export':
        this.exportConverter(action.event, action.entity);
        return true;
    }
    return false;
  }

  private onDebugConfigChanged(id: string, debugSettings: EntityDebugSettings): void {
    this.converterService.getConverter(id).pipe(
      switchMap(converter => this.converterService.saveConverter({ ...converter, debugSettings })),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.config.updateData());
  }

  private configureTableTitle(converterScope: string): string {
    if (converterScope === 'tenant') {
      return this.translate.instant('converter.converters');
    } else if (converterScope === 'edges') {
      return this.translate.instant('edge.converter-templates');
    }
  }

}
