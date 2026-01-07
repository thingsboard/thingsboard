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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  Input,
  OnInit,
  output,
  ViewChild
} from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { charsWithNumRegex, oneSpaceInsideRegex } from '@shared/models/regex.constants';
import {
  ArgumentEntityType,
  ArgumentEntityTypeParamsMap,
  ArgumentEntityTypeTranslations,
  ArgumentType,
  ArgumentTypeTranslations,
  CalculatedFieldArgumentValue,
  FORBIDDEN_NAMES,
  forbiddenNamesValidator,
  getCalculatedFieldCurrentEntityFilter,
  uniqueNameValidator
} from '@shared/models/calculated-field.models';
import { debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DatasourceType } from '@shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { BehaviorSubject, merge } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { EntityAutocompleteComponent } from '@shared/components/entity/entity-autocomplete.component';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { TenantId } from '@shared/models/id/tenant-id';

@Component({
  selector: 'tb-calculated-field-argument-panel',
  templateUrl: './calculated-field-argument-panel.component.html',
  styleUrls: ['../common/calculated-field-panel.scss', './calculated-field-argument-panel.component.scss']
})
export class CalculatedFieldArgumentPanelComponent implements OnInit, AfterViewInit {

  @Input() buttonTitle: string;
  @Input() argument: CalculatedFieldArgumentValue;
  @Input() entityId: EntityId;
  @Input() tenantId: string;
  @Input() entityName: string;
  @Input() ownerId: EntityId;
  @Input() isScript: boolean;
  @Input() usedArgumentNames: string[];
  @Input() watchKeyChange = false;
  @Input() hiddenEntityTypes = false;
  @Input() hiddenEntityKeyTypes = false;
  @Input() hiddenDefaultValue = false;
  @Input() defaultValueRequired = false;
  @Input() hint: string;
  @Input() predefinedEntityFilter: EntityFilter;
  @Input() forbiddenNames = FORBIDDEN_NAMES;
  @Input() argumentEntityTypes = Object.values(ArgumentEntityType).filter(value => value !== ArgumentEntityType.RelationQuery) as ArgumentEntityType[];
  @Input() argumentNameContext: {[key: string]: string} = {
    label: 'calculated-fields.argument-name',
    required: 'calculated-fields.hint.argument-name-required',
    duplicate: 'calculated-fields.hint.argument-name-duplicate',
    pattern: 'calculated-fields.hint.argument-name-pattern',
    maxlength: 'calculated-fields.hint.argument-name-max-length',
    forbidden: 'calculated-fields.hint.argument-name-forbidden'
  };
  @Input() readonly = false;

  @ViewChild('entityAutocomplete') entityAutocomplete: EntityAutocompleteComponent;

  argumentsDataApplied = output<CalculatedFieldArgumentValue>();

  argumentType = this.fb.control(ArgumentEntityType.Current, Validators.required);

  readonly maxDataPointsPerRollingArg = getCurrentAuthState(this.store).maxDataPointsPerRollingArg;
  readonly defaultLimit = Math.floor(this.maxDataPointsPerRollingArg / 10);

