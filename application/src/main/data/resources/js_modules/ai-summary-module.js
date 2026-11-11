export function getAnalytics(widgetCtx, isDefault, prompt) {
    if (!isDefault && !prompt) {
        throw new Error('You should indicate prompt/promptId or use default prompt');
    }

    const services = initializeServices(widgetCtx);
    injectStyles(DIALOG_STYLES);

    services.customDialog.customDialog(
        createDialogTemplate(),
        (instance) => new AnalyticsDialogController(instance, services, isDefault, prompt).initialize(),
        null,
        {width: '600px', maxWidth: '90%'}
    ).subscribe();
}

function initializeServices(widgetCtx) {
    const $injector = widgetCtx.$scope.$injector;
    const config = widgetCtx.widget.config;

    return {
        ctx: widgetCtx,
        customDialog: $injector.get(widgetCtx.servicesMap.get('customDialog')),
        widgetService: $injector.get(widgetCtx.servicesMap.get('entityService')).widgetService,
        jwtToken: localStorage.getItem('jwt_token'),
        rxjs: widgetCtx.rxjs,
        title: config.showTitle && config.title ? config.title : ''
    };
}

function createDialogTemplate() {
    return `
        <div *ngIf="title" mat-dialog-title>{{title}}</div>
        <div mat-dialog-content style="padding: 0 24px">
            <div *ngIf="!summaryContent" class="flex items-center justify-center">
                <mat-spinner [diameter]="40"></mat-spinner>
            </div>
            <div *ngIf="summaryContent" class="tz-markdown-content" [innerHTML]="summaryContent"></div>
        </div>
        <div mat-dialog-actions class="flex items-center justify-end">
            <button mat-raised-button color="primary" (click)="cancel()">Close</button>
        </div>`;
}

function injectStyles(styleContent) {
    if (document.getElementById('custom-tz-dialog-styles')) return;

    const styleEl = document.createElement('style');
    styleEl.id = 'custom-tz-dialog-styles';
    styleEl.textContent = styleContent;
    document.head.appendChild(styleEl);
}

function removeStyles() {
    const styleEl = document.getElementById('custom-tz-dialog-styles');
    if (styleEl) {
        document.head.removeChild(styleEl);
    }
}

class AnalyticsDialogController {
    constructor(instance, services, isDefault, prompt) {
        this.instance = instance;
        this.services = services;
        this.isDefault = isDefault;
        this.prompt = prompt;
    }

    initialize() {
        this.instance.summaryContent = '';
        this.instance.title = this.services.title;
        this.instance.cancel = () => {
            removeStyles();
            this.instance.dialogRef.close(null);
        };

        this.loadAnalytics();
    }

    loadAnalytics() {
        const dataForExport = this.services.ctx.defaultSubscription.exportData();
        const csvData = DataTransformer.toCsv(dataForExport);

        const summaryRequest = this.buildSummaryRequest(csvData);
        const {delay, switchMap} = this.services.rxjs;

        this.services.ctx.http.post(`/apiTrendz/agent/prompts/execute`, summaryRequest, {
            headers: {
                'X-Authorization': `Bearer ${this.services.jwtToken}`,
                'Jwt': `${this.services.jwtToken}`
            }
        })
            .pipe(
                delay(200),
                switchMap(executionId => this.fetchTaskResult(executionId))
            )
            .subscribe({
                next: (result) => {
                    this.instance.summaryContent = MarkdownParser.parse(result) || 'Summary wasn`t generated!';
                },
                error: (error) => {
                    console.error('Error fetching summary:', error);
                    this.instance.dialogRef.close(null);
                }
            });
    }

    buildSummaryRequest(csvData) {
        if (this.isDefault || !this.prompt) {
            return {data: csvData};
        }

        const isUUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(this.prompt);
        return isUUID
            ? {promptId: this.prompt, data: csvData}
            : {prompt: this.prompt, data: csvData};
    }

