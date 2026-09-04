// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EditorOptions } from 'hugerte';

export const HUGERTE_ASSETS_PATH = 'assets/hugerte';

export const HUGERTE_BODY_ID = 'hugerte';

export function defaultHugeRteOptions(overrides: Partial<EditorOptions>): Partial<EditorOptions> {
  return {
    base_url: '/' + HUGERTE_ASSETS_PATH,
    suffix: '.min',
    body_id: HUGERTE_BODY_ID,
    autofocus: false,
    branding: false,
    relative_urls: false,
    urlconverter_callback: (url) => url,
    ...overrides
  };
}
