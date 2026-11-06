// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, EventEmitter, input, Output, signal } from '@angular/core';
import { AiQuestion, AiQuestionAnswer } from '@shared/models/solution-creator.models';

@Component({
  selector: 'tb-ai-question',
  templateUrl: './ai-question.component.html',
  standalone: false
})
export class AiQuestionComponent {

  questions = input.required<AiQuestion[]>();
  answered = input<AiQuestionAnswer[] | null>(null);
  disabled = input(false);
  time = input<number | undefined>(undefined);

  @Output() done = new EventEmitter<AiQuestionAnswer[]>();
  @Output() skip = new EventEmitter<AiQuestionAnswer[]>();

  at = signal(0);
  move = signal<'next' | 'back' | null>(null);
  private picked = signal<Record<string, string>>({});
  private typed = signal<Record<string, string>>({});

  current = computed(() => this.questions()[Math.min(this.at(), this.questions().length - 1)]);
  last = computed(() => this.at() >= this.questions().length - 1);

  valueOf(question: AiQuestion): string {
    const picked = this.picked()[question.id];
    if (picked !== undefined) {
      return picked;
    }
    return question.options.some(option => option.label === question.recommended) ? question.recommended : question.options[0]?.label ?? '';
  }

  isOther(question: AiQuestion): boolean {
    return this.picked()[question.id] === null;
  }

  otherText(question: AiQuestion): string {
    return this.typed()[question.id] ?? '';
  }

  ready(question: AiQuestion): boolean {
    return this.isOther(question) ? !!this.otherText(question).trim() : !!this.valueOf(question);
  }

  pick(question: AiQuestion, label: string): void {
    this.picked.update(values => ({...values, [question.id]: label}));
  }

  pickOther(question: AiQuestion): void {
    if (!this.isOther(question)) {
      this.picked.update(values => ({...values, [question.id]: null}));
    }
  }

  type(question: AiQuestion, area: HTMLTextAreaElement): void {
    this.pickOther(question);
    this.typed.update(values => ({...values, [question.id]: area.value}));
    area.style.height = 'auto';
    area.style.height = area.scrollHeight + 'px';
  }

  next(): void {
    const question = this.current();
    if (!question || !this.ready(question) || this.disabled()) {
      return;
    }
    if (this.last()) {
      this.done.emit(this.answers());
    } else {
      this.move.set('next');
      this.at.update(index => index + 1);
    }
  }

  back(): void {
    this.move.set('back');
    this.at.update(index => Math.max(0, index - 1));
  }

  skipAll(): void {
    if (!this.disabled()) {
      this.skip.emit(this.questions().map(question => ({question, answer: this.defaultOf(question)})));
    }
  }

  onEnter(event: Event): void {
    const keyboard = event as KeyboardEvent;
    if (!keyboard.shiftKey && !keyboard.isComposing) {
      event.preventDefault();
      this.next();
    }
  }

  private answers(): AiQuestionAnswer[] {
    return this.questions().map(question => ({
      question,
      answer: this.isOther(question) ? this.otherText(question).trim() : this.valueOf(question)
    }));
  }

  private defaultOf(question: AiQuestion): string {
    return question.options.some(option => option.label === question.recommended) ? question.recommended : question.options[0]?.label ?? '';
  }
}
