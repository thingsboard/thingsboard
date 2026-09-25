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
