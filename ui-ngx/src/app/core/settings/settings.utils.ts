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

import { environment as env } from '@env/environment';
import { TranslateService } from '@ngx-translate/core';
import _moment from 'moment';
import { Observable } from 'rxjs';

export function updateUserLang(translate: TranslateService, document: Document, userLang: string, translations = env.supportedLangs): Observable<any> {
  let targetLang = userLang;
  if (!translations) {
    translations = env.supportedLangs;
  }
  if (!env.production) {
    console.log(`User lang: ${targetLang}`);
  }
  if (!targetLang) {
    targetLang = translate.getBrowserCultureLang();
    if (!env.production) {
      console.log(`Fallback to browser lang: ${targetLang}`);
    }
  }
  const detectedSupportedLang = detectSupportedLang(targetLang, translations);
  if (!env.production) {
    console.log(`Detected supported lang: ${detectedSupportedLang}`);
  }
  document.documentElement.lang = detectedSupportedLang.replace('_', '-');
  _moment.locale([detectedSupportedLang]);
  return translate.use(detectedSupportedLang);
}

function detectSupportedLang(targetLang: string, translations: string[]): string {
  const langTag = (targetLang || '').split('-').join('_');
  if (langTag.length) {
    if (translations.indexOf(langTag) > -1) {
      return langTag;
    } else {
      const parts = langTag.split('_');
      let lang;
      if (parts.length === 2) {
        lang = parts[0];
      } else {
        lang = langTag;
      }
      const foundLangs = translations.filter(
        (supportedLang: string) => {
          const supportedLangParts = supportedLang.split('_');
          return supportedLangParts[0] === lang;
        }
      );
      if (foundLangs.length) {
        return foundLangs[0];
      }
    }
  }
  return env.defaultLang;
}
