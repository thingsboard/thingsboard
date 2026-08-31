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

import {
  AfterViewInit,
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
import { WidgetResource } from '@shared/models/widget.models';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import {
  AngularContainerFunctionEditorCompleter,
  HTMLContainerFunctionEditorCompleter,
  HtmlContainerWidgetSettings,
  HtmlContainerWidgetType
} from '@home/components/widget/lib/html/html-container-widget.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isJSResource } from '@shared/models/resource.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-html-container-settings',
  templateUrl: './html-container-settings.component.html',
  styleUrls: ['./html-container-settings.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => HtmlContainerSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => HtmlContainerSettingsComponent),
      multi: true,
    }
  ],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class HtmlContainerSettingsComponent implements OnInit, AfterViewInit, ControlValueAccessor, Validator {

  HtmlContainerWidgetType = HtmlContainerWidgetType;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  get containerFunctionEditorCompleter() {
    return this.htmlContainerSettingsForm.get('type').value === HtmlContainerWidgetType.ANGULAR
      ? AngularContainerFunctionEditorCompleter
      : HTMLContainerFunctionEditorCompleter;
  }

  @HostBinding('class')
  hostClass = 'tb-html-container-settings';

  @ViewChild('leftPanel', { read: ElementRef })
  leftPanelElmRef!: ElementRef;

  @ViewChild('rightPanel', { read: ElementRef })
  rightPanelElmRef!: ElementRef;

  @Input()
  disabled: boolean;

  fullscreen = false;

  tabsAnimationDuration = '500ms';

  htmlContainerSettingsForm: UntypedFormGroup;

  private modelValue: HtmlContainerWidgetSettings;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
  }

  get resourcesFormArray(): UntypedFormArray {
    return this.htmlContainerSettingsForm.get('resources') as UntypedFormArray;
  }

  get resourcesControls(): UntypedFormGroup[] {
    return this.resourcesFormArray.controls as UntypedFormGroup[];
  }

  ngOnInit(): void {
    this.htmlContainerSettingsForm = this.fb.group({
      type: [null, []],
      html: [null, []],
      css: [null, []],
      js: [null, []],
      resources: this.fb.array([])
    });
    this.htmlContainerSettingsForm.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateResources());
    this.htmlContainerSettingsForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngAfterViewInit(): void {
    if (this.leftPanelElmRef && this.rightPanelElmRef) {
      this.initSplitLayout(this.leftPanelElmRef.nativeElement,
        this.rightPanelElmRef.nativeElement);
    }
  }

  private initSplitLayout(leftPanel: any, rightPanel: any) {
    Split([leftPanel, rightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.htmlContainerSettingsForm.disable({emitEvent: false});
    } else {
      this.htmlContainerSettingsForm.enable({emitEvent: false});
    }
  }

  writeValue(value: HtmlContainerWidgetSettings): void {
    this.modelValue = value;
    this.htmlContainerSettingsForm.get('type').patchValue(value.type, {emitEvent: false});
    this.htmlContainerSettingsForm.get('html').patchValue(value.html, {emitEvent: false});
    this.htmlContainerSettingsForm.get('css').patchValue(value.css, {emitEvent: false});
    this.htmlContainerSettingsForm.get('js').patchValue(value.js, {emitEvent: false});
    this.resourcesFormArray.clear({emitEvent: false});
    value.resources.forEach(r => {
      this.resourcesFormArray.push(this.buildResourceFormGroup(r), {emitEvent: false});
    });
  }

  validate(_c: UntypedFormControl) {
    return this.htmlContainerSettingsForm.valid ? null : {
      htmlContainerSettings: {
        valid: false,
      }
    };
  }

  addResource() {
    const newResource: WidgetResource = {
      url: '',
      isModule: false
    };
    this.resourcesFormArray.push(this.buildResourceFormGroup(newResource));
  }

  removeResource(index: number) {
    this.resourcesFormArray.removeAt(index);
  }

  toggleFullScreen(): void {
    this.fullscreen = !this.fullscreen;
    this.tabsAnimationDuration = '0ms';
    setTimeout(() => {
      this.tabsAnimationDuration = '500ms';
    });
  }

  private propagateChange = (_v: any) => { };

  private updateModel() {
    this.modelValue = this.htmlContainerSettingsForm.value;
    this.propagateChange(this.modelValue);
  }

  private updateResources() {
    if (this.htmlContainerSettingsForm.get('type').value === HtmlContainerWidgetType.PLAIN) {
      const resources: WidgetResource[] = this.resourcesFormArray.value;
      const filtered = resources.filter(r => !isJSResource(r.url));
      let updated = filtered.length !== resources.length;
      filtered.forEach((r) => {
        if (r.isModule) {
          r.isModule = false;
          updated = true;
        }
      });
      if (updated) {
        this.resourcesFormArray.clear();
        filtered.forEach(r => {
          this.resourcesFormArray.push(this.buildResourceFormGroup(r));
        });
      }
    }
  }

  private buildResourceFormGroup(resource: WidgetResource): UntypedFormGroup {
    return this.fb.group({
      url: [resource.url, [Validators.required]],
      isModule: [resource.isModule]
    });
  }
}
