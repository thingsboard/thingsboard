///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subscription } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FlowDirective } from '@flowjs/ngx-flow';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';

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
export class FileInputComponent extends PageComponent implements AfterViewInit, OnDestroy, ControlValueAccessor, OnChanges {

  @Input()
  label: string;

  @Input()
  accept = '*/*';

  @Input()
  noFileText = 'import.no-file';

  @Input()
  inputId = this.utils.guid();

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

  private requiredAsErrorValue: boolean;
  get requiredAsError(): boolean {
    return this.requiredAsErrorValue;
  }
  @Input()
  set requiredAsError(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredAsErrorValue !== newVal) {
      this.requiredAsErrorValue = newVal;
    }
  }

  @Input()
  disabled: boolean;

  @Input()
  existingFileName: string;

  @Input()
  convertToBase64 = false;

  @Output()
  fileNameChanged = new EventEmitter<string>();

  fileName: string;
  fileContent: any;

  @ViewChild('flow', {static: true})
  flow: FlowDirective;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              public translate: TranslateService) {
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
              const fileContent = this.convertToBase64 ? window.btoa(reader.result) : reader.result;
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
          if (this.convertToBase64) {
            reader.readAsBinaryString(file.file);
          } else {
            reader.readAsText(file.file);
          }
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
    if (this.autoUploadSubscription) {
      this.autoUploadSubscription.unsubscribe();
    }
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
    this.fileName = this.existingFileName || null;
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (propName === 'existingFileName') {
          this.fileName = this.existingFileName || null;
        }
      }
    }
  }

  private updateModel() {
    this.propagateChange(this.fileContent);
    this.fileNameChanged.emit(this.fileName);
  }

  clearFile() {
    this.fileName = null;
    this.fileContent = null;
    this.updateModel();
  }
}
