// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ConverterMsg } from '@shared/models/converter.models';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
    selector: 'tb-update-payload-panel',
    templateUrl: './updated-payload-panel.component.html',
    standalone: false
})
export class UpdatedPayloadPanelComponent implements OnInit {

  @Input()
  originalMsg: ConverterMsg;

  @Output()
  changesMsgApplied = new EventEmitter<ConverterMsg>();

  payloadForm = this.fb.group({
    payload: [],
    metadata: []
  })

  constructor(private fb: FormBuilder,
              private popover: TbPopoverComponent<UpdatedPayloadPanelComponent>,) {}

  ngOnInit() {
    this.payloadForm.patchValue(this.originalMsg, {emitEvent: false});
  }

  cancel() {
    this.popover.hide();
  }

  apply() {
    this.changesMsgApplied.emit(this.payloadForm.value as ConverterMsg);
  }
}
