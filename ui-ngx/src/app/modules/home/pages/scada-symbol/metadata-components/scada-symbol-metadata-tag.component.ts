// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
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
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { ScadaSymbolTag } from '@home/components/widget/lib/scada/scada-symbol.models';
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

  @ViewChild('editClickActionButton')
  editClickActionButton: MatButton;

  @Input()
  disabled: boolean;

  @Input()
  elementStateRenderFunctionCompleter: TbEditorCompleter;

  @Input()
  clickActionFunctionCompleter: TbEditorCompleter;

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

  writeValue(value: ScadaSymbolTag): void {
    this.modelValue = value;
    const clickAction = value?.actions?.click?.actionFunction;
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
  }

  editClickAction(): void {
    this.openTagFunction('clickAction', this.editClickActionButton);
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
