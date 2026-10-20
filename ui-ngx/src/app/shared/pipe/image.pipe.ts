// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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

export type CustomImageUrlCallback = (url: string) => Observable<SafeUrl | string> | null;

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
      const loginLogo = !!args?.loginLogo;
      const loginFavicon = !!args?.loginFavicon;
      let imageObservable: Observable<SafeUrl | string>;
      if (loginLogo || loginFavicon) {
        const faviconElseLogo = loginFavicon;
        imageObservable = this.imageService.resolveLoginImageUrl(url, faviconElseLogo, asString, emptyUrl);
      } else {
        if (!!args?.customImageUrlCallback) {
          const callback: CustomImageUrlCallback = args.customImageUrlCallback;
          imageObservable = callback(url);
        }
        if (!imageObservable) {
          imageObservable = this.imageService.resolveImageUrl(url, preview, asString, emptyUrl);
        }
      }
      imageObservable.subscribe((imageUrl) => {
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
