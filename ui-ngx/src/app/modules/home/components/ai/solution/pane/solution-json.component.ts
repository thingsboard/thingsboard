// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, effect, EventEmitter, input, Output, untracked, viewChild } from '@angular/core';
import { AiFile, aiFileOf } from '@home/components/ai/ai-file.models';
import { AiFileBodyComponent, AiFileSaver } from '@home/components/ai/ai-file-body.component';

@Component({
  selector: 'tb-solution-json',
  templateUrl: './solution-json.component.html',
  styleUrls: ['./solution-json.component.scss'],
  standalone: false
})
export class SolutionJsonComponent {

  value = input<unknown>(null);

  fileName = input('solution.json');

  draftId = input('');

  wrap = input(false);

  whitespace = input(false);

  @Output() editingChange = new EventEmitter<boolean>();

  @Output() applied = new EventEmitter<unknown>();

  @Output() discarded = new EventEmitter<void>();

  body = viewChild.required(AiFileBodyComponent);

  file = computed<AiFile>(() => aiFileOf('solution-json:' + this.draftId() + ':' + this.fileName(),
    {name: this.fileName(), content: JSON.stringify(this.value() ?? null, null, 2) + '\n'}));

  error = computed(() => {
    const body = this.body();
    if (!body.editing()) {
      return '';
    }
    try {
      JSON.parse(body.draft());
      return '';
    } catch (e) {
      return (e as Error).message;
    }
  });

  readonly saver: AiFileSaver = content => {
    try {
      this.applied.emit(JSON.parse(content));
      return Promise.resolve(true);
    } catch {
      return Promise.resolve(false);
    }
  };

  private editingEffect = effect(() => {
    const editing = this.body().editing();
    untracked(() => this.editingChange.emit(editing));
  });

  text(): string {
    const body = this.body();
    return body.editing() ? body.draft() : this.file().content;
  }
}
