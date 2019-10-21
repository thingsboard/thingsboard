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
import { JsonFormComponentData } from './json-form-component.models';

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

  data: JsonFormComponentData;

  model: any;
  schema: any;
  form: any;
  groupInfoes: any[];

  isModelValid = true;

  isFullscreen = false;
  targetFullscreenElement: HTMLElement;
  fullscreenFinishFn: () => void;

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

  writeValue(data: JsonFormComponentData): void {
    this.data = data;
    this.schema = this.data && this.data.schema ? deepClone(this.data.schema) : {
      type: 'object'
    };
    this.schema.strict = true;
    this.form = this.data && this.data.form ? deepClone(this.data.form) : [ '*' ];
    this.groupInfoes = this.data && this.data.groupInfoes ? deepClone(this.data.groupInfoes) : [];
    this.model = this.data && this.data.model || {};
    this.model = inspector.sanitize(this.schema, this.model).data;
    this.updateAndRender();
    this.isModelValid = this.validateModel();
    if (!this.isModelValid) {
      this.updateView();
    }
}

  updateView() {
    if (this.data) {
      this.data.model = this.model;
      this.propagateChange(this.data);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'readonly') {
          this.updateAndRender();
        }
      }
    }
  }

  private onModelChange(key: (string | number)[], val: any) {
    if (isString(val) && val === '') {
      val = undefined;
    }
    if (JsonFormUtils.updateValue(key, this.model, val)) {
      this.formProps.model = this.model;
      this.isModelValid = this.validateModel();
      this.updateView();
    }
  }

  private onColorClick(key: (string | number)[],
                       val: tinycolor.ColorFormats.RGBA,
                       colorSelectedFn: (color: tinycolor.ColorFormats.RGBA) => void) {
    this.dialogs.colorPicker(tinycolor(val).toRgbString()).subscribe((color) => {
      if (colorSelectedFn) {
        colorSelectedFn(tinycolor(color).toRgb());
      }
    });
  }

  private onToggleFullscreen(element: HTMLElement, fullscreenFinishFn?: () => void) {
    this.targetFullscreenElement = element;
    this.isFullscreen = !this.isFullscreen;
    this.fullscreenFinishFn = fullscreenFinishFn;
  }

  onFullscreenChanged(fullscreen: boolean) {
    this.formProps.isFullscreen = fullscreen;
    this.renderReactSchemaForm(false);
    if (this.fullscreenFinishFn) {
      this.fullscreenFinishFn();
      this.fullscreenFinishFn = null;
    }
  }

  private updateAndRender() {

    this.formProps.option.formDefaults.readonly = this.readonly;
    this.formProps.schema = this.schema;
    this.formProps.form = this.form;
    this.formProps.groupInfoes = this.groupInfoes;
    this.formProps.model = deepClone(this.model);
    this.renderReactSchemaForm();
  }

  private renderReactSchemaForm(destroy: boolean = true) {
    if (destroy) {
      this.destroyReactSchemaForm();
    }
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

