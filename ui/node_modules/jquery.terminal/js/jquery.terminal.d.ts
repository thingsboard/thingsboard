/*
 * helper types
 */
type anyFunction = (...args: any[]) => any;
type StringOrNumber = string | number | null;

type JSONObject = {
    [key: string]: TypeOrArray<StringOrNumber | boolean | JSONObject>
}

type mapFunction = (key: string, value: anyFunction) => any;
type voidFunction = () => void;

type TypeOrArray<T> = T | T[];
type TypeOrString<T> = T | string;
type TypeOrPromise<T> = T | PromiseLike<T>;

declare namespace JQueryTerminal {
    type interpterFunction = (this: JQueryTerminal, command: string, term: JQueryTerminal) => any;
    type terminalObjectFunction = (...args: (string | number | RegExp)[]) => (void | PromiseLike<echoValue>);
    type Interpterer = string | interpterFunction | ObjectInterpreter;
    type ObjectInterpreter = {
        [key: string]: ObjectInterpreter | terminalObjectFunction;
    }

    type RegExpReplacementFunction = (...args: string[]) => string;
    type IterateFormattingArgument = {
        count: number,
        index: number,
        formatting: string,
        length: number,
        text: boolean,
        space: number
    };
    type IterateFormattingFunction = (data: IterateFormattingArgument) => void;

    type strings = {
        comletionParameters: string;
        wrongPasswordTryAgain: string;
        wrongPassword: string;
        ajaxAbortError: string;
        wrongArity: string;
        commandNotFound: string;
        oneRPCWithIgnore: string;
        oneInterpreterFunction: string;
        loginFunctionMissing: string;
        noTokenError: string;
        serverResponse: string;
        wrongGreetings: string;
        notWhileLogin: string;
        loginIsNotAFunction: string;
        canExitError: string;
        invalidCompletion: string;
        invalidSelector: string;
        invalidTerminalId: string;
        login: string;
        password: string;
        recursiveCall: string;
        notAString: string;
        redrawError: string;
        invalidStrings: string;
        defunctTerminal: string;
    };

    type ParsedCommand<T> = {
        command: string;
        name: string;
        args: T[];
        args_quotes: string[];
        rest: string;
    };

    type ParsedOptions = {
        _: string[];
        [key: string]: boolean | string | string[];
    };

    type FormatterRegExpFunction = (...args: string[]) => string;
    type FormaterRegExpReplacement = string | FormatterRegExpFunction;
    type FormatterFunction = (str: string, options: JSONObject) => (string | [string, number]);

    type Formatter = [RegExp, FormaterRegExpReplacement] | [RegExp, FormaterRegExpReplacement, { loop: boolean }] | FormatterFunction;
    type keymapFunctionOptionalArg = (event: JQueryKeyEventObject, original?: keymapFunction) => any;

    type keymapFunction<T = JQueryTerminal> = (this: T, event: JQueryKeyEventObject, original: keymapFunctionOptionalArg) => any;
    type keymapObject<T = JQueryTerminal> = { [key: string]: keymapFunction<T> };
    type keymapObjectOptionalArg = { [key: string]: keymapFunctionOptionalArg };

    type commandsCmdFunction<T = Cmd> = (this: T, command: string) => any;
    type echoValue = string | string[] | (() => string | string[]);
    type errorArgument = string | (() => string) | PromiseLike<string>;
    type setStringFunction = (value: string) => void;
    type setEchoValueFunction = (value: TypeOrPromise<echoValue>) => void;
    type greetingsArg = ((this: JQueryTerminal, setGreeting: setEchoValueFunction) => void) | string | null;
    type cmdPrompt<T = Cmd> = ((this: Cmd, setPrompt: setStringFunction) => void) | string;

    type ExtendedPrompt =  ((this: JQueryTerminal, setPrompt: setStringFunction) => (void | PromiseLike<string>)) | string;

    type pushOptions = {
        infiniteLogin?: boolean;
        prompt?: ExtendedPrompt;
        login?: LoginArgument;
        name?: string;
        completion?: Completion;
    }

    type historyFilterFunction = (command: string) => boolean;
    type historyFilter = null | RegExp | historyFilterFunction;

    type KeyEventHandler<T = JQueryTerminal> = (this: T, event: JQueryKeyEventObject) => (boolean | void);

