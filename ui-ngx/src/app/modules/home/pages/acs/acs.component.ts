import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';


@Component({
  selector: 'tb-acs',
  templateUrl: './acs.component.html',
  styleUrls: ['./acs.component.scss']
})
export class AcsComponent implements OnInit {

  constructor(private http: HttpClient) { }

  displayedColumns: string[] = ['Device Name', 'SSID', 'Last Inform', 'symbol'];
  dataSource : any[] = ELEMENT_DATA
  ngOnInit(): void {
    this.http.post( 'http://127.0.0.1:3000/login',{
      "username": "admin",
      "password": "admin"
  },{withCredentials:true}).subscribe(data => {
      this.http.get<any[]>('http://localhost:3000/api/devices',{withCredentials:true}).subscribe((deviceData)=>{
        this.dataSource = deviceData
      })
    })
        
  }


}


const ELEMENT_DATA: any[] = [
  {
      "DeviceID.ID": {
          "value": [
              "202BC1-BM632w-000000",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830,
          "object": false,
          "objectTimestamp": 1607846439830
      },
      "InternetGatewayDevice": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.DeviceInfo": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.DeviceInfo.HardwareVersion": {
          "value": [
              "00040501",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439831,
          "object": false,
          "objectTimestamp": 1607844892668
      },
      "InternetGatewayDevice.DeviceInfo.ProvisioningCode": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439831,
          "object": false,
          "objectTimestamp": 1607844892668
      },
      "InternetGatewayDevice.DeviceInfo.SoftwareVersion": {
          "value": [
              "V100R001IRQC56B017",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439831,
          "object": false,
          "objectTimestamp": 1607844892668
      },
      "InternetGatewayDevice.DeviceInfo.SpecVersion": {
          "value": [
              "1.0",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439831,
          "object": false,
          "objectTimestamp": 1607844892668
      },
      "InternetGatewayDevice.DeviceSummary": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.IDLE": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.IPPingDiagnostics": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.LANDevice": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.LANDevice.1": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Active": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.HostName": {
          "value": [
              "android-d87bf88d22e66acf",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.IPAddress": {
          "value": [
              "192.168.1.2",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.MACAddress": {
          "value": [
              "40:B0:FA:9C:4A:50",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Active": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.HostName": {
          "value": [
              "android-d91540e8540e9c7a",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.IPAddress": {
          "value": [
              "192.168.1.4",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.MACAddress": {
          "value": [
              "10:68:3F:77:88:20",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Active": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.HostName": {
          "value": [
              "Lena-PC",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.IPAddress": {
          "value": [
              "192.168.1.5",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.MACAddress": {
          "value": [
              "C0:14:3D:C0:CF:93",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Active": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.HostName": {
          "value": [
              "localhost",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.IPAddress": {
          "value": [
              "192.168.1.6",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.MACAddress": {
          "value": [
              "1C:3E:84:AC:BB:76",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Active": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.HostName": {
          "value": [
              "Munas-iphone",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.IPAddress": {
          "value": [
              "192.168.1.7",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.MACAddress": {
          "value": [
              "C0:9F:42:56:33:DF",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Active": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.HostName": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.IPAddress": {
          "value": [
              "192.168.1.3",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.MACAddress": {
          "value": [
              "20:10:7a:08:4d:43",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892682,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.HostNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.X_HUAWEI_Host": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceConfig": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.LANHostConfigManagement": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.LANWLANConfigurationNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AssociatedDevice": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AutoChannelEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607844892680,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BSSID": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BeaconType": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Channel": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Enable": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MACAddressControlEnabled": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MaxBitRate": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.PreSharedKey": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.RegulatoryDomain": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID": {
          "value": [
              "Sanad'Ibras 1Amin WiMax",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439832,
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSIDAdvertisementEnabled": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Standard": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Status": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalAssociations": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesReceived": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesSent": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsReceived": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsSent": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPEncryptionLevel": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKey": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKeyIndex": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WMMEnable": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPS": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_AssociateDeviceNum": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_ChannelUsed": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_PowerValue": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesReceivedError": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesSentError": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsReceivedError": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsSentError": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WLANVersion": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NBWControl": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NGIControl": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NHtMcs": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NTxRxStream": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanIsolateControl": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilter": {
          "object": true,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilternum": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilterpolicy": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanStaWakeEnable": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDevice.1.X_HUAWEI_WLANEnable": {
          "object": false,
          "objectTimestamp": 1607846439830,
          "writable": true,
          "writableTimestamp": 1607846439830
      },
      "InternetGatewayDevice.LANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.Layer2Bridging": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.Layer3Forwarding": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.Layer3QoS": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestPassword": {
          "value": [
              "69t0mkjya1",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892687,
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestURL": {
          "value": [
              "http://127.0.0.1:59253/",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439831,
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestUsername": {
          "value": [
              "202BC1-BM632w-000000",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892687,
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.ParameterKey": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439831,
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.Password": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607844892686,
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformInterval": {
          "value": [
              300,
              "xsd:unsignedInt"
          ],
          "valueTimestamp": 1607844892687,
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.URL": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.Username": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServer.X_HUAWEI_SSLCertEnable": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.ManagementServerConfig": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.OperatorProfile": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.Services": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.Time": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.UserInterface": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice": {
          "object": true,
          "objectTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection": {
          "object": true,
          "objectTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AddressingType": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AutoDisconnectTime": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionStatus": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionTrigger": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionType": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSEnabled": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSOverrideAllowed": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSServers": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DefaultGateway": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Enable": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ExternalIPAddress": {
          "value": [
              "172.3.89.139",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439831,
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.LastConnectionError": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddress": {
          "value": [
              "20:2B:C1:E0:06:65",
              "xsd:string"
          ],
          "valueTimestamp": 1607844892672,
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddressOverride": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MaxMTUSize": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.NATEnabled": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Name": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMapping": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMappingNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PossibleConnectionTypes": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Reset": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.RouteProtocolRx": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Stats": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.SubnetMask": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Uptime": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DHCPRelay": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DMZ": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTrigger": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTriggerNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_ServiceList": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.WiMAX": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.X_HUAWEI_FireWall": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": true,
          "writableTimestamp": 1607844892668
      },
      "InternetGatewayDevice.X_HUAWEI_SyslogConfig": {
          "object": true,
          "objectTimestamp": 1607844892668,
          "writable": false,
          "writableTimestamp": 1607844892668
      },
      "DeviceID.Manufacturer": {
          "value": [
              "Huawei Technologies Co., Ltd.",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830,
          "object": false,
          "objectTimestamp": 1607846439830
      },
      "DeviceID.OUI": {
          "value": [
              "202BC1",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830,
          "object": false,
          "objectTimestamp": 1607846439830
      },
      "DeviceID.ProductClass": {
          "value": [
              "BM632w",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830,
          "object": false,
          "objectTimestamp": 1607846439830
      },
      "DeviceID.SerialNumber": {
          "value": [
              "000000",
              "xsd:string"
          ],
          "valueTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830,
          "object": false,
          "objectTimestamp": 1607846439830
      },
      "Events.Inform": {
          "value": [
              1607846439830,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830,
          "object": false,
          "objectTimestamp": 1607846439830
      },
      "Events.Registered": {
          "value": [
              1607844892668,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846439830,
          "writable": false,
          "writableTimestamp": 1607846439830,
          "object": false,
          "objectTimestamp": 1607846439830
      }
  },
  {
      "DeviceID.ID": {
          "value": [
              "202BC1-BM632w-000002",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359,
          "object": false,
          "objectTimestamp": 1607846440359
      },
      "InternetGatewayDevice": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.DeviceInfo": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.DeviceInfo.HardwareVersion": {
          "value": [
              "00040501",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440360,
          "object": false,
          "objectTimestamp": 1607844894764
      },
      "InternetGatewayDevice.DeviceInfo.ProvisioningCode": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440360,
          "object": false,
          "objectTimestamp": 1607844894764
      },
      "InternetGatewayDevice.DeviceInfo.SoftwareVersion": {
          "value": [
              "V100R001IRQC56B017",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440360,
          "object": false,
          "objectTimestamp": 1607844894764
      },
      "InternetGatewayDevice.DeviceInfo.SpecVersion": {
          "value": [
              "1.0",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440360,
          "object": false,
          "objectTimestamp": 1607844894764
      },
      "InternetGatewayDevice.DeviceSummary": {
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.IDLE": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.IPPingDiagnostics": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.LANDevice": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.LANDevice.1": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Active": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.HostName": {
          "value": [
              "android-d87bf88d22e66acf",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.IPAddress": {
          "value": [
              "192.168.1.2",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.MACAddress": {
          "value": [
              "40:B0:FA:9C:4A:50",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Active": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.HostName": {
          "value": [
              "android-d91540e8540e9c7a",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.IPAddress": {
          "value": [
              "192.168.1.4",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.MACAddress": {
          "value": [
              "10:68:3F:77:88:20",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Active": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.HostName": {
          "value": [
              "Lena-PC",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.IPAddress": {
          "value": [
              "192.168.1.5",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.MACAddress": {
          "value": [
              "C0:14:3D:C0:CF:93",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Active": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.HostName": {
          "value": [
              "localhost",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.IPAddress": {
          "value": [
              "192.168.1.6",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.MACAddress": {
          "value": [
              "1C:3E:84:AC:BB:76",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Active": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.HostName": {
          "value": [
              "Munas-iphone",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.IPAddress": {
          "value": [
              "192.168.1.7",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.MACAddress": {
          "value": [
              "C0:9F:42:56:33:DF",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Active": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.HostName": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.IPAddress": {
          "value": [
              "192.168.1.3",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.MACAddress": {
          "value": [
              "20:10:7a:08:4d:43",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.HostNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.X_HUAWEI_Host": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceConfig": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.LANHostConfigManagement": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.LANWLANConfigurationNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AssociatedDevice": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AutoChannelEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BSSID": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BeaconType": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Channel": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Enable": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MACAddressControlEnabled": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MaxBitRate": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.PreSharedKey": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.RegulatoryDomain": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID": {
          "value": [
              "Sanad'Ibras 1Amin WiMax",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440361,
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSIDAdvertisementEnabled": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Standard": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Status": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalAssociations": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesReceived": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesSent": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsReceived": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsSent": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPEncryptionLevel": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKey": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKeyIndex": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WMMEnable": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPS": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_AssociateDeviceNum": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_ChannelUsed": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_PowerValue": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesReceivedError": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesSentError": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsReceivedError": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsSentError": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WLANVersion": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NBWControl": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NGIControl": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NHtMcs": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NTxRxStream": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanIsolateControl": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilter": {
          "object": true,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilternum": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilterpolicy": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanStaWakeEnable": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDevice.1.X_HUAWEI_WLANEnable": {
          "object": false,
          "objectTimestamp": 1607846440359,
          "writable": true,
          "writableTimestamp": 1607846440359
      },
      "InternetGatewayDevice.LANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.Layer2Bridging": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.Layer3Forwarding": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.Layer3QoS": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestPassword": {
          "value": [
              "26rrlwrjnlt",
              "xsd:string"
          ],
          "valueTimestamp": 1607844894783,
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestURL": {
          "value": [
              "http://127.0.0.1:59283/",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440360,
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestUsername": {
          "value": [
              "202BC1-BM632w-000002",
              "xsd:string"
          ],
          "valueTimestamp": 1607844894783,
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.ParameterKey": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440360,
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.Password": {
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607844894782,
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformInterval": {
          "value": [
              300,
              "xsd:unsignedInt"
          ],
          "valueTimestamp": 1607844894783,
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.URL": {
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.Username": {
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServer.X_HUAWEI_SSLCertEnable": {
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.ManagementServerConfig": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.OperatorProfile": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.Services": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.Time": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.UserInterface": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.WANDevice": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.WANDevice.1": {
          "object": true,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice": {
          "object": true,
          "objectTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1": {
          "object": true,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection": {
          "object": true,
          "objectTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1": {
          "object": true,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AddressingType": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AutoDisconnectTime": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionStatus": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionTrigger": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionType": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSEnabled": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSOverrideAllowed": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSServers": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DefaultGateway": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Enable": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ExternalIPAddress": {
          "value": [
              "172.3.89.139",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440360,
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.LastConnectionError": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddress": {
          "value": [
              "20:2B:C1:E0:06:65",
              "xsd:string"
          ],
          "valueTimestamp": 1607845238335,
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddressOverride": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MaxMTUSize": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.NATEnabled": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Name": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMapping": {
          "object": true,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMappingNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PossibleConnectionTypes": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Reset": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.RouteProtocolRx": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Stats": {
          "object": true,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.SubnetMask": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Uptime": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DHCPRelay": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DMZ": {
          "object": true,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTrigger": {
          "object": true,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTriggerNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": false,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_ServiceList": {
          "object": false,
          "objectTimestamp": 1607845238333,
          "writable": true,
          "writableTimestamp": 1607845238333
      },
      "InternetGatewayDevice.WANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.WiMAX": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.X_HUAWEI_FireWall": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": true,
          "writableTimestamp": 1607844894764
      },
      "InternetGatewayDevice.X_HUAWEI_SyslogConfig": {
          "object": true,
          "objectTimestamp": 1607844894764,
          "writable": false,
          "writableTimestamp": 1607844894764
      },
      "DeviceID.Manufacturer": {
          "value": [
              "Huawei Technologies Co., Ltd.",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359,
          "object": false,
          "objectTimestamp": 1607846440359
      },
      "DeviceID.OUI": {
          "value": [
              "202BC1",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359,
          "object": false,
          "objectTimestamp": 1607846440359
      },
      "DeviceID.ProductClass": {
          "value": [
              "BM632w",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359,
          "object": false,
          "objectTimestamp": 1607846440359
      },
      "DeviceID.SerialNumber": {
          "value": [
              "000002",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359,
          "object": false,
          "objectTimestamp": 1607846440359
      },
      "Events.Inform": {
          "value": [
              1607846440359,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359,
          "object": false,
          "objectTimestamp": 1607846440359
      },
      "Events.Registered": {
          "value": [
              1607844894764,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846440359,
          "writable": false,
          "writableTimestamp": 1607846440359,
          "object": false,
          "objectTimestamp": 1607846440359
      }
  },
  {
      "DeviceID.ID": {
          "value": [
              "202BC1-BM632w-000003",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983,
          "object": false,
          "objectTimestamp": 1607846440983
      },
      "InternetGatewayDevice": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.DeviceInfo": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.DeviceInfo.HardwareVersion": {
          "value": [
              "00040501",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440984,
          "object": false,
          "objectTimestamp": 1607844895520
      },
      "InternetGatewayDevice.DeviceInfo.ProvisioningCode": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440984,
          "object": false,
          "objectTimestamp": 1607844895520
      },
      "InternetGatewayDevice.DeviceInfo.SoftwareVersion": {
          "value": [
              "V100R001IRQC56B017",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440984,
          "object": false,
          "objectTimestamp": 1607844895520
      },
      "InternetGatewayDevice.DeviceInfo.SpecVersion": {
          "value": [
              "1.0",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440984,
          "object": false,
          "objectTimestamp": 1607844895520
      },
      "InternetGatewayDevice.DeviceSummary": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.IDLE": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.IPPingDiagnostics": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.LANDevice": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.LANDevice.1": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Active": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.HostName": {
          "value": [
              "android-d87bf88d22e66acf",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.IPAddress": {
          "value": [
              "192.168.1.2",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.MACAddress": {
          "value": [
              "40:B0:FA:9C:4A:50",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Active": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.HostName": {
          "value": [
              "android-d91540e8540e9c7a",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.IPAddress": {
          "value": [
              "192.168.1.4",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.MACAddress": {
          "value": [
              "10:68:3F:77:88:20",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Active": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.HostName": {
          "value": [
              "Lena-PC",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.IPAddress": {
          "value": [
              "192.168.1.5",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.MACAddress": {
          "value": [
              "C0:14:3D:C0:CF:93",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Active": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.HostName": {
          "value": [
              "localhost",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.IPAddress": {
          "value": [
              "192.168.1.6",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.MACAddress": {
          "value": [
              "1C:3E:84:AC:BB:76",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Active": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.HostName": {
          "value": [
              "Munas-iphone",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.IPAddress": {
          "value": [
              "192.168.1.7",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.MACAddress": {
          "value": [
              "C0:9F:42:56:33:DF",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Active": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.HostName": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.IPAddress": {
          "value": [
              "192.168.1.3",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.MACAddress": {
          "value": [
              "20:10:7a:08:4d:43",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895534,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.HostNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.X_HUAWEI_Host": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceConfig": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.LANHostConfigManagement": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.LANWLANConfigurationNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AssociatedDevice": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AutoChannelEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607844895532,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BSSID": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BeaconType": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Channel": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Enable": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MACAddressControlEnabled": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MaxBitRate": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.PreSharedKey": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.RegulatoryDomain": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID": {
          "value": [
              "Sanad'Ibras 1Amin WiMax",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440985,
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSIDAdvertisementEnabled": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Standard": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Status": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalAssociations": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesReceived": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesSent": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsReceived": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsSent": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPEncryptionLevel": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKey": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKeyIndex": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WMMEnable": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPS": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_AssociateDeviceNum": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_ChannelUsed": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_PowerValue": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesReceivedError": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesSentError": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsReceivedError": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsSentError": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WLANVersion": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NBWControl": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NGIControl": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NHtMcs": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NTxRxStream": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanIsolateControl": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilter": {
          "object": true,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilternum": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilterpolicy": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanStaWakeEnable": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDevice.1.X_HUAWEI_WLANEnable": {
          "object": false,
          "objectTimestamp": 1607846440983,
          "writable": true,
          "writableTimestamp": 1607846440983
      },
      "InternetGatewayDevice.LANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.Layer2Bridging": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.Layer3Forwarding": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.Layer3QoS": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestPassword": {
          "value": [
              "2f4wd692sdg",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895539,
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestURL": {
          "value": [
              "http://127.0.0.1:59299/",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440984,
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestUsername": {
          "value": [
              "202BC1-BM632w-000003",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895539,
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.ParameterKey": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440984,
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.Password": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607844895538,
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformInterval": {
          "value": [
              300,
              "xsd:unsignedInt"
          ],
          "valueTimestamp": 1607844895539,
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.URL": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.Username": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServer.X_HUAWEI_SSLCertEnable": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.ManagementServerConfig": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.OperatorProfile": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.Services": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.Time": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.UserInterface": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice": {
          "object": true,
          "objectTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection": {
          "object": true,
          "objectTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AddressingType": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AutoDisconnectTime": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionStatus": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionTrigger": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionType": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSEnabled": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSOverrideAllowed": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSServers": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DefaultGateway": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Enable": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ExternalIPAddress": {
          "value": [
              "172.3.89.139",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440984,
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.LastConnectionError": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddress": {
          "value": [
              "20:2B:C1:E0:06:65",
              "xsd:string"
          ],
          "valueTimestamp": 1607844895524,
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddressOverride": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MaxMTUSize": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.NATEnabled": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Name": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMapping": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMappingNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PossibleConnectionTypes": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Reset": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.RouteProtocolRx": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Stats": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.SubnetMask": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Uptime": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DHCPRelay": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DMZ": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTrigger": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTriggerNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_ServiceList": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.WiMAX": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.X_HUAWEI_FireWall": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": true,
          "writableTimestamp": 1607844895520
      },
      "InternetGatewayDevice.X_HUAWEI_SyslogConfig": {
          "object": true,
          "objectTimestamp": 1607844895520,
          "writable": false,
          "writableTimestamp": 1607844895520
      },
      "DeviceID.Manufacturer": {
          "value": [
              "Huawei Technologies Co., Ltd.",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983,
          "object": false,
          "objectTimestamp": 1607846440983
      },
      "DeviceID.OUI": {
          "value": [
              "202BC1",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983,
          "object": false,
          "objectTimestamp": 1607846440983
      },
      "DeviceID.ProductClass": {
          "value": [
              "BM632w",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983,
          "object": false,
          "objectTimestamp": 1607846440983
      },
      "DeviceID.SerialNumber": {
          "value": [
              "000003",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983,
          "object": false,
          "objectTimestamp": 1607846440983
      },
      "Events.Inform": {
          "value": [
              1607846440983,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983,
          "object": false,
          "objectTimestamp": 1607846440983
      },
      "Events.Registered": {
          "value": [
              1607844895520,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846440983,
          "writable": false,
          "writableTimestamp": 1607846440983,
          "object": false,
          "objectTimestamp": 1607846440983
      }
  },
  {
      "DeviceID.ID": {
          "value": [
              "202BC1-BM632w-000004",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368,
          "object": false,
          "objectTimestamp": 1607846440368
      },
      "InternetGatewayDevice": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.DeviceInfo": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.DeviceInfo.HardwareVersion": {
          "value": [
              "00040501",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440369,
          "object": false,
          "objectTimestamp": 1607844896491
      },
      "InternetGatewayDevice.DeviceInfo.ProvisioningCode": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440369,
          "object": false,
          "objectTimestamp": 1607844896491
      },
      "InternetGatewayDevice.DeviceInfo.SoftwareVersion": {
          "value": [
              "V100R001IRQC56B017",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440369,
          "object": false,
          "objectTimestamp": 1607844896491
      },
      "InternetGatewayDevice.DeviceInfo.SpecVersion": {
          "value": [
              "1.0",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440369,
          "object": false,
          "objectTimestamp": 1607844896491
      },
      "InternetGatewayDevice.DeviceSummary": {
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.IDLE": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.IPPingDiagnostics": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.LANDevice": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.LANDevice.1": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Active": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.HostName": {
          "value": [
              "android-d87bf88d22e66acf",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.IPAddress": {
          "value": [
              "192.168.1.2",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.MACAddress": {
          "value": [
              "40:B0:FA:9C:4A:50",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Active": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.HostName": {
          "value": [
              "android-d91540e8540e9c7a",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.IPAddress": {
          "value": [
              "192.168.1.4",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.MACAddress": {
          "value": [
              "10:68:3F:77:88:20",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Active": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.HostName": {
          "value": [
              "Lena-PC",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.IPAddress": {
          "value": [
              "192.168.1.5",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.MACAddress": {
          "value": [
              "C0:14:3D:C0:CF:93",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Active": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.HostName": {
          "value": [
              "localhost",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.IPAddress": {
          "value": [
              "192.168.1.6",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.MACAddress": {
          "value": [
              "1C:3E:84:AC:BB:76",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Active": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.HostName": {
          "value": [
              "Munas-iphone",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.IPAddress": {
          "value": [
              "192.168.1.7",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.MACAddress": {
          "value": [
              "C0:9F:42:56:33:DF",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Active": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.HostName": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.IPAddress": {
          "value": [
              "192.168.1.3",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.MACAddress": {
          "value": [
              "20:10:7a:08:4d:43",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.HostNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.X_HUAWEI_Host": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceConfig": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.LANHostConfigManagement": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.LANWLANConfigurationNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AssociatedDevice": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AutoChannelEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BSSID": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BeaconType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Channel": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Enable": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MACAddressControlEnabled": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MaxBitRate": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.PreSharedKey": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.RegulatoryDomain": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID": {
          "value": [
              "Sanad'Ibras 1Amin WiMax",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSIDAdvertisementEnabled": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Standard": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Status": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalAssociations": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesReceived": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesSent": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsReceived": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsSent": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPEncryptionLevel": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKey": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKeyIndex": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WMMEnable": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPS": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_AssociateDeviceNum": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_ChannelUsed": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_PowerValue": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesReceivedError": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesSentError": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsReceivedError": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsSentError": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WLANVersion": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NBWControl": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NGIControl": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NHtMcs": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NTxRxStream": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanIsolateControl": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilter": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilternum": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilterpolicy": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanStaWakeEnable": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDevice.1.X_HUAWEI_WLANEnable": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.LANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.Layer2Bridging": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.Layer3Forwarding": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.Layer3QoS": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestPassword": {
          "value": [
              "9qrzidpy16",
              "xsd:string"
          ],
          "valueTimestamp": 1607844896510,
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestURL": {
          "value": [
              "http://127.0.0.1:59308/",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440369,
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestUsername": {
          "value": [
              "202BC1-BM632w-000004",
              "xsd:string"
          ],
          "valueTimestamp": 1607844896510,
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.ParameterKey": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440369,
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.Password": {
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607844896509,
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformInterval": {
          "value": [
              300,
              "xsd:unsignedInt"
          ],
          "valueTimestamp": 1607844896510,
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.URL": {
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.Username": {
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServer.X_HUAWEI_SSLCertEnable": {
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.ManagementServerConfig": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.OperatorProfile": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.Services": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.Time": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.UserInterface": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.WANDevice": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.WANDevice.1": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice": {
          "object": true,
          "objectTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection": {
          "object": true,
          "objectTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AddressingType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AutoDisconnectTime": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionStatus": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionTrigger": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionType": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSEnabled": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSOverrideAllowed": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSServers": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DefaultGateway": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Enable": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ExternalIPAddress": {
          "value": [
              "172.3.89.139",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440369,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.LastConnectionError": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddress": {
          "value": [
              "20:2B:C1:E0:06:65",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440370,
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddressOverride": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MaxMTUSize": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.NATEnabled": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Name": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMapping": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMappingNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PossibleConnectionTypes": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Reset": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.RouteProtocolRx": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Stats": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.SubnetMask": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Uptime": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DHCPRelay": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DMZ": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTrigger": {
          "object": true,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTriggerNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_ServiceList": {
          "object": false,
          "objectTimestamp": 1607846440368,
          "writable": true,
          "writableTimestamp": 1607846440368
      },
      "InternetGatewayDevice.WANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.WiMAX": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.X_HUAWEI_FireWall": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": true,
          "writableTimestamp": 1607844896491
      },
      "InternetGatewayDevice.X_HUAWEI_SyslogConfig": {
          "object": true,
          "objectTimestamp": 1607844896491,
          "writable": false,
          "writableTimestamp": 1607844896491
      },
      "DeviceID.Manufacturer": {
          "value": [
              "Huawei Technologies Co., Ltd.",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368,
          "object": false,
          "objectTimestamp": 1607846440368
      },
      "DeviceID.OUI": {
          "value": [
              "202BC1",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368,
          "object": false,
          "objectTimestamp": 1607846440368
      },
      "DeviceID.ProductClass": {
          "value": [
              "BM632w",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368,
          "object": false,
          "objectTimestamp": 1607846440368
      },
      "DeviceID.SerialNumber": {
          "value": [
              "000004",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368,
          "object": false,
          "objectTimestamp": 1607846440368
      },
      "Events.Inform": {
          "value": [
              1607846440368,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368,
          "object": false,
          "objectTimestamp": 1607846440368
      },
      "Events.Registered": {
          "value": [
              1607844896491,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846440368,
          "writable": false,
          "writableTimestamp": 1607846440368,
          "object": false,
          "objectTimestamp": 1607846440368
      }
  },
  {
      "DeviceID.ID": {
          "value": [
              "202BC1-BM632w-000001",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881,
          "object": false,
          "objectTimestamp": 1607846440881
      },
      "InternetGatewayDevice": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.DeviceInfo": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.DeviceInfo.HardwareVersion": {
          "value": [
              "00040501",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440882,
          "object": false,
          "objectTimestamp": 1607844893561
      },
      "InternetGatewayDevice.DeviceInfo.ProvisioningCode": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440882,
          "object": false,
          "objectTimestamp": 1607844893561
      },
      "InternetGatewayDevice.DeviceInfo.SoftwareVersion": {
          "value": [
              "V100R001IRQC56B017",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440882,
          "object": false,
          "objectTimestamp": 1607844893561
      },
      "InternetGatewayDevice.DeviceInfo.SpecVersion": {
          "value": [
              "1.0",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440882,
          "object": false,
          "objectTimestamp": 1607844893561
      },
      "InternetGatewayDevice.DeviceSummary": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.IDLE": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.IPPingDiagnostics": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.LANDevice": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.LANDevice.1": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Active": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.HostName": {
          "value": [
              "android-d87bf88d22e66acf",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.IPAddress": {
          "value": [
              "192.168.1.2",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.MACAddress": {
          "value": [
              "40:B0:FA:9C:4A:50",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.1.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Active": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.HostName": {
          "value": [
              "android-d91540e8540e9c7a",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.IPAddress": {
          "value": [
              "192.168.1.4",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.MACAddress": {
          "value": [
              "10:68:3F:77:88:20",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.2.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Active": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.HostName": {
          "value": [
              "Lena-PC",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.IPAddress": {
          "value": [
              "192.168.1.5",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.MACAddress": {
          "value": [
              "C0:14:3D:C0:CF:93",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.3.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Active": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.HostName": {
          "value": [
              "localhost",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.IPAddress": {
          "value": [
              "192.168.1.6",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.MACAddress": {
          "value": [
              "1C:3E:84:AC:BB:76",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.4.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Active": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.HostName": {
          "value": [
              "Munas-iphone",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.IPAddress": {
          "value": [
              "192.168.1.7",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.MACAddress": {
          "value": [
              "C0:9F:42:56:33:DF",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.5.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Active": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.AddressSource": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.HostName": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.IPAddress": {
          "value": [
              "192.168.1.3",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.Layer2Interface": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.LeaseTimeRemaining": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.MACAddress": {
          "value": [
              "20:10:7a:08:4d:43",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893575,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.Host.6.X_HUAWEI_DeviceType": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.HostNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.Hosts.X_HUAWEI_Host": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceConfig": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.LANEthernetInterfaceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.LANHostConfigManagement": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.LANWLANConfigurationNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AssociatedDevice": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.AutoChannelEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607844893573,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BSSID": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BasicEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.BeaconType": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Channel": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Enable": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.IEEE11iEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MACAddressControlEnabled": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.MaxBitRate": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.PreSharedKey": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.RegulatoryDomain": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID": {
          "value": [
              "Sanad'Ibras 1Amin WiMax",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440883,
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSIDAdvertisementEnabled": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Standard": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.Status": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalAssociations": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesReceived": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalBytesSent": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsReceived": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.TotalPacketsSent": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPEncryptionLevel": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKey": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WEPKeyIndex": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WMMEnable": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPAEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.WPS": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_AssociateDeviceNum": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_ChannelUsed": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedAuthenticationMode": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_MixedEncryptionModes": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_PowerValue": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesReceivedError": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalBytesSentError": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsReceivedError": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_TotalPacketsSentError": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WLANVersion": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NBWControl": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NGIControl": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NHtMcs": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_Wlan11NTxRxStream": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanIsolateControl": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilter": {
          "object": true,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilternum": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanMacFilterpolicy": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.X_HUAWEI_WlanStaWakeEnable": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDevice.1.X_HUAWEI_WLANEnable": {
          "object": false,
          "objectTimestamp": 1607846440881,
          "writable": true,
          "writableTimestamp": 1607846440881
      },
      "InternetGatewayDevice.LANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.Layer2Bridging": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.Layer3Forwarding": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.Layer3QoS": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestPassword": {
          "value": [
              "1edjqr6n2b",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893580,
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestURL": {
          "value": [
              "http://127.0.0.1:59268/",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440882,
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.ConnectionRequestUsername": {
          "value": [
              "202BC1-BM632w-000001",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893580,
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.ParameterKey": {
          "value": [
              "",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440882,
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.Password": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformEnable": {
          "value": [
              true,
              "xsd:boolean"
          ],
          "valueTimestamp": 1607844893579,
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.PeriodicInformInterval": {
          "value": [
              300,
              "xsd:unsignedInt"
          ],
          "valueTimestamp": 1607844893580,
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.URL": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.Username": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServer.X_HUAWEI_SSLCertEnable": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.ManagementServerConfig": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.OperatorProfile": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.Services": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.Time": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.UserInterface": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice": {
          "object": true,
          "objectTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection": {
          "object": true,
          "objectTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AddressingType": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.AutoDisconnectTime": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionStatus": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionTrigger": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionType": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSEnabled": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSOverrideAllowed": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DNSServers": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.DefaultGateway": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Enable": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ExternalIPAddress": {
          "value": [
              "172.3.89.139",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440882,
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.LastConnectionError": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddress": {
          "value": [
              "20:2B:C1:E0:06:65",
              "xsd:string"
          ],
          "valueTimestamp": 1607844893565,
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MACAddressOverride": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.MaxMTUSize": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.NATEnabled": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Name": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMapping": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PortMappingNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.PossibleConnectionTypes": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Reset": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.RouteProtocolRx": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Stats": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.SubnetMask": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.Uptime": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DHCPRelay": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_DMZ": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTrigger": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_PortTriggerNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.X_HUAWEI_ServiceList": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WANDeviceNumberOfEntries": {
          "object": false,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.WiMAX": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.X_HUAWEI_FireWall": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": true,
          "writableTimestamp": 1607844893561
      },
      "InternetGatewayDevice.X_HUAWEI_SyslogConfig": {
          "object": true,
          "objectTimestamp": 1607844893561,
          "writable": false,
          "writableTimestamp": 1607844893561
      },
      "DeviceID.Manufacturer": {
          "value": [
              "Huawei Technologies Co., Ltd.",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881,
          "object": false,
          "objectTimestamp": 1607846440881
      },
      "DeviceID.OUI": {
          "value": [
              "202BC1",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881,
          "object": false,
          "objectTimestamp": 1607846440881
      },
      "DeviceID.ProductClass": {
          "value": [
              "BM632w",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881,
          "object": false,
          "objectTimestamp": 1607846440881
      },
      "DeviceID.SerialNumber": {
          "value": [
              "000001",
              "xsd:string"
          ],
          "valueTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881,
          "object": false,
          "objectTimestamp": 1607846440881
      },
      "Events.Inform": {
          "value": [
              1607846440881,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881,
          "object": false,
          "objectTimestamp": 1607846440881
      },
      "Events.Registered": {
          "value": [
              1607844893561,
              "xsd:dateTime"
          ],
          "valueTimestamp": 1607846440881,
          "writable": false,
          "writableTimestamp": 1607846440881,
          "object": false,
          "objectTimestamp": 1607846440881
      }
  }
];


