import { Ace } from 'ace-builds';

export interface CustomAnnotation extends Ace.Annotation {
  custom?: boolean;
}

export interface ConnectorConfigValidation {
  valid: boolean;
  annotations: CustomAnnotation[];
}
