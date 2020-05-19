export default class ActivityCtrl {
  constructor(ActivityService, targetUser) {
    this.watchedRepos = [];

    ActivityService.findAllWatchedRepos(targetUser)
      .then(result => {
        this.watchedRepos = result;
      });
  }
}

ActivityCtrl.$inject = ['ActivityService', 'targetUser'];
