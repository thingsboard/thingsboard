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

import { Pipe, PipeTransform } from '@angular/core';
import { ImageService } from '@core/http/image.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { isDefinedAndNotNull } from '@core/utils';
import { NO_IMAGE_DATA_URI } from '@shared/models/resource.models';

const LOADING_IMAGE_DATA_URI = 'data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRG' +
                                      'LTgiPz4KPHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHZlcnNpb249IjEuMSIgdmlld0JveD0iMCAw' +
                                      'IDIwIDIwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjwvc3ZnPgo=';

@Pipe({
  name: 'image'
})
export class ImagePipe implements PipeTransform {

  constructor(private imageService: ImageService,
              private sanitizer: DomSanitizer) { }

  transform(url: string, args?: any): Observable<SafeUrl | string> {
    const ignoreLoadingImage = !!args?.ignoreLoadingImage;
    const asString = !!args?.asString;
    const image$ = ignoreLoadingImage ? new Subject<SafeUrl | string>() : new BehaviorSubject<SafeUrl | string>(LOADING_IMAGE_DATA_URI);
    if (isDefinedAndNotNull(url)) {
      const preview = !!args?.preview;
      this.imageService.resolveImageUrl(url, preview, asString).subscribe((imageUrl) => {
        image$.next(imageUrl);
        image$.complete();
      });
    } else {
      const emptyUrl = args?.emptyUrl || NO_IMAGE_DATA_URI;
      image$.next(asString ? emptyUrl : this.sanitizer.bypassSecurityTrustUrl(emptyUrl));
      image$.complete();
    }
    return image$.asObservable();
  }

}
