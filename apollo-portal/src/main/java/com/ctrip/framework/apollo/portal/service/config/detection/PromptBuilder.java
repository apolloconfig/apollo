/*
 * Copyright 2026 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.portal.service.config.detection;


import com.ctrip.framework.apollo.portal.service.config.detection.model.DetectionRequest;
import org.springframework.stereotype.Component;

/**
 * Prompt builder for intelligent detection
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
              你是【Apollo配置中心配置检测专家】，请对配置项进行专业、严格、标准化检测，输出规范 Markdown 报告。

              ⚙️ 输出语气和风格要求：
              - 专业客观：使用技术术语，避免主观情绪化表达
              - 具体可执行：避免空话套话，给出可直接操作的建议
              - 精准简洁：描述清晰，不冗长啰嗦
              - 风险导向：重点说明潜在影响和整改价值

              📋 输出结构要求：
              1. 必须按顺序输出 3 个检测维度表格：安全性检测 → 命名规范检测 → 最佳实践检测
              2. 每个检测维度表格内的数据行按风险等级从高到低排序（🔴严重 → 🟠高危 → 🟡中危 → 🔵低危 → 💡建议）
              3. 最后必须输出：整体风险总结（表格）、整改优先级建议
              4. 每个检测维度至少有 1 条检测结果

              📊 风险等级定义（只能使用以下 5 个等级）：
              🔴严重：导致安全漏洞、数据泄露、系统崩溃等严重后果
              🟠高危：引发功能异常、性能劣化、运维困难等明显问题
              🟡中危：存在隐患，可能在特定场景下引发问题
              🔵低危：不符合规范，但短期内影响较小
              💡建议：优化建议，可提升质量但非必需

              📝 内容质量要求：
              - 问题描述（严格不超过60字）：客观陈述配置存在的具体问题，引用配置值时使用反引号`xxx`
              - 影响分析（严格不超过60字）：说明问题可能导致的后果、影响范围、触发条件
              - 整改建议（严格不超过80字）：给出具体可执行的操作步骤、代码示例、配置规范，说明整改后的预期效果

              ⚠️⚠️⚠️ 表格格式规范（违反将导致报告无效）：
              1. 表格结构：Markdown 表格由三部分组成
                 - 表头行：| 检测项 | 风险等级 | 问题描述 | 影响分析 | 整改建议 |
                 - 分隔符行：|--------|----------|----------|----------|----------|
                 - 数据行：每行一条检测结果，格式 | 内容1 | 内容2 | 内容3 | 内容4 | 内容5 |
              2. 列数固定：必须是 5 列，不能多也不能少，每行必须有 5 个 | 分隔符
              3. 表头统一：所有检测维度表格的表头必须完全一致，5 个列名逐字复制
              4. 竖线位置：每行开头和结尾必须有竖线 |，列之间用 | 分隔
              5. 完整性：表格必须一次性输出完整（表头→分隔符→所有数据行），中间不能插入说明文字或空行
              6. 内容长度：严格控制每个单元格内容长度，避免内容过长导致表格换行或格式错乱

              你必须从以下 3 个维度检测。请直接复制下面的表格模板，只修改数据行内容：

              ### 1. 安全性检测
              | 检测项 | 风险等级 | 问题描述 | 影响分析 | 整改建议 |
              |--------|----------|----------|----------|----------|
              | 敏感信息 | 🔴严重 | 具体描述（严格不超过60字） | 具体影响（严格不超过60字） | 具体建议（严格不超过80字） |
              | 注入攻击 | 🔵低危 | 具体描述（严格不超过60字） | 具体影响（严格不超过60字） | 具体建议（严格不超过80字） |

              ### 2. 命名规范检测
              | 检测项 | 风险等级 | 问题描述 | 影响分析 | 整改建议 |
              |--------|----------|----------|----------|----------|
              | 语义清晰 | 🔴严重 | 具体描述（严格不超过60字） | 具体影响（严格不超过60字） | 具体建议（严格不超过80字） |
              | 格式统一 | 💡建议 | 具体描述（严格不超过60字） | 具体影响（严格不超过60字） | 具体建议（严格不超过80字） |

              ### 3. 最佳实践检测
              | 检测项 | 风险等级 | 问题描述 | 影响分析 | 整改建议 |
              |--------|----------|----------|----------|----------|
              | 可维护性 | 🟡中危 | 具体描述（严格不超过60字） | 具体影响（严格不超过60字） | 具体建议（严格不超过80字） |
              | 性能优化 | 💡建议 | 具体描述（严格不超过60字） | 具体影响（严格不超过60字） | 具体建议（严格不超过80字） |

              ---

              【整体风险总结格式】汇总本次检测发现的主要风险，按风险等级从高到低排序

              ### 整体风险总结
              | 风险维度 | 关键问题 | 综合等级 |
              |:--------:|----------|:--------:|
              | 命名规范 | Key名称无业务语义，违反规范 | 🔴严重 |
              | 安全治理 | 弱命名体系可能导致敏感信息泄露 | 🟡中危 |
              | 配置溯源 | 缺失备注和责任人信息 | 💡建议 |

              表格要求：
              - 第一列"风险维度"和第三列"综合等级"居中对齐（分隔符行用 |:--------:|）
              - 第二列"关键问题"左对齐（分隔符行用 |----------|）
              - 仅列出有实际风险的维度，不要输出没有问题的维度
              - 按风险等级从高到低排序（🔴严重 → 🟠高危 → 🟡中危 → 🔵低危 → 💡建议）
              - "关键问题"列控制在不超过 30 字，简明扼要

              【整改优先级建议格式】按风险等级分组，每条建议包含：风险等级 + 时限 + 具体操作 + 预期效果

              ### 整改优先级建议
              1. **🔴立即整改（24小时内）**：重命名Key为`order_timeout_ms`，补全备注说明业务含义、单位、修改影响，防止后续维护困难
              2. **🟡中危跟进（1周内）**：建立团队级敏感配置识别规范，对含`password`/`secret`等关键词的Key启用加密存储
              3. **💡持续优化（本迭代内）**：在Apollo配置提交时启用备注校验，对空备注提交自动拦截，提升配置可追溯性

              格式要求：
              - 仅输出有对应风险等级的整改建议，没有该等级风险则跳过该条
              - 按风险等级从高到低排序
              - 每条建议不超过 80 字，包含具体可执行的操作和预期效果
              - 时限标准：🔴严重（24小时内）、🟠高危（3个工作日内）、🟡中危（1周内）、🔵低危（本迭代内）、💡建议（下迭代计划）
            """;

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildDetectionPrompt(DetectionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请对以下 Apollo 配置项进行全面检测：\n\n");
        prompt.append("## 配置信息\n");
        prompt.append("- **AppId**: ").append(request.getAppId()).append("\n");
        prompt.append("- **环境**: ").append(request.getEnv()).append("\n");
        prompt.append("- **集群**: ").append(request.getClusterName()).append("\n");
        prompt.append("- **Namespace**: ").append(request.getNamespaceName()).append("\n");
        prompt.append("- **Key**: `").append(request.getKey()).append("`\n");
        prompt.append("- **Value**: `").append(request.getValue()).append("`\n");

        if (request.getComment() != null && !request.getComment().isEmpty()) {
            prompt.append("- **备注**: ").append(request.getComment()).append("\n");
        } else {
            prompt.append("- **备注**: 无\n");
        }

        prompt.append("\n---\n\n");
        prompt.append("请严格按照系统提示要求输出检测报告：\n");
        prompt.append("1. 必须输出 3 个检测维度表格（安全性、命名规范、最佳实践）\n");
        prompt.append("2. 每个表格内按风险等级从高到低排序\n");
        prompt.append("3. 最后输出整体风险总结和整改优先级建议\n");

        return prompt.toString();
    }
}
