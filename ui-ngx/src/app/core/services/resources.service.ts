///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ReplaySubject, Observable, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ResourcesService {

  private loadedResources: { [url: string]: ReplaySubject<any> } = {};

  private anchor = this.document.getElementsByTagName('head')[0] || this.document.getElementsByTagName('body')[0];

  constructor(@Inject(DOCUMENT) private readonly document: any) {}

  public loadResource(url: string): Observable<any> {
    if (this.loadedResources[url]) {
      return this.loadedResources[url].asObservable();
    }

    let fileType;
    const match = /[.](css|less|html|htm|js)?((\?|#).*)?$/.exec(url);
    if (match !== null) {
      fileType = match[1];
    }
    if (!fileType) {
      return throwError(new Error(`Unable to detect file type from url: ${url}`));
    } else if (fileType !== 'css' && fileType !== 'js') {
      return throwError(new Error(`Unsupported file type: ${fileType}`));
    }
    return this.loadResourceByType(fileType, url);
  }

  private loadResourceByType(type: 'css' | 'js', url: string): Observable<any> {
    const subject = new ReplaySubject();
    this.loadedResources[url] = subject;
    let el;
    let loaded = false;
    switch (type) {
      case 'js':
        el = this.document.createElement('script');
        el.type = 'text/javascript';
        el.async = true;
        el.src = url;
        break;
      case 'css':
        el = this.document.createElement('link');
        el.type = 'text/css';
        el.rel = 'stylesheet';
        el.href = url;
        break;
    }
    el.onload = el.onreadystatechange = (e) => {
      if (el.readyState && !/^c|loade/.test(el.readyState) || loaded) { return; }
      el.onload = el.onreadystatechange = null;
      loaded = true;
      this.loadedResources[url].next();
      this.loadedResources[url].complete();
    };
    el.onerror = () => {
      this.loadedResources[url].error(new Error(`Unable to load ${url}`));
      delete this.loadedResources[url];
    };
    this.anchor.appendChild(el);
    return subject.asObservable();
  }
}
