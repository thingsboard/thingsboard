const tree = Object.create(null);

import Node from './node';
import Color from './color';
import AtRule from './atrule';
import DetachedRuleset from './detached-ruleset';
import Operation from './operation';
import Dimension from './dimension';
import Unit from './unit';
import Keyword from './keyword';
import Variable from './variable';
import Property from './property';
import Ruleset from './ruleset';
import Element from './element';
import Attribute from './attribute';
import Combinator from './combinator';
import Selector from './selector';
import Quoted from './quoted';
import Expression from './expression';
import Declaration from './declaration';
import Call from './call';
import URL from './url';
import Import from './import';
import Comment from './comment';
import Anonymous from './anonymous';
import Value from './value';
import JavaScript from './javascript';
import Assignment from './assignment';
import Condition from './condition';
import Paren from './paren';
import Media from './media';
import UnicodeDescriptor from './unicode-descriptor';
import Negative from './negative';
import Extend from './extend';
import VariableCall from './variable-call';
import NamespaceValue from './namespace-value';

// mixins
import MixinCall from './mixin-call';
import MixinDefinition from './mixin-definition';

export default {
    Node, Color, AtRule, DetachedRuleset, Operation,
    Dimension, Unit, Keyword, Variable, Property,
    Ruleset, Element, Attribute, Combinator, Selector,
    Quoted, Expression, Declaration, Call, URL, Import,
    Comment, Anonymous, Value, JavaScript, Assignment,
    Condition, Paren, Media, UnicodeDescriptor, Negative,
    Extend, VariableCall, NamespaceValue,
    mixin: {
        Call: MixinCall,
        Definition: MixinDefinition
    }
};