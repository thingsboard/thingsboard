///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { HasTenantId } from '@shared/models/entity.models';
import { AiModelId } from '@shared/models/id/ai-model-id';

export interface AiModel extends Omit<BaseData<AiModelId>, 'label'>, HasTenantId, ExportableEntity<AiModelId> {
  modelType: string;
  configuration: {
    provider: AiProvider
    providerConfig: {
      apiKey?: string;
      personalAccessToken?: string;
      endpoint?: string;
      serviceVersion?: string;
      projectId?: string;
      location?: string;
      serviceAccountKey?: string;
      serviceAccountKeyFileName?: string
    };
    modelId: string;
    temperature?: number | null;
    topP?: number;
    topK?: number;
    frequencyPenalty?: number;
    presencePenalty?: number;
    maxOutputTokens?: number;
  }
}

export enum AiProvider {
  OPENAI = 'OPENAI',
  AZURE_OPENAI = 'AZURE_OPENAI',
  GOOGLE_AI_GEMINI = 'GOOGLE_AI_GEMINI',
  GOOGLE_VERTEX_AI_GEMINI = 'GOOGLE_VERTEX_AI_GEMINI',
  MISTRAL_AI = 'MISTRAL_AI',
  ANTHROPIC = 'ANTHROPIC',
  AMAZON_BEDROCK = 'AMAZON_BEDROCK',
  GITHUB_MODELS = 'GITHUB_MODELS'
}

export const AiProviderWithApiKey: AiProvider[] = [
  AiProvider.OPENAI,
  AiProvider.AZURE_OPENAI,
  AiProvider.GOOGLE_AI_GEMINI,
  AiProvider.MISTRAL_AI,
  AiProvider.ANTHROPIC,
  AiProvider.AMAZON_BEDROCK
]

export const AiProviderTranslations = new Map<AiProvider, string>(
  [
    [AiProvider.OPENAI , 'ai-models.ai-providers.openai'],
    [AiProvider.AZURE_OPENAI , 'ai-models.ai-providers.azure-openai'],
    [AiProvider.GOOGLE_AI_GEMINI , 'ai-models.ai-providers.google-ai-gemini'],
    [AiProvider.GOOGLE_VERTEX_AI_GEMINI , 'ai-models.ai-providers.google-vertex-ai-gemini'],
    [AiProvider.MISTRAL_AI , 'ai-models.ai-providers.mistral-ai'],
    [AiProvider.ANTHROPIC , 'ai-models.ai-providers.anthropic'],
    [AiProvider.AMAZON_BEDROCK , 'ai-models.ai-providers.amazon-bedrock'],
    [AiProvider.GITHUB_MODELS , 'ai-models.ai-providers.github-models']
  ]
);

export const AiModelMap = new Map<AiProvider, string[]>(
  [
    [AiProvider.OPENAI , [
      'o4-mini',
      'o3-pro',
      'o3',
      'o3-mini',
      'o1',
      'gpt-4.1',
      'gpt-4.1-mini',
      'gpt-4.1-nano',
      'gpt-4o',
      'gpt-4o-mini'
    ]],
    [AiProvider.AZURE_OPENAI , []],
    [AiProvider.GOOGLE_AI_GEMINI , [
      'gemini-2.5-pro',
      'gemini-2.5-flash',
      'gemini-2.0-flash',
      'gemini-2.0-flash-lite',
    ]],
    [AiProvider.GOOGLE_VERTEX_AI_GEMINI , [
      'gemini-2.5-pro',
      'gemini-2.5-flash',
      'gemini-2.0-flash',
      'gemini-2.0-flash-lite',
    ]],
    [AiProvider.MISTRAL_AI , [
      'magistral-medium-latest',
      'magistral-small-latest',
      'mistral-large-latest',
      'mistral-medium-latest',
      'mistral-small-latest',
      'pixtral-large-latest',
      'ministral-8b-latest',
      'ministral-3b-latest',
      'open-mistral-nemo'
    ]],
    [AiProvider.ANTHROPIC , [
      'claude-opus-4-0',
      'claude-sonnet-4-0',
      'claude-3-7-sonnet-latest',
      'claude-3-5-sonnet-latest',
      'claude-3-5-haiku-latest'
    ]],
    [AiProvider.AMAZON_BEDROCK , []],
    [AiProvider.GITHUB_MODELS , []]
  ]
);

export interface AiModelWithUserMsg {
  userMessage: {
    contents: Array<{contentType: string; text: string}>;
  }
  chatModelConfig: {
    modelType: string;
    provider: AiProvider
    providerConfig: {
      apiKey?: string;
      personalAccessToken?: string;
      endpoint?: string;
      serviceVersion?: string;
      projectId?: string;
      location?: string;
      serviceAccountKey?: string;
      serviceAccountKeyFileName?: string
    };
    // chatModelConfig: {
      modelId: string;
      maxRetries: number;
      timeoutSeconds: number;
    // }
  }
}


export interface CheckConnectivityResult {
  status: string;
  errorDetails: string;
}
