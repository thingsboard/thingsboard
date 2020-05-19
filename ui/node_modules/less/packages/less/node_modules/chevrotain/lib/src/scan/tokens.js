"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var utils_1 = require("../utils/utils");
function tokenStructuredMatcher(tokInstance, tokConstructor) {
    var instanceType = tokInstance.tokenTypeIdx;
    if (instanceType === tokConstructor.tokenTypeIdx) {
        return true;
    }
    else {
        return (tokConstructor.isParent === true &&
            tokConstructor.categoryMatchesMap[instanceType] === true);
    }
}
exports.tokenStructuredMatcher = tokenStructuredMatcher;
// Optimized tokenMatcher in case our grammar does not use token categories
// Being so tiny it is much more likely to be in-lined and this avoid the function call overhead
function tokenStructuredMatcherNoCategories(token, tokType) {
    return token.tokenTypeIdx === tokType.tokenTypeIdx;
}
exports.tokenStructuredMatcherNoCategories = tokenStructuredMatcherNoCategories;
exports.tokenShortNameIdx = 1;
exports.tokenIdxToClass = {};
function augmentTokenTypes(tokenTypes) {
    // collect the parent Token Types as well.
    var tokenTypesAndParents = expandCategories(tokenTypes);
    // add required tokenType and categoryMatches properties
    assignTokenDefaultProps(tokenTypesAndParents);
    // fill up the categoryMatches
    assignCategoriesMapProp(tokenTypesAndParents);
    assignCategoriesTokensProp(tokenTypesAndParents);
    utils_1.forEach(tokenTypesAndParents, function (tokType) {
        tokType.isParent = tokType.categoryMatches.length > 0;
    });
}
exports.augmentTokenTypes = augmentTokenTypes;
function expandCategories(tokenTypes) {
    var result = utils_1.cloneArr(tokenTypes);
    var categories = tokenTypes;
    var searching = true;
    while (searching) {
        categories = utils_1.compact(utils_1.flatten(utils_1.map(categories, function (currTokType) { return currTokType.CATEGORIES; })));
        var newCategories = utils_1.difference(categories, result);
        result = result.concat(newCategories);
        if (utils_1.isEmpty(newCategories)) {
            searching = false;
        }
        else {
            categories = newCategories;
        }
    }
    return result;
}
exports.expandCategories = expandCategories;
function assignTokenDefaultProps(tokenTypes) {
    utils_1.forEach(tokenTypes, function (currTokType) {
        if (!hasShortKeyProperty(currTokType)) {
            exports.tokenIdxToClass[exports.tokenShortNameIdx] = currTokType;
            currTokType.tokenTypeIdx = exports.tokenShortNameIdx++;
        }
        // CATEGORIES? : TokenType | TokenType[]
        if (hasCategoriesProperty(currTokType) &&
            !utils_1.isArray(currTokType.CATEGORIES)
        // &&
        // !isUndefined(currTokType.CATEGORIES.PATTERN)
        ) {
            currTokType.CATEGORIES = [currTokType.CATEGORIES];
        }
        if (!hasCategoriesProperty(currTokType)) {
            currTokType.CATEGORIES = [];
        }
        if (!hasExtendingTokensTypesProperty(currTokType)) {
            currTokType.categoryMatches = [];
        }
        if (!hasExtendingTokensTypesMapProperty(currTokType)) {
            currTokType.categoryMatchesMap = {};
        }
    });
}
exports.assignTokenDefaultProps = assignTokenDefaultProps;
function assignCategoriesTokensProp(tokenTypes) {
    utils_1.forEach(tokenTypes, function (currTokType) {
        // avoid duplications
        currTokType.categoryMatches = [];
        utils_1.forEach(currTokType.categoryMatchesMap, function (val, key) {
            currTokType.categoryMatches.push(exports.tokenIdxToClass[key].tokenTypeIdx);
        });
    });
}
exports.assignCategoriesTokensProp = assignCategoriesTokensProp;
function assignCategoriesMapProp(tokenTypes) {
    utils_1.forEach(tokenTypes, function (currTokType) {
        singleAssignCategoriesToksMap([], currTokType);
    });
}
exports.assignCategoriesMapProp = assignCategoriesMapProp;
function singleAssignCategoriesToksMap(path, nextNode) {
    utils_1.forEach(path, function (pathNode) {
        nextNode.categoryMatchesMap[pathNode.tokenTypeIdx] = true;
    });
    utils_1.forEach(nextNode.CATEGORIES, function (nextCategory) {
        var newPath = path.concat(nextNode);
        // avoids infinite loops due to cyclic categories.
        if (!utils_1.contains(newPath, nextCategory)) {
            singleAssignCategoriesToksMap(newPath, nextCategory);
        }
    });
}
exports.singleAssignCategoriesToksMap = singleAssignCategoriesToksMap;
function hasShortKeyProperty(tokType) {
    return utils_1.has(tokType, "tokenTypeIdx");
}
exports.hasShortKeyProperty = hasShortKeyProperty;
function hasCategoriesProperty(tokType) {
    return utils_1.has(tokType, "CATEGORIES");
}
exports.hasCategoriesProperty = hasCategoriesProperty;
function hasExtendingTokensTypesProperty(tokType) {
    return utils_1.has(tokType, "categoryMatches");
}
exports.hasExtendingTokensTypesProperty = hasExtendingTokensTypesProperty;
function hasExtendingTokensTypesMapProperty(tokType) {
    return utils_1.has(tokType, "categoryMatchesMap");
}
exports.hasExtendingTokensTypesMapProperty = hasExtendingTokensTypesMapProperty;
function isTokenType(tokType) {
    return utils_1.has(tokType, "tokenTypeIdx");
}
exports.isTokenType = isTokenType;
//# sourceMappingURL=tokens.js.map