    type ExceptionHandler = (this: JQueryTerminal, e: Error | TerminalExeption, label: string) => void;
    type processRPCResponseFunction = (this: JQueryTerminal, result: JSONObject, term: JQueryTerminal) => void;
    type ObjectWithThenMethod = {
        then: () => any;
    }
    type SetLoginCallback = (token: string) => (void | ObjectWithThenMethod);
    type LoginFunction = (username: string, password: string, cb: SetLoginCallback) => (void | ObjectWithThenMethod);

    type LoginArgument = string | boolean | JQueryTerminal.LoginFunction;

    type Completion = string[] | CompletionFunction;

    type SetComplationCallback = (complation: string[]) => void;

    type CompletionFunction = (this: JQueryTerminal, str: string, callback: SetComplationCallback) => void;

    type EchoCommandCallback = (command: string) => void;

    // all arguments are optional so you can use $.noop
    type DoubleTabFunction = (this: JQueryTerminal, str: string, matched: string[], echoCmd: () => void) => (void | boolean);

    type RPCErrorCallback = (this: JQueryTerminal, error: any) => void;

    type RequestResponseCallback = (this: JQueryTerminal, xhr: JQuery.jqXHR, json: any, term: JQueryTerminal) => void;

    type EchoFinalizeFunction = (div: JQuery) => void;
    type EventCallback = (this: JQueryTerminal, term: JQueryTerminal) => (void | boolean);

    type formatOptions = {
        linksNoReferrer?: boolean;
        anyLinks?: boolean;
        char_width?: number;
        linksNoFollow?: boolean;
    };

    type InterpreterItem = {
        completion: "settings" | JQueryTerminal.Completion;
        history?: boolean;
        // all other iterpreters are converted to function
        interpreter: JQueryTerminal.interpterFunction;
        keydown?: KeyEventHandler<JQueryTerminal>;
        keypress?: KeyEventHandler<JQueryTerminal>;
        mask?: boolean | string;
        infiniteLogin?: boolean;
        prompt: ExtendedPrompt;
    }

    type PushPopCallback = (this: JQueryTerminal, before: JQueryTerminal.InterpreterItem, after: JQueryTerminal.InterpreterItem) => void;

    type Lines = Array<{ string: any, options: LineEchoOptions, index: number }>;

    type View = {
        focus: boolean;
        mask: string | boolean;
        prompt?: ExtendedPrompt;
        command: string;
        position: number;
        lines: Lines;
        interpreters?: Stack<InterpreterItem>;
        history: string[];
    }

    type CompleteOptions = {
        word?: boolean;
        echo?: boolean;
        escape?: boolean;
        caseSensitive?: boolean;
        doubleTab?: DoubleTabFunction;
    }

    type LineEchoOptions = {
        exec: boolean;
        finalize: JQueryTerminal.EchoFinalizeFunction;
        flush: boolean;
        formatters: boolean;
        keepWords: boolean;
        raw: boolean;
    }

    type EchoOptions = {
        flush?: boolean;
        raw?: boolean;
        finalize?: JQueryTerminal.EchoFinalizeFunction;
        keepWords?: boolean;
        formatters?: boolean;
    }


    interface History<T = string> {
        new(name?: string, size?: number, memory?: boolean): History<T>;
        append(item: T): void;
        set(items: T[]): void;
        data(): T[];
        reset(): void;
        last(): any;
        end(): boolean;
        position(): number;
        current(): T;
        next(): T | void;
        previous(): T | void;
        clear(): void;
        enabled(): boolean;
        enable(): void;
        purge(): void;
        disable(): void;
        toogle(value?: boolean): void;
    }

    interface Stack<T> {
        new(init?: T[]): Stack<T>;
        data(): T[];
        map(fn: (item: T, index?: number) => any): any[];
        size(): number;
        pop(): null | T;
        push(): T;
        top(): T;
        clone(): Stack<T>;
    }

    interface Cycle<T> {
        new(...args: T[]): Cycle<T>;
        get(): T[];
        index(): number;
        rotate(): T | void;
        length(): number;
        remove(i: number): void;
        set(item: T): void;
        front(): void | T;
        map(fn: (item: T, index: number) => any): any[];
        forEach(fn: (item: T, index: number) => any): void;
        append(item: T): void;
    }
}


interface JQuery<TElement = HTMLElement> {
    terminal(interpreter?: TypeOrArray<JQueryTerminal.Interpterer>, options?: TerminalOptions): JQueryTerminal;
    resizer(arg: TypeOrString<anyFunction>): JQuery;
    cmd(options?: CmdOptions): Cmd;
    text_length(): number;
    caret(pos?: number): number;
    visible(): JQuery;
    hidden(): JQuery;
}

