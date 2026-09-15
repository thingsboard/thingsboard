// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
import { isDefined } from '@core/utils';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { CalculatedFieldsTableEntity } from '@home/components/calculated-fields/calculated-fields-table-config';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getTimezoneInfo } from '@shared/models/time/time.models';

@Injectable({ providedIn: 'root' })
export class CalculatedFieldFormService {
  private fb = inject(FormBuilder);
  private calculatedFieldsService = inject(CalculatedFieldsService);

  buildForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      entityId: [null, Validators.required],
      type: [CalculatedFieldType.SIMPLE],
      debugSettings: [],
      configuration: this.fb.control<CalculatedFieldConfiguration>({} as CalculatedFieldConfiguration),
    });
  }

  buildAlarmRuleForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      entityId: [null, Validators.required],
      type: [CalculatedFieldType.ALARM],
      debugSettings: [],
      configuration: this.fb.group({
        type: [CalculatedFieldType.ALARM],
        arguments: this.fb.control({}, Validators.required),
        propagate: [false],
        propagateToOwner: [false],
        propagateToTenant: [false],
        propagateRelationTypes: [null],
        createRules: [null, Validators.required],
        clearRule: [null],
      }),
    });
  }

  setupTypeChange(form: FormGroup, destroyRef: DestroyRef, isEditActive?: () => boolean): void {
    form.get('type').valueChanges.pipe(
      pairwise(),
      takeUntilDestroyed(destroyRef)
    ).subscribe(([prevType, nextType]) => {
      const shouldCheck = isEditActive ? isEditActive() : true;
      if (shouldCheck && prevType !== nextType) {
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
      if (config.type === CalculatedFieldType.ENTITY_AGGREGATION && config.interval) {
        config.interval.tz = getTimezoneInfo(config.interval.tz, undefined, true)?.id;
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
            let args = null;
            if (event?.arguments) {
              try {
                args = JSON.parse(event.arguments);
              } catch (e) {}
            }
            return testDialogFn(formValue, args, false, expression);
          }),
          takeUntilDestroyed(destroyRef)
        );
    }
    return testDialogFn(formValue, null, false, expression);
  }
}
