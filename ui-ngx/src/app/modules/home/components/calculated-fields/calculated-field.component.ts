///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, DestroyRef, Inject, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import {
  CalculatedFieldConfiguration,
  CalculatedFieldInfo,
  calculatedFieldsEntityTypeList,
  CalculatedFieldType,
  calculatedFieldTypes,
  CalculatedFieldTypeTranslations,
  OutputStrategyType
} from '@shared/models/calculated-field.models';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { isDefined } from '@core/utils';
import { EntityId } from '@shared/models/id/entity-id';
import { pairwise, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BaseData } from '@shared/models/base-data';
import { Observable } from 'rxjs';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import {
  CalculatedFieldsTableConfig,
  CalculatedFieldsTableEntity
} from '@home/components/calculated-fields/calculated-fields-table-config';
import { TenantId } from '@shared/models/id/tenant-id';

@Component({
  selector: 'tb-calculated-field',
  templateUrl: './calculated-field.component.html',
  styleUrls: ['./calculated-field.component.scss']
})
export class CalculatedFieldComponent extends EntityComponent<CalculatedFieldsTableEntity> {

  @Input()
  standalone = false;

  @Input()
  entityName: string;

  disabledConfiguration = false;

  readonly tenantId = getCurrentAuthUser(this.store).tenantId;
  readonly ownerId = new TenantId(getCurrentAuthUser(this.store).tenantId);
  readonly EntityType = EntityType;
  readonly calculatedFieldsEntityTypeList = calculatedFieldsEntityTypeList;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly fieldTypes = calculatedFieldTypes;
  readonly CalculatedFieldTypeTranslations = CalculatedFieldTypeTranslations;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: CalculatedFieldInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: CalculatedFieldsTableConfig,
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef,
              private destroyRef: DestroyRef,
              private calculatedFieldsService: CalculatedFieldsService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  additionalDebugActionConfig = {
    ...this.entitiesTableConfig.additionalDebugActionConfig,
    action: () => this.entitiesTableConfig.additionalDebugActionConfig.action(
      { id: this.entity.id, ...this.entityFormValue() }, false,
      (expression) => {
        if (expression) {
          this.entityForm.get('configuration').setValue({...this.entityFormValue().configuration, expression});
          this.entityForm.get('configuration').markAsDirty();
        }
      }),
  };

  get entityId(): EntityId {
    return this.entityForm.get('entityId').value;
  }

  get entitiesTableConfig(): CalculatedFieldsTableConfig {
    return this.entitiesTableConfigValue;
  }

  changeEntity(entity: BaseData<EntityId>): void {
    this.entityName = entity?.name;
  }

  buildForm(entity?: CalculatedFieldInfo): FormGroup {
    const form = this.fb.group({
      name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
      entityId: [null],
      type: [CalculatedFieldType.SIMPLE],
      debugSettings: [],
      configuration: this.fb.control<CalculatedFieldConfiguration>({} as CalculatedFieldConfiguration),
    });
    form.get('type').valueChanges.pipe(
      pairwise(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(([prevType, nextType]) => {
      if (this.isEditValue) {
        if (![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(prevType) ||
          ![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(nextType)) {
          form.get('configuration').setValue(({} as CalculatedFieldConfiguration), {emitEvent: false});
        }
      }
    });
    return form;
  }

  updateForm(entity: CalculatedFieldInfo) {
    const { configuration = {} as CalculatedFieldConfiguration, type = CalculatedFieldType.SIMPLE, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.entityId, ...value } = entity ?? {};
    if (configuration.type !== CalculatedFieldType.ALARM) {
      if (isDefined(configuration?.output) && !configuration?.output?.strategy) {
        configuration.output.strategy = {type: OutputStrategyType.RULE_CHAIN};
      }
    }
    this.entityForm.patchValue({ type }, {emitEvent: false});
    setTimeout(() => {
      this.entityForm.patchValue({ configuration, debugSettings, entityId, ...value }, {emitEvent: false});
      this.entityForm.get('type').updateValueAndValidity({onlySelf: true});
    });
    if (!entityId) {
      this.entityForm.get('configuration').disable({emitEvent: false});
      this.disabledConfiguration = true;
    }
  }

  onTestScript(expression?: string): Observable<string> {
    const calculatedFieldId = this.entity?.id?.id;
    if (calculatedFieldId) {
      return this.calculatedFieldsService.getLatestCalculatedFieldDebugEvent(calculatedFieldId, {ignoreLoading: true})
        .pipe(
          switchMap(event => {
            const args = event?.arguments ? JSON.parse(event.arguments) : null;
            return this.entitiesTableConfig.getTestScriptDialog(this.entityFormValue(), args, false, expression);
          }),
          takeUntilDestroyed(this.destroyRef)
        )
    }

    return this.entitiesTableConfig.getTestScriptDialog(this.entityFormValue(), null, false, expression);
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
        this.entityForm.get('entityId').disable({emitEvent: false});
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }
}
