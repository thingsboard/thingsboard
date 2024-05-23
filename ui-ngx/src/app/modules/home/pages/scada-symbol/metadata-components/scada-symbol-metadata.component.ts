///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { IotSvgMetadata } from '@home/components/widget/lib/svg/iot-svg.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ToggleHeaderOption } from '@shared/components/toggle-header.component';
import { TranslateService } from '@ngx-translate/core';
import {
  ScadaSymbolMetadataTagsComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tags.component';

@Component({
  selector: 'tb-scada-symbol-metadata',
  templateUrl: './scada-symbol-metadata.component.html',
  styleUrls: ['./scada-symbol-metadata.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolMetadataComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolMetadataComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolMetadataComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @ViewChild('symbolMetadataTags')
  symbolMetadataTags: ScadaSymbolMetadataTagsComponent;

  @Input()
  disabled: boolean;

  @Input()
  tags: string[];

  private modelValue: IotSvgMetadata;

  private propagateChange = null;

  public metadataFormGroup: UntypedFormGroup;

  headerOptions: ToggleHeaderOption[] = [
    {
      name: this.translate.instant('scada.general'),
      value: 'general'
    },
    {
      name: this.translate.instant('scada.tags'),
      value: 'tags'
    },
    {
      name: this.translate.instant('scada.behavior.behavior'),
      value: 'behavior'
    },
    {
      name: this.translate.instant('scada.properties'),
      value: 'properties'
    }
  ];

  selectedOption = 'general';

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private translate: TranslateService) {
    super(store);
  }

  ngOnInit(): void {
    this.metadataFormGroup = this.fb.group({
      title: [null, [Validators.required]],
      stateRenderFunction: [null],
      tags: [null],
      behavior: [null],
      properties: [null]
    });

    this.metadataFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.metadataFormGroup.disable({emitEvent: false});
    } else {
      this.metadataFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: IotSvgMetadata): void {
    this.selectedOption = 'general';
    this.modelValue = value;
    this.metadataFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  editTagStateRenderFunction(tag: string): void {
    this.selectedOption = 'tags';
    this.symbolMetadataTags.editTagStateRenderFunction(tag);
  }

  editTagClickAction(tag: string): void {
    this.selectedOption = 'tags';
    this.symbolMetadataTags.editTagClickAction(tag);
  }

  public validate(c: UntypedFormControl) {
    const valid = this.metadataFormGroup.valid;
    return valid ? null : {
      metadata: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const metadata: IotSvgMetadata = this.metadataFormGroup.getRawValue();
    this.modelValue = metadata;
    this.propagateChange(this.modelValue);
  }
}