interface JQueryStatic {
    omap(object: { [key: string]: anyFunction }, fn: mapFunction): { [key: string]: anyFunction };
    jrpc(url: string, method: string, params: any[], sucess?: (json: JSONObject, status: string, jqxhr: JQuery.jqXHR) => void, error?: (jqxhr: JQuery.jqXHR, status: string) => void): void;
    terminal: JQueryTerminalStatic;
}


interface JQueryTerminalStatic {
    version: string,
    data: string;
    color_names: string[];
    defaults: {
        formatters: TypeOrArray<JQueryTerminal.Formatter>;
        strings: JQueryTerminal.strings;
        [key: string]: any;
    };
    History(name?: string, size?: number, memory?: boolean): JQueryTerminal.History<any>;
    Stack(init?: any[]): JQueryTerminal.Stack<any>;
    Cycle(...args: any[]): JQueryTerminal.Cycle<any>;
    valid_color(color: string): boolean;
    unclosed_strings(str: string): boolean;
    escape_regex(str: string): string;
    have_formatting(str: string): boolean;
    is_formatting(str: string): boolean;
    format_split(str: string): string[];
    tracking_replace(str: string, rex: RegExp, replacement: string | JQueryTerminal.RegExpReplacementFunction, position: number): [string, number];
    iterate_formatting(str: string, callback: (data: JSONObject) => void): void;
    substring(str: string, start_index: number, end_index: number): string;
    normalize(str: string): string;
    split_equal(str: string, len: number, keep_words?: boolean): string[];
    amp(str: string): string;
    encode(str: string): string;
    nested_formatting(str: string): string;
    escape_formatting(str: string): string;
    /**
     * if options have position it will return [string, display_position]
     */
    apply_formatters(str: string, options: JSONObject): string | [string, number];
    format(str: string, options?: JQueryTerminal.formatOptions): string;
    escape_brackets(str: string): string;
    unescape_brackets(str: string): string;
    length(str: string): number;
    columns(arr: string[], cols: number, space: number): string;
    strip(str: string): string;
    active(): JQueryTerminal | void;
    last_id(): number;
    parse_argument(arg: string, strict?: boolean): number | RegExp | string;
    parse_arguments(str: string): Array<number | RegExp | string>;
    split_arguments(str: string): string[];
    parse_command(str: string): JQueryTerminal.ParsedCommand<number | RegExp | string>;
    split_command(str: string): JQueryTerminal.ParsedCommand<string>;
    parse_option(arg: string | string[], options?: { booleans: string[] }): JQueryTerminal.ParsedOptions;
    extended_command(term: JQueryTerminal, str: string): void;
    /**
     * formatter is an object that can be used in RegExp functions
     */
    formatter: any;
    Exception: TerminalExeption;
}


type TerminalExeption = {
    new(typeOrMessage: string, message?: string, stack?: string): TerminalExeption;
    message: string;
    type: string;
    stack?: string;
};

type CmdOptions = {
    mask?: string | boolean;
    caseSensitiveSearch?: boolean;
    historySize?: number;
    prompt?: JQueryTerminal.cmdPrompt;
    enabled?: boolean;
    history?: boolean | "memory";
    tabs?: number;
    onPositionChange?: (position: number, display_position: number) => void;
    clickTimeout?: number;
    holdTimeout?: number;
    holdRepeatTimeout?: number;
    width?: number;
    historyFilter?: JQueryTerminal.historyFilter;
    commands?: JQueryTerminal.commandsCmdFunction;
    char_width?: number;
    onCommandChange?: (this: Cmd, command: string) => void;
    name?: string;
    keypress?: JQueryTerminal.KeyEventHandler<Cmd>;
    keydown?: JQueryTerminal.KeyEventHandler<Cmd>;
}
type CmdOption = "mask" | "caseSensitiveSearch" | "historySize" |  "prompt" | "enabled" | "history" |
    "tabs" | "onPositionChange" |  "clickTimeout" |  "holdTimeout" |  "holdRepeatTimeout" |  "width" |
    "historyFilter" | "commands" | "char_width" | "onCommandChange" | "name" | "keypress" | "keydown";

