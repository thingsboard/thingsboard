// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Inject,
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
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipGrid } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AddEntityGroupsToEdgeDialogData } from '@home/dialogs/add-entity-groups-to-edge-dialog.models';
import { CustomerService } from '@core/http/customer.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityInfoData } from '@shared/models/entity.models';

@Component({
    selector: 'tb-edge-entity-group-list',
    templateUrl: './edge-entity-group-list.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EdgeEntityGroupListComponent),
            multi: true
        }
    ],
    standalone: false
})
export class EdgeEntityGroupListComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges {

  edgeEntityGroupListFormGroup: UntypedFormGroup;

  modelValue: Array<string> | null;

  @Input()
  groupType: EntityType;

  @Input()
  excludeGroupAll: boolean;

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
  disabled: boolean;

  @ViewChild('entityGroupInput') entityGroupInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityGroupAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipGrid;

  entityGroups: Array<EntityInfoData> = [];
  filteredEntityGroups: Observable<Array<EntityInfoData>>;
  ownerId: EntityId;
  edgeId: string;
  tenantId: string;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityGroupsToEdgeDialogData,
              private fb: UntypedFormBuilder) {
    const authUser = getCurrentAuthUser(this.store);
    this.tenantId = authUser.tenantId;
    this.ownerId = this.data.ownerId;
    this.edgeId = this.data.edgeId;
    this.groupType = this.data.groupType;
    this.edgeEntityGroupListFormGroup = this.fb.group({
      entityGroups: [this.entityGroups, this.required ? [Validators.required] : []],
      entityGroup: [null]
    });
  }

  updateValidators() {
    this.edgeEntityGroupListFormGroup.get('entityGroups').setValidators(this.required ? [Validators.required] : []);
    this.edgeEntityGroupListFormGroup.get('entityGroups').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntityGroups = this.edgeEntityGroupListFormGroup.get('entityGroup').valueChanges
      .pipe(
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
      this.edgeEntityGroupListFormGroup.disable({emitEvent: false});
    } else {
      this.edgeEntityGroupListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<string> | Array<EntityInfoData> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      if ((value[0] as EntityInfoData).id) {
        const groups = value as EntityInfoData[];
        this.modelValue = groups.map(group => group.id.id);
        this.entityGroups = [...groups];
        this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      } else {
        const ids = value as string[];
        this.modelValue = [...ids];
        this.entityGroupService.getEntityGroupEntityInfosByIds(ids, {ignoreLoading: true}).subscribe(
          (entityGroups) => {
            this.entityGroups = entityGroups;
            this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
          }
        );
      }
    } else {
      this.entityGroups = [];
      this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  reset() {
    this.entityGroups = [];
    this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    this.modelValue = null;
    if (this.entityGroupInput) {
      this.entityGroupInput.nativeElement.value = '';
    }
    this.edgeEntityGroupListFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
    this.propagateChange(this.modelValue);
    this.dirty = true;
  }

  add(entityGroup: EntityInfoData): void {
    if (!this.modelValue || this.modelValue.indexOf(entityGroup.id.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entityGroup.id.id);
      this.entityGroups.push(entityGroup);
      this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    }
    this.propagateChange(this.modelValue);
    this.clear();
  }

  remove(entityGroup: EntityInfoData) {
    const index = this.entityGroups.indexOf(entityGroup);
    if (index >= 0) {
      this.entityGroups.splice(index, 1);
      this.edgeEntityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.propagateChange(this.modelValue);
      this.clear();
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
        return data;
      })
    );
  }

  getEntityGroups(pageLink: PageLink): Observable<PageData<EntityInfoData>> {
    return this.entityGroupService.getEntityGroupEntityInfosHierarchyByOwnerId(pageLink, this.ownerId.entityType as EntityType,
      this.ownerId.id, this.groupType, {ignoreLoading: true});
  }

  onFocus() {
    if (this.dirty) {
      this.edgeEntityGroupListFormGroup.get('entityGroup').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear(value: string = '') {
    this.entityGroupInput.nativeElement.value = value;
    this.edgeEntityGroupListFormGroup.get('entityGroup').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityGroupInput.nativeElement.blur();
      this.entityGroupInput.nativeElement.focus();
    }, 0);
  }
}
