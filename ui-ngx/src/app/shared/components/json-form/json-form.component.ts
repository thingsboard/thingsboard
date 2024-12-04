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
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  Renderer2,
  SimpleChanges,
  ViewChild, ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone, isString, unwrapModule } from '@app/core/utils';
import { JsonFormProps } from './react/json-form.models';
import inspector from 'schema-inspector';
import tinycolor from 'tinycolor2';
import { DialogService } from '@app/core/services/dialog.service';
import JsonFormUtils from './react/json-form-utils';
import { JsonFormComponentData } from './json-form-component.models';
import { GroupInfo } from '@shared/models/widget.models';
import { Observable } from 'rxjs/internal/Observable';
import { forkJoin, from } from 'rxjs';
import { TbPopoverService } from '@shared/components/popover.service';

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
export class JsonFormComponent implements ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @ViewChild('reactRoot', {static: true})
  reactRootElmRef: ElementRef<HTMLElement>;

  @ViewChild('reactFullscreen', {static: true})
  reactFullscreenElmRef: ElementRef<HTMLElement>;

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
    onIconClick: this.onIconClick.bind(this),
    onToggleFullscreen: this.onToggleFullscreen.bind(this),
    onHelpClick: this.onHelpClick.bind(this)
  };

  data: JsonFormComponentData;

  model: any;
  schema: any;
  form: any;
  groupInfoes: GroupInfo[];

  isModelValid = true;

  isFullscreen = false;
  fullscreenFinishFn: (el: Element) => void;

  private reactRoot: any;

  private propagateChange = null;
  private propagateChangePending = false;
  private writingValue = false;
  private updateViewPending = false;

  constructor(public elementRef: ElementRef,
              private dialogs: DialogService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
  }

  ngOnDestroy(): void {
    this.destroyReactSchemaForm();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.propagateChange(this.data);
      }, 0);
    }
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
  }

  public validate(c: UntypedFormControl) {
    return this.isModelValid ? null : {
      modelValid: false
    };
  }

  writeValue(data: JsonFormComponentData): void {
    this.writingValue = true;
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
    this.writingValue = false;
    if (!this.isModelValid || this.updateViewPending) {
      this.updateView();
    }
}

  updateView() {
    if (!this.writingValue) {
      this.updateViewPending = false;
      if (this.data) {
        this.data.model = this.model;
        if (this.propagateChange) {
          try {
            this.propagateChange(this.data);
          } catch (e) {
            this.propagateChangePending = true;
          }
        } else {
          this.propagateChangePending = true;
        }
      }
    } else {
      this.updateViewPending = true;
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

  private onModelChange(key: (string | number)[], val: any, forceUpdate = false) {
    if (isString(val) && val === '') {
      val = undefined;
    }
    if (JsonFormUtils.updateValue(key, this.model, val) || forceUpdate) {
      this.isModelValid = this.validateModel();
      this.updateView();
    }
  }

  private onColorClick(key: (string | number)[],
                       val: tinycolor.ColorFormats.RGBA,
                       colorSelectedFn: (color: tinycolor.ColorFormats.RGBA) => void) {
    this.dialogs.colorPicker(tinycolor(val).toRgbString()).subscribe((result) => {
      if (!result?.canceled && colorSelectedFn) {
        colorSelectedFn(tinycolor(result?.color).toRgb());
      }
    });
  }

  private onIconClick(key: (string | number)[],
                      val: string,
                      iconSelectedFn: (icon: string) => void) {
    this.dialogs.materialIconPicker(val).subscribe((result) => {
      if (!result?.canceled && iconSelectedFn) {
        iconSelectedFn(result?.icon);
      }
    });
  }

  private onToggleFullscreen(fullscreenFinishFn?: (el: Element) => void) {
    this.isFullscreen = !this.isFullscreen;
    this.fullscreenFinishFn = fullscreenFinishFn;
    this.cd.markForCheck();
  }

  onFullscreenChanged(fullscreen: boolean) {
    this.formProps.isFullscreen = fullscreen;
    this.renderReactSchemaForm(false);
    if (this.fullscreenFinishFn) {
      this.fullscreenFinishFn(this.reactFullscreenElmRef.nativeElement);
      this.fullscreenFinishFn = null;
    }
  }

  private onHelpClick(event: MouseEvent, helpId: string, helpVisibleFn: (visible: boolean) => void, helpReadyFn: (ready: boolean) => void) {
    const trigger = event.currentTarget as Element;
    this.popoverService.toggleHelpPopover(trigger, this.renderer, this.viewContainerRef, helpId, '', '', null, helpVisibleFn, helpReadyFn);
  }

  private updateAndRender() {

    this.formProps.option.formDefaults.readonly = this.readonly;
    this.formProps.schema = this.schema;
    this.formProps.form = this.form;
    this.formProps.groupInfoes = this.groupInfoes;
    this.formProps.model = this.model;
    this.renderReactSchemaForm();
  }

  private renderReactSchemaForm(destroy: boolean = true) {
    if (destroy) {
      this.destroyReactSchemaForm();
    }

    // import ReactSchemaForm from './react/json-form-react';
    const reactSchemaFormObservables: Observable<any>[] = [
      from(import('react')),
      from(import('react-dom')),
      from(import('react-dom/client')),
      from(import('./react/json-form-react'))
    ];
    forkJoin(reactSchemaFormObservables).subscribe(
      (modules) => {
        const react = unwrapModule(modules[0]);
        const reactDomClient =  unwrapModule(modules[2]);
        const jsonFormReact = unwrapModule(modules[3]);
        this.reactRoot = reactDomClient.createRoot(this.reactRootElmRef.nativeElement);
        this.reactRoot.render(react.createElement(jsonFormReact, this.formProps));
      }
    );
  }

  private destroyReactSchemaForm() {
    this.reactRoot?.unmount();
  }

  private validateModel(): boolean {
    if (this.schema && this.model) {
      return JsonFormUtils.validateBySchema(this.schema, this.model).valid;
    }
    return true;
  }
}