// we copy methods from jQuery to overwrite it
// see: https://github.com/Microsoft/TypeScript/issues/978
interface Cmd<TElement = HTMLElement> extends JQuery<TElement> {
    option(name: CmdOption, value: any): Cmd;
    option(name: CmdOption): any;
    name(name: string): Cmd;
    name(): string;
    purge(): Cmd;
    history(): JQueryTerminal.History<string>;
    delete(count: number, stay?: boolean): string;
    set(command: string, stay?: boolean, silent?: boolean): Cmd;
    keymap(shortcut: string, callback: JQueryTerminal.keymapFunction<Cmd>): Cmd;
    keymap(shortcut: string): JQueryTerminal.keymapFunctionOptionalArg;
    keymap(arg: JQueryTerminal.keymapObject<Cmd>): Cmd;
    keymap(): JQueryTerminal.keymapObjectOptionalArg;
    insert(value: string, stay?: boolean): Cmd;
    /* jQuery types */
    get(index: number): TElement;
    get(): TElement[];
    get<T extends string>(): T;
    commands(fn: JQueryTerminal.commandsCmdFunction): Cmd;
    commands(): JQueryTerminal.commandsCmdFunction<void>;
    destroy(): Cmd;
    prompt(prompt: JQueryTerminal.cmdPrompt): Cmd;
    prompt(last_render: true): string;
    prompt<T extends JQueryTerminal.cmdPrompt<void>>(): T;
    kill_text(): string;
    position(): JQueryCoordinates;
    position<T extends number>(): number;
    position(value: number, silent?: boolean): Cmd;
    refresh(): Cmd;
    display_position(): number;
    display_position(value: number): Cmd;
    visible(): Cmd;
    //jQuery methods
    show(duration: JQuery.Duration, easing: string, complete: (this: TElement) => void): this;
    show(duration: JQuery.Duration, easing_complete: string | ((this: TElement) => void)): this;
    show(duration_complete_options?: JQuery.Duration | ((this: TElement) => void) | JQuery.EffectsOptions<TElement>): this;
    show(): Cmd;
    //jQuery methods
    resize<TData>(eventData: TData,
        handler: JQuery.EventHandler<TElement, TData> | JQuery.EventHandlerBase<any, JQuery.Event<TElement, TData>>): this;
    resize(handler?: JQuery.EventHandler<TElement> | JQuery.EventHandlerBase<any, JQuery.Event<TElement>> | false): this;
    //jQuery Terminal method
    resize(num_chars?: number): Cmd;
    enable(): Cmd;
    isenabled(): boolean;
    disable(focus?: boolean): Cmd;
    mask(mask: boolean | string): Cmd;
    mask<T extends boolean | string>(): T;
}
type TerminalOption =  "prompt" | "name" | "history" | "exit" | "clear" | "enabled" | "maskCHar" |
    "wrap" | "checkArity" | "invokeMethods" | "anyLinks" | "raw" |  "keymap" | "exceptionHandler" |
    "pauseEvents" | "softPause" | "memory" | "cancelableAjax" | "processArguments" | "onCommandChange" |
    "linksNoReferrer" | "javascriptLinks" | "processRPCResponse" | "completionEscape" | "convertLinks" |
    "unixFormattingEscapeBrackets" |  "extra" | "tabs" | "historySize" | "greetings" | "scrollObject" |
    "historyState" | "importHistory" | "historyFilter" | "echoCommand" | "scrollOnEcho" | "login" |
    "outputLimit" | "onAjaxError" | "pasteImage" | "scrollBottomOffset" | "wordAutocomplete" |
    "caseSensitiveAutocomplete" | "caseSensitiveSearch" | "clickTimeout" | "holdTimeout" |
    "holdRepeatTimeout" | "request" | "describe" | "onRPCError" | "doubleTab" | "completion" |
    "onInit" | "onClear" | "onBlur" | "onFocus" | "onExit" | "onTerminalChange" | "onPush" |
    "onPop" | "keypress" | "keydown" | "onAfterRedraw" | "onEchoCommand" | "onFlush" | "strings";


