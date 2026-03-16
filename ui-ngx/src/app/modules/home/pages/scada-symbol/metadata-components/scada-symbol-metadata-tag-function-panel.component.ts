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
import {
  scadaSymbolClickActionHighlightRules,
  scadaSymbolRenderFunctionHighlightRules
} from '@home/pages/scada-symbol/scada-symbol-editor.models';
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
  tagFunctionType: 'renderFunction' | 'clickAction';

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

  panelTitle: string;

  tagFunctionArgs: string[];

  tagFunctionHelpId: string;

  highlightRules: AceHighlightRules;

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
    if (this.tagFunctionType === 'renderFunction') {
      this.panelTitle = 'scada.state-render-function';
      this.tagFunctionArgs = ['ctx', 'element'];
      this.highlightRules = scadaSymbolRenderFunctionHighlightRules;
      this.tagFunctionHelpId = 'scada/tag_state_render_fn';
    } else if (this.tagFunctionType === 'clickAction') {
      this.panelTitle = 'scada.tag.on-click-action';
      this.tagFunctionArgs = ['ctx', 'element', 'event'];
      this.highlightRules = scadaSymbolClickActionHighlightRules;
      this.tagFunctionHelpId = 'scada/tag_click_action_fn';
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
