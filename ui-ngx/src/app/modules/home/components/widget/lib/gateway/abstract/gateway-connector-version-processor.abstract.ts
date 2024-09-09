import { GatewayConnector, GatewayVersion } from '@home/components/widget/lib/gateway/gateway-widget.models';

export abstract class GatewayConnectorVersionProcessor<BasicConfig> {
  gatewayVersion: number;
  configVersion: number;

  protected constructor(protected gatewayVersionStr: string, protected connector: GatewayConnector<BasicConfig>) {
    this.gatewayVersion = this.parseVersion(this.gatewayVersionStr);
    this.configVersion = this.parseVersion(connector.configVersion);
  }

  getProcessedByVersion(): GatewayConnector<BasicConfig> {
    if (this.isVersionUpdateNeeded()) {
      return this.isVersionUpgradeNeeded()
        ? this.getUpgradedVersion()
        : this.getDowngradedVersion();
    }

    return this.connector;
  }

  private isVersionUpdateNeeded(): boolean {
    if (!this.gatewayVersion) {
      return false;
    }

    return this.configVersion !== this.gatewayVersion;
  }

  private isVersionUpgradeNeeded(): boolean {
    return (!this.configVersion || this.configVersion < this.gatewayVersion) && this.gatewayVersionStr === GatewayVersion.Current;
  }

  private parseVersion(version: string): number {
    return Number(version?.replace(/\./g, ''));
  }

  protected abstract getDowngradedVersion(): GatewayConnector<BasicConfig>;
  protected abstract getUpgradedVersion(): GatewayConnector<BasicConfig>;
}