type TerminalOptions = {
    prompt?: JQueryTerminal.ExtendedPrompt;
    name?: string;
    history?: boolean;
    exit?: boolean;
    clear?: boolean;
    enabled?: boolean;
    maskCHar?: string;
    wrap?: boolean;
    checkArity?: boolean;
    invokeMethods?: boolean;
    anyLinks?: boolean;
    raw?: boolean;
    keymap?: JQueryTerminal.keymapObject;
    exceptionHandler?: null | JQueryTerminal.ExceptionHandler;
    pauseEvents?: boolean;
    softPause?: boolean;
    memory?: boolean;
    cancelableAjax?: boolean;
    processArguments?: boolean;
    onCommandChange?: (this: JQueryTerminal, command: string) => void;
    linksNoReferrer?: boolean;
    javascriptLinks?: boolean;
    processRPCResponse?: null | JQueryTerminal.processRPCResponseFunction;
    completionEscape?: boolean;
    convertLinks?: boolean;
    unixFormattingEscapeBrackets?: boolean; // provided by unix_formatting
    extra?: any;
    tabs?: number;
    historySize?: number;
    greetings?: JQueryTerminal.greetingsArg;
    scrollObject?: null | JQuery.Selector | HTMLElement | JQuery;
    historyState?: boolean;
    importHistory?: boolean;
    historyFilter?: JQueryTerminal.historyFilter;
    echoCommand?: boolean;
    scrollOnEcho?: boolean;
    login?: JQueryTerminal.LoginArgument;
    outputLimit?: number;
    onAjaxError?: (this: JQueryTerminal, xhr: JQuery.jqXHR, status: string, error: string) => void;
    pasteImage?: boolean;
    scrollBottomOffset?: boolean;
    wordAutocomplete?: boolean;
    caseSensitiveAutocomplete?: boolean;
    caseSensitiveSearch?: boolean;
    clickTimeout?: number;
    holdTimeout?: number;
    holdRepeatTimeout?: number;
    request?: JQueryTerminal.RequestResponseCallback;
    response?: JQueryTerminal.RequestResponseCallback;
    describe?: string;
    onRPCError?: JQueryTerminal.RPCErrorCallback;
    doubleTab?: JQueryTerminal.DoubleTabFunction;
    completion?: JQueryTerminal.Completion;
    onInit?: JQueryTerminal.EventCallback;
    onClear?: JQueryTerminal.EventCallback;
    onBlur?: JQueryTerminal.EventCallback;
    onFocus?: JQueryTerminal.EventCallback;
    onExit?: JQueryTerminal.EventCallback;
    onTerminalChange?: JQueryTerminal.EventCallback;
    onPush?: JQueryTerminal.PushPopCallback;
    onPop?: JQueryTerminal.PushPopCallback;
    keypress?: JQueryTerminal.KeyEventHandler;
    keydown?: JQueryTerminal.KeyEventHandler;
    onAfterRedraw?: JQueryTerminal.EventCallback;
    onEchoCommand?: (this: JQueryTerminal, div: JQuery, command: string) => void;
    onFlush?: JQueryTerminal.EventCallback;
    strings?: JQueryTerminal.strings;
}

