// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, EventEmitter, inject, input, Output, signal } from '@angular/core';
import { ChatLevel } from '@shared/models/ai-chat.models';

@Component({
  selector: 'tb-ai-chat-title',
  templateUrl: './ai-chat-title.component.html',
  styleUrls: ['./ai-chat-title.component.scss'],
  standalone: false
})
export class AiChatTitleComponent {

  title = input<string | null | undefined>(null);
  placeholder = input('');
  level = input<ChatLevel | null>(null);
  statusTip = input('');
  renamable = input(true);
  compact = input(false);

  @Output() renamed = new EventEmitter<string>();

  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  renaming = signal(false);
  nameClipped = signal(false);

  startRename(): void {
    if (!this.title() || !this.renamable()) {
      return;
    }
    this.renaming.set(true);
    setTimeout(() => {
      const field = this.host.nativeElement.querySelector<HTMLInputElement>('.ai-chat-rename');
      field?.focus();
      field?.select();
    });
  }

  cancelRename(): void {
    this.renaming.set(false);
  }

  onRenameKeydown(event: KeyboardEvent): void {
    if (event.isComposing) {
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      this.finishRename((event.target as HTMLInputElement).value);
    } else if (event.key === 'Escape') {
      event.stopPropagation();
      this.renaming.set(false);
    }
  }

  finishRename(value: string): void {
    if (!this.renaming()) {
      return;
    }
    this.renaming.set(false);
    const title = value.trim();
    if (title && title !== this.title()) {
      this.renamed.emit(title);
    }
  }
}
