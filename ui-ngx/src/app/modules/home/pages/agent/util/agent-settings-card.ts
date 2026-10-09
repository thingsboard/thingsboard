// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
/**
 * The shared entity-details-page wraps an agent detail component in a `.settings-card` capped at
 * 60–80% width, which is too narrow for compose editing. The card is marked so a component-scoped
 * override can widen it, and unmarked on destroy.
 *
 * The lookup walks this component's own ancestors only: a page can hold more than one
 * `.settings-card`, so falling back to a document-wide search risks widening an unrelated card and
 * leaving the class stuck on it.
 */
export function markSettingsCardFullscreen(host: HTMLElement, cssClass: string): HTMLElement | null {
  const card = host.closest('.settings-card') as HTMLElement | null;
  if (card) {
    card.classList.add(cssClass);
  }
  return card;
}

export function clearSettingsCardFullscreen(card: HTMLElement | null, cssClass: string): void {
  if (card) {
    card.classList.remove(cssClass);
  }
}
