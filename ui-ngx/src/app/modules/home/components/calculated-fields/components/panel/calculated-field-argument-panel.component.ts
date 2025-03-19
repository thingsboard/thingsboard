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

import { AfterViewInit, ChangeDetectorRef, Component, Input, OnInit, output, ViewChild } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { charsWithNumRegex, oneSpaceInsideRegex } from '@shared/models/regex.constants';
import {
  ArgumentEntityType,
  ArgumentEntityTypeParamsMap,
  ArgumentEntityTypeTranslations,
  ArgumentType,
  ArgumentTypeTranslations,
  CalculatedFieldArgumentValue,
  CalculatedFieldType,
  getCalculatedFieldCurrentEntityFilter
} from '@shared/models/calculated-field.models';
import { debounceTime, delay, distinctUntilChanged, filter } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DatasourceType } from '@shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { merge } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { EntityAutocompleteComponent } from '@shared/components/entity/entity-autocomplete.component';

@Component({
  selector: 'tb-calculated-field-argument-panel',
  templateUrl: './calculated-field-argument-panel.component.html',
  styleUrls: ['./calculated-field-argument-panel.component.scss']
})
export class CalculatedFieldArgumentPanelComponent implements OnInit, AfterViewInit {

  @Input() buttonTitle: string;
  @Input() index: number;
  @Input() argument: CalculatedFieldArgumentValue;
  @Input() entityId: EntityId;
  @Input() tenantId: string;
  @Input() entityName: string;
  @Input() entityHasError: boolean;
  @Input() calculatedFieldType: CalculatedFieldType;
  @Input() usedArgumentNames: string[];

  @ViewChild('entityAutocomplete') entityAutocomplete: EntityAutocompleteComponent;

  argumentsDataApplied = output<{ value: CalculatedFieldArgumentValue, index: number }>();

  readonly maxDataPointsPerRollingArg = getCurrentAuthState(this.store).maxDataPointsPerRollingArg;
  readonly defaultLimit = Math.floor(this.maxDataPointsPerRollingArg / 10);

  argumentFormGroup = this.fb.group({
    argumentName: ['', [Validators.required, this.uniqNameRequired(), this.notEqualCtxValidator(), Validators.pattern(charsWithNumRegex), Validators.maxLength(255)]],
    refEntityId: this.fb.group({
      entityType: [ArgumentEntityType.Current],
      id: ['']
    }),
    refEntityKey: this.fb.group({
      type: [ArgumentType.LatestTelemetry, [Validators.required]],
      key: ['', [Validators.pattern(oneSpaceInsideRegex)]],
      scope: [{ value: AttributeScope.SERVER_SCOPE, disabled: true }, [Validators.required]],
    }),
    defaultValue: ['', [Validators.pattern(oneSpaceInsideRegex)]],
    limit: [{ value: this.defaultLimit, disabled: !this.maxDataPointsPerRollingArg }, [Validators.required, Validators.min(1), Validators.max(this.maxDataPointsPerRollingArg)]],
    timeWindow: [MINUTE * 15, [Validators.required]],
  });

  argumentTypes: ArgumentType[];
  entityFilter: EntityFilter;

  readonly argumentEntityTypes = Object.values(ArgumentEntityType) as ArgumentEntityType[];
  readonly ArgumentEntityTypeTranslations = ArgumentEntityTypeTranslations;
  readonly ArgumentType = ArgumentType;
  readonly DataKeyType = DataKeyType;
  readonly EntityType = EntityType;
  readonly datasourceType = DatasourceType;
  readonly ArgumentTypeTranslations = ArgumentTypeTranslations;
  readonly AttributeScope = AttributeScope;
  readonly ArgumentEntityType = ArgumentEntityType;
  readonly ArgumentEntityTypeParamsMap = ArgumentEntityTypeParamsMap;

  private currentEntityFilter: EntityFilter;

  constructor(
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private popover: TbPopoverComponent<CalculatedFieldArgumentPanelComponent>,
    private store: Store<AppState>
  ) {
    this.observeEntityFilterChanges();
    this.observeEntityTypeChanges()
    this.observeEntityKeyChanges();
    this.observeUpdatePosition();
  }

  get entityType(): ArgumentEntityType {
    return this.argumentFormGroup.get('refEntityId').get('entityType').value;
  }

  get refEntityIdFormGroup(): FormGroup {
    return this.argumentFormGroup.get('refEntityId') as FormGroup;
  }

  get refEntityKeyFormGroup(): FormGroup {
    return this.argumentFormGroup.get('refEntityKey') as FormGroup;
  }

  get enableAttributeScopeSelection(): boolean {
    return this.entityType === ArgumentEntityType.Device
      || (this.entityType === ArgumentEntityType.Current
        && (this.entityId.entityType === EntityType.DEVICE || this.entityId.entityType === EntityType.DEVICE_PROFILE))
  }

  ngOnInit(): void {
    this.argumentFormGroup.patchValue(this.argument, {emitEvent: false});
    this.currentEntityFilter = getCalculatedFieldCurrentEntityFilter(this.entityName, this.entityId);
    this.updateEntityFilter(this.argument.refEntityId?.entityType, true);
    this.toggleByEntityKeyType(this.argument.refEntityKey?.type);
    this.setInitialEntityKeyType();

    this.argumentTypes = Object.values(ArgumentType)
      .filter(type => type !== ArgumentType.Rolling || this.calculatedFieldType === CalculatedFieldType.SCRIPT);
  }

