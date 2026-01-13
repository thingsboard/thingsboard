///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { FetchTo, FetchToTranslation } from '../rule-node-config.models';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-msg-metadata-chip',
  templateUrl: './msg-metadata-chip.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MsgMetadataChipComponent),
    multi: true
  }]
})

export class MsgMetadataChipComponent implements OnInit, ControlValueAccessor {

  @Input() labelText: string;
  @Input() translation: Map<FetchTo, string> = FetchToTranslation;

  private propagateChange: (value: any) => void = () => {};

  public chipControlGroup: FormGroup;
  public selectOptions = [];

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    this.initOptions();
    this.chipControlGroup = this.fb.group({
      chipControl: [null, []]
    });

    this.chipControlGroup.get('chipControl').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
        if (value) {
          this.propagateChange(value);
        }
      }
    );
  }

  initOptions() {
    for (const key of this.translation.keys()) {
      this.selectOptions.push({
        value: key,
        name: this.translate.instant(this.translation.get(key))
      });
    }
  }

  writeValue(value: string | null): void {
    this.chipControlGroup.get('chipControl').patchValue(value, {emitEvent: false});
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.chipControlGroup.disable({emitEvent: false});
    } else {
      this.chipControlGroup.enable({emitEvent: false});
    }
  }
}
