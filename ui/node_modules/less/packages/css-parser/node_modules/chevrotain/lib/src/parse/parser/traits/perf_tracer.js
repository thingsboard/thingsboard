"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var utils_1 = require("../../../utils/utils");
var parser_1 = require("../parser");
/**
 * Trait responsible for runtime parsing errors.
 */
var PerformanceTracer = /** @class */ (function () {
    function PerformanceTracer() {
    }
    PerformanceTracer.prototype.initPerformanceTracer = function (config) {
        if (utils_1.has(config, "traceInitPerf")) {
            var userTraceInitPerf = config.traceInitPerf;
            var traceIsNumber = typeof userTraceInitPerf === "number";
            this.traceInitMaxIdent = traceIsNumber
                ? userTraceInitPerf
                : Infinity;
            this.traceInitPerf = traceIsNumber
                ? userTraceInitPerf > 0
                : userTraceInitPerf;
        }
        else {
            this.traceInitMaxIdent = 0;
            this.traceInitPerf = parser_1.DEFAULT_PARSER_CONFIG.traceInitPerf;
        }
        this.traceInitIndent = -1;
    };
    PerformanceTracer.prototype.TRACE_INIT = function (phaseDesc, phaseImpl) {
        // No need to optimize this using NOOP pattern because
        // It is not called in a hot spot...
        if (this.traceInitPerf === true) {
            this.traceInitIndent++;
            var indent = new Array(this.traceInitIndent + 1).join("\t");
            if (this.traceInitIndent < this.traceInitMaxIdent) {
                console.log(indent + "--> <" + phaseDesc + ">");
            }
            var _a = utils_1.timer(phaseImpl), time = _a.time, value = _a.value;
            /* istanbul ignore next - Difficult to reproduce specific performance behavior (>10ms) in tests */
            var traceMethod = time > 10 ? console.warn : console.log;
            if (this.traceInitIndent < this.traceInitMaxIdent) {
                traceMethod(indent + "<-- <" + phaseDesc + "> time: " + time + "ms");
            }
            this.traceInitIndent--;
            return value;
        }
        else {
            return phaseImpl();
        }
    };
    return PerformanceTracer;
}());
exports.PerformanceTracer = PerformanceTracer;
//# sourceMappingURL=perf_tracer.js.map