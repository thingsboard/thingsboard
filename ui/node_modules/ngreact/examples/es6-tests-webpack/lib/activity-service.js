export default class ActivityService {
  constructor($http) {
    this.$http = $http;
  }

  findAllWatchedRepos(user) {
    return this.$http.get(`https://api.github.com/users/${user}/subscriptions`)
      .then(function (response) {
        return response.data;
      });
  }
}

ActivityService.$inject = ['$http'];
