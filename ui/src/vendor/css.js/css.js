/* eslint-disable */

/* jshint unused:false */
/* global base64_decode, CSSWizardView, window, console, jQuery */
var fi = function () {

    this.cssImportStatements = [];
    this.cssKeyframeStatements = [];

    this.cssRegex = new RegExp('([\\s\\S]*?){([\\s\\S]*?)}', 'gi');
    this.cssMediaQueryRegex = '((@media [\\s\\S]*?){([\\s\\S]*?}\\s*?)})';
    this.cssKeyframeRegex = '((@.*?keyframes [\\s\\S]*?){([\\s\\S]*?}\\s*?)})';
    this.combinedCSSRegex = '((\\s*?@media[\\s\\S]*?){([\\s\\S]*?)}\\s*?})|(([\\s\\S]*?){([\\s\\S]*?)})'; //to match css & media queries together
    this.cssCommentsRegex = '(\\/\\*[\\s\\S]*?\\*\\/)';
    this.cssImportStatementRegex = new RegExp('@import .*?;', 'gi');
};

/*
 Strip outs css comments and returns cleaned css string

 @param css, the original css string to be stipped out of comments

 @return cleanedCSS contains no css comments
 */
fi.prototype.stripComments = function (cssString) {
    var regex = new RegExp(this.cssCommentsRegex, 'gi');

    return cssString.replace(regex, '');
};

/*
 Parses given css string, and returns css object
 keys as selectors and values are css rules
 eliminates all css comments before parsing

 @param source css string to be parsed

 @return object css
 */
fi.prototype.parseCSS = function (source) {

    if (source === undefined) {
        return [];
    }

    var css = [];
    //strip out comments
    //source = this.stripComments(source);

    //get import statements

    while (true) {
        var imports = this.cssImportStatementRegex.exec(source);
        if (imports !== null) {
            this.cssImportStatements.push(imports[0]);
            css.push({
                selector: '@imports',
                type: 'imports',
                styles: imports[0]
            });
        } else {
            break;
        }
    }
    source = source.replace(this.cssImportStatementRegex, '');
    //get keyframe statements
    var keyframesRegex = new RegExp(this.cssKeyframeRegex, 'gi');
    var arr;
    while (true) {
        arr = keyframesRegex.exec(source);
        if (arr === null) {
            break;
        }
        css.push({
            selector: '@keyframes',
            type: 'keyframes',
            styles: arr[0]
        });
    }
    source = source.replace(keyframesRegex, '');

    //unified regex
    var unified = new RegExp(this.combinedCSSRegex, 'gi');

    while (true) {
        arr = unified.exec(source);
        if (arr === null) {
            break;
        }
        var selector = '';
        if (arr[2] === undefined) {
            selector = arr[5].split('\r\n').join('\n').trim();
        } else {
            selector = arr[2].split('\r\n').join('\n').trim();
        }

        /*
         fetch comments and associate it with current selector
         */
        var commentsRegex = new RegExp(this.cssCommentsRegex, 'gi');
        var comments = commentsRegex.exec(selector);
        if (comments !== null) {
            selector = selector.replace(commentsRegex, '').trim();
        }

        //determine the type
        if (selector.indexOf('@media') !== -1) {
            //we have a media query
            var cssObject = {
                selector: selector,
                type: 'media',
                subStyles: this.parseCSS(arr[3] + '\n}') //recursively parse media query inner css
            };
            if (comments !== null) {
                cssObject.comments = comments[0];
            }
            css.push(cssObject);
        } else {
            //we have standart css
            var rules = this.parseRules(arr[6]);
            var style = {
                selector: selector,
                rules: rules
            };
            if (selector === '@font-face') {
                style.type = 'font-face';
            }
            if (comments !== null) {
                style.comments = comments[0];
            }
            css.push(style);
        }
    }

    return css;
};

/*
 parses given string containing css directives
 and returns an array of objects containing ruleName:ruleValue pairs

 @param rules, css directive string example
 \n\ncolor:white;\n    font-size:18px;\n
 */
fi.prototype.parseRules = function (rules) {
    //convert all windows style line endings to unix style line endings
    rules = rules.split('\r\n').join('\n');
    var ret = [];

    rules = rules.split(';');

    //proccess rules line by line
    for (var i = 0; i < rules.length; i++) {
        var line = rules[i];

        //determine if line is a valid css directive, ie color:white;
        line = line.trim();
        if (line.indexOf(':') !== -1) {
            //line contains :
            line = line.split(':');
            var cssDirective = line[0].trim();
            var cssValue = line.slice(1).join(':').trim();

            //more checks
            if (cssDirective.length < 1 || cssValue.length < 1) {
                continue; //there is no css directive or value that is of length 1 or 0
                // PLAIN WRONG WHAT ABOUT margin:0; ?
            }

            //push rule
            ret.push({
                directive: cssDirective,
                value: cssValue
            });
        } else {
            //if there is no ':', but what if it was mis splitted value which starts with base64
            if (line.trim().substr(0, 7) == 'base64,') { //hack :)
                ret[ret.length - 1].value += line.trim();
            } else {
                //add rule, even if it is defective
                if (line.length > 0) {
                    ret.push({
                        directive: '',
                        value: line,
                        defective: true
                    });
                }
            }
        }
    }

    return ret; //we are done!
};
/*
 just returns the rule having given directive
 if not found returns false;
 */
