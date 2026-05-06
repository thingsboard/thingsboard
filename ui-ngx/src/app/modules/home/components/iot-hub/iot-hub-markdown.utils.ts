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

export const ITEM_LINK_PLACEHOLDER_REGEX =
  /\$\{item-link:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\}/g;

export const ITEM_LINK_KEY_REGEX =
  /^item-link:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$/;

export function itemLinkCardTag(itemId: string): string {
  return `<tb-iot-hub-item-link-card itemId="${itemId}"></tb-iot-hub-item-link-card>`;
}

export function replaceItemLinkPlaceholders(markdown: string): string {
  if (!markdown) {
    return markdown;
  }
  return markdown.replace(ITEM_LINK_PLACEHOLDER_REGEX, (_match, uuid) => itemLinkCardTag(uuid));
}
