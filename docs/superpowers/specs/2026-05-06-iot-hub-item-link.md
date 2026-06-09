# IoT Hub `${item-link:uuid}` Markdown Component — Design

**Date:** 2026-05-06
**Status:** Approved
**Branch:** `feature/iot-hub`

## Summary

Add an `${item-link:<uuid>}` markdown placeholder that renders a small card
(thumbnail + name + creator) for any IoT Hub marketplace item, modeled on
the cards already shown in the home-page search popup. Used by IoT Hub
content authors to cross-link items from readme and install instructions.

## Decisions

| Question | Decision |
|---|---|
| UUID identifies | Marketplace **item ID** (stable across versions) |
| Render scope | Item readme + device install instructions + solution install instructions |
| Click behavior | Plain anchor → `/iot-hub/{itemId}` with `target="_blank"` |
| Syntax | `${item-link:<uuid>}` — ID only, type derived from API response |
| Unavailable item handling | Render disabled card labeled "Item unavailable" |
| Resolution strategy | Render skeleton, fetch async, swap when ready (per card) |

## Architecture

Three building blocks, scoped tightly:

1. **`replaceItemLinkPlaceholders(markdown: string): string`** — pure
   string transform. Rewrites `${item-link:<uuid>}` to
   `<tb-iot-hub-item-link-card itemId="<uuid>"></tb-iot-hub-item-link-card>`.
2. **`TbIotHubItemLinkCardComponent`** — Angular component that owns
   fetch, state (`loading` / `loaded` / `unavailable`), and visual.
3. **`IotHubItemLinkModule`** — declares the component; passed as
   `additionalCompileModules` on every `tb-markdown` instance that needs it.

`tb-markdown` already supports compile-time injection of additional
modules; placeholder rewriting + module registration is all that is
needed to make the component render inside markdown.

## Placeholder syntax

- Format: `${item-link:<uuid>}`
- Regex: strict 36-char UUID match
  (`/\$\{item-link:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\}/g`)
- Non-UUID payloads (typos) are left as-is in rendered output, so the
  author sees the issue during preview.
- Placeholders inside fenced code blocks are still replaced — matches
  the existing `prefixResourceUrls` / `resolveDocLinks` precedent in the
  same files.

## Integration points

| File | Hook |
|---|---|
| `iot-hub-item-detail-dialog.component.ts` `loadReadme()` | Add as a step in the existing pipeline next to `prefixResourceUrls` and `resolveDocLinks` |
| `device-install-dialog.component.ts` `resolveVariables()` | Add an `^item-link:(uuid)$` branch alongside `gateway.downloadButton` and the callout matchers |
| `solution-install-dialog.component.ts` constructor | New step before assigning `this.details` |

All three call the same backing helper. Each `tb-markdown` instance
in those templates gets `[additionalCompileModules]="[IotHubItemLinkModule]"`.

## Component spec

**Location:**
`ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/`

**Inputs:**
- `@Input() itemId: string`

**Lifecycle:**
- `ngOnInit()` calls
  `iotHubApiService.getPublishedVersion(itemId, { ignoreErrors: true, ignoreLoading: true })`.
- Success → `state = 'loaded'`, store item.
- Error / 404 / network failure → `state = 'unavailable'`.

**Template — three states:**

- **loading:** skeleton card matching the loaded layout (gray thumb +
  two text shimmer lines).
- **loaded:** anchor to `/iot-hub/{itemId}` (`target="_blank"`,
  `rel="noopener noreferrer"`), thumbnail slot + name + creator row.
- **unavailable:** non-clickable card, 60 % opacity, `link_off` icon
  in the thumbnail slot, label "Item unavailable" (translated).

**Thumbnail rules** (mirror the search popup at
`iot-hub-home.component.html:103-115`):
- Compact types (`CALCULATED_FIELD`, `ALARM_RULE`, `RULE_CHAIN`):
  colored square + `getCompactIcon()` + item color.
- Other types: image via `iotHubApiService.resolveResourceUrl(item.image)`,
  fallback to a `mat-icon` of the type when no image is present.
- The same branching exists in `iot-hub-home.component.ts` (search popup)
  and `iot-hub-item-detail-dialog.component.ts`. The new card duplicates
  it locally — extracting a shared helper is out of scope for this PR.

**Styling:**
- Component-scoped SCSS.
- Fixed width ~320 px, block-level (each card on its own line in markdown).
- Subtle CSS-keyframe shimmer for the skeleton state.
- Hover: matches search-popup card hover.

**Click:**
- Plain anchor — middle-click / ctrl-click / "open in new tab" all work
  natively. Existing route `/iot-hub/:itemId`
  (`TbIotHubItemResolverComponent`) handles resolution, the
  unpublished-version warning, and opening the detail dialog on the
  type page.

## Translations

Add the following keys (and propagate to other locale files):

| Key | English |
|---|---|
| `iot-hub.item-link-unavailable` | "Item unavailable" |

## Out of scope

- Changelog rendering (explicitly excluded).
- IoT Hub creator-side authoring helpers (placeholder is plain text in
  raw markdown; nothing required on the IoT Hub backend).
- Batch endpoint for resolving multiple items in one request — N parallel
  requests is fine for the expected 0–5 references per page.
- A non-block (inline) variant of the card.
- Hover preview / tooltip / type chip on the card itself.

## Testing

- Component unit tests: state transitions (loading → loaded, loading
  → unavailable), thumbnail logic for compact vs. non-compact types,
  fallback when image missing.
- Regex unit test for `replaceItemLinkPlaceholders` covering: valid
  UUID, invalid UUID (left untouched), placeholder inside fenced code
  (still replaced — documented behavior), multiple placeholders in one
  document.
- Manual visual QA in all three render sites.

## Risks / open items

- `tb-markdown` recompiles on every `data` change; if a parent toggles
  the markdown rapidly, the card re-fetches. Acceptable: readmes /
  instructions don't churn during normal viewing.
- `getPublishedVersion` returns the **current** published version. If a
  newer published version changes the name/thumbnail, links update
  automatically — that is the documented behavior of the ID-only syntax.
