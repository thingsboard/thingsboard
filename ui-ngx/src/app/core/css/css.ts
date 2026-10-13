// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
interface CSSRule {
  directive: string;
  value: string;
  defective?: boolean;
  type?: string;
  nested?: CSSRule[];
}

interface CSSObject {
  selector: string;
  type?: 'media' | 'keyframes' | 'imports' | 'font-face' | 'at-rule';
  rules?: CSSRule[];
  subStyles?: CSSObject[];
  styles?: string;
}

type CSSBlock = { prelude: string; body: string } | { text: string };

export default class CSSParser {
  cssPreviewNamespace: string = '';
  testMode: boolean | ((action: string, css: string) => string) = false;

  cssImportStatements: string[] = [];

  private readonly groupingAtRuleRegex: RegExp = /^@(media|container|supports|layer|scope|document|starting-style)\b/i;
  private readonly keyframesRegex: RegExp = /^@(-[a-z]+-)?keyframes\b/i;
  private readonly scopeRegex: RegExp = /^@scope\b/i;
  private readonly importStatementRegex: RegExp = /^@import\b/i;
  private readonly hoistedStatementRegex: RegExp = /^@(import|charset)\b/i;

  /**
   * Removes CSS comments from the provided CSS string, leaving comment-like text inside strings intact.
   * @param cssString - The CSS string to strip comments from.
   * @returns The CSS string with comments removed.
   */
  stripComments(cssString: string): string {
    let result = '';
    let start = 0;
    let i = 0;
    while (i < cssString.length) {
      const ch = cssString[i];
      if (ch === '\\') {
        i += 2;
      } else if (ch === '"' || ch === '\'') {
        i++;
        while (i < cssString.length && cssString[i] !== ch && cssString[i] !== '\n') {
          i += cssString[i] === '\\' ? 2 : 1;
        }
        i++;
      } else if (ch === '/' && cssString[i + 1] === '*') {
        const end = cssString.indexOf('*/', i + 2);
        result += cssString.slice(start, i);
        i = start = end === -1 ? cssString.length : end + 2;
      } else {
        i++;
      }
    }
    return result + cssString.slice(start);
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
    let leading = true;

    for (const block of this.splitBlocks(this.stripComments(source))) {
      if ('text' in block) {
        const statement = block.text.trim();
        if (!statement.startsWith('@')) {
          continue;
        }
        const styles = statement.endsWith(';') ? statement : `${statement};`;
        if (this.importStatementRegex.test(styles)) {
          this.cssImportStatements.push(styles);
        }
        // statements before the first block, @import and @charset go first; the rest keep source order (e.g. @layer a;)
        if (leading || this.hoistedStatementRegex.test(styles)) {
          css.push({ selector: '@imports', type: 'imports', styles });
        } else {
          css.push({ selector: styles, type: 'at-rule', styles });
        }
        continue;
      }
      leading = false;
      const cleanSelector = block.prelude.replace(/\r\n/g, '\n').trim();

      if (this.keyframesRegex.test(cleanSelector)) {
        css.push({ selector: '@keyframes', type: 'keyframes', styles: `${cleanSelector} {${block.body}}` });
      } else if (this.groupingAtRuleRegex.test(cleanSelector)) {
        css.push({
          selector: cleanSelector,
          type: 'media',
          subStyles: this.parseCSS(block.body),
        });
      } else if (cleanSelector.startsWith('@') && cleanSelector !== '@font-face') {
        // descriptor at-rules (@property, @counter-style, @page, ...) have no selectors to namespace, keep them verbatim
        css.push({
          selector: cleanSelector,
          type: 'at-rule',
          styles: `${cleanSelector} {${block.body}}`,
        });
      } else {
        const rules = this.parseStyleBody(block.body);
        const style: CSSObject = {
          selector: cleanSelector,
          rules,
          ...(cleanSelector === '@font-face' && {type: 'font-face'}),
        };
        css.push(style);
      }
    }

    return css;
  }

