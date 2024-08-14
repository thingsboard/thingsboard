import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ConnectorBaseConfig,
  ConnectorConfigValidation
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { defaultHttpOptionsFromConfig } from '@core/http/http-utils';

@Injectable()
export class ConfigurationValidateService {

  constructor(private http: HttpClient) {}

  validateConfiguration(
    gatewayDeviceId: string,
    connectorType: string,
    connectorConfig: ConnectorBaseConfig
  ): Observable<ConnectorConfigValidation> {
    const url = `/api/gateway/${gatewayDeviceId}/configuration/${connectorType}/validate`;
    return this.http.post(
      url,
      connectorConfig,
      defaultHttpOptionsFromConfig({ignoreErrors: true})
    ) as Observable<ConnectorConfigValidation>;
  }
}
