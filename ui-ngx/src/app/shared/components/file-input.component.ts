///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { FlowDirective } from '@flowjs/ngx-flow';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { DialogService } from '@core/services/dialog.service';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';
import { coerceBoolean } from '@shared/decorators/coercion';

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
  hint: string;

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
  maxSizeByte: number;

  @Input()
  contentConvertFunction: (content: string) => any;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  requiredAsError: boolean;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  existingFileName: string;

  @Input()
  @coerceBoolean()
  readAsBinary = false;

  @Input()
  workFromFileObj = false;

  @Input()
  @coerceBoolean()
  asButton: boolean;

  @Input()
  uploadButtonClass = 'browse-file';

  @Input()
  uploadButtonText: string;

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

  @Output()
  mediaTypeChanged = new EventEmitter<string>();

  fileName: string | string[];
  fileContent: any;
  files: File[];

  mediaType: string;

  @ViewChild('flow', {static: true})
  flow: FlowDirective;

  @ViewChild('flowInput', {static: true})
  flowInput: ElementRef;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private translate: TranslateService,
              private dialog: DialogService,
              private fileSize: FileSizePipe) {
    super(store);
  }

  ngAfterViewInit() {
    this.autoUploadSubscription = this.flow.events$.subscribe(event => {
      if (event.type === 'filesAdded') {
        const readers = [];
        let showMaxSizeAlert = false;
        (event.event[0] as flowjs.FlowFile[]).forEach(file => {
          if (this.filterFile(file)) {
            if (this.checkMaxSize(file)) {
              readers.push(this.readerAsFile(file));
            } else {
              showMaxSizeAlert = true;
            }
          }
        });

        if (showMaxSizeAlert) {
          this.dialog.alert(
            this.translate.instant('dashboard.cannot-upload-file'),
            this.translate.instant('dashboard.maximum-upload-file-size', {size: this.fileSize.transform(this.maxSizeByte)})
          ).subscribe(() => { });
        }

        if (readers.length) {
          Promise.all(readers).then((files) => {
            const validResults = files.filter(file => file.fileContent != null || file.files != null);

            if (validResults.length === 1) {
              this.fileContent = validResults[0].fileContent;
              this.fileName = validResults[0].fileName;
              this.files = validResults[0].files;
              this.mediaType = validResults[0].mediaType;
              this.updateModel();
            } else if (validResults.length > 1) {
              this.fileContent = validResults.map(content => content.fileContent);
              this.fileName = validResults.map(content => content.fileName);
              this.files = validResults.map(content => content.files);
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
      if (this.workFromFileObj) {
        resolve({
          fileContent: null,
          fileName: file.name,
          files: file.file,
          mediaType: file.file.type || null
        });
        return;
      }

      const reader = new FileReader();
      reader.onload = () => {
        let fileName = null;
        let fileContent = null;
        let mediaType = null;
        if (reader.readyState === reader.DONE) {
          fileContent = reader.result;
          if (fileContent && fileContent.length > 0) {
            if (this.contentConvertFunction) {
              fileContent = this.contentConvertFunction(fileContent);
            }
            fileName = fileContent ? file.name : null;
            mediaType = file?.file?.type || null;
          }
        }
        resolve({fileContent, fileName, files: null, mediaType});
      };
      reader.onerror = () => {
        resolve({fileContent: null, fileName: null, files: null, mediaType: null});
      };
      if (this.readAsBinary) {
        reader.readAsBinaryString(file.file);
      } else {
        reader.readAsText(file.file);
      }
    });
  }

  private checkMaxSize(file: flowjs.FlowFile): boolean {
    return !this.maxSizeByte || file.size <= this.maxSizeByte;
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
    let fileName = null;
    if (this.workFromFileObj && value instanceof File) {
      fileName = Array.isArray(value) ? value.map(file => file.name) : value.name;
    }
    this.fileName = this.existingFileName || fileName;
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
    if (this.workFromFileObj) {
      this.propagateChange(this.files);
    } else {
      this.propagateChange(this.fileContent);
      this.mediaTypeChanged.emit(this.mediaType);
      this.fileNameChanged.emit(this.fileName);
    }
  }

  clearFile() {
    this.fileName = null;
    this.fileContent = null;
    this.files = null;
    this.updateModel();
  }

  private updateMultipleFileMode(multiple: boolean) {
    this.flow.flowJs.opts.singleFile = !multiple;
    if (!multiple) {
      this.flowInput.nativeElement.removeAttribute('multiple');
    }
  }
}
