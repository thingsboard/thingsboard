// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, EventEmitter, inject, input, Output, Renderer2, signal, ViewContainerRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AiQuestion, AiQuestionAnswer } from '@shared/models/solution-creator.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { ColorPickerPanelComponent } from '@shared/components/color-picker/color-picker-panel.component';
import tinycolor from 'tinycolor2';

const MULTI_SEPARATOR = ', ';

const HEX_COLOR = /#[0-9a-f]{6}(?![0-9a-z])|#[0-9a-f]{3}(?![0-9a-z])/i;

export function aiColorOf(text: string | undefined): string | null {
  return text?.match(HEX_COLOR)?.[0] ?? null;
}

export function aiAnswersText(answers: AiQuestionAnswer[]): string {
  return answers.map(item => item.question.question.trim() + ' ' + item.answer.trim()).join('\n');
}

export function aiAnswersPrompt(answers: AiQuestionAnswer[]): {message: string; secrets?: Record<string, string>} {
  const secrets: Record<string, string> = {};
  const lines = answers.map((item, index) => {
    if (!item.secret || !item.answer) {
      return item.question.question.trim() + ' ' + item.answer.trim();
    }
    const name = (item.question.id || '').replace(/[^A-Za-z0-9_.-]/g, '_').slice(0, 64) || 'answer_' + (index + 1);
    secrets[name] = item.answer;
    return item.question.question.trim() + ' {{secret:' + name + '}}';
  });
  return Object.keys(secrets).length ? {message: lines.join('\n'), secrets} : {message: lines.join('\n')};
}

export function aiAnswersOf(content: string | undefined, questions: AiQuestion[] | undefined): AiQuestionAnswer[] | null {
  if (!content || !questions?.length) {
    return null;
  }
  const lines = content.split('\n').map(line => line.trim()).filter(Boolean);
  const answers: AiQuestionAnswer[] = [];
  for (const line of lines) {
    const question = questions.find(item => line.startsWith(item.question.trim() + ' '));
    if (!question) {
      return null;
    }
    answers.push({question, answer: line.slice(question.question.trim().length).trim()});
  }
  return answers.length ? answers : null;
}

