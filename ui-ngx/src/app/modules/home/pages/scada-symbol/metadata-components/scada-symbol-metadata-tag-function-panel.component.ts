// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetService } from '@core/http/widget.service';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { AceHighlightRules } from '@shared/models/ace/ace.models';
import { scadaSymbolRenderFunctionHighlightRules } from '@home/pages/scada-symbol/scada-symbol-editor.models';
import { JsFuncComponent } from '@shared/components/js-func.component';

@Component({
    selector: 'tb-scada-symbol-metadata-tag-function-panel',
    templateUrl: './scada-symbol-metadata-tag-function-panel.component.html',
    styleUrls: ['./scada-symbol-metadata-tag-function-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ScadaSymbolMetadataTagFunctionPanelComponent implements OnInit, AfterViewInit {

  @ViewChild('tagFunctionComponent')
  tagFunctionComponent: JsFuncComponent;

  @Input()
  tagFunction: string;

  @Input()
  tag: string;

  @Input()
  completer: TbEditorCompleter;

  @Input()
  disabled: boolean;

  @Input()
  popover: TbPopoverComponent<ScadaSymbolMetadataTagFunctionPanelComponent>;

  @Output()
  tagFunctionApplied = new EventEmitter<string>();

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  tagFunctionFormGroup: UntypedFormGroup;

  panelTitle = 'scada.state-render-function';

  tagFunctionArgs = ['ctx', 'element'];

  tagFunctionHelpId = 'scada/tag_state_render_fn';

  highlightRules: AceHighlightRules = scadaSymbolRenderFunctionHighlightRules;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService) {
  }

  ngOnInit(): void {
    this.tagFunctionFormGroup = this.fb.group(
      {
        tagFunction: [this.tagFunction, []]
      }
    );
    if (this.disabled) {
      this.tagFunctionFormGroup.disable({emitEvent: false});
    }
  }

  ngAfterViewInit() {
    this.tagFunctionComponent.focus();
  }

  cancel() {
    this.popover?.hide();
  }

  applyTagFunction() {
    const tagFunction: string = this.tagFunctionFormGroup.get('tagFunction').value;
    this.tagFunctionApplied.emit(tagFunction);
  }
}
