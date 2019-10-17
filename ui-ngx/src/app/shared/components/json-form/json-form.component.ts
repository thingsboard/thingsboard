///
/// Copyright © 2016-2019 The Thingsboard Authors
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
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone, isString } from '@app/core/utils';
import { TranslateService } from '@ngx-translate/core';
import { JsonFormProps } from './react/json-form.models';
import inspector from 'schema-inspector';
import * as tinycolor from 'tinycolor2';
import { DialogService } from '@app/core/services/dialog.service';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import ReactSchemaForm from './react/json-form-react';
import JsonFormUtils from './react/json-form-utils';

@Component({
  selector: 'tb-json-form',
  templateUrl: './json-form.component.html',
  styleUrls: ['./json-form.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsonFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => JsonFormComponent),
      multi: true,
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class JsonFormComponent implements OnInit, ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @ViewChild('reactRoot', {static: true})
  reactRootElmRef: ElementRef<HTMLElement>;

  @Input() schema: any;

  @Input() form: any;

  @Input() groupInfoes: any[];

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }
  @Input()
  set required(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  formProps: JsonFormProps = {
    isFullscreen: false,
    option: {
      formDefaults: {
        startEmpty: true
      }
    },
    onModelChange: this.onModelChange.bind(this),
    onColorClick: this.onColorClick.bind(this),
    onToggleFullscreen: this.onToggleFullscreen.bind(this)
  };

  model: any;

  isModelValid = true;

  isFullscreen = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              private translate: TranslateService,
              private dialogs: DialogService,
              protected store: Store<AppState>) {
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
    this.destroyReactSchemaForm();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
  }

  public validate(c: FormControl) {
    return this.isModelValid ? null : {
      modelValid: false
    };
  }

  writeValue(value: any): void {
    this.model = value || {};
    this.model = inspector.sanitize(this.schema, this.model).data;
    this.updateAndRender();
    this.isModelValid = this.validateModel();
    if (!this.isModelValid) {
      this.updateView();
    }
  }

  updateView() {
    this.propagateChange(this.model);
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'schema') {
          this.model = inspector.sanitize(this.schema, this.model).data;
          this.isModelValid = this.validateModel();
        }
        if (['readonly', 'schema', 'form', 'groupInfoes'].includes(propName)) {
          this.updateAndRender();
        }
      }
    }
  }

  onFullscreenChanged() {}

  private onModelChange(key: string | string[], val: any) {
    if (isString(val) && val === '') {
      val = undefined;
    }
    if (JsonFormUtils.updateValue(key, this.model, val)) {
      this.formProps.model = this.model;
      this.updateView();
    }
  }

  private onColorClick(event: MouseEvent, key: string, val: string) {
    this.dialogs.colorPicker(tinycolor(val).toRgbString()).subscribe((color) => {
      const e = event as any;
      if (e.data && e.data.onValueChanged) {
        e.data.onValueChanged(tinycolor(color).toRgb());
      }
    });
  }

  private onToggleFullscreen() {
    this.isFullscreen = !this.isFullscreen;
    this.formProps.isFullscreen = this.isFullscreen;
  }

  private updateAndRender() {
    const schema = this.schema ? deepClone(this.schema) : {
      type: 'object'
    };
    schema.strict = true;
    const form = this.form ? deepClone(this.form) : [ '*' ];
    const groupInfoes = this.groupInfoes ? deepClone(this.groupInfoes) : [];
    this.formProps.option.formDefaults.readonly = this.readonly;
    this.formProps.schema = schema;
    this.formProps.form = form;
    this.formProps.groupInfoes = groupInfoes;
    this.formProps.model = deepClone(this.model);
    this.renderReactSchemaForm();
  }

  private renderReactSchemaForm() {
    ReactDOM.render(React.createElement(ReactSchemaForm, this.formProps), this.reactRootElmRef.nativeElement);
  }

  private destroyReactSchemaForm() {
    ReactDOM.unmountComponentAtNode(this.reactRootElmRef.nativeElement);
  }

  private validateModel(): boolean {
    if (this.schema && this.model) {
      return JsonFormUtils.validateBySchema(this.schema, this.model).valid;
    }
    return true;
  }
}

