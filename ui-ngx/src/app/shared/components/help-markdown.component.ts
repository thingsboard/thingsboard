// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  Component,
  EventEmitter,
  Input, OnChanges,
  OnDestroy, OnInit,
  Output, SimpleChanges
} from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { share } from 'rxjs/operators';
import { HelpService } from '@core/services/help.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { base64toString } from '@core/utils';

@Component({
    selector: 'tb-help-markdown',
    templateUrl: './help-markdown.component.html',
    styleUrls: ['./help-markdown.component.scss'],
    standalone: false
})
export class HelpMarkdownComponent implements OnDestroy, OnInit, OnChanges {

  @Input() helpId: string;

  @Input() helpContent: string;

  @Input() helpContentBase64: string;

  @Input() asyncHelpContent: Observable<string>;

  @Input()
  @coerceBoolean()
  visible: boolean;

  @Input() style: { [klass: string]: any } = {};

  @Output() markdownReady = new EventEmitter<void>();

  markdownText = new BehaviorSubject<string>(null);

  markdownText$ = this.markdownText.pipe(
    share()
  );

  private loadHelpPending = false;

  constructor(private help: HelpService) {}

  ngOnInit(): void {
    this.loadHelpWhenVisible();
  }

  ngOnDestroy(): void {
    this.markdownText.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'visible') {
          if (this.loadHelpPending) {
            this.loadHelpPending = false;
            this.loadHelp();
          }
        }
        if (['helpId', 'helpContent', 'helpContentBase64', 'asyncHelpContent'].includes(propName)) {
          this.markdownText.next(null);
          this.loadHelpWhenVisible();
        }
      }
    }
  }

  private loadHelpWhenVisible() {
    if (this.visible) {
      this.loadHelp();
    } else {
      this.loadHelpPending = true;
    }
  }

  private loadHelp() {
    if (this.helpId) {
      this.help.getHelpContent(this.helpId).subscribe((content) => {
        this.markdownText.next(content);
      });
    } else if (this.helpContent) {
      this.markdownText.next(this.helpContent);
    } else if (this.helpContentBase64) {
      this.markdownText.next(base64toString(this.helpContentBase64));
    } else if (this.asyncHelpContent) {
      this.asyncHelpContent.subscribe((content) => {
        this.markdownText.next(content);
      });
    }
  }

  onMarkdownReady() {
    this.markdownReady.next();
  }

  markdownClick($event: MouseEvent) {
  }

}
