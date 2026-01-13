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

import { Component, DestroyRef, Inject, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AbstractControl,
  FormGroupDirective,
  NgForm,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { EntityAlias, EntityAliases, EntityAliasFilter } from '@shared/models/alias.models';
import { DatasourceType, TargetDeviceType, Widget, widgetType } from '@shared/models/widget.models';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DialogService } from '@core/services/dialog.service';
import { deepClone, isUndefined } from '@core/utils';
import { EntityAliasDialogComponent, EntityAliasDialogData } from './entity-alias-dialog.component';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface EntityAliasesDialogData {
  entityAliases: EntityAliases;
  widgets: Array<Widget>;
  isSingleEntityAlias?: boolean;
  isSingleWidget?: boolean;
  allowedEntityTypes?: Array<EntityType | AliasEntityType>;
  disableAdd?: boolean;
  singleEntityAlias?: EntityAlias;
  customTitle?: string;
}

@Component({
  selector: 'tb-entity-aliases-dialog',
  templateUrl: './entity-aliases-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: EntityAliasesDialogComponent}],
  styleUrls: ['./entity-aliases-dialog.component.scss']
})
export class EntityAliasesDialogComponent extends DialogComponent<EntityAliasesDialogComponent, EntityAliases>
  implements ErrorStateMatcher {

  title: string;
  disableAdd: boolean;
  allowedEntityTypes: Array<EntityType | AliasEntityType>;

  aliasToWidgetsMap: {[aliasId: string]: Array<string>} = {};

  entityAliasesFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EntityAliasesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<EntityAliasesDialogComponent, EntityAliases>,
              private fb: UntypedFormBuilder,
              private dashboardUtils: DashboardUtilsService,
              private translate: TranslateService,
              private dialogs: DialogService,
              private dialog: MatDialog,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
    this.title = data.customTitle ? data.customTitle : 'entity.aliases';
    this.disableAdd = this.data.disableAdd;
    this.allowedEntityTypes = this.data.allowedEntityTypes;

    if (data.widgets) {
      let widgetsTitleList: Array<string>;
      if (this.data.isSingleWidget && this.data.widgets.length === 1) {
        const widget = this.data.widgets[0];
        widgetsTitleList = [widget.config.title];
        for (const aliasId of Object.keys(this.data.entityAliases)) {
          this.aliasToWidgetsMap[aliasId] = widgetsTitleList;
        }
      } else {
        this.data.widgets.forEach((widget) => {
          if (widget.type === widgetType.rpc) {
            if (widget.config.targetDevice?.type === TargetDeviceType.entity && widget.config.targetDevice.entityAliasId) {
              this.addWidgetTitleToWidgetsMap(widget.config.targetDevice.entityAliasId, widget.config.title);
            }
          } else if (widget.type === widgetType.alarm) {
            if (widget.config.alarmSource) {
              this.addWidgetTitleToWidgetsMap(widget.config.alarmSource.entityAliasId, widget.config.title);
            }
          } else {
            this.dashboardUtils.getWidgetDatasources(widget).forEach((datasource) => {
              if ([DatasourceType.entity, DatasourceType.entityCount, DatasourceType.alarmCount].includes(datasource.type)
                && datasource.entityAliasId) {
                this.addWidgetTitleToWidgetsMap(datasource.entityAliasId, widget.config.title);
              }
            });
          }
        });
      }
    }
    const entityAliasControls: Array<AbstractControl> = [];
    for (const aliasId of Object.keys(this.data.entityAliases)) {
      const entityAlias = this.data.entityAliases[aliasId];
      if (!entityAlias.filter) {
        entityAlias.filter = {
          resolveMultiple: false
        };
      }
      if (isUndefined(entityAlias.filter.resolveMultiple)) {
        entityAlias.filter.resolveMultiple = false;
      }
      entityAliasControls.push(this.createEntityAliasFormControl(aliasId, entityAlias));
    }

    this.entityAliasesFormGroup = this.fb.group({
      entityAliases: this.fb.array(entityAliasControls)
    });
  }

  private addWidgetTitleToWidgetsMap(aliasId: string, widgetTitle: string) {
    let widgetsTitleList: Array<string> = this.aliasToWidgetsMap[aliasId];
    if (!widgetsTitleList) {
      widgetsTitleList = [];
      this.aliasToWidgetsMap[aliasId] = widgetsTitleList;
    }
    if (!widgetsTitleList.includes(widgetTitle)) {
      widgetsTitleList.push(widgetTitle);
    }
  }

  private createEntityAliasFormControl(aliasId: string, entityAlias: EntityAlias): AbstractControl {
    const aliasFormControl = this.fb.group({
      id: [aliasId],
      alias: [entityAlias ? entityAlias.alias : null, [Validators.required]],
      filter: [entityAlias ? entityAlias.filter : null],
      resolveMultiple: [entityAlias ? entityAlias.filter.resolveMultiple : false]
    });
    aliasFormControl.get('resolveMultiple').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((resolveMultiple: boolean) => {
      (aliasFormControl.get('filter').value as EntityAliasFilter).resolveMultiple = resolveMultiple;
    });
    return aliasFormControl;
  }


  entityAliasesFormArray(): UntypedFormArray {
    return this.entityAliasesFormGroup.get('entityAliases') as UntypedFormArray;
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  removeAlias(index: number) {
    const entityAlias = (this.entityAliasesFormGroup.get('entityAliases').value as any[])[index];
    const widgetsTitleList = this.aliasToWidgetsMap[entityAlias.id];
    if (widgetsTitleList) {
      let widgetsListHtml = '';
      for (const widgetTitle of widgetsTitleList) {
        widgetsListHtml += '<br/>\'' + widgetTitle + '\'';
      }
      const message = this.translate.instant('entity.unable-delete-entity-alias-text',
        {entityAlias: entityAlias.alias, widgetsList: widgetsListHtml});
      this.dialogs.alert(this.translate.instant('entity.unable-delete-entity-alias-title'),
        message, this.translate.instant('action.close'), true);
    } else {
      (this.entityAliasesFormGroup.get('entityAliases') as UntypedFormArray).removeAt(index);
      this.entityAliasesFormGroup.markAsDirty();
    }
  }

  public addAlias() {
    this.openAliasDialog(-1);
  }

  public editAlias(index: number) {
    this.openAliasDialog(index);
  }

  private openAliasDialog(index: number) {
    const isAdd = index === -1;
    let alias;
    const aliasesArray = this.entityAliasesFormGroup.get('entityAliases').value as any[];
    if (!isAdd) {
      alias = aliasesArray[index];
    }
    this.dialog.open<EntityAliasDialogComponent, EntityAliasDialogData,
      EntityAlias>(EntityAliasDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        allowedEntityTypes: this.allowedEntityTypes,
        entityAliases: aliasesArray,
        alias: isAdd ? null : deepClone(alias)
      }
    }).afterClosed().subscribe((entityAlias) => {
      if (entityAlias) {
        if (isAdd) {
          (this.entityAliasesFormGroup.get('entityAliases') as UntypedFormArray)
            .push(this.createEntityAliasFormControl(entityAlias.id, entityAlias));
        } else {
          const aliasFormControl = (this.entityAliasesFormGroup.get('entityAliases') as UntypedFormArray).at(index);
          aliasFormControl.get('alias').patchValue(entityAlias.alias);
          aliasFormControl.get('filter').patchValue(entityAlias.filter);
          aliasFormControl.get('resolveMultiple').patchValue(entityAlias.filter.resolveMultiple);
        }
        this.entityAliasesFormGroup.markAsDirty();
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    const entityAliases: EntityAliases = {};
    const uniqueAliasList: {[alias: string]: string} = {};

    let valid = true;
    let message: string;

    const aliasesArray = this.entityAliasesFormGroup.get('entityAliases').value as any[];
    for (const aliasValue of aliasesArray) {
      const aliasId: string = aliasValue.id;
      const alias: string = aliasValue.alias.trim();
      const filter: EntityAliasFilter = aliasValue.filter;
      if (uniqueAliasList[alias]) {
        valid = false;
        message = this.translate.instant('entity.duplicate-alias-error', {alias});
        break;
      } else if (!filter || !filter.type) {
        valid = false;
        message = this.translate.instant('entity.missing-entity-filter-error', {alias});
        break;
      } else {
        uniqueAliasList[alias] = alias;
        entityAliases[aliasId] = {id: aliasId, alias, filter};
      }
    }
    if (valid) {
      this.dialogRef.close(entityAliases);
    } else {
      this.store.dispatch(new ActionNotificationShow(
        {
          message,
          type: 'error'
        }));
    }
  }
}
