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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnChanges,
  Renderer2,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  ArgumentEntityType,
  ArgumentType,
  ArgumentTypeTranslations,
  CalculatedFieldArgument,
  CalculatedFieldArgumentValue,
  CalculatedFieldType,
} from '@shared/models/calculated-field.models';
import { CalculatedFieldArgumentPanelComponent } from '@home/components/calculated-fields/components/public-api';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL, isDefined, isDefinedAndNotNull, isEqual } from '@core/utils';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';
import { EntityService } from '@core/http/entity.service';
import { MatSort } from '@angular/material/sort';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { catchError } from 'rxjs/operators';
import { NEVER } from 'rxjs';

@Component({
  selector: 'tb-calculated-field-arguments-table',
  templateUrl: './calculated-field-arguments-table.component.html',
  styleUrls: [`calculated-field-arguments-table.component.scss`],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldArgumentsTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldArgumentsTableComponent),
      multi: true
    }
  ],
})
export class CalculatedFieldArgumentsTableComponent implements ControlValueAccessor, Validator, OnChanges, AfterViewInit {

  @Input() entityId: EntityId;
  @Input() tenantId: string;
  @Input() entityName: string;
  @Input() calculatedFieldType: CalculatedFieldType;

  @ViewChild(MatSort, { static: true }) sort: MatSort;

  errorText = '';
  argumentsFormArray = this.fb.array<AbstractControl>([]);
  entityNameMap = new Map<string, string>();
  entityNameErrorSet = new Set<string>();
  sortOrder = { direction: 'asc', property: '' };
  dataSource = new CalculatedFieldArgumentDatasource();

  readonly entityTypeTranslations = entityTypeTranslations;
  readonly ArgumentTypeTranslations = ArgumentTypeTranslations;
  readonly ArgumentEntityType = ArgumentEntityType;
  readonly ArgumentType = ArgumentType;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly maxArgumentsPerCF = getCurrentAuthState(this.store).maxArgumentsPerCF;

  private popoverComponent: TbPopoverComponent<CalculatedFieldArgumentPanelComponent>;
  private propagateChange: (argumentsObj: Record<string, CalculatedFieldArgument>) => void = () => {};