    fetchTaskResult(executionId) {
        const {delay, switchMap, of} = this.services.rxjs;

        return this.services.ctx.http.get(`/apiTrendz/task/execution/poll/${executionId}`, {
            headers: {
                'X-Authorization': `Bearer ${this.services.jwtToken}`,
                'Jwt': `${this.services.jwtToken}`
            }
        })
            .pipe(
                delay(300),
                switchMap(execution => {
                    if (!execution) {
                        throw new Error('Failed to execute task');
                    }

                    const validStatuses = ['CREATED', 'FINISHED', 'RUNNING'];
                    if (!validStatuses.includes(execution.status)) {
                        throw new Error(this.extractErrorMessage(execution.jsonResult));
                    }

                    if (execution.status !== 'FINISHED') {
                        return this.fetchTaskResult(executionId);
                    }

                    return of(execution.jsonResult?.result || null);
                })
            );
    }

    extractErrorMessage(jsonResult) {
        try {
            return jsonResult.map(el => {
                const msg = el.message || '';
                try {
                    return JSON.parse(msg).message;
                } catch {
                    return msg;
                }
            }).join('\n');
        } catch {
            return 'Invalid response!';
        }
    }
}

class DataTransformer {
    static toCsv(data) {
        if (!data || !data.length) {
            return '';
        }

        const header = Object.keys(data[0]).join(';');
        const rows = data.map(obj => Object.values(obj).join(';')).join('\n');
        return `${header}\n${rows}`;
    }
}

class MarkdownParser {
    static parse(content) {
        if (!content) return null;

        let summary = this.escapeHtml(content);

        summary = this.parseCodeBlocks(summary);
        summary = this.parseBlockquotes(summary);
        summary = this.processLists(summary);
        summary = this.parseHeaders(summary);
        summary = this.parseHorizontalRules(summary);

        summary = this.parseInlineCode(summary);
        summary = this.parseBoldText(summary);
        summary = this.parseItalicText(summary);
        summary = this.parseLinks(summary);

        summary = this.parseParagraphs(summary);

        return summary;
    }

