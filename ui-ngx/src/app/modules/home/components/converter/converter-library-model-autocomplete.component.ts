// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR, UntypedFormGroup } from '@angular/forms';
import { ConverterType, Model } from '@shared/models/converter.models';
import { IntegrationType } from '@shared/models/integration.models';
import { merge, Observable, shareReplay, Subject } from 'rxjs';
import { ConverterLibraryService } from '@core/http/converter-library.service';
import { debounceTime, map, switchMap, tap } from 'rxjs/operators';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { MatFormFieldAppearance } from '@angular/material/form-field';

@Component({
    selector: 'tb-converter-library-model-autocomplete',
    templateUrl: './converter-library-model-autocomplete.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ConverterLibraryModelAutocompleteComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ConverterLibraryModelAutocompleteComponent implements OnInit, ControlValueAccessor {
  @ViewChild('input', {static: true}) input: ElementRef;

  modelFormGroup: UntypedFormGroup;

  private modelValue: Model | string | null;
  private _integrationType: IntegrationType;
  private _converterType: ConverterType;
  private _vendorName: string;

  filteredModels$: Observable<Array<Model>>;

  searchText = '';

  private refresh$ = new Subject<Array<Model>>();

  private dirty = false;

  private modelsCache$: Observable<Model[]>;

  private propagateChange = (_val: any) => {};

  @Input()
  set integrationType(value: IntegrationType) {
    if (this._integrationType !== value) {
      this._integrationType = value;
      this.reset();
    }
  }
  get integrationType(): IntegrationType {
    return this._integrationType;
  }

  @Input()
  set converterType(value: ConverterType) {
    if (this._converterType !== value) {
      this._converterType = value;
      this.reset();
    }
  }
  get converterType(): ConverterType {
    return this._converterType;
  }

  @Input()
  set vendorName(value: string) {
    if (this._vendorName !== value) {
      this._vendorName = value;
      this.reset();
    }
  }
  get vendorName(): string {
    return this._vendorName;
  }

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  constructor(private fb: FormBuilder,
              private converterLibraryService: ConverterLibraryService) {
    this.modelFormGroup = this.fb.group({
      model: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredModels$ = merge(
      this.refresh$.asObservable(),
      this.modelFormGroup.get('model').valueChanges
        .pipe(
          debounceTime(150),
          tap((value) => {
            let modelValue = typeof value === 'string' || !value ? null : value;
            this.updateView(modelValue);
            if (value === null) {
              this.clear();
            }
          }),
          map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
          switchMap(searchText => this.fetchModels(searchText)),
        )
    ).pipe(
      shareReplay(1),
    );
  }

  displayModelFn(model?: Model): string {
    return model ? model.info.label : '';
  }

  writeValue(model: Model | string): void {
    this.searchText = '';
    if(isDefinedAndNotNull(model) && (model as Model)?.name) {
      this.modelValue = model as Model;
      this.modelFormGroup.get('model').patchValue(model, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.modelFormGroup.get('model').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  private updateView(value: Model | string | null): void {
    if(!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(value);
    }
  }

  onFocus() {
    if (this.dirty) {
      this.modelFormGroup.get('model').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear() {
    this.modelFormGroup.get('model').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.input.nativeElement.blur();
      this.input.nativeElement.focus();
    }, 0);
  }

  onLinkClick(event: MouseEvent, url: string): void {
    event.stopPropagation();
    window.open(url, '_blank');
  }

  private reset() {
    this.modelFormGroup.get('model').patchValue('', {emitEvent: false});
    this.refresh$.next(null)
    this.dirty = true;
    this.modelsCache$ = null;
  }

  private fetchModels(searchText: string) {
    this.searchText = searchText;
    if(!this.modelsCache$) {
      this.modelsCache$ = this.converterLibraryService
        .getModels(this.integrationType, this.vendorName, this.converterType, {ignoreLoading: true}).pipe(
          shareReplay(1)
        );
    }
    return this.modelsCache$.pipe(
      map(values => this.filterModels(values, searchText))
    );
  }

  private filterModels(models: Model[], searchText: string): Model[] {
    const search = (searchText ?? '').toLowerCase().trim();
    if (!search) return models;
    return models.filter(v => (v.info.label.toLowerCase() + v.info.description.toLowerCase()).includes(search));
  }

  onBlur() {
    const control = this.modelFormGroup.get('model');
    const currentErrors = control.errors || {};
    const isInvalidSelection = typeof control.value === 'string'  && control.value.length > 0;

    if (isInvalidSelection) {
      control.setErrors({ ...currentErrors, notValid: true }, { emitEvent: false });
    } else if (currentErrors.hasOwnProperty('notValid')) {
      const { notValid, ...remainingErrors } = currentErrors;
      const newErrors = Object.keys(remainingErrors).length ? remainingErrors : null;
      control.setErrors(newErrors, { emitEvent: false });
    }
  }
}
