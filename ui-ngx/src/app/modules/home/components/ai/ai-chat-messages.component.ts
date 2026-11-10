// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { AiQuestion, AiQuestionAnswer, ApprovalChatMessage, ChatMessage, GeneralChatMessage } from '@shared/models/solution-creator.models';
import { ClipboardService } from 'ngx-clipboard';
import { TranslateService } from '@ngx-translate/core';
import {
  ANSWER_PROBLEMS,
  AnswerProblem,
  AnswerRating,
  ApprovalResult,
  ToolExecutionRequested,
  ToolApprovalResult,
  ToolExecutionResult
} from '@shared/models/ai-chat.models';
import {
  aiPlanOf,
  AiSource,
  aiSourcesOf,
  CITE_SOURCES_TOOL,
  AiPlanProgress,
  aiPlanProgressOf,
  AiPlanStep,
  AiPrompt,
  aiPromptsOf,
  aiQuestionsOf,
  ASK_USER_TOOL,
  AiWorkState,
  isHiddenAiAction,
  isKitCardTool,
  PROPOSE_PLAN_TOOL,
  SUGGEST_PROMPTS_TOOL,
  UPDATE_PLAN_TOOL
} from '@home/components/ai/ai-action.models';
import { AiPlanRun } from '@home/components/ai/ai-plan.component';
import { AiFile, aiFileOf, WRITE_FILE_TOOL } from '@home/components/ai/ai-file.models';
import { aiAnswersOf, aiAnswersText } from '@home/components/ai/ai-question.component';
import { EntityId } from '@shared/models/id/entity-id';

export interface AnswerRatingEvent {
  index: number;
  rating: AnswerRating;
}

