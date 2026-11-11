// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
export interface TbColor {
  light: string;
  dark: string;
}

export interface TbColorScheme {
  [key: string]: TbColor;
}
