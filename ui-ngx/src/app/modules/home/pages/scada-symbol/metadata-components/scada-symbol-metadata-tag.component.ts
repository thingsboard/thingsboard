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
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  Output, Renderer2,
  SimpleChanges,
  ViewChild, ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import { IotSvgTag } from '@home/components/widget/lib/svg/iot-svg.models';
import { MatExpansionPanel } from '@angular/material/expansion';
import { JsFuncComponent } from '@shared/components/js-func.component';
import { MatSelect } from '@angular/material/select';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import {
  scadaSymbolClickActionHighlightRules,
  scadaSymbolClickActionPropertiesHighlightRules,
  scadaSymbolElementStateRenderHighlightRules,
  scadaSymbolElementStateRenderPropertiesHighlightRules
} from '@home/pages/scada-symbol/scada-symbol.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { deepClone } from '@core/utils';
import {
  ScadaSymbolBehaviorPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behavior-panel.component';
import {
  ScadaSymbolMetadataTagFunctionPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag-function-panel.component';

@Component({
  selector: 'tb-scada-symbol-metadata-tag',
  templateUrl: './scada-symbol-metadata-tag.component.html',
  styleUrls: ['./scada-symbol-metadata-tag.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolMetadataTagComponent implements ControlValueAccessor, OnInit, Validator {

  @ViewChild('editStateRenderFunctionButton')
  editStateRenderFunctionButton: MatButton;

  @ViewChild('editClickActionButton')
  editClickActionButton: MatButton;

  @Input()
  disabled: boolean;

  @Input()
  elementStateRenderFunctionCompleter: TbEditorCompleter;

  @Input()
  clickActionFunctionCompleter: TbEditorCompleter;

  tagFormGroup: UntypedFormGroup;

  modelValue: IotSvgTag;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit() {
    this.tagFormGroup = this.fb.group({
      tag: [null, []],
      stateRenderFunction: [null, []],
      clickAction: [null, []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tagFormGroup.disable({emitEvent: false});
    } else {
      this.tagFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: IotSvgTag): void {
    this.modelValue = value;
    const clickAction = value?.actions && value?.actions.click ? value.actions.click.actionFunction : null;
    this.tagFormGroup.patchValue(
      {
        tag: value?.tag,
        stateRenderFunction: value?.stateRenderFunction,
        clickAction
      }, {emitEvent: false}
    );
  }

  public validate(_c: UntypedFormControl) {
    const valid = this.tagFormGroup.valid;
    return valid ? null : {
      tag: {
        valid: false,
      },
    };
  }

  editTagStateRenderFunction(): void {
    this.openTagFunction('renderFunction', this.editStateRenderFunctionButton);
    /*this.openPanelWithCallback(this.expansionPanel, () => {
      this.openPanelWithCallback(this.renderFunctionExpansionPanel, () => {
        this.stateRenderFunction.focus();
      });
    });*/
  }

  editClickAction(): void {
    this.openTagFunction('clickAction', this.editClickActionButton);
    /*this.openPanelWithCallback(this.expansionPanel, () => {
      this.openPanelWithCallback(this.clickActionExpansionPanel, () => {
        this.clickAction.focus();
      });
    });*/
  }

  private openTagFunction(tagFunctionType: 'renderFunction' | 'clickAction',
                          button: MatButton) {
    const trigger = button._elementRef.nativeElement;
    trigger.scrollIntoView();
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      let tagFunctionControl: AbstractControl;
      let completer: TbEditorCompleter;
      if (tagFunctionType === 'renderFunction') {
        tagFunctionControl = this.tagFormGroup.get('stateRenderFunction');
        completer = this.elementStateRenderFunctionCompleter;
      } else if (tagFunctionType === 'clickAction') {
        tagFunctionControl = this.tagFormGroup.get('clickAction');
        completer = this.clickActionFunctionCompleter;
      }
      const ctx: any = {
        tagFunction: tagFunctionControl.value,
        tagFunctionType,
        tag: this.tagFormGroup.get('tag').value,
        completer
      };
      const scadaSymbolTagFunctionPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ScadaSymbolMetadataTagFunctionPanelComponent,
        ['leftOnly', 'leftTopOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      scadaSymbolTagFunctionPanelPopover.tbComponentRef.instance.popover = scadaSymbolTagFunctionPanelPopover;
      scadaSymbolTagFunctionPanelPopover.tbComponentRef.instance.tagFunctionApplied.subscribe((tagFunction) => {
        scadaSymbolTagFunctionPanelPopover.hide();
        tagFunctionControl.patchValue(tagFunction, {emitEvent: false});
        this.updateModel();
      });
    }
  }

/*  private openPanelWithCallback(panel: MatExpansionPanel, callback: () => void) {
    if (!panel.expanded) {
      const s = panel.afterExpand.subscribe(() => {
        s.unsubscribe();
        setTimeout(() => {
          callback();
        });
      });
      panel.open();
    } else {
      callback();
    }
  }*/

  private updateModel() {
    const value = this.tagFormGroup.value;
    this.modelValue = {
      tag: value.tag,
      stateRenderFunction: value.stateRenderFunction
    };
    if (value.clickAction) {
      this.modelValue.actions = {
        click: {
          actionFunction: value.clickAction
        }
      };
    } else {
      this.modelValue.actions = null;
    }
    this.propagateChange(this.modelValue);
  }
}