  argumentFormGroup = this.fb.group({
    argumentName: ['', [Validators.required, Validators.pattern(charsWithNumRegex), Validators.maxLength(255)]],
    refEntityId: [null],
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
  entityNameSubject = new BehaviorSubject<string>(null);

  enableAutocomplete = false;

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
    private store: Store<AppState>,
    private destroyRef: DestroyRef
  ) {
    this.observeEntityFilterChanges();
    this.observeArgumentTypeChanges();
    this.observeEntityKeyChanges();
  }

  get entityType(): ArgumentEntityType {
    return this.argumentType.value;
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
    this.updatedFormValidators();
    this.updatedArgumentType();
    this.argumentFormGroup.patchValue(this.argument, {emitEvent: false});
    this.currentEntityFilter = getCalculatedFieldCurrentEntityFilter(this.entityName, this.entityId);
    this.updateEntityFilter(this.entityType, true);
    this.updatedRefEntityIdState(this.entityType, false);
    this.toggleByEntityKeyType(this.argument.refEntityKey?.type);
    this.setInitialEntityKeyType();
    this.setInitialEntityType();
    if (this.watchKeyChange) {
      this.setWatchKeyChange();
    }

    if (this.defaultValueRequired) {
      this.argumentFormGroup.get('defaultValue').addValidators(Validators.required);
      this.argumentFormGroup.get('defaultValue').updateValueAndValidity({onlySelf: true});
    }

    this.argumentTypes = Object.values(ArgumentType)
      .filter(type => type !== ArgumentType.Rolling || this.isScript);

    if (this.readonly) {
      this.argumentType.disable({emitEvent: false});
      this.argumentFormGroup.disable({emitEvent: false});
    }
  }

  ngAfterViewInit(): void {
    if (this.argument.refEntityId?.id === NULL_UUID) {
      this.entityAutocomplete.selectEntityFormGroup.get('entity').markAsTouched();
    }
  }

  saveArgument(): void {
    const value = this.argumentFormGroup.value as CalculatedFieldArgumentValue;
    if (this.entityType === ArgumentEntityType.Owner) {
      value.refDynamicSourceConfiguration = {type: ArgumentEntityType.Owner};
    } else if (this.entityType === ArgumentEntityType.Tenant) {
      value.refEntityId = new TenantId(this.tenantId) as any;
    }
    if (this.entityType !== ArgumentEntityType.Current && this.entityType !== ArgumentEntityType.Tenant) {
      value.entityName = this.entityNameSubject.value;
    }
    if (value.defaultValue) {
      value.defaultValue = value.defaultValue.trim();
    }
    value.refEntityKey.key = value.refEntityKey.key.trim();
    this.argumentsDataApplied.emit(value);
  }

  cancel(): void {
    this.popover.hide();
  }

  private updatedFormValidators(): void {
    this.argumentFormGroup.get('argumentName').addValidators(
      [uniqueNameValidator(this.usedArgumentNames), forbiddenNamesValidator(this.forbiddenNames)]);
    this.argumentFormGroup.get('argumentName').updateValueAndValidity({emitEvent: false});
  }

  private updatedArgumentType(): void {
    let argumentType = ArgumentEntityType.Current;
    if (this.argument.refDynamicSourceConfiguration?.type === ArgumentEntityType.Owner) {
      this.enableAutocomplete = (this.entityId.entityType === EntityType.DEVICE_PROFILE || this.entityId.entityType === EntityType.ASSET_PROFILE);
      argumentType = ArgumentEntityType.Owner;
    } else if (this.argument.refEntityId?.entityType) {
      argumentType = this.argument.refEntityId.entityType;
    }
    this.argumentType.setValue(argumentType, {emitEvent: false});
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
      case ArgumentEntityType.Owner:
        entityFilter = {
          type: AliasFilterType.singleEntity,
          singleEntity: this.ownerId
        };
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
      this.argumentFormGroup.get('refEntityKey').get('key').setValue('', {emitEvents: !this.watchKeyChange});
      if (this.watchKeyChange && this.argumentFormGroup.get('argumentName').pristine) {
        this.argumentFormGroup.get('argumentName').markAsUntouched({emitEvent: false});
        this.argumentFormGroup.get('argumentName').setValue('', {emitEvent: false});
      }
    } else if (this.predefinedEntityFilter) {
      entityFilter = this.predefinedEntityFilter;
    }
    this.entityFilter = entityFilter;
    this.cd.markForCheck();
  }

  private observeEntityFilterChanges(): void {
    merge(
      this.argumentType.valueChanges,
      this.refEntityKeyFormGroup.get('type').valueChanges,
      this.argumentFormGroup.get('refEntityId').valueChanges.pipe(filter(Boolean)),
      this.refEntityKeyFormGroup.get('scope').valueChanges,
    )
      .pipe(debounceTime(50), takeUntilDestroyed())
      .subscribe(() => this.updateEntityFilter(this.entityType));
  }

  private observeArgumentTypeChanges(): void {
    this.argumentType.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(type => {
        this.argumentFormGroup.get('refEntityId').setValue(null);
        this.enableAutocomplete = (this.entityId.entityType === EntityType.DEVICE_PROFILE || this.entityId.entityType === EntityType.ASSET_PROFILE) && type === ArgumentEntityType.Owner;
        this.updatedRefEntityIdState(type);
        if (!this.enableAttributeScopeSelection) {
          this.refEntityKeyFormGroup.get('scope').setValue(AttributeScope.SERVER_SCOPE);
        }
      });
  }

  private observeEntityKeyChanges(): void {
    this.argumentFormGroup.get('refEntityKey').get('type').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(type => this.toggleByEntityKeyType(type));
  }

  private setInitialEntityKeyType(): void {
    if (!this.isScript && this.argument.refEntityKey?.type === ArgumentType.Rolling) {
      const typeControl = this.argumentFormGroup.get('refEntityKey').get('type');
      typeControl.setValue(null);
      typeControl.markAsTouched();
    }
  }

  private setInitialEntityType() {
    if (!this.argumentEntityTypes.includes(this.entityType)) {
      this.argumentType.setValue(null);
      this.argumentType.markAsTouched();
    }
  }

  private setWatchKeyChange(): void {
    this.refEntityKeyFormGroup.get('key').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((key) => {
      if (this.argumentFormGroup.get('argumentName').pristine) {
        this.argumentFormGroup.get('argumentName').setValue(key);
        this.argumentFormGroup.get('argumentName').markAsTouched({emitEvent: false});
      }
    });
  }

  private updatedRefEntityIdState(type: ArgumentEntityType, emitEvent = true): void {
    const isEntityWithId = !!type && ![ArgumentEntityType.Tenant, ArgumentEntityType.Current, ArgumentEntityType.Owner].includes(type);
    this.argumentFormGroup.get('refEntityId')[isEntityWithId ? 'enable' : 'disable']({emitEvent});
    if (!isEntityWithId) {
      this.entityNameSubject.next(null);
    }
  }
}
