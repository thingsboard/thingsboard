// this should preferably only add one of the inline array and $inject array
// low prio to fix for now
var foobar = (function() {
    return function(b) {
        "ngInject";
    };
})();
myMod.service("b", foobar);
