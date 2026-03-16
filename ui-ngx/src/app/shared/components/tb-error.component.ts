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

import { ChangeDetectorRef, Component, Input } from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-error',
    template: `
    <div [@animation]="state" [style.margin-top]="noMargin ? '0' : '0.5rem'" style="font-size: .75rem">
      <mat-error>
        {{message}}
      </mat-error>
    </div>
  `,
    styles: [`
    :host {
        height: 24px;
    }
  `],
    animations: [
        trigger('animation', [
            state('show', style({
                opacity: 1,
                transform: 'translateY(0)'
            })),
            state('hide', style({
                opacity: 0,
                transform: 'translateY(-1rem)'
            })),
            transition('* <=> *', animate('200ms ease-out'))
        ]),
    ],
    standalone: false
})
export class TbErrorComponent {
  errorValue: string;
  state = 'hide';
  message: string;

  constructor(private cd: ChangeDetectorRef) {
  }

  @Input()
  @coerceBoolean()
  noMargin = false;

  @Input()
  set error(value) {
    if (this.errorValue !== value) {
      this.errorValue = value;
      if (value) {
        this.message = value;
      }
      this.state = value ? 'show' : 'hide';
      this.cd.markForCheck();
    }
  }
}
