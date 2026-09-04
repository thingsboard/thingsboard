// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  Validators
} from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { IAliasController } from '@core/api/widget-api.models';
import { map, mergeMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-entity-alias-input',
    templateUrl: './entity-alias-input.component.html',
    styleUrls: ['./entity-alias-input.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityAliasInputComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EntityAliasInputComponent implements ControlValueAccessor, OnInit {

  @HostBinding('class')
  hostClass = 'tb-entity-alias-input';

  @ViewChild('entityAliasInput') entityAliasInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  aliasController: IAliasController;

  entityAliasFormControl: UntypedFormControl;

  filteredEntityAliases: Observable<Array<string>>;
  aliasSearchText = '';

  private entityAliasList: Array<string> = [];
  private entityAliasDirty = false;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.entityAliasFormControl = this.fb.control(null, this.required ? [Validators.required] : []);
    this.entityAliasFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );

    this.filteredEntityAliases = this.entityAliasFormControl.valueChanges
    .pipe(
      map(value => value ? value : ''),
      mergeMap(name => this.fetchEntityAliases(name) )
    );

    if (this.aliasController) {
      const entityAliases = this.aliasController.getEntityAliases();
      for (const aliasId of Object.keys(entityAliases)) {
        this.entityAliasList.push(entityAliases[aliasId].alias);
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityAliasFormControl.disable({emitEvent: false});
    } else {
      this.entityAliasFormControl.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.entityAliasFormControl.patchValue(value, {emitEvent: false});
    this.entityAliasDirty = true;
  }

  onEntityAliasFocus() {
    if (this.entityAliasDirty) {
      this.entityAliasFormControl.updateValueAndValidity({onlySelf: true});
      this.entityAliasDirty = false;
    }
  }

  clearEntityAlias() {
    this.entityAliasFormControl.patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.entityAliasInput.nativeElement.blur();
      this.entityAliasInput.nativeElement.focus();
    }, 0);
  }

  private fetchEntityAliases(searchText?: string): Observable<Array<string>> {
    this.aliasSearchText = searchText;
    let result = this.entityAliasList;
    if (searchText && searchText.length) {
      result = this.entityAliasList.filter((entityAlias) => entityAlias.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  private updateModel() {
    const value = this.entityAliasFormControl.value;
    this.propagateChange(value);
  }
}
