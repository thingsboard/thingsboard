///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ComponentRef, Directive, ElementRef, Input, ViewContainerRef } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: '[tb-circular-progress]'
})
export class CircularProgressDirective {

  showProgressValue = false;

  children: JQuery<any>;

  cssWidth: any;

  @Input('tb-circular-progress')
  set showProgress(showProgress: boolean) {
    if (this.showProgressValue !== showProgress) {
      const element = this.elementRef.nativeElement;
      this.showProgressValue = showProgress;
      this.spinnerRef.instance._elementRef.nativeElement.style.display = showProgress ? 'block' : 'none';
      if (showProgress) {
        this.cssWidth = $(element).prop('style').width;
        if (!this.cssWidth) {
          $(element).css('width', '');
          const width = $(element).prop('offsetWidth');
          $(element).css('width', width + 'px');
        }
        this.children = $(element).children();
        $(element).empty();
        $(element).append($(this.spinnerRef.instance._elementRef.nativeElement));
      } else {
        $(element).empty();
        $(element).append(this.children);
        if (this.cssWidth) {
          $(element).css('width', this.cssWidth);
        } else {
          $(element).css('width', '');
        }
      }
    }
  }

  spinnerRef: ComponentRef<MatProgressSpinner>;

  constructor(private elementRef: ElementRef,
              private viewContainerRef: ViewContainerRef) {
    this.createCircularProgress();
  }

  createCircularProgress() {
    this.elementRef.nativeElement.style.position = 'relative';
    this.spinnerRef = this.viewContainerRef.createComponent(MatProgressSpinner, {index: 0});
    this.spinnerRef.instance.mode = 'indeterminate';
    this.spinnerRef.instance.diameter = 20;
    const el = this.spinnerRef.instance._elementRef.nativeElement;
    el.style.margin = 'auto';
    el.style.position = 'absolute';
    el.style.left = '0';
    el.style.right = '0';
    el.style.top = '0';
    el.style.bottom = '0';
    el.style.display = 'none';
  }
}
