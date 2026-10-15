// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgZone, Pipe, PipeTransform } from '@angular/core';
import { ImageService } from '@core/http/image.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { AsyncSubject, BehaviorSubject, Observable } from 'rxjs';
import { isDefinedAndNotNull } from '@core/utils';
import { NO_IMAGE_DATA_URI } from '@shared/models/resource.models';

const LOADING_IMAGE_DATA_URI = 'data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRG' +
                                      'LTgiPz4KPHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHZlcnNpb249IjEuMSIgdmlld0JveD0iMCAw' +
                                      'IDIwIDIwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjwvc3ZnPgo=';

export interface UrlHolder {
  url?: string;
}

@Pipe({
    name: 'image',
    standalone: false
})
export class ImagePipe implements PipeTransform {

  constructor(private imageService: ImageService,
              private sanitizer: DomSanitizer,
              private zone: NgZone) { }

  transform(urlData: string | UrlHolder, args?: any, triggerUpdate?: number): Observable<SafeUrl | string> {
    const ignoreLoadingImage = !!args?.ignoreLoadingImage;
    const asString = !!args?.asString;
    const emptyUrl = args?.emptyUrl || NO_IMAGE_DATA_URI;
    const image$ = ignoreLoadingImage
      ? new AsyncSubject<SafeUrl | string>()
      : new BehaviorSubject<SafeUrl | string>(LOADING_IMAGE_DATA_URI);
    const url = (typeof urlData === 'string') ? urlData : urlData?.url;
    if (isDefinedAndNotNull(url)) {
      const preview = !!args?.preview;
      this.imageService.resolveImageUrl(url, preview, asString, emptyUrl).subscribe((imageUrl) => {
        Promise.resolve().then(() => {
          this.zone.run(() => {
            image$.next(imageUrl);
            image$.complete();
          });
        });
      });
    } else {
      Promise.resolve().then(() => {
        this.zone.run(() => {
          image$.next(asString ? emptyUrl : this.sanitizer.bypassSecurityTrustUrl(emptyUrl));
          image$.complete();
        });
      });
    }
    return image$.asObservable();
  }

}
