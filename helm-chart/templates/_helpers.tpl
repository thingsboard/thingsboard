{{/*
Expand the name of the chart.
*/}}
{{- define "thingsboard.name" -}}
{{- default .Chart.Name .Values.global.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "thingsboard.fullname" -}}
{{- if .Values.global.fullnameOverride }}
{{- .Values.global.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.global.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "thingsboard.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "thingsboard.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "thingsboard.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "thingsboardJSExecutor.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "thingsboardJSExecutor.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "thingsboardMqttTransport.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "thingsboardMqttTransport.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "thingsboardHttpTransport.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "thingsboardHttpTransport.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "thingsboardCoapTransport.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "thingsboardCoapTransport.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "thingsboardWebUI.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "thingsboardWebUI.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "postgres.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "postgres.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "cassandra.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "cassandra.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "redis.labels" -}}
helm.sh/chart: {{ include "thingsboard.chart" . }}
{{ include "redis.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "thingsboard.selectorLabels" -}}
app.kubernetes.io/name: {{ include "thingsboard.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "thingsboardJSExecutor.selectorLabels" -}}
app.kubernetes.io/name: {{ include "thingsboard.name" . }}-js-executor
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "thingsboardMqttTransport.selectorLabels" -}}
app.kubernetes.io/name: {{ include "thingsboard.name" . }}-mqtt-transport
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "thingsboardHttpTransport.selectorLabels" -}}
app.kubernetes.io/name: {{ include "thingsboard.name" . }}-http-transport
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "thingsboardCoapTransport.selectorLabels" -}}
app.kubernetes.io/name: {{ include "thingsboard.name" . }}-coap-transport
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "thingsboardWebUI.selectorLabels" -}}
app.kubernetes.io/name: {{ include "thingsboard.name" . }}-web-ui
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "postgres.selectorLabels" -}}
app.kubernetes.io/name: postgres
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "cassandra.selectorLabels" -}}
app.kubernetes.io/name: cassandra
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "redis.selectorLabels" -}}
app.kubernetes.io/name: redis
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "thingsboard.serviceAccountName" -}}
{{- if .Values.thingsboard.serviceAccount.create }}
{{- default (include "thingsboard.fullname" .) .Values.thingsboard.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.thingsboard.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "thingsboard.kafka.localHostname" -}}
{{- if .Values.kafka.enabled -}}
{{- $port := .Values.kafka.service.port | toString }}
{{- printf "%s-%s.%s.svc.cluster.local:%s" (include "thingsboard.fullname" .) "kafka" .Release.Namespace $port -}}
{{- else if .Values.externalKafka.hostname }}
{{- $port := .Values.externalKafka.port | toString }}
{{- printf "%s:%s" .Values.externalKafka.hostname $port -}}
{{- end -}}
{{- end -}}

{{- define "thingsboard.zookeeper.localHostname" -}}
{{- if .Values.kafka.enabled -}}
{{- printf "%s-%s.%s.svc.cluster.local:%s" (include "thingsboard.fullname" .) "zookeeper" .Release.Namespace "2181" -}}
{{- else if .Values.externalKafka.zookeeper.hostname }}
{{- $port := .Values.externalKafka.zookeeper.port | toString }}
{{- printf "%s:%s" .Values.externalKafka.zookeeper.hostname $port -}}
{{- end -}}
{{- end -}}