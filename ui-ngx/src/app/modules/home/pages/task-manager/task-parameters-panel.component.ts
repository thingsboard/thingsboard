// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { Job, JobStatus } from '@app/shared/models/job.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Ace } from 'ace-builds';
import { getAce, updateEditorSize } from '@shared/models/ace/ace.models';
import { deepClone } from '@core/utils';

@Component({
    selector: 'tb-task-parameters-panel',
    templateUrl: './task-parameters-panel.component.html',
    styleUrls: ['./task-parameters-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class TaskParametersPanelComponent implements OnInit, OnDestroy {

  @ViewChild('taskContainer', {static: true})
  taskContainerElmRef: ElementRef;

  @ViewChild('taskPanel', {static: true})
  taskPanelElmRef: ElementRef;

  JobStatus = JobStatus;

  @Input()
  job: Job;

  private aceEditor: Ace.Editor;

  constructor(private popover: TbPopoverComponent<TaskParametersPanelComponent>,
              private renderer: Renderer2) {
  }

  ngOnInit() {
    this.createEditor();
  }

  ngOnDestroy() {
    this.aceEditor?.destroy();
  }

  cancel() {
    this.popover.hide();
  }

  private createEditor() {
    const editorElement = this.taskContainerElmRef.nativeElement;
    const editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/json`,
      theme: 'ace/theme/github',
      showFoldWidgets: true,
      foldStyle: 'markbeginend',
      showGutter: true,
      showPrintMargin: false,
      readOnly: true,
      enableSnippets: false,
      enableBasicAutocompletion: false,
      enableLiveAutocompletion: false
    };

    getAce().subscribe(
      (ace) => {
        this.aceEditor = ace.edit(editorElement, editorOptions);
        this.aceEditor.session.setUseWrapMode(false);
        const cloneConfig = deepClone(this.job.configuration);
        delete cloneConfig.toReprocess;
        delete cloneConfig.tasksKey;
        delete cloneConfig.type;
        const value = JSON.stringify(cloneConfig, null, 2);
        this.aceEditor.setValue(value, -1);
        updateEditorSize(editorElement, value, this.aceEditor, this.renderer, {showGutter: true});
        this.renderer.setStyle(this.taskPanelElmRef.nativeElement, 'width', editorElement.style.width);
        this.popover.updatePosition();
      }
    );
  }
}
