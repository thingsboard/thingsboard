import {Component, OnInit} from '@angular/core';
import {MenuSection} from '@core/services/menu.models';
import {Router} from '@angular/router';
import {guid} from '@core/utils';

@Component({
  selector: 'tb-target-add-menu',
  templateUrl: './target-add-menu.component.html',
  styleUrls: ['./target-add-menu.component.scss']
})
export class TargetAddMenuComponent implements OnInit {
  private showTooltip = false;
  private closing = false;
  public section: MenuSection = {
    id: 'target.add',
    name: 'add_menu',
    type: 'toggle',
    isMdiIcon: true,
    path: '/add',
    icon: 'plus',
    pages: [
      {
        id: guid(),
        name: 'New Task',
        type: 'link',
        path: '/tasks',
        isMdiIcon: true,
        icon: 'task'
      },
      {
        id: guid(),
        name: 'Quick Meeting',
        type: 'link',
        path: '/meeting',
        isMdiIcon: true,
        icon: 'task'
      },
      {
        id: guid(),
        name: 'Upload Document',
        type: 'link',
        path: '/docs',
        isMdiIcon: true,
        icon: 'task'
      },
      {
        id: guid(),
        name: 'New Messages',
        type: 'link',
        path: '/messages',
        isMdiIcon: true,
        icon: 'task'
      },
      {
        id: guid(),
        name: 'Create Workspace',
        type: 'link',
        path: '/workspace',
        isMdiIcon: true,
        icon: 'task'
      },
    ]
  };

  constructor(private router: Router) {
  }

  ngOnInit() {
  }

  mouseEnter() {
    this.closing = false;
    this.showTooltip = true;
  }

  mouseLeave() {
    this.closing = true;
    setTimeout(() => {
      if (this.closing) {
        this.showTooltip = false;
      }
    }, 200);
  }

  shouldShow() {
    return this.showTooltip;
  }
}
