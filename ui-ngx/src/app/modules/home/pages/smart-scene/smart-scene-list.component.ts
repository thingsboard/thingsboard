///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { SelectionModel } from '@angular/cdk/collections';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatSnackBar } from '@angular/material/snack-bar';

interface Customer {
  id: number;
  name: string;
  email: string;
}

interface SmartScene {
  id: number;
  title: string;
  description: string;
  createdTime: Date;
  lastRun: Date | null;
  status: 'active' | 'inactive';
  category: string;
  customersList: Customer[];
  public: boolean;
  tags: string[];
}

@Component({
  selector: 'tb-smart-scene-list',
  templateUrl: './smart-scene-list.component.html',
  styleUrls: ['./smart-scene-list.component.scss']
})
export class SmartSceneListComponent implements OnInit, AfterViewInit {
  displayedColumns: string[] = [
    'select', 
    'status', 
    'createdTime', 
    'title', 
    'category',
    'customers', 
    'lastRun',
    'public', 
    'actions'
  ];
  
  dataSource: MatTableDataSource<SmartScene>;
  selection = new SelectionModel<SmartScene>(true, []);
  
  searchText = '';
  statusFilter = 'all';
  categoryFilter = 'all';
  
  categories = ['Home', 'Office', 'Security', 'Lighting', 'Energy', 'Entertainment'];
  
  smartScenes: SmartScene[] = [];
  
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  
  constructor(private snackBar: MatSnackBar) {
    // Create sample data
    const sampleData = this.generateSampleData();
    this.smartScenes = sampleData;
  }
  
  ngOnInit(): void {
    this.dataSource = new MatTableDataSource(this.smartScenes);
  }
  
  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }
  
  // Generate sample data for demonstration
  generateSampleData(): SmartScene[] {
    const customers: Customer[] = [
      { id: 1, name: 'Nguyễn Văn A', email: 'nguyenvana@example.com' },
      { id: 2, name: 'Trần Thị B', email: 'tranthib@example.com' },
      { id: 3, name: 'Lê Văn C', email: 'levanc@example.com' },
      { id: 4, name: 'Phạm Thị D', email: 'phamthid@example.com' },
      { id: 5, name: 'Hoàng Văn E', email: 'hoangvane@example.com' },
      { id: 6, name: 'Đỗ Thị F', email: 'dothif@example.com' }
    ];
    
    const scenes: SmartScene[] = [];
    
    for (let i = 1; i <= 20; i++) {
      // Randomly assign 1-5 customers to each scene
      const sceneCustomers: Customer[] = [];
      const numCustomers = Math.floor(Math.random() * 5) + 1;
      const shuffled = [...customers].sort(() => 0.5 - Math.random());
      
      for (let j = 0; j < numCustomers; j++) {
        if (shuffled[j]) {
          sceneCustomers.push(shuffled[j]);
        }
      }
      
      // Create a scene with random data
      const scene: SmartScene = {
        id: i,
        title: `Kịch bản thông minh ${i}`,
        description: `Mô tả chi tiết cho kịch bản thông minh số ${i}`,
        createdTime: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000),
        lastRun: Math.random() > 0.3 ? new Date(Date.now() - Math.floor(Math.random() * 7) * 24 * 60 * 60 * 1000) : null,
        status: Math.random() > 0.3 ? 'active' : 'inactive',
        category: this.categories[Math.floor(Math.random() * this.categories.length)],
        customersList: sceneCustomers,
        public: Math.random() > 0.5,
        tags: ['automation', 'smart', Math.random() > 0.5 ? 'featured' : 'standard']
      };
      
      scenes.push(scene);
    }
    
    return scenes;
  }
  
  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  masterToggle() {
    this.isAllSelected()
      ? this.selection.clear()
      : this.dataSource.data.forEach(row => this.selection.select(row));
  }

  getMoreCustomersTooltip(scene: SmartScene): string {
    return scene.customersList
      .slice(3)
      .map(c => c.name)
      .join(', ');
  }

  resetFilters() {
    this.searchText = '';
    this.statusFilter = 'all';
    this.categoryFilter = 'all';
    this.applyFilter();
  }

  applyFilter() {
    this.dataSource.filter = this.searchText.trim().toLowerCase();
  }

  togglePublic(scene: SmartScene) {
    scene.public = !scene.public;
    this.showNotification(`${scene.title} is now ${scene.public ? 'public' : 'private'}`);
  }

  // Dummy handlers for actions
  runScene(scene: SmartScene) {
    scene.lastRun = new Date();
    this.showNotification(`Running scene: ${scene.title}`);
  }

  editScene(scene: SmartScene) {
    this.showNotification(`Editing scene: ${scene.title}`);
  }

  duplicateScene(scene: SmartScene) {
    const newScene: SmartScene = {
      ...scene,
      id: this.dataSource.data.length + 1,
      title: `${scene.title} (Copy)`,
      createdTime: new Date(),
      lastRun: null
    };
    
    this.dataSource.data = [...this.dataSource.data, newScene];
    this.showNotification(`Duplicated: ${scene.title}`);
  }

  shareScene(scene: SmartScene) {
    this.showNotification(`Sharing scene: ${scene.title}`);
  }

  downloadScene(scene: SmartScene) {
    this.showNotification(`Downloading scene: ${scene.title}`);
  }

  deleteScene(scene: SmartScene) {
    const index = this.dataSource.data.findIndex(s => s.id === scene.id);
    if (index > -1) {
      const data = [...this.dataSource.data];
      data.splice(index, 1);
      this.dataSource.data = data;
      this.showNotification(`Deleted scene: ${scene.title}`);
    }
  }

  bulkDelete() {
    const selectedIds = this.selection.selected.map(s => s.id);
    this.dataSource.data = this.dataSource.data.filter(s => !selectedIds.includes(s.id));
    this.showNotification(`Deleted ${selectedIds.length} scenes`);
    this.selection.clear();
  }

  bulkExport() {
    this.showNotification(`Exporting ${this.selection.selected.length} scenes`);
  }

  bulkShare() {
    this.showNotification(`Sharing ${this.selection.selected.length} scenes`);
  }

  viewScene(scene: SmartScene) {
    alert('Chi tiết kịch bản:\n' +
      'Tên: ' + scene.title + '\n' +
      'Trạng thái: ' + scene.status + '\n' +
      'Ngày tạo: ' + scene.createdTime.toLocaleString() + '\n' +
      'Khách hàng: ' + scene.customersList.map(c => c.name).join(', '));
  }

  createScene() {
    this.showNotification('Creating new scene');
  }

  

  // Show notification
  showNotification(message: string) {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'end',
      verticalPosition: 'bottom'
    });
  }
}
