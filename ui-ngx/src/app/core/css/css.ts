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

interface CSSRule {
  directive: string;
  value: string;
  defective?: boolean;
  type?: string;
}

interface CSSObject {
  selector: string;
  type?: 'media' | 'keyframes' | 'imports' | 'font-face';
  rules?: CSSRule[];
  subStyles?: CSSObject[];
  styles?: string;
  comments?: string;
}

export default class CSSParser {
  cssPreviewNamespace: string = '';
  testMode: boolean | ((action: string, css: string) => string) = false;

  cssImportStatements: string[] = [];

  private readonly cssKeyframeRegex: string = '((@.*?keyframes [\\s\\S]*?){([\\s\\S]*?}\\s*?)})';
  private readonly combinedCSSRegex: string = '((\\s*?@media[\\s\\S]*?){([\\s\\S]*?)}\\s*?})|(([\\s\\S]*?){([\\s\\S]*?)})';
  private readonly cssCommentsRegex: string = '(\\/\\*[\\s\\S]*?\\*\\/)';
  private readonly cssImportStatementRegex: RegExp = /@import .*?;/gi;

  /**
   * Removes CSS comments from the provided CSS string.
   * @param cssString - The CSS string to strip comments from.
   * @returns The CSS string with comments removed.
   */
  stripComments(cssString: string): string {
    const regex = new RegExp(this.cssCommentsRegex, 'gi');
    return cssString.replace(regex, '');
  }

  /**
   * Parses a CSS string into an array of CSS objects with selectors and rules.
   * @param source - The CSS string to parse.
   * @returns An array of CSS objects.
   */
  parseCSS(source?: string): CSSObject[] {
    if (!source) {
      return [];
    }

    const css: CSSObject[] = [];
    let cssSource = source;

    const importStatementRegex = new RegExp(this.cssImportStatementRegex.source, this.cssImportStatementRegex.flags);
    cssSource = cssSource.replace(importStatementRegex, (match) => {
      this.cssImportStatements.push(match);
      css.push({ selector: '@imports', type: 'imports', styles: match });
      return '';
    });

    // Extract keyframe statements
    const keyframesRegex = new RegExp(this.cssKeyframeRegex, 'gi');
    cssSource = cssSource.replace(keyframesRegex, (match) => {
      css.push({ selector: '@keyframes', type: 'keyframes', styles: match });
      return '';
    });

    // Parse remaining CSS
    const unifiedRegex = new RegExp(this.combinedCSSRegex, 'gi');
    let match: RegExpExecArray | null;
    while ((match = unifiedRegex.exec(cssSource)) !== null) {
      const selector = (match[2] ?? match[5]).replace(/\r\n/g, '\n').trim();

      // Extract comments
      const commentsRegex = new RegExp(this.cssCommentsRegex, 'gi');
      const comments = commentsRegex.exec(selector);
      const cleanSelector = comments ? selector.replace(commentsRegex, '').trim() : selector;

      if (cleanSelector.includes('@media')) {
        css.push({
          selector: cleanSelector,
          type: 'media',
          subStyles: this.parseCSS(match[3] + '\n}'),
          ...(comments && {comments: comments[0]}),
        });
      } else {
        const rules = this.parseRules(match[6]);
        const style: CSSObject = {
          selector: cleanSelector,
          rules,
          ...(cleanSelector === '@font-face' && {type: 'font-face'}),
          ...(comments && {comments: comments[0]}),
        };
        css.push(style);
      }
    }

    return css;
  }

  /**
   * Parses CSS rules into an array of rule objects.
   * @param rules - The CSS rules string.
   * @returns An array of rule objects with directive and value.
   */
  parseRules(rules: string): CSSRule[] {
    const normalizedRules = rules.replace(/\r\n/g, '\n');
    const ruleList = normalizedRules.split(/;(?![^(]*\))/);
    const result: CSSRule[] = [];

    for (const line of ruleList) {
      const trimmedLine = line.trim();
      if (!trimmedLine) continue;

      if (trimmedLine.includes(':')) {
        const [directive, ...valueParts] = trimmedLine.split(':');
        const value = valueParts.join(':').trim();
        if (directive.trim() && value) {
          result.push({directive: directive.trim(), value});
        }
      } else if (trimmedLine.startsWith('base64,')) {
        if (result.length > 0) {
          result[result.length - 1].value += trimmedLine;
        }
      } else if (trimmedLine) {
        result.push({directive: '', value: trimmedLine, defective: true});
      }
    }

    return result;
  }

  /**
   * Finds a rule matching the given directive in the rules array.
   * @param rules - The array of CSS rules.
   * @param directive - The directive to search for.
   * @param value - Optional value to match.
   * @returns The matching rule or false if not found.
   */
  findCorrespondingRule(rules: CSSRule[], directive: string, value?: string): CSSRule | false {
    return rules.find(rule => rule.directive === directive && (!value || rule.value === value)) || false;
  }

