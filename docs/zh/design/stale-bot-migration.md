# Stale Bot 迁移调研与设计

## 1. 文档信息

- 状态：已实现，待合并与上线
- 调研日期：2026-10-08
- 影响范围：GitHub Issue、Pull Request 及仓库 Actions
- 目标：将已废弃的 Probot Stale GitHub App 迁移至 `actions/stale`

## 2. 背景

Apollo 当前通过 `.github/stale.yml` 配置 Probot Stale GitHub App，自动标记并关闭长时间没有活动的 Issue 和 Pull Request。

`probot/stale` 已于 2023-05-20 归档，项目 README 明确建议迁移到 [`actions/stale`](https://github.com/actions/stale)。继续使用旧 App 存在以下问题：

1. 上游不再维护，无法获得安全更新和 GitHub API 兼容性修复。
2. 自动化逻辑依赖仓库外部安装的 GitHub App，配置与运行记录不集中在 Actions 中。
3. 旧配置包含已无法直接映射到新版 Action 的能力，需要明确迁移后的行为差异。

## 3. 当前接入情况

### 3.1 接入方式

仓库当前使用的是 Probot Stale GitHub App，而不是 GitHub Actions：

- 配置文件：`.github/stale.yml`
- 配置文件第 16 行明确标注 `Configuration for probot-stale`
- `.github/workflows/` 下不存在 Stale Actions workflow
- `stale[bot]` 在 2026-10-03 仍对 [Issue #5670](https://github.com/apolloconfig/apollo/issues/5670#issuecomment-5965333426) 执行了 stale 标记，说明旧 App 仍在运行

仅删除 `.github/stale.yml` 不能视为完成迁移。旧 App 需要由具有 GitHub App 管理权限的管理员停用或从仓库安装范围中移除，否则可能继续以默认配置运行，或与新 workflow 重复处理。

### 3.2 当前处理策略

| 对象 | 标记 stale | 关闭等待时间 | 单次处理上限 |
| --- | ---: | ---: | ---: |
| Issue | 30 天无活动 | 标记后 7 天 | 30 |
| Pull Request | 30 天无活动 | 标记后 14 天 | 30 |

以下标签会豁免 stale 处理：

- `bug`
- `discussion`
- `enhancement`
- `feature`
- `feature request`
- `help wanted`
- `info`
- `need investigation`
- `tips`

此外，当前配置会豁免：

- 位于 Project 中的 Issue 或 Pull Request
- 关联 Milestone 的 Issue 或 Pull Request
- 已分配 Assignee 的 Issue 或 Pull Request

### 3.3 配置历史

| Commit | 日期 | 变更 |
| --- | --- | --- |
| `1cbb3f26` | 2019-11-26 | 首次接入 stale bot |
| `b9b79111` | 2019-12-01 | 增加 `feature request` 豁免标签 |
| `ab6a1725` | 2021-07-03 | 调整为当前 30/7/14 天策略，并将处理上限提高到 30 |

## 4. 目标方案

### 4.1 版本选择

截至调研日期，`actions/stale` 最新稳定版本是 `v11.0.0`，发布于 2026-07-28。

为了避免可移动 tag 被替换，workflow 使用完整 commit SHA，并在行尾标注版本：

```yaml
uses: actions/stale@4391f3da665fdf50b6810c1a66712fb9ba21aa93 # v11.0.0
```

`v11.0.0` 使用 Node.js 24。GitHub-hosted `ubuntu-latest` runner 可以直接运行；如果未来改用 self-hosted runner，Actions Runner 版本需要不低于 `2.327.1`。

### 4.2 触发方式

workflow 每天运行一次，同时支持手动触发：

```yaml
on:
  schedule:
    - cron: "17 2 * * *"
  workflow_dispatch:
```

Cron 使用 UTC 时间。选择非整点执行可以避开 GitHub Actions 定时任务的常见高峰。

### 4.3 权限

```yaml
permissions:
  actions: write
  issues: write
  pull-requests: write
```

说明：

- `issues: write`：添加标签、评论和关闭 Issue。
- `pull-requests: write`：添加标签、评论和关闭 Pull Request。
- `actions: write`：保存 `operations-per-run` 分批处理状态。
- 不启用 `delete-branch`，因此不需要 `contents: write`。
- 不需要 Personal Access Token，使用默认 `github.token` 即可。
- workflow 不读取仓库内容，因此不需要 `actions/checkout`。

## 5. 配置字段映射

| 旧 Probot 配置 | `actions/stale` 配置 | 说明 |
| --- | --- | --- |
| `daysUntilStale` | `days-before-issue-stale`、`days-before-pr-stale` | 分别显式配置 Issue 和 PR |
| `daysUntilClose` | `days-before-issue-close`、`days-before-pr-close` | `false` 对应 `-1` |
| `exemptLabels` | `exempt-issue-labels`、`exempt-pr-labels` | 新版使用逗号分隔字符串 |
| `exemptMilestones` | `exempt-all-milestones` | 可直接映射 |
| `exemptAssignees` | `exempt-all-assignees` | 可直接映射 |
| `exemptProjects` | 无直接等价配置 | 需要改变管理方式或增加额外脚本 |
| `staleLabel` | `stale-issue-label`、`stale-pr-label` | 分别显式配置 |
| `issues.markComment` | `stale-issue-message` | 可直接迁移现有文案 |
| `issues.closeComment` | `close-issue-message` | 可直接迁移现有文案 |
| `pulls.markComment` | `stale-pr-message` | 可直接迁移现有文案 |
| `pulls.closeComment` | `close-pr-message` | 可直接迁移现有文案 |
| `limitPerRun` | `operations-per-run` | 含义不同，只能近似映射 |

### 5.1 Project 豁免差异

`actions/stale@v11` 没有“所有位于 Project 中的条目自动豁免”配置。当前 GitHub token 也没有 `read:project` 权限，因此本次调研无法统计 Apollo Projects v2 中可能受影响的条目数量。

可选方案：

1. 使用专用豁免标签，例如 `stale-exempt`，由维护者为需要长期保留的 Project 条目添加该标签。
2. 编写额外 GraphQL workflow，同步 Project 条目到豁免标签，但会增加权限、代码和维护成本。
3. 放弃 Project 自动豁免，接受 Project 条目可能被 stale 的行为变化。

本次实现未引入 `stale-exempt`，因为上游仓库目前不存在该标签，而标签创建不属于代码变更。因此，迁移后 Project 成员关系本身不再自动豁免，这是相对旧配置的明确行为差异。

如果维护者需要继续保护 Project 中的长期条目，推荐后续创建 `stale-exempt` 标签，将其加入 `exempt-issue-labels` 和 `exempt-pr-labels`，并在切换前为相应条目补充标签。该方案使用 `actions/stale` 原生能力，权限和维护成本最低，且豁免原因可以直接从 Issue 或 Pull Request 页面看到。

### 5.2 单次处理上限差异

旧配置分别为 Issue 和 Pull Request 设置 `limitPerRun: 30`。新版只有全局 `operations-per-run`，并且统计的是 GitHub API 写操作，而不是处理的 Issue 或 Pull Request 数量。

一次 stale 处理通常包含添加标签和发表评论等多个写操作，因此不存在严格的一对一映射。初始值建议设置为 `60`，上线后根据执行日志、积压数量和 API 使用量调整。

`actions/stale` 会保存分批处理状态。当一次运行达到操作上限时，下一次运行会从未处理的位置继续。

### 5.3 Issue 关闭原因差异

`actions/stale` 默认使用：

```yaml
close-issue-reason: not_planned
```

现有 Bot 最近关闭的 Issue #5621 和 #5613 的 `state_reason` 均为 `completed`。为了保持现有语义，需要显式配置：

```yaml
close-issue-reason: completed
```

Pull Request 不使用该字段。

## 6. 目标 Workflow

本次新增 `.github/workflows/stale.yml`：

```yaml
#
# Copyright 2026 Apollo Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

name: Mark stale issues and pull requests

on:
  schedule:
    - cron: "17 2 * * *"
  workflow_dispatch:

permissions:
  actions: write
  issues: write
  pull-requests: write

jobs:
  stale:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/stale@4391f3da665fdf50b6810c1a66712fb9ba21aa93 # v11.0.0
        with:
          days-before-issue-stale: 30
          days-before-issue-close: 7
          days-before-pr-stale: 30
          days-before-pr-close: 14

          stale-issue-label: stale
          stale-pr-label: stale

          exempt-issue-labels: >-
            bug,
            discussion,
            enhancement,
            feature,
            feature request,
            help wanted,
            info,
            need investigation,
            tips
          exempt-pr-labels: >-
            bug,
            discussion,
            enhancement,
            feature,
            feature request,
            help wanted,
            info,
            need investigation,
            tips

          exempt-all-milestones: true
          exempt-all-assignees: true
          remove-stale-when-updated: true

          stale-issue-message: >-
            This issue has been automatically marked as stale because it has not
            had activity in the last 30 days. It will be closed in 7 days unless
            it is tagged "help wanted" or other activity occurs. Thank you for
            your contributions.
          close-issue-message: >-
            This issue has been automatically closed because it has not had
            activity in the last 7 days. If this issue is still valid, please
            ping a maintainer and ask them to label it as "help wanted".
            Thank you for your contributions.

          stale-pr-message: >-
            This pull request has been automatically marked as stale because it
            has not had activity in the last 30 days. It will be closed in 14
            days if no further activity occurs. Please feel free to give a
            status update now, ping for review, or re-open when it's ready.
            Thank you for your contributions!
          close-pr-message: >-
            This pull request has been automatically closed because it has not
            had activity in the last 14 days. Please feel free to give a status
            update now, ping for review, or re-open when it's ready.
            Thank you for your contributions!

          close-issue-reason: completed
          operations-per-run: 60
```

当前 workflow 未包含 `stale-exempt`；如果后续创建该标签，应将其加入两个豁免标签列表。

## 7. 实施步骤

### 7.1 可选 Dry-run 阶段

本次提交中的 workflow 是最终可写配置，不包含 `debug-only`。如果维护者希望在正式切换前验证命中范围，可以采用两阶段上线：

1. 在合并前临时为 Action 增加：

   ```yaml
   debug-only: true
   ```

2. 合并后通过 `workflow_dispatch` 手动运行。
3. 检查日志中的候选 Issue 和 Pull Request，重点验证：
   - 30 天活跃时间计算是否符合预期。
   - 9 个豁免标签是否生效。
   - Milestone 和 Assignee 豁免是否生效。
   - Project 中没有其他豁免条件的条目是否会被命中。
   - Issue 和 Pull Request 的 7 天、14 天关闭期限是否正确。
4. 通过后续变更删除 `debug-only: true`，再执行正式切换。

`debug-only` 模式不会修改线上 Issue 或 Pull Request。采用该方式需要一次后续变更才能启用实际处理。

### 7.2 切换阶段

1. 确认是否接受 Project 自动豁免的行为差异；如采用标签方案，先创建并补充 `stale-exempt` 标签。
2. 由管理员停用或卸载旧 Stale GitHub App，或者将 Apollo 从 App 的仓库安装范围中移除。
3. 合并本次变更，同时删除 `.github/stale.yml` 并启用 `.github/workflows/stale.yml`。
4. 通过 `workflow_dispatch` 手动执行一次，并确认只有新 Action 产生操作。

旧 App 停用和新 workflow 启用应在同一切换窗口内完成，避免双重处理。

### 7.3 观察阶段

至少观察 14 天，覆盖 Issue 和 Pull Request 的完整关闭等待周期：

- 每日 workflow 是否成功运行。
- 是否存在权限或 API rate limit 错误。
- `operations-per-run` 是否造成长期积压。
- stale 标签在新活动出现后是否被移除。
- Issue 关闭原因是否保持为 `completed`。
- 是否发生 Project 条目被意外标记或关闭。
- 是否仍有 `stale[bot]` 产生的新评论。

## 8. 验证标准

迁移完成需要满足以下条件：

- 默认分支存在 `.github/workflows/stale.yml`。
- workflow 可以通过定时任务和手动方式触发。
- dry-run 命中范围符合预期。
- Issue 在 30 天后标记、再等待 7 天关闭。
- Pull Request 在 30 天后标记、再等待 14 天关闭。
- 标签、Milestone 和 Assignee 豁免生效。
- 新活动会移除 `stale` 标签。
- Issue 关闭原因是 `completed`。
- 旧 `.github/stale.yml` 已删除。
- 旧 Stale GitHub App 已停用或不再包含 Apollo 仓库。
- 不再出现旧 `stale[bot]` 的新操作。

## 9. 回滚方案

如果新 workflow 出现异常：

1. 立即将 `debug-only` 设置为 `true`，或者临时禁用 workflow。
2. 检查已受影响的 Issue 和 Pull Request，恢复错误标签或重新打开误关闭条目。
3. 修正筛选条件、权限或处理上限后重新 dry-run。
4. 只有在无法短期修复且确实需要恢复自动处理时，才考虑重新启用旧 App；旧 App 已停止维护，不应作为长期回滚状态。

回滚不应同时启用旧 App 和可写模式的新 workflow。

## 10. 风险与应对

| 风险 | 影响 | 应对措施 |
| --- | --- | --- |
| 新旧 Bot 同时运行 | 重复评论、标签或关闭 | 切换窗口内先停旧 App，再启用新 workflow |
| Project 豁免丢失 | Project 条目被意外标记或关闭 | 使用 `stale-exempt` 标签，并在 dry-run 中检查候选条目 |
| 操作上限含义变化 | 处理速度下降或出现积压 | 初始设置为 60，根据日志调整 |
| 默认关闭原因变化 | Issue 被标记为 `not_planned` | 显式设置 `close-issue-reason: completed` |
| 权限过大 | workflow 获得不必要的仓库权限 | 仅授予 `actions`、`issues`、`pull-requests` 写权限 |
| Action tag 被移动 | 执行非预期代码 | 固定完整 commit SHA |
| self-hosted runner 版本过旧 | Node.js 24 Action 无法运行 | 保持 GitHub-hosted runner，或升级 runner 至 `2.327.1+` |

## 11. 上线前确认事项

合并及正式切换前需要维护者确认：

1. 是否接受 Project 成员关系不再自动豁免，或在切换前创建并补充 `stale-exempt` 标签。
2. 是否需要先统计现有 Projects v2 条目；若需要，调研账号需获得 `read:project` 权限。
3. 由哪位具有 GitHub App 管理权限的管理员负责停用旧 App。

## 12. 参考资料

- [Probot Stale（已归档）](https://github.com/probot/stale)
- [actions/stale](https://github.com/actions/stale)
- [actions/stale v11.0.0](https://github.com/actions/stale/releases/tag/v11.0.0)
- [actions/stale v11.0.0 inputs](https://github.com/actions/stale/blob/v11.0.0/action.yml)
- [GitHub：Closing inactive issues](https://docs.github.com/en/actions/tutorials/manage-your-work/close-inactive-issues)
- [Apollo Issue #5670 stale comment](https://github.com/apolloconfig/apollo/issues/5670#issuecomment-5965333426)
