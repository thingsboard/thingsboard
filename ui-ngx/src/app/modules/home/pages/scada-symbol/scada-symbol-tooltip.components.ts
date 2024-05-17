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

import {
  AfterViewInit,
  Component,
  ComponentRef,
  Directive,
  ElementRef,
  EventEmitter,
  Input,
  NgModule, OnDestroy,
  Output,
  Type,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ScadaSymbolElement } from '@home/pages/scada-symbol/scada-symbol.models';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { ENTER } from '@angular/cdk/keycodes';
import Timeout = NodeJS.Timeout;

@Directive()
abstract class ScadaSymbolPanelComponent implements AfterViewInit {

  @Input()
  symbolElement: ScadaSymbolElement;

  @Output()
  viewInited = new EventEmitter();

  protected constructor(public element: ElementRef<HTMLElement>) {
  }

  ngAfterViewInit() {
    this.viewInited.emit();
  }
}

@Component({
  template: `<div class="tb-scada-symbol-tooltip-panel">
    <span>{{ symbolElement?.element?.type }}:</span>
    <button mat-stroked-button color="primary" (click)="onAddTag()">Add tag</button>
  </div>`,
  styleUrls: ['./scada-symbol-tooltip.component.scss'],
  encapsulation: ViewEncapsulation.None
})
class ScadaSymbolAddTagPanelComponent extends ScadaSymbolPanelComponent {

  @Output()
  addTag = new EventEmitter();

  constructor(public element: ElementRef<HTMLElement>) {
    super(element);
  }

  public onAddTag() {
    this.addTag.emit();
  }

}

@Component({
  template: `<div class="tb-scada-symbol-tooltip-panel">
    <span>{{ isAdd ? 'Enter tag:' : 'Update tag:' }}</span>
    <mat-form-field class="tb-inline-field" appearance="outline" subscriptSizing="dynamic">
      <input #tagField matInput [(ngModel)]="tag" (keydown)="tagEnter($event)" (blur)="onBlur()"
             placeholder="{{ 'widget-config.set' | translate }}">
    </mat-form-field>
    <button type="button" mat-icon-button class="tb-mat-20"
            matTooltip="Apply"
            matTooltipPosition="above"
            [disabled]="!tag"
            (click)="onApply()">
      <mat-icon class="material-icons">done</mat-icon>
    </button>
    <button type="button" mat-icon-button class="tb-mat-20"
            matTooltip="Cancel"
            matTooltipPosition="above"
            (click)="onCancel()">
      <mat-icon class="material-icons">close</mat-icon>
    </button>
  </div>`,
  styleUrls: ['./scada-symbol-tooltip.component.scss'],
  encapsulation: ViewEncapsulation.None
})
class ScadaSymbolTagInputPanelComponent extends ScadaSymbolPanelComponent implements AfterViewInit, OnDestroy {

  @ViewChild('tagField')
  tagField: ElementRef<HTMLInputElement>;

  @Input()
  isAdd: boolean;

  @Input()
  tag: string;

  @Output()
  apply = new EventEmitter<string>();

  @Output()
  cancel = new EventEmitter();

  private closed = false;

  private blurTimeout: Timeout;

  constructor(public element: ElementRef<HTMLElement>) {
    super(element);
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
    setTimeout(() => {
      this.tagField.nativeElement.focus();
    });
  }

  ngOnDestroy() {
    if (this.blurTimeout) {
      clearTimeout(this.blurTimeout);
      this.blurTimeout = null;
    }
  }

  public tagEnter($event: KeyboardEvent) {
    if ($event.keyCode === ENTER) {
      $event.preventDefault();
      if (this.tag) {
        this.closed = true;
        this.apply.emit(this.tag);
      }
    }
  }

  public onApply() {
    this.closed = true;
    if (this.tag) {
      this.apply.emit(this.tag);
    } else {
      this.cancel.emit();
    }
  }

  public onCancel() {
    this.closed = true;
    this.cancel.emit();
  }

  public onBlur() {
    this.blurTimeout = setTimeout(() => {
      if (!this.closed) {
        this.closed = true;
        this.cancel.emit();
      }
    }, 300);
  }

}

