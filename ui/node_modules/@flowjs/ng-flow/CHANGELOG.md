# 2.0.0

## Features
 - **flowDragEnter** directive added
 - **flowPreventDrop** directive added
 - event attributes, such as **flowFilesSubmitted**, now can be assigned inside **flowInit**
 directive
 - **flowInit** directive also **$broadcast** all events to child scopes

## Breaking Changes
 - Module **ngFlow** was renamed to **flow**
 - All directives and attributes, starting with **ng-flow**, was renamed to **flow**
 - **flowBtn** directive attributes **ng-directory** and **ng-single-file** renamed to
 **flow-directory** and **flow-single-file**
 - **ngDragOverClass** directive dropped, use **flowDragEnter** directive instead
 - some files in src directory was renamed