export as namespace chevrotain

export declare const VERSION: string

/**
 * This class does not actually exists nor is exposed at runtime.
 * This is just a helper to avoid duplications in the Type Definitions
 * Of `CstParser` and `EmbeddedActionsParser`
 */
declare abstract class BaseParser {
    /**
     * This must be called at the end of a Parser constructor.
     * See: http://sap.github.io/chevrotain/docs/tutorial/step2_parsing.html#under-the-hood
     */
    protected performSelfAnalysis(): void

    /**
     * It is recommended to reuse the same Parser instance
     * by passing an empty array to the input argument
     * and only later setting the input by using the input property.
     * See: http://sap.github.io/chevrotain/docs/FAQ.html#major-performance-benefits
     *
     * @param tokenVocabulary - A data structure containing all the Tokens used by the Parser.
     * @param config - The Parser's configuration.
     */
    constructor(tokenVocabulary: TokenVocabulary, config?: IParserConfig)

    errors: IRecognitionException[]

    /**
     * Flag indicating the Parser is at the recording phase.
     * Can be used to implement methods similar to {@link BaseParser.ACTION}
     * Or any other logic to requires knowledge of the recording phase.
     * See:
     *   - https://sap.github.io/chevrotain/docs/guide/internals.html#grammar-recording
     * to learn more on the recording phase and how Chevrotain works.
     */
    RECORDING_PHASE: boolean

    /**
     * Resets the parser state, should be overridden for custom parsers which "carry" additional state.
     * When overriding, remember to also invoke the super implementation!
     */
    reset(): void

    getBaseCstVisitorConstructor(): {
        new (...args: any[]): ICstVisitor<any, any>
    }

    getBaseCstVisitorConstructorWithDefaults(): {
        new (...args: any[]): ICstVisitor<any, any>
    }

    getGAstProductions(): Record<string, Rule>

    getSerializedGastProductions(): ISerializedGast[]

    /**
     * @param startRuleName
     * @param precedingInput - The token vector up to (not including) the content assist point
     */
    computeContentAssist(
        startRuleName: string,
        precedingInput: IToken[]
    ): ISyntacticContentAssistPath[]

    /**
     * @param grammarRule - The rule to try and parse in backtracking mode.
     * @param args - argumens to be passed to the grammar rule execution
     *
     * @return a lookahead function that will try to parse the given grammarRule and will return true if succeed.
     */
    protected BACKTRACK<T>(
        grammarRule: (...args: any[]) => T,
        args?: any[]
    ): () => boolean

    /**
     * The Semantic Actions wrapper.
     * Should be used to wrap semantic actions that either:
     * - May fail when executing in "recording phase".
     * - Have global side effects that should be avoided during "recording phase".
     *
     * For more information see:
     *   - https://sap.github.io/chevrotain/docs/guide/internals.html#grammar-recording
     */
    protected ACTION<T>(impl: () => T): T

