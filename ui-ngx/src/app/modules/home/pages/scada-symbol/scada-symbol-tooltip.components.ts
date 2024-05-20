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
  OnInit,
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
import { MatButton } from '@angular/material/button';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;

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
    <button mat-stroked-button color="primary" (click)="onAddTag()">
      <mat-icon>add</mat-icon>
      <span>Add tag</span>
    </button>
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
      <mat-icon>done</mat-icon>
    </button>
    <button type="button" mat-icon-button class="tb-mat-20"
            matTooltip="Cancel"
            matTooltipPosition="above"
            (click)="onCancel()">
      <mat-icon>close</mat-icon>
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
      <mat-icon>edit</mat-icon>
    </button>
    <button #removeTagButton type="button"
            mat-icon-button class="tb-mat-20"
            matTooltip="Remove tag"
            matTooltipPosition="above">
      <mat-icon>delete</mat-icon>
    </button>
  </div>`,
  styleUrls: ['./scada-symbol-tooltip.component.scss'],
  encapsulation: ViewEncapsulation.None
})
class ScadaSymbolTagPanelComponent extends ScadaSymbolPanelComponent implements AfterViewInit {

  @ViewChild('removeTagButton', {read: ElementRef})
  removeTagButton: ElementRef<HTMLElement>;

  @Output()
  updateTag = new EventEmitter();

  @Output()
  removeTag = new EventEmitter();

  constructor(public element: ElementRef<HTMLElement>,
              private container: ViewContainerRef) {
    super(element);
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
    setTimeout(() => {
      const el = $(this.removeTagButton.nativeElement);
      el.tooltipster(
        {
          zIndex: 200,
          arrow: true,
          theme: ['iot-svg', 'tb-active'],
          interactive: true,
          trigger: 'click',
          side: 'top',
          content: ''
        }
      );
      const tooltip = el.tooltipster('instance');
      const compRef =
        setTooltipComponent(this.symbolElement, this.container, ScadaSymbolRemoveTagConfirmComponent, tooltip);
      compRef.instance.removeTag.subscribe(() => {
        tooltip.destroy();
        this.removeTag.emit();
      });
      compRef.instance.cancel.subscribe(() => {
        tooltip.close();
      });
      tooltip.on('ready', () => {
        compRef.instance.yesButton.focus();
      });
    });
  }

  public onUpdateTag() {
    this.updateTag.emit();
  }
}

@Component({
  template: `<div class="tooltipster-content tb-scada-symbol-tooltip-panel column">
    <div class="tb-confirm-text" [innerHTML]="deleteText"></div>
    <div class="tb-scada-symbol-tooltip-panel">
      <button mat-stroked-button color="primary" (click)="onCancel()">
        <mat-icon>close</mat-icon>
        <span>No</span>
      </button>
      <button #yesButton
              mat-stroked-button color="primary" (click)="onRemoveTag()">
        <mat-icon>done</mat-icon>
        <span>Yes</span>
      </button>
    </div>
  </div>`,
  styleUrls: ['./scada-symbol-tooltip.component.scss'],
  encapsulation: ViewEncapsulation.None
})
class ScadaSymbolRemoveTagConfirmComponent extends ScadaSymbolPanelComponent implements OnInit, AfterViewInit {

  @ViewChild('yesButton')
  yesButton: MatButton;

  deleteText: string;

  @Output()
  cancel = new EventEmitter();

  @Output()
  removeTag = new EventEmitter();

  constructor(public element: ElementRef<HTMLElement>) {
    super(element);
  }

  ngOnInit() {
    this.deleteText = `Are you sure you want to delete tag<br/><b>${this.symbolElement?.tag}</b>
                          from <b>${this.symbolElement?.element?.type}</b> element?`;
  }

  public onCancel() {
    this.cancel.emit();
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
      ScadaSymbolTagPanelComponent,
      ScadaSymbolRemoveTagConfirmComponent
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
                                                                  container: ViewContainerRef,
                                                                  componentType: Type<T>,
                                                                  tooltip?: ITooltipsterInstance): ComponentRef<T> => {
  if (!tooltip) {
    tooltip = symbolElement.tooltip;
  }
  const componentRef = container.createComponent(componentType);
  componentRef.instance.symbolElement = symbolElement;
  componentRef.instance.viewInited.subscribe(() => {
    if (tooltip.status().open) {
      tooltip.reposition();
    }
  });
  tooltip.on('destroyed', () => {
    componentRef.destroy();
  });
  const parentElement = componentRef.instance.element.nativeElement;
  const content = parentElement.firstChild;
  parentElement.removeChild(content);
  parentElement.style.display = 'none';
  tooltip.content(content);
  return componentRef;
};