function recommendedOf(question: AiQuestion): string[] {
  const recommended = Array.isArray(question.recommended) ? question.recommended : question.recommended ? [question.recommended] : [];
  return recommended.filter(label => question.options.some(option => option.label === label));
}

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

  private translate = inject(TranslateService);
  private popoverService = inject(TbPopoverService);
  private renderer = inject(Renderer2);
  private viewContainerRef = inject(ViewContainerRef);

  colorOf = aiColorOf;

  at = signal(0);
  move = signal<'next' | 'back' | null>(null);
  private picked = signal<Record<string, string>>({});
  private chosen = signal<Record<string, string[]>>({});
  private typed = signal<Record<string, string>>({});

  current = computed(() => this.questions()[Math.min(this.at(), this.questions().length - 1)]);
  last = computed(() => this.at() >= this.questions().length - 1);

  valueOf(question: AiQuestion): string {
    const picked = this.picked()[question.id];
    if (picked !== undefined) {
      return picked;
    }
    return this.isFree(question) ? '' : this.defaultOf(question);
  }

  isFree(question: AiQuestion): boolean {
    return !question.options?.length;
  }

  isChosen(question: AiQuestion, label: string): boolean {
    return (this.chosen()[question.id] ?? recommendedOf(question)).includes(label);
  }

  toggle(question: AiQuestion, label: string): void {
    const current = this.chosen()[question.id] ?? recommendedOf(question);
    this.chosen.update(values => ({...values, [question.id]: current.includes(label) ? current.filter(item => item !== label) : [...current, label]}));
  }

  isOther(question: AiQuestion): boolean {
    if (this.isFree(question)) {
      return true;
    }
    if (question.multiple) {
      return !!this.otherText(question).trim();
    }
    return this.picked()[question.id] === null;
  }

  otherText(question: AiQuestion): string {
    return this.typed()[question.id] ?? '';
  }

  ready(question: AiQuestion): boolean {
    if (question.multiple && !this.isFree(question)) {
      return true;
    }
    return this.isOther(question) ? !!this.otherText(question).trim() : !!this.valueOf(question);
  }

  sheet = computed(() => this.questions().length > 1 && this.questions().every(question =>
    !question.multiple && !this.isFree(question) && (this.isChipRow(question) || question.options.length <= 3)));

  allReady = computed(() => this.questions().every(question => this.ready(question)));

  finish(): void {
    if (!this.disabled() && this.questions().every(question => this.ready(question))) {
      this.done.emit(this.answers());
    }
  }

  isChipRow(question: AiQuestion): boolean {
    return !question.multiple && question.options.length >= 2 && question.options.length <= (this.isColor(question) ? 6 : 4)
      && question.options.every(option => option.label.length <= 24);
  }

  chosenHint(question: AiQuestion): string | undefined {
    const value = this.valueOf(question);
    return question.options.find(option => option.label === value)?.hint;
  }

  pick(question: AiQuestion, label: string): void {
    this.picked.update(values => ({...values, [question.id]: label}));
  }

  isColor(question: AiQuestion): boolean {
    return !question.multiple && !question.secret
      && (!!question.color || (!!question.options?.length && question.options.every(option => !!aiColorOf(option.label))));
  }

  pickColor(question: AiQuestion, trigger: HTMLElement): void {
    if (this.disabled()) {
      return;
    }
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
      return;
    }
    const current = aiColorOf(this.isOther(question) ? this.otherText(question) : this.valueOf(question)) ?? '#305680';
    const popover = this.popoverService.displayPopover({
      trigger,
      renderer: this.renderer,
      hostView: this.viewContainerRef,
      componentType: ColorPickerPanelComponent,
      preferredPlacement: ['bottomLeft', 'bottom', 'topLeft', 'top'],
      context: {color: current, disableAlpha: true, colorCancelButton: true},
      showCloseButton: false,
      popoverContentStyle: {padding: '12px 4px 12px 12px'},
      isModal: true
    });
    popover.tbComponentRef.instance.popover = popover;
    popover.tbComponentRef.instance.colorSelected.subscribe((color: string) => {
      popover.hide();
      const hex = tinycolor(color).toHexString();
      if (!question.options?.length) {
        this.typed.update(values => ({...values, [question.id]: hex}));
        return;
      }
      const option = question.options.find(item => aiColorOf(item.label)?.toLowerCase() === hex);
      if (option) {
        this.pick(question, option.label);
      } else {
        this.picked.update(values => ({...values, [question.id]: null}));
        this.typed.update(values => ({...values, [question.id]: hex}));
      }
    });
    popover.tbComponentRef.instance.colorCancelDialog.subscribe(() => popover.hide());
  }

  pickOther(question: AiQuestion): void {
    if (question.multiple || this.isFree(question)) {
      return;
    }
    if (!this.isOther(question)) {
      this.picked.update(values => ({...values, [question.id]: null}));
    }
  }

  type(question: AiQuestion, area: HTMLTextAreaElement | HTMLInputElement): void {
    this.pickOther(question);
    this.typed.update(values => ({...values, [question.id]: area.value}));
    if (area instanceof HTMLTextAreaElement) {
      area.style.height = 'auto';
      area.style.height = area.scrollHeight + 'px';
    }
  }

  shown(item: {question: AiQuestion; answer: string}): string {
    return item.question.secret && item.answer ? '••••••••' : item.answer;
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
      if (this.sheet()) {
        this.finish();
      } else {
        this.next();
      }
    }
  }

  private answers(): AiQuestionAnswer[] {
    return this.questions().map(question => ({question, answer: this.answerOf(question), secret: !!question.secret && !question.options?.length}));
  }

  private answerOf(question: AiQuestion): string {
    if (question.multiple && !this.isFree(question)) {
      const values = [...question.options.map(option => option.label).filter(label => this.isChosen(question, label))];
      const typed = this.otherText(question).trim();
      if (typed) {
        values.push(typed);
      }
      return values.length ? values.join(MULTI_SEPARATOR) : this.translate.instant('ai-assistant.question.none');
    }
    return this.isOther(question) ? this.otherText(question).trim() : this.valueOf(question);
  }

  private defaultOf(question: AiQuestion): string {
    if (this.isFree(question)) {
      return this.translate.instant('ai-assistant.question.no-answer');
    }
    if (question.multiple) {
      const recommended = recommendedOf(question);
      return recommended.length ? recommended.join(MULTI_SEPARATOR) : this.translate.instant('ai-assistant.question.none');
    }
    return recommendedOf(question)[0] ?? question.options?.[0]?.label ?? '';
  }
}
