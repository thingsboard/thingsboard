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
  ElementRef,
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
  readAsBinary = false;

  private multipleFileValue = false;

  @Input()
  set multipleFile(value: boolean) {
    this.multipleFileValue = value;
    if (this.flow?.flowJs) {
      this.updateMultipleFileMode(this.multipleFile);
    }
  }

  get multipleFile(): boolean {
    return this.multipleFileValue;
  }

  @Output()
  fileNameChanged = new EventEmitter<string|string[]>();

  fileName: string | string[];
  fileContent: any;

  @ViewChild('flow', {static: true})
  flow: FlowDirective;

  @ViewChild('flowInput', {static: true})
  flowInput: ElementRef;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              public translate: TranslateService) {
    super(store);
  }

  ngAfterViewInit() {
    this.autoUploadSubscription = this.flow.events$.subscribe(event => {
      if (event.type === 'filesAdded') {
        const readers = [];
        (event.event[0] as flowjs.FlowFile[]).forEach(file => {
          if (this.filterFile(file)) {
            readers.push(this.readerAsFile(file));
          }
        });
        if (readers.length) {
          Promise.all(readers).then((filesContent) => {
            filesContent = filesContent.filter(content => content.fileContent != null);
            if (filesContent.length === 1) {
              this.fileContent = filesContent[0].fileContent;
              this.fileName = filesContent[0].fileName;
              this.updateModel();
            } else if (filesContent.length > 1) {
              this.fileContent = filesContent.map(content => content.fileContent);
              this.fileName = filesContent.map(content => content.fileName);
              this.updateModel();
            }
          });
        }
      }
    });
    if (!this.multipleFile) {
      this.updateMultipleFileMode(this.multipleFile);
    }
  }

  private readerAsFile(file: flowjs.FlowFile): Promise<any> {
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => {
        let fileName = null;
        let fileContent = null;
        if (typeof reader.result === 'string') {
          fileContent = reader.result;
          if (fileContent && fileContent.length > 0) {
            if (this.contentConvertFunction) {
              fileContent = this.contentConvertFunction(fileContent);
            }
            if (fileContent) {
              fileName = file.name;
            }
          }
        }
        resolve({fileContent, fileName});
      };
      reader.onerror = () => {
        resolve({fileContent: null, fileName: null});
      };
      if (this.readAsBinary) {
        reader.readAsBinaryString(file.file);
      } else {
        reader.readAsText(file.file);
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

  private updateMultipleFileMode(multiple: boolean) {
    this.flow.flowJs.opts.singleFile = !multiple;
    if (!multiple) {
      this.flowInput.nativeElement.removeAttribute('multiple');
    }
  }
}
