// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { CdkTextareaAutosize } from '@angular/cdk/text-field';

@Component({
  selector: 'tb-ai-prompt-input',
  templateUrl: './ai-prompt-input.component.html',
  styleUrls: ['./ai-prompt-input.component.scss'],
  standalone: false
})
export class AiPromptInputComponent {

  @Input() placeholder = '';

  @Input() loading: boolean;

  @Output() messageSent = new EventEmitter<string>();

  @ViewChild('autosize') autosize!: CdkTextareaAutosize;

  @ViewChild('textarea') textarea!: ElementRef<HTMLTextAreaElement>;

  message = new FormControl<string>('');

  onEnter(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!ke.shiftKey) {
      ke.preventDefault();
      this.send();
    }
  }

  send(): void {
    const msg = this.message.value?.trim();
    if (msg && !this.loading) {
      this.reset();
      this.messageSent.emit(msg);
    }
  }

  reset(): void {
    this.message.reset('');
    // Not autosize.reset(): it restores a height captured before the placeholder was known.
    this.autosize?.resizeToFitContent(true);
  }

  focus(): void {
    this.textarea?.nativeElement.focus();
  }

  setValue(value: string): void {
    this.message.setValue(value);
    setTimeout(() => {
      this.textarea?.nativeElement.focus();
      this.autosize?.resizeToFitContent(true);
    });
  }
}
