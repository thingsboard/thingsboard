// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR, UntypedFormGroup } from '@angular/forms';
import { ConverterType, Vendor } from '@shared/models/converter.models';
import { merge, Observable, shareReplay, Subject } from 'rxjs';
import { debounceTime, map, switchMap, tap } from 'rxjs/operators';
import { ConverterLibraryService } from '@core/http/converter-library.service';
import { IntegrationType } from '@shared/models/integration.models';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { MatFormFieldAppearance } from '@angular/material/form-field';

@Component({
    selector: 'tb-converter-library-vendor-autocomplete',
    templateUrl: './converter-library-vendor-autocomplete.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ConverterLibraryVendorAutocompleteComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ConverterLibraryVendorAutocompleteComponent implements OnInit, ControlValueAccessor {
  @ViewChild('input', {static: true}) input: ElementRef;

  vendorFormGroup: UntypedFormGroup;

  private modelValue: Vendor | string | null;
  private _integrationType: IntegrationType;
  private _converterType: ConverterType;

  filteredVendors$: Observable<Array<Vendor>>;

  searchText = '';

  private refresh$ = new Subject<Array<Vendor>>();

  private dirty = false;

  private vendorsCache$: Observable<Vendor[]>;

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
  appearance: MatFormFieldAppearance = 'fill';

  constructor(private fb: FormBuilder,
              private converterLibraryService: ConverterLibraryService) {
    this.vendorFormGroup = this.fb.group({
      vendor: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredVendors$ = merge(
      this.refresh$.asObservable(),
      this.vendorFormGroup.get('vendor').valueChanges
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
          switchMap(searchText => this.fetchVendors(searchText)),
        )
    ).pipe(
      shareReplay(1),
    );
  }

  displayVendorFn(vendor?: Vendor): string {
    return vendor ? vendor.name : '';
  }

  writeValue(vendor: Vendor | string): void {
    this.searchText = '';
    if(isDefinedAndNotNull(vendor) && (vendor as Vendor)?.name) {
      this.modelValue = vendor as Vendor;
      this.vendorFormGroup.get('vendor').patchValue(vendor, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.vendorFormGroup.get('vendor').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  private updateView(value: Vendor | string | null): void {
    if(!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(value);
    }
  }

  onFocus() {
    if (this.dirty) {
      this.vendorFormGroup.get('vendor').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  clear() {
    this.vendorFormGroup.get('vendor').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.input.nativeElement.blur();
      this.input.nativeElement.focus();
    }, 0);
  }

  private reset() {
    this.vendorFormGroup.get('vendor').patchValue('', {emitEvent: false});
    this.refresh$.next(null)
    this.dirty = true;
    this.vendorsCache$ = null;
  }

  private fetchVendors(searchText: string) {
    this.searchText = searchText;
    if(!this.vendorsCache$) {
      this.vendorsCache$ = this.converterLibraryService
        .getVendors(this.integrationType, this.converterType, {ignoreLoading: true}).pipe(
          shareReplay(1)
        );
    }
    return this.vendorsCache$.pipe(
      map(values => this.filterVendors(values, searchText))
    );
  }

  private filterVendors(vendors: Vendor[], searchText: string): Vendor[] {
    const search = (searchText ?? '').toLowerCase().trim();
    if (!search) return vendors;
    return vendors.filter(v => v.name.toLowerCase().includes(search));
  }

  onBlur() {
    const control = this.vendorFormGroup.get('vendor');
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
