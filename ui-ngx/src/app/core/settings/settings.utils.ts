// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { environment as env } from '@env/environment';
import { TranslateService, TranslateStore } from '@ngx-translate/core';
import { mergeMap } from 'rxjs/operators';
import _moment from 'moment';
import { Observable } from 'rxjs';

export function updateUserLang(translate: TranslateService, translateStore: TranslateStore, document: Document, userLang: string,
                               translations = env.supportedLangs, reload = false): Observable<any> {
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
  if (reload) {
    translateStore.addLanguages(translations);
    if (translateStore.hasTranslationFor(detectedSupportedLang)) {
      return translate.currentLoader.getTranslation(detectedSupportedLang).pipe(
        mergeMap((value) => {
          translate.setTranslation(detectedSupportedLang, value, true);
          if (translate.getCurrentLang() !== detectedSupportedLang) {
            const currentLanguage = translate.getCurrentLang();
            translate.currentLoader.getTranslation(currentLanguage).subscribe(currentLangValue => {
              translate.setTranslation(currentLanguage, currentLangValue, true);
            });
          }
          return translate.use(detectedSupportedLang);
        })
      );
    } else {
      return translate.use(detectedSupportedLang);
    }
  } else {
    if (detectedSupportedLang === env.defaultLang && translateStore.hasTranslationFor(detectedSupportedLang)) {
      return translate.currentLoader.getTranslation(detectedSupportedLang).pipe(
        mergeMap((value) => {
          translate.setTranslation(detectedSupportedLang, value, true);
          return translate.use(detectedSupportedLang);
        })
      );
    } else {
      return translate.use(detectedSupportedLang);
    }
  }
}

function detectSupportedLang(targetLang: string, translations: string[]): string {
  const langTag = (targetLang || '').split('-').join('_');
  if (langTag.length) {
    if (translations.indexOf(langTag) > -1) {
      return langTag;
    } else {
      const parts = langTag.split('_');
      let lang: string;
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
