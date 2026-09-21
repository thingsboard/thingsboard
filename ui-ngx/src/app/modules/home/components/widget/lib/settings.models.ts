// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export type FontStyle = 'normal' | 'italic' | 'oblique';
export type FontWeight = 'normal' | 'bold' | 'bolder' | 'lighter'
                         | '100' | '200' | '300' | '400' | '500'
                         | '600' | '700' | '800' | '900';

export interface FontSettings {
  family?: string;
  size?: number;
  style?: FontStyle;
  weight?: FontWeight;
  color?: string;
  shadowColor?: string;
}

export function getFontFamily(fontSettings: FontSettings): string {
  let family = fontSettings && fontSettings.family ? fontSettings.family : 'Roboto';
  if (family === 'RobotoDraft') {
    family = 'Roboto';
  }
  return family;
}

export function prepareFontSettings(fontSettings: FontSettings, defaultFontSettings: FontSettings): FontSettings {
  const result = {...defaultFontSettings, ...(fontSettings || {})};
  result.family = getFontFamily(result);
  return result;
}
