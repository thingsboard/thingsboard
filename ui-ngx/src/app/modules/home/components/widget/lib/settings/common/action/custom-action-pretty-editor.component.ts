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
  ElementRef,
  forwardRef,
  Input,
  QueryList,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { combineLatest } from 'rxjs';
import { CustomActionDescriptor, WidgetActionType } from '@shared/models/widget.models';
import {
  CustomPrettyActionEditorCompleter
} from '@home/components/widget/lib/settings/common/action/custom-action.models';

@Component({
    selector: 'tb-custom-action-pretty-editor',
    templateUrl: './custom-action-pretty-editor.component.html',
    styleUrls: ['./custom-action-pretty-editor.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CustomActionPrettyEditorComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class CustomActionPrettyEditorComponent implements AfterViewInit, ControlValueAccessor {

  @Input() disabled: boolean;

  action: CustomActionDescriptor;

  fullscreen = false;

  helpId= 'widget/action/custom_pretty_action_fn';

  @Input()
  set widgetActionType(type: WidgetActionType) {
    if (type === WidgetActionType.placeMapItem) {
      this.helpId = 'widget/action/place_map_item/place_map_item_action';
    } else {
      this.helpId = 'widget/action/custom_pretty_action_fn';
    }
  }

  @ViewChildren('leftPanel')
  leftPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('rightPanel')
  rightPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  customPrettyActionEditorCompleter = CustomPrettyActionEditorCompleter;

  private propagateChange = (_: any) => {};

  constructor() {
  }

  ngAfterViewInit(): void {
    combineLatest([this.leftPanelElmRef.changes, this.rightPanelElmRef.changes]).subscribe(() => {
      if (this.leftPanelElmRef.length && this.rightPanelElmRef.length) {
        this.initSplitLayout(this.leftPanelElmRef.first.nativeElement,
          this.rightPanelElmRef.first.nativeElement);
      }
    });
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
  }

  writeValue(obj: CustomActionDescriptor): void {
    this.action = obj;
  }

  public onActionUpdated(valid: boolean = true) {
    if (!valid) {
      this.propagateChange(null);
    } else {
      this.propagateChange(this.action);
    }
  }
}