interface JQueryTerminal<TElement = HTMLElement> extends JQuery<TElement> {
    set_command(command: string): JQueryTerminal;
    id(): number;
    clear(): JQueryTerminal;
    export_view(): JQueryTerminal.View;
    import_view(view: JQueryTerminal.View): JQueryTerminal;
    save_state(command?: string, ignore_hash?: boolean, index?: number): JQueryTerminal;
    exec(command: string, silent?: boolean, defered?: JQuery.Deferred<void>): JQuery.Promise<void>;
    autologin(user: string, token: string, silent?: boolean): JQueryTerminal;
    // there is success and error callbacks because we call this function from terminal and auth function can
    // be created by user
    login(auth: JQueryTerminal.LoginFunction, infinite?: boolean, success?: () => void, error?: () => void): JQueryTerminal;
    settings(): any; // we use any because option types have optional values that will throw error when used
    before_cursor(word?: boolean): string;
    complete(commands: string[], options?: JQueryTerminal.CompleteOptions): boolean;
    commands(): JQueryTerminal.interpterFunction;
    set_interpreter(interpter: TypeOrArray<JQueryTerminal.Interpterer>, login?: JQueryTerminal.LoginArgument): JQueryTerminal;
    greetings(): JQueryTerminal;
    paused(): boolean;
    pause(): JQueryTerminal;
    resume(): JQueryTerminal;
    cols(): number;
    rows(): number;
    history(): JQueryTerminal.History<string>;
    history_state(toogle: boolean): JQueryTerminal;
    clear_history_state(): JQueryTerminal;
    next(selector?: JQuery.Selector): this;
    next(): JQueryTerminal;
    focus(handler?: JQuery.EventHandler<TElement> | JQuery.EventHandlerBase<any, JQuery.Event<TElement>> | false): this;
    focus<TData>(eventData: TData,
        handler: JQuery.EventHandler<TElement, TData> | JQuery.EventHandlerBase<any, JQuery.Event<TElement, TData>>): this;
    focus(toggle?: boolean): JQueryTerminal;
    freeze(toogle?: boolean): JQueryTerminal;
    frozen(): boolean;
    enable(silent?: boolean): JQueryTerminal;
    disable(silent?: boolean): JQueryTerminal;
    enabled(): boolean;
    signature(): string;
    version(): string;
    cmd(): Cmd;
    get_command(): string;
    set_command(cmd: string, silent?: boolean): JQueryTerminal;
    set_position(pos: number, relative?: boolean): JQueryTerminal;
    get_position(): number;
    insert(str: string, stay?: boolean): JQueryTerminal;
    set_prompt(prompt: JQueryTerminal.ExtendedPrompt): JQueryTerminal;
    get_prompt<T extends JQueryTerminal.ExtendedPrompt>(): T;
    set_mask(toggle?: boolean | string): JQueryTerminal;
    get_output<T extends JQueryTerminal.Lines | string[]>(raw?: boolean): T;
    resize<TData>(eventData: TData,
        handler: JQuery.EventHandler<TElement, TData> | JQuery.EventHandlerBase<any, JQuery.Event<TElement, TData>>): this;
    resize(handler?: JQuery.EventHandler<TElement> | JQuery.EventHandlerBase<any, JQuery.Event<TElement>> | false): this;
    resize(width?: number, height?: number): JQueryTerminal;
    refresh(): JQueryTerminal;
    flush(options?: { update?: boolean, scroll?: boolean }): JQueryTerminal;
    update(line: number, str: string, options?: JQueryTerminal.EchoOptions): JQueryTerminal;
    // options for remove_line is useless but that's how API look like
    remove_line(line: number): JQueryTerminal;
    last_index(): number;
    echo(arg: TypeOrPromise<JQueryTerminal.echoValue>, options?: JQueryTerminal.EchoOptions): JQueryTerminal;
    error(arg: JQueryTerminal.errorArgument, options?: JQueryTerminal.EchoOptions): JQueryTerminal;
    exception<T extends Error>(e: T, label?: string): JQueryTerminal;
    scroll<TData>(eventData: TData,
        handler: JQuery.EventHandler<TElement, TData> | JQuery.EventHandlerBase<any, JQuery.Event<TElement, TData>>): this;
    scroll(handler?: JQuery.EventHandler<TElement> | JQuery.EventHandlerBase<any, JQuery.Event<TElement>> | false): this;
    scroll(amount: number): JQueryTerminal;
    logout(local?: boolean): JQueryTerminal;
    token<T extends string | void>(local?: boolean): T;
    set_token(token?: string, local?: boolean): JQueryTerminal;
    get_token<T extends string | void>(local?: boolean): T;
    login_name<T extends string | void>(local?: boolean): T;
    name(): string;
    prefix_name(local?: boolean): string;
    read(message: string, success?: (result: string) => void, cancel?: voidFunction): JQuery.Promise<string>;
    push(interpreter: TypeOrArray<JQueryTerminal.Interpterer>, options?: JQueryTerminal.pushOptions): JQueryTerminal;
    pop(echoCommand?: string, silent?: boolean): JQueryTerminal;
    option(options: TerminalOptions | TerminalOption, value?: any): any;
    invoke_key(shorcut: string): JQueryTerminal;
    keymap(shortcut: string, callback: JQueryTerminal.keymapFunction): JQueryTerminal;
    keymap(shortcut: string): JQueryTerminal.keymapFunctionOptionalArg;
    keymap(arg: JQueryTerminal.keymapObject): JQueryTerminal;
    keymap(): JQueryTerminal.keymapObjectOptionalArg;
    //keymap<T extends JQueryTerminal.keymapObject | JQueryTerminal.keymapFunctionOptionalArg | JQueryTerminal>(keymap?: JQueryTerminal.keymapObject | string, fn?: JQueryTerminal.keymapFunction): T;
    level(): number;
    reset(): JQueryTerminal;
    purge(): JQueryTerminal;
    destroy(): JQueryTerminal;
    scroll_to_bottom(): JQueryTerminal;
    is_bottom(): boolean;
}
