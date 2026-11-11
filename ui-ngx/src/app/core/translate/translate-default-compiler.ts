// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  MESSAGE_FORMAT_CONFIG,
  MessageFormatConfig,
  TranslateMessageFormatCompiler
} from 'ngx-translate-messageformat-compiler';
import { Inject, Injectable, Optional } from '@angular/core';
import { parse } from '@messageformat/parser';

@Injectable({ providedIn: 'root' })
export class TranslateDefaultCompiler extends TranslateMessageFormatCompiler {

  constructor(
    @Optional()
    @Inject(MESSAGE_FORMAT_CONFIG)
      config?: MessageFormatConfig
  ) {
    super(config);
  }

  public compile(value: string, lang: string): any {
    return this.defaultCompile(value, lang);
  }

  public compileTranslations(translations: any, lang: string): any {
    return this.defaultCompile(translations, lang);
  }

  private defaultCompile(src: any, lang: string): any {
    if (typeof src !== 'object') {
      if (this.checkIsPlural(src)) {
        try {
          return super.compile(src, lang.replace('_', '-'));
        } catch (e) {
          console.warn('Failed compile translate:', src, e);
          return src;
        }
      } else {
        return src;
      }
    } else {
      const result = {};
      for (const key of Object.keys(src)) {
        result[key] = this.defaultCompile(src[key], lang);
      }
      return result;
    }
  }

  private checkIsPlural(src: string): boolean {
    let tokens: any[];
    try {
      tokens = parse(src.replace(/\{\{/g, '{').replace(/\}\}/g, '}'),
        {cardinal: [], ordinal: []});
    } catch (e) {
      console.warn(`Failed to parse source: ${src}`);
      console.error(e);
      return false;
    }
    // Route both plural and select expressions through the MessageFormat compiler; otherwise a select
    // like { type, select, ... } is returned verbatim and its {name} arguments never get interpolated.
    const res = tokens.filter(
      (value) => typeof value !== 'string'
        && (value.type === 'plural' || value.type === 'select' || value.type === 'selectordinal')
    );
    return res.length > 0;
  }

}
