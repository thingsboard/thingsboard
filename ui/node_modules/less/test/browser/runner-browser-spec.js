describe('less.js browser behaviour', function() {
    testLessEqualsInDocument();

    it('has some log messages', function() {
        expect(logMessages.length).to.be.above(0);
    });

    for (var i = 0; i < testFiles.length; i++) {
        var sheet = testSheets[i];
        testSheet(sheet);
    }
});
