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

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subscription } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FlowDirective } from '@flowjs/ngx-flow';

@Component({
  selector: 'tb-file-input',
  templateUrl: './file-input.component.html',
  styleUrls: ['./file-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FileInputComponent),
      multi: true
    }
  ]
})
export class FileInputComponent extends PageComponent implements AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  @Input()
  accept = '*/*';

  @Input()
  allowedExtensions: string;

  @Input()
  dropLabel: string;

  @Input()
  contentConvertFunction: (content: string) => any;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  @Input()
  disabled: boolean;

  fileName: string;
  fileContent: any;

  @ViewChild('flow', {static: true})
  flow: FlowDirective;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngAfterViewInit() {
    this.autoUploadSubscription = this.flow.events$.subscribe(event => {
      if (event.type === 'fileAdded') {
        const file = event.event[0] as flowjs.FlowFile;
        if (this.filterFile(file)) {
          const reader = new FileReader();
          reader.onload = (loadEvent) => {
            if (typeof reader.result === 'string') {
              const fileContent = reader.result;
              if (fileContent && fileContent.length > 0) {
                if (this.contentConvertFunction) {
                  this.fileContent = this.contentConvertFunction(fileContent);
                } else {
                  this.fileContent = fileContent;
                }
                if (this.fileContent) {
                  this.fileName = file.name;
                } else {
                  this.fileName = null;
                }
                this.updateModel();
              }
            }
          };
          reader.readAsText(file.file);
        }
      }
    });
  }

  private filterFile(file: flowjs.FlowFile): boolean {
    if (this.allowedExtensions) {
      return this.allowedExtensions.split(',').indexOf(file.getExtension()) > -1;
    } else {
      return true;
    }
  }

  ngOnDestroy() {
    this.autoUploadSubscription.unsubscribe();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: any): void {
    this.fileName = null;
  }

  private updateModel() {
    this.propagateChange(this.fileContent);
  }

  clearFile() {
    this.fileName = null;
    this.fileContent = null;
    this.updateModel();
  }
}
