export declare function warning(valid: boolean, message: string): void;
export declare function note(valid: boolean, message: string): void;
export declare function resetWarned(): void;
export declare function call(method: (valid: boolean, message: string) => void, valid: boolean, message: string): void;
export declare function warningOnce(valid: boolean, message: string): void;
export declare function noteOnce(valid: boolean, message: string): void;
export default warningOnce;
