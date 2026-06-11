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

import {
  Directive,
  ElementRef,
  OnDestroy,
  inject, AfterViewInit, Renderer2,
} from '@angular/core';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { Subscription } from 'rxjs';
import { onParentScrollOrWindowResize } from '@core/utils';

@Directive({
  selector: 'input[matAutocomplete], textarea[matAutocomplete]',
  standalone: false
})
export class AutocompleteAutoScrollRepositionDirective implements AfterViewInit, OnDestroy {
  private readonly trigger = inject(MatAutocompleteTrigger, { host: true });
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly renderer = inject(Renderer2);

  private parentScrollSubscription: Subscription = null;
  private isIntersecting: boolean = false;

  private intersectionObserver = new IntersectionObserver((entries) => {
    if (this.isIntersecting !== entries[0].isIntersecting) {
      this.isIntersecting = entries[0].isIntersecting;
      this.updatePanelVisibility();
    }
  }, {threshold: [0.5]});

  constructor() {
  }

  ngAfterViewInit(): void {
    this.parentScrollSubscription = onParentScrollOrWindowResize(this.elementRef.nativeElement).subscribe(() => {
      if (this.trigger.panelOpen) {
        this.trigger.updatePosition();
      }
    });
    this.intersectionObserver.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    if (this.parentScrollSubscription) {
      this.parentScrollSubscription.unsubscribe();
      this.parentScrollSubscription = null;
    }
    if (this.intersectionObserver) {
      this.intersectionObserver.unobserve(this.elementRef.nativeElement);
      this.intersectionObserver.disconnect();
      this.intersectionObserver = null;
    }
  }

  private updatePanelVisibility(): void {
    if (this.trigger.panelOpen) {
      if (this.isIntersecting) {
        this.renderer.removeStyle(this.trigger.autocomplete.panel.nativeElement, 'display');
      } else {
        this.renderer.setStyle(this.trigger.autocomplete.panel.nativeElement, 'display', 'none');
      }
    }
  }

}