    /**
     * Like `CONSUME` with the numerical suffix as a parameter, e.g:
     * consume(0, X) === CONSUME(X)
     * consume(1, X) === CONSUME1(X)
     * consume(2, X) === CONSUME2(X)
     * ...
     * @see CONSUME
     */
    protected consume(
        idx: number,
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * Like `OPTION` with the numerical suffix as a parameter, e.g:
     * option(0, X) === OPTION(X)
     * option(1, X) === OPTION1(X)
     * option(2, X) === OPTION2(X)
     * ...
     * @see SUBRULE
     */
    protected option<OUT>(
        idx: number,
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * Like `OR` with the numerical suffix as a parameter, e.g:
     * or(0, X) === OR(X)
     * or(1, X) === OR1(X)
     * or(2, X) === OR2(X)
     * ...
     * @see OR
     */
    protected or(idx: number, altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * Like `MANY` with the numerical suffix as a parameter, e.g:
     * many(0, X) === MANY(X)
     * many(1, X) === MANY1(X)
     * many(2, X) === MANY2(X)
     * ...
     * @see MANY
     */
    protected many(
        idx: number,
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * Like `AT_LEAST_ONE` with the numerical suffix as a parameter, e.g:
     * atLeastOne(0, X) === AT_LEAST_ONE(X)
     * atLeastOne(1, X) === AT_LEAST_ONE1(X)
     * atLeastOne(2, X) === AT_LEAST_ONE2(X)
     * ...
     * @see AT_LEAST_ONE
     */
    protected atLeastOne(
        idx: number,
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     *
     * A Parsing DSL method use to consume a single Token.
     * In EBNF terms this is equivalent to a Terminal.
     *
     * A Token will be consumed, IFF the next token in the token vector matches <tokType>.
     * otherwise the parser may attempt to perform error recovery (if enabled).
     *
     * The index in the method name indicates the unique occurrence of a terminal consumption
     * inside a the top level rule. What this means is that if a terminal appears
     * more than once in a single rule, each appearance must have a **different** index.
     *
     * For example:
     * ```
     *   this.RULE("qualifiedName", () => {
     *   this.CONSUME1(Identifier);
     *     this.MANY(() => {
     *       this.CONSUME1(Dot);
     *       // here we use CONSUME2 because the terminal
     *       // 'Identifier' has already appeared previously in the
     *       // the rule 'parseQualifiedName'
     *       this.CONSUME2(Identifier);
     *     });
     *   })
     * ```
     *
     * - See more details on the [unique suffixes requirement](http://sap.github.io/chevrotain/docs/FAQ.html#NUMERICAL_SUFFIXES).
     *
     * @param tokType - The Type of the token to be consumed.
     * @param options - optional properties to modify the behavior of CONSUME.
     */
    protected CONSUME(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME1(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME2(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME3(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME4(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME5(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME6(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME7(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME8(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * @see CONSUME
     * @hidden
     */
    protected CONSUME9(
        tokType: TokenType,
        options?: ConsumeMethodOpts
    ): IToken

    /**
     * Parsing DSL Method that Indicates an Optional production.
     * in EBNF notation this is equivalent to: "[...]".
     *
     * Note that there are two syntax forms:
     * - Passing the grammar action directly:
     *   ```
     *     this.OPTION(() => {
     *       this.CONSUME(Digit)}
     *     );
     *   ```
     *
     * - using an "options" object:
     *   ```
     *     this.OPTION({
     *       GATE:predicateFunc,
     *       DEF: () => {
     *         this.CONSUME(Digit)
     *     }});
     *   ```
     *
     * The optional 'GATE' property in "options" object form can be used to add constraints
     * to invoking the grammar action.
     *
     * As in CONSUME the index in the method name indicates the occurrence
     * of the optional production in it's top rule.
     *
     * @param  actionORMethodDef - The grammar action to optionally invoke once
     *                             or an "OPTIONS" object describing the grammar action and optional properties.
     */
    // TODO: return `OUT | undefined` explicitly
    protected OPTION<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION1<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION2<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION3<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION4<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION5<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION6<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION7<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION8<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * @see OPTION
     * @hidden
     */
    protected OPTION9<OUT>(
        actionORMethodDef: GrammarAction<OUT> | DSLMethodOpts<OUT>
    ): OUT

    /**
     * Parsing DSL method that indicates a choice between a set of alternatives must be made.
     * This is equivalent to an EBNF alternation (A | B | C | D ...), except
     * that the alternatives are ordered like in a PEG grammar.
     * This means that the **first** matching alternative is always chosen.
     *
     * There are several forms for the inner alternatives array:
     *
     * - Passing alternatives array directly:
     *   ```
     *     this.OR([
     *       { ALT:() => { this.CONSUME(One) }},
     *       { ALT:() => { this.CONSUME(Two) }},
     *       { ALT:() => { this.CONSUME(Three) }}
     *     ])
     *   ```
     *
     * - Passing alternative array directly with predicates (GATE):
     *   ```
     *     this.OR([
     *       { GATE: predicateFunc1, ALT:() => { this.CONSUME(One) }},
     *       { GATE: predicateFuncX, ALT:() => { this.CONSUME(Two) }},
     *       { GATE: predicateFuncX, ALT:() => { this.CONSUME(Three) }}
     *     ])
     *   ```
     *
     * - These syntax forms can also be mixed:
     *   ```
     *     this.OR([
     *       {
     *         GATE: predicateFunc1,
     *         ALT:() => { this.CONSUME(One) }
     *       },
     *       { ALT:() => { this.CONSUME(Two) }},
     *       { ALT:() => { this.CONSUME(Three) }}
     *     ])
     *   ```
     *
     * - Additionally an "options" object may be used:
     *   ```
     *     this.OR({
     *       DEF:[
     *         { ALT:() => { this.CONSUME(One) }},
     *         { ALT:() => { this.CONSUME(Two) }},
     *         { ALT:() => { this.CONSUME(Three) }}
     *       ],
     *       // OPTIONAL property
     *       ERR_MSG: "A Number"
     *     })
     *   ```
     *
     * The 'predicateFuncX' in the long form can be used to add constraints to choosing the alternative.
     *
     * As in CONSUME the index in the method name indicates the occurrence
     * of the alternation production in it's top rule.
     *
     * @param altsOrOpts - A set of alternatives or an "OPTIONS" object describing the alternatives and optional properties.
     *
     * @returns The result of invoking the chosen alternative.
     */
    protected OR(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR1(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR2(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR3(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR4(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR5(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR6(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR7(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR8(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * @see OR
     * @hidden
     */
    protected OR9(altsOrOpts: IOrAlt[] | OrMethodOpts): any

    /**
     * Parsing DSL method, that indicates a repetition of zero or more.
     * This is equivalent to EBNF repetition {...}.
     *
     * Note that there are two syntax forms:
     * - Passing the grammar action directly:
     *   ```
     *     this.MANY(() => {
     *       this.CONSUME(Comma)
     *       this.CONSUME(Digit)
     *      })
     *   ```
     *
     * - using an "options" object:
     *   ```
     *     this.MANY({
     *       GATE: predicateFunc,
     *       DEF: () => {
     *              this.CONSUME(Comma)
     *              this.CONSUME(Digit)
     *            }
     *     });
     *   ```
     *
     * The optional 'GATE' property in "options" object form can be used to add constraints
     * to invoking the grammar action.
     *
     * As in CONSUME the index in the method name indicates the occurrence
     * of the repetition production in it's top rule.
     *
     * @param actionORMethodDef - The grammar action to optionally invoke multiple times
     *                             or an "OPTIONS" object describing the grammar action and optional properties.
     *
     */
    protected MANY(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY1(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY2(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY3(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY4(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY5(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY6(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY7(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY8(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * @see MANY
     * @hidden
     */
    protected MANY9(
        actionORMethodDef: GrammarAction<any> | DSLMethodOpts<any>
    ): void

    /**
     * Parsing DSL method, that indicates a repetition of zero or more with a separator
     * Token between the repetitions.
     *
     * Example:
     *
     * ```
     *     this.MANY_SEP({
     *         SEP:Comma,
     *         DEF: () => {
     *             this.CONSUME(Number};
     *             // ...
     *         })
     * ```
     *
     * Note that because this DSL method always requires more than one argument the options object is always required
     * and it is not possible to use a shorter form like in the MANY DSL method.
     *
     * Note that for the purposes of deciding on whether or not another iteration exists
     * Only a single Token is examined (The separator). Therefore if the grammar being implemented is
     * so "crazy" to require multiple tokens to identify an item separator please use the more basic DSL methods
     * to implement it.
     *
     * As in CONSUME the index in the method name indicates the occurrence
     * of the repetition production in it's top rule.
     *
     * @param options - An object defining the grammar of each iteration and the separator between iterations
     *
     */
    protected MANY_SEP(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP1(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP2(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP3(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP4(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP5(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP6(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP7(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP8(options: ManySepMethodOpts<any>): void

    /**
     * @see MANY_SEP
     * @hidden
     */
    protected MANY_SEP9(options: ManySepMethodOpts<any>): void

    /**
     * Convenience method, same as MANY but the repetition is of one or more.
     * failing to match at least one repetition will result in a parsing error and
     * cause a parsing error.
     *
     * @see MANY
     *
     * @param actionORMethodDef  - The grammar action to optionally invoke multiple times
     *                             or an "OPTIONS" object describing the grammar action and optional properties.
     *
     */
    protected AT_LEAST_ONE(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE1(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE2(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE3(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE4(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE5(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE6(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE7(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE8(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * @see AT_LEAST_ONE
     * @hidden
     */
    protected AT_LEAST_ONE9(
        actionORMethodDef: GrammarAction<any> | DSLMethodOptsWithErr<any>
    ): void

    /**
     * Convenience method, same as MANY_SEP but the repetition is of one or more.
     * failing to match at least one repetition will result in a parsing error and
     * cause the parser to attempt error recovery.
     *
     * Note that an additional optional property ERR_MSG can be used to provide custom error messages.
     *
     * @see MANY_SEP
     *
     * @param options - An object defining the grammar of each iteration and the separator between iterations
     *
     * @return {ISeparatedIterationResult<OUT>}
     */
    protected AT_LEAST_ONE_SEP(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP1(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP2(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP3(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP4(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP5(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP6(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP7(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP8(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * @see AT_LEAST_ONE_SEP
     * @hidden
     */
    protected AT_LEAST_ONE_SEP9(
        options: AtLeastOneSepMethodOpts<any>
    ): void

    /**
     * Returns an "imaginary" Token to insert when Single Token Insertion is done
     * Override this if you require special behavior in your grammar.
     * For example if an IntegerToken is required provide one with the image '0' so it would be valid syntactically.
     */
    protected getTokenToInsert(tokType: TokenType): IToken

    /**
     * By default all tokens type may be inserted. This behavior may be overridden in inheriting Recognizers
     * for example: One may decide that only punctuation tokens may be inserted automatically as they have no additional
     * semantic value. (A mandatory semicolon has no additional semantic meaning, but an Integer may have additional meaning
     * depending on its int value and context (Inserting an integer 0 in cardinality: "[1..]" will cause semantic issues
     * as the max of the cardinality will be greater than the min value (and this is a false error!).
     */
    protected canTokenTypeBeInsertedInRecovery(
        tokType: TokenType
    ): boolean

    /**
     * @deprecated - will be removed in the future
     */
    protected getNextPossibleTokenTypes(
        grammarPath: ITokenGrammarPath
    ): TokenType[]

    input: IToken[]

    protected SKIP_TOKEN(): IToken

    /**
     * Look-Ahead for the Token Vector
     * LA(1) is the next Token ahead.
     * LA(n) is the nth Token ahead.
     * LA(0) is the previously consumed Token.
     *
     * Looking beyond the end of the Token Vector or before its begining
     * will return in an IToken of type EOF {@link EOF}.
     * This behavior can be used to avoid infinite loops.
     *
     * This is often used to implement custom lookahead logic for GATES.
     * https://sap.github.io/chevrotain/docs/features/gates.html
     */
    protected LA(howMuch: number): IToken
}

/**
 * @deprecated Soft deprecated, CstParser or EmbeddedActionsParser instead.
 *    - {@link CstParser}
 *    - {@link EmbeddedActionsParser}
 */
export declare class Parser extends BaseParser {
    /**
     * @deprecated use {@link Parser.performSelfAnalysis} **instance** method instead.
     */
    protected static performSelfAnalysis(parserInstance: Parser): void

    /**
     *
     * @param name - The name of the rule.
     * @param implementation - The implementation of the rule.
     * @param [config] - The rule's optional configuration.
     *
     * @returns - The parsing rule which is the production implementation wrapped with the parsing logic that handles
     *                     Parser state / error recovery&reporting/ ...
     */
    protected RULE<T>(
        name: string,
        implementation: (...implArgs: any[]) => T,
        config?: IRuleConfig<T>
    ): (idxInCallingRule?: number, ...args: any[]) => T | any

    /**
     * Same as {@link Parser.RULE}, but should only be used in
     * "extending" grammars to override rules/productions from the super grammar.
     * See [Parser Inheritance Example](https://github.com/SAP/chevrotain/tree/master/examples/parser/inheritance).
     */
    protected OVERRIDE_RULE<T>(
        name: string,
        impl: (...implArgs: any[]) => T,
        config?: IRuleConfig<T>
    ): (idxInCallingRule?: number, ...args: any[]) => T | any

    protected SUBRULE<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE1<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE2<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE3<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE4<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE5<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE6<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE7<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE8<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE9<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T
}

/**
 * A Parser that outputs a Concrete Syntax Tree.
 * See:
 *    - https://sap.github.io/chevrotain/docs/tutorial/step3_adding_actions_root.html#alternatives
 *    - https://sap.github.io/chevrotain/docs/guide/concrete_syntax_tree.html
 * For in depth docs.
 */
export declare class CstParser extends BaseParser {
    // TODO: remove `outputCST` from the config options in the constructor
    /**
     * @deprecated use {@link Parser.performSelfAnalysis} **instance** method instead.
     */
    protected static performSelfAnalysis(parserInstance: Parser): void

    /**
     * Creates a Grammar Rule
     */
    protected RULE(
        name: string,
        implementation: (...implArgs: any[]) => any,
        config?: IRuleConfig<CstNode>
    ): (idxInCallingRule?: number, ...args: any[]) => CstNode

    /**
     * Overrides a Grammar Rule
     * See usage example in: https://github.com/SAP/chevrotain/blob/master/examples/parser/versioning/versioning.js
     */
    protected OVERRIDE_RULE<T>(
        name: string,
        implementation: (...implArgs: any[]) => any,
        config?: IRuleConfig<CstNode>
    ): (idxInCallingRule?: number, ...args: any[]) => CstNode

    /**
     * Like `SUBRULE` with the numerical suffix as a parameter, e.g:
     * subrule(0, X) === SUBRULE(X)
     * subrule(1, X) === SUBRULE1(X)
     * subrule(2, X) === SUBRULE2(X)
     * ...
     * @see SUBRULE
     */
    protected subrule(
        idx: number,
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * The Parsing DSL Method is used by one rule to call another.
     * It is equivalent to a non-Terminal in EBNF notation.
     *
     * This may seem redundant as it does not actually do much.
     * However using it is **mandatory** for all sub rule invocations.
     *
     * Calling another rule without wrapping in SUBRULE(...)
     * will cause errors/mistakes in the Parser's self analysis phase,
     * which will lead to errors in error recovery/automatic lookahead calculation
     * and any other functionality relying on the Parser's self analysis
     * output.
     *
     * As in CONSUME the index in the method name indicates the occurrence
     * of the sub rule invocation in its rule.
     *
     */
    protected SUBRULE(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE1(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE2(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE3(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE4(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE5(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE6(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE7(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE8(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE9(
        ruleToCall: (idx: number) => CstNode,
        options?: SubruleMethodOpts
    ): CstNode
}

/**
 * A Parser that relies on end user's embedded actions to control its output.
 * For more details see:
 *   - https://sap.github.io/chevrotain/docs/tutorial/step3_adding_actions_root.html#alternatives
 *   - https://sap.github.io/chevrotain/docs/tutorial/step3b_adding_actions_embedded.html#simple-example
 */
export declare class EmbeddedActionsParser extends BaseParser {
    /**
     * @deprecated use {@link Parser.performSelfAnalysis} **instance** method instead.
     */
    protected static performSelfAnalysis(parserInstance: Parser): void

    // TODO: remove `outputCST` from the config options in the constructor

    /**
     * Creates a Grammar Rule
     */
    protected RULE<T>(
        name: string,
        implementation: (...implArgs: any[]) => T,
        config?: IRuleConfig<T>
    ): (idxInCallingRule?: number, ...args: any[]) => T

    /**
     * Overrides a Grammar Rule
     * See usage example in: https://github.com/SAP/chevrotain/blob/master/examples/parser/versioning/versioning.js
     */
    protected OVERRIDE_RULE<T>(
        name: string,
        impl: (...implArgs: any[]) => T,
        config?: IRuleConfig<T>
    ): (idxInCallingRule?: number, ...args: any[]) => T

    /**
     * Like `SUBRULE` with the numerical suffix as a parameter, e.g:
     * subrule(0, X) === SUBRULE(X)
     * subrule(1, X) === SUBRULE1(X)
     * subrule(2, X) === SUBRULE2(X)
     * ...
     * @see SUBRULE
     */
    protected subrule<T>(
        idx: number,
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * The Parsing DSL Method is used by one rule to call another.
     * It is equivalent to a non-Terminal in EBNF notation.
     *
     * This may seem redundant as it does not actually do much.
     * However using it is **mandatory** for all sub rule invocations.
     *
     * Calling another rule without wrapping in SUBRULE(...)
     * will cause errors/mistakes in the Parser's self analysis phase,
     * which will lead to errors in error recovery/automatic lookahead calculation
     * and any other functionality relying on the Parser's self analysis
     * output.
     *
     * As in CONSUME the index in the method name indicates the occurrence
     * of the sub rule invocation in its rule.
     *
     */
    protected SUBRULE<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE1<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE2<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE3<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE4<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE5<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE6<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE7<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE8<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T

    /**
     * @see SUBRULE
     * @hidden
     */
    protected SUBRULE9<T>(
        ruleToCall: (idx: number) => T,
        options?: SubruleMethodOpts
    ): T
}

export declare enum ParserDefinitionErrorType {
    INVALID_RULE_NAME = 0,
    DUPLICATE_RULE_NAME = 1,
    INVALID_RULE_OVERRIDE = 2,
    DUPLICATE_PRODUCTIONS = 3,
    UNRESOLVED_SUBRULE_REF = 4,
    LEFT_RECURSION = 5,
    NONE_LAST_EMPTY_ALT = 6,
    AMBIGUOUS_ALTS = 7,
    CONFLICT_TOKENS_RULES_NAMESPACE = 8,
    INVALID_TOKEN_NAME = 9,
    INVALID_NESTED_RULE_NAME = 10,
    DUPLICATE_NESTED_NAME = 11,
    NO_NON_EMPTY_LOOKAHEAD = 12,
    AMBIGUOUS_PREFIX_ALTS = 13,
    TOO_MANY_ALTS = 14
}

export interface ILexerDefinitionError {
    message: string
    type: LexerDefinitionErrorType
    tokenTypes?: TokenType[]
}

export declare class Lexer {
    static SKIPPED: string

    /**
     * A Constant to mark "abstract" TokenTypes that are used
     * purely as token categories.
     * See: {@link createToken.categories}
     */
    static NA: RegExp
    lexerDefinitionErrors: ILexerDefinitionError[]

    /**
     * @param lexerDefinition -
     *  Structure composed of Tokens Types this lexer will identify.
     *
     *  In the simple case the structure is an array of TokenTypes.
     *  In the case of {@link IMultiModeLexerDefinition} the structure is an object with two properties:
     *    1. a "modes" property where each value is an array of TokenTypes.
     *    2. a "defaultMode" property specifying the initial lexer mode.
     *
     *  for example:
     *
     *  ```
     *    {
     *        modes : {
     *          modeX : [Token1, Token2],
     *          modeY : [Token3, Token4]
     *        },
     *
     *        defaultMode : "modeY"
     *    }
     *  ```
     *
     *  A lexer with {@link MultiModesDefinition} is simply multiple Lexers where only one Lexer(mode) can be active at the same time.
     *  This is useful for lexing languages where there are different lexing rules depending on context.
     *
     *  The current lexing mode is selected via a "mode stack".
     *  The last (peek) value in the stack will be the current mode of the lexer.
     *  Defining entering and exiting lexer modes is done using the "push_mode" and "pop_mode" properites
     *  of the {@link createToken.config} parameter.
     *
     *  - The Lexer will match the **first** pattern that matches, Therefor the order of Token Types is significant.
     *    For example when one pattern may match a prefix of another pattern.
     *
     *    Note that there are situations in which we may wish to order the longer pattern after the shorter one.
     *    For example: [keywords vs Identifiers](https://github.com/SAP/chevrotain/tree/master/examples/lexer/keywords_vs_identifiers).
     */
    constructor(
        lexerDefinition: TokenType[] | IMultiModeLexerDefinition,
        config?: ILexerConfig
    )

    /**
     * Will lex(Tokenize) a string.
     * Note that this can be called repeatedly on different strings as this method
     * does not modify the state of the Lexer.
     *
     * @param text - The string to lex
     * @param [initialMode] - The initial Lexer Mode to start with, by default this will be the first mode in the lexer's
     *                                 definition. If the lexer has no explicit modes it will be the implicit single 'default_mode' mode.
     */
    tokenize(text: string, initialMode?: string): ILexingResult
}

export interface ILexingResult {
    tokens: IToken[]
    groups: {
        [groupName: string]: IToken[]
    }
    errors: ILexingError[]
}

export interface ILexingError {
    offset: number
    line: number
    column: number
    length: number
    message: string
}

export interface ILexerConfig {
    /**
     * An optional flag indicating that lexer definition errors
     * should not automatically cause an error to be raised.
     * This can be useful when wishing to indicate lexer errors in another manner
     * than simply throwing an error (for example in an online playground).
     */
    deferDefinitionErrorsHandling?: boolean

    /**
     * "full" location information means all six combinations of /(end|start)(Line|Column|Offset)/ properties.
     * "onlyStart" means that only startLine, startColumn and startOffset will be tracked
     * "onlyOffset" means that only the startOffset will be tracked.
     *
     * The less position tracking the faster the Lexer will be and the less memory used.
     * However the difference is not large (~10% On V8), thus reduced location tracking options should only be used
     * in edge cases where every last ounce of performance is needed.
     */
    // TODO: consider renaming this to LocationTracking to align with NodeLocationTracking option on the ParserConfig.
    positionTracking?: "full" | "onlyStart" | "onlyOffset"

    /**
     * A regExp defining custom line terminators.
     * This will be used to calculate the line and column information.
     *
     * Note that the regExp should use the global flag, for example: /\n/g
     *
     * The default is: /\n|\r\n?/g
     *
     * But some grammars have a different definition, for example in ECMAScript:
     * https://www.ecma-international.org/ecma-262/8.0/index.html#sec-line-terminators
     * U+2028 and U+2029 are also treated as line terminators.
     *
     * In that case we would use /\n|\r|\u2028|\u2029/g
     *
     * Note that it is also possible to supply an optimized RegExp like implementation
     * as only a subset of the RegExp APIs is needed, {@link ILineTerminatorsTester}
     * for details.
     *
     * keep in mind that for the default pattern: /\n|\r\n?/g an optimized implementation is already built-in.
     * This means the optimization is only relevant for lexers overriding the default pattern.
     */
    lineTerminatorsPattern?: RegExp | ILineTerminatorsTester

    /**
     * Characters or CharCodes that represent line terminators for this lexer.
     * This always needs to be provided when using a custom {@link ILexerConfig.lineTerminatorsPattern}.
     * In the future this duplication may be removed or reduced.
     */
    lineTerminatorCharacters?: (number | string)[]

    /**
     * When true this flag will cause the Lexer to throw an Error
     * When it is unable to perform all of its performance optimizations.
     *
     * In addition error messages will be printed to the console with details
     * how to resolve the optimizations issues.
     *
     * Use this flag to guarantee higher lexer performance.
     * The optimizations can boost the lexer's performance anywhere from 30%
     * to 100%+ depending on the number of TokenTypes used.
     */
    ensureOptimizations?: boolean

    /**
     * Can be used to disable lexer optimizations
     * If there is a suspicion they are causing incorrect behavior.
     * Note that this would have negative performance implications.
     */
    safeMode?: boolean

    /**
     * A custom error message provider.
     * Can be used to override the default error messages.
     * For example:
     *   - Translating the error messages to a different languages.
     *   - Changing the formatting.
     */
    errorMessageProvider?: ILexerErrorMessageProvider

    /**
     * Enabling this flag will print performance tracing logs during lexer
     * Initialization (constructor invocation), this is useful to narrow down the cause
     * of the initialization performance problem.
     *
     * You can also pass a numerical value which affects the verbosity
     * of the traces, this number is the maximum nesting level of the traces, e.g:
     * 0: Traces disabled === 'false'
     * 1: Top Level traces only.
     * 2: One level of nested inner traces.
     * ...
     *
     * Note that passing the boolean `true` is identical to passing the numerical value `infinity`
     */
    traceInitPerf?: boolean | number

    /**
     * This flag will avoid running the Lexer validations during Lexer initialization.
     *
     * This can substantially improve the Lexer's initialization (constructor) time.
     * @see ILexerConfig.traceInitPerf to measure the Lexer validations cost for your Lexer.
     *
     * Note that the Lexer validations are **extremely useful** during development time,
     * e.g: Detecting empty/invalid regExp Patterns.
     * So they should not be skipped during development flows.
     *   - For example: via a conditional that checks an env variable.
     */
    skipValidations?: boolean
}

export interface ILexerErrorMessageProvider {
    /**
     * An Unexpected Character Error occurs when the lexer is unable to match a range of one or more
     * characters in the input text against any of the Token Types in it's Lexer definition
     *
     * @param fullText - Full original input text.
     *
     * @param startOffset - Offset in input text where error starts.
     *
     * @param length - Error length.
     *
     * @param line - Line number where the error occured. (optional)
     *                    Will not be provided when lexer is not defined to track lines/columns
     *
     * @param column - Column number where the error occured. (optional)
     *                    Will not be provided when lexer is not defined to track lines/columns
     */
    buildUnexpectedCharactersMessage(
        fullText: string,
        startOffset: number,
        length: number,
        line?: number,
        column?: number
    ): string

    /**
     * Unable To Pop Lexer Mode Error happens when lexer tries to pop the last remaining mode from the mode stack
     * so that there is no longer any active lexer mode
     * This error only relevant for multi-mode lexers
     *
     * @param token - The Token that requested pop mode.
     */
    buildUnableToPopLexerModeMessage(token: IToken): string
}

/**
 * This is the default logic Chevrotain uses to construct lexing error messages.
 * It can be used as a reference or as a starting point customize a lexer's
 * error messages.
 *
 * - See: {@link ILexerConfig.errorMessageProvider}
 */
export declare const defaultLexerErrorProvider: ILexerErrorMessageProvider

/**
 * A subset of the regExp interface.
 * Needed to compute line/column info by a chevrotain lexer.
 */
export interface ILineTerminatorsTester {
    /**
     * Just like regExp.test
     */
    test: (text: string) => boolean

    /**
     * Just like the regExp lastIndex with the global flag enabled
     * It should be updated after every match to point to the offset where the next
     * match attempt starts.
     */
    lastIndex: number
}

export type TokenPattern =
    | RegExp
    | string
    | CustomPatternMatcherFunc
    | ICustomPattern

export interface ITokenConfig {
    name: string

    /**
     * Categories enable polymorphism on Token Types.
     * A TokenType X with categories C1, C2, ... ,Cn can
     * be matched by the parser against any of those categories.
     * In practical terms this means that:
     * CONSUME(C1) can match a Token of type X.
     */
    categories?: TokenType | TokenType[]

    /**
     * The Label is a human readable name to be used
     * in error messages and syntax diagrams.
     *
     * For example a TokenType may be called LCurly, which is
     * short for "left curly brace". The much easier to understand
     * label could simply be "{".
     */
    label?: string

    /**
     * This defines what sequence of characters would be matched
     * To this TokenType when Lexing.
     *
     * For Custom Patterns see: http://sap.github.io/chevrotain/docs/guide/custom_token_patterns.html
     */
    pattern?: TokenPattern

    /**
     * The group property will cause the lexer to collect
     * Tokens of this type separately from the other Tokens.
     *
     * For example this could be used to collect comments for
     * post processing.
     *
     * See: https://github.com/SAP/chevrotain/tree/master/examples/lexer/token_groups
     */
    group?: string

    /**
     * A name of a Lexer mode to "enter" once this Token Type has been matched.
     * Lexer modes can be used to support different sets of possible Tokens Types
     *
     * Lexer Modes work as a stack of Lexers, so "entering" a mode means pushing it to the top of the stack.
     *
     * See: https://github.com/SAP/chevrotain/tree/master/examples/lexer/multi_mode_lexer
     */
    push_mode?: string

    /**
     * If "pop_mode" is true the Lexer will pop the last mode of the modes stack and
     * continue lexing using the new mode at the top of the stack.
     *
     * See: https://github.com/SAP/chevrotain/tree/master/examples/lexer/multi_mode_lexer
     */
    pop_mode?: boolean

    /**
     * The "longer_alt" property will cause the Lexer to attempt matching against another Token Type
     * every time this Token Type has been matched.
     *
     * This feature can be useful when two Token Types have common prefixes which
     * cannot be resolved (only) by the ordering of the Tokens in the lexer definition.
     *
     * For example see: https://github.com/SAP/chevrotain/tree/master/examples/lexer/keywords_vs_identifiers
     * For resolving the keywords vs Identifier ambiguity.
     */
    longer_alt?: TokenType

    /**
     * Can a String matching this Token Type's pattern possibly contain a line terminator?
     * If true and the line_breaks property is not also true this will cause inaccuracies in the Lexer's line / column tracking.
     */
    line_breaks?: boolean

    /**
     * Possible starting characters or charCodes of the pattern.
     * These will be used to optimize the Lexer's performance.
     *
     * These are normally **automatically** computed, however the option to explicitly
     * specify those can enable optimizations even when the automatic analysis fails.
     *
     * e.g:
     *   * strings hints should be one character long.
     *    ```
     *      { start_chars_hint: ["a", "b"] }
     *    ```
     *
     *   * number hints are the result of running ".charCodeAt(0)" on the strings.
     *    ```
     *      { start_chars_hint: [97, 98] }
     *    ```
     *
     *   * For unicode characters outside the BMP use the first of their surrogate pairs.
     *     for example: The '💩' character is represented by surrogate pairs: '\uD83D\uDCA9'
     *       and D83D is 55357 in decimal.
     *    * Note that "💩".charCodeAt(0) === 55357
     */
    start_chars_hint?: (string | number)[]
}

/**
 * Creates a new TokenType which can then be used
 * to define a Lexer and Parser
 */
export declare function createToken(config: ITokenConfig): TokenType

/**
 * Utility to create Chevrotain IToken "instances"
 * Note that Chevrotain tokens are not real TokenTypes instances
 * and thus the instanceOf cannot be used with them.
 */
export declare function createTokenInstance(
    tokType: TokenType,
    image: string,
    startOffset: number,
    endOffset: number,
    startLine: number,
    endLine: number,
    startColumn: number,
    endColumn: number
): IToken

/**
 *  API #1 [Custom Token Patterns](http://sap.github.io/chevrotain/docs/guide/custom_token_patterns.html).
 */
export declare type CustomPatternMatcherFunc = (
    /**
     * The full input string.
     */
    text: string,
    /**
     * The offset at which to attempt a match
     */
    offset: number,
    /**
     * Previously scanned Tokens
     */
    tokens: IToken[],
    /**
     * Token Groups
     */
    groups: {
        [groupName: string]: IToken[]
    }
) => CustomPatternMatcherReturn | RegExpExecArray | null // RegExpExecArray included for legacy reasons

export type CustomPatternMatcherReturn = [string] & { payload?: any }

export interface TokenType {
    name: string
    GROUP?: string
    PATTERN?: TokenPattern
    LABEL?: string
    LONGER_ALT?: TokenType
    POP_MODE?: boolean
    PUSH_MODE?: string
    LINE_BREAKS?: boolean
    CATEGORIES?: TokenType[]
    tokenTypeIdx?: number
    categoryMatches?: number[]
    categoryMatchesMap?: {
        [tokType: number]: boolean
    }
    isParent?: boolean
    START_CHARS_HINT?: (string | number)[]
}

/**
 *  API #2 for [Custom Token Patterns](http://sap.github.io/chevrotain/docs/guide/custom_token_patterns.html).
 */
interface ICustomPattern {
    exec: CustomPatternMatcherFunc
}

/**
 * Things to note:
 *  - The offset range is inclusive to exclusive.
 *
 * - A lineTerminator as the last character does not effect the Token's line numbering.
 *   In other words a new line only starts **after** a line terminator.
 *
 * - A Token's image is it's **literal** text.
 *   e.g unicode escaping is untouched.
 */
export interface IToken {
    /** The textual representation of the Token as it appeared in the text. */
    image: string
    /** Offset of the first character of the Token. */
    startOffset: number
    /** Line of the first character of the Token. */
    startLine?: number
    /** Column of the first character of the Token. */
    startColumn?: number
    /** Offset of the last character of the Token. */
    endOffset?: number
    /** Line of the last character of the Token. */
    endLine?: number
    /** Column of the last character of the Token. */
    endColumn?: number
    /** this marks if a Token does not really exist and has been inserted "artificially" during parsing in rule error recovery. */
    isInsertedInRecovery?: boolean
    /** An number index representing the type of the Token use <getTokenConstructor> to get the Token Type from a token "instance"  */
    tokenTypeIdx: number
    /**
     * The actual Token Type of this Token "instance"
     * This is the same Object returned by the "createToken" API.
     * This property is very useful for debugging the Lexing and Parsing phases.
     */
    tokenType: TokenType

    /**
     * Custom Payload value, this is an optional feature of Custom Token Patterns
     * For additional details see the docs:
     * https://sap.github.io/chevrotain/docs/guide/custom_token_patterns.html#custom-payloads
     */
    payload?: any
}

export declare function tokenName(tokType: TokenType): string

/**
 *  Returns a human readable label for a TokenType if such exists,
 *  otherwise will return the TokenType's name.
 *
 *  Labels are useful in improving the readability of error messages and syntax diagrams.
 *  To define labels provide the label property in the {@link createToken} config parameter.
 */
export declare function tokenLabel(tokType: TokenType): string

/**
 * A Utility method to check if a token is of the type of the argument Token class.
 * This utility is needed because Chevrotain tokens support "categories" which means
 * A TokenType may have multiple categories.
 *
 * This means a simple comparison using the {@link IToken.tokenType} property may not suffice.
 * For example:
 *
 * ```
 *   import { createToken, tokenMatcher, Lexer } from "chevrotain"
 *
 *   // An "abstract" Token used only for categorization purposes.
 *   const NumberTokType = createToken({ name: "NumberTokType", pattern: Lexer.NA })
 *
 *   const IntegerTokType = createToken({
 *     name: "IntegerTokType",
 *     pattern: /\d+/,
 *     // Integer "Is A" Number
 *     categories: [NumberTokType]
 *   })
 *
 *   const DecimalTokType = createToken({
 *     name: "DecimalTokType",
 *     pattern: /\d+\.\d+/,
 *     // Double "Is A" Number
 *     categories: [NumberTokType]
 *   })
 *
 *   // Will always be false as the tokenType property can only
 *   // be Integer or Double Token Types as the Number TokenType is "abstract".
 *   if (myToken.tokenType === NumberTokType) { /* ... *\/ }
 *
 *   // Will be true when myToken is of Type Integer or Double.
 *   // Because the hierarchy defined by the categories is taken into account.
 *   if (tokenMatcher(myToken, NumberTokType) { /* ... *\/ }
 * ```
 *
 * @returns true iff the token matches the TokenType.
 */
export function tokenMatcher(token: IToken, tokType: TokenType): boolean

export declare type MultiModesDefinition = {
    [modeName: string]: TokenType[]
}

export interface IMultiModeLexerDefinition {
    modes: MultiModesDefinition
    defaultMode: string
}

export type TokenTypeDictionary = { [tokenName: string]: TokenType }

export declare type TokenVocabulary =
    | TokenTypeDictionary
    | TokenType[]
    | IMultiModeLexerDefinition

export interface IRuleConfig<T> {
    /**
     * The function which will be invoked to produce the returned value for a production that have not been
     * successfully executed and the parser recovered from.
     */
    recoveryValueFunc?: () => T
    /**
     * Enable/Disable re-sync error recovery for this specific production.
     */
    resyncEnabled?: boolean
}

export interface DSLMethodOpts<T> {
    /**
     * in-lined method name
     */
    NAME?: string
    /**
     * The Grammar to process in this method.
     */
    DEF: GrammarAction<T>
    /**
     * A semantic constraint on this DSL method
     * @see https://github.com/SAP/chevrotain/blob/master/examples/parser/predicate_lookahead/predicate_lookahead.js
     * For farther details.
     */
    GATE?: () => boolean

    /**
     * Maximum number of "following tokens" which would be used to
     * Choose between the alternatives.
     *
     * By default this value is determined by the {@link IParserConfig.maxLookahead} value.
     * A Higher value may be used for a specific DSL method to resolve ambiguities
     * And a lower value may be used to resolve slow initialization times issues.
     *
     * TODO: create full docs and link
     */
    MAX_LOOKAHEAD?: number
}

export interface DSLMethodOptsWithErr<T> extends DSLMethodOpts<T> {
    /**
     *  Short title/classification to what is being matched.
     *  Will be used in the error message,.
     *  If none is provided, the error message will include the names of the expected
     *  Tokens sequences which start the method's inner grammar
     */
    ERR_MSG?: string
}

export interface OrMethodOpts {
    NAME?: string
    /**
     * The set of alternatives,
     * See detailed description in {@link Parser.OR}
     */
    DEF: IOrAlt[]
    /**
     * A description for the alternatives used in error messages
     * If none is provided, the error message will include the names of the expected
     * Tokens sequences which may start each alternative.
     */
    ERR_MSG?: string

    /**
     * A Flag indicating that **all** ambiguities in this alternation should
     * be ignored.
     *
     * This flag should only be used in rare circumstances,
     * As normally alternation ambiguities should be resolved in other ways:
     * - Re-ordering the alternatives.
     * - Re-factoring the grammar to extract common prefixes before alternation.
     * - Using gates {@link IOrAlt.GATE} to implement custom lookahead logic.
     * - Using the more granular {@link IOrAlt.IGNORE_AMBIGUITIES} on a **specific** alternative.
     */
    IGNORE_AMBIGUITIES?: boolean

    /**
     * Maximum number of "following tokens" which would be used to
     * Choose between the alternatives.
     *
     * By default this value is determined by the {@link IParserConfig.maxLookahead} value.
     * A Higher value may be used for a specific DSL method to resolve ambiguities
     * And a lower value may be used to resolve slow initialization times issues.
     *
     * TODO: create full docs and link
     */
    MAX_LOOKAHEAD?: number
}

export interface ManySepMethodOpts<T> {
    NAME?: string
    /**
     * The grammar to process in each iteration.
     */
    DEF: GrammarAction<T>
    /**
     * The separator between each iteration.
     */
    SEP: TokenType

    /**
     * @see DSLMethodOpts.MAX_LOOKAHEAD
     */
    MAX_LOOKAHEAD?: number
}

export interface AtLeastOneSepMethodOpts<T> extends ManySepMethodOpts<T> {
    /**
     *  Short title/classification to what is being matched.
     *  Will be used in the error message,.
     *  If none is provided, the error message will include the names of the expected
     *  Tokens sequences which start the method's inner grammar.
     */
    ERR_MSG?: string
}

export interface ConsumeMethodOpts {
    /**
     *  A custom Error message if the Token could not be consumed.
     *  This will override any error message provided by the parser's "errorMessageProvider"
     */
    ERR_MSG?: string
    /**
     * A label to be used instead of the TokenType name in the created CST.
     */
    LABEL?: string
}

export interface SubruleMethodOpts {
    /**
     * The arguments to parameterized rules, see:
     * https://github.com/SAP/chevrotain/blob/master/examples/parser/parametrized_rules/parametrized.js
     */
    ARGS?: any[]
    /**
     * A label to be used instead of the subrule's name in the created CST.
     */
    LABEL?: string
}

export declare type GrammarAction<OUT> = () => OUT

/**
 * TODO: remove this in next major version this `IOrAlt` is enough
 * @deprecated
 */
export declare type IAnyOrAlt = any

export interface IOrAlt {
    NAME?: string
    GATE?: () => boolean
    ALT: () => any
    /**
     * A Flag indicating that any ambiguities involving this
     * specific alternative Should be ignored.
     *
     * This flag will be **implicitly** enabled if a GATE is used
     * as the assumption is that the GATE is used to resolve an ambiguity.
     */
    IGNORE_AMBIGUITIES?: boolean
}

export interface IOrAltWithGate extends IOrAlt {
    // TODO: deprecate this interface
}

export interface ICstVisitor<IN, OUT> {
    visit(cstNode: CstNode | CstNode[], param?: IN): OUT
    validateVisitor(): void
}

/**
 * A [Concrete Syntax Tree](http://sap.github.io/chevrotain/docs/guide/concrete_syntax_tree.html) Node.
 * This structure represents the whole parse tree of the grammar
 * This means that information on each and every Token is present.
 * This is unlike an AST (Abstract Syntax Tree) where some of the syntactic information is missing.
 *
 * For example given an ECMAScript grammar, an AST would normally not contain information on the location
 * of Commas, Semi colons, redundant parenthesis ect, however a CST would have that information.
 */
export interface CstNode {
    readonly name: string
    readonly children: CstChildrenDictionary
    readonly recoveredNode?: boolean

    /**
     * Will only be present if the {@link IParserConfig.nodeLocationTracking} is
     * **not** set to "none".
     * See: http://sap.github.io/chevrotain/docs/guide/concrete_syntax_tree.html#cstnode-location
     * For more details.
     */
    readonly location?: CstNodeLocation
    /**
     * Only relevant for [in-lined](http://sap.github.io/chevrotain/docs/guide/concrete_syntax_tree.html#in-lined-rules) rules.
     * the fullName will **also** include the name of the top level rule containing this nested rule.
     */
    readonly fullName?: string
}

/**
 *  The Column/Line properties will only be present when
 *  The {@link IParserConfig.nodeLocationTracking} is set to "full".
 */
export interface CstNodeLocation {
    startOffset: number
    startLine: number
    startColumn?: number
    endOffset?: number
    endLine?: number
    endColumn?: number
}

export declare type CstChildrenDictionary = {
    [identifier: string]: CstElement[]
}

export declare type CstElement = IToken | CstNode

export declare type nodeLocationTrackingOptions = "full" | "onlyOffset" | "none"

export interface IParserConfig {
    /**
     * Is the error recovery / fault tolerance of the Chevrotain Parser enabled.
     */
    recoveryEnabled?: boolean
    /**
     * Maximum number of tokens the parser will use to choose between alternatives.
     * By default this value is `4`.
     * In the future it may be reduced to `3` due to performance considerations.
     */
    maxLookahead?: number
    /**
     * @deprecated - use the IGNORE_AMBIGUITIES flag on the relevant DSL method instead
     *               - {@link IOrAlt.IGNORE_AMBIGUITIES}
     *               - {@link OrMethodOpts.IGNORE_AMBIGUITIES}
     *
     * Used to mark parser definition errors that should be ignored.
     * For example:
     *
     * ```
     *    {
     *      myCustomRule : {
     *                      OR3 : true
     *                     },
     *      myOtherRule : {
     *                     OPTION1 : true,
     *                     OR4 : true
     *                    }
     *    }
     * ```
     *
     * Be careful when ignoring errors, they are usually there for a reason :).
     */
    ignoredIssues?: IgnoredParserIssues
    /**
     * Enable This Flag to to support Dynamically defined Tokens.
     * This will disable performance optimizations which cannot work if the whole Token vocabulary is not known
     * During Parser initialization.
     *
     * See [runnable example](https://github.com/SAP/chevrotain/tree/master/examples/parser/dynamic_tokens)
     */
    dynamicTokensEnabled?: boolean
    /**
     * @deprecated - extend either CstParser or EmbeddedActionsParser to control this flag instead
     *               - @see CstParser
     *               - @see EmbeddedActionsParser
     *
     * Enable automatic Concrete Syntax Tree creation
     * For in-depth docs on [Concrete Syntax Trees](http://sap.github.io/chevrotain/docs/guide/concrete_syntax_tree.html):
     */
    outputCst?: boolean
    /**
     * Enable computation of CST nodes location.
     * By default this is set to "none", meaning this feature is disabled.
     * See: http://sap.github.io/chevrotain/docs/guide/concrete_syntax_tree.html#cstnode-location
     * For more details.
     */
    nodeLocationTracking?: nodeLocationTrackingOptions
    /**
     * A custom error message provider.
     * Can be used to override the default error messages.
     * For example:
     *   - Translating the error messages to a different languages.
     *   - Changing the formatting.
     *   - Providing special error messages under certain conditions, e.g: missing semicolons.
     */
    errorMessageProvider?: IParserErrorMessageProvider
    /**
     * Enabling this flag will print performance tracing logs during parser
     * Initialization (constructor invocation), this is useful to narrow down the cause
     * of the initialization performance problem.
     *
     * You can also pass a numerical value which affects the verbosity
     * of the traces, this number is the maximum nesting level of the traces, e.g:
     * 0: Traces disabled === 'false'
     * 1: Top Level traces only.
     * 2: One level of nested inner traces.
     * ...
     *
     * Note that passing the boolean `true` is identical to passing the numerical value `infinity`
     */
    traceInitPerf?: boolean | number
    /**
     * This flag will avoid running the grammar validations during Parser initialization.
     *
     * This can substantially improve the Parser's initialization (constructor) time.
     * @see IParserConfig.traceInitPerf to measure the grammar validations cost for your parser.
     *
     * Note that the grammar validations are **extremely useful** during development time,
     * e.g: detecting ambiguities / left recursion.
     * So they should not be skipped during development flows.
     *   - For example: via a conditional that checks an env variable.
     */
    skipValidations?: boolean
}

/**
 * See: {@link IParserConfig.ignoredIssues}
 */
export declare type IgnoredParserIssues = {
    [ruleName: string]: IgnoredRuleIssues
}

/**
 * See: {@link IParserConfig.ignoredIssues}
 */
export declare type IgnoredRuleIssues = {
    [dslNameAndOccurrence: string]: boolean
}

export interface IParserErrorMessageProvider {
    /**
     * Mismatched Token Error happens when the parser attempted to consume a terminal and failed.
     * It corresponds to a failed {@link Parser.CONSUME} in Chevrotain DSL terms.
     *
     * @param options.expected - The expected Token Type.
     *
     * @param options.actual - The actual Token "instance".
     *
     * @param options.previous - The previous Token "instance".
     *                                This is useful if options.actual[0] is of type chevrotain.EOF and you need to know the last token parsed.
     *
     * @param options.ruleName - The rule in which the error occurred.
     */
    buildMismatchTokenMessage?(options: {
        expected: TokenType
        actual: IToken
        previous: IToken
        ruleName: string
    }): string
    /**
     * A Redundant Input Error happens when the parser has completed parsing but there
     * is still unprocessed input remaining.
     *
     * @param options.firstRedundant - The first unprocessed token "instance".
     *
     * @param options.ruleName - The rule in which the error occurred.
     */
    buildNotAllInputParsedMessage?(options: {
        firstRedundant: IToken
        ruleName: string
    }): string
    /**
     * A No Viable Alternative Error happens when the parser cannot detect any valid alternative in an alternation.
     * It corresponds to a failed {@link Parser.OR} in Chevrotain DSL terms.
     *
     * @param options.expectedPathsPerAlt - First level of the array represents each alternative
     *                           The next two levels represent valid (expected) paths in each alternative.
     *
     * @param options.actual - The actual sequence of tokens encountered.
     *
     * @param options.previous - The previous Token "instance".
     *                                This is useful if options.actual[0] is of type chevrotain.EOF and you need to know the last token parsed.
     *
     * @param options.customUserDescription - A user may provide custom error message descriptor in the {@link Parser.AT_LEAST_ONE} DSL method
     *                                        options parameter, this is that custom message.
     *
     * @param options.ruleName - The rule in which the error occurred.
     */
    buildNoViableAltMessage?(options: {
        expectedPathsPerAlt: TokenType[][][]
        actual: IToken[]
        previous: IToken
        customUserDescription: string
        ruleName: string
    }): string
    /**
     * An Early Exit Error happens when the parser cannot detect the first mandatory iteration of a repetition.
     * It corresponds to a failed {@link Parser.AT_LEAST_ONE} or {@link Parser.AT_LEAST_ONE_SEP} in Chevrotain DSL terms.
     *
     * @param options.expectedIterationPaths - The valid (expected) paths in the first iteration.
     *
     * @param options.actual - The actual sequence of tokens encountered.
     *
     * @param options.previous - The previous Token "instance".
     *                                This is useful if options.actual[0] is of type chevrotain.EOF and you need to know the last token parsed.
     *
     * @param options.customUserDescription - A user may provide custom error message descriptor in the {@link Parser.AT_LEAST_ONE} DSL method
     *                                        options parameter, this is that custom message.
     *
     * @param options.ruleName - The rule in which the error occurred.
     */
    buildEarlyExitMessage?(options: {
        expectedIterationPaths: TokenType[][]
        actual: IToken[]
        previous: IToken
        customUserDescription: string
        ruleName: string
    }): string
}

export interface IRecognizerContext {
    /**
     * A copy of the parser's rule stack at the "time" the RecognitionException occurred.
     * This can be used to help debug parsing errors (How did we get here?).
     */
    ruleStack: string[]
    /**
     * A copy of the parser's rule occurrence stack at the "time" the RecognitionException occurred.
     * This can be used to help debug parsing errors (How did we get here?).
     */
    ruleOccurrenceStack: number[]
}

export declare type ISeparatedIterationResult<OUT> = {
    values: OUT[]
    separators: IToken[]
}

export interface ISerializedGast {
    type:
        | "NonTerminal"
        | "Flat"
        | "Option"
        | "RepetitionMandatory"
        | "RepetitionMandatoryWithSeparator"
        | "Repetition"
        | "RepetitionWithSeparator"
        | "Alternation"
        | "Terminal"
        | "Rule"
    definition?: ISerializedGast[]
}

/**
 * Structure for the path the parser "took" to reach a certain position
 * in the grammar.
 */
export interface IGrammarPath {
    /**
     * The Grammar rules invoked and still unterminated to reach this Grammar Path.
     */
    ruleStack: string[]
    /**
     * The occurrence index (SUBRULE1/2/3/5/...) of each Grammar rule invoked and still unterminated.
     * Used to distinguish between **different** invocations of the same subrule at the same top level rule.
     */
    occurrenceStack: number[]
}

export interface ISyntacticContentAssistPath extends IGrammarPath {
    nextTokenType: TokenType
    nextTokenOccurrence: number
}

export interface ITokenGrammarPath extends IGrammarPath {
    lastTok: TokenType
    lastTokOccurrence: number
}

export declare enum LexerDefinitionErrorType {
    MISSING_PATTERN = 0,
    INVALID_PATTERN = 1,
    EOI_ANCHOR_FOUND = 2,
    UNSUPPORTED_FLAGS_FOUND = 3,
    DUPLICATE_PATTERNS_FOUND = 4,
    INVALID_GROUP_TYPE_FOUND = 5,
    PUSH_MODE_DOES_NOT_EXIST = 6,
    MULTI_MODE_LEXER_WITHOUT_DEFAULT_MODE = 7,
    MULTI_MODE_LEXER_WITHOUT_MODES_PROPERTY = 8,
    MULTI_MODE_LEXER_DEFAULT_MODE_VALUE_DOES_NOT_EXIST = 9,
    LEXER_DEFINITION_CANNOT_CONTAIN_UNDEFINED = 10,
    SOI_ANCHOR_FOUND = 11,
    EMPTY_MATCH_PATTERN = 12,
    NO_LINE_BREAKS_FLAGS = 13,
    UNREACHABLE_PATTERN = 14,
    IDENTIFY_TERMINATOR = 15,
    CUSTOM_LINE_BREAK = 16
}

/**
 * Type of End Of File Token.
 */
export declare const EOF: TokenType

/**
 * Convenience used to express an **empty** alternative in an OR (alternation).
 * can be used to more clearly describe the intent in a case of empty alternation.
 *
 * For example:
 *
 * 1. without using EMPTY_ALT:
 *  ```
 *    this.OR([
 *      {ALT: () => {
 *        this.CONSUME1(OneTok)
 *        return "1"
 *      }},
 *      {ALT: () => {
 *        this.CONSUME1(TwoTok)
 *        return "2"
 *      }},
 *      // implicitly empty because there are no invoked grammar
 *      // rules (OR/MANY/CONSUME...) inside this alternative.
 *      {ALT: () => {
 *        return "666"
 *      }},
 *    ])
 *  ```
 *
 * 2. using EMPTY_ALT:
 *  ```
 *    this.OR([
 *      {ALT: () => {
 *        this.CONSUME1(OneTok)
 *        return "1"
 *      }},
 *      {ALT: () => {
 *        this.CONSUME1(TwoTok)
 *        return "2"
 *      }},
 *      // explicitly empty, clearer intent
 *      {ALT: EMPTY_ALT("666")},
 *    ])
 *  ```
 */
export declare function EMPTY_ALT<T>(value?: T): () => T

/**
 * This is the default logic Chevrotain uses to construct error messages.
 * It can be used as a reference or as a starting point customize a parser's
 * error messages.
 *
 * - See: {@link IParserConfig.errorMessageProvider}
 */
export declare const defaultParserErrorProvider: IParserErrorMessageProvider

/**
 * A Chevrotain Parser runtime exception.
 */
export interface IRecognitionException {
    name: string
    message: string
    /**
     * The token which caused the parser error.
     */
    token: IToken
    /**
     * Additional tokens which have been re-synced in error recovery due to the original error.
     * This information can be used the calculate the whole text area which has been skipped due to an error.
     * For example for displaying with a red underline in a text editor.
     */
    resyncedTokens: IToken[]
    context: IRecognizerContext
}

/**
 * A utility to detect if an Error is a Chevrotain Parser's runtime exception.
 */
export declare function isRecognitionException(error: Error): boolean

// TODO refactor exceptions constructors to classes

/**
 * An exception of this type will be saved in {@link Parser.errors} when {@link Parser.CONSUME}
 * was called but failed to match the expected Token Type.
 */
export declare class MismatchedTokenException extends Error {
    constructor(message: string, token: IToken, previousToken: IToken)
}

/**
 * An exception of this type will be saved in {@link Parser.errors} when {@link Parser.OR}
 * was called yet none of the possible alternatives could be matched.
 */
export declare class NoViableAltException extends Error {
    constructor(message: string, token: IToken, previousToken: IToken)
}

/**
 * An exception of this type will be saved in {@link Parser.errors} when
 * the parser has finished yet there exists remaining input (tokens) that has not processed.
 */
export declare class NotAllInputParsedException extends Error {
    constructor(message: string, token: IToken)
}

/**
 * An exception of this type will be saved in {@link Parser.errors} when {@link Parser.AT_LEAST_ONE}
 * or {@link Parser.AT_LEAST_ONE_SEP} was called but failed to match even a single iteration.
 */
export declare class EarlyExitException extends Error {
    constructor(message: string, token: IToken, previousToken: IToken)
}

export interface IOptionallyNamedProduction {
    name?: string
}
export interface IProduction {
    accept(visitor: IGASTVisitor): void
}

export interface IProductionWithOccurrence extends IProduction {
    idx: number
}

/**
 * A very basic implementation of a Visitor Pattern
 * For the Grammar AST structure.
 *
 * This may be useful for advanced users who create custom logic on the grammar AST.
 * For example, custom validations or introspection.
 */
export abstract class GAstVisitor {
    visit(node: IProduction): any

    abstract visitNonTerminal(node: NonTerminal): any

    abstract visitFlat(node: Flat): any

    abstract visitOption(node: Option): any

    abstract visitRepetition(node: Repetition): any

    abstract visitRepetitionMandatory(node: RepetitionMandatory): any

    abstract visitRepetitionMandatoryWithSeparator(
        node: RepetitionMandatoryWithSeparator
    ): any

    abstract visitRepetitionWithSeparator(node: RepetitionWithSeparator): any

    abstract visitAlternation(node: Alternation): any

    abstract visitTerminal(node: Terminal): any

    abstract visitRule(node: Rule): any
}

/**
 * The Grammar AST class representing a top level {@link Parser.RULE} call.
 */
export declare class Rule {
    name: string
    orgText: string
    definition: IProduction[]

    constructor(options: {
        name: string
        definition: IProduction[]
        orgText?: string
    })

    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class representing a top level {@link Parser.SUBRULE} call.
 */
export declare class NonTerminal implements IProductionWithOccurrence {
    nonTerminalName: string
    referencedRule: Rule
    idx: number
    constructor(options: {
        nonTerminalName: string
        referencedRule?: Rule
        idx?: number
    })
    definition: IProduction[]
    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class used to represent a sequence.
 * This is normally only used in {@link Alternation} to distinguish
 * between the different alternatives.
 */
export declare class Flat implements IOptionallyNamedProduction {
    name: string
    definition: IProduction[]

    constructor(options: { definition: IProduction[]; name?: string })

    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class representing a {@link Parser.OPTION} call.
 */
export declare class Option
    implements IProductionWithOccurrence, IOptionallyNamedProduction {
    idx: number
    name?: string
    definition: IProduction[]

    constructor(options: {
        definition: IProduction[]
        idx?: number
        name?: string
    })

    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class representing a {@link Parser.AT_LEAST_ONE} call.
 */
export declare class RepetitionMandatory
    implements IProductionWithOccurrence, IOptionallyNamedProduction {
    name: string
    idx: number
    definition: IProduction[]

    constructor(options: {
        definition: IProduction[]
        idx?: number
        name?: string
    })

    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class representing a {@link Parser.AT_LEAST_ONE_SEP} call.
 */
export declare class RepetitionMandatoryWithSeparator
    implements IProductionWithOccurrence, IOptionallyNamedProduction {
    separator: TokenType
    idx: number
    name: string
    definition: IProduction[]

    constructor(options: {
        definition: IProduction[]
        separator: TokenType
        idx?: number
        name?: string
    })

    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class representing a {@link Parser.MANY} call.
 */
export declare class Repetition
    implements IProductionWithOccurrence, IOptionallyNamedProduction {
    separator: TokenType
    idx: number
    name: string
    definition: IProduction[]

    constructor(options: {
        definition: IProduction[]
        idx?: number
        name?: string
    })

    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class representing a {@link Parser.MANY_SEP} call.
 */
export declare class RepetitionWithSeparator
    implements IProductionWithOccurrence, IOptionallyNamedProduction {
    separator: TokenType
    idx: number
    name: string
    definition: IProduction[]

    constructor(options: {
        definition: IProduction[]
        separator: TokenType
        idx?: number
        name?: string
    })

    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class representing a {@link Parser.OR} call.
 */
export declare class Alternation
    implements IProductionWithOccurrence, IOptionallyNamedProduction {
    idx: number
    name: string
    definition: IProduction[]

    constructor(options: {
        definition: IProduction[]
        idx?: number
        name?: string
    })

    accept(visitor: IGASTVisitor): void
}

/**
 * The Grammar AST class representing a {@link Parser.CONSUME} call.
 */
export declare class Terminal implements IProductionWithOccurrence {
    terminalType: TokenType
    idx: number
    constructor(options: { terminalType: TokenType; idx?: number })
    accept(visitor: IGASTVisitor): void
}

export interface IGASTVisitor {
    visit(prod: IProduction): any
}

/**
 * Serialize a Grammar to a JSON Object.
 *
 * This can be useful for scenarios requiring exporting the grammar structure
 * for example drawing syntax diagrams.
 */
export declare function serializeGrammar(topRules: Rule[]): ISerializedGast[]

/**
 * Like {@link serializeGrammar} but for a single GAST Production instead of a set of Rules.
 */
export declare function serializeProduction(node: IProduction): ISerializedGast

/**
 * A utility to resolve a grammar AST (rules parameter).
 * "Resolving" means assigning the appropiate value for all {@link NonTerminal.referencedRule}
 * properties in the grammar AST.
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html)
 */
export declare function resolveGrammar(options: {
    rules: Rule[]
    errMsgProvider?: IGrammarResolverErrorMessageProvider
}): IParserDefinitionError[]

/**
 * A utility to validate a grammar AST (rules parameter).
 * For example: left recursion detection, ambiguity detection, ...
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html)
 */
export declare function validateGrammar(options: {
    rules: Rule[]
    /**
     * The maximum lookahead used in the grammar.
     * This number is needed to perform ambiguity detection.
     */
    maxLookahead: number
    /**
     * The Token Types used by the grammar.
     */
    tokenTypes: TokenType[]
    grammarName: string
    errMsgProvider: IGrammarValidatorErrorMessageProvider
    ignoredIssues?: IgnoredParserIssues
}): IParserDefinitionError[]

/**
 * A utility for assigning unique occurence indices to a grammar AST (rules parameter).
 * This can be useful when using Chevrotain to create custom APIs.
 *
 * - FAQ: [Why are these unique occurences needed](http://sap.github.io/chevrotain/docs/FAQ.html#NUMERICAL_SUFFIXES)
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html)
 */
export declare function assignOccurrenceIndices(options: {
    rules: Rule[]
}): void

/**
 * The default grammar validations errror message provider used by Chevrotain.
 * this can be used as the basis for custom error providers when using Chevrotain's
 * custom APIs.
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html#grammar-validations)
 */
export declare const defaultGrammarValidatorErrorProvider: IGrammarValidatorErrorMessageProvider

/**
 * The default grammar resolver errror message provider used by Chevrotain.
 * this can be used as the basis for custom error providers when using Chevrotain's
 * custom APIs.
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html#grammar-validations)
 */
export declare const defaultGrammarResolverErrorProvider: IGrammarResolverErrorMessageProvider

/**
 * Implementing this interface enables customizing grammar validation errors
 * when using custom APIs.
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html#grammar-validations)
 */
export interface IGrammarValidatorErrorMessageProvider {
    buildDuplicateFoundError(
        topLevelRule: Rule,
        duplicateProds: IProductionWithOccurrence[]
    ): string
    buildInvalidNestedRuleNameError(
        topLevelRule: Rule,
        nestedProd: IOptionallyNamedProduction
    ): string
    buildDuplicateNestedRuleNameError(
        topLevelRule: Rule,
        nestedProd: IOptionallyNamedProduction[]
    ): string
    buildNamespaceConflictError(topLevelRule: Rule): string
    buildAlternationPrefixAmbiguityError(options: {
        topLevelRule: Rule
        prefixPath: TokenType[]
        ambiguityIndices: number[]
        alternation: Alternation
    }): string
    buildAlternationAmbiguityError(options: {
        topLevelRule: Rule
        prefixPath: TokenType[]
        ambiguityIndices: number[]
        alternation: Alternation
    }): string
    buildEmptyRepetitionError(options: {
        topLevelRule: Rule
        repetition: IProductionWithOccurrence
    }): string
    buildTokenNameError(options: {
        tokenType: TokenType
        expectedPattern: RegExp
    }): any
    buildEmptyAlternationError(options: {
        topLevelRule: Rule
        alternation: Alternation
        emptyChoiceIdx: number
    }): any
    buildTooManyAlternativesError(options: {
        topLevelRule: Rule
        alternation: Alternation
    }): string
    buildLeftRecursionError(options: {
        topLevelRule: Rule
        leftRecursionPath: Rule[]
    }): string
    buildInvalidRuleNameError(options: {
        topLevelRule: Rule
        expectedPattern: RegExp
    }): string
    buildDuplicateRuleNameError(options: {
        topLevelRule: Rule | string
        grammarName: string
    }): string
}

/**
 * Implementing this interface enables customizing grammar resolving errors
 * when using custom APIs.
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html#grammar-validations)
 */
export interface IGrammarResolverErrorMessageProvider {
    buildRuleNotFoundError(
        topLevelRule: Rule,
        undefinedRule: NonTerminal
    ): string
}

/**
 * Structure of grammar validations errors.
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html#grammar-validations)
 */
export interface IParserDefinitionError {
    message: string
    type: ParserDefinitionErrorType
    ruleName?: string
}

/**
 * @deprecated
 * This function no longer does anything, Avoid using this function
 * As it will be removed in future versions.
 */
export declare function clearCache(): void

/**
 * Structure of configuration object for {@link createSyntaxDiagramsCode}
 */
export interface ICreateSyntaxDiagramsConfig {
    /**
     * Base Url to load the runtime resources for rendering the diagrams
     */
    resourceBase?: string
    /**
     * Url to load the styleSheet, replace with your own for styling customization.
     */
    css?: string
}

/**
 * Will generate an html source code (text).
 * This html text will render syntax diagrams for the provided grammar.
 *
 * - See detailed docs for [Syntax Diagrams](http://sap.github.io/chevrotain/docs/guide/generating_syntax_diagrams.html).
 */
export declare function createSyntaxDiagramsCode(
    grammar: ISerializedGast[],
    config?: ICreateSyntaxDiagramsConfig
): string

/**
 * Generate A Parser factory from a set of Rules.
 *
 * This variant will Create a factory function that once invoked with a IParserConfig will return
 * a Parser Object.
 *
 * - Note that this happens using the Function constructor (a type of "eval") so it will not work in environments
 *   where content security policy is enabled, such as certain websites, Chrome extensions ect...
 *
 *   This means this function is best used for development flows to reduce the feedback loops
 *   or for productive flows targeting node.js only.
 *
 *   For productive flows targeting a browser runtime see {@link generateParserModule}.
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html).
 */
export declare function generateParserFactory(options: {
    name: string
    rules: Rule[]
    tokenVocabulary: TokenVocabulary
}): (config?: IParserConfig) => Parser

/**
 * Generate A Parser's text from a set of Rules.
 *
 * This variant will generate the **string literal** for a UMD module https://github.com/umdjs/umd
 * That exports a Parser Constructor.
 *
 * - Note that the constructor exposed by the generated module must receive the TokenVocabulary as the first
 *   argument, the IParser config can be passed as the second argument.
 *
 * - See detailed docs for [Custom APIs](http://sap.github.io/chevrotain/docs/guide/custom_apis.html).
 */
export declare function generateParserModule(options: {
    name: string
    rules: Rule[]
}): string
