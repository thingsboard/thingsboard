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

import { ChangeDetectorRef, Component, DestroyRef, inject, Inject, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import {
  CalculatedFieldConfiguration,
  CalculatedFieldInfo,
  calculatedFieldsEntityTypeList,
  CalculatedFieldType,
  calculatedFieldTypes,
  CalculatedFieldTypeTranslations
} from '@shared/models/calculated-field.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { Observable } from 'rxjs';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import {
  CalculatedFieldsTableConfig,
  CalculatedFieldsTableEntity
} from '@home/components/calculated-fields/calculated-fields-table-config';
import { TenantId } from '@shared/models/id/tenant-id';
import { CalculatedFieldFormService } from '@home/components/calculated-fields/calculated-field-form.service';

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

  private cfFormService = inject(CalculatedFieldFormService);
  private destroyRef = inject(DestroyRef);

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: CalculatedFieldInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: CalculatedFieldsTableConfig,
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
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

  buildForm(_entity?: CalculatedFieldInfo): FormGroup {
    const form = inject(CalculatedFieldFormService).buildForm();
    inject(CalculatedFieldFormService).setupTypeChange(form, inject(DestroyRef), () => this.isEditValue);
    return form;
  }

  updateForm(entity: CalculatedFieldInfo) {
    const { configuration = {} as CalculatedFieldConfiguration, type = CalculatedFieldType.SIMPLE, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.entityId, ...value } = entity ?? {};
    const preparedConfig = this.cfFormService.prepareConfig(configuration);
    this.entityForm.patchValue({ type }, {emitEvent: false});
    setTimeout(() => {
      this.entityForm.patchValue({ configuration: preparedConfig, debugSettings, entityId, ...value }, {emitEvent: false});
      this.entityForm.get('type').updateValueAndValidity({onlySelf: true});
    });
  }

  onTestScript(expression?: string): Observable<string> {
    return this.cfFormService.testScript(
      this.entity?.id?.id,
      this.entityFormValue(),
      this.entitiesTableConfig.getTestScriptDialog.bind(this.entitiesTableConfig),
      this.destroyRef,
      expression
    );
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
