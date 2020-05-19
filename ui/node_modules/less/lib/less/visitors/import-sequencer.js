class ImportSequencer {
    constructor(onSequencerEmpty) {
        this.imports = [];
        this.variableImports = [];
        this._onSequencerEmpty = onSequencerEmpty;
        this._currentDepth = 0;
    }

    addImport(callback) {
        const importSequencer = this;

        const importItem = {
            callback,
            args: null,
            isReady: false
        };

        this.imports.push(importItem);
        return function(...args) {
            importItem.args = Array.prototype.slice.call(args, 0);
            importItem.isReady = true;
            importSequencer.tryRun();
        };
    }

    addVariableImport(callback) {
        this.variableImports.push(callback);
    }

    tryRun() {
        this._currentDepth++;
        try {
            while (true) {
                while (this.imports.length > 0) {
                    const importItem = this.imports[0];
                    if (!importItem.isReady) {
                        return;
                    }
                    this.imports = this.imports.slice(1);
                    importItem.callback.apply(null, importItem.args);
                }
                if (this.variableImports.length === 0) {
                    break;
                }
                const variableImport = this.variableImports[0];
                this.variableImports = this.variableImports.slice(1);
                variableImport();
            }
        } finally {
            this._currentDepth--;
        }
        if (this._currentDepth === 0 && this._onSequencerEmpty) {
            this._onSequencerEmpty();
        }
    }
}

export default ImportSequencer;
