// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, effect, ElementRef, EventEmitter, inject, input, Output, signal, viewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ClipboardService } from 'ngx-clipboard';
import { deepClone } from '@core/utils';
import { highlightCode } from '@home/components/ai/ai-code-block';

@Component({
  selector: 'tb-solution-json',
  templateUrl: './solution-json.component.html',
  styleUrls: ['./solution-json.component.scss'],
  standalone: false
})
export class SolutionJsonComponent {

  value = input<unknown>(null);

  fileName = input('solution.json');

  @Output() editingChange = new EventEmitter<boolean>();

  editing = signal(false);

  copied = signal(false);

  control = new FormControl<unknown>(null);

  private code = viewChild<ElementRef<HTMLElement>>('code');
  private clipboard = inject(ClipboardService);
  private copiedTimer: ReturnType<typeof setTimeout>;

  private render = effect(() => {
    const target = this.code()?.nativeElement;
    const text = this.text();
    if (target && !this.editing()) {
      target.replaceChildren(...highlightCode('json', text));
    }
  });

  text(): string {
    return JSON.stringify(this.value() ?? null, null, 2);
  }

  copy(): void {
    if (this.clipboard.copyFromContent(this.editing() ? JSON.stringify(this.control.value, null, 2) : this.text())) {
      this.copied.set(true);
      clearTimeout(this.copiedTimer);
      this.copiedTimer = setTimeout(() => this.copied.set(false), 1200);
    }
  }

  download(): void {
    const blob = new Blob([this.text()], {type: 'application/json'});
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = this.fileName();
    link.click();
    URL.revokeObjectURL(url);
  }

  toggleEdit(): void {
    const on = !this.editing();
    if (on) {
      this.control.setValue(deepClone(this.value()));
      this.control.markAsPristine();
    }
    this.editing.set(on);
    this.editingChange.emit(on);
  }

  edited(): unknown {
    return this.control.value;
  }

  get dirty(): boolean {
    return this.control.dirty;
  }

  done(): void {
    this.control.markAsPristine();
    this.editing.set(false);
    this.editingChange.emit(false);
  }
}
