export interface TwoFactorAuthSettings {
  maxVerificationFailuresBeforeUserLockout: number;
  providers: Array<TwoFactorAuthProviderConfig>;
  totalAllowedTimeForVerification: number;
  useSystemTwoFactorAuthSettings: boolean;
  verificationCodeCheckRateLimit: string;
  verificationCodeSendRateLimit: string;
}

export type TwoFactorAuthProviderConfig = Partial<TotpTwoFactorAuthProviderConfig | SmsTwoFactorAuthProviderConfig>

export interface TotpTwoFactorAuthProviderConfig {
  providerType: TwoFactorAuthProviderType;
  issuerName: string;
}

export interface SmsTwoFactorAuthProviderConfig {
  providerType: TwoFactorAuthProviderType;
  smsVerificationMessageTemplate: string;
  verificationCodeLifetime: number;
}

export enum TwoFactorAuthProviderType{
  TOTP = 'TOTP',
  SMS = 'SMS'
}

export interface TwoFactorAuthSettingsForm extends TwoFactorAuthSettings {

}
