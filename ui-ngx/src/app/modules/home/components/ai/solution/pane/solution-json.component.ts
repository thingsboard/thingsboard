// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, effect, ElementRef, EventEmitter, inject, input, Output, signal, viewChild } from '@angular/core';
import { ClipboardService } from 'ngx-clipboard';
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

  draft = signal('');

  error = computed(() => {
    try {
      JSON.parse(this.draft());
      return '';
    } catch (e) {
      return (e as Error).message;
    }
  });

  private original = '';

  private code = viewChild<ElementRef<HTMLElement>>('code');
  private layer = viewChild<ElementRef<HTMLElement>>('layer');
  private area = viewChild<ElementRef<HTMLTextAreaElement>>('area');
  private clipboard = inject(ClipboardService);
  private copiedTimer: ReturnType<typeof setTimeout>;

  private render = effect(() => {
    const target = this.code()?.nativeElement;
    const text = this.text();
    if (target && !this.editing()) {
      target.replaceChildren(...highlightCode('json', text));
    }
  });

  private paint = effect(() => {
    const layer = this.layer()?.nativeElement;
    const text = this.draft();
    if (layer && this.editing()) {
      layer.replaceChildren(...highlightCode('json', text));
    }
  });

  text(): string {
    return JSON.stringify(this.value() ?? null, null, 2);
  }

  copy(): void {
    if (this.clipboard.copyFromContent(this.editing() ? this.draft() : this.text())) {
      this.copied.set(true);
      clearTimeout(this.copiedTimer);
      this.copiedTimer = setTimeout(() => this.copied.set(false), 1200);
    }
  }

  download(): void {
    const blob = new Blob([this.editing() ? this.draft() : this.text()], {type: 'application/json'});
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
      this.original = this.text();
      this.draft.set(this.original);
      setTimeout(() => {
        const area = this.area()?.nativeElement;
        area?.focus({preventScroll: true});
        area?.setSelectionRange(0, 0);
      });
    }
    this.editing.set(on);
    this.editingChange.emit(on);
  }

  onInput(value: string): void {
    this.draft.set(value);
  }

  onKey(event: KeyboardEvent): void {
    const area = event.target as HTMLTextAreaElement;
    if (event.key === 'Tab' && !event.shiftKey && !event.metaKey && !event.ctrlKey && !event.altKey) {
      event.preventDefault();
      area.setRangeText('  ', area.selectionStart, area.selectionEnd, 'end');
      this.onInput(area.value);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      if (this.draft() === this.original) {
        this.done();
      }
    }
  }

  focusEditor(event: MouseEvent): void {
    if (event.target !== this.area()?.nativeElement) {
      this.area()?.nativeElement.focus();
    }
  }

  edited(): unknown {
    return JSON.parse(this.draft());
  }

  get dirty(): boolean {
    return this.draft() !== this.original && !this.error();
  }

  done(): void {
    this.editing.set(false);
    this.editingChange.emit(false);
  }
}