  /**
   * Splits comment-free CSS into top-level blocks by matching braces, skipping strings and escapes.
   * @param source - The CSS string to split.
   * @returns Top-level blocks with prelude and body, and the text between them split on top-level semicolons.
   */
  private splitBlocks(source: string): CSSBlock[] {
    const blocks: CSSBlock[] = [];
    let depth = 0;
    let parens = 0;
    let start = 0;
    let bodyStart = 0;
    let i = 0;
    while (i < source.length) {
      const ch = source[i];
      if (ch === '\\') {
        i += 2;
        continue;
      }
      if (ch === '"' || ch === '\'') {
        i++;
        // an unescaped newline ends a string, so an unclosed quote doesn't swallow the rest of the CSS
        while (i < source.length && source[i] !== ch && source[i] !== '\n') {
          i += source[i] === '\\' ? 2 : 1;
        }
      } else if (ch === '(') {
        parens++;
      } else if (ch === ')') {
        parens = Math.max(0, parens - 1);
      } else if (ch === '{') {
        parens = 0;
        if (depth++ === 0) {
          bodyStart = i + 1;
        }
      } else if (ch === '}') {
        parens = 0;
        if (depth === 0) {
          start = i + 1;
        } else if (--depth === 0) {
          blocks.push({ prelude: source.slice(start, bodyStart - 1), body: source.slice(bodyStart, i) });
          start = i + 1;
        }
      } else if (ch === ';' && depth === 0 && parens === 0) {
        blocks.push({ text: source.slice(start, i + 1) });
        start = i + 1;
      }
      i++;
    }
    if (depth > 0) {
      blocks.push({ prelude: source.slice(start, bodyStart - 1), body: source.slice(bodyStart) });
    } else if (start < source.length) {
      blocks.push({ text: source.slice(start) });
    }
    return blocks;
  }

  /**
   * Parses the body of a style rule: declarations plus nested rules (CSS Nesting), kept in source order.
   * @param body - The style rule body.
   * @returns An array of rule objects; nested rules carry their prelude as directive.
   */
  private parseStyleBody(body: string): CSSRule[] {
    const rules: CSSRule[] = [];
    let text = '';
    for (const block of this.splitBlocks(body)) {
      if ('text' in block) {
        text += block.text;
      } else {
        rules.push(...this.parseRules(text));
        text = '';
        rules.push({ directive: block.prelude.trim(), value: '', nested: this.parseStyleBody(block.body) });
      }
    }
    rules.push(...this.parseRules(text));
    return rules;
  }

  /**
   * Splits a selector list on top-level commas, keeping commas inside :is()/:has()/[attr] intact.
   * @param selector - The selector list.
   * @returns The complex selectors of the list.
   */
  private splitSelectorList(selector: string): string[] {
    const parts: string[] = [];
    let depth = 0;
    let start = 0;
    for (let i = 0; i < selector.length; i++) {
      const ch = selector[i];
      if (ch === '\\') {
        i++;
      } else if (ch === '(' || ch === '[') {
        depth++;
      } else if (ch === ')' || ch === ']') {
        depth--;
      } else if (ch === ',' && depth === 0) {
        parts.push(selector.slice(start, i));
        start = i + 1;
      }
    }
    parts.push(selector.slice(start));
    return parts;
  }

  private namespaceSelectorList(selector: string, namespaceClass: string): string {
    return this.splitSelectorList(selector)
      .map(sel => `${namespaceClass} ${sel}`)
      .join(',');
  }

  /**
   * Namespaces the scope root of an @scope prelude. Rules inside @scope are relative to the root,
   * so namespacing them would require the namespace element to be inside the root.
   * @param prelude - The @scope prelude, e.g. `@scope (.card) to (.content)`.
   * @param namespaceClass - The namespace selector.
   * @returns The prelude with the namespaced root; a missing root becomes the namespace itself.
   */
  private namespaceScope(prelude: string, namespaceClass: string): string {
    const rest = prelude.replace(this.scopeRegex, '').trim();
    if (!rest.startsWith('(')) {
      return `@scope (${namespaceClass})${rest ? ` ${rest}` : ''}`;
    }
    let depth = 0;
    let end = 0;
    while (end < rest.length) {
      if (rest[end] === '(') {
        depth++;
      } else if (rest[end] === ')' && --depth === 0) {
        break;
      }
      end++;
    }
    return `@scope (${this.namespaceSelectorList(rest.slice(1, end), namespaceClass)})${rest.slice(end + 1)}`;
  }