fi.prototype.findCorrespondingRule = function (rules, directive, value) {
    if (value === undefined) {
        value = false;
    }
    var ret = false;
    for (var i = 0; i < rules.length; i++) {
        if (rules[i].directive == directive) {
            ret = rules[i];
            if (value === rules[i].value) {
                break;
            }
        }
    }
    return ret;
};

/*
 Finds styles that have given selector, compress them,
 and returns them
 */
fi.prototype.findBySelector = function (cssObjectArray, selector, contains) {
    if (contains === undefined) {
        contains = false;
    }

    var found = [];
    for (var i = 0; i < cssObjectArray.length; i++) {
        if (contains === false) {
            if (cssObjectArray[i].selector === selector) {
                found.push(cssObjectArray[i]);
            }
        } else {
            if (cssObjectArray[i].selector.indexOf(selector) !== -1) {
                found.push(cssObjectArray[i]);
            }
        }

    }
    if (found.length < 2) {
        return found;
    } else {
        var base = found[0];
        for (i = 1; i < found.length; i++) {
            this.intelligentCSSPush([base], found[i]);
        }
        return [base]; //we are done!! all properties merged into base!
    }
};

/*
 deletes cssObjects having given selector, and returns new array
 */
fi.prototype.deleteBySelector = function (cssObjectArray, selector) {
    var ret = [];
    for (var i = 0; i < cssObjectArray.length; i++) {
        if (cssObjectArray[i].selector !== selector) {
            ret.push(cssObjectArray[i]);
        }
    }
    return ret;
};

/*
 Compresses given cssObjectArray and tries to minimize
 selector redundence.
 */
fi.prototype.compressCSS = function (cssObjectArray) {
    var compressed = [];
    var done = {};
    for (var i = 0; i < cssObjectArray.length; i++) {
        var obj = cssObjectArray[i];
        if (done[obj.selector] === true) {
            continue;
        }

        var found = this.findBySelector(cssObjectArray, obj.selector); //found compressed
        if (found.length !== 0) {
            compressed.push(found[0]);
            done[obj.selector] = true;
        }
    }
    return compressed;
};

/*
 Received 2 css objects with following structure
 {
 rules : [{directive:"", value:""}, {directive:"", value:""}, ...]
 selector : "SOMESELECTOR"
 }

 returns the changed(new,removed,updated) values on css1 parameter, on same structure

 if two css objects are the same, then returns false

 if a css directive exists in css1 and     css2, and its value is different, it is included in diff
 if a css directive exists in css1 and not css2, it is then included in diff
 if a css directive exists in css2 but not css1, then it is deleted in css1, it would be included in diff but will be marked as type='DELETED'

 @object css1 css object
 @object css2 css object

 @return diff css object contains changed values in css1 in regards to css2 see test input output in /test/data/css.js
 */
fi.prototype.cssDiff = function (css1, css2) {
    if (css1.selector !== css2.selector) {
        return false;
    }

    //if one of them is media query return false, because diff function can not operate on media queries
    if ((css1.type === 'media' || css2.type === 'media')) {
        return false;
    }

    var diff = {
        selector: css1.selector,
        rules: []
    };
    var rule1, rule2;
    for (var i = 0; i < css1.rules.length; i++) {
        rule1 = css1.rules[i];
        //find rule2 which has the same directive as rule1
        rule2 = this.findCorrespondingRule(css2.rules, rule1.directive, rule1.value);
        if (rule2 === false) {
            //rule1 is a new rule in css1
            diff.rules.push(rule1);
        } else {
            //rule2 was found only push if its value is different too
            if (rule1.value !== rule2.value) {
                diff.rules.push(rule1);
            }
        }
    }

    //now for rules exists in css2 but not in css1, which means deleted rules
    for (var ii = 0; ii < css2.rules.length; ii++) {
        rule2 = css2.rules[ii];
        //find rule2 which has the same directive as rule1
        rule1 = this.findCorrespondingRule(css1.rules, rule2.directive);
        if (rule1 === false) {
            //rule1 is a new rule
            rule2.type = 'DELETED'; //mark it as a deleted rule, so that other merge operations could be true
            diff.rules.push(rule2);
        }
    }


    if (diff.rules.length === 0) {
        return false;
    }
    return diff;
};

