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
        this.login();
    }



    public login(): void {
        this.http.post('http://127.0.0.1:3000/login', {
            "username": "admin",
            "password": "admin"
        }, { withCredentials: true }).subscribe((tokenObj: string) => {
            console.log('Token:', tokenObj);
            this.httpOptions.headers = this.httpOptions.headers.set('Cookie', 'genieacs-ui-jwt=' + tokenObj);

        })
    }

    public change(id, parameterName, newValue): void {
        this.http.post('http://localhost:3000/api/devices/' + id + '/tasks',
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
            this.httpOptions).subscribe((dta) => {
                console.log("obada hereee", dta)

            })
    }

    public refresh(): void {
        this.http.post('http://localhost:3000/api/devices/202BC1-BM632w-000000/tasks',
            [
                {
                    "name": "getParameterValues",
                    "device": "202BC1-BM632w-000000",
                    "parameterNames": [
                        "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID" // Parameters to refresh .
                    ],
                    "status": "pending"
                }
            ],
            this.httpOptions).subscribe((dta) => {
                console.log("obada hereee", dta)

            })
    }

}