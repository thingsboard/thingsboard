"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var generate_1 = require("./generate");
function generateParserFactory(options) {
    var wrapperText = generate_1.genWrapperFunction({
        name: options.name,
        rules: options.rules
    });
    var constructorWrapper = new Function("tokenVocabulary", "config", "chevrotain", wrapperText);
    return function (config) {
        return constructorWrapper(options.tokenVocabulary, config, 
        // TODO: check how the require is transpiled/webpacked
        require("../api"));
    };
}
exports.generateParserFactory = generateParserFactory;
function generateParserModule(options) {
    return generate_1.genUmdModule({ name: options.name, rules: options.rules });
}
exports.generateParserModule = generateParserModule;
//# sourceMappingURL=generate_public.js.map