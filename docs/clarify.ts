import { defineConfig } from '@clarify-labs/cli'

export default defineConfig({
  title: 'Apollo Documentation',
  description: 'Apollo configuration center documentation',
  navigation: {
    links: [
      { label: { en: 'GitHub', zh: 'GitHub' }, href: 'https://github.com/apolloconfig/apollo', external: true },
      { label: { en: 'Official Site', zh: '官网' }, href: 'https://www.apolloconfig.com/', external: true },
    ],
      tabs: [
    {
      tab: { en: 'Getting Started', zh: '快速开始' },
      pages: [
        {
          group: { en: 'Start Here', zh: '开始这里' },
          pages: [
            { page: 'README', title: { en: 'Overview', zh: '概览' } },
            { page: 'quick-start', title: { en: 'Quick Start', zh: '快速开始' } },
            { page: 'deployment/quick-start', title: { en: 'Quick Start Guide', zh: '快速开始指南' } },
            { page: 'deployment/quick-start-docker', title: { en: 'Docker Quick Start', zh: 'Docker 快速开始' } },
          ],
        },
      ],
    },
    {
      tab: { en: 'Deployment', zh: '部署' },
      pages: [
        {
          group: { en: 'Deployment Guides', zh: '部署指南' },
          pages: [
            { page: 'deployment/deployment-architecture', title: { en: 'Deployment Architecture', zh: '部署架构' } },
            { page: 'deployment/distributed-deployment-guide', title: { en: 'Distributed Deployment', zh: '分布式部署' } },
            { page: 'deployment/third-party-tool-btpanel', title: { en: 'BTPanel', zh: 'BTPanel' } },
            { page: 'deployment/third-party-tool-rainbond', title: { en: 'Rainbond', zh: 'Rainbond' } },
          ],
        },
      ],
    },
    {
      tab: { en: 'Client', zh: '客户端' },
      pages: [
        {
          group: { en: 'SDKs & Clients', zh: 'SDK 与客户端' },
          pages: [
            { page: 'client/java-sdk-user-guide', title: { en: 'Java SDK', zh: 'Java SDK' } },
            { page: 'client/nodejs-sdks-user-guide', title: { en: 'Node.js SDK', zh: 'Node.js SDK' } },
            { page: 'client/golang-sdks-user-guide', title: { en: 'Go SDK', zh: 'Go SDK' } },
            { page: 'client/dotnet-sdk-user-guide', title: { en: '.NET SDK', zh: '.NET SDK' } },
            { page: 'client/python-sdks-user-guide', title: { en: 'Python SDK', zh: 'Python SDK' } },
            { page: 'client/php-sdks-user-guide', title: { en: 'PHP SDK', zh: 'PHP SDK' } },
            { page: 'client/rust-sdks-user-guide', title: { en: 'Rust SDK', zh: 'Rust SDK' } },
            { page: 'client/c-sdks-user-guide', title: { en: 'C SDK', zh: 'C SDK' } },
            { page: 'client/cpp-sdks-user-guide', title: { en: 'C++ SDK', zh: 'C++ SDK' } },
            { page: 'client/other-language-client-user-guide', title: { en: 'Other Languages', zh: '其他语言' } },
            { page: 'client/k8s-configmap-user-guide', title: { en: 'Kubernetes ConfigMap', zh: 'Kubernetes ConfigMap' } },
          ],
        },
      ],
    },
    {
      tab: { en: 'Portal & Extension', zh: 'Portal 与扩展' },
      pages: [
        {
          group: { en: 'Portal & Extensions', zh: 'Portal 与扩展' },
          pages: [
            { page: 'portal/apollo-user-guide', title: { en: 'Portal User Guide', zh: 'Portal 用户指南' } },
            { page: 'portal/apollo-open-api-platform', title: { en: 'Open API Platform', zh: 'Open API 平台' } },
            { page: 'portal/apollo-user-practices', title: { en: 'User Practices', zh: '用户实践' } },
            { page: 'extension/portal-how-to-implement-user-login-function', title: { en: 'Implement User Login', zh: '实现用户登录' } },
            { page: 'extension/portal-how-to-enable-email-service', title: { en: 'Enable Email Service', zh: '启用邮件服务' } },
            { page: 'extension/portal-how-to-enable-session-store', title: { en: 'Enable Session Store', zh: '启用 Session Store' } },
            { page: 'extension/portal-how-to-enable-webhook-notification', title: { en: 'Enable Webhook Notification', zh: '启用 Webhook 通知' } },
          ],
        },
      ],
    },
    {
      tab: { en: 'Design & Contribution', zh: '设计与贡献' },
      pages: [
        {
          group: { en: 'Design & Development', zh: '设计与开发' },
          pages: [
            { page: 'design/apollo-introduction', title: { en: 'Introduction', zh: '简介' } },
            { page: 'design/apollo-design', title: { en: 'Design', zh: '设计' } },
            { page: 'design/apollo-core-concept-namespace', title: { en: 'Namespace Concept', zh: 'Namespace 概念' } },
            { page: 'contribution/apollo-development-guide', title: { en: 'Development Guide', zh: '开发指南' } },
            { page: 'contribution/apollo-release-guide', title: { en: 'Release Guide', zh: '发布指南' } },
          ],
        },
      ],
    },
    {
      tab: { en: 'FAQ & Community', zh: 'FAQ 与社区' },
      pages: [
        {
          group: { en: 'Support & Community', zh: '支持与社区' },
          pages: [
            { page: 'faq/faq', title: { en: 'FAQ', zh: 'FAQ' } },
            { page: 'faq/common-issues-in-deployment-and-development-phase', title: { en: 'Common Issues', zh: '常见问题' } },
            { page: 'community/team', title: { en: 'Team', zh: '团队' } },
            { page: 'community/thank-you', title: { en: 'Thank You', zh: '感谢' } },
            { page: 'misc/apollo-benchmark', title: { en: 'Benchmark', zh: '性能基准' } },
          ],
        },
      ],
    },
  ],
  },
  footer: {
    links: [
      { label: { en: 'GitHub', zh: 'GitHub' }, href: 'https://github.com/apolloconfig/apollo', external: true },
      { label: { en: 'Releases', zh: '版本发布' }, href: 'https://github.com/apolloconfig/apollo/releases', external: true },
    ],
    copyright: { en: '© 2026 Apollo Configuration Center', zh: '© 2026 Apollo 配置中心' },
  },
  locales: {
    default: 'en',
    missing: 'fallback',
    locales: [
      { code: 'en', label: 'English', dir: 'ltr' },
      { code: 'zh', label: '中文', dir: 'ltr' },
    ],
  },
  theme: {
    preset: 'default',
    layout: {
      maxWidth: '1200px',
    },
  },
})
