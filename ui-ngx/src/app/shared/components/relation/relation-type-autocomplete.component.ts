// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, mergeMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { BroadcastService } from '@app/core/services/broadcast.service';
import { RelationTypes } from '@app/shared/models/relation.models';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceArray, coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-relation-type-autocomplete',
    templateUrl: './relation-type-autocomplete.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RelationTypeAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class RelationTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  relationTypeFormGroup: FormGroup;

  modelValue: string | null;

  @Input()
  @coerceBoolean()
  showLabel = true;

  @Input()
  @coerceArray()
  additionalClasses: Array<string>;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @ViewChild('relationTypeInput', {static: true}) relationTypeInput: ElementRef;

  filteredRelationTypes: Observable<Array<string>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              public translate: TranslateService,
              private fb: FormBuilder) {
    this.relationTypeFormGroup = this.fb.group({
      relationType: [null, this.required ? [Validators.required, Validators.maxLength(255)] : [Validators.maxLength(255)]]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    this.filteredRelationTypes = this.relationTypeFormGroup.get('relationType').valueChanges
      .pipe(
        tap(value => {
          this.updateView(value);
        }),
        // startWith<string | EntitySubtype>(''),
        map(value => value ? value : ''),
        mergeMap(type => this.fetchRelationTypes(type) )
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.relationTypeFormGroup.disable({emitEvent: false});
    } else {
      this.relationTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    this.relationTypeFormGroup.get('relationType').patchValue(value, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.relationTypeFormGroup.get('relationType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayRelationTypeFn(relationType?: string): string | undefined {
    return relationType ? relationType : undefined;
  }

  fetchRelationTypes(searchText?: string, strictMatch: boolean = false): Observable<Array<string>> {
    this.searchText = searchText;
    return of(RelationTypes).pipe(
      map(relationTypes => relationTypes.filter( relationType => {
        if (strictMatch) {
          return searchText ? relationType === searchText : false;
        } else {
          return searchText ? relationType.toUpperCase().startsWith(searchText.toUpperCase()) : true;
        }
      }))
    );
  }

  clear() {
    this.relationTypeFormGroup.get('relationType').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.relationTypeInput.nativeElement.blur();
      this.relationTypeInput.nativeElement.focus();
    }, 0);
  }

}