  /**
   * Parses CSS rules into an array of rule objects.
   * @param rules - The CSS rules string.
   * @returns An array of rule objects with directive and value.
   */
  parseRules(rules: string): CSSRule[] {
    const normalizedRules = rules.replace(/\r\n/g, '\n');
    const ruleList = this.splitDeclarations(normalizedRules);
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
   * Splits declarations on semicolons outside strings and parentheses (e.g. `url(data:...;base64,...)`).
   * @param rules - The declarations string.
   * @returns The individual declarations.
   */
  private splitDeclarations(rules: string): string[] {
    const parts: string[] = [];
    let parens = 0;
    let start = 0;
    for (let i = 0; i < rules.length; i++) {
      const ch = rules[i];
      if (ch === '\\') {
        i++;
      } else if (ch === '"' || ch === '\'') {
        i++;
        while (i < rules.length && rules[i] !== ch && rules[i] !== '\n') {
          i += rules[i] === '\\' ? 2 : 1;
        }
      } else if (ch === '(') {
        parens++;
      } else if (ch === ')') {
        parens = Math.max(0, parens - 1);
      } else if (ch === ';' && parens === 0) {
        parts.push(rules.slice(start, i));
        start = i + 1;
      }
    }
    parts.push(rules.slice(start));
    return parts;
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
    const spaces = this.getSpaces(depth);
    let result = '';

    // Append imports
    for (const obj of css) {
      if (obj.type === 'imports') {
        result += `${spaces}${obj.styles}\n\n`;
      }
    }

    // Append styles
    for (const obj of css) {
      if (!obj.selector) continue;
      if (obj.type === 'media') {
        result += `${spaces}${obj.selector} {\n${this.getCSSForEditor(obj.subStyles, depth + 1)}${spaces}}\n\n`;
      } else if (obj.type === 'at-rule') {
        result += `${spaces}${obj.styles}\n\n`;
      } else if (obj.type !== 'keyframes' && obj.type !== 'imports') {
        result += `${spaces}${obj.selector} {\n${this.getCSSOfRules(obj.rules ?? [], depth + 1)}${spaces}}\n\n`;
      }
    }

    // Append keyframes
    for (const obj of css) {
      if (obj.type === 'keyframes') {
        result += `${spaces}${obj.styles}\n\n`;
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
      if (rule.nested) {
        result += `${this.getSpaces(depth)}${rule.directive} {\n${this.getCSSOfRules(rule.nested, depth + 1)}${this.getSpaces(depth)}}\n`;
      } else if (rule.defective) {
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
      if (this.isVerbatim(obj)) {
        continue;
      }

      if (obj.type !== 'media') {
        obj.selector = this.namespaceSelectorList(obj.selector, namespaceClass);
      } else if (this.scopeRegex.test(obj.selector)) {
        obj.selector = this.namespaceScope(obj.selector, namespaceClass);
      } else {
        obj.subStyles = this.applyNamespacing(obj.subStyles ?? [], forcedNamespace);
      }
    }

    return cssObjectArray;
  }

  private isVerbatim(obj: CSSObject): boolean {
    return obj.type === 'keyframes' || obj.type === 'imports' || obj.type === 'at-rule' || obj.type === 'font-face';
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
      if (this.isVerbatim(obj)) {
        continue;
      }
      if (obj.type !== 'media') {
        obj.selector = obj.selector.split(namespaceClass + ' ').join('');
      } else if (this.scopeRegex.test(obj.selector)) {
        obj.selector = obj.selector.replace(`@scope (${namespaceClass})`, '@scope').split(namespaceClass + ' ').join('');
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
