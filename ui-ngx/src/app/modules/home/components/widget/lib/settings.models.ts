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
