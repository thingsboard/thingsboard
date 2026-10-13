// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';
import { coerceBoolean } from '@shared/decorators/coercion';

type OriginatorType = 'entity' | 'groupTenant' | 'ownerGroup';

@Component({
    selector: 'tb-originator-select',
    templateUrl: './originator-select.component.html',
    styleUrls: ['./originator-select.component.scss'],
    encapsulation: ViewEncapsulation.None,
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => OriginatorSelectComponent),
            multi: true
        }],
    standalone: false
})
export class OriginatorSelectComponent implements ControlValueAccessor, OnInit, OnDestroy {

  originatorFormGroup: UntypedFormGroup;

  modelValue: EntityId | null;

  @Input()
  allowedEntityTypes: Array<EntityType>;

  @Input()
  singleEntityText = 'scheduler.single-entity';

  @Input()
  groupOfEntitiesText = 'scheduler.group-entities';

  @Input()
  entitiesGroupOwnerText = 'scheduler.entities-group-owner';
  headerOptions: ToggleHeaderOption[] = null;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Output()
  currentGroupType = new EventEmitter<EntityType>();

  currentUser = getCurrentAuthUser(this.store);

  private destroy$ = new Subject<void>();

  private loadData = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private fb: UntypedFormBuilder) {
    this.originatorFormGroup = this.fb.group({
      originator: ['entity'],
      entityOriginatorId: [null],
      groupOriginatorId: [null],
      groupOwnerId: [null]
    });
    this.originatorFormGroup.get('originator').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      (originator: OriginatorType) => {
        if (originator === 'groupTenant' || originator === 'ownerGroup') {
          const originatorId = {
            entityType: EntityType.ENTITY_GROUP,
            id: null
          };
          this.originatorFormGroup.patchValue({
            groupOriginatorId: originatorId,
            groupOwnerId: null
          }, {emitEvent: false});
        } else if (originator === 'entity') {
          this.originatorFormGroup.get('entityOriginatorId').patchValue(null, {emitEvent: false});
        }
      }
    );
    this.originatorFormGroup.get('groupOwnerId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      () => {
        this.originatorFormGroup.patchValue({
          groupOriginatorId: null
        }, {emitEvent: false});
      }
    );
    this.originatorFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateView(value);
    });
  }

  get originator(): OriginatorType {
    return this.originatorFormGroup.get('originator').value;
  }

  get isSingleEntityMode(): boolean {
    return this.originator === 'entity';
  }

  get isSingleAllowedEntityType(): boolean {
    return this.allowedEntityTypes?.length === 1;
  }

  get singleAllowedEntityType(): EntityType {
    return this.allowedEntityTypes?.[0];
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.headerOptions = [
      {
        name: this.translate.instant(this.singleEntityText),
        value: 'entity'
      },
      {
        name: this.translate.instant(this.groupOfEntitiesText),
        value: 'groupTenant'
      },
      {
        name: this.translate.instant(this.entitiesGroupOwnerText),
        value: 'ownerGroup'
      }
    ];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.originatorFormGroup.disable({emitEvent: false});
    } else {
      this.originatorFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityId | null): void {
    this.modelValue = value;
    if (this.modelValue && this.modelValue.entityType === EntityType.ENTITY_GROUP) {
      this.originatorFormGroup.patchValue({
        originator: 'groupTenant',
        groupOriginatorId: this.modelValue
      }, {emitEvent: false});
      this.loadData = true;
    } else {
      this.originatorFormGroup.patchValue({
        originator: 'entity',
        entityOriginatorId: value,
        groupOriginatorId: null
      }, {emitEvent: false});
    }
  }

  entityGroupLoaded(entityGroup: EntityGroupInfo) {
    if (this.loadData) {
      this.loadData = false;
      if (isDefinedAndNotNull(entityGroup)) {
        if (this.currentUser.authority === Authority.TENANT_ADMIN && entityGroup.ownerId?.id !== this.currentUser.tenantId) {
          this.originatorFormGroup.get('originator').patchValue('ownerGroup', {emitEvent: false});
          this.originatorFormGroup.get('groupOwnerId').patchValue(entityGroup.ownerId, {emitEvent: false});
        } else {
          this.originatorFormGroup.get('originator').patchValue('groupTenant', {emitEvent: false});
          this.originatorFormGroup.get('groupOwnerId').patchValue(null, {emitEvent: false});
        }
      } else {
        this.originatorFormGroup.patchValue({
          originator: 'groupTenant',
          groupOwnerId: null,
          groupOriginatorId: null
        }, {emitEvent: true});
      }
    }
  }


  updateView(value: {originator: string; entityOriginatorId: EntityId; groupOriginatorId: EntityId} | null) {
    let originatorId = null;
    if (value) {
      originatorId = value.originator !== 'entity' ? value.groupOriginatorId : value.entityOriginatorId;
    }
    if (!isEqual(this.modelValue, originatorId)) {
      this.modelValue = originatorId;
      if (this.modelValue && this.modelValue.id) {
        this.propagateChange(this.modelValue);
      } else {
        this.propagateChange(null);
      }
    }
  }
}
