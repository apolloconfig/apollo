# Apollo 发布指南（Skill 自动化版）

本文档面向在 Codex / Claude Code 中通过自然语言触发 Skill 的发布方式。
目标是：减少手工步骤、统一发布质量，并在关键外部动作前保留人工确认。

## 1. 适用范围与推荐顺序

Apollo 发布流程由 3 个 Skill 覆盖：

1. `apollo-java-release`：发布 `apolloconfig/apollo-java`
2. `apollo-release`：发布 `apolloconfig/apollo`
3. `apollo-helm-chart-release`：发布 `apolloconfig/apollo-helm-chart`

推荐顺序：

1. 先发布 `apollo-java-release`（不依赖其它发布流程）
2. 再发布 `apollo-release`（通常依赖本次 Java SDK 版本）
3. 最后发布 `apollo-helm-chart-release`

> 若本次发布不涉及某个仓库，可跳过对应子流程。

## 2. 发布前准备

### 2.1 权限与工具

- GitHub 账号具备对应仓库的 PR、Release、Workflow、Discussion、Milestone 权限
- 本地可用命令：`git`、`gh`、`python3`、`jq`
- Helm Chart 发布额外需要：`helm`

### 2.2 仓库与分支状态

- 在每个目标仓库执行前，确保工作区干净（无未提交变更）
- 确认基线分支最新（`apollo` 默认 `master`，其余仓库按各自默认分支）

### 2.3 建议提前准备的发布输入

- 发布版本号（如 `2.8.0`）
- 下一开发版本（如 `2.9.0-SNAPSHOT`）
- Highlights 对应 PR 列表（如 `PR_ID_1,PR_ID_2,PR_ID_3`）

### 2.4 安装发布 Skill（Codex / Claude Code）

在使用本文档后续流程前，请先安装这 3 个 Skill：

- `apollo-java-release`
- `apollo-release`
- `apollo-helm-chart-release`

推荐方式（自然语言）：

1. 在 Codex 会话中使用 `skill-installer`
2. 安装来源指定为完整 GitHub 地址：`https://github.com/apolloconfig/apollo-skills`
3. 安装上述 3 个 Skill

示例表达（自然语言）：

- “请用 `skill-installer` 从 `https://github.com/apolloconfig/apollo-skills` 安装 `apollo-java-release`、`apollo-release`、`apollo-helm-chart-release`”

如需手动安装，也可以将这 3 个 Skill 目录放到本地 Skill 目录（通常为 `$CODEX_HOME/skills` 或 `~/.codex/skills`）后重启客户端。

## 3. 发布 Apollo Java（apollo-java-release）

### 3.1 触发方式（自然语言）

在 `apollo-java` 仓库会话中，直接发起类似请求：

- “使用 `apollo-java-release` 发布 `X.Y.Z`，下一个版本 `A.B.C-SNAPSHOT`，highlights 用这些 PR：`...`”

### 3.2 Skill 会自动完成

- 版本变更 PR（`revision` 从 SNAPSHOT 到正式版本）
- pre-release 创建
- 触发 `release.yml`，等待 Sonatype Central 发布完成
- 发布公告讨论（Announcements）
- 发布后回切到下一 SNAPSHOT，并创建 post-release PR
- pre-release 转正式 release

### 3.3 checkpoint 交互方式

- Skill 在关键外部动作前会自动暂停，并由系统提示是否继续
- 你只需在对话中选择继续，或要求先修改文案/参数

## 4. 发布 Apollo Server（apollo-release）

### 4.1 触发方式（自然语言）

在 `apollo` 仓库会话中，直接发起类似请求：

- “使用 `apollo-release` 发布 `X.Y.Z`，下一个版本 `A.B.C-SNAPSHOT`，highlights PR 是 `...`”

### 4.2 Skill 会自动完成

- 版本 bump PR（`pom.xml` 的 `revision`）
- 从 `CHANGES.md` 生成 Release Notes 与公告草稿
- 创建 pre-release（`vX.Y.Z`）
- 触发包构建与 checksum 上传（GitHub Action）
- 触发 Docker 发布 workflow
- pre-release 转正式 release
- 发布 Announcements 讨论
- 发布后回切 `next-snapshot`、归档 `CHANGES.md`、里程碑维护、创建 post-release PR

### 4.3 checkpoint 交互方式

与 `apollo-java-release` 一致：

- 系统会在关键步骤提示你确认是否继续
- 可在暂停点要求先调整 highlights、release note 或其它参数

## 5. 发布 Helm Chart（apollo-helm-chart-release）

### 5.1 触发方式（自然语言）

在 `apollo-helm-chart` 仓库会话中，直接发起类似请求：

- “使用 `apollo-helm-chart-release` 发布当前 chart 版本变更”

### 5.2 Skill 会自动完成

- chart 版本变更检测与一致性校验
- `helm lint`、`helm package`、`helm repo index`
- 变更白名单校验（防止误提交）
- 生成分支与 commit 草案
- 在 push / PR 前停下来等待你确认（默认不自动发布）

## 6. 发布后统一验收

建议至少检查：

1. Apollo Release 页面包含 3 个 zip + 3 个 sha1
2. Docker 镜像 tag 可用
3. Maven Central 上 apollo-java 对应版本可检索
4. Helm 仓库 `docs/index.yaml` 已包含新 chart 版本
5. 关键路径冒烟验证通过（配置发布、灰度发布、客户端拉取、Portal 核心操作）

## 7. 常见操作（Skill 使用视角）

### 7.1 中断后继续

直接在对话中要求继续即可，例如：

- “继续刚才的发布流程”
- “从下一个 checkpoint 继续执行”

Skill 会根据状态文件自动恢复，不会重复已完成步骤。

### 7.2 先演练再正式发布

可先要求 dry-run，例如：

- “先用 dry-run 跑一遍 `apollo-release`，我先检查流程”

确认输出后，再要求正式执行。

### 7.3 调整 Highlights / 文案

在 pre-release 创建前可直接提出调整，例如：

- “把 highlights 改成这些 PR：`...`，然后重新生成 release notes”

确认无误后再继续下一步。
