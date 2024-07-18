import { DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export abstract class TbDatasource<DataType> implements DataSource<DataType> {

  protected dataSubject = new BehaviorSubject<Array<DataType>>([]);

  connect(): Observable<Array<DataType>> {
    return this.dataSubject.asObservable();
  }

  disconnect(): void {
    this.dataSubject.complete();
  }

  loadData(data: Array<DataType>): void {
    this.dataSubject.next(data);
  }

  isEmpty(): Observable<boolean> {
    return this.dataSubject.pipe(
      map((data: DataType[]) => !data.length)
    );
  }

  total(): Observable<number> {
    return this.dataSubject.pipe(
      map((data: DataType[]) => data.length)
    );
  }
}
