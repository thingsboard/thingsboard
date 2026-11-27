// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { coerceBoolean } from '@shared/public-api';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, map, startWith, switchMap } from 'rxjs/operators';
import { edgeSupportedGroupTypes, EntityGroupPattern } from '../rule-node-config.models';

/** Mirrors TbAbstractEdgeEntityGroupActionNode#isTemplatized: ${metadataKey} or $[messageKey]. */
const isTemplatized = (value: string): boolean => value.includes('${') || value.includes('$[');

/** Mirrors TbAbstractEdgeEntityGroupActionNode#isReservedGroupName: EntityGroup.GROUP_ALL_NAME and EdgeUtils#isEdgeGroupAll. */
const isReservedGroupName = (name: string): boolean =>
  name === 'All' || (name.startsWith('[Edge]') && name.endsWith('All'));

interface GroupTypeOption {
  value: EntityType;
  label: string;
}

@Component({
    selector: 'tb-entity-group-patterns',
    templateUrl: './entity-group-patterns.component.html',
    styleUrls: ['./entity-group-patterns.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityGroupPatternsComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => EntityGroupPatternsComponent),
            multi: true,
        }
    ],
    standalone: false
})
export class EntityGroupPatternsComponent implements ControlValueAccessor, OnInit, Validator {

  @Input()
  @coerceBoolean()
  disabled = false;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  allowedGroupTypes: Array<EntityType> = edgeSupportedGroupTypes;

  patternsFormGroup: FormGroup;

  private propagateChange = null;
  private readonly groupNameSuggestions = new WeakMap<AbstractControl, Observable<Array<string>>>();
  private readonly groupTypeOptions = new WeakMap<AbstractControl, Observable<Array<GroupTypeOption>>>();

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              private entityGroupService: EntityGroupService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.patternsFormGroup = this.fb.group({
      patterns: this.fb.array([])
    });

    this.patternsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateModel());
  }

  get patternsFormArray(): FormArray {
    return this.patternsFormGroup.get('patterns') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.patternsFormGroup.disable({emitEvent: false});
    } else {
      this.patternsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(patterns: Array<EntityGroupPattern>): void {
    const rowValues = patterns ?? [];
    if (this.patternsFormArray.length === rowValues.length) {
      this.patternsFormArray.patchValue(rowValues, {emitEvent: false});
      return;
    }
    const rows: Array<FormGroup> = rowValues.map(pattern => this.createRow(pattern));
    this.patternsFormGroup.setControl('patterns', this.fb.array(rows), {emitEvent: false});
  }

  validate() {
    if (!this.patternsFormArray.length) {
      return this.required ? {entityGroupPatternsRequired: true} : null;
    }
    return this.patternsFormGroup.valid ? null : {entityGroupFieldsRequired: true};
  }

  addPattern(): void {
    this.patternsFormArray.push(this.createRow({groupType: null, groupName: null}));
  }

  removePattern(index: number): void {
    this.patternsFormArray.removeAt(index);
  }

  suggestionsFor(row: AbstractControl): Observable<Array<string>> {
    return this.groupNameSuggestions.get(row);
  }

  groupTypeOptionsFor(row: AbstractControl): Observable<Array<GroupTypeOption>> {
    return this.groupTypeOptions.get(row);
  }

  // Options bind the enum but display the label, so without this the field reads DEVICE back after picking Device.
  displayGroupType = (value: string): string => {
    if (!value) {
      return '';
    }
    const entityType = this.toEntityType(value);
    return entityType ? this.groupTypeLabel(entityType) : value;
  };

  private createRow(pattern: EntityGroupPattern): FormGroup {
    const row = this.fb.group({
      groupType: [pattern?.groupType ?? null,
        [Validators.required, Validators.pattern(/.*\S.*/), this.groupTypeValidator]],
      groupName: [pattern?.groupName ?? null, [Validators.required, Validators.pattern(/.*\S.*/)]]
    });
    this.groupNameSuggestions.set(row, this.buildGroupNameSuggestions(row));
    this.groupTypeOptions.set(row, this.buildGroupTypeOptions(row));
    return row;
  }

  // Mirrors TbAbstractEdgeEntityGroupActionNode#validateConfig, so a typo fails here and not at node startup.
  private groupTypeValidator: ValidatorFn = control => {
    const value: string = control.value;
    if (!value || isTemplatized(value)) {
      return null;
    }
    return this.toEntityType(value) ? null : {entityGroupTypeUnsupported: true};
  };

  // Filtered here rather than in the template, which would re-filter on every change detection pass.
  private buildGroupTypeOptions(row: FormGroup): Observable<Array<GroupTypeOption>> {
    const options: Array<GroupTypeOption> = this.allowedGroupTypes.map(value => ({value, label: this.groupTypeLabel(value)}));
    const groupTypeControl = row.get('groupType');
    return groupTypeControl.valueChanges.pipe(
      startWith(groupTypeControl.value),
      map((value: string) => {
        const search = (value ?? '').toString().trim().toLowerCase();
        if (!search) {
          return options;
        }
        return options.filter(option =>
          option.value.toLowerCase().includes(search) || option.label.toLowerCase().includes(search));
      })
    );
  }

  private groupTypeLabel(groupType: EntityType): string {
    return this.translate.instant(entityTypeTranslations.get(groupType).type);
  }

  private buildGroupNameSuggestions(row: FormGroup): Observable<Array<string>> {
    // Whole row, so changing the type refreshes the list; debounce + switchMap keep it to one request in flight.
    return row.valueChanges.pipe(
      startWith(row.value),
      debounceTime(150),
      switchMap((value: EntityGroupPattern) => this.fetchGroupNames(value.groupType, value.groupName))
    );
  }

  private fetchGroupNames(groupType: string, search: string): Observable<Array<string>> {
    const entityType = this.toEntityType(groupType);
    if (!entityType) {
      return of([]);
    }
    const pageLink = new PageLink(20, 0, search, {property: 'name', direction: Direction.ASC});
    // includeShared stays true: false empties the list for a customer user without generic read, and the edge
    // is not known here anyway, so this can only ever be a suggestion.
    return this.entityGroupService.getEntityGroups(pageLink, entityType, true, {ignoreLoading: true}).pipe(
      catchError(() => of(null)),
      map(pageData => pageData ? pageData.data.map(group => group.name).filter(name => !isReservedGroupName(name)) : [])
    );
  }

  private toEntityType(groupType: string): EntityType | null {
    if (!groupType) {
      return null;
    }
    const normalized = groupType.trim().toUpperCase().replace(/ /g, '_') as EntityType;
    return this.allowedGroupTypes.includes(normalized) ? normalized : null;
  }

  private updateModel(): void {
    if (!this.propagateChange) {
      return;
    }
    const patterns: Array<EntityGroupPattern> = this.patternsFormArray.value.map(pattern => ({
      groupType: pattern.groupType ? pattern.groupType.trim() : pattern.groupType,
      groupName: pattern.groupName ? pattern.groupName.trim() : pattern.groupName
    }));
    this.propagateChange(this.patternsFormGroup.valid && patterns.length ? patterns : null);
  }
}
