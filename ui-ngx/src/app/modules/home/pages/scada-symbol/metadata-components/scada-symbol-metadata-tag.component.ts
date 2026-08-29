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
  Component,
  forwardRef,
  Input,
  OnInit,
  QueryList,
  Renderer2,
  ViewChild,
  ViewChildren,
  ViewContainerRef,
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
  Validator
} from '@angular/forms';
import {
  ScadaSymbolActionTrigger,
  scadaSymbolActionTriggers,
  ScadaSymbolTag
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
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
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ScadaSymbolMetadataTagComponent implements ControlValueAccessor, OnInit, Validator {

  @ViewChild('editStateRenderFunctionButton')
  editStateRenderFunctionButton: MatButton;

  @ViewChildren('editActionButton')
  editActionButtons: QueryList<MatButton>;

  @Input()
  disabled: boolean;

  @Input()
  elementStateRenderFunctionCompleter: TbEditorCompleter;

  @Input()
  actionFunctionCompleter: TbEditorCompleter;

  actionTriggers = scadaSymbolActionTriggers;

  tagFormGroup: UntypedFormGroup;

  modelValue: ScadaSymbolTag;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit() {
    const controls: {[key: string]: any} = {
      tag: [null, []],
      stateRenderFunction: [null, []]
    };
    for (const trigger of this.actionTriggers) {
      controls[this.actionControlName(trigger)] = [null, []];
    }
    this.tagFormGroup = this.fb.group(controls);
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

  writeValue(value: ScadaSymbolTag): void {
    this.modelValue = value;
    const patch: {[key: string]: any} = {
      tag: value?.tag,
      stateRenderFunction: value?.stateRenderFunction
    };
    for (const trigger of this.actionTriggers) {
      patch[this.actionControlName(trigger)] = value?.actions?.[trigger]?.actionFunction;
    }
    this.tagFormGroup.patchValue(patch, {emitEvent: false});
  }

  public validate(_c: UntypedFormControl) {
    const valid = this.tagFormGroup.valid;
    return valid ? null : {
      tag: {
        valid: false,
      },
    };
  }

  actionControlName(trigger: ScadaSymbolActionTrigger): string {
    return trigger + 'Action';
  }

  editTagStateRenderFunction(): void {
    this.openTagFunction('renderFunction', this.editStateRenderFunctionButton);
  }

  editAction(trigger: ScadaSymbolActionTrigger): void {
    const button = this.editActionButtons
      .find(b => b._elementRef.nativeElement.getAttribute('data-trigger') === trigger);
    if (button) {
      this.openTagFunction(trigger, button);
    }
  }

  private openTagFunction(tagFunctionType: 'renderFunction' | ScadaSymbolActionTrigger,
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
      } else {
        tagFunctionControl = this.tagFormGroup.get(this.actionControlName(tagFunctionType));
        completer = this.actionFunctionCompleter;
      }
      const scadaSymbolTagFunctionPanelPopover =  this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ScadaSymbolMetadataTagFunctionPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          tagFunction: tagFunctionControl.value,
          tagFunctionType,
          tag: this.tagFormGroup.get('tag').value,
          completer,
          disabled: this.disabled
        },
        isModal: true
      });
      scadaSymbolTagFunctionPanelPopover.tbComponentRef.instance.popover = scadaSymbolTagFunctionPanelPopover;
      scadaSymbolTagFunctionPanelPopover.tbComponentRef.instance.tagFunctionApplied.subscribe((tagFunction) => {
        scadaSymbolTagFunctionPanelPopover.hide();
        tagFunctionControl.patchValue(tagFunction, {emitEvent: false});
        this.updateModel();
      });
    }
  }

  private updateModel() {
    const value = this.tagFormGroup.value;
    this.modelValue = {
      tag: value.tag,
      stateRenderFunction: value.stateRenderFunction
    };
    let actions: ScadaSymbolTag['actions'] = null;
    for (const trigger of this.actionTriggers) {
      const actionFunction = value[this.actionControlName(trigger)];
      if (actionFunction) {
        actions = actions || {};
        actions[trigger] = {
          actionFunction
        };
      }
    }
    this.modelValue.actions = actions;
    this.propagateChange(this.modelValue);
  }
}
