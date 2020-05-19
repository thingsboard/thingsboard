"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// semantic version
var version_1 = require("./version");
exports.VERSION = version_1.VERSION;
var parser_1 = require("./parse/parser/parser");
exports.Parser = parser_1.Parser;
exports.CstParser = parser_1.CstParser;
exports.EmbeddedActionsParser = parser_1.EmbeddedActionsParser;
exports.ParserDefinitionErrorType = parser_1.ParserDefinitionErrorType;
exports.EMPTY_ALT = parser_1.EMPTY_ALT;
var lexer_public_1 = require("./scan/lexer_public");
exports.Lexer = lexer_public_1.Lexer;
exports.LexerDefinitionErrorType = lexer_public_1.LexerDefinitionErrorType;
// Tokens utilities
var tokens_public_1 = require("./scan/tokens_public");
exports.createToken = tokens_public_1.createToken;
exports.createTokenInstance = tokens_public_1.createTokenInstance;
exports.EOF = tokens_public_1.EOF;
exports.tokenLabel = tokens_public_1.tokenLabel;
exports.tokenMatcher = tokens_public_1.tokenMatcher;
exports.tokenName = tokens_public_1.tokenName;
// Other Utilities
var errors_public_1 = require("./parse/errors_public");
exports.defaultGrammarResolverErrorProvider = errors_public_1.defaultGrammarResolverErrorProvider;
exports.defaultGrammarValidatorErrorProvider = errors_public_1.defaultGrammarValidatorErrorProvider;
exports.defaultParserErrorProvider = errors_public_1.defaultParserErrorProvider;
var exceptions_public_1 = require("./parse/exceptions_public");
exports.EarlyExitException = exceptions_public_1.EarlyExitException;
exports.isRecognitionException = exceptions_public_1.isRecognitionException;
exports.MismatchedTokenException = exceptions_public_1.MismatchedTokenException;
exports.NotAllInputParsedException = exceptions_public_1.NotAllInputParsedException;
exports.NoViableAltException = exceptions_public_1.NoViableAltException;
var lexer_errors_public_1 = require("./scan/lexer_errors_public");
exports.defaultLexerErrorProvider = lexer_errors_public_1.defaultLexerErrorProvider;
// grammar reflection API
var gast_public_1 = require("./parse/grammar/gast/gast_public");
exports.Alternation = gast_public_1.Alternation;
exports.Flat = gast_public_1.Flat;
exports.NonTerminal = gast_public_1.NonTerminal;
exports.Option = gast_public_1.Option;
exports.Repetition = gast_public_1.Repetition;
exports.RepetitionMandatory = gast_public_1.RepetitionMandatory;
exports.RepetitionMandatoryWithSeparator = gast_public_1.RepetitionMandatoryWithSeparator;
exports.RepetitionWithSeparator = gast_public_1.RepetitionWithSeparator;
exports.Rule = gast_public_1.Rule;
exports.Terminal = gast_public_1.Terminal;
// GAST Utilities
var gast_public_2 = require("./parse/grammar/gast/gast_public");
exports.serializeGrammar = gast_public_2.serializeGrammar;
exports.serializeProduction = gast_public_2.serializeProduction;
var gast_visitor_public_1 = require("./parse/grammar/gast/gast_visitor_public");
exports.GAstVisitor = gast_visitor_public_1.GAstVisitor;
var gast_resolver_public_1 = require("./parse/grammar/gast/gast_resolver_public");
exports.assignOccurrenceIndices = gast_resolver_public_1.assignOccurrenceIndices;
exports.resolveGrammar = gast_resolver_public_1.resolveGrammar;
exports.validateGrammar = gast_resolver_public_1.validateGrammar;
/* istanbul ignore next */
function clearCache() {
    console.warn("The clearCache function was 'soft' removed from the Chevrotain API." +
        "\n\t It performs no action other than printing this message." +
        "\n\t Please avoid using it as it will be completely removed in the future");
}
exports.clearCache = clearCache;
var render_public_1 = require("./diagrams/render_public");
exports.createSyntaxDiagramsCode = render_public_1.createSyntaxDiagramsCode;
var generate_public_1 = require("./generate/generate_public");
exports.generateParserFactory = generate_public_1.generateParserFactory;
exports.generateParserModule = generate_public_1.generateParserModule;
//# sourceMappingURL=api.js.map