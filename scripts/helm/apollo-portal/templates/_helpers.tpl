{{/* vim: set filetype=mustache: */}}

{{/*
Full name for apollo-portal
*/}}
{{- define "apollo.portal.fullName" -}}
{{- if .Values.fullNameOverride -}}
{{- .Values.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- if contains .Values.name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name .Values.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "apollo.portal.labels" -}}
{{- if .Chart.AppVersion -}}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
{{- end -}}

{{/*
Service name for portal
*/}}
{{- define "apollo.portal.serviceName" -}}
{{- if .Values.service.fullNameOverride -}}
{{- .Values.service.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{ include "apollo.portal.fullName" .}}
{{- end -}}
{{- end -}}

{{/*
Service name for portaldb
*/}}
{{- define "apollo.portaldb.serviceName" -}}
{{- if .Values.portaldb.service.enabled -}}
{{- if .Values.portaldb.service.fullNameOverride -}}
{{- .Values.portaldb.service.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name .Values.portaldb.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- else -}}
{{- .Values.portaldb.host -}}
{{- end -}}
{{- end -}}

{{/*
Service port for portaldb
*/}}
{{- define "apollo.portaldb.servicePort" -}}
{{- if .Values.portaldb.service.enabled -}}
{{- .Values.portaldb.service.port -}}
{{- else -}}
{{- .Values.portaldb.port -}}
{{- end -}}
{{- end -}}
