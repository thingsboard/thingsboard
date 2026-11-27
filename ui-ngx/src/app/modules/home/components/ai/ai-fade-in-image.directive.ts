// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Directive } from '@angular/core';

@Directive({
  selector: 'img[tbAiFadeIn]',
  standalone: false,
  host: {
    class: 'tb-ai-fade-in',
    '[class.loaded]': 'loaded',
    '(load)': 'loaded = true',
    '(error)': 'loaded = true'
  }
})
export class AiFadeInImageDirective {

  loaded = false;

}