  ngAfterViewInit(): void {
    if (this.entityHasError) {
      this.entityAutocomplete.selectEntityFormGroup.get('entity').markAsTouched();
    }
  }

  saveArgument(): void {
    const { refEntityId, ...restConfig } = this.argumentFormGroup.value;
    const value = (refEntityId.entityType === ArgumentEntityType.Current ? restConfig : { refEntityId, ...restConfig }) as CalculatedFieldArgumentValue;
    if (refEntityId.entityType === ArgumentEntityType.Tenant) {
      refEntityId.id = this.tenantId;
    }
    if (value.defaultValue) {
      value.defaultValue = value.defaultValue.trim();
    }
    value.refEntityKey.key = value.refEntityKey.key.trim();
    this.argumentsDataApplied.emit({ value, index: this.index });
  }

  cancel(): void {
    this.popover.hide();
  }

  private toggleByEntityKeyType(type: ArgumentType): void {
    const isAttribute = type === ArgumentType.Attribute;
    const isRolling = type === ArgumentType.Rolling;
    this.argumentFormGroup.get('refEntityKey').get('scope')[isAttribute? 'enable' : 'disable']({ emitEvent: false });
    this.argumentFormGroup.get('limit')[isRolling? 'enable' : 'disable']({ emitEvent: false });
    this.argumentFormGroup.get('timeWindow')[isRolling? 'enable' : 'disable']({ emitEvent: false });
    this.argumentFormGroup.get('defaultValue')[isRolling? 'disable' : 'enable']({ emitEvent: false });
  }

  private updateEntityFilter(entityType: ArgumentEntityType = ArgumentEntityType.Current, onInit = false): void {
    let entityFilter: EntityFilter;
    switch (entityType) {
      case ArgumentEntityType.Current:
        entityFilter = this.currentEntityFilter;
        break;
      case ArgumentEntityType.Tenant:
        entityFilter = {
          type: AliasFilterType.singleEntity,
          singleEntity: {
            id: this.tenantId,
            entityType: EntityType.TENANT
          },
        };
        break;
      default:
        entityFilter = {
          type: AliasFilterType.singleEntity,
          singleEntity: this.argumentFormGroup.get('refEntityId').value as unknown as EntityId,
        };
    }
    if (!onInit) {
      this.argumentFormGroup.get('refEntityKey').get('key').setValue('');
    }
    this.entityFilter = entityFilter;
    this.cd.markForCheck();
  }

  private observeEntityFilterChanges(): void {
    merge(
      this.refEntityIdFormGroup.get('entityType').valueChanges,
      this.refEntityKeyFormGroup.get('type').valueChanges,
      this.refEntityIdFormGroup.get('id').valueChanges.pipe(filter(Boolean)),
      this.refEntityKeyFormGroup.get('scope').valueChanges,
    )
      .pipe(debounceTime(50), takeUntilDestroyed())
      .subscribe(() => this.updateEntityFilter(this.entityType));
  }

  private observeEntityTypeChanges(): void {
    this.argumentFormGroup.get('refEntityId').get('entityType').valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(type => {
        this.argumentFormGroup.get('refEntityId').get('id').setValue('');
        this.argumentFormGroup.get('refEntityId')
          .get('id')[type === ArgumentEntityType.Tenant || type === ArgumentEntityType.Current ? 'disable' : 'enable']();
        if (!this.enableAttributeScopeSelection) {
          this.refEntityKeyFormGroup.get('scope').setValue(AttributeScope.SERVER_SCOPE);
        }
      });
  }

  private uniqNameRequired(): ValidatorFn {
    return (control: FormControl) => {
      const newName = control.value.trim().toLowerCase();
      const isDuplicate = this.usedArgumentNames?.some(name => name.toLowerCase() === newName);

      return isDuplicate ? { duplicateName: true } : null;
    };
  }

  private observeEntityKeyChanges(): void {
    this.argumentFormGroup.get('refEntityKey').get('type').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(type => this.toggleByEntityKeyType(type));
  }

  private setInitialEntityKeyType(): void {
    if (this.calculatedFieldType === CalculatedFieldType.SIMPLE && this.argument.refEntityKey?.type === ArgumentType.Rolling) {
      const typeControl = this.argumentFormGroup.get('refEntityKey').get('type');
      typeControl.setValue(null);
      typeControl.markAsTouched();
    }
  }

  private notEqualCtxValidator(): ValidatorFn {
    return (control: FormControl) => {
      const trimmedValue = control.value.trim().toLowerCase();
      return trimmedValue === 'ctx' ? { equalCtx: true } : null;
    };
  }

  private observeUpdatePosition(): void {
    merge(
      this.refEntityIdFormGroup.get('entityType').valueChanges,
      this.refEntityKeyFormGroup.get('type').valueChanges,
      this.argumentFormGroup.get('timeWindow').valueChanges,
      this.refEntityIdFormGroup.get('id').valueChanges.pipe(filter(Boolean)),
    )
      .pipe(delay(50), takeUntilDestroyed())
      .subscribe(() => this.popover.updatePosition());
  }
}
