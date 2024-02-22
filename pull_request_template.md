## Pull Request description

Improved code maintainability and testability by adopting constructor injection, which promotes clearer dependency management and makes it easier to write unit tests.

## General checklist

- [x] You have reviewed the guidelines [document](https://docs.google.com/document/d/1wqcOafLx5hth8SAg4dqV_LV3un3m5WYR8RdTJ4MbbUM/edit?usp=sharing).
- [x] [Labels](https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/managing-labels#about-labels) that classify your pull request have been added.
- [x] The [milestone](https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/about-milestones) is specified and corresponds to fix version.  
- [x] Description references specific [issue](https://github.com/thingsboard/thingsboard/issues).
- [x] Description contains human-readable scope of changes.
- [x] Description contains brief notes about what needs to be added to the documentation.
- [x] No merge conflicts, commented blocks of code, code formatting issues.
- [x] Changes are backward compatible or upgrade script is provided.
- [x] Similar PR is opened for PE version to simplify merge. Crosslinks between PRs added. Required for internal contributors only.
  
## Front-End feature checklist

- [ ] Screenshots with affected component(s) are added. The best option is to provide 2 screens: before and after changes;
- [ ] If you change the widget or other API, ensure it is backward-compatible or upgrade script is present.
- [ ] Ensure new API is documented [here](https://github.com/thingsboard/thingsboard-ui-help)

## Back-End feature checklist

- [x] Added corresponding unit and/or integration test(s). Provide written explanation in the PR description if you have failed to add tests.
- [x] If new dependency was added: the dependency tree is checked for conflicts.
- [x] If new service was added: the service is marked with corresponding @TbCoreComponent, @TbRuleEngineComponent, @TbTransportComponent, etc.
- [x] If new REST API was added: the RestClient.java was updated, issue for [Python REST client](https://github.com/thingsboard/thingsboard-python-rest-client) is created.
- [x] If new yml property was added: make sure a description is added (above or near the property).



