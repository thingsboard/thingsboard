///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { ImageResourceInfo } from '@shared/models/resource.models';
import * as svgjs from '@svgdotjs/svg.js';
import { Box, Element, Rect, Style, SVG, Svg, Timeline } from '@svgdotjs/svg.js';
import { ResizeObserver } from '@juggle/resize-observer';
import { EventEmitter, ViewContainerRef } from '@angular/core';
import { forkJoin, from } from 'rxjs';
import {
  setupAddTagPanelTooltip,
  setupTagPanelTooltip
} from '@home/pages/scada-symbol/scada-symbol-tooltip.components';
import { iotSvgContentData } from '@home/components/widget/lib/svg/iot-svg.models';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;
import TooltipPositioningSide = JQueryTooltipster.TooltipPositioningSide;
import ITooltipsterHelper = JQueryTooltipster.ITooltipsterHelper;
import ITooltipPosition = JQueryTooltipster.ITooltipPosition;

export interface ScadaSymbolData {
  imageResource: ImageResourceInfo;
  svgContent: string;
}

export class ScadaSymbolEditObject {

  public svgShape: Svg;
  private svgRootNodePart: string;
  private box: Box;
  private elements: ScadaSymbolElement[] = [];
  private readonly shapeResize$: ResizeObserver;
  private performSetup = false;
  private hoverFilterStyle: Style;
  public scale = 1;

  public dirty = false;

  public tags: string[] = [];

  public tagsUpdated = new EventEmitter<string[]>();

  constructor(private rootElement: HTMLElement,
              public viewContainerRef: ViewContainerRef) {
    this.shapeResize$ = new ResizeObserver(() => {
      this.resize();
    });
  }

  public setContent(svgContent: string) {
    this.shapeResize$.unobserve(this.rootElement);
    if (this.svgShape) {
      this.destroyElements();
      this.svgShape.remove();
    }
    this.scale = 1;
    this.dirty = false;
    const contentData = iotSvgContentData(svgContent);
    this.svgRootNodePart = contentData.svgRootNode;
    this.svgShape = SVG().addTo(this.rootElement).svg(contentData.innerSvg);
    this.svgShape.node.style.overflow = 'visible';
    this.svgShape.node.style['user-select'] = 'none';
    this.box = this.svgShape.bbox();
    this.svgShape.size(this.box.width, this.box.height);
    this.svgShape.viewbox(`0 0 ${this.box.width} ${this.box.height}`);
    this.svgShape.style().attr('tb:inner', true).rule('.tb-element', {cursor: 'pointer', transition: '0.2s filter ease-in-out'});
    this.updateHoverFilterStyle();
    this.performSetup = true;
    this.shapeResize$.observe(this.rootElement);
  }

  public getContent(): string {
    if (this.svgShape) {
      const svgContent = this.svgShape.svg((e: Element) => {
        if (e.node.hasAttribute('tb:inner')) {
          return false;
        } else {
          e.node.classList.remove('tb-element', 'tooltipstered');
          if (!e.node.classList.length) {
            e.node.removeAttribute('class');
          }
          e.attr('svgjs:data', null);
        }
      }, false);
      return `${this.svgRootNodePart}\n${svgContent}\n</svg>`;
    } else {
      return null;
    }
  }

  public cancelEdit() {
    this.elements.filter(e => e.isEditing()).forEach(e => e.stopEdit(true));
  }

  private doSetup() {
    this.setupZoomPan(0);
    (window as any).SVG = svgjs;
    forkJoin([
      from(import('tooltipster')),
      from(import('tooltipster/dist/js/plugins/tooltipster/SVG/tooltipster-SVG.min.js'))
    ]).subscribe(() => {
      this.setupElements();
    });
  }

  private setupZoomPan(margin: number) {
    this.svgShape.on('zoom', (e) => {
      const {
        detail: { level, focus }
      } = e as any;
      this.svgShape.zoom(level, focus);
      const box = this.restrictToMargins(this.svgShape.viewbox(), margin);
      this.svgShape.viewbox(box);
      setTimeout(() => {
        this.updateTooltipPositions();
      });
      e.preventDefault();
    });
    this.svgShape.on('panning', (e) => {
      const box = (e as any).detail.box;
      this.svgShape.viewbox(this.restrictToMargins(box, margin));
      setTimeout(() => {
        this.updateTooltipPositions();
      });
      e.preventDefault();
    });
    this.svgShape.on('panStart', (e) => {
      this.svgShape.node.style.cursor = 'grab';
    });
    this.svgShape.on('panEnd', (e) => {
      this.svgShape.node.style.cursor = 'default';
    });
  }

