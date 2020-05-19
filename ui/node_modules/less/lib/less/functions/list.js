import Comment from '../tree/comment';
import Dimension from '../tree/dimension';
import Declaration from '../tree/declaration';
import Expression from '../tree/expression';
import Ruleset from '../tree/ruleset';
import Selector from '../tree/selector';
import Element from '../tree/element';
import Quote from '../tree/quoted';

const getItemsFromNode = node => {
    // handle non-array values as an array of length 1
    // return 'undefined' if index is invalid
    const items = Array.isArray(node.value) ?
        node.value : Array(node);

    return items;
};

export default {
    _SELF: function(n) {
        return n;
    },
    extract: function(values, index) {
        // (1-based index)
        index = index.value - 1;

        return getItemsFromNode(values)[index];
    },
    length: function(values) {
        return new Dimension(getItemsFromNode(values).length);
    },
    /**
     * Creates a Less list of incremental values.
     * Modeled after Lodash's range function, also exists natively in PHP
     * 
     * @param {Dimension} [start=1]
     * @param {Dimension} end  - e.g. 10 or 10px - unit is added to output
     * @param {Dimension} [step=1] 
     */
    range: function(start, end, step) {
        let from;
        let to;
        let stepValue = 1;
        const list = [];
        if (end) {
            to = end;
            from = start.value;
            if (step) {
                stepValue = step.value;
            }
        }
        else {
            from = 1;
            to = start;
        }

        for (let i = from; i <= to.value; i += stepValue) {
            list.push(new Dimension(i, to.unit));
        }

        return new Expression(list);
    },
    each: function(list, rs) {
        const rules = [];
        let newRules;
        let iterator;

        if (list.value && !(list instanceof Quote)) {
            if (Array.isArray(list.value)) {
                iterator = list.value;
            } else {
                iterator = [list.value];
            }
        } else if (list.ruleset) {
            iterator = list.ruleset.rules;
        } else if (list.rules) {
            iterator = list.rules;
        } else if (Array.isArray(list)) {
            iterator = list;
        } else {
            iterator = [list];
        }

        let valueName = '@value';
        let keyName = '@key';
        let indexName = '@index';

        if (rs.params) {
            valueName = rs.params[0] && rs.params[0].name;
            keyName = rs.params[1] && rs.params[1].name;
            indexName = rs.params[2] && rs.params[2].name;
            rs = rs.rules;
        } else {
            rs = rs.ruleset;
        }

        for (let i = 0; i < iterator.length; i++) {
            let key;
            let value;
            const item = iterator[i];
            if (item instanceof Declaration) {
                key = typeof item.name === 'string' ? item.name : item.name[0].value;
                value = item.value;
            } else {
                key = new Dimension(i + 1);
                value = item;
            }

            if (item instanceof Comment) {
                continue;
            }

            newRules = rs.rules.slice(0);
            if (valueName) {
                newRules.push(new Declaration(valueName,
                    value,
                    false, false, this.index, this.currentFileInfo));
            }
            if (indexName) {
                newRules.push(new Declaration(indexName,
                    new Dimension(i + 1),
                    false, false, this.index, this.currentFileInfo));
            }
            if (keyName) {
                newRules.push(new Declaration(keyName,
                    key,
                    false, false, this.index, this.currentFileInfo));
            }

            rules.push(new Ruleset([ new(Selector)([ new Element("", '&') ]) ],
                newRules,
                rs.strictImports,
                rs.visibilityInfo()
            ));
        }

        return new Ruleset([ new(Selector)([ new Element("", '&') ]) ],
            rules,
            rs.strictImports,
            rs.visibilityInfo()
        ).eval(this.context);
    }
};
