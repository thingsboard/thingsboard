let expect = window.expect,
  inject = window.inject,
  module = window.module,

  angular = window.angular,
  React = window.React,
  ReactDOM = window.ReactDOM,
  ReactTestUtils = React.addons.TestUtils;

describe('Factory: <WatchList />', function () {
  let WatchList,

    stubWatchListData,
    firstWatchedRepo,
    secondWatchedRepo,

    watchListElement,
    renderedElement,
    listDomEl;

  beforeEach(function () {
    firstWatchedRepo = 'some name';
    secondWatchedRepo = 'another name';

    stubWatchListData = [{name: firstWatchedRepo}, {name: secondWatchedRepo}];

    module('app');

    inject(function (_$timeout_, _WatchListComponent_) {
      WatchList = _WatchListComponent_;
    });

    watchListElement = (
      <WatchList
        watchedRepos={stubWatchListData}
        />
    );
    renderedElement = ReactTestUtils.renderIntoDocument(watchListElement);

    listDomEl = ReactDOM.findDOMNode(renderedElement);
  });

  afterEach(function () {
  });

  it('should be a list', () => {
    expect(listDomEl.tagName).to.equal('UL');
  });

  it('should render a list item for each watched repo', function () {
    var listItems = listDomEl.querySelectorAll('li');

    expect(listItems).to.have.length(stubWatchListData.length);
  });

  it('should render a list item for each watched repo', function () {
    var listItems = listDomEl.querySelectorAll('li');

    angular.forEach(listItems, function (listItemEl, index) {
      expect(listItemEl.textContent).to.equal(stubWatchListData[index].name);
    });
  });

  it('should include a key for each child list item', function () {
    var listElement = ReactTestUtils.findRenderedComponentWithType(renderedElement, WatchList);

    React.Children.forEach(listElement.props.children, function (listItem, index) {
      var expectedKey = index.toString();

      expect(listItem.key).to.equal(expectedKey);
    });
  });
});
