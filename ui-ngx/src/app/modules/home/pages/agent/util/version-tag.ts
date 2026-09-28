// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
export function versionTag(version: string | undefined | null): string {
  if (!version) {
    return '<span style="color:rgba(0,0,0,0.38);">—</span>';
  }
  return version;
}
