// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Pipe, PipeTransform, SecurityContext } from '@angular/core';
import { DomSanitizer, SafeHtml, SafeUrl } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { UrlHolder } from '@shared/pipe/image.pipe';

@Pipe({
  name: 'safeSvg',
  standalone: false
})
export class SafeSvgPipe implements PipeTransform {

  constructor(private http: HttpClient,
              private sanitizer: DomSanitizer) {
  }

  transform(urlData: string | UrlHolder | SafeUrl): Observable<SafeHtml> {
    const url = (typeof urlData === 'string') ? urlData : ('url' in urlData ? urlData.url : this.sanitizer.sanitize(SecurityContext.URL, urlData));
    if (url) {
      return this.http.get(url, {responseType: 'text'}).pipe(
        map((data) => this.sanitizer.bypassSecurityTrustHtml(data))
      );
    } else {
      return of(this.sanitizer.bypassSecurityTrustHtml(''));
    }
  }
}