  /**
   * Finds CSS objects by selector, optionally merging duplicates.
   * @param cssObjectArray - The array of CSS objects.
   * @param selector - The selector to search for.
   * @param contains - If true, matches selectors containing the string.
   * @returns An array of matching CSS objects.
   */
  findBySelector(cssObjectArray: CSSObject[], selector: string, contains: boolean = false): CSSObject[] {
    const found = cssObjectArray.filter(obj => contains ? obj.selector.includes(selector) : obj.selector === selector);

    if (found.length < 2) return found;

    const base = found[0];
    for (let i = 1; i < found.length; i++) {
      this.intelligentCSSPush([base], found[i]);
    }
    return [base];
  }

  /**
   * Deletes CSS objects with the given selector.
   * @param cssObjectArray - The array of CSS objects.
   * @param selector - The selector to delete.
   * @returns A new array without the matching CSS objects.
   */
  deleteBySelector(cssObjectArray: CSSObject[], selector: string): CSSObject[] {
    return cssObjectArray.filter(obj => obj.selector !== selector);
  }

  /**
   * Compresses CSS objects by merging duplicates.
   * @param cssObjectArray - The array of CSS objects to compress.
   * @returns A compressed array of CSS objects.
   */
  compressCSS(cssObjectArray: CSSObject[]): CSSObject[] {
    const compressed: CSSObject[] = [];
    const done = new Set<string>();

    for (const obj of cssObjectArray) {
      if (done.has(obj.selector)) continue;
      const found = this.findBySelector(cssObjectArray, obj.selector);
      if (found.length) {
        compressed.push(found[0]);
        done.add(obj.selector);
      }
    }
    return compressed;
  }

  /**
   * Computes the difference between two CSS objects.
   * @param css1 - The first CSS object.
   * @param css2 - The second CSS object.
   * @returns A CSS object with the differences or false if no differences.
   */
  cssDiff(css1: CSSObject, css2: CSSObject): CSSObject | false {
    if (css1.selector !== css2.selector || css1.type === 'media' || css2.type === 'media') {
      return false;
    }

    const diff: CSSObject = {selector: css1.selector, rules: []};
    const rules1 = css1.rules ?? [];
    const rules2 = css2.rules ?? [];

    for (const rule1 of rules1) {
      const rule2 = this.findCorrespondingRule(rules2, rule1.directive, rule1.value);
      if (!rule2 || rule1.value !== rule2.value) {
        diff.rules!.push(rule1);
      }
    }

    for (const rule2 of rules2) {
      if (!this.findCorrespondingRule(rules1, rule2.directive)) {
        diff.rules!.push({...rule2, type: 'DELETED'});
      }
    }

    return diff.rules!.length ? diff : false;
  }

  /**
   * Merges two CSS object arrays intelligently.
   * @param cssObjectArray - The target CSS object array.
   * @param newArray - The source CSS object array to merge.
   * @param reverse - If true, prioritizes styles in newArray.
   */
  intelligentMerge(cssObjectArray: CSSObject[], newArray: CSSObject[], reverse: boolean = false): void {
    for (const obj of newArray) {
      this.intelligentCSSPush(cssObjectArray, obj, reverse);
    }
    for (const obj of cssObjectArray) {
      if (obj.type !== 'media' && obj.type !== 'keyframes') {
        obj.rules = this.compactRules(obj.rules ?? []);
      }
    }
  }

  /**
   * Pushes a CSS object into an array, merging with existing selectors.
   * @param cssObjectArray - The target CSS object array.
   * @param minimalObject - The CSS object to push.
   * @param reverse - If true, traverses array in reverse for priority.
   */
  intelligentCSSPush(cssObjectArray: CSSObject[], minimalObject: CSSObject, reverse: boolean = false): void {
    const cssObject = (reverse ? cssObjectArray.slice().reverse() : cssObjectArray)
      .find(obj => obj.selector === minimalObject.selector) ?? false;

    if (!cssObject) {
      cssObjectArray.push(minimalObject);
      return;
    }

    if (minimalObject.type !== 'media') {
      for (const rule of minimalObject.rules ?? []) {
        const oldRule = this.findCorrespondingRule(cssObject.rules ?? [], rule.directive);
        if (!oldRule) {
          cssObject.rules!.push(rule);
        } else if (rule.type === 'DELETED') {
          oldRule.type = 'DELETED';
        } else {
          oldRule.value = rule.value;
        }
      }
    } else {
      cssObject.subStyles = minimalObject.subStyles;
    }
  }

  /**
   * Filters out rules marked as DELETED.
   * @param rules - The array of CSS rules.
   * @returns A compacted array of rules.
   */
  compactRules(rules: CSSRule[]): CSSRule[] {
    return rules.filter(rule => rule.type !== 'DELETED');
  }

