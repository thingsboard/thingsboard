import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable({
    providedIn: 'root'
})

export class AcsService {

    private httpOptions = {
        headers: new HttpHeaders({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }),
        withCredentials: true
    };



    //   return this.http.get(this.heroesUrl, httpOptions)

    constructor(private http: HttpClient) {
        // this.login();
    }

    public removeItem(array, item) {
        for (var i in array) {
            if (array[i] == item) {
                array.splice(i, 1);
                break;
            }
        }
    }



    // public login(): void {
    //     this.http.post('http://127.0.0.1:3000/login', {
    //         "username": "admin",
    //         "password": "admin"
    //     }, { withCredentials: true }).subscribe((tokenObj: string) => {
    //         console.log('Token:', tokenObj);
    //         this.httpOptions.headers = this.httpOptions.headers.set('Cookie', 'genieacs-ui-jwt=' + tokenObj);

    //     })
    // }

    public change(id, parameterName, newValue): void {
        this.http.post('http://localhost:8080/api/v1/tr69/tasks/?deviceID=' + id  ,
            [
                {
                    "device": id,
                    "name": "setParameterValues",
                    "parameterValues": [
                        [
                            parameterName,
                            newValue,
                            "xsd:string"
                        ]
                    ],
                    "status": "pending"
                }
            ],
            ).subscribe((dta) => {
                console.log("obada hereee", dta)

            })
    }

    public refresh(id, parameterName): void {
        this.http.post('http://localhost:8080/api/v1/tr69/tasks/?deviceID=' + id  ,
            [
                {
                    "name": "getParameterValues",
                    "device": id,
                    "parameterNames": [
                        parameterName
                    ],
                    "status": "pending"
                }
            ],
            ).subscribe((dta) => {
                console.log("obada hereee", dta)

            })
    }


    public deleteDevice(id): void {
        this.http.delete('http://localhost:8080/api/v1/tr69/devices/?deviceID='+id).subscribe((dta) => {
            console.log("deleted!!!")

        })
    }



    public rebootDevice(id): void {
        this.http.post('http://localhost:8080/api/v1/tr69/actions/?deviceID='+id,
            [
                {
                    "name": "reboot",
                    "device": id,
                    "status": "pending"
                }
            ],
            this.httpOptions).subscribe((dta) => {
                console.log("reboot");

            })
    }

    public resetDevice(id): void {
        this.http.post('http://localhost:8080/api/v1/tr69/actions/?deviceID='+id,
            [
                {
                    "name": "factoryReset",
                    "device": id,
                    "status": "pending"
                }
            ],
            this.httpOptions).subscribe((dta) => {
                console.log("reset");

            })
    }

    public tagDevice(id, tagValue: Record<string, boolean>): void {

        this.http.post('http://localhost:8080/api/v1/tr69/tag/?deviceID='+id,
            tagValue,
            this.httpOptions).subscribe((dta) => {
                console.log("tag");

            })
    }

    public untagDevice(id, untagValue: Record<string, boolean>): void {
        this.http.post('http://localhost:8080/api/v1/tr69/tag/?deviceID='+id,
            untagValue,
            this.httpOptions).subscribe((dta) => {
                console.log("untag");

            })
    }



}