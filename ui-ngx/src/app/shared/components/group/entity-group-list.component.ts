// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId, entityIdEquals } from '@shared/models/id/entity-id';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipGrid } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { EntityInfoData } from '@shared/models/entity.models';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { ENTER } from '@angular/cdk/keycodes';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';

export type CreateEntityGroupFunction = (groupType: EntityType, groupName?: string, ownerId?: EntityId) => Observable<EntityInfoData>;

@Component({
    selector: 'tb-entity-group-list',
    templateUrl: './entity-group-list.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityGroupListComponent),
            multi: true
        }
    ],
    standalone: false
})
export class EntityGroupListComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges {

  entityGroupListFormGroup: UntypedFormGroup;

  modelValue: Array<string> | null;

  @Input()
  groupType: EntityType;

  private ownerIdValue: EntityId;
  get ownerId(): EntityId {
    return this.ownerIdValue;
  }

  @Input()
  set ownerId(value: EntityId) {
    if (!entityIdEquals(this.ownerIdValue, value)) {
      this.reset();
    }
    this.ownerIdValue = value;
  }

  @Input()
  excludeGroupAll: boolean;

  @Input()
  labelText: string;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  useGroupInfoValue = false;

  @Input()
  createGroupFunction: CreateEntityGroupFunction;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  syncIdsWithDB = false;

  @ViewChild('entityGroupInput') entityGroupInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityGroupAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipGrid;

  entityGroups: Array<EntityInfoData> = [];
  filteredEntityGroups: Observable<Array<EntityInfoData>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder,
              public truncate: TruncatePipe) {
    this.entityGroupListFormGroup = this.fb.group({
      entityGroups: [this.entityGroups, this.required ? [Validators.required] : []],
      entityGroup: [null]
    });
  }

  updateValidators() {
    this.entityGroupListFormGroup.get('entityGroups').setValidators(this.required ? [Validators.required] : []);
    this.entityGroupListFormGroup.get('entityGroups').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntityGroups = this.entityGroupListFormGroup.get('entityGroup').valueChanges
      .pipe(
        // startWith<string | BaseData<EntityId>>(''),
        tap((value) => {
          if (value && typeof value !== 'string') {
            this.add(value);
          } else if (value === null) {
            this.clear(this.entityGroupInput.nativeElement.value);
          }
        }),
        filter((value) => typeof value === 'string'),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchEntityGroups(name) ),
        share()
      );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'groupType') {
          this.reset();
        }
      }
    }
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityGroupListFormGroup.disable({emitEvent: false});
    } else {
      this.entityGroupListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | Array<EntityInfoData> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      if ((value[0] as EntityInfoData).id) {
        const groups = value as EntityInfoData[];
        this.modelValue = groups.map(group => group.id.id);
        this.entityGroups = [...groups];
        this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      } else {
        const ids = value as string[];
        this.modelValue = [...ids];
        this.entityGroupService.getEntityGroupEntityInfosByIds(ids, {ignoreLoading: true}).subscribe(
          (entityGroups) => {
            this.entityGroups = entityGroups;
            if (this.syncIdsWithDB && this.modelValue.length !== this.entityGroups.length) {
              this.modelValue = this.entityGroups.map(entity => entity.id.id);
              if (!this.modelValue.length) {
                this.modelValue = null;
              }
              this.propagateChange(this.modelValue);
            }
          }
        );
      }
    } else {
      this.entityGroups = [];
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  reset() {
    this.entityGroups = [];
    this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    this.modelValue = null;
    if (this.entityGroupInput) {
      this.entityGroupInput.nativeElement.value = '';
    }
    this.entityGroupListFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
    this.updateView();
    this.dirty = true;
  }

  add(entityGroup: EntityInfoData): void {
    if (!this.modelValue || this.modelValue.indexOf(entityGroup.id.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entityGroup.id.id);
      this.entityGroups.push(entityGroup);
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    }
    this.updateView();
    this.clear();
  }

  remove(entityGroup: EntityInfoData) {
    const index = this.entityGroups.indexOf(entityGroup);
    if (index >= 0) {
      this.entityGroups.splice(index, 1);
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.updateView();
      this.clear();
    }
  }

  updateView() {
    if (this.useGroupInfoValue) {
      this.propagateChange(this.entityGroups);
    } else {
      this.propagateChange(this.modelValue);
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
        if (this.modelValue) {
          data = data.filter(group => !this.modelValue.includes(group.id.id));
        }
        return data;
      })
    );
  }

  getEntityGroups(pageLink: PageLink): Observable<PageData<EntityInfoData>> {
    if (this.ownerId) {
      return this.entityGroupService.getEntityGroupEntityInfosByOwnerId(
        pageLink, this.ownerId.entityType as EntityType, this.ownerId.id, this.groupType, {ignoreLoading: true});
    } else {
      return this.entityGroupService.getEntityGroupEntityInfos(pageLink, this.groupType, true, {ignoreLoading: true});
    }
  }

  onFocus() {
    if (this.dirty) {
      this.entityGroupListFormGroup.get('entityGroup').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text != null && text.length > 0) ? true : false;
  }

  clear(value: string = '') {
    this.entityGroupInput.nativeElement.value = value;
    this.entityGroupListFormGroup.get('entityGroup').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityGroupInput.nativeElement.blur();
      this.entityGroupInput.nativeElement.focus();
    }, 0);
  }

  entityGroupEnter($event: KeyboardEvent) {
    if ($event.keyCode === ENTER) {
      $event.preventDefault();
      this.createEntityGroup($event, this.searchText);
    }
  }

  createEntityGroup($event: Event, groupName: string) {
    $event.preventDefault();
    if (this.createGroupFunction) {
      this.createGroupFunction(this.groupType, groupName, this.ownerId).subscribe(
        (newGroup) => {
          if (!newGroup) {
            setTimeout(() => {
              this.entityGroupInput.nativeElement.blur();
              this.entityGroupInput.nativeElement.focus();
            }, 0);
          } else {
            this.add(newGroup);
          }
        }
      );
    }
  }

}
