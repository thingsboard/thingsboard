import { EventEmitter, Injectable } from "@angular/core";

@Injectable({
    providedIn: 'root'
})
export class EmitService {
    dataKeysEmitter = new EventEmitter<any>();

    constructor() {}
}
