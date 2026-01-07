///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
  styleUrls: ['./help-markdown.component.scss']
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
