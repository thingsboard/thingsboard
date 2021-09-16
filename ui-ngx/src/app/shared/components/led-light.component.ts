///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { RaphaelElement, RaphaelPaper, RaphaelSet } from 'raphael';
import * as tinycolor_ from 'tinycolor2';

const tinycolor = tinycolor_;

interface CircleElement extends RaphaelElement {
  theGlow?: RaphaelSet;
}

@Component({
  selector: 'tb-led-light',
  templateUrl: './led-light.component.html',
  styleUrls: []
})
export class LedLightComponent implements OnInit, AfterViewInit, OnChanges {

  @Input() size: number;

  @Input() colorOn: string;

  @Input() colorOff: string;

  @Input() offOpacity: number;

  private enabledValue: boolean;
  get enabled(): boolean {
    return this.enabledValue;
  }
  @Input()
  set enabled(value: boolean) {
    this.enabledValue = coerceBooleanProperty(value);
  }

  private canvasSize: number;
  private radius: number;
  private glowSize: number;
  private glowColor: string;

  private paper: RaphaelPaper;
  private circleElement: CircleElement;

  constructor(private elementRef: ElementRef<HTMLElement>) {
  }

  ngOnInit(): void {
    this.offOpacity = this.offOpacity || 0.4;
    this.glowColor = tinycolor(this.colorOn).lighten().toHexString();
  }

  ngAfterViewInit(): void {
    this.update();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'enabled' && this.circleElement) {
          this.draw();
        } else if (propName === 'size') {
          this.update();
        }
      }
    }
  }

  private update() {
    this.size = this.size || 50;
    this.canvasSize = this.size;
    this.radius = this.canvasSize / 4;
    this.glowSize = this.radius / 5;
    if (this.paper) {
      this.paper.remove();
    }
    import('raphael').then(
      (raphael) => {
        this.paper = raphael.default($('#canvas_container', this.elementRef.nativeElement)[0], this.canvasSize, this.canvasSize);
        const center = this.canvasSize / 2;
        this.circleElement = this.paper.circle(center, center, this.radius);
        this.draw();
      }
    );
  }

  private draw() {
    if (this.enabled) {
      this.circleElement.attr('fill', this.colorOn);
      this.circleElement.attr('stroke', this.colorOn);
      this.circleElement.attr('opacity', 1);
      if (this.circleElement.theGlow) {
        this.circleElement.theGlow.remove();
      }
      this.circleElement.theGlow = this.circleElement.glow(
        {
          color: this.glowColor,
          width: this.radius + this.glowSize,
          opacity: 0.8,
          fill: true
        });
    } else {
      if (this.circleElement.theGlow) {
        this.circleElement.theGlow.remove();
      }
      this.circleElement.attr('fill', this.colorOff);
      this.circleElement.attr('stroke', this.colorOff);
      this.circleElement.attr('opacity', this.offOpacity);
    }
  }

}
