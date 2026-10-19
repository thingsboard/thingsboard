// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface CreatorView {
  id: string;
  createdTime: number;
  displayName: string;
  contactEmail: string;
  website: string;
  description: string;
  avatarUrl: string;
  githubUrl: string;
  linkedinUrl: string;
  twitterUrl: string;
  youtubeUrl: string;
  publishedCount: number;
  verified: boolean;
}