/*
 Merges 2 different css objects together
 using intelligentCSSPush,

 @param cssObjectArray, target css object array
 @param newArray, source array that will be pushed into cssObjectArray parameter
 @param reverse, [optional], if given true, first parameter will be traversed on reversed order
 effectively giving priority to the styles in newArray
 */
fi.prototype.intelligentMerge = function (cssObjectArray, newArray, reverse) {
    if (reverse === undefined) {
        reverse = false;
    }


    for (var i = 0; i < newArray.length; i++) {
        this.intelligentCSSPush(cssObjectArray, newArray[i], reverse);
    }
    for (i = 0; i < cssObjectArray.length; i++) {
        var cobj = cssObjectArray[i];
        if (cobj.type === 'media' || (cobj.type === 'keyframes')) {
            continue;
        }
        cobj.rules = this.compactRules(cobj.rules);
    }
};

/*
 inserts new css objects into a bigger css object
 with same selectors groupped together

 @param cssObjectArray, array of bigger css object to be pushed into
 @param minimalObject, single css object
 @param reverse [optional] default is false, if given, cssObjectArray will be reversly traversed
 resulting more priority in minimalObject's styles
 */
fi.prototype.intelligentCSSPush = function (cssObjectArray, minimalObject, reverse) {
    var pushSelector = minimalObject.selector;
    //find correct selector if not found just push minimalObject into cssObject
    var cssObject = false;

    if (reverse === undefined) {
        reverse = false;
    }

    if (reverse === false) {
        for (var i = 0; i < cssObjectArray.length; i++) {
            if (cssObjectArray[i].selector === minimalObject.selector) {
                cssObject = cssObjectArray[i];
                break;
            }
        }
    } else {
        for (var j = cssObjectArray.length - 1; j > -1; j--) {
            if (cssObjectArray[j].selector === minimalObject.selector) {
                cssObject = cssObjectArray[j];
                break;
            }
        }
    }

    if (cssObject === false) {
        cssObjectArray.push(minimalObject); //just push, because cssSelector is new
    } else {
        if (minimalObject.type !== 'media') {
            for (var ii = 0; ii < minimalObject.rules.length; ii++) {
                var rule = minimalObject.rules[ii];
                //find rule inside cssObject
                var oldRule = this.findCorrespondingRule(cssObject.rules, rule.directive);
                if (oldRule === false) {
                    cssObject.rules.push(rule);
                } else if (rule.type == 'DELETED') {
                    oldRule.type = 'DELETED';
                } else {
                    //rule found just update value

                    oldRule.value = rule.value;
                }
            }
        } else {
            cssObject.subStyles = minimalObject.subStyles; //TODO, make this intelligent too
        }

    }
};

/*
 filter outs rule objects whose type param equal to DELETED

 @param rules, array of rules

 @returns rules array, compacted by deleting all unneccessary rules
 */
fi.prototype.compactRules = function (rules) {
    var newRules = [];
    for (var i = 0; i < rules.length; i++) {
        if (rules[i].type !== 'DELETED') {
            newRules.push(rules[i]);
        }
    }
    return newRules;
};
/*
 computes string for ace editor using this.css or given cssBase optional parameter

 @param [optional] cssBase, if given computes cssString from cssObject array
 */
fi.prototype.getCSSForEditor = function (cssBase, depth) {
    if (depth === undefined) {
        depth = 0;
    }
    var ret = '';
    if (cssBase === undefined) {
        cssBase = this.css;
    }
    //append imports
    for (var i = 0; i < cssBase.length; i++) {
        if (cssBase[i].type == 'imports') {
            ret += cssBase[i].styles + '\n\n';
        }
    }
    for (i = 0; i < cssBase.length; i++) {
        var tmp = cssBase[i];
        if (tmp.selector === undefined) { //temporarily omit media queries
            continue;
        }
        var comments = "";
        if (tmp.comments !== undefined) {
            comments = tmp.comments + '\n';
        }

        if (tmp.type == 'media') { //also put media queries to output
            ret += comments + tmp.selector + '{\n';
            ret += this.getCSSForEditor(tmp.subStyles, depth + 1);
            ret += '}\n\n';
        } else if (tmp.type !== 'keyframes' && tmp.type !== 'imports') {
            ret += this.getSpaces(depth) + comments + tmp.selector + ' {\n';
            ret += this.getCSSOfRules(tmp.rules, depth + 1);
            ret += this.getSpaces(depth) + '}\n\n';
        }
    }

    //append keyFrames
    for (i = 0; i < cssBase.length; i++) {
        if (cssBase[i].type == 'keyframes') {
            ret += cssBase[i].styles + '\n\n';
        }
    }

    return ret;
};

