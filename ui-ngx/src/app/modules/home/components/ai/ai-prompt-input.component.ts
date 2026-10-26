// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  EventEmitter,
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
import { CdkTextareaAutosize } from '@angular/cdk/text-field';
import { toSignal } from '@angular/core/rxjs-interop';
import { of, Subscription } from 'rxjs';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer } from '@angular/platform-browser';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { guid, isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { AiResourceService } from '@core/http/ai-resource.service';
import { AiPromptMessage, AiResourceConfig } from '@shared/models/ai-chat.models';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';
import {
  AiAttachmentItem,
  fitAiImage,
  isAiAttachmentAccepted,
  namePastedImage
} from '@home/components/ai/ai-attachment.models';

interface PendingAttachment extends AiAttachmentItem {
  previewUrl?: string;
  upload?: Subscription;
}

@Component({
  selector: 'tb-ai-prompt-input',
  templateUrl: './ai-prompt-input.component.html',
  styleUrls: ['./ai-prompt-input.component.scss'],
  standalone: false
})
export class AiPromptInputComponent implements OnChanges, AfterViewInit, OnDestroy {

  private aiResourceService = inject(AiResourceService);
  private store = inject<Store<AppState>>(Store);
  private translate = inject(TranslateService);
  private sanitizer = inject(DomSanitizer);
  private renderer = inject(Renderer2);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private fileSize = new FileSizePipe();

  @Input() placeholder = '';

  @Input() loading: boolean;

  @Input() toastTarget: string;

  @Input() dropZone: HTMLElement;

  @Output() messageSent = new EventEmitter<AiPromptMessage>();

  @ViewChild('autosize') autosize!: CdkTextareaAutosize;

  @ViewChild('textarea') textarea!: ElementRef<HTMLTextAreaElement>;

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  message = new FormControl<string>('');

  config = toSignal(this.aiResourceService.getConfig(), {initialValue: null as AiResourceConfig | null});

  attachments = signal<PendingAttachment[]>([]);

  attachEnabled = computed(() => !!this.config());

  accept = computed(() => this.config()?.extensions.join(',') ?? '');

  uploading = computed(() => this.attachments().some(attachment => attachment.status === 'uploading'));

  private dropTarget: HTMLElement;
  private dropOverlay: HTMLElement;
  private dragDepth = 0;
  private unlisten: Array<() => void> = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.dropZone) {
      this.listenForDrops(this.dropZone ?? this.host.nativeElement);
    }
  }

  ngAfterViewInit(): void {
    if (!this.dropTarget) {
      this.listenForDrops(this.host.nativeElement);
    }
  }

  ngOnDestroy(): void {
    this.stopListening();
    this.clearAttachments(true);
  }

  onEnter(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!ke.shiftKey) {
      ke.preventDefault();
      this.send();
    }
  }

  send(): void {
    const msg = this.message.value?.trim() ?? '';
    const ready = this.attachments().filter(attachment => attachment.status === 'ready');
    if (this.loading || this.uploading() || (!msg && !ready.length)) {
      return;
    }
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
    setTimeout(() => {
      this.textarea?.nativeElement.focus();
      this.autosize?.resizeToFitContent(true);
    });
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

  removeAttachment(item: AiAttachmentItem): void {
    const attachment = this.attachments().find(candidate => candidate.key === item.key);
    if (!attachment) {
      return;
    }
    this.discard(attachment);
    this.attachments.update(list => list.filter(candidate => candidate !== attachment));
  }

  async addFiles(files: File[]): Promise<void> {
    const config = this.config();
    if (!config || !files.length) {
      return;
    }
    for (const original of files) {
      if (this.attachments().length >= config.maxPerMessage) {
        this.showError(this.translate.instant('ai-assistant.attachments-limit', {count: config.maxPerMessage}));
        return;
      }
      if (!isAiAttachmentAccepted(original, config.extensions)) {
        this.showError(this.translate.instant('ai-assistant.attachment-not-supported', {name: original.name}));
        continue;
      }
      const file = await fitAiImage(original, config.maxSize);
      if (file.size > config.maxSize) {
        this.showError(this.translate.instant('ai-assistant.attachment-too-large',
          {name: original.name, size: this.fileSize.transform(config.maxSize)}));
        continue;
      }
      this.startUpload(file, config);
    }
  }

  private startUpload(file: File, config: AiResourceConfig): void {
    const image = ['image/png', 'image/jpeg', 'image/webp'].includes(file.type);
    const previewUrl = image ? URL.createObjectURL(file) : undefined;
    const attachment: PendingAttachment = {
      key: guid(),
      name: file.name,
      mediaType: file.type || config.mediaTypes[0],
      size: file.size,
      image,
      previewUrl,
      url$: previewUrl ? of(previewUrl) : undefined,
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
      error: error => {
        this.patch(attachment.key, {status: 'error'});
        this.showError(this.errorMessage(error, file.name));
      }
    });
    this.patch(attachment.key, {upload});
  }

  private patch(key: string, changes: Partial<PendingAttachment>): void {
    this.attachments.update(list => list.map(attachment => attachment.key === key ? {...attachment, ...changes} : attachment));
  }

  private clearText(): void {
    this.message.reset('');
    // Not autosize.reset(): it restores a height captured before the placeholder was known.
    this.autosize?.resizeToFitContent(true);
  }

  private clearAttachments(discard: boolean): void {
    const attachments = untracked(() => this.attachments());
    if (!attachments.length) {
      return;
    }
    if (discard) {
      attachments.forEach(attachment => this.discard(attachment));
    }
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

  private errorMessage(error: any, name: string): string {
    const responseType = isNotEmptyStr(error?.error) ? 'text' : undefined;
    const message = error ? parseHttpErrorMessage(error, this.translate, responseType, this.sanitizer).message : null;
    return message || this.translate.instant('ai-assistant.attachment-upload-failed', {name});
  }

  private showError(message: string): void {
    this.store.dispatch(new ActionNotificationShow({
      message,
      target: this.toastTarget,
      modern: true,
      type: 'error',
      verticalPosition: 'bottom',
      horizontalPosition: 'center'
    }));
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
          this.showDropOverlay();
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
          this.hideDropOverlay();
        }
      }),
      this.renderer.listen(target, 'drop', (event: DragEvent) => {
        if (!hasFiles(event) || !this.attachEnabled()) {
          return;
        }
        event.preventDefault();
        this.dragDepth = 0;
        this.hideDropOverlay();
        this.addFiles(Array.from(event.dataTransfer.files ?? []));
      })
    ];
  }

  private stopListening(): void {
    this.unlisten.forEach(unlisten => unlisten());
    this.unlisten = [];
    this.dragDepth = 0;
    this.hideDropOverlay();
  }

  private showDropOverlay(): void {
    if (this.dropOverlay || !this.dropTarget) {
      return;
    }
    const overlay: HTMLElement = this.renderer.createElement('div');
    overlay.className = 'tb-ai-drop-overlay';
    Object.assign(overlay.style, {
      position: 'absolute', inset: '8px', zIndex: '20', display: 'flex', flexDirection: 'column', gap: '8px',
      alignItems: 'center', justifyContent: 'center', borderRadius: '12px', pointerEvents: 'none',
      border: '2px dashed var(--tb-primary-500)', background: 'rgba(255, 255, 255, 0.92)',
      color: 'var(--tb-primary-500)', fontSize: '16px', fontWeight: '500', textAlign: 'center', padding: '16px'
    });
    const icon: HTMLElement = this.renderer.createElement('span');
    icon.className = 'material-icons';
    icon.style.fontSize = '40px';
    icon.textContent = 'upload_file';
    const title: HTMLElement = this.renderer.createElement('span');
    title.textContent = this.translate.instant('ai-assistant.drop-files');
    const hint: HTMLElement = this.renderer.createElement('span');
    Object.assign(hint.style, {fontSize: '13px', fontWeight: '400', color: 'rgba(0, 0, 0, 0.54)'});
    hint.textContent = this.translate.instant('ai-assistant.attachments-hint',
      {size: this.fileSize.transform(this.config()?.maxSize ?? 0)});
    overlay.append(icon, title, hint);
    if (getComputedStyle(this.dropTarget).position === 'static') {
      this.renderer.setStyle(this.dropTarget, 'position', 'relative');
    }
    this.renderer.appendChild(this.dropTarget, overlay);
    this.dropOverlay = overlay;
  }

  private hideDropOverlay(): void {
    if (this.dropOverlay) {
      this.dropOverlay.remove();
      this.dropOverlay = null;
    }
  }

}
