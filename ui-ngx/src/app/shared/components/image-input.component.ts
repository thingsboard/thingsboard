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
  ChangeDetectorRef,
  Component, EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  Output,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { Subscription } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FlowConfig } from '@flowjs/ngx-flow';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { UtilsService } from '@core/services/utils.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ImagePipe } from '@shared/pipe/image.pipe';

@Component({
    selector: 'tb-image-input',
    templateUrl: './image-input.component.html',
    styleUrls: ['./image-input.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ImageInputComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ImageInputComponent extends PageComponent implements AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input()
  accept = 'image/*';

  @Input()
  label: string;

  @Input()
  maxSizeByte: number;

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

  @Input()
  showClearButton = true;

  @Input()
  showPreview = true;

  @Input()
  inputId = this.utils.guid();

  @Input()
  allowedExtensions: string;

  @Input()
  @coerceBoolean()
  processImageApiLink = false;

  @Input()
  @coerceBoolean()
  resultAsFile = false;

  @Input()
  @coerceBoolean()
  showFileName = false;

  @Input()
  fileName: string;

  @Output()
  fileNameChanged = new EventEmitter<string>();

  imageUrl: string;
  file: File;

  safeImageUrl: SafeUrl;

  @ViewChild('flow', {static: true})
  flow: FlowConfig;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private sanitizer: DomSanitizer,
              private imagePipe: ImagePipe,
              private dialog: DialogService,
              private translate: TranslateService,
              private fileSize: FileSizePipe,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngAfterViewInit() {
    this.autoUploadSubscription = this.flow.events$.subscribe(event => {
      if (event.type === 'fileAdded') {
        const flowFile = event.event[0] as flowjs.FlowFile;
        const file = flowFile.file;
        const fileName = flowFile.name;
        if (this.maxSizeByte && this.maxSizeByte < file.size) {
          this.dialog.alert(
            this.translate.instant('dashboard.cannot-upload-file'),
            this.translate.instant('dashboard.maximum-upload-file-size', {size: this.fileSize.transform(this.maxSizeByte)})
          ).subscribe(
            () => { }
          );
          return false;
        }
        if (this.filterFile(flowFile)) {
          const reader = new FileReader();
          reader.onload = (_loadEvent) => {
            if (typeof reader.result === 'string' && reader.result.startsWith('data:image/')) {
              this.imageUrl = reader.result;
              this.safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(this.imageUrl);
              this.file = file;
              this.fileName = fileName;
              this.updateModel();
            }
          };
          reader.readAsDataURL(file);
        }
      }
    });
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

  writeValue(value: string): void {
    this.imageUrl = value;
    if (this.imageUrl) {
      if (this.processImageApiLink) {
        this.imagePipe.transform(this.imageUrl, {preview: true, ignoreLoadingImage: true}).subscribe(
          (res) => {
            this.safeImageUrl = res;
          }
        );
      } else {
        this.safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(this.imageUrl);
      }
    } else {
      this.safeImageUrl = null;
    }
  }

  private updateModel() {
    this.cd.markForCheck();
    if (this.resultAsFile) {
      this.propagateChange(this.file);
    } else {
      this.propagateChange(this.imageUrl);
    }
    this.fileNameChanged.emit(this.fileName);
  }

  private filterFile(file: flowjs.FlowFile): boolean {
    if (this.allowedExtensions) {
      return this.allowedExtensions.split(',').indexOf(file.getExtension()) > -1;
    } else {
      return true;
    }
  }

  clearImage() {
    this.imageUrl = null;
    this.safeImageUrl = null;
    this.file = null;
    this.fileName = null;
    this.updateModel();
  }
}
