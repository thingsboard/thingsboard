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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-script-lang',
  templateUrl: './script-lang.component.html',
  styleUrls: ['./script-lang.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TbScriptLangComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TbScriptLangComponent extends PageComponent implements ControlValueAccessor, OnInit {

  scriptLangFormGroup: UntypedFormGroup;

  scriptLanguage = ScriptLanguage;

  @Input()
  disabled: boolean;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
    this.scriptLangFormGroup = this.fb.group({
      scriptLang: [null]
    });
  }

  ngOnInit() {
    this.scriptLangFormGroup.get('scriptLang').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (scriptLang) => {
        this.updateView(scriptLang);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.scriptLangFormGroup.disable({emitEvent: false});
    } else {
      this.scriptLangFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(scriptLang: ScriptLanguage): void {
    this.scriptLangFormGroup.get('scriptLang').patchValue(scriptLang, {emitEvent: false});
  }

  updateView(scriptLang: ScriptLanguage) {
    this.propagateChange(scriptLang);
  }
}
