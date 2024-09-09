import { GatewayConnector } from '@home/components/widget/lib/gateway/gateway-widget.models';

export abstract class GatewayConnectorVersionProcessor<BasicConfig> {
  gatewayVersion: number;
  configVersion: number;

  protected constructor(protected gatewayVersionStr: string, protected connector: GatewayConnector) {
    this.gatewayVersion = this.parseVersion(this.gatewayVersionStr);
    this.configVersion = this.parseVersion(connector.configVersion);
  }

  getProcessedByVersion(): BasicConfig {
    if (this.isVersionUpdateNeeded()) {
      return this.configVersion > this.gatewayVersion
        ? this.getDowngradedVersion()
        : this.getUpgradedVersion();
    }

    return this.connector.configurationJson as unknown as BasicConfig;
  }

  private isVersionUpdateNeeded(): boolean {
    if (!this.gatewayVersion || !this.configVersion) {
      return false;
    }

    return this.configVersion !== this.gatewayVersion;
  }

  private parseVersion(version: string): number {
    return Number(version?.replace(/\./g, ''));
  }

  abstract getDowngradedVersion(): BasicConfig;
  abstract getUpgradedVersion(): BasicConfig;
}
