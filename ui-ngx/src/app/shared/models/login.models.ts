// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Authority } from '@shared/models/authority.enum';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface PublicLoginRequest {
  publicId: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  scope?: Authority;
}