  private restrictToMargins(box: Box, margin: number): Box {
    if (box.x < -margin) {
      box.x = -margin;
    } else if ((box.x + box.width) > (this.box.width + margin)) {
      box.x = this.box.width + margin - box.width;
    }
    if (box.y < -margin) {
      box.y = -margin;
    } else if ((box.y + box.height) > (this.box.height + margin)) {
      box.y = this.box.height + margin - box.height;
    }
    return box;
  }

  private setupElements() {
    this.svgShape.children().forEach(child => {
      this.addElement(child);
    });
    const overlappingGroups: ScadaSymbolElement[][] = [];
    for (const el of this.elements) {
      for (const other of this.elements) {
        if (el !== other && el.overlappingCenters(other)) {
          let overlappingGroup: ScadaSymbolElement[];
          for (const list of overlappingGroups) {
            if (list.includes(other) || list.includes(el)) {
              overlappingGroup = list;
              break;
            }
          }
          if (!overlappingGroup) {
            overlappingGroup = [el, other];
            overlappingGroups.push(overlappingGroup);
          } else {
            if (!overlappingGroup.includes(el)) {
              overlappingGroup.push(el);
            } else if (!overlappingGroup.includes(other)){
              overlappingGroup.push(other);
            }
          }
        }
      }
    }
    for (const group of overlappingGroups) {
      const centers = group.map(e => e.box.cy);
      const center = centers.reduce((a, b) => a + b, 0) / centers.length;
      const textElement = group.find(e => e.isText());
      const slots = textElement ? group.length % 2 === 0 ? (group.length + 1) : group.length : group.length;
      if (textElement) {
        textElement.setInnerTooltipOffset(0, center);
        group.splice(group.indexOf(textElement), 1);
      }
      let offset = - (elementTooltipMinHeight * slots) / 2 + elementTooltipMinHeight / 2;
      for (const element of group) {
        if (textElement && offset === 0) {
          offset += elementTooltipMinHeight;
        }
        element.setInnerTooltipOffset(offset, center);
        offset += elementTooltipMinHeight;
      }
    }
    for (const el of this.elements) {
      el.init();
    }
    this.updateTags();
  }

  private addElement(e: Element) {
    if (hasBBox(e)) {
      const scadaSymbolElement = new ScadaSymbolElement(this, e);
      this.elements.push(scadaSymbolElement);
      e.children().forEach(child => {
        if (!(child.type === 'tspan' && e.type === 'text')) {
          this.addElement(child);
        }
      }, true);
    }
  }

  public destroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    this.destroyElements();
    this.tagsUpdated.complete();
  }

  private destroyElements() {
    this.elements.forEach(e => {
      e.destroy();
    });
    this.elements.length = 0;
  }

  private resize() {
    if (this.svgShape) {
      const targetWidth = this.rootElement.getBoundingClientRect().width;
      const targetHeight = this.rootElement.getBoundingClientRect().height;
      let scale: number;
      if (targetWidth < targetHeight) {
        scale = targetWidth / this.box.width;
      } else {
        scale = targetHeight / this.box.height;
      }
      if (this.scale !== scale) {
        this.scale = scale;
        this.svgShape.node.style.transform = `scale(${this.scale})`;
        this.updateHoverFilterStyle();
        this.updateZoomOptions();
        this.updateTooltipPositions();
      }
      if (this.performSetup) {
        this.performSetup = false;
        this.doSetup();
      }
    }
  }

  private updateHoverFilterStyle() {
    if (this.hoverFilterStyle) {
      this.hoverFilterStyle.remove();
    }
    const whiteBlur = (2.8 / this.scale).toFixed(2);
    const blackBlur = (1.2 / this.scale).toFixed(2);
    this.hoverFilterStyle =
      this.svgShape.style().attr('tb:inner', true).rule('.hovered',
        {
          filter:
            `drop-shadow(0px 0px ${whiteBlur}px white) drop-shadow(0px 0px ${whiteBlur}px white)
             drop-shadow(0px 0px ${whiteBlur}px white) drop-shadow(0px 0px ${whiteBlur}px white)
             drop-shadow(0px 0px ${blackBlur}px black)`
        }
      );
  }

  private updateZoomOptions() {
    this.svgShape.panZoom({
      zoomMin: 1,
      zoomMax: 4,
      zoomFactor: 2 / this.scale
    });
  }

  private updateTooltipPositions() {
    const container = this.rootElement.getBoundingClientRect();
    for (const e of this.elements) {
      e.updateTooltipPosition(container);
    }
  }

  public updateTags() {
    this.tags = this.elements
    .filter(e => e.hasTag())
    .map(e => e.tag)
    .filter((v, i, a) => a.indexOf(v) === i)
    .sort();
    this.tagsUpdated.emit(this.tags);
  }

}

