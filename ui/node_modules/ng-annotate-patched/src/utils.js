function stableSort(arr, compareFn) {
    return arr.map((value, index) => [value, index]).sort(([a, ai], [b, bi]) => {
        const cmp = compareFn(a, b);
        return cmp === 0 ? ai - bi : cmp;
    }).map(([value, _]) => value);
}


module.exports = { stableSort };
