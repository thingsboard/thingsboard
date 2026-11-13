// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  Component,
  forwardRef,
  Input,
  OnInit,
  Renderer2,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { activeActionTriggers, ScadaSymbolTag } from '@home/components/widget/lib/scada/scada-symbol.models';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { MatButton } from '@angular/material/button';
import {
  ScadaSymbolMetadataTagActionsPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag-actions-panel.component';
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

  @ViewChild('editActionsButton')
  editActionsButton: MatButton;

  @Input()
  disabled: boolean;

  @Input()
  elementStateRenderFunctionCompleter: TbEditorCompleter;

  @Input()
  actionFunctionCompleter: TbEditorCompleter;

  actionsCount = 0;

  tagFormGroup: UntypedFormGroup;

  modelValue: ScadaSymbolTag;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit() {
    this.tagFormGroup = this.fb.group({
      tag: [null, []],
      stateRenderFunction: [null, []]
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

  writeValue(value: ScadaSymbolTag): void {
    this.modelValue = value;
    this.tagFormGroup.patchValue(
      {
        tag: value?.tag,
        stateRenderFunction: value?.stateRenderFunction
      }, {emitEvent: false}
    );
    this.updateActionsCount();
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
    this.openTagFunction(this.editStateRenderFunctionButton);
  }

  editActions(): void {
    const trigger = this.editActionsButton._elementRef.nativeElement;
    trigger.scrollIntoView();
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const actionsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ScadaSymbolMetadataTagActionsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          tag: this.tagFormGroup.get('tag').value,
          actions: this.modelValue?.actions,
          completer: this.actionFunctionCompleter,
          disabled: this.disabled
        },
        isModal: true
      });
      actionsPanelPopover.tbComponentRef.instance.popover = actionsPanelPopover;
      actionsPanelPopover.tbComponentRef.instance.actionsApplied.subscribe((actions) => {
        actionsPanelPopover.hide();
        this.updateModel(actions);
      });
    }
  }

  private openTagFunction(button: MatButton) {
    const trigger = button._elementRef.nativeElement;
    trigger.scrollIntoView();
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const tagFunctionControl = this.tagFormGroup.get('stateRenderFunction');
      const scadaSymbolTagFunctionPanelPopover =  this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ScadaSymbolMetadataTagFunctionPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          tagFunction: tagFunctionControl.value,
          tag: this.tagFormGroup.get('tag').value,
          completer: this.elementStateRenderFunctionCompleter,
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

  private updateModel(actions = this.modelValue?.actions) {
    const value = this.tagFormGroup.value;
    this.modelValue = {
      tag: value.tag,
      stateRenderFunction: value.stateRenderFunction,
      actions
    };
    this.updateActionsCount();
    this.propagateChange(this.modelValue);
  }

  private updateActionsCount() {
    this.actionsCount = activeActionTriggers(this.modelValue?.actions).length;
  }
}
