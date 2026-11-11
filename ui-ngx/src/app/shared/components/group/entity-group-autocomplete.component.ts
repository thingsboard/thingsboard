// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { merge, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { isEqual, isString } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityInfoData } from '@shared/models/entity.models';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { coerceBoolean } from '@app/shared/decorators/coercion';

@Component({
    selector: 'tb-entity-group-autocomplete',
    templateUrl: './entity-group-autocomplete.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityGroupAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class EntityGroupAutocompleteComponent implements ControlValueAccessor, OnInit, OnDestroy {

  selectEntityGroupFormGroup: FormGroup;

  modelValue: EntityId | string | null = null;

  private groupTypeValue: EntityType;
  get groupType(): EntityType {
    return this.groupTypeValue;
  }

  @Input({required: true})
  set groupType(value: EntityType) {
    if (this.groupTypeValue !== value) {
      if (this.groupTypeValue) {
        this.reset();
      }
      this.groupTypeValue = value;
    }
  }

  private ownerIdValue: EntityId | null = null;
  get ownerId(): EntityId {
    return this.ownerIdValue;
  }

  @Input()
  set ownerId(value: EntityId | null) {
    if (!isEqual(this.ownerIdValue, value)) {
      this.ownerIdValue = value;
      const currentEntityGroup = this.getCurrentEntityGroup();
      const keepEntityGroup = currentEntityGroup === null;
      this.reset(keepEntityGroup);
    }
  }

  @Input()
  excludeGroupIds: Array<string>;

  @Input()
  excludeGroupAll: boolean;

  @Input()
  placeholderText: string;

  @Input({required: true})
  notFoundText: string;

  @Input()
  requiredText: string;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  useFullEntityId: boolean;

  @Output()
  entityGroupLoaded = new EventEmitter<EntityInfoData>();

  @ViewChild('entityGroupInput', {static: true}) entityGroupInput: ElementRef<HTMLInputElement>;

  filteredEntityGroups: Observable<Array<EntityInfoData>>;

  allEntityGroups: Observable<Array<EntityInfoData>>;

  searchText = '';

  private pristine = true;
  private cleanFilteredEntityGroups: Subject<Array<EntityInfoData>> = new Subject();

  private propagateChange = (v: any) => { };

  constructor(public translate: TranslateService,
              private entityGroupService: EntityGroupService,
              private fb: FormBuilder) {
    this.selectEntityGroupFormGroup = this.fb.group({
      entityGroup: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    const getEntityGroups =  this.selectEntityGroupFormGroup.get('entityGroup').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = this.useFullEntityId ? value.id : value.id.id;
          }
          this.updateView(modelValue, value);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        switchMap(name => this.fetchEntityGroups(name)),
        share()
      );

    this.filteredEntityGroups = merge(
      this.cleanFilteredEntityGroups,
      getEntityGroups
    );
  }

  ngOnDestroy() {
    this.cleanFilteredEntityGroups.complete();
    this.cleanFilteredEntityGroups = null;
  }

  getCurrentEntityGroup(): EntityInfoData | null {
    const currentEntityGroup = this.selectEntityGroupFormGroup.get('entityGroup').value;
    if (currentEntityGroup && typeof currentEntityGroup !== 'string') {
      return currentEntityGroup as EntityInfoData;
    } else {
      return null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectEntityGroupFormGroup.disable({emitEvent: false});
    } else {
      this.selectEntityGroupFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | EntityInfoData | EntityId | null): void {
    this.searchText = '';
    if (value !== null) {
      if ((value as EntityInfoData)?.id?.id) {
        const entityGroup = value as EntityInfoData;
        this.modelValue = this.useFullEntityId ? entityGroup.id : entityGroup.id.id;
        this.selectEntityGroupFormGroup.get('entityGroup').patchValue(entityGroup, {emitEvent: false});
        this.entityGroupLoaded.next(entityGroup);
      } else {
        let groupId: string;
        if (typeof value === 'string') {
          groupId = value;
        } else {
          groupId = (value as EntityId).id;
        }
        this.entityGroupService.getEntityGroup(groupId, {ignoreLoading: true, ignoreErrors: true}).subscribe({
          next: ({ name, id, ownerId, type }) => {
            const entityGroup = { name, id };
            this.modelValue = this.useFullEntityId ? id : id.id;
            this.ownerIdValue = ownerId;
            this.groupTypeValue = type;
            this.selectEntityGroupFormGroup.get('entityGroup').patchValue(entityGroup, {emitEvent: false});
            this.entityGroupLoaded.next(entityGroup);
          },
          error: () => {
            this.modelValue = null;
            this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
            this.entityGroupLoaded.next(null);
          }
        });
      }
    } else {
      this.modelValue = null;
      this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
      this.entityGroupLoaded.next(null);
    }
    this.pristine = true;
  }

  onFocus() {
    if (this.pristine) {
      this.selectEntityGroupFormGroup.get('entityGroup').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.pristine = false;
    }
  }

  reset(keepEntityGroup = false) {
    this.cleanFilteredEntityGroups.next([]);
    this.allEntityGroups = null;
    this.pristine = true;
    if (!keepEntityGroup) {
      this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
      setTimeout(() => this.updateView(null, this.getCurrentEntityGroup()));
    }
  }

  updateView(value: EntityId | string | null, entityGroup: EntityInfoData | string | null ) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
      if (!(isString(entityGroup) || entityGroup === null)) {
        // @ts-ignore
        this.entityGroupLoaded.next(entityGroup);
      }
    }
  }

  displayEntityGroupFn(entityGroup?: EntityInfoData): string | undefined {
    return entityGroup ? entityGroup.name : undefined;
  }

  fetchEntityGroups(searchText?: string): Observable<Array<EntityInfoData>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getEntityGroups(pageLink).pipe(
      catchError(() => of(emptyPageData<EntityInfoData>())),
      map(pageData => {
        let data = pageData.data;
        if (this.excludeGroupAll) {
          data = data.filter(group => group.name !== 'All');
        }
        if (this.excludeGroupIds && this.excludeGroupIds.length) {
          const groups: Array<EntityInfoData> = [];
          data.forEach((group) => {
            if (this.excludeGroupIds.indexOf(group.id.id) === -1) {
              groups.push(group);
            }
          });
          return groups;
        } else {
          return data;
        }
      })
    );
  }

  getEntityGroups(pageLink: PageLink): Observable<PageData<EntityInfoData>> {
    if (this.ownerId) {
      return this.entityGroupService
        .getEntityGroupEntityInfosByOwnerId(pageLink, this.ownerId.entityType as EntityType,
          this.ownerId.id, this.groupType, {ignoreLoading: true});
    } else {
      return this.entityGroupService.getEntityGroupEntityInfos(pageLink, this.groupType, true, {ignoreLoading: true});
    }
  }

  clear() {
    this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.entityGroupInput.nativeElement.blur();
      this.entityGroupInput.nativeElement.focus();
    }, 0);
  }

}
