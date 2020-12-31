///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Ace } from 'ace-builds';

export type tbMetaType = 'object' | 'function' | 'service' | 'property' | 'argument';

export type TbEditorCompletions = {[name: string]: TbEditorCompletion};

export interface FunctionArgType {
  type?: string;
  description?: string;
}

export interface FunctionArg extends FunctionArgType {
  name: string;
  type?: string;
  description?: string;
  optional?: boolean;
}

export interface TbEditorCompletion {
  meta: tbMetaType;
  description?: string;
  type?: string;
  args?: FunctionArg[];
  return?: FunctionArgType;
  children?: TbEditorCompletions;
}

interface TbEditorAceCompletion extends Ace.Completion {
  isTbEditorAceCompletion: true;
  snippet: string;
  description?: string;
  type?: string;
  args?: FunctionArg[];
  return?: FunctionArgType;
}

export class TbEditorCompleter implements Ace.Completer {

  identifierRegexps: RegExp[] = [
    /[a-zA-Z_0-9\$\-\u00A2-\u2000\u2070-\uFFFF.]/
  ];

  constructor(private editorCompletions: TbEditorCompletions) {
  }

  getCompletions(editor: Ace.Editor, session: Ace.EditSession,
                 position: Ace.Point, prefix: string, callback: Ace.CompleterCallback): void {
    const result = this.prepareCompletions(prefix);
    if (result) {
      callback(null, result);
    }
  }

  private resolvePath(prefix: string): string[] {
    if (!prefix || !prefix.length) {
      return [];
    }
    let parts = prefix.split('.');
    if (parts.length) {
      parts.pop();
    } else {
      parts = [];
    }
    let currentCompletions = this.editorCompletions;
    for (const part of parts) {
      if (currentCompletions[part]) {
        currentCompletions = currentCompletions[part].children;
        if (!currentCompletions) {
          return null;
        }
      } else {
        return null;
      }
    }
    return parts;
  }

  private prepareCompletions(prefix: string): Ace.Completion[]  {
    const path = this.resolvePath(prefix);
    if (path !== null) {
      return this.toAceCompletionsList(this.editorCompletions, path);
    } else {
      return [];
    }
  }

  private toAceCompletionsList(completions: TbEditorCompletions, parentPath: string[]): Ace.Completion[]  {
    const result: Ace.Completion[] = [];
    let targetCompletions = completions;
    let parentPrefix = '';
    if (parentPath.length) {
      parentPrefix = parentPath.join('.') + '.';
      for (const path of parentPath) {
        targetCompletions = targetCompletions[path].children;
      }
    }
    for (const key of Object.keys(targetCompletions)) {
      result.push(this.toAceCompletion(key, targetCompletions[key], parentPrefix));
    }
    return result;
  }

  private toAceCompletion(name: string, completion: TbEditorCompletion, parentPrefix: string): Ace.Completion {
    const aceCompletion: TbEditorAceCompletion = {
      isTbEditorAceCompletion: true,
      snippet: parentPrefix + name,
      name,
      caption: parentPrefix + name,
      score: 100000,
      value: parentPrefix + name,
      meta: completion.meta,
      type: completion.type,
      description: completion.description,
      args: completion.args,
      return: completion.return
    };
    return aceCompletion;
  }

  getDocTooltip(completion: TbEditorAceCompletion) {
    if (completion && completion.isTbEditorAceCompletion) {
      return {
        docHTML: this.createDocHTML(completion)
      };
    }
  }

  private createDocHTML(completion: TbEditorAceCompletion): string {
    let title = `<b>${completion.name}</b>`;
    if (completion.meta === 'function') {
      title += '(';
      if (completion.args) {
        const strArgs: string[] = [];
        for (const arg of completion.args) {
          let strArg = `${arg.name}`;
          if (arg.optional) {
            strArg += '?';
          }
          if (arg.type) {
            strArg += `: ${arg.type}`;
          }
          strArgs.push(strArg);
        }
        title += strArgs.join(', ');
      }
      title += '): ';
      if (completion.return) {
        title += completion.return.type;
      } else {
        title += 'void';
      }
    } else {
      title += `: ${completion.type ? completion.type : completion.meta}`;
    }
    let html = `<div class="tb-ace-doc-tooltip"><code class="title">${title}</code>`;
    if (completion.description) {
      html += `<hr><div>${completion.description}</div>`;
    }
    if (completion.args || completion.return) {
      let functionInfoBlock = '<div class="tb-function-info">';
      if (completion.args) {
        functionInfoBlock += '<div class="tb-api-title">Parameters</div>';
        let argsTable = '<table class="tb-api-table"><tbody>';
        const strArgs: string[] = [];
        for (const arg of completion.args) {
          let strArg = `<tr><td class="arg-name"><code>${arg.name}`;
          if (arg.optional) {
            strArg += ' (optional)';
          }
          strArg += '</code></td><td>';
          if (arg.type) {
            strArg += `<code>${arg.type}</code>`;
          }
          strArg += '</td><td class="arg-description">';
          if (arg.description) {
            strArg += `${arg.description}`;
          }
          strArg += '</td></tr>';
          strArgs.push(strArg);
        }
        argsTable += strArgs.join('') + '</tbody></table>';
        functionInfoBlock += argsTable;
      }
      if (completion.return) {
        let returnStr = '<div class="tb-api-title">Returns</div>';
        returnStr += `<div class="tb-function-return"><code>${completion.return.type}</code>`;
        if (completion.return.description) {
          returnStr += `: ${completion.return.description}`;
        }
        returnStr += '</div>';
        functionInfoBlock += returnStr;
      }
      functionInfoBlock += '</div>';
      html += functionInfoBlock;
    }
    html += '</div>';
    return html;
  }
}
