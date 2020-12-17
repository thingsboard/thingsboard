import { Component, OnInit, Inject, ViewChild, AfterViewInit, EventEmitter } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { AcsService } from './acs-service';
import { AcsComponent } from './acs.component';

@Component({
    selector: 'dialog-data-example-dialog',
    templateUrl: 'dialog-data.html',
})
export class DialogDataDialog implements OnInit, AfterViewInit {

    @ViewChild(MatPaginator) paginator: MatPaginator;

    myDataSouce: MatTableDataSource<any>

    constructor(@Inject(MAT_DIALOG_DATA) public data: any[], private acsService: AcsService) { }
    displayedColumns: string[] = ['Parameter', 'Value'];
    ngOnInit(): void {
        this.myDataSouce = new MatTableDataSource(this.data);
        setTimeout(() => this.myDataSouce.paginator = this.paginator);

    }
    ngAfterViewInit(): void {
        setTimeout(() => this.myDataSouce.paginator = this.paginator);

    }

    updateValue(parameterName, value) {
        let newValue = prompt(parameterName, value);
        let deviceID = this.data[0].deviceData['value'][0];
        this.acsService.change(deviceID, parameterName, newValue);
    };

}