@Component({
  template: `<div class="tb-scada-symbol-tooltip-panel">
    <span>{{ symbolElement?.element?.type }}:</span>
    <span><b>{{ symbolElement?.tag }}</b></span>
    <button type="button" mat-icon-button class="tb-mat-20"
            matTooltip="Update tag"
            matTooltipPosition="above"
            (click)="onUpdateTag()">
      <mat-icon class="material-icons">edit</mat-icon>
    </button>
    <button type="button" mat-icon-button class="tb-mat-20"
            matTooltip="Remove tag"
            matTooltipPosition="above"
            (click)="onRemoveTag()">
      <mat-icon class="material-icons">delete</mat-icon>
    </button>
  </div>`,
  styleUrls: ['./scada-symbol-tooltip.component.scss'],
  encapsulation: ViewEncapsulation.None
})
class ScadaSymbolTagPanelComponent extends ScadaSymbolPanelComponent implements AfterViewInit {

  @Output()
  updateTag = new EventEmitter();

  @Output()
  removeTag = new EventEmitter();

  constructor(public element: ElementRef<HTMLElement>) {
    super(element);
  }

  public onUpdateTag() {
    this.updateTag.emit();
  }

  public onRemoveTag() {
    this.removeTag.emit();
  }
}

@NgModule({
  declarations:
    [
      ScadaSymbolAddTagPanelComponent,
      ScadaSymbolTagInputPanelComponent,
      ScadaSymbolTagPanelComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ]
})
export class ScadaSymbolTooltipComponentsModule { }

export const setupAddTagPanelTooltip = (symbolElement: ScadaSymbolElement, container: ViewContainerRef) => {
  symbolElement.stopEdit();
  symbolElement.tooltip.off('close');
  const componentRef = setTooltipComponent(symbolElement, container, ScadaSymbolAddTagPanelComponent);
  componentRef.instance.addTag.subscribe(() => {
    componentRef.destroy();
    setupTagInputPanelTooltip(symbolElement, container, true);
  });
};

export const setupTagPanelTooltip = (symbolElement: ScadaSymbolElement, container: ViewContainerRef) => {
  symbolElement.stopEdit();
  symbolElement.unhighlight();
  const componentRef = setTooltipComponent(symbolElement, container, ScadaSymbolTagPanelComponent);
  componentRef.instance.updateTag.subscribe(() => {
    componentRef.destroy();
    setupTagInputPanelTooltip(symbolElement, container, false);
  });
  componentRef.instance.removeTag.subscribe(() => {
    componentRef.destroy();
    symbolElement.clearTag();
  });
  symbolElement.tooltip.open();
};

const setupTagInputPanelTooltip = (symbolElement: ScadaSymbolElement, container: ViewContainerRef, isAdd: boolean) => {

  symbolElement.startEdit(() => {
    if (isAdd) {
      symbolElement.unhighlight();
      symbolElement.tooltip.close();
    } else {
      componentRef.destroy();
      setupTagPanelTooltip(symbolElement, container);
    }
  });

  const componentRef = setTooltipComponent(symbolElement, container, ScadaSymbolTagInputPanelComponent);

  componentRef.instance.isAdd = isAdd;
  if (!isAdd) {
    componentRef.instance.tag = symbolElement.tag;
  }
  componentRef.instance.apply.subscribe((newTag) => {
    componentRef.destroy();
    if (isAdd) {
      symbolElement.tooltip.off('close');
    }
    symbolElement.setTag(newTag);
  });
  componentRef.instance.cancel.subscribe(() => {
    symbolElement.stopEdit(true);
  });
  if (isAdd) {
    symbolElement.tooltip.option('delay', [0, 10000000]);
    symbolElement.tooltip.on('close', () => {
      componentRef.destroy();
      symbolElement.tooltip.option('delay', [0, 300]);
      setupAddTagPanelTooltip(symbolElement, container);
    });
  }
};

const setTooltipComponent = <T extends ScadaSymbolPanelComponent>(symbolElement: ScadaSymbolElement,
                                                                  container: ViewContainerRef, componentType: Type<T>): ComponentRef<T> => {
  const componentRef = container.createComponent(componentType);
  componentRef.instance.symbolElement = symbolElement;
  componentRef.instance.viewInited.subscribe(() => {
    if (symbolElement.tooltip.status().open) {
      symbolElement.tooltip.reposition();
    }
  });
  const parentElement = componentRef.instance.element.nativeElement;
  const content = parentElement.firstChild;
  parentElement.removeChild(content);
  parentElement.style.display = 'none';
  symbolElement.tooltip.content(content);
  return componentRef;
};