export interface AnswerReportEvent {
  index: number;
  problem: AnswerProblem;
  comment?: string;
}
import { decorateCodeBlocks, decorateTables } from '@home/components/ai/ai-code-block';
import { decorateHints, decorateLinks, decorateReferences, decorateSeverities } from '@home/components/ai/ai-references';
import { AlarmSeverity, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { Router } from '@angular/router';

const SOLUTION_STEP_TOOLS = ['create_solution', 'install_solution'];

@Component({
  selector: 'tb-ai-chat-messages',
  templateUrl: './ai-chat-messages.component.html',
  styleUrls: ['./ai-chat-messages.component.scss'],
  standalone: false
})
export class AiChatMessagesComponent implements OnChanges, OnDestroy {

  @Input() messages: ChatMessage[] = [];
  @Input() loading = false;
  @Input() loadingLongTime = false;
  @Input() additionalStyles: string[];
  @Input() completedExecutions: Record<string, ToolExecutionResult> = {};
  @Input() autoApprove = false;
  @Input() scrollContainer: HTMLElement;
  @Input() initialScroll: 'lastUserMessage' | 'bottom' = 'lastUserMessage';
  @Input() denyReasons: Record<string, string> = {};
  @Input() work: AiWorkState | undefined;
  @Input() feedback = false;
  @Input() shownFile: string | null = null;
  @Output() rate = new EventEmitter<AnswerRatingEvent>();
  @Output() chip = new EventEmitter<string>();
  @Output() report = new EventEmitter<AnswerReportEvent>();
  @Output() approvalAction = new EventEmitter<ApprovalResult>();
  @Output() askAgain = new EventEmitter<void>();
  @Output() editMessage = new EventEmitter<string>();
  @Output() resume = new EventEmitter<void>();
  @Output() autoApproveRest = new EventEmitter<void>();
  @Output() runPlan = new EventEmitter<{run: AiPlanRun; steps: AiPlanStep[]}>();
  @Output() changePlan = new EventEmitter<void>();
  @Output() openFile = new EventEmitter<AiFile>();
  @Output() prompt = new EventEmitter<string>();
  @Output() answer = new EventEmitter<string>();
  @Output() resourceError = new EventEmitter<any>();

  waitApproval = false;

  actionRunning = false;

  runningLead = -1;

  runningCount = 0;

  summaries: Record<number, EntityId[]> = {};
  prompts: {index: number; items: AiPrompt[]} | null = null;
  asking: {index: number; questions: AiQuestion[]} | null = null;
  retryIndex = -1;
  stoppedSteps: Record<number, number> = {};
  answersAt: Record<number, AiQuestionAnswer[]> = {};
  offerIndex = -1;
  streaming = false;
  files: Record<number, AiFile> = {};
  sources: Record<number, AiSource[]> = {};
  plans: Record<number, {steps: AiPlanStep[]; progress: AiPlanProgress | null; gate: boolean}> = {};

  private keptAsking = new Set<number>();

  dayLines: Record<number, string> = {};

  private unfolded = new Set<number>();

  private dayTimer: ReturnType<typeof setTimeout>;

  private static readonly PIN_TO_BOTTOM_MS = 1500;
  private static readonly COPIED_MS = 1200;
  private static readonly FOLD_LINES = 6;
  private static readonly FOLD_CHARS = 600;

  private releaseBottom: () => void;

  lastAnswerIndex = -1;

  readonly problems = ANSWER_PROBLEMS;

  reportingIndex = -1;

  reportProblem: AnswerProblem | null = null;

  copiedIndex = -1;

  private copiedTimeout: ReturnType<typeof setTimeout>;

  constructor(private elementRef: ElementRef<HTMLElement>,
              private clipboard: ClipboardService,
              private translate: TranslateService,
              private router: Router) {}

  copyText(text: string, index: number): void {
    if (this.clipboard.copyFromContent(text)) {
      this.copiedIndex = index;
      clearTimeout(this.copiedTimeout);
      this.copiedTimeout = setTimeout(() => this.copiedIndex = -1, AiChatMessagesComponent.COPIED_MS);
    }
  }

  decorateCode(answer: HTMLElement, message?: ChatMessage): void {
    decorateCodeBlocks(answer, this.translate, text => this.clipboard.copyFromContent(text));
    decorateTables(answer);
    decorateReferences(answer, (message as GeneralChatMessage)?.references);
    decorateLinks(answer);
    decorateHints(answer);
    decorateSeverities(answer, (message as GeneralChatMessage)?.severities,
      severity => this.translate.instant(alarmSeverityTranslations.get(severity as AlarmSeverity) ?? severity));
  }

  @HostListener('click', ['$event'])
  openInApp(event: MouseEvent): void {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
      return;
    }
    const link = (event.target as HTMLElement)?.closest('.agent a[href]') as HTMLAnchorElement | null;
    if (!link || link.target === '_blank' || link.origin !== location.origin) {
      return;
    }
    event.preventDefault();
    this.router.navigateByUrl(link.pathname + link.search + link.hash);
  }

  isFolded(index: number, content: string): boolean {
    return !this.unfolded.has(index) && (content.split('\n').length > AiChatMessagesComponent.FOLD_LINES || content.length > AiChatMessagesComponent.FOLD_CHARS);
  }

  unfold(index: number): void {
    this.unfolded.add(index);
  }

  withoutDoneMark(content: string): string {
    return (content ?? '').replace(/^\s*(✅|🛠️|🛠|🤔|💡|🛑)\s*/u, '');
  }

  copyAnswer(answer: HTMLElement, index: number): void {
    const text = (answer.querySelector<HTMLElement>('tb-markdown') ?? answer).innerText.trim();
    if (this.clipboard.copyFromContent(text)) {
      this.copiedIndex = index;
      clearTimeout(this.copiedTimeout);
      this.copiedTimeout = setTimeout(() => this.copiedIndex = -1, AiChatMessagesComponent.COPIED_MS);
    }
  }

  toggleRating(index: number, current: string | undefined, rating: 'UP' | 'DOWN'): void {
    this.rate.emit({index, rating: current === rating ? 'NONE' : rating});
  }

  openReport(index: number): void {
    this.reportingIndex = index;
    this.reportProblem = null;
  }

  closeReport(): void {
    this.reportingIndex = -1;
    this.reportProblem = null;
  }

  sendReport(index: number, comment: string): void {
    if (!this.reportProblem) {
      return;
    }
    const words = comment.trim();
    this.report.emit(words ? {index, problem: this.reportProblem, comment: words} : {index, problem: this.reportProblem});
    this.closeReport();
  }

  fullTime(time: number): string {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const text = new Intl.DateTimeFormat(undefined, {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
    }).format(time);
    return zone ? `${text} (${zone})` : text;
  }

  getApprovalData(msg: ApprovalChatMessage): ToolExecutionRequested {
    return msg.data;
  }

  denyReasonOf(msg: ApprovalChatMessage): string | undefined {
    return this.completedExecutions[msg.data.executionId]?.reason ?? this.denyReasons[msg.data.executionId];
  }

  getCompletedExecution(msg: ApprovalChatMessage): ToolExecutionResult | undefined {
    return this.completedExecutions[msg.data.executionId];
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.loadingLongTime?.currentValue) {
      if (this.isNearBottom()) {
        setTimeout(() => this.scrollToBottom('smooth'), 0);
      }
    }
    if (changes.messages) {
      this.refreshDayLines();
    }
    if (changes.messages || changes.completedExecutions || changes.loading || changes.autoApprove || changes.denyReasons) {
      this.offerIndex = this.findOffer();
    }
    if (changes.messages || changes.completedExecutions || changes.loading) {
      this.summaries = this.findSummaries();
      this.prompts = this.findPrompts();
      this.asking = this.findAsking();
      this.plans = this.findPlans();
      this.files = this.findFiles();
      this.sources = this.findSources();
      this.answersAt = this.findAnswers();
      this.retryIndex = this.findRetry();
      this.stoppedSteps = this.findStoppedSteps();
    }
    if (changes.messages) {
      this.lastAnswerIndex = this.findLastAnswerIndex();
      const tail = this.messages[this.messages.length - 1];
      this.streaming = tail?.from === 'AI' && !!tail.streaming;
      const running = this.messages.map((message, index) => message.from === 'ACTION' && message.data.status === 'RUNNING' ? index : -1)
        .filter(index => index >= 0);
      this.actionRunning = running.length > 0;
      this.runningLead = running.length ? running[0] : -1;
      this.runningCount = running.length;
      if (changes.messages.firstChange) {
        if (this.initialScroll === 'bottom') {
          this.pinToBottom();
        } else {
          setTimeout(() => this.scrollToLastUserMessage(), 0);
        }
        return;
      }
      const wasNearBottom = this.isNearBottom();
      const prevMsgs = changes.messages.previousValue as ChatMessage[];
      const prevLastMsg = prevMsgs?.[prevMsgs.length - 1];
      const msgs = changes.messages.currentValue as ChatMessage[];
      const lastMsg = msgs[msgs.length - 1];
      // In auto-approve we are not blocked on the user, so the assistant keeps working: the
      // thinking indicator must stay visible (waitApproval false) and the view must follow the
      // stream of auto-generated cards down to the bottom.
      this.waitApproval = lastMsg?.from === 'APPROVAL' && !this.autoApprove;
      if (this.autoApprove) {
        setTimeout(() => this.scrollToBottom('auto'), 0);
      } else if (lastMsg?.from === 'USER') {
        setTimeout(() => this.scrollToBottom('smooth'), 0);
      } else if (lastMsg?.from === 'AI') {
        if (prevLastMsg?.from === 'USER') {
          setTimeout(() => this.scrollToLastUserMessage(), 0);
        } else if (wasNearBottom) {
          setTimeout(() => this.scrollToLastAiMessage(), 0);
        }
      } else if (lastMsg?.from === 'ACTION') {
        if (wasNearBottom) {
          setTimeout(() => this.scrollToBottom('smooth'), 0);
        }
      } else if (lastMsg?.from === 'APPROVAL') {
        if (prevLastMsg?.from === 'USER') {
          setTimeout(() => this.scrollToLastUserMessage(), 0);
        } else if (wasNearBottom) {
          setTimeout(() => this.scrollToLastAiMessage(), 0);
        }
      }
    }
  }

  keepAsking(): void {
    this.keptAsking.add(this.turnStart(this.offerIndex));
    this.offerIndex = -1;
  }

  private findOffer(): number {
    const last = this.messages.length - 1;
    const pending = this.messages[last];
    if (this.autoApprove || pending?.from !== 'APPROVAL' || !pending.data.needsApproval || pending.data.autoApproved
      || this.approvalResult(pending) || this.denyReasons[pending.data.executionId]) {
      return -1;
    }
    const start = this.turnStart(last);
    if (this.keptAsking.has(start)) {
      return -1;
    }
    const approvedBefore = this.messages.slice(start + 1, last).some(message => message.from === 'APPROVAL'
      && !message.data.autoApproved && this.approvalResult(message)?.approvalStatus === ToolApprovalResult.APPROVED);
    return approvedBefore ? last : -1;
  }

  private approvalResult(message: ApprovalChatMessage): ToolExecutionResult | undefined {
    return this.completedExecutions[message.data.executionId] ?? message.result;
  }

  private turnStart(index: number): number {
    for (let i = index; i >= 0; i--) {
      if (this.messages[i].from === 'USER') {
        return i;
      }
    }
    return -1;
  }

  approval($event: ApprovalResult) {
    this.waitApproval = false;
    this.approvalAction.emit($event);
  }

  private refreshDayLines(): void {
    const lines: Record<number, string> = {};
    let previous: string | undefined;
    let first = true;
    this.messages.forEach((message, index) => {
      const time = (message as {time?: number}).time;
      if (!time) {
        return;
      }
      const day = new Date(time).toDateString();
      if (!first && day !== previous) {
        lines[index] = this.dayLabel(time);
      }
      previous = day;
      first = false;
    });
    this.dayLines = lines;
    clearTimeout(this.dayTimer);
    const midnight = new Date();
    midnight.setHours(24, 0, 1, 0);
    this.dayTimer = setTimeout(() => this.refreshDayLines(), midnight.getTime() - Date.now());
  }

  private dayLabel(time: number): string {
    const date = new Date(time);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const day = new Date(date);
    day.setHours(0, 0, 0, 0);
    const days = Math.round((today.getTime() - day.getTime()) / 86_400_000);
    if (days === 0) {
      return this.translate.instant('ai-assistant.today');
    }
    if (days === 1) {
      return this.translate.instant('ai-assistant.yesterday');
    }
    const options: Intl.DateTimeFormatOptions = days > 1 && days <= 6
      ? {weekday: 'long', day: 'numeric', month: 'long'}
      : date.getFullYear() === today.getFullYear() ? {day: 'numeric', month: 'long'} : {day: 'numeric', month: 'long', year: 'numeric'};
    return new Intl.DateTimeFormat(this.translate.currentLang?.replace('_', '-') || undefined, options).format(date).replace(',', '');
  }

  private findSummaries(): Record<number, EntityId[]> {
    const summaries: Record<number, EntityId[]> = {};
    let entities: EntityId[] = [];
    let seen = new Set<string>();
    let lastAi = -1;
    const close = (finished: boolean) => {
      if (finished && lastAi >= 0 && entities.length) {
        summaries[lastAi] = entities;
      }
      entities = [];
      seen = new Set<string>();
      lastAi = -1;
    };
    const add = (ids?: EntityId[]) => (ids ?? []).forEach(id => {
      const key = `${id.entityType}:${id.id}`;
      if (!seen.has(key)) {
        seen.add(key);
        entities.push(id);
      }
    });
    this.messages.forEach((message, index) => {
      switch (message.from) {
        case 'USER':
          close(true);
          break;
        case 'APPROVAL':
          add((this.completedExecutions[message.data.executionId] ?? message.result)?.affectedEntities);
          break;
        case 'ACTION':
          if (!SOLUTION_STEP_TOOLS.includes(message.data.tool)) {
            add(message.data.affectedEntities);
          }
          break;
        case 'AI':
          lastAi = index;
          break;
      }
    });
    close(!this.loading);
    return summaries;
  }

  isListedAction(message: ChatMessage): boolean {
    return message.from === 'ACTION' && !isKitCardTool(message.data.tool) && !isHiddenAiAction(message.data.tool);
  }

  answerQuestions(answers: AiQuestionAnswer[]): void {
    this.answer.emit(aiAnswersText(answers));
  }

  get retryPrompt(): string {
    return this.translate.instant('ai-assistant.action.try-again-prompt');
  }

  stoppedText(index: number, content: string): string {
    const steps = this.stoppedSteps[index];
    if (steps === undefined) {
      return content;
    }
    return steps ? this.translate.instant('ai-assistant.stopped-after', {count: steps}) : this.translate.instant('ai-assistant.stopped-before');
  }

  private findRetry(): number {
    if (this.loading) {
      return -1;
    }
    for (let i = this.messages.length - 1; i >= 0; i--) {
      const message = this.messages[i];
      if (message.from === 'USER') {
        return -1;
      }
      if (message.from === 'ACTION' && this.isListedAction(message)) {
        return message.data.status === 'FAILURE' ? i : -1;
      }
    }
    return -1;
  }

  private findStoppedSteps(): Record<number, number> {
    const steps: Record<number, number> = {};
    let done = 0;
    this.messages.forEach((message, index) => {
      if (message.from === 'USER') {
        done = 0;
      } else if (message.from === 'ACTION' && this.isListedAction(message) && message.data.status === 'SUCCESS') {
        done++;
      } else if (message.from === 'APPROVAL' && (this.completedExecutions[message.data.executionId] ?? message.result)?.executionStatus === 'SUCCESS') {
        done++;
      } else if ((message as GeneralChatMessage).status === 'STOPPED') {
        steps[index] = done;
      }
    });
    return steps;
  }

  private findAsking(): {index: number; questions: AiQuestion[]} | null {
    if (this.loading) {
      return null;
    }
    for (let i = this.messages.length - 1; i >= 0; i--) {
      const message = this.messages[i];
      if (message.from === 'USER') {
        return null;
      }
      if (message.from === 'ACTION' && message.data.tool === ASK_USER_TOOL) {
        const questions = aiQuestionsOf(message.data.arguments);
        return questions.length ? {index: i, questions} : null;
      }
    }
    return null;
  }

  private findSources(): Record<number, AiSource[]> {
    const sources: Record<number, AiSource[]> = {};
    let answer = -1;
    let pending: AiSource[] = [];
    const place = () => {
      if (pending.length && answer >= 0) {
        sources[answer] = [...(sources[answer] ?? []), ...pending];
        pending = [];
      }
    };
    this.messages.forEach((message, index) => {
      if (message.from === 'USER') {
        place();
        answer = -1;
        pending = [];
      } else if (message.from === 'AI' && !message.status) {
        answer = index;
        place();
      } else if (message.from === 'ACTION' && message.data.tool === CITE_SOURCES_TOOL && message.data.status !== 'FAILURE') {
        pending.push(...aiSourcesOf(message.data.arguments));
        place();
      }
    });
    return sources;
  }

  private findFiles(): Record<number, AiFile> {
    const files: Record<number, AiFile> = {};
    this.messages.forEach((message, index) => {
      if (message.from === 'ACTION' && message.data.tool === WRITE_FILE_TOOL) {
        const file = aiFileOf(message.data.executionId, message.data.arguments);
        if (file) {
          files[index] = file;
        }
      }
    });
    return files;
  }

  private findPlans(): Record<number, {steps: AiPlanStep[]; progress: AiPlanProgress | null; gate: boolean}> {
    const plans: Record<number, {steps: AiPlanStep[]; progress: AiPlanProgress | null; gate: boolean}> = {};
    let current = -1;
    this.messages.forEach((message, index) => {
      if (message.from !== 'ACTION') {
        return;
      }
      if (message.data.tool === PROPOSE_PLAN_TOOL) {
        const steps = aiPlanOf(message.data.arguments);
        if (steps.length) {
          plans[index] = {steps, progress: null, gate: false};
          current = index;
        }
      } else if (message.data.tool === UPDATE_PLAN_TOOL && current >= 0) {
        plans[current].progress = aiPlanProgressOf(message.data.arguments);
      }
    });
    if (current >= 0 && !this.loading && !plans[current].progress
      && !this.messages.slice(current + 1).some(message => message.from === 'USER')) {
      plans[current].gate = true;
    }
    return plans;
  }

  private findAnswers(): Record<number, AiQuestionAnswer[]> {
    const answers: Record<number, AiQuestionAnswer[]> = {};
    let questions: AiQuestion[] = [];
    this.messages.forEach((message, index) => {
      if (message.from === 'ACTION' && message.data.tool === ASK_USER_TOOL) {
        questions = aiQuestionsOf(message.data.arguments);
      } else if (message.from === 'USER') {
        const found = questions.length ? aiAnswersOf(message.content, questions) : null;
        if (found) {
          answers[index] = found;
        }
        questions = [];
      }
    });
    return answers;
  }

  private findPrompts(): {index: number; items: AiPrompt[]} | null {
    if (this.loading) {
      return null;
    }
    let items: AiPrompt[] = [];
    let answer = -1;
    for (let i = this.messages.length - 1; i >= 0; i--) {
      const message = this.messages[i];
      if (message.from === 'USER') {
        break;
      }
      if (message.from === 'ACTION' && message.data.tool === SUGGEST_PROMPTS_TOOL && !items.length) {
        items = aiPromptsOf(message.data.arguments);
      }
      if (message.from === 'AI' && !message.status && answer < 0) {
        answer = i;
      }
    }
    return items.length && answer >= 0 ? {index: answer, items} : null;
  }

  private findLastAnswerIndex(): number {
    for (let i = this.messages.length - 1; i >= 0; i--) {
      const message = this.messages[i];
      if (message.from === 'AI' && !message.status) {
        return i;
      }
    }
    return -1;
  }

  ngOnDestroy(): void {
    clearTimeout(this.copiedTimeout);
    clearTimeout(this.dayTimer);
    this.releaseBottom?.();
  }

  private pinToBottom(): void {
    const scroller = this.scroller;
    const stick = () => scroller.scrollTop = scroller.scrollHeight;
    const observer = new ResizeObserver(stick);
    observer.observe(this.elementRef.nativeElement);
    const release = () => {
      observer.disconnect();
      clearTimeout(timeout);
      scroller.removeEventListener('wheel', release);
      scroller.removeEventListener('touchstart', release);
      scroller.removeEventListener('keydown', release);
      this.releaseBottom = undefined;
    };
    const timeout = setTimeout(release, AiChatMessagesComponent.PIN_TO_BOTTOM_MS);
    scroller.addEventListener('wheel', release, {passive: true});
    scroller.addEventListener('touchstart', release, {passive: true});
    scroller.addEventListener('keydown', release);
    this.releaseBottom = release;
    stick();
  }

  scrollToBottom(behavior: ScrollBehavior = 'smooth'): void {
    const el = this.scroller;
    el.scrollTo({top: el.scrollHeight, behavior: this.motion(behavior)});
  }

  scrollToLastUserMessage(): void {
    const userMessages = this.elementRef.nativeElement.querySelectorAll('.user');
    if (userMessages.length) {
      userMessages[userMessages.length - 1].scrollIntoView({block: 'start', behavior: this.motion('smooth')});
    }
  }

  private scrollToLastAiMessage(): void {
    const aiMessages = this.elementRef.nativeElement.querySelectorAll('.ai');
    if (aiMessages.length) {
      aiMessages[aiMessages.length - 1].scrollIntoView({block: 'start', behavior: this.motion('smooth')});
    }
  }

  parseResult(msg: string): string {
    if (this.isValidJson(msg)) {
      const parsedMsg = JSON.parse(msg);
      if (Array.isArray(parsedMsg)) {
        const dashboardList = parsedMsg.map((item) => {
          return '\n' +
            '#### 📊️ ' + item.name + '\n' +
            '*Assigned to: **' + item.assignedTo + '***\n\n' +
            item.description + '\n';
        });
        return dashboardList.join('---\n');
      } else {
        return '```json\n' +
          msg +
          '{:copy-code}\n' +
          '```';
      }
    } else {
      return msg;
    }
  }

  private motion(behavior: ScrollBehavior): ScrollBehavior {
    return behavior === 'smooth' && matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : behavior;
  }

  private get scroller(): HTMLElement {
    return this.scrollContainer ?? this.elementRef.nativeElement;
  }

  private isNearBottom(threshold = 150): boolean {
    const el = this.scroller;
    return el.scrollHeight - el.scrollTop - el.clientHeight <= threshold;
  }

  private isValidJson(str: string): boolean {
    try {
      const result = JSON.parse(str);
      return (typeof result === 'object' && result !== null);
    } catch (e) {
      return false;
    }
  }
}
