// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  Renderer2,
  signal,
  SimpleChanges,
  untracked,
  ViewChild
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer } from '@angular/platform-browser';
import { guid, isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { AiResourceService } from '@core/http/ai-resource.service';
import { AiPromptMessage, AiResourceConfig, AiResourceInfo } from '@shared/models/ai-chat.models';
import {
  aiAttachmentKind,
  fitAiImage,
  formatAiFileSize,
  isAiAttachmentAccepted,
  namePastedImage
} from '@home/components/ai/ai-attachment.models';

interface PendingAttachment {
  key: string;
  name: string;
  size: number;
  icon: string;
  kind: string;
  status: 'uploading' | 'ready' | 'error';
  progress: number;
  reason?: string;
  previewUrl?: string;
  resource?: AiResourceInfo;
  upload?: Subscription;
}

const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp'];
const FLY_DURATION = 520;

@Component({
  selector: 'tb-ai-prompt-input',
  templateUrl: './ai-prompt-input.component.html',
  styleUrls: ['./ai-prompt-input.component.scss'],
  standalone: false
})
export class AiPromptInputComponent implements OnChanges, AfterViewInit, OnDestroy {

  private aiResourceService = inject(AiResourceService);
  private translate = inject(TranslateService);
  private sanitizer = inject(DomSanitizer);
  private renderer = inject(Renderer2);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  @Input() placeholder = '';

  @Input() loading: boolean;

  @Input() docked = false;

  @Input() dropZone: HTMLElement;

  @Input() showAutoApprove = false;

  @Input() autoApprove = false;

  @Input() autoApproveInChat = true;

  @Output() messageSent = new EventEmitter<AiPromptMessage>();

  @Output() autoApproveChange = new EventEmitter<boolean>();

  @Input() stoppable = false;

  @Input() stopping = false;

  @Output() stopped = new EventEmitter<void>();

  get canStop(): boolean {
    return this.loading && this.stoppable;
  }

  stop(): void {
    if (this.canStop && !this.stopping) {
      this.stopped.emit();
    }
  }

  @ViewChild('textarea') textarea!: ElementRef<HTMLTextAreaElement>;

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  message = new FormControl<string>('');

  config = toSignal(this.aiResourceService.getConfig(), {initialValue: null as AiResourceConfig | null});

  attachments = signal<PendingAttachment[]>([]);

  attachEnabled = computed(() => !!this.config());

  accept = computed(() => this.config()?.extensions.join(',') ?? '');

  maxPerMessage = computed(() => this.config()?.maxPerMessage ?? 0);

  maxSize = computed(() => formatAiFileSize(this.config()?.maxSize ?? 0));

  uploading = computed(() => this.attachments().some(attachment => attachment.status === 'uploading'));

  hasText = signal(false);

  dragging = signal(false);

  flying = signal(false);

  private dropTarget: HTMLElement;
  private dragDepth = 0;
  private unlisten: Array<() => void> = [];
  private flyTimer: ReturnType<typeof setTimeout>;

  get canSend(): boolean {
    const ready = this.attachments().some(attachment => attachment.status === 'ready');
    return !this.loading && !this.uploading() && (!!this.message.value?.trim() || ready);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.dropZone) {
      this.listenForDrops(this.dropZone ?? this.host.nativeElement);
    }
  }

  ngAfterViewInit(): void {
    if (!this.dropTarget) {
      this.listenForDrops(this.host.nativeElement);
    }
    setTimeout(() => this.resize());
  }

  ngOnDestroy(): void {
    clearTimeout(this.flyTimer);
    this.stopListening();
    this.clearAttachments(true);
  }

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey) {
      return;
    }
    const target = event.target as HTMLElement;
    if (target?.closest?.('input, textarea, select, [contenteditable]')) {
      return;
    }
    const visible = Array.from(document.querySelectorAll<HTMLTextAreaElement>('.tb-ai-composer textarea'))
      .filter(textarea => textarea.offsetParent);
    const field = this.textarea?.nativeElement;
    if (field && visible[visible.length - 1] === field) {
      event.preventDefault();
      field.focus();
      field.selectionStart = field.selectionEnd = field.value.length;
    }
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.canStop) {
      event.preventDefault();
      this.stop();
      return;
    }
    if (event.key === 'Enter' && (!event.shiftKey || event.metaKey || event.ctrlKey)) {
      event.preventDefault();
      this.send();
    }
  }

  onInput(): void {
    this.hasText.set(!!this.message.value);
    this.resize();
  }

  send(): void {
    const msg = this.message.value?.trim() ?? '';
    const ready = this.attachments().filter(attachment => attachment.status === 'ready');
    if (this.loading || this.uploading() || (!msg && !ready.length)) {
      return;
    }
    this.fly();
    this.clearText();
    this.clearAttachments(false);
    this.messageSent.emit({message: msg, resources: ready.map(attachment => attachment.resource)});
  }

  reset(): void {
    this.clearText();
    this.clearAttachments(true);
  }

  focus(): void {
    this.textarea?.nativeElement.focus();
  }

  setValue(value: string): void {
    this.message.setValue(value);
    this.hasText.set(!!value);
    setTimeout(() => {
      this.textarea?.nativeElement.focus();
      this.resize();
    });
  }

  toggleAutoApprove(event: Event): void {
    this.autoApprove = (event.target as HTMLInputElement).checked;
    this.autoApproveChange.emit(this.autoApprove);
  }

  openFilePicker(): void {
    this.fileInput?.nativeElement.click();
  }

  onFilesPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(Array.from(input.files ?? []));
    input.value = '';
  }

  onPaste(event: ClipboardEvent): void {
    const files = Array.from(event.clipboardData?.files ?? []);
    if (files.length && this.attachEnabled()) {
      event.preventDefault();
      this.addFiles(files.map(namePastedImage));
    }
  }

  removeAttachment(attachment: PendingAttachment): void {
    this.discard(attachment);
    this.attachments.update(list => list.filter(candidate => candidate.key !== attachment.key));
  }

  meta(attachment: PendingAttachment): string {
    if (attachment.status === 'error') {
      return attachment.reason;
    }
    if (attachment.status === 'uploading') {
      return this.translate.instant('ai-assistant.attachment-uploading', {progress: attachment.progress});
    }
    return `${this.translate.instant(attachment.kind)} · ${formatAiFileSize(attachment.size)}`;
  }

  async addFiles(files: File[]): Promise<void> {
    const config = this.config();
    if (!config || !files.length) {
      return;
    }
    for (const original of files) {
      const accepted = this.attachments().filter(attachment => attachment.status !== 'error').length;
      if (accepted >= config.maxPerMessage) {
        this.refuse(original, this.translate.instant('ai-assistant.attachment-over-limit', {count: config.maxPerMessage}));
        continue;
      }
      if (!isAiAttachmentAccepted(original, config.extensions)) {
        this.refuse(original, this.translate.instant('ai-assistant.attachment-unsupported'));
        continue;
      }
      const file = await fitAiImage(original, config.maxSize);
      if (file.size > config.maxSize) {
        this.refuse(original, this.translate.instant('ai-assistant.attachment-larger-than', {size: formatAiFileSize(config.maxSize)}));
        continue;
      }
      this.startUpload(file, config);
    }
  }

  private refuse(file: File, reason: string): void {
    this.attachments.update(list => [...list, {
      ...this.describe(file, file.type),
      status: 'error',
      progress: 0,
      reason
    }]);
  }

  private describe(file: File, mediaType: string): Pick<PendingAttachment, 'key' | 'name' | 'size' | 'icon' | 'kind'> {
    const kind = aiAttachmentKind(mediaType, file.name);
    return {key: guid(), name: file.name, size: file.size, icon: kind.icon, kind: kind.label};
  }

  private startUpload(file: File, config: AiResourceConfig): void {
    const previewUrl = IMAGE_TYPES.includes(file.type) ? URL.createObjectURL(file) : undefined;
    const attachment: PendingAttachment = {
      ...this.describe(file, file.type || config.mediaTypes[0]),
      previewUrl,
      status: 'uploading',
      progress: 0
    };
    this.attachments.update(list => [...list, attachment]);
    const upload = this.aiResourceService.upload(file).subscribe({
      next: event => {
        if (event.type === 'progress') {
          this.patch(attachment.key, {progress: event.progress});
        } else {
          if (previewUrl) {
            this.aiResourceService.rememberUrl(event.resource.id, previewUrl);
          }
          this.patch(attachment.key, {status: 'ready', resource: event.resource, progress: 100});
        }
      },
      error: error => this.patch(attachment.key, {status: 'error', reason: this.errorMessage(error)})
    });
    this.patch(attachment.key, {upload});
  }

  private patch(key: string, changes: Partial<PendingAttachment>): void {
    this.attachments.update(list => list.map(attachment => attachment.key === key ? {...attachment, ...changes} : attachment));
  }

  private fly(): void {
    this.flying.set(false);
    clearTimeout(this.flyTimer);
    requestAnimationFrame(() => {
      this.flying.set(true);
      this.flyTimer = setTimeout(() => this.flying.set(false), FLY_DURATION);
    });
  }

  private resize(): void {
    const field = this.textarea?.nativeElement;
    if (!field) {
      return;
    }
    const cap = this.docked ? Math.min(160, Math.round(innerHeight * 0.3)) : Math.min(220, Math.round(innerHeight * 0.4));
    field.style.height = 'auto';
    field.style.height = Math.min(field.scrollHeight, cap) + 'px';
  }

  private clearText(): void {
    this.message.reset('');
    this.hasText.set(false);
    setTimeout(() => this.resize());
  }

  private clearAttachments(discard: boolean): void {
    const attachments = untracked(() => this.attachments());
    if (!attachments.length) {
      return;
    }
    attachments
      .filter(attachment => discard || attachment.status !== 'ready')
      .forEach(attachment => this.discard(attachment));
    this.attachments.set([]);
  }

  private discard(attachment: PendingAttachment): void {
    attachment.upload?.unsubscribe();
    if (attachment.resource) {
      this.aiResourceService.delete(attachment.resource.id).subscribe({error: () => {}});
    }
    if (attachment.previewUrl) {
      URL.revokeObjectURL(attachment.previewUrl);
    }
  }

  private errorMessage(error: any): string {
    const responseType = isNotEmptyStr(error?.error) ? 'text' : undefined;
    const message = error ? parseHttpErrorMessage(error, this.translate, responseType, this.sanitizer).message : null;
    return message || this.translate.instant('ai-assistant.attachment-failed');
  }

  private listenForDrops(target: HTMLElement): void {
    this.stopListening();
    this.dropTarget = target;
    const hasFiles = (event: DragEvent) => Array.from(event.dataTransfer?.types ?? []).includes('Files');
    this.unlisten = [
      this.renderer.listen(target, 'dragenter', (event: DragEvent) => {
        if (!hasFiles(event) || !this.attachEnabled()) {
          return;
        }
        event.preventDefault();
        if (this.dragDepth++ === 0) {
          this.dragging.set(true);
        }
      }),
      this.renderer.listen(target, 'dragover', (event: DragEvent) => {
        if (hasFiles(event) && this.attachEnabled()) {
          event.preventDefault();
          event.dataTransfer.dropEffect = 'copy';
        }
      }),
      this.renderer.listen(target, 'dragleave', (event: DragEvent) => {
        if (hasFiles(event) && this.dragDepth > 0 && --this.dragDepth === 0) {
          this.dragging.set(false);
        }
      }),
      this.renderer.listen(target, 'drop', (event: DragEvent) => {
        if (!hasFiles(event) || !this.attachEnabled()) {
          return;
        }
        event.preventDefault();
        this.dragDepth = 0;
        this.dragging.set(false);
        this.addFiles(Array.from(event.dataTransfer.files ?? []));
      })
    ];
  }

  private stopListening(): void {
    this.unlisten.forEach(unlisten => unlisten());
    this.unlisten = [];
    this.dragDepth = 0;
    this.dragging.set(false);
  }

}
