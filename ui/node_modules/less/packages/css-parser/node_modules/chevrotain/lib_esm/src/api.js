// semantic version
export { VERSION } from "./version";
export { Parser, CstParser, EmbeddedActionsParser, ParserDefinitionErrorType, EMPTY_ALT } from "./parse/parser/parser";
export { Lexer, LexerDefinitionErrorType } from "./scan/lexer_public";
// Tokens utilities
export { createToken, createTokenInstance, EOF, tokenLabel, tokenMatcher, tokenName } from "./scan/tokens_public";
// Other Utilities
export { defaultGrammarResolverErrorProvider, defaultGrammarValidatorErrorProvider, defaultParserErrorProvider } from "./parse/errors_public";
export { EarlyExitException, isRecognitionException, MismatchedTokenException, NotAllInputParsedException, NoViableAltException } from "./parse/exceptions_public";
export { defaultLexerErrorProvider } from "./scan/lexer_errors_public";
// grammar reflection API
export { Alternation, Flat, NonTerminal, Option, Repetition, RepetitionMandatory, RepetitionMandatoryWithSeparator, RepetitionWithSeparator, Rule, Terminal } from "./parse/grammar/gast/gast_public";
// GAST Utilities
export { serializeGrammar, serializeProduction } from "./parse/grammar/gast/gast_public";
export { GAstVisitor } from "./parse/grammar/gast/gast_visitor_public";
export { assignOccurrenceIndices, resolveGrammar, validateGrammar } from "./parse/grammar/gast/gast_resolver_public";
/* istanbul ignore next */
export function clearCache() {
    console.warn("The clearCache function was 'soft' removed from the Chevrotain API." +
        "\n\t It performs no action other than printing this message." +
        "\n\t Please avoid using it as it will be completely removed in the future");
}
export { createSyntaxDiagramsCode } from "./diagrams/render_public";
export { generateParserFactory, generateParserModule } from "./generate/generate_public";
//# sourceMappingURL=api.js.map