fi.prototype.getImports = function (cssObjectArray) {
    var imps = [];
    for (var i = 0; i < cssObjectArray.length; i++) {
        if (cssObjectArray[i].type == 'imports') {
            imps.push(cssObjectArray[i].styles);
        }
    }
    return imps;
};
/*
 given rules array, returns visually formatted css string
 to be used inside editor
 */
fi.prototype.getCSSOfRules = function (rules, depth) {
    var ret = '';
    for (var i = 0; i < rules.length; i++) {
        if (rules[i] === undefined) {
            continue;
        }
        if (rules[i].defective === undefined) {
            ret += this.getSpaces(depth) + rules[i].directive + ' : ' + rules[i].value + ';\n';
        } else {
            ret += this.getSpaces(depth) + rules[i].value + ';\n';
        }

    }
    return ret || '\n';
};

/*
 A very simple helper function returns number of spaces appended in a single string,
 the number depends input parameter, namely input*2
 */
fi.prototype.getSpaces = function (num) {
    var ret = '';
    for (var i = 0; i < num * 4; i++) {
        ret += ' ';
    }
    return ret;
};

/*
 Given css string or objectArray, parses it and then for every selector,
 prepends this.cssPreviewNamespace to prevent css collision issues

 @returns css string in which this.cssPreviewNamespace prepended
 */
fi.prototype.applyNamespacing = function (css, forcedNamespace) {
    var cssObjectArray = css;
    var namespaceClass = '.' + this.cssPreviewNamespace;
    if (forcedNamespace !== undefined) {
        namespaceClass = forcedNamespace;
    }

    if (typeof css === 'string') {
        cssObjectArray = this.parseCSS(css);
    }

    for (var i = 0; i < cssObjectArray.length; i++) {
        var obj = cssObjectArray[i];

        //bypass namespacing for @font-face @keyframes @import
        if (obj.selector.indexOf('@font-face') > -1 || obj.selector.indexOf('keyframes') > -1 || obj.selector.indexOf('@import') > -1 || obj.selector.indexOf('.form-all') > -1 || obj.selector.indexOf('#stage') > -1) {
            continue;
        }

        if (obj.type !== 'media') {
            var selector = obj.selector.split(',');
            var newSelector = [];
            for (var j = 0; j < selector.length; j++) {
                if (selector[j].indexOf('.supernova') === -1) { //do not apply namespacing to selectors including supernova
                    newSelector.push(namespaceClass + ' ' + selector[j]);
                } else {
                    newSelector.push(selector[j]);
                }
            }
            obj.selector = newSelector.join(',');
        } else {
            obj.subStyles = this.applyNamespacing(obj.subStyles, forcedNamespace); //handle media queries as well
        }
    }

    return cssObjectArray;
};

/*
 given css string or object array, clears possible namespacing from
 all of the selectors inside the css
 */
fi.prototype.clearNamespacing = function (css, returnObj) {
    if (returnObj === undefined) {
        returnObj = false;
    }
    var cssObjectArray = css;
    var namespaceClass = '.' + this.cssPreviewNamespace;
    if (typeof css === 'string') {
        cssObjectArray = this.parseCSS(css);
    }

    for (var i = 0; i < cssObjectArray.length; i++) {
        var obj = cssObjectArray[i];

        if (obj.type !== 'media') {
            var selector = obj.selector.split(',');
            var newSelector = [];
            for (var j = 0; j < selector.length; j++) {
                newSelector.push(selector[j].split(namespaceClass + ' ').join(''));
            }
            obj.selector = newSelector.join(',');
        } else {
            obj.subStyles = this.clearNamespacing(obj.subStyles, true); //handle media queries as well
        }
    }
    if (returnObj === false) {
        return this.getCSSForEditor(cssObjectArray);
    } else {
        return cssObjectArray;
    }

};

/*
 creates a new style tag (also destroys the previous one)
 and injects given css string into that css tag
 */
fi.prototype.createStyleElement = function (id, css, format) {
    if (format === undefined) {
        format = false;
    }

    if (this.testMode === false && format !== 'nonamespace') {
        //apply namespacing classes
        css = this.applyNamespacing(css);
    }

    if (typeof css != 'string') {
        css = this.getCSSForEditor(css);
    }
    //apply formatting for css
    if (format === true) {
        css = this.getCSSForEditor(this.parseCSS(css));
    }

    if (this.testMode !== false) {
        return this.testMode('create style #' + id, css); //if test mode, just pass result to callback
    }

    var __el = document.getElementById(id);
    if (__el) {
        __el.parentNode.removeChild(__el);
    }

    var head = document.head || document.getElementsByTagName('head')[0],
        style = document.createElement('style');

    style.id = id;
    style.type = 'text/css';

    head.appendChild(style);

    if (style.styleSheet && !style.sheet) {
        style.styleSheet.cssText = css;
    } else {
        style.appendChild(document.createTextNode(css));
    }
};

export default fi;

/* eslint-enable */