const hasBBox = (e: Element): boolean => {
  try {
    if (e.bbox) {
      const box = e.bbox();
      return !!box.width || !!box.height;
    } else {
      return false;
    }
  } catch (_e) {
    return false;
  }
};

const isDomRectContained = (target: DOMRect, container: DOMRect, horizontalGap = 0, verticalGap = 0): boolean => (
  target.left >= container.left - horizontalGap &&
  target.left + target.width <= container.left + container.width + horizontalGap &&
  target.top >= container.top - verticalGap &&
  target.top + target.height <= container.top + container.height + verticalGap
);

const elementTooltipMinHeight = 36 + 8;
const elementTooltipMinWidth = 100;

const groupRectStroke = 2;
const groupRectPadding = 2;

export class ScadaSymbolElement {

  private highlightRect: Rect;
  private highlightRectTimeline: Timeline;

  public tooltip: ITooltipsterInstance;

  public tag: string;

  private editing = false;
  private onCancelEdit: () => void;

  private innerTooltipOffset = 0;

  public readonly box: Box;

  private highlighted = false;

  private tooltipMouseX: number;
  private tooltipMouseY: number;

  constructor(private editObject: ScadaSymbolEditObject,
              public element: Element) {
    this.tag = element.attr('tb:tag');
    this.box = element.rbox(this.editObject.svgShape);
  }

  public init() {
    if (this.isGroup()) {
      this.highlightRect =
        this.editObject.svgShape
        .rect(this.box.width + groupRectPadding * 2, this.box.height + groupRectPadding * 2)
        .x(this.box.x - groupRectPadding)
        .y(this.box.y - groupRectPadding)
        .attr({
          'tb:inner': true,
          fill: 'none',
          rx: this.unscaled(6),
          stroke: 'rgba(0, 0, 0, 0.38)',
          'stroke-dasharray': '1',
          'stroke-width': this.unscaled(groupRectStroke),
          opacity: 0});
      this.highlightRectTimeline = new Timeline();
      this.highlightRect.timeline(this.highlightRectTimeline);
      this.highlightRect.hide();
    } else {
      this.element.addClass('tb-element');
    }
    this.element.on('mouseenter', (event) => {
      this.highlight();
    });
    this.element.on('mouseleave', (event) => {
      this.unhighlight();
    });
    if (this.hasTag()) {
      this.createTagTooltip();
    } else {
      this.createAddTagTooltip();
    }
  }

  public overlappingCenters(otherElement: ScadaSymbolElement): boolean {
    if (this.isGroup() || otherElement.isGroup()) {
      return false;
    }
    return Math.abs(this.box.cx - otherElement.box.cx) * this.editObject.scale < elementTooltipMinWidth &&
      Math.abs(this.box.cy - otherElement.box.cy) * this.editObject.scale < elementTooltipMinHeight;
  }

  public highlight() {
    if (!this.highlighted) {
      this.highlighted = true;
      if (this.isGroup()) {
        this.highlightRectTimeline.finish();
        this.highlightRect
        .attr({
          rx: this.unscaled(6),
          'stroke-width': this.unscaled(groupRectStroke)
        });
        this.highlightRect.show();
        this.highlightRect.animate(300).attr({opacity: 1});
      } else {
        this.element.addClass('hovered');
      }
      if (this.hasTag()) {
        this.tooltip.reposition();
        $(this.tooltip.elementTooltip()).addClass('tb-active');
      }
    }
  }

