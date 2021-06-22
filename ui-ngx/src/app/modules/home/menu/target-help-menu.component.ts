import {Component, OnInit} from '@angular/core';
import {MenuSection} from '@core/services/menu.models';
import {Router} from '@angular/router';
import {guid} from '@core/utils';

@Component({
  selector: 'tb-target-help-menu',
  templateUrl: './target-help-menu.component.html',
  styleUrls: ['./target-help-menu.component.scss']
})
export class TargetHelpMenuComponent implements OnInit {
  private showTooltip = false;
  private closing = false;


  public section: MenuSection = {
    id: 'target.add',
    name: 'help_menu',
    type: 'toggle',
    isMdiIcon: true,
    path: '/plus',
    icon: 'plus',
    pages: [
      {
        id: guid(),
        name: 'Contact Support',
        type: 'link',
        path: '/support',
        isMdiIcon: true,
        icon: 'contact_support'
      },
      {
        id: guid(),
        name: 'Online Chat',
        type: 'link',
        path: '/chat',
        isMdiIcon: true,
        icon: 'online_chat'
      },
      {
        id: guid(),
        name: 'Documentation',
        type: 'link',
        path: '/docs',
        isMdiIcon: true,
        icon: 'documentation'
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
