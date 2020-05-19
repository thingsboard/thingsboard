function WatchListDirective(WatchListComponent, reactDirective) {
  return reactDirective(WatchListComponent, [
    'watchedRepos'
  ]);
}

WatchListDirective.$inject = ['WatchListComponent', 'reactDirective'];

export default WatchListDirective;