  public unhighlight() {
    if (this.highlighted) {
      this.highlighted = false;
      if (this.isGroup()) {
        this.highlightRectTimeline.finish();
        this.highlightRect.animate(300).attr({opacity: 0}).after(() => {
          this.highlightRect.hide();
        });
      } else {
        this.element.removeClass('hovered');
      }
      if (this.hasTag() && !this.editing) {
        $(this.tooltip.elementTooltip()).removeClass('tb-active');
      }
    }
  }

  public clearTag() {
    this.tooltip.destroy();
    this.tag = null;
    this.element.attr('tb:tag', null);
    this.unhighlight();
    this.createAddTagTooltip();
    this.editObject.dirty = true;
    this.editObject.updateTags();
  }

  public setTag(tag: string) {
    this.tooltip.destroy();
    this.tag = tag;
    this.element.attr('tb:tag', tag);
    this.createTagTooltip();
    this.editObject.dirty = true;
    this.editObject.updateTags();
  }

  public startEdit(onCancelEdit: () => void) {
    if (!this.editing) {
      this.editObject.cancelEdit();
      this.editing = true;
      this.onCancelEdit = onCancelEdit;
    }
  }

  public stopEdit(cancel = false) {
    if (this.editing) {
      this.editing = false;
      if (cancel && this.onCancelEdit) {
        this.onCancelEdit();
      }
      this.onCancelEdit = null;
      if (this.hasTag() && !this.highlighted) {
        $(this.tooltip.elementTooltip()).removeClass('tb-active');
      }
    }
  }

  public isEditing() {
    return this.editing;
  }

  public updateTooltipPosition(container: DOMRect) {
    if (this.tooltip && !this.tooltip.status().destroyed) {
      this.tooltip.reposition();
      const tooltipElement = this.tooltip.elementTooltip();
      if (tooltipElement) {
        if (isDomRectContained(tooltipElement.getBoundingClientRect(), container,
          elementTooltipMinWidth, elementTooltipMinHeight)) {
          tooltipElement.style.visibility = null;
        } else {
          tooltipElement.style.visibility = 'hidden';
        }
      }
    }
  }

  public setInnerTooltipOffset(offset: number, center: number) {
    this.innerTooltipOffset = offset + (center - this.box.cy) * this.editObject.scale;
  }

  public destroy() {
    if (this.tooltip) {
      this.tooltip.destroy();
    }
  }

  private unscaled(size: number): number {
    return size / (this.editObject.scale * this.editObject.svgShape.zoom());
  }

  private scaled(size: number): number {
    return size * (this.editObject.scale * this.editObject.svgShape.zoom());
  }

  private createTagTooltip() {
    const el = $(this.element.node);
    el.tooltipster(
      {
        zIndex: 100,
        arrow: this.isGroup(),
        distance: this.isGroup() ? (this.scaled(groupRectPadding) + groupRectStroke) : 6,
        theme: ['iot-svg'],
        delay: 0,
        animationDuration: 0,
        interactive: true,
        trigger: 'custom',
        side: 'top',
        trackOrigin: false,
        content: '',
        functionPosition: (instance, helper, position) =>
          this.innerTagTooltipPosition(instance, helper, position),
        functionReady: (instance, helper) => {
          const tooltipEl = $(helper.tooltip);
          tooltipEl.on('mouseenter', () => {
            this.highlight();
          });
          tooltipEl.on('mouseleave', () => {
            this.unhighlight();
          });
        }
      }
    );
    this.tooltip = el.tooltipster('instance');
    this.setupTagPanel();
  }

  private setupTagPanel() {
    setupTagPanelTooltip(this, this.editObject.viewContainerRef);
  }

