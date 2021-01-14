///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  MESSAGE_FORMAT_CONFIG,
  MessageFormatConfig,
  TranslateMessageFormatCompiler
} from 'ngx-translate-messageformat-compiler';
import { Inject, Injectable, Optional } from '@angular/core';
import messageFormatParser from 'messageformat-parser';

@Injectable({ providedIn: 'root' })
export class TranslateDefaultCompiler extends TranslateMessageFormatCompiler {

  constructor(
    @Optional()
    @Inject(MESSAGE_FORMAT_CONFIG)
      config?: MessageFormatConfig
  ) {
    super(config);
  }

  public compile(value: string, lang: string): (params: any) => string {
    return this.defaultCompile(value, lang);
  }

  public compileTranslations(translations: any, lang: string): any {
    return this.defaultCompile(translations, lang);
  }

  private defaultCompile(src: any, lang: string): any {
    if (typeof src !== 'object') {
      if (this.checkIsPlural(src)) {
        return super.compile(src, lang);
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
      tokens = messageFormatParser.parse(src.replace(/\{\{/g, '{').replace(/\}\}/g, '}'),
        {cardinal: [], ordinal: []});
    } catch (e) {
      console.warn(`Failed to parse source: ${src}`);
      console.error(e);
      return false;
    }
    const res = tokens.filter(
      (value) => typeof value !== 'string' && value.type === 'plural'
    );
    return res.length > 0;
  }

}
