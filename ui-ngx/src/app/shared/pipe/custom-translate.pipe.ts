///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Pipe, PipeTransform } from '@angular/core';
import { i18nPrefix, i18nRegExp } from '@shared/models/constants';
import { TranslateService } from '@ngx-translate/core';
import { isString } from '@core/utils';

@Pipe({
  name: 'customTranslate'
})
export class CustomTranslatePipe implements PipeTransform {

  constructor(private translate: TranslateService) {}
  public transform(text: string, def?: any): string {
    if (text && isString(text)) {
      if (text.includes(`{${i18nPrefix}`)) {
        const matches = text.match(i18nRegExp);
        let result = text;
        for (const match of matches) {
          const translationId = match.substring(6, match.length - 1);
          result = result.replace(match, this.doTranslate(translationId, def || match));
        }
        return result;
      }
    }
    return text;
  }

  private doTranslate(translationValue: string, defaultValue: string): string {
    const translation = this.translate.instant(translationValue);
    return translation !== translationValue ? translation : defaultValue;
  }

}
