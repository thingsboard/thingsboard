import { cloneArr, compact, contains, difference, flatten, forEach, has, isArray, isEmpty, map } from "../utils/utils";
export function tokenStructuredMatcher(tokInstance, tokConstructor) {
    var instanceType = tokInstance.tokenTypeIdx;
    if (instanceType === tokConstructor.tokenTypeIdx) {
        return true;
    }
    else {
        return (tokConstructor.isParent === true &&
            tokConstructor.categoryMatchesMap[instanceType] === true);
    }
}
// Optimized tokenMatcher in case our grammar does not use token categories
// Being so tiny it is much more likely to be in-lined and this avoid the function call overhead
export function tokenStructuredMatcherNoCategories(token, tokType) {
    return token.tokenTypeIdx === tokType.tokenTypeIdx;
}
export var tokenShortNameIdx = 1;
export var tokenIdxToClass = {};
export function augmentTokenTypes(tokenTypes) {
    // collect the parent Token Types as well.
    var tokenTypesAndParents = expandCategories(tokenTypes);
    // add required tokenType and categoryMatches properties
    assignTokenDefaultProps(tokenTypesAndParents);
    // fill up the categoryMatches
    assignCategoriesMapProp(tokenTypesAndParents);
    assignCategoriesTokensProp(tokenTypesAndParents);
    forEach(tokenTypesAndParents, function (tokType) {
        tokType.isParent = tokType.categoryMatches.length > 0;
    });
}
export function expandCategories(tokenTypes) {
    var result = cloneArr(tokenTypes);
    var categories = tokenTypes;
    var searching = true;
    while (searching) {
        categories = compact(flatten(map(categories, function (currTokType) { return currTokType.CATEGORIES; })));
        var newCategories = difference(categories, result);
        result = result.concat(newCategories);
        if (isEmpty(newCategories)) {
            searching = false;
        }
        else {
            categories = newCategories;
        }
    }
    return result;
}
export function assignTokenDefaultProps(tokenTypes) {
    forEach(tokenTypes, function (currTokType) {
        if (!hasShortKeyProperty(currTokType)) {
            tokenIdxToClass[tokenShortNameIdx] = currTokType;
            currTokType.tokenTypeIdx = tokenShortNameIdx++;
        }
        // CATEGORIES? : TokenType | TokenType[]
        if (hasCategoriesProperty(currTokType) &&
            !isArray(currTokType.CATEGORIES)
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
export function assignCategoriesTokensProp(tokenTypes) {
    forEach(tokenTypes, function (currTokType) {
        // avoid duplications
        currTokType.categoryMatches = [];
        forEach(currTokType.categoryMatchesMap, function (val, key) {
            currTokType.categoryMatches.push(tokenIdxToClass[key].tokenTypeIdx);
        });
    });
}
export function assignCategoriesMapProp(tokenTypes) {
    forEach(tokenTypes, function (currTokType) {
        singleAssignCategoriesToksMap([], currTokType);
    });
}
export function singleAssignCategoriesToksMap(path, nextNode) {
    forEach(path, function (pathNode) {
        nextNode.categoryMatchesMap[pathNode.tokenTypeIdx] = true;
    });
    forEach(nextNode.CATEGORIES, function (nextCategory) {
        var newPath = path.concat(nextNode);
        // avoids infinite loops due to cyclic categories.
        if (!contains(newPath, nextCategory)) {
            singleAssignCategoriesToksMap(newPath, nextCategory);
        }
    });
}
export function hasShortKeyProperty(tokType) {
    return has(tokType, "tokenTypeIdx");
}
export function hasCategoriesProperty(tokType) {
    return has(tokType, "CATEGORIES");
}
export function hasExtendingTokensTypesProperty(tokType) {
    return has(tokType, "categoryMatches");
}
export function hasExtendingTokensTypesMapProperty(tokType) {
    return has(tokType, "categoryMatchesMap");
}
export function isTokenType(tokType) {
    return has(tokType, "tokenTypeIdx");
}
//# sourceMappingURL=tokens.js.map