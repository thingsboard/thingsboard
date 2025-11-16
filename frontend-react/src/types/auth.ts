export interface LoginRequest {
  username: string
  password: string
}

export interface TokenResponse {
  token: string
  refresh_token: string
  token_type: string
  user?: UserInfo
}

export interface UserInfo {
  id: string
  email: string
  first_name?: string
  last_name?: string
  authority: 'SYS_ADMIN' | 'TENANT_ADMIN' | 'CUSTOMER_USER'
  tenant_id?: string
  customer_id?: string
}
