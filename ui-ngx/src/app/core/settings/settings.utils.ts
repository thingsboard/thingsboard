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

import { environment as env } from '@env/environment';
import { TranslateService } from '@ngx-translate/core';
import * as _moment from 'moment';
// Supported Langs: cs_CZ,de_DE,el_GR,en_US,es_ES,fa_IR,fr_FR,it_IT,ja_JA,ka_GE,ko_KR,lv_LV,pt_BR,ro_RO,ru_RU,
// sl_SI,tr_TR,uk_UA,zh_CN,zh_TW
import 'moment/locale/cs';
import 'moment/locale/de';
import 'moment/locale/el';
import 'moment/locale/es';
import 'moment/locale/fa';
import 'moment/locale/fr';
import 'moment/locale/it';
import 'moment/locale/ja';
import 'moment/locale/ka';
import 'moment/locale/ko';
import 'moment/locale/lv';
import 'moment/locale/pt';
import 'moment/locale/ro';
import 'moment/locale/ru';
import 'moment/locale/sl';
import 'moment/locale/tr';
import 'moment/locale/uk';
import 'moment/locale/zh-cn';
import 'moment/locale/zh-tw';

export function updateUserLang(translate: TranslateService, userLang: string) {
  let targetLang = userLang;
  if (!env.production) {
    console.log(`User lang: ${targetLang}`);
  }
  if (!targetLang) {
    targetLang = translate.getBrowserCultureLang();
    if (!env.production) {
      console.log(`Fallback to browser lang: ${targetLang}`);
    }
  }
  const detectedSupportedLang = detectSupportedLang(targetLang);
  if (!env.production) {
    console.log(`Detected supported lang: ${detectedSupportedLang}`);
  }
  translate.use(detectedSupportedLang);
  _moment.locale([detectedSupportedLang]);
}

function detectSupportedLang(targetLang: string): string {
  const langTag = (targetLang || '').split('-').join('_');
  if (langTag.length) {
    if (env.supportedLangs.indexOf(langTag) > -1) {
      return langTag;
    } else {
      const parts = langTag.split('_');
      let lang;
      if (parts.length === 2) {
        lang = parts[0];
      } else {
        lang = langTag;
      }
      const foundLangs = env.supportedLangs.filter(
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
