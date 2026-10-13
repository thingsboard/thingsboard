// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AgentAppTemplate } from '@shared/models/agent.models';

// Extracts the leading dotted numeric segments of a version, ignoring any
// trailing suffix (e.g. "4.3.1.2EDGEPE" -> [4, 3, 1, 2]).
function versionSegments(version: string | undefined | null): number[] {
  const match = (version || '').match(/^\d+(?:\.\d+)*/);
  return match ? match[0].split('.').map(seg => parseInt(seg, 10)) : [];
}

// Compares two version strings numerically, newest-first. Missing trailing
// segments count as 0 (so 4.3.1 < 4.3.1.1), and versions with equal numeric
// parts fall back to a descending string compare on the whole label to keep
// the order deterministic for differing suffixes.
export function compareVersionsDesc(a: string | undefined | null, b: string | undefined | null): number {
  const sa = versionSegments(a);
  const sb = versionSegments(b);
  const len = Math.max(sa.length, sb.length);
  for (let i = 0; i < len; i++) {
    const da = sa[i] ?? 0;
    const db = sb[i] ?? 0;
    if (da !== db) {
      return db - da;
    }
  }
  return (b || '').localeCompare(a || '');
}

// Orders templates newest-first by semantic version. The stored nextVersion
// chain is intentionally not used for ordering: it does not follow version
// order (it can link across minor versions), so walking it can put e.g. 4.2.x
// ahead of 4.3.x. Sorting the parsed numeric segments guarantees the whole
// 4.3.* group precedes 4.2.*, etc.
export function orderTemplatesNewestFirst(templates: AgentAppTemplate[]): AgentAppTemplate[] {
  if (!templates?.length) {
    return templates ? [...templates] : [];
  }
  return [...templates].sort((a, b) => compareVersionsDesc(a.currentVersion, b.currentVersion));
}
