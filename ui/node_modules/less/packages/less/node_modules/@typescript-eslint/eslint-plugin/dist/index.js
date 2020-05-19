"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
const rules_1 = __importDefault(require("./rules"));
const all_json_1 = __importDefault(require("./configs/all.json"));
const base_json_1 = __importDefault(require("./configs/base.json"));
const recommended_json_1 = __importDefault(require("./configs/recommended.json"));
const eslint_recommended_1 = __importDefault(require("./configs/eslint-recommended"));
module.exports = {
    rules: rules_1.default,
    configs: {
        all: all_json_1.default,
        base: base_json_1.default,
        recommended: recommended_json_1.default,
        'eslint-recommended': eslint_recommended_1.default,
    },
};
//# sourceMappingURL=index.js.map