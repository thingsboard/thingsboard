// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { forkJoin, Observable, of } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { map, tap } from 'rxjs/operators';

export type GetImageSrcCallback = (image: HTMLImageElement) => string;

export type SetImageSrcCallback = (image: HTMLImageElement,
                                   origUrl: string, newUrl: string) => void;

@Pipe({
    name: 'htmlWithImage',
    standalone: false
})
export class HtmlWithImagePipe implements PipeTransform {

  private domParser: DOMParser;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer) {
    this.domParser = new DOMParser();
  }

  transform(html: string | HTMLElement, args?: any): Observable<SafeHtml | string> {
    const imageTasks: Observable<any>[] = [];
    const getImageSrcCallback: GetImageSrcCallback = args?.getImageSrcCallback || ((image) => image.getAttribute('src'));
    const setImageSrcCallback: SetImageSrcCallback = args?.setImageSrcCallback || null;
    let images: HTMLCollectionOf<HTMLImageElement>;
    let document: Document = null;
    if (typeof html === 'string') {
      document = this.domParser.parseFromString(html, "text/html");
      images = document.images;
    } else {
      images = html.getElementsByTagName("img");
    }
    for (let i= 0; i < images.length; i++) {
      const image = images.item(i);
      const origImageUrl = getImageSrcCallback(image);
      imageTasks.push(this.imagePipe.transform(origImageUrl,
        {asString: true, ignoreLoadingImage: true, ...(args || {}) }).pipe(
        tap((newUrl) => {
          image.setAttribute('src', newUrl as string);
          if (setImageSrcCallback) {
            setImageSrcCallback(image, origImageUrl, newUrl);
          }
        })
      ));
    }
    let imagesConvert: Observable<any>;
    if (imageTasks.length) {
      imagesConvert = forkJoin(imageTasks);
    } else {
      imagesConvert = of(null);
    }
    return imagesConvert.pipe(
      map(() => {
        if (document) {
          const result = document.body.innerHTML;
          return this.sanitizer.bypassSecurityTrustHtml(result);
        }
        return null;
      })
    )
  }
}
