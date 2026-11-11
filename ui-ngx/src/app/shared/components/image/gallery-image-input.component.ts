// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  Output
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
  isImageResourceUrl,
  prependTbImagePrefix,
  removeTbImagePrefix,
  ResourceSubType
} from '@shared/models/resource.models';
import { ImageService } from '@core/http/image.service';
import { MatDialog } from '@angular/material/dialog';
import {
  ImageGalleryDialogComponent,
  ImageGalleryDialogData
} from '@shared/components/image/image-gallery-dialog.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { loadImageSize } from '@shared/models/widget/maps/map.models';

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
    ],
    standalone: false
})
export class GalleryImageInputComponent extends PageComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  @Input()
  hint: string;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  @coerceBoolean()
  detectImageSize = false;

  @Input()
  @coerceBoolean()
  noPadding = false;

  @Input()
  disabled: boolean;

  @Output()
  imageSize = new EventEmitter<{width: number; height: number;}>();

  imageUrl: string;

  imageResource: ImageResourceInfo;

  loadingImageResource = false;

  ImageLinkType = ImageLinkType;

  linkType: ImageLinkType = ImageLinkType.none;

  externalLinkControl = new FormControl(null);

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private imageService: ImageService,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit() {
    this.externalLinkControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
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
    value = removeTbImagePrefix(value);
    if (this.imageUrl !== value) {
      this.reset();
      this.imageUrl = value;
      this.detectLinkType();
      if (this.linkType === ImageLinkType.resource) {
        const params = extractParamsFromImageResourceUrl(this.imageUrl);
        if (params) {
          this.loadingImageResource = true;
          this.imageService.getImageInfo(params.type, params.key, {ignoreLoading: true, ignoreErrors: true}).subscribe(
            {
              next: (res) => {
                this.imageResource = res;
                this.loadingImageResource = false;
                this.imageLoaded();
                this.cd.markForCheck();
              },
              error: () => {
                this.reset();
                this.loadingImageResource = false;
                this.cd.markForCheck();
              }
            }
          );
        } else {
          this.reset();
          this.cd.markForCheck();
        }
      } else if (this.linkType === ImageLinkType.base64) {
        this.imageLoaded();
        this.cd.markForCheck();
      } else if (this.linkType === ImageLinkType.external) {
        this.externalLinkControl.setValue(this.imageUrl, {emitEvent: false});
        this.imageLoaded();
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

  private updateModel(value: string, forcedToUpdate = false): void {
    this.cd.markForCheck();
    if (this.imageUrl !== value || forcedToUpdate) {
      this.imageUrl = value;
      this.propagateChange(prependTbImagePrefix(this.imageUrl));
      this.imageLoaded();
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

  openGallery($event: Event): void {
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
        const forcedToUpdate =  this.imageUrl === image.link && this.imageResource?.descriptor?.etag !== image.descriptor.etag;
        this.linkType = ImageLinkType.resource;
        this.imageResource = image;
        this.updateModel(image.link, forcedToUpdate);
      }
    });
  }

  private imageLoaded() {
    if (this.detectImageSize && this.imageUrl) {
      if (this.linkType === ImageLinkType.resource) {
        const width = this.imageResource?.descriptor?.width;
        const height = this.imageResource?.descriptor?.height;
        this.imageSize.emit({width, height});
      } else {
        loadImageSize(this.imageUrl).subscribe({
          next: size => {
            this.imageSize.emit({width: size[0], height: size[1]});
          },
          error: _e => {}
        });
      }
    }
  }

}
