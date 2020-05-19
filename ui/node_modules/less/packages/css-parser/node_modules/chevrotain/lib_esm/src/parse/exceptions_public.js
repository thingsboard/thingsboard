import { contains } from "../utils/utils";
var MISMATCHED_TOKEN_EXCEPTION = "MismatchedTokenException";
var NO_VIABLE_ALT_EXCEPTION = "NoViableAltException";
var EARLY_EXIT_EXCEPTION = "EarlyExitException";
var NOT_ALL_INPUT_PARSED_EXCEPTION = "NotAllInputParsedException";
var RECOGNITION_EXCEPTION_NAMES = [
    MISMATCHED_TOKEN_EXCEPTION,
    NO_VIABLE_ALT_EXCEPTION,
    EARLY_EXIT_EXCEPTION,
    NOT_ALL_INPUT_PARSED_EXCEPTION
];
Object.freeze(RECOGNITION_EXCEPTION_NAMES);
// hacks to bypass no support for custom Errors in javascript/typescript
export function isRecognitionException(error) {
    // can't do instanceof on hacked custom js exceptions
    return contains(RECOGNITION_EXCEPTION_NAMES, error.name);
}
export function MismatchedTokenException(message, token, previousToken) {
    this.name = MISMATCHED_TOKEN_EXCEPTION;
    this.message = message;
    this.token = token;
    this.previousToken = previousToken;
    this.resyncedTokens = [];
}
// must use the "Error.prototype" instead of "new Error"
// because the stack trace points to where "new Error" was invoked"
MismatchedTokenException.prototype = Error.prototype;
export function NoViableAltException(message, token, previousToken) {
    this.name = NO_VIABLE_ALT_EXCEPTION;
    this.message = message;
    this.token = token;
    this.previousToken = previousToken;
    this.resyncedTokens = [];
}
NoViableAltException.prototype = Error.prototype;
export function NotAllInputParsedException(message, token) {
    this.name = NOT_ALL_INPUT_PARSED_EXCEPTION;
    this.message = message;
    this.token = token;
    this.resyncedTokens = [];
}
NotAllInputParsedException.prototype = Error.prototype;
export function EarlyExitException(message, token, previousToken) {
    this.name = EARLY_EXIT_EXCEPTION;
    this.message = message;
    this.token = token;
    this.previousToken = previousToken;
    this.resyncedTokens = [];
}
EarlyExitException.prototype = Error.prototype;
//# sourceMappingURL=exceptions_public.js.map