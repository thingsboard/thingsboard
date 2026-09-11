// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
