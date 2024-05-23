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
  ChangeDetectorRef,
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { IotSvgTag } from '@home/components/widget/lib/svg/iot-svg.models';
import { MatExpansionPanel } from '@angular/material/expansion';
import { JsFuncComponent } from '@shared/components/js-func.component';
import { MatSelect } from '@angular/material/select';

@Component({
  selector: 'tb-scada-symbol-metadata-tag',
  templateUrl: './scada-symbol-metadata-tag.component.html',
  styleUrls: ['./scada-symbol-metadata-tag.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolMetadataTagComponent implements ControlValueAccessor, OnInit, OnChanges {

  @ViewChild('tagSelect')
  tagSelect: MatSelect;

  @ViewChild('expansionPanel')
  expansionPanel: MatExpansionPanel;

  @ViewChild('renderFunctionExpansionPanel')
  renderFunctionExpansionPanel: MatExpansionPanel;

  @ViewChild('clickActionExpansionPanel')
  clickActionExpansionPanel: MatExpansionPanel;

  @ViewChild('stateRenderFunction')
  stateRenderFunction: JsFuncComponent;

  @ViewChild('clickAction')
  clickAction: JsFuncComponent;

  @Input()
  disabled: boolean;

  @Input()
  tags: string[];

  @Output()
  tagRemoved = new EventEmitter();

  availableTags: string[];

  tagFormGroup: UntypedFormGroup;

  modelValue: IotSvgTag;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.availableTags = (this.tags || []).concat();
    this.tagFormGroup = this.fb.group({
      tag: [null, [Validators.required]],
      stateRenderFunction: [null, []],
      clickAction: [null, []]
    });
    this.tagFormGroup.valueChanges.subscribe(
      () => this.updateModel()
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['tags'].includes(propName)) {
          this.availableTags = (this.tags || []).concat();
          if (this.modelValue?.tag && !this.availableTags.includes(this.modelValue?.tag)) {
            this.availableTags.push(this.modelValue?.tag);
            this.availableTags.sort();
          }
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
      this.tagFormGroup.disable({emitEvent: false});
    } else {
      this.tagFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: IotSvgTag): void {
    this.modelValue = value;
    if (value?.tag && !(this.tags || []).includes(value?.tag)) {
      this.availableTags = (this.tags || []).concat(value.tag).sort();
    }
    const clickAction = value?.actions && value?.actions.click ? value.actions.click.actionFunction : null;
    this.tagFormGroup.patchValue(
      {
        tag: value?.tag,
        stateRenderFunction: value?.stateRenderFunction,
        clickAction
      }, {emitEvent: false}
    );
  }

  editTagStateRenderFunction(): void {
    this.openPanelWithCallback(this.expansionPanel, () => {
      this.openPanelWithCallback(this.renderFunctionExpansionPanel, () => {
        this.stateRenderFunction.focus();
      });
    });
  }

  editClickAction(): void {
    this.openPanelWithCallback(this.expansionPanel, () => {
      this.openPanelWithCallback(this.clickActionExpansionPanel, () => {
        this.clickAction.focus();
      });
    });
  }

  focus() {
    this.tagSelect._elementRef.nativeElement.scrollIntoView();
    this.tagSelect.focus();
  }

  private openPanelWithCallback(panel: MatExpansionPanel, callback: () => void) {
    if (!panel.expanded) {
      const s = panel.afterExpand.subscribe(() => {
        s.unsubscribe();
        setTimeout(() => {
          callback();
        });
      });
      panel.open();
    } else {
      callback();
    }
  }

  private updateModel() {
    const value = this.tagFormGroup.value;
    if (value.tag && !this.modelValue?.tag) {
      this.expansionPanel.open();
    }
    this.modelValue = {
      tag: value.tag,
      stateRenderFunction: value.stateRenderFunction
    };
    if (value.clickAction) {
      this.modelValue.actions = {
        click: {
          actionFunction: value.clickAction
        }
      };
    } else {
      this.modelValue.actions = null;
    }
    this.propagateChange(this.modelValue);
  }
}
