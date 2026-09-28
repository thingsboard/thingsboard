// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder, UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityId, entityIdEquals } from '@app/shared/models/id/entity-id';
import { entityGroupsTitle } from '@shared/models/entity-group.models';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-owner-entity-group-list',
    templateUrl: './owner-entity-group-list.component.html',
    styleUrls: ['./owner-entity-group-list.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => OwnerEntityGroupListComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => OwnerEntityGroupListComponent),
            multi: true
        }
    ],
    standalone: false
})
export class OwnerEntityGroupListComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  entityType: EntityType;

  ownerEntityGroupListFormGroup: UntypedFormGroup;

  modelValue: Array<string> | null;

  currentUser = getCurrentAuthUser(this.store);

  private propagateChange = (v: any) => { };

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.ownerEntityGroupListFormGroup = this.fb.group({
      ownerEntityGroups: this.fb.array([], [])
    });
    this.ownerEntityGroupListFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.ownerEntityGroupListFormGroup.disable({emitEvent: false});
    } else {
      this.ownerEntityGroupListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | undefined): void {
    this.modelValue = value;
    this.ownerEntityGroupListFormGroup.setControl('ownerEntityGroups',
      this.prepareOwnerEntityGroupsFormArray(
        [{ownerId: {id: this.currentUser.tenantId, entityType: EntityType.TENANT}, groupIds: []}]), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    return this.ownerEntityGroupListFormGroup.valid && this.ownerEntityGroupsArray().length ? null : {
      ownerEntityGroups: {
        valid: false,
      },
    };
  }

  private prepareOwnerEntityGroupsFormArray(ownerGroupsArray: {ownerId: EntityId, groupIds: string[] }[] | undefined): UntypedFormArray {
    const ownerEntityGroupsControls: Array<AbstractControl> = [];
    if (ownerGroupsArray) {
      for (const ownerGroups of ownerGroupsArray) {
        ownerEntityGroupsControls.push(this.createOwnerEntityGroupsControl(ownerGroups));
      }
    }
    return this.fb.array(ownerEntityGroupsControls);
  }

  private createOwnerEntityGroupsControl(ownerGroups: {ownerId: EntityId, groupIds: string[] }): AbstractControl {
    const ownerEntityGroupsControl = this.fb.group(
      {
        ownerId: [ownerGroups.ownerId, [Validators.required]],
        groupIds: [ownerGroups.groupIds, [Validators.required]]
      }
    );
    return ownerEntityGroupsControl;
  }

  ownerEntityGroupsArray(): UntypedFormGroup[] {
    return (this.ownerEntityGroupListFormGroup.get('ownerEntityGroups') as UntypedFormArray).controls as UntypedFormGroup[];
  }

  entityGroupsTitle(): string {
    return this.entityType ? this.translate.instant(entityGroupsTitle(this.entityType)) : '';
  }

  public excludeOwnerIds(ownerEntityGroupsControl: AbstractControl): Array<string> {
    const currentOwnerId: EntityId = ownerEntityGroupsControl.get('ownerId').value;
    const value: {ownerId: EntityId, groupIds: string[] }[] =
      this.ownerEntityGroupListFormGroup.get('ownerEntityGroups').value || [];
    const excludeOwnerIds: string[] = [];
    if (value) {
      for (const ownerGroups of value) {
        if (ownerGroups.ownerId && !entityIdEquals(ownerGroups.ownerId, currentOwnerId)) {
          excludeOwnerIds.push(ownerGroups.ownerId.id);
        }
      }
    }
    return excludeOwnerIds;
  }

  public removeOwnerEntityGroups(index: number) {
    (this.ownerEntityGroupListFormGroup.get('ownerEntityGroups') as UntypedFormArray).removeAt(index);
  }

  public addOwnerEntityGroup() {
    const ownerEntityGroupsArray = this.ownerEntityGroupListFormGroup.get('ownerEntityGroups') as UntypedFormArray;
    const ownerGroups: {ownerId: EntityId, groupIds: string[] } = {
      ownerId: null,
      groupIds: []
    };
    const ownerEntityGroupsControl = this.createOwnerEntityGroupsControl(ownerGroups);
    ownerEntityGroupsArray.push(ownerEntityGroupsControl);
    this.ownerEntityGroupListFormGroup.updateValueAndValidity();
  }

  private updateModel() {
    const value: {ownerId: EntityId, groupIds: string[] }[] =
      this.ownerEntityGroupListFormGroup.get('ownerEntityGroups').value || [];
    let modelValue: string[] = null;
    if (value && value.length) {
      modelValue = [];
      for (const ownerGroups of value) {
        if (ownerGroups && ownerGroups.groupIds && ownerGroups.groupIds.length) {
          modelValue.push(...ownerGroups.groupIds);
        }
      }
    }
    this.modelValue = modelValue;
    this.propagateChange(this.modelValue);
  }

}
