// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
