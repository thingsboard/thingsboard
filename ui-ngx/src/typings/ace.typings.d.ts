// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
declare module 'ace-code' {
  export type EditSession = import('ace-builds').Ace.EditSession;
  export type Editor = import('ace-builds').Ace.Editor;
  export type Point = import('ace-builds').Ace.Point;
  export type Range = import('ace-builds').Ace.Range;

  export namespace Ace {
    export type EditSession = import('ace-builds').Ace.EditSession;
    export type Editor = import('ace-builds').Ace.Editor;
    export type Point = import('ace-builds').Ace.Point;
    export type Range = import('ace-builds').Ace.Range;
    export type Completion = {
      value: string;
      meta?: string;
      type?: string;
      caption?: string;
      snippet?: string;
      score?: number;
      exactMatch?: number;
      docHTML?: string;
      [key: string]: any;
    };
    export type Document = import('ace-builds').Ace.Document;
    export type MarkerGroupItem = import('ace-builds').Ace.MarkerGroupItem;
  }
}

// ace-linters' bundled types reference htmlhint's Ruleset type for its (unused-by-us) HTML
// validation service. htmlhint is only a devDependency of ace-linters (needed to build its
// bundled .d.ts, not to consume it), so it's correctly absent from this project's node_modules -
// this shim just satisfies the import without pulling htmlhint in as a real dependency.
declare module 'htmlhint/dist/core/types' {
  export interface Ruleset {
    [key: string]: any;
  }
}

declare module 'ace-code/src/autocomplete' {
  export class CompletionProvider {
    registerCompleter(completer: any): void;
    [key: string]: any;
  }
}

declare module 'ace-code/src/ext/command_bar' {
  export class CommandBarTooltip {
    constructor(editor: any);
    [key: string]: any;
  }
}

declare module 'ace-code/src/ext/inline_autocomplete' {
  export class InlineAutocomplete {
    constructor(editor: any);
    [key: string]: any;
  }
}