  private createAddTagTooltip() {
    const el = $(this.element.node);
    el.tooltipster(
      {
        zIndex: 100,
        arrow: true,
        theme: ['iot-svg', 'tb-active'],
        delay: [0, 300],
        interactive: true,
        trigger: 'hover',
        side: ['top', 'left', 'bottom', 'right'],
        trackOrigin: false,
        content: '',
        functionBefore: (instance, helper) => {
          const mouseEvent = (helper.event as MouseEvent);
          this.tooltipMouseX = mouseEvent.clientX;
          this.tooltipMouseY = mouseEvent.clientY;
          let side: TooltipPositioningSide;
          if (this.isGroup()) {
            side = 'top';
          } else {
            side = this.calculateTooltipSide(helper.origin.getBoundingClientRect(),
              mouseEvent.clientX, mouseEvent.clientY);
          }
          instance.option('side', side);
        },
        functionPosition: (instance, helper, position) =>
          this.innerAddTagTooltipPosition(instance, helper, position),
        functionReady: (instance, helper) => {
          const tooltipEl = $(helper.tooltip);
          tooltipEl.on('mouseenter', () => {
            this.highlight();
          });
          tooltipEl.on('mouseleave', () => {
            this.unhighlight();
          });
        }
      }
    );
    this.tooltip = el.tooltipster('instance');
    this.setupAddTagPanel();
  }

  private setupAddTagPanel() {
    setupAddTagPanelTooltip(this, this.editObject.viewContainerRef);
  }

  private innerTagTooltipPosition(instance: ITooltipsterInstance, helper: ITooltipsterHelper,
                                  position: ITooltipPosition): ITooltipPosition {
    const clientRect = helper.origin.getBoundingClientRect();
    if (!this.isGroup()) {
      position.coord.top = clientRect.top + (clientRect.height - position.size.height) / 2
        + this.innerTooltipOffset * this.editObject.svgShape.zoom();
      position.coord.left = clientRect.left + (clientRect.width - position.size.width) / 2;
    } else {
      position.distance = this.scaled(groupRectPadding) + groupRectStroke;
      position.coord.top = clientRect.top - position.size.height - (this.scaled(groupRectPadding) + groupRectStroke);
    }
    return position;
  }

  private innerAddTagTooltipPosition(instance: ITooltipsterInstance,
                                     helper: ITooltipsterHelper, position: ITooltipPosition): ITooltipPosition {
    const distance = 10;
    switch (position.side) {
      case 'right':
        position.coord.top = this.tooltipMouseY - position.size.height / 2;
        position.coord.left = this.tooltipMouseX + distance;
        position.target = this.tooltipMouseY;
        break;
      case 'top':
        position.coord.top = this.tooltipMouseY - position.size.height - distance;
        position.coord.left = this.tooltipMouseX - position.size.width / 2;
        position.target = this.tooltipMouseX;
        if (this.isGroup()) {
          position.coord.top -= elementTooltipMinHeight;
        }
        break;
      case 'left':
        position.coord.top = this.tooltipMouseY - position.size.height / 2;
        position.coord.left = this.tooltipMouseX - position.size.width - distance;
        position.target = this.tooltipMouseY;
        break;
      case 'bottom':
        position.coord.top = this.tooltipMouseY + distance;
        position.coord.left = this.tooltipMouseX - position.size.width / 2;
        position.target = this.tooltipMouseX;
        break;
    }
    return position;
  }

  private calculateTooltipSide(clientRect: DOMRect, mouseX: number, mouseY: number): TooltipPositioningSide {
    let side: TooltipPositioningSide;
    const cx = clientRect.left + clientRect.width / 2;
    const cy = clientRect.top + clientRect.height / 2;
    if (clientRect.width > clientRect.height) {
      if (Math.abs(cx - mouseX) > clientRect.width / 4) {
        if (mouseX < cx) {
          side = 'left';
        } else {
          side = 'right';
        }
      } else {
        if (mouseY < cy) {
          side = 'top';
        } else {
          side = 'bottom';
        }
      }
    } else {
      if (Math.abs(cy - mouseY) > clientRect.height / 4) {
        if (mouseY < cy) {
          side = 'top';
        } else {
          side = 'bottom';
        }
      } else {
        if (mouseX < cx) {
          side = 'left';
        } else {
          side = 'right';
        }
      }
    }
    return side;
  }

  public hasTag() {
    return !!this.tag;
  }

  public isGroup() {
    return this.element.type === 'g';
  }

  public isText() {
    return this.element.type === 'text';
  }

}
