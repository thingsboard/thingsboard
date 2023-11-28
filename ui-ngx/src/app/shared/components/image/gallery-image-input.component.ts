///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  extractParamsFromImageResourceUrl,
  ImageResourceInfo,
  isBase64DataImageUrl,
  isImageResourceUrl
} from '@shared/models/resource.models';
import { ImageService } from '@core/http/image.service';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ImageGalleryComponent } from '@shared/components/image/image-gallery.component';

export enum ImageLinkType {
  none = 'none',
  base64 = 'base64',
  external = 'external',
  resource = 'resource'
}

@Component({
  selector: 'tb-gallery-image-input',
  templateUrl: './gallery-image-input.component.html',
  styleUrls: ['./gallery-image-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GalleryImageInputComponent),
      multi: true
    }
  ]
})
export class GalleryImageInputComponent extends PageComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  disabled: boolean;

  imageUrl: string;

  imageResource: ImageResourceInfo;

  loadingImageResource = false;

  ImageLinkType = ImageLinkType;

  linkType: ImageLinkType = ImageLinkType.none;

  externalLinkControl = new FormControl(null);

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private imageService: ImageService,
              private cd: ChangeDetectorRef,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private popoverService: TbPopoverService) {
    super(store);
  }

  ngOnInit() {
    this.externalLinkControl.valueChanges.subscribe((value) => {
      if (this.linkType === ImageLinkType.external) {
        this.updateModel(value);
      }
    });
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
    if (this.disabled) {
      this.detectLinkType();
      this.externalLinkControl.disable({emitEvent: false});
    } else {
      this.externalLinkControl.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    if (this.imageUrl !== value) {
      this.reset();
      this.imageUrl = value;
      this.detectLinkType();
      if (this.linkType === ImageLinkType.resource) {
        const params = extractParamsFromImageResourceUrl(value);
        this.loadingImageResource = true;
        this.imageService.getImageInfo(params.type, params.key, {ignoreLoading: true, ignoreErrors: true}).subscribe(
          {
            next: (res) => {
              this.imageResource = res;
              this.loadingImageResource = false;
              this.cd.markForCheck();
            },
            error: () => {
              this.reset();
              this.loadingImageResource = false;
              this.cd.markForCheck();
            }
          }
        );
      } else if (this.linkType === ImageLinkType.base64) {
        this.cd.markForCheck();
      } else if (this.linkType === ImageLinkType.external) {
        this.externalLinkControl.setValue(this.imageUrl, {emitEvent: false});
        this.cd.markForCheck();
      }
    }
  }

  private detectLinkType() {
    if (this.imageUrl) {
      if (isImageResourceUrl(this.imageUrl)) {
        this.linkType = ImageLinkType.resource;
      } else if (isBase64DataImageUrl(this.imageUrl)) {
        this.linkType = ImageLinkType.base64;
      } else {
        this.linkType = ImageLinkType.external;
      }
    } else {
      this.linkType = ImageLinkType.none;
    }
  }

  private updateModel(value: string) {
    this.cd.markForCheck();
    if (this.imageUrl !== value) {
      this.imageUrl = value;
      this.propagateChange(this.imageUrl);
    }
  }

  private reset() {
    this.linkType = ImageLinkType.none;
    this.imageResource = null;
    this.externalLinkControl.setValue(null, {emitEvent: false});
  }

  clearImage() {
    this.reset();
    this.updateModel(null);
  }

  setLink($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.linkType = ImageLinkType.external;
  }

  toggleGallery($event: Event, browseGalleryButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = browseGalleryButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        pageMode: false,
        popoverMode: true,
        mode: 'grid',
        selectionMode: true
      };
      const imageGalleryPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ImageGalleryComponent, 'top', true, null,
        ctx,
        {},
        {}, {}, true);
      imageGalleryPopover.tbComponentRef.instance.imageSelected.subscribe((image) => {
        imageGalleryPopover.hide();
        this.linkType = ImageLinkType.resource;
        this.imageResource = image;
        this.updateModel(image.link);
      });
    }
  }

}
