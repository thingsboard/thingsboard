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

import {
  Component,
  forwardRef, HostBinding,
  Input,
  OnChanges,
  OnInit, QueryList,
  SimpleChanges,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { IotSvgTag } from '@home/components/widget/lib/svg/iot-svg.models';
import {
  ScadaSymbolMetadataTagComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag.component';

const tagValidator = (control: AbstractControl): ValidationErrors | null => {
  const tag: IotSvgTag = control.value;
  if (!tag.tag) {
    return {
      tag: true
    };
  }
  return null;
};

@Component({
  selector: 'tb-scada-symbol-metadata-tags',
  templateUrl: './scada-symbol-metadata-tags.component.html',
  styleUrls: ['./scada-symbol-metadata-tags.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolMetadataTagsComponent implements ControlValueAccessor, OnInit, Validator, OnChanges {

  @HostBinding('style.display') styleDisplay = 'flex';
  @HostBinding('style.overflow') styleOverflow = 'hidden';

  @ViewChildren(ScadaSymbolMetadataTagComponent)
  metadataTags: QueryList<ScadaSymbolMetadataTagComponent>;

  @Input()
  disabled: boolean;

  @Input()
  tags: string[];

  availableTags: string[];

  tagsFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit() {
    this.tagsFormGroup = this.fb.group({
      tags: this.fb.array([])
    });
    this.updateAvailableTags();
    this.tagsFormGroup.valueChanges.subscribe(
      () => {
        let tags: IotSvgTag[] = this.tagsFormGroup.get('tags').value;
        if (tags) {
          tags = tags.filter(t => !!t.tag);
        }
        this.updateAvailableTags();
        this.propagateChange(tags);
      }
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['tags'].includes(propName)) {
          this.updateAvailableTags();
        }
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
      this.tagsFormGroup.disable({emitEvent: false});
    } else {
      this.tagsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: IotSvgTag[] | undefined): void {
    const tags= value || [];
    this.tagsFormGroup.setControl('tags', this.prepareTagsFormArray(tags), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    const valid = this.tagsFormGroup.valid;
    return valid ? null : {
      tags: {
        valid: false,
      },
    };
  }

  tagsFormArray(): UntypedFormArray {
    return this.tagsFormGroup.get('tags') as UntypedFormArray;
  }

  trackByTag(index: number, tagControl: AbstractControl): any {
    return tagControl;
  }

  removeTag(index: number, emitEvent = true) {
    (this.tagsFormGroup.get('tags') as UntypedFormArray).removeAt(index, {emitEvent});
  }

  addTag() {
    this.addNewTag(null);
    setTimeout(() => {
      const tagComponent = this.metadataTags.get(this.metadataTags.length-1);
      tagComponent.focus();
    });
  }

  editTagStateRenderFunction(tag: string): void {
    this.addTagIfNotPresent(tag);
    setTimeout(() => {
      const tags: IotSvgTag[] = this.tagsFormGroup.get('tags').value;
      const index = tags.findIndex(t => t.tag === tag);
      const tagComponent = this.metadataTags.get(index);
      tagComponent?.editTagStateRenderFunction();
    });
  }

  editTagClickAction(tag: string): void {
    this.addTagIfNotPresent(tag);
    setTimeout(() => {
      const tags: IotSvgTag[] = this.tagsFormGroup.get('tags').value;
      const index = tags.findIndex(t => t.tag === tag);
      const tagComponent = this.metadataTags.get(index);
      tagComponent?.editClickAction();
    });
  }

  private addTagIfNotPresent(tag: string) {
    const tags: IotSvgTag[] = this.tagsFormGroup.get('tags').value;
    if (!tags || !tags.find(t => t.tag === tag)) {
      this.addNewTag(tag);
    }
  }

  private addNewTag(tagLabel: string) {
    const tag: IotSvgTag = {
      tag: tagLabel
    };
    const tagsArray = this.tagsFormGroup.get('tags') as UntypedFormArray;
    const tagControl = this.fb.control(tag, [tagValidator]);
    tagsArray.push(tagControl);
  }

  private prepareTagsFormArray(tags: IotSvgTag[] | undefined): UntypedFormArray {
    const tagsControls: Array<AbstractControl> = [];
    if (tags) {
      tags.forEach((tag) => {
        tagsControls.push(this.fb.control(tag, [tagValidator]));
      });
    }
    return this.fb.array(tagsControls);
  }

  private updateAvailableTags() {
    if (this.tags) {
      for (const c of this.tagsFormArray().controls) {
        const svgTag: IotSvgTag = c.value;
        if (svgTag.tag && !this.tags.includes(svgTag.tag)) {
          const index = this.tagsFormArray().controls.indexOf(c);
          this.removeTag(index, false);
        }
      }
    }
    const svgTags: IotSvgTag[] = this.tagsFormGroup.get('tags').value;
    const usedTags = (svgTags || []).filter(t => !!t.tag).map(t => t.tag);
    this.availableTags = (this.tags || []).filter(t => !usedTags.includes(t));
  }
}
