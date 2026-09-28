// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
export interface TranslationInfo {
  localeCode: string;
  language: string;
  country: string;
  progress: number;
  customized: boolean;
}

export enum CustomTranslationState {
  Translated = 'T',
  Untranslated = 'U',
  Customized = 'C',
  Added = 'A'
}

export interface CustomTranslationEditData {
  [s: string]: CustomTranslationData;
}

export interface CustomTranslationData {
  t?: string; //translated
  o: string; //original
  p?: string; //parent
  s: CustomTranslationState; //state
}

export interface CustomTranslationEditInfo extends CustomTranslationData {
  k: string; //key
  edit?: boolean;
  value?: string;
}
