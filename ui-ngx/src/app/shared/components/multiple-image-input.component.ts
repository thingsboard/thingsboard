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

import { AfterViewInit, ChangeDetectorRef, Component, forwardRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, FormArray, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { Subscription } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FlowConfig, FlowDrop } from '@flowjs/ngx-flow';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { UtilsService } from '@core/services/utils.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { DndDropEvent } from 'ngx-drag-drop';
import { isUndefined } from '@core/utils';

@Component({
    selector: 'tb-multiple-image-input',
    templateUrl: './multiple-image-input.component.html',
    styleUrls: ['./multiple-image-input.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MultipleImageInputComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MultipleImageInputComponent extends PageComponent implements AfterViewInit, OnDestroy, ControlValueAccessor {

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
  inputId = this.utils.guid();

  imageUrls: string[];
  safeImageUrls: SafeUrl[];

  dragIndex: number;

  @ViewChild('flow', {static: true})
  flow: FlowConfig;

  @ViewChild('flowDrop', {static: true})
  flowDrop: FlowDrop;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  private viewInited = false;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private sanitizer: DomSanitizer,
              private dialog: DialogService,
              private translate: TranslateService,
              private fileSize: FileSizePipe,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngAfterViewInit() {
    this.autoUploadSubscription = this.flow.events$.subscribe(event => {
      if (event.type === 'filesAdded') {
        const readers = [];
        (event.event[0] as flowjs.FlowFile[]).forEach(file => {
          readers.push(this.readImageUrl(file));
        });
        if (readers.length) {
          Promise.all(readers).then((files) => {
            files = files.filter(file => file.imageUrl != null || file.safeImageUrl != null);
            this.imageUrls = this.imageUrls.concat(files.map(content => content.imageUrl));
            this.safeImageUrls = this.safeImageUrls.concat(files.map(content => content.safeImageUrl));
            this.updateModel();
          });
        }
      }
    });
    if (this.disabled) {
      this.flowDrop.disable();
    } else {
      this.flowDrop.enable();
    }
    this.viewInited = true;
  }

  private readImageUrl(file: flowjs.FlowFile): Promise<any> {
    return new Promise((resolve) => {
      if (this.maxSizeByte && this.maxSizeByte < file.size) {
        resolve({imageUrl: null, safeImageUrl: null});
      }
      const reader = new FileReader();
      reader.onload = () => {
        let imageUrl = null;
        let safeImageUrl = null;
        if (typeof reader.result === 'string' && reader.result.startsWith('data:image/')) {
          imageUrl = reader.result;
          if (imageUrl && imageUrl.length > 0) {
            safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(imageUrl);
          }
        }
        resolve({imageUrl, safeImageUrl});
      };
      reader.onerror = () => {
        resolve({imageUrl: null, safeImageUrl: null});
      };
      reader.readAsDataURL(file.file);
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
    if (this.viewInited) {
      if (this.disabled) {
        this.flowDrop.disable();
      } else {
        this.flowDrop.enable();
      }
    }
  }

  writeValue(value: string[]): void {
    this.imageUrls = value || [];
    this.safeImageUrls = this.imageUrls.map(imageUrl => this.sanitizer.bypassSecurityTrustUrl(imageUrl));
  }

  private updateModel() {
    this.cd.markForCheck();
    this.propagateChange(this.imageUrls);
  }

  clearImage(index: number) {
    this.imageUrls.splice(index, 1);
    this.safeImageUrls.splice(index, 1);
    this.updateModel();
  }

  imageDragStart(index: number) {
    setTimeout(() => {
      this.dragIndex = index;
      this.cd.markForCheck();
    });
  }

  imageDragEnd() {
    this.dragIndex = -1;
    this.cd.markForCheck();
  }

  imageDrop(event: DndDropEvent) {
    let index = event.index;
    if (isUndefined(index)) {
      index = this.safeImageUrls.length;
    }
    moveItemInArray(this.imageUrls, this.dragIndex, index);
    moveItemInArray(this.safeImageUrls, this.dragIndex, index);
    this.dragIndex = -1;
    this.updateModel();
  }
}
