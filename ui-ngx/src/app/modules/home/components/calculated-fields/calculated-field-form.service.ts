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

import { DestroyRef, inject, Injectable } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { pairwise, switchMap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import {
  CalculatedField,
  CalculatedFieldConfiguration,
  CalculatedFieldEventArguments,
  CalculatedFieldType,
  OutputStrategyType
} from '@shared/models/calculated-field.models';
import { oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { isDefined } from '@core/utils';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { CalculatedFieldsTableEntity } from '@home/components/calculated-fields/calculated-fields-table-config';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityType } from '@shared/models/entity-type.models';

@Injectable({ providedIn: 'root' })
export class CalculatedFieldFormService {
  private fb = inject(FormBuilder);
  private calculatedFieldsService = inject(CalculatedFieldsService);

  buildForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
      entityId: [{type: EntityType.DEVICE_PROFILE, id: null}, Validators.required],
      type: [CalculatedFieldType.SIMPLE],
      debugSettings: [],
      configuration: this.fb.control<CalculatedFieldConfiguration>({} as CalculatedFieldConfiguration),
    });
  }

  setupTypeChange(form: FormGroup, destroyRef: DestroyRef, isEditActive?: () => boolean): void {
    form.get('type').valueChanges.pipe(
      pairwise(),
      takeUntilDestroyed(destroyRef)
    ).subscribe(([prevType, nextType]) => {
      const shouldCheck = isEditActive ? isEditActive() : true;
      if (shouldCheck) {
        if (![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(prevType) ||
          ![CalculatedFieldType.SIMPLE, CalculatedFieldType.SCRIPT].includes(nextType)) {
          form.get('configuration').setValue({} as CalculatedFieldConfiguration, { emitEvent: false });
        }
      }
    });
  }

  prepareConfig(configuration: CalculatedFieldConfiguration): CalculatedFieldConfiguration {
    const config = configuration || ({} as CalculatedFieldConfiguration);
    if (config.type !== CalculatedFieldType.ALARM) {
      if (isDefined(config?.output) && !config?.output?.strategy) {
        config.output.strategy = { type: OutputStrategyType.RULE_CHAIN };
      }
    }
    return config;
  }

  testScript(
    calculatedFieldId: string,
    formValue: CalculatedField,
    testDialogFn: (calculatedField: CalculatedFieldsTableEntity, argumentsObj?: CalculatedFieldEventArguments, openCalculatedFieldEdit?: boolean, expression?: string) => Observable<string>,
    destroyRef: DestroyRef,
    expression?: string,
  ): Observable<string> {
    if (calculatedFieldId) {
      return this.calculatedFieldsService.getLatestCalculatedFieldDebugEvent(calculatedFieldId, {ignoreLoading: true})
        .pipe(
          switchMap(event => {
            const args = event?.arguments ? JSON.parse(event.arguments) : null;
            return testDialogFn(formValue, args, false, expression);
          }),
          takeUntilDestroyed(destroyRef)
        );
    }
    return testDialogFn(formValue, null, false, expression);
  }
}