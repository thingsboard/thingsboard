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

import { ChangeDetectorRef, Component, forwardRef, Input, OnDestroy } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { moveItemInArray } from '@angular/cdk/drag-drop';
import { DndDropEvent } from 'ngx-drag-drop';
import { isUndefined } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ImageLinkType } from '@shared/components/image/gallery-image-input.component';
import {
  ImageResourceInfo,
  prependTbImagePrefixToUrls,
  removeTbImagePrefixFromUrls,
  ResourceSubType
} from '@shared/models/resource.models';
import { MatDialog } from '@angular/material/dialog';
import {
  ImageGalleryDialogComponent,
  ImageGalleryDialogData
} from '@shared/components/image/image-gallery-dialog.component';

@Component({
  selector: 'tb-multiple-gallery-image-input',
  templateUrl: './multiple-gallery-image-input.component.html',
  styleUrls: ['./multiple-gallery-image-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MultipleGalleryImageInputComponent),
      multi: true
    }
  ]
})
export class MultipleGalleryImageInputComponent extends PageComponent implements OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  disabled: boolean;

  imageUrls: string[];

  ImageLinkType = ImageLinkType;

  linkType: ImageLinkType = ImageLinkType.none;

  externalLinkControl = new FormControl(null);

  dragIndex: number;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnDestroy() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string[]): void {
    this.reset();
    this.imageUrls = removeTbImagePrefixFromUrls(value);
  }

  private updateModel() {
    this.cd.markForCheck();
    this.propagateChange(prependTbImagePrefixToUrls(this.imageUrls));
  }

  private reset() {
    this.linkType = ImageLinkType.none;
    this.externalLinkControl.setValue(null, {emitEvent: false});
  }

  clearImage(index: number) {
    this.imageUrls.splice(index, 1);
    this.updateModel();
  }

  setLink($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.linkType = ImageLinkType.external;
  }

  declineLink($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.reset();
  }

  applyLink($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.imageUrls.push(this.externalLinkControl.value);
    this.reset();
    this.updateModel();
  }

  toggleGallery($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ImageGalleryDialogComponent, ImageGalleryDialogData,
      ImageResourceInfo>(ImageGalleryDialogComponent, {
      autoFocus: false,
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        imageSubType: ResourceSubType.IMAGE
      }
    }).afterClosed().subscribe((image) => {
      if (image) {
        this.imageUrls.push(image.link);
        this.updateModel();
      }
    });
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
      index = this.imageUrls.length;
    }
    moveItemInArray(this.imageUrls, this.dragIndex, index);
    this.dragIndex = -1;
    this.updateModel();
  }
}
