import { genUmdModule, genWrapperFunction } from "./generate";
export function generateParserFactory(options) {
    var wrapperText = genWrapperFunction({
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
export function generateParserModule(options) {
    return genUmdModule({ name: options.name, rules: options.rules });
}
//# sourceMappingURL=generate_public.js.map