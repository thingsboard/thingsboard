import { Component, forwardRef, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
} from '@angular/forms';
import {
  DynamicValueSourceType,
  dynamicValueSourceTypeTranslationMap,
  getDynamicSourcesForAllowUser
} from '@shared/models/query/query.models';

@Component({
  selector: 'tb-alarm-dynamic-value',
  templateUrl: './alarm-dynamic-value.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AlarmDynamicValue),
    multi: true
  }]
})

export class AlarmDynamicValue implements ControlValueAccessor, OnInit{
  public dynamicValue: FormGroup;
  public dynamicValueSourceTypes: DynamicValueSourceType[] = getDynamicSourcesForAllowUser(false);
  public dynamicValueSourceTypeTranslations = dynamicValueSourceTypeTranslationMap;
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.dynamicValue = this.fb.group({
      sourceType: [null],
      sourceAttribute: [null]
    })

    this.dynamicValue.get('sourceType').valueChanges.subscribe(
      (sourceType) => {
        if (!sourceType) {
          this.dynamicValue.get('sourceAttribute').patchValue(null, {emitEvent: false});
        }
      }
    );

    this.dynamicValue.valueChanges.subscribe(() => {
      this.updateModel();
    })
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(dynamicValue: {sourceType: string, sourceAttribute: string}): void {
    if(dynamicValue) {
      this.dynamicValue.patchValue(dynamicValue, {emitEvent: false});
    }
  }

  private updateModel() {
    this.propagateChange(this.dynamicValue.value);
  }
}
