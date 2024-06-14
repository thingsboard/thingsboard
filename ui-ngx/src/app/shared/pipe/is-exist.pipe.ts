import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'isExist'
})
export class IsExistPipe implements PipeTransform {
  transform(value: unknown): boolean {
    return value !== null && value !== undefined;
  }
}
