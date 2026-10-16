// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'tb-iot-hub-search-page',
  standalone: false,
  templateUrl: './iot-hub-search-page.component.html',
  styleUrls: ['./iot-hub-search-page.component.scss']
})
export class TbIotHubSearchPageComponent implements OnInit {

  searchText = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const search = this.route.snapshot.queryParamMap.get('search');
    if (search) {
      this.searchText = search;
    }
  }

  navigateBack(): void {
    void this.router.navigate(['/iot-hub']);
  }
}
