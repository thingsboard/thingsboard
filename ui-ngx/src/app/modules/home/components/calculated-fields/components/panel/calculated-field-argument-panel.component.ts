///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, ElementRef, Input, OnInit, output, ViewChild } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { charNumRegex, noLeadTrailSpacesRegex } from '@shared/models/regex.constants';
import {
  ArgumentEntityType,
  ArgumentEntityTypeTranslations,
  ArgumentType,
  ArgumentTypeTranslations,
  CalculatedFieldArgumentValue,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';
import { delay, distinctUntilChanged, filter, throttleTime } from 'rxjs/operators';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DatasourceType } from '@shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { merge } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';

@Component({
  selector: 'tb-calculated-field-argument-panel',
  templateUrl: './calculated-field-argument-panel.component.html',
  styleUrls: ['./calculated-field-argument-panel.component.scss']
})
export class CalculatedFieldArgumentPanelComponent extends PageComponent implements OnInit {

  @Input() popover: TbPopoverComponent<CalculatedFieldArgumentPanelComponent>;
  @Input() buttonTitle: string;
  @Input() index: number;
  @Input() argument: CalculatedFieldArgumentValue;
  @Input() entityId: EntityId;
  @Input() tenantId: string;
  @Input() calculatedFieldType: CalculatedFieldType;

  @ViewChild('timeseriesInput') timeseriesInput: ElementRef;

  argumentsDataApplied = output<{ value: CalculatedFieldArgumentValue, index: number }>();

  argumentFormGroup = this.fb.group({
    argumentName: ['', [Validators.required, Validators.pattern(charNumRegex), Validators.maxLength(255)]],
    refEntityId: this.fb.group({
      entityType: [ArgumentEntityType.Current],
      id: ['']
    }),
    refEntityKey: this.fb.group({
      type: [ArgumentType.LatestTelemetry, [Validators.required]],
      key: [''],
      scope: [{ value: AttributeScope.SERVER_SCOPE, disabled: true }],
    }),
    defaultValue: ['', [Validators.pattern(noLeadTrailSpacesRegex)]],
    limit: [10],
    timeWindow: [MINUTE * 15],
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

  constructor(
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {
    super();

    this.observeEntityFilterChanges();
    this.observeEntityTypeChanges()
    this.observeEntityKeyChanges();
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

  ngOnInit(): void {
    this.argumentFormGroup.patchValue(this.argument, {emitEvent: false});
    this.updateEntityFilter(this.argument.refEntityId?.entityType, true);
    this.toggleByEntityKeyType(this.argument.refEntityKey?.type);
    this.setInitialEntityKeyType();

    this.argumentTypes = Object.values(ArgumentType)
      .filter(type => type !== ArgumentType.Rolling || this.calculatedFieldType === CalculatedFieldType.SCRIPT);
  }

  saveArgument(): void {
    const { refEntityId, ...restConfig } = this.argumentFormGroup.value;
    const value = (refEntityId.entityType === ArgumentEntityType.Current ? restConfig : { refEntityId, ...restConfig }) as CalculatedFieldArgumentValue;
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
    let entityId: EntityId;
    switch (entityType) {
      case ArgumentEntityType.Current:
        entityId = this.entityId
        break;
      case ArgumentEntityType.Tenant:
        entityId = {
          id: this.tenantId,
          entityType: EntityType.TENANT
        };
        break;
      default:
        entityId = this.argumentFormGroup.get('refEntityId').value as unknown as EntityId;
    }
    if (!onInit) {
      this.argumentFormGroup.get('refEntityKey').get('key').setValue('');
    }
    this.entityFilter = {
      type: AliasFilterType.singleEntity,
      singleEntity: entityId,
    };
    this.cd.markForCheck();
  }

  private observeEntityFilterChanges(): void {
    merge(
      this.refEntityIdFormGroup.get('entityType').valueChanges,
      this.refEntityKeyFormGroup.get('type').valueChanges,
      this.refEntityIdFormGroup.get('id').valueChanges.pipe(filter(Boolean)),
      this.refEntityKeyFormGroup.get('scope').valueChanges,
    )
      .pipe(throttleTime(100), delay(50), takeUntilDestroyed())
      .subscribe(() => this.updateEntityFilter(this.entityType));
  }

  private observeEntityTypeChanges(): void {
    this.argumentFormGroup.get('refEntityId').get('entityType').valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(type => {
        this.argumentFormGroup.get('refEntityId').get('id').setValue('');
        this.argumentFormGroup.get('refEntityId')
          .get('id')[type === ArgumentEntityType.Tenant || type === ArgumentEntityType.Current ? 'disable' : 'enable']();
      });
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
      typeControl.updateValueAndValidity();
    }
  }
}