  constructor(
    private fb: FormBuilder,
    private popoverService: TbPopoverService,
    private viewContainerRef: ViewContainerRef,
    private cd: ChangeDetectorRef,
    private renderer: Renderer2,
    private entityService: EntityService,
    private destroyRef: DestroyRef,
    private store: Store<AppState>
  ) {
    this.argumentsFormArray.valueChanges.pipe(takeUntilDestroyed()).subscribe(value => {
      this.updateEntityNameMap(value);
      this.updateDataSource(value);
      this.propagateChange(this.getArgumentsObject(value));
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.calculatedFieldType?.previousValue
      && changes.calculatedFieldType.currentValue !== changes.calculatedFieldType.previousValue) {
      this.argumentsFormArray.updateValueAndValidity();
    }
  }

  ngAfterViewInit(): void {
    this.sort.sortChange.asObservable().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.sortOrder.property = this.sort.active;
      this.sortOrder.direction = this.sort.direction;
      this.updateDataSource(this.argumentsFormArray.value);
    });
  }

  registerOnChange(fn: (argumentsObj: Record<string, CalculatedFieldArgument>) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_): void {}

  validate(): ValidationErrors | null {
    this.updateErrorText();
    return this.errorText ? { argumentsFormArray: false } : null;
  }

  onDelete($event: Event, argument: CalculatedFieldArgumentValue): void {
    $event.stopPropagation();
    const index = this.argumentsFormArray.controls.findIndex(control => isEqual(control.value, argument));
    this.argumentsFormArray.removeAt(index);
    this.argumentsFormArray.markAsDirty();
  }

  manageArgument($event: Event, matButton: MatButton, argument = {} as CalculatedFieldArgumentValue, index?: number): void {
    $event?.stopPropagation();
    if (this.popoverComponent && !this.popoverComponent.tbHidden) {
      this.popoverComponent.hide();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx = {
        index,
        argument,
        entityId: this.entityId,
        calculatedFieldType: this.calculatedFieldType,
        buttonTitle: this.argumentsFormArray.at(index)?.value ? 'action.apply' : 'action.add',
        tenantId: this.tenantId,
        entityName: this.entityName,
        entityHasError: this.entityNameErrorSet.has(argument.refEntityId?.id),
        usedArgumentNames: this.argumentsFormArray.value.map(({ argumentName }) => argumentName).filter(name => name !== argument.argumentName),
      };
      this.popoverComponent = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, CalculatedFieldArgumentPanelComponent, isDefined(index) ? 'left' : 'right', false, null,
        ctx,
        {},
        {}, {}, true);
      this.popoverComponent.tbComponentRef.instance.argumentsDataApplied.subscribe(({ value, index }) => {
        this.popoverComponent.hide();
        const formGroup = this.fb.group(value);
        if (isDefinedAndNotNull(index)) {
          this.argumentsFormArray.setControl(index, formGroup);
        } else {
          this.argumentsFormArray.push(formGroup);
        }
        formGroup.markAsDirty();
        this.cd.markForCheck();
      });
    }
  }

  private updateDataSource(value: CalculatedFieldArgumentValue[]): void {
    const sortedValue = this.sortData(value);
    this.dataSource.loadData(sortedValue);
  }

  private updateErrorText(): void {
    if (this.calculatedFieldType === CalculatedFieldType.SIMPLE
      && this.argumentsFormArray.controls.some(control => control.value.refEntityKey.type === ArgumentType.Rolling)) {
      this.errorText = 'calculated-fields.hint.arguments-simple-with-rolling';
    } else if (this.entityNameErrorSet.size) {
      this.errorText = 'calculated-fields.hint.arguments-entity-not-found';
    } else if (!this.argumentsFormArray.controls.length) {
      this.errorText = 'calculated-fields.hint.arguments-empty';
    } else {
      this.errorText = '';
    }
  }

  private getArgumentsObject(value: CalculatedFieldArgumentValue[]): Record<string, CalculatedFieldArgument> {
    return value.reduce((acc, argumentValue) => {
      const { argumentName, ...argument } = argumentValue as CalculatedFieldArgumentValue;
      acc[argumentName] = argument;
      return acc;
    }, {} as Record<string, CalculatedFieldArgument>);
  }

  writeValue(argumentsObj: Record<string, CalculatedFieldArgument>): void {
    this.argumentsFormArray.clear();
    this.populateArgumentsFormArray(argumentsObj)
  }

  getEntityDetailsPageURL(id: string, type: EntityType): string {
    return getEntityDetailsPageURL(id, type);
  }

  private populateArgumentsFormArray(argumentsObj: Record<string, CalculatedFieldArgument>): void {
    Object.keys(argumentsObj).forEach(key => {
      const value: CalculatedFieldArgumentValue = {
        ...argumentsObj[key],
        argumentName: key
      };
      this.argumentsFormArray.push(this.fb.group(value), { emitEvent: false });
    });
    this.argumentsFormArray.updateValueAndValidity();
  }

  private updateEntityNameMap(value: CalculatedFieldArgumentValue[]): void {
    this.entityNameErrorSet.clear();
    value.forEach(({ refEntityId = {}}) => {
      if (refEntityId.id && !this.entityNameMap.has(refEntityId.id)) {
        const { id, entityType } = refEntityId as EntityId;
        this.entityService.getEntity(entityType as EntityType, id, { ignoreLoading: true, ignoreErrors: true })
          .pipe(
            catchError(() => {
              this.entityNameErrorSet.add(id);
              return NEVER;
            }),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe(entity => this.entityNameMap.set(id, entity.name));
      }
    });
  }

  private getSortValue(argument: CalculatedFieldArgumentValue, column: string): string {
    switch (column) {
      case 'entityType':
        if (argument.refEntityId?.entityType === ArgumentEntityType.Tenant) {
          return 'calculated-fields.argument-current-tenant';
        } else if (argument.refEntityId?.id) {
          return entityTypeTranslations.get((argument.refEntityId)?.entityType as unknown as EntityType).type;
        } else {
          return 'calculated-fields.argument-current';
        }
      case 'type':
        return ArgumentTypeTranslations.get(argument.refEntityKey.type);
      case 'key':
        return argument.refEntityKey.key;
      default:
        return argument.argumentName;
    }
  }

  private sortData(data: CalculatedFieldArgumentValue[]): CalculatedFieldArgumentValue[] {
    return data.sort((a, b) => {
      const valA = this.getSortValue(a, this.sortOrder.property) ?? '';
      const valB = this.getSortValue(b, this.sortOrder.property) ?? '';
      return (this.sortOrder.direction === 'asc' ? 1 : -1) * valA.localeCompare(valB);
    });
  }
}

class CalculatedFieldArgumentDatasource extends TbTableDatasource<CalculatedFieldArgumentValue> {
  constructor() {
    super();
  }
}
