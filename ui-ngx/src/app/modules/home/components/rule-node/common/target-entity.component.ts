// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormGroup, NG_VALUE_ACCESSOR, UntypedFormBuilder } from '@angular/forms';
import { EntityService } from '@app/core/http/entity.service';
import { coerceBoolean } from '@app/shared/decorators/coercion';
import { EntityType } from '@app/shared/models/entity-type.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';

@Component({
    selector: 'tb-target-entity',
    templateUrl: './target-entity.component.html',
    styleUrls: ['./target-entity.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TargetEntityComponent),
            multi: true
        }
    ],
    standalone: false
})

export class TargetEntityComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input() allowedGroupTypes: Array<EntityType>;

  @Input()
  @coerceBoolean()
  isTypeSelected = false;

  private propagateChange = null;
  public targetEntityControlGroup: FormGroup;
  private destroy$ = new Subject<void>();

  constructor(private fb: UntypedFormBuilder,
              private entityService: EntityService) {
  }

  ngOnInit(): void {
    this.targetEntityControlGroup = this.fb.group({
      entityGroupId: [null, []],
      groupOwnerId: [null, []]
    });

    this.targetEntityControlGroup.get('entityGroupId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.propagateChange(value);
    })

    this.targetEntityControlGroup.get('groupOwnerId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.targetEntityControlGroup.get('entityGroupId').patchValue(null, {emitEvent: false});
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.targetEntityControlGroup.disable({emitEvent: false});
    } else {
      this.targetEntityControlGroup.enable({emitEvent: false});
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(entityId: EntityId): void {
    if (entityId?.entityType === EntityType.ENTITY_GROUP) {
      this.targetEntityControlGroup.get('entityGroupId').patchValue(entityId, {emitEvent: false});
      if (entityId) {
        this.entityService.getEntity(entityId.entityType as EntityType, entityId.id).pipe(
          takeUntil(this.destroy$)
        ).subscribe((value: BaseData<EntityId>) => {
          this.targetEntityControlGroup.get('groupOwnerId').patchValue(value.ownerId, {emitEvent: false});
        });
      }
    }
  }
}