  /**
   * Generates a formatted CSS string for an editor.
   * @param cssBase - The CSS object array to format.
   * @param depth - The indentation depth.
   * @returns A formatted CSS string.
   */
  getCSSForEditor(cssBase?: CSSObject[], depth: number = 0): string {
    const css = cssBase ?? this.parseCSS('');
    let result = '';

    // Append imports
    for (const obj of css) {
      if (obj.type === 'imports') {
        result += `${obj.styles}\n\n`;
      }
    }

    // Append styles
    for (const obj of css) {
      if (!obj.selector) continue;
      const comments = obj.comments ? `${obj.comments}\n` : '';

      if (obj.type === 'media') {
        result += `${comments}${obj.selector} {\n${this.getCSSForEditor(obj.subStyles, depth + 1)}}\n\n`;
      } else if (obj.type !== 'keyframes' && obj.type !== 'imports') {
        result += `${this.getSpaces(depth)}${comments}${obj.selector} {\n${this.getCSSOfRules(obj.rules ?? [], depth + 1)}${this.getSpaces(depth)}}\n\n`;
      }
    }

    // Append keyframes
    for (const obj of css) {
      if (obj.type === 'keyframes') {
        result += `${obj.styles}\n\n`;
      }
    }

    return result;
  }

  /**
   * Retrieves all import statements from a CSS object array.
   * @param cssObjectArray - The CSS object array.
   * @returns An array of import statement strings.
   */
  getImports(cssObjectArray: CSSObject[]): string[] {
    return cssObjectArray.filter(obj => obj.type === 'imports').map(obj => obj.styles!);
  }

  /**
   * Formats CSS rules into a string for an editor.
   * @param rules - The array of CSS rules.
   * @param depth - The indentation depth.
   * @returns A formatted CSS rules string.
   */
  getCSSOfRules(rules: CSSRule[], depth: number): string {
    let result = '';
    for (const rule of rules) {
      if (!rule) continue;
      if (rule.defective) {
        result += `${this.getSpaces(depth)}${rule.value};\n`;
      } else {
        result += `${this.getSpaces(depth)}${rule.directive}: ${rule.value};\n`;
      }
    }
    return result || '\n';
  }

  /**
   * Generates indentation spaces based on depth.
   * @param num - The indentation level.
   * @returns A string of spaces.
   */
  getSpaces(num: number): string {
    return ' '.repeat(num * 4);
  }

  /**
   * Applies a namespace to CSS selectors to prevent collisions.
   * @param css - The CSS string or object array.
   * @param forcedNamespace - Optional custom namespace.
   * @returns The namespaced CSS object array.
   */
  applyNamespacing(css: string | CSSObject[], forcedNamespace?: string): CSSObject[] {
    const namespaceClass = forcedNamespace ?? `.${this.cssPreviewNamespace}`;
    const cssObjectArray = typeof css === 'string' ? this.parseCSS(css) : css;

    for (const obj of cssObjectArray) {
      if (['@font-face', 'keyframes', '@import', '.form-all', '#stage'].some(s => obj.selector.includes(s))) {
        continue;
      }

      if (obj.type !== 'media') {
        obj.selector = obj.selector.split(',')
          .map(sel => sel.includes('.supernova') ? sel : `${namespaceClass} ${sel}`)
          .join(',');
      } else {
        obj.subStyles = this.applyNamespacing(obj.subStyles ?? [], forcedNamespace);
      }
    }

    return cssObjectArray;
  }

  /**
   * Removes namespacing from CSS selectors.
   * @param css - The CSS string or object array.
   * @param returnObj - If true, returns the CSS object array.
   * @returns The CSS string or object array with namespacing removed.
   */
  clearNamespacing(css: string | CSSObject[], returnObj: boolean = false): string | CSSObject[] {
    const namespaceClass = `.${this.cssPreviewNamespace}`;
    const cssObjectArray = typeof css === 'string' ? this.parseCSS(css) : css;

    for (const obj of cssObjectArray) {
      if (obj.type !== 'media') {
        obj.selector = obj.selector
          .split(',')
          .map(sel => sel.split(namespaceClass + ' ').join(''))
          .join(',');
      } else {
        obj.subStyles = this.clearNamespacing(obj.subStyles ?? [], true) as CSSObject[];
      }
    }

    return returnObj ? cssObjectArray : this.getCSSForEditor(cssObjectArray);
  }

  /**
   * Creates a style element with the provided CSS.
   * @param id - The ID for the style element.
   * @param css - The CSS string or object array.
   * @param format - If true, formats the CSS; if 'nonamespace', skips namespacing.
   */
  createStyleElement(id: string, css: string | CSSObject[], format: boolean | 'nonamespace' = false): void | string {
    let cssString = typeof css === 'string' ? css : this.getCSSForEditor(css);

    if (this.testMode === false && format !== 'nonamespace') {
      cssString = this.getCSSForEditor(this.applyNamespacing(css));
    }

    if (format === true) {
      cssString = this.getCSSForEditor(this.parseCSS(cssString));
    }

    if (typeof this.testMode === 'function') {
      return this.testMode(`create style #${id}`, cssString);
    }

    const existingElement = document.getElementById(id);
    existingElement?.remove();

    if (!css) {
      return;
    }

    const style = document.createElement('style');
    style.id = id;

    if ('styleSheet' in style && !('sheet' in style)) {
      (style as any).styleSheet.cssText = cssString;
    } else {
      style.appendChild(document.createTextNode(cssString));
    }

    (document.head || document.getElementsByTagName('head')[0]).appendChild(style);
  }
}
