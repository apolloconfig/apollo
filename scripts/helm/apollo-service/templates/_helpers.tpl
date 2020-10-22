{{/* vim: set filetype=mustache: */}}

{{/*
Common labels
*/}}
{{- define "apollo.service.labels" -}}
{{- if .Chart.AppVersion -}}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
{{- end -}}

{{/*
Service name for configdb
*/}}
{{- define "apollo.configdb.serviceName" -}}
{{- if .Values.configdb.service.enabled -}}
{{- if .Values.configdb.service.fullNameOverride -}}
{{- .Values.configdb.service.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name .Values.configdb.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- else -}}
{{- .Values.configdb.host -}}
{{- end -}}
{{- end -}}

{{/*
Full name for config service
*/}}
{{- define "apollo.configService.fullName" -}}
{{- if .Values.configService.fullNameOverride -}}
{{- .Values.configService.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- if contains .Values.configService.name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name .Values.configService.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Service name for config service
*/}}
{{- define "apollo.configService.serviceName" -}}
{{- if .Values.configService.service.fullNameOverride -}}
{{- .Values.configService.service.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{ include "apollo.configService.fullName" .}}
{{- end -}}
{{- end -}}

{{/*
Config service url to be accessed by apollo-client
*/}}
{{- define "apollo.configService.serviceUrl" -}}
{{- if .Values.configService.config.configServiceUrlOverride -}}
{{ .Values.configService.config.configServiceUrlOverride }}
{{- else -}}
http://{{ include "apollo.configService.serviceName" .}}.{{ .Release.Namespace }}:{{ .Values.configService.service.port }}{{ .Values.configService.config.contextPath }}
{{- end -}}
{{- end -}}

{{/*
Full name for admin service
*/}}
{{- define "apollo.adminService.fullName" -}}
{{- if .Values.adminService.fullNameOverride -}}
{{- .Values.adminService.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- if contains .Values.adminService.name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name .Values.adminService.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Service name for admin service
*/}}
{{- define "apollo.adminService.serviceName" -}}
{{- if .Values.adminService.service.fullNameOverride -}}
{{- .Values.adminService.service.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{ include "apollo.adminService.fullName" .}}
{{- end -}}
{{- end -}}

{{/*
Admin service url to be accessed by apollo-portal
*/}}
{{- define "apollo.adminService.serviceUrl" -}}
{{- if .Values.configService.config.adminServiceUrlOverride -}}
{{ .Values.configService.config.adminServiceUrlOverride -}}
{{- else -}}
http://{{ include "apollo.adminService.serviceName" .}}.{{ .Release.Namespace }}:{{ .Values.adminService.service.port }}{{ .Values.adminService.config.contextPath }}
{{- end -}}
{{- end -}}