    static escapeHtml(content) {
        return content
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    static parseHeaders(summary) {
        return summary
            .replace(/^###### (.*$)/gm, '<h6>$1</h6>')
            .replace(/^##### (.*$)/gm, '<h5>$1</h5>')
            .replace(/^#### (.*$)/gm, '<h4>$1</h4>')
            .replace(/^### (.*$)/gm, '<h3>$1</h3>')
            .replace(/^## (.*$)/gm, '<h2>$1</h2>')
            .replace(/^# (.*$)/gm, '<h1>$1</h1>');
    }

    static parseHorizontalRules(summary) {
        return summary.replace(/^---$/gm, '<hr>');
    }

    static parseBlockquotes(summary) {
        return summary.replace(/^> (.*)$/gm, '<blockquote>$1</blockquote>');
    }

    static parseCodeBlocks(summary) {
        return summary.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');
    }

    static parseInlineCode(summary) {
        return summary.replace(/`([^`]+)`/g, '<code>$1</code>');
    }

    static parseBoldText(summary) {
        return summary
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/__(.*?)__/g, '<strong>$1</strong>');
    }

    static parseItalicText(summary) {
        return summary
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/_(.*?)_/g, '<em>$1</em>');
    }

    static parseLinks(summary) {
        return summary.replace(/\[([^\]]+)\]\(([^)]+)\)/g,
            '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
    }

    static processLists(summary) {
        const lines = summary.split('\n');
        const processor = new ListProcessor();
        return processor.process(lines);
    }

    static parseParagraphs(summary) {
        const lines = summary.split('\n');
        let result = '';
        let currentParagraph = [];
        for (const line of lines) {
            const isBlockElement = line.match(/^<(h[1-6]|ul|ol|blockquote|pre|hr|div)/);

            if (isBlockElement) {
                if (currentParagraph.length > 0) {
                    result += `<p>${currentParagraph.join('<br>')}</p>`;
                    currentParagraph = [];
                }
                result += line;
            } else if (line.trim() === '') {
                if (currentParagraph.length > 0) {
                    result += `<p>${currentParagraph.join('<br>')}</p>`;
                    currentParagraph = [];
                }
            } else {
                currentParagraph.push(line);
            }
        }

        if (currentParagraph.length > 0) {
            result += `<p>${currentParagraph.join('<br>')}</p>`;
        }

        result = result.replace(/<\/p><p>/g, '');

        return result.trim();
    }
}

class ListProcessor {
    constructor() {
        this.reset();
    }

    reset() {
        this.result = [];
        this.listStack = [];
    }

    process(lines) {
        this.reset();

        for (const line of lines) {
            this.processLine(line);
        }

        this.closeAllLists();
        return this.result.join('\n');
    }

    processLine(line) {
        const listMatch = this.parseListItem(line);

        if (listMatch) {
            this.processListItem(listMatch);
        } else {
            this.processNonListLine(line);
        }
    }

    parseListItem(line) {
        const unorderedMatch = line.match(/^(\s*)([*-]) (.*)$/);
        if (unorderedMatch) {
            return {
                indent: unorderedMatch[1].length,
                type: 'ul',
                content: unorderedMatch[3],
                marker: unorderedMatch[2]
            };
        }

        const orderedMatch = line.match(/^(\s*)(\d+)\. (.*)$/);
        if (orderedMatch) {
            return {
                indent: orderedMatch[1].length,
                type: 'ol',
                content: orderedMatch[3],
                marker: orderedMatch[2]
            };
        }

        return null;
    }

    processListItem(listMatch) {
        const {indent, type, content} = listMatch;
        const level = Math.floor(indent / 4);

        this.adjustListStack(level, type);

        if (this.listStack.length > 0) {
            const currentList = this.listStack[this.listStack.length - 1];
            currentList.items.push(`<li>${content}</li>`);
        }
    }

    adjustListStack(targetLevel, listType) {
        while (this.listStack.length > targetLevel + 1) {
            this.closeCurrentList();
        }

        if (this.listStack.length === targetLevel + 1) {
            const currentList = this.listStack[this.listStack.length - 1];
            if (currentList.type !== listType) {
                this.closeCurrentList();
                this.startNewList(listType, targetLevel);
            }
        } else if (this.listStack.length === targetLevel) {
            this.startNewList(listType, targetLevel);
        }
    }

    startNewList(listType, level) {
        const newList = {
            type: listType,
            level: level,
            items: []
        };
        this.listStack.push(newList);
    }

    closeCurrentList() {
        if (this.listStack.length === 0) return;

        const currentList = this.listStack.pop();
        const listHtml = `<${currentList.type}>${currentList.items.join('')}</${currentList.type}>`;

        if (this.listStack.length > 0) {
            const parentList = this.listStack[this.listStack.length - 1];
            if (parentList.items.length > 0) {
                const lastItemIndex = parentList.items.length - 1;
                const lastItem = parentList.items[lastItemIndex];
                parentList.items[lastItemIndex] = lastItem.replace('</li>', listHtml + '</li>');
            }
        } else {
            this.result.push(listHtml);
        }
    }

    closeAllLists() {
        while (this.listStack.length > 0) {
            this.closeCurrentList();
        }
    }

    processNonListLine(line) {
        this.closeAllLists();
        this.result.push(line);
    }
}

const DIALOG_STYLES = `
    .tz-markdown-content h1 { margin: 0; font-size: 2rem; line-height: 2.1rem; }
    .tz-markdown-content h2 { margin: 0; font-size: 1.7rem; line-height: 1.8rem; }
    .tz-markdown-content h3 { margin: 0; font-size: 1.4rem; line-height: 1.5rem; }
    .tz-markdown-content h4 { margin: 0; font-size: 1.1rem; line-height: 1.2rem; }
    .tz-markdown-content h5 { margin: 0; font-size: 1rem; line-height: 1.1rem; }
    .tz-markdown-content h6 { margin: 0; font-size: 0.8rem; line-height: 0.9rem; }
    .tz-markdown-content p { margin: 0; font-size: 1rem; line-height: 1.1rem; }
`;
