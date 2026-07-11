# AI 论文共写工作台 v2.0 产品与开发总规格

> 当前实现以 [V2_ACADEMIC_UPGRADE.md](V2_ACADEMIC_UPGRADE.md) 为 v2.0 事实来源。本文后续大量 v1 页面和流程规格继续保留，用于解释兼容能力与历史设计，不再代表产品只面向本科课程论文。

## 1. 项目定义

### 1.1 产品名称
`AI 论文共写工作台`

### 1.2 产品目标
面向本科生、硕士生、博士生与科研人员，围绕研究项目共享材料和知识库，支持多个学术文档、章节级写作、证据绑定、版本管理、审查与导出。

### 1.3 核心定位
- AI 角色：`研究共创助手 / 共写者`
- 推荐任务与审查：`可选辅助层`
- 不做强制流程闯关
- 材料不足时：`禁止兜底生成`
- 文件内容理解：`必须依赖 AI 语义解析`
- 本地仅做：`技术预处理`
- 章节生成：`只使用当前文档选定材料`
- AI 使用：`保留操作、材料依据、采纳与披露记录`

### 1.4 目标用户
- 本科生
- 硕士生
- 博士生
- 科研人员

### 1.5 非目标
- 不支持无材料情况下通用生成
- 不做本地语义理解
- 不由 AI 替用户做最终学术判断
- v2.0 暂不深做多人协作、投稿全规则库、CSL/RIS/BibTeX 和生产级权限体系

---

## 1.6 v2.0 当前架构

```text
研究项目
-> 学术项目画像
-> 共享材料 / 项目知识库
-> 多个独立学术文档
-> 章节树与文档专属材料范围
-> 章节级生成 / 共写预览 / 版本恢复
-> 动态 readiness / 审查 / 组装 / 导出
```

学术阶段支持 `UNDERGRADUATE / MASTER / DOCTORAL / RESEARCHER`，文档支持课程论文、研究计划、本科毕业论文、硕士论文、博士论文、期刊稿、会议稿、文献综述和研究报告。

要求优先级固定为：

`用户确认的学校/导师/课程/期刊要求 > 文档设置 > 研究范式规则 > 平台默认模板`

研究计划不要求已有结果；系统综述不要求实验数据；定量、实验、定性、计算与设计科学文档根据章节类型要求相应真实研究成果；博士论文增加原创贡献确认。

---

## 2. v1 兼容 MVP 范围

### 2.1 必须实现
- 文件上传与项目创建
- 本地预处理
- AI 语义解析
- 材料充足性检查
- 初稿生成
- 共写迭代
- 推荐任务与审查侧栏
- 申诉 / 复审
- 导出
- AI 调用失败兜底
- 解析补全机制

### 2.2 暂不实现
- 多人协作
- 教师端
- 多项目跨文献图谱
- 自动联网检索复杂学术数据库
- 高级模板市场
- 移动端重度编辑

---

## 3. 核心原则

- `共写优先`：AI 先出框架、初稿、改写版本
- `任务可选`：系统给出推荐步骤，但不强制逐项通关
- `审查可参考`：AI 给提示和修正建议，不充当唯一裁判
- `用户主导`：用户可按系统方式推进，也可按自己的习惯写作
- `材料真实优先`：材料不足时不兜底生成，明确拦截并提示补充
- `语义理解必须由 AI 完成`：本地不做内容含义判断
- `减少返工优先`：会影响内容准确性或引发大范围返工的问题，不能只做轻提示

---

## 4. 交互模型

### 4.1 共写模式（默认）
用户上传材料后，AI 直接输出：
- 题目建议
- 正文框架
- 段落骨架
- 可编辑初稿

用户直接在正文上修改，并继续调用 AI 共写。

### 4.2 引导模式（可选）
系统在侧边栏提供：
- 推荐任务步骤
- 缺口提示
- 审查结果
- 下一步建议

用户可按此推进，但不要求逐项完成。

### 4.3 混合使用
用户可以：
- 先让 AI 出稿，再回头补证据或修结构
- 只采纳部分推荐任务
- 完全按自己的路径写，系统仅在后台给提示

---

## 5. 核心用户流程

1. 用户创建论文项目并上传待解析的写作输入
2. 系统执行本地预处理
3. 系统调用 AI 完成语义解析与分类
4. 系统执行 Requirement Preflight
5. 系统执行 Material Sufficiency Check
6. 若材料不足，阻断生成并输出补充建议
7. 若材料充足，生成题目建议、全文框架、段落骨架、可编辑初稿
8. 用户在共写工作台中直接编辑
9. 用户可调用 AI 执行重写、补证据、调结构、压重复
10. 系统持续输出推荐任务与审查结果
11. 用户可忽略建议、局部修正、申诉或继续生成
12. 用户导出最终稿

---

## 6. 页面规格

### P1 项目首页
功能：
- 新建论文项目
- 查看历史项目
- 删除 / 归档项目

字段：
- 项目名称
- 最近更新时间
- 当前状态
- 最近版本摘要

### P2 上传页
功能：
- 上传待解析的写作输入
- 支持多文件、拖拽、补传、粘贴文本

支持格式：
- `doc/docx`
- `pdf`
- `txt/md`
- `jpg/png/heic/webp`
- `xls/xlsx/csv`
- `ppt/pptx`
- 压缩包
- 链接 / DOI

### P3 解析中页
展示：
- 本地预处理进度
- AI 语义解析进度
- 当前解析文件数
- 关键材料状态

### P4 解析补全页
触发条件：
- `ai_partial`
- `ai_failed`
- 关键字段低置信度

功能：
- 查看缺口
- 手动补文字
- 重传清晰文件
- 原文件定位补充

### P5 材料不足页
触发条件：
- `blocked_insufficient_material`

展示：
- 无法生成说明
- 缺失项
- 建议补充内容
- 建议补充数量
- 返回上传入口

### P6 生成中页
展示：
- 题目建议生成中
- 框架生成中
- 初稿生成中
- 部分生成状态

### P7 共写工作台主页面
布局：
- 左侧：推荐任务与审查区
- 中间：正文编辑区
- 右侧：AI 共写面板
- 下方：版本记录

中间编辑区功能：
- 直接编辑正文
- 段落选中操作
- 内联引用提示
- 差异对比

右侧 AI 面板功能：
- 生成初稿
- 改写选中内容
- 补证据
- 调结构
- 压重复
- 改表达

### P8 审查详情抽屉
展示分类：
- `仅提示`
- `局部修正`
- `必须确认`

### P9 申诉复审页
功能：
- 提交误判理由
- 引用证据
- 查看复审结果
- 带风险继续

### P10 导出页
功能：
- 导出 docx
- 导出 pdf
- 导出带批注版本

### P11 错误提示组件
用于：
- AI 调用失败
- 生成中断
- 重试入口
- 回退到最后成功版本

---

## 7. 上传、解析与材料理解

### 7.1 用户上传内容定义
用户上传的是：`待解析的写作输入`

系统会先进行解析、分类和理解，再决定它是否以及如何参与正文框架、段落内容和引用生成。

### 7.2 上传方式
- 拖拽多文件
- 批量选择
- 粘贴文本
- 上传整包
- 先传后补
- 单次多文件上传

### 7.3 本地预处理职责
仅允许处理：
- 文件读取
- 格式识别
- 页数拆分
- 基础 OCR
- 文本抽取
- 图片 / 表格切片

### 7.4 AI 语义解析职责
必须由 AI 完成：
- 材料类型判断
- 与论文主题相关性判断
- 是否属于老师要求 / 研究成果 / 草稿 / 参考资料
- 图表语义理解
- 论点支撑关系识别
- 文本段落用途识别

### 7.5 资料来源
采用：
`平台内已接入资料检索（包括用户上传资料检索与已接入公开资料检索） + 用户补传`

---

## 8. 解析补全机制

### 8.1 解析状态
- `preprocessed`
- `ai_parsed`
- `ai_partial`
- `ai_failed`

### 8.2 原则
- 可继续处理已解析部分
- 缺失部分单独标出来
- 不让整份材料卡死
- 若缺失部分属于关键材料，则不得进入正文生成

### 8.3 向用户追问方式
- 只问缺口，不要求整份重传
- 只追问必要片段、页码、图片内容、模糊句子
- 支持手动输入、重传清晰版、原文件标注补充

### 8.4 约束
- 低置信度内容必须显式标记
- 待确认内容不能当成事实直接写进正文
- AI 解析失败时，不能用本地规则替代语义理解

---

## 9. 材料充足性检查

### 9.1 目的
在生成正文框架或初稿前，判断当前材料是否足以支撑生成。

### 9.2 材料不足时原则
- `禁止生成`
- 不做通用知识兜底
- 不写泛化正文
- 不硬凑框架

### 9.3 系统输出
系统必须明确说明：
- 为什么当前材料不足
- 具体缺什么
- 建议补充什么
- 建议补充多少

### 9.4 缺失项示例
- 缺老师要求
- 缺核心研究成果
- 缺可引用文献
- 缺关键数据 / 图表 / 案例
- 缺想重点论证的核心观点

### 9.5 数量建议
根据以下因素动态给出：
- 论文类型
- 字数要求
- 老师要求
- 当前已有材料量

### 9.6 生成状态
新增：
- `blocked_insufficient_material`

---

## 10. 要求处理

### 10.1 Requirement Preflight
最小关键字段：
- 题目
- 字数
- 截止时间
- 引用格式

### 10.2 仅在以下情况要求用户确认
- 老师要求缺失
- 多份要求冲突
- OCR 低置信度
- 新上传要求改变原基准

### 10.3 输出
- `RequirementSnapshot`
- 作为后台规则基准

---

## 11. 推荐任务与审查机制

### 11.1 推荐任务
系统可持续给出：
- 检查作业要求是否完整
- 补充缺失来源
- 调整提纲逻辑
- 修正正文重复
- 完成定稿前检查

### 11.2 审查结果分级
- `仅提示`
- `局部修正`
- `必须确认`

### 11.3 分级含义
- `仅提示`
  - 不影响当前内容准确性
  - 只是优化建议
  - 不打断流程

- `局部修正`
  - 影响局部表达、局部引用或局部逻辑
  - 不需要整篇返工
  - 应尽量定位到段落、句子或引用点

- `必须确认`
  - 影响正文正确性
  - 影响老师要求基准
  - 影响关键事实、关键来源或整体结构方向
  - 必须处理后才能可靠继续

### 11.4 用户决策
用户可：
- 接受建议
- 暂时忽略
- 稍后处理
- 标记误判并申诉
- 带风险继续

---

## 12. 共写与内容生成

### 12.1 初稿生成内容
- 题目建议
- 全文框架
- 段落骨架
- 可编辑初稿

### 12.2 共写操作
- 重写
- 补证据
- 调结构
- 压重复
- 改表达

### 12.3 生成前置条件
必须同时满足：
- 关键材料 `ai_parsed`
- Requirement Snapshot 可用
- Material Sufficiency Check = `eligible`

否则禁止生成初稿。

---

## 13. 来源绑定与重复控制

### 13.1 来源绑定
AI 在后台完成：
- `Source Intake`
- `Evidence Mapping`

自动把：
- 论点
- 段落
- 来源

绑定起来。

若关键论点无来源，按影响范围标为：
- `局部修正`
- 或 `必须确认`

### 13.2 重复控制原则
减少 `信息重复`，保留合理的 `结构性重复`

允许：
- 引言点题
- 小结回顾
- 结论总结
- 过渡承接

拦截：
- 同一观点反复展开
- 同一证据重复使用但无新分析
- 只换句式不增加信息

---

## 14. AIGC 味控制

目标不是躲检测，而是降低模板化、空话化和过度平滑的 AI 味。

默认机制：
- `来源驱动`
- `用户驱动`
- `过程驱动`

具体策略：
- 每段只讲一个新信息
- 优先用具体证据、具体术语、具体材料
- 保留适度学术不确定性和边界感
- 避免全篇使用同一类万能句式
- 用户可在定稿前做最后一轮“人味校正”

---

## 15. AI 调用失败兜底

### 15.1 规则
- 不本地硬补写
- 不伪装成正常结果
- 保留当前输入内容
- 保留最后一次成功版本
- 提供重试

### 15.2 提示文案
- `AI 暂时不可用，请稍后重试`
- `本次生成未完成，请重试`

### 15.3 部分生成
若只完成部分输出，显式标记：
- `部分生成`

---

## 16. 申诉与复审

### 16.1 原则
AI 评审不是终审。

### 16.2 用户可执行
- 对审查结果发起 `申诉 / 标记误判`

### 16.3 复审
复审时重新读取：
- `RequirementSnapshot`
- 相关证据

### 16.4 复审结果
- `维持建议`
- `降低影响等级`
- `撤销判断`

### 16.5 最终控制权
- 最终保留用户确认权

---

## 17. 数据对象

### 17.1 `WritingWorkspace`
- `id`
- `user_id`
- `title`
- `status`
- `current_draft_version_id`
- `requirement_snapshot_id`
- `created_at`
- `updated_at`

### 17.2 `MaterialItem`
- `id`
- `workspace_id`
- `filename`
- `file_type`
- `source_type`
- `raw_file_url`
- `is_key_material`
- `parse_stage`
- `confidence_score`
- `created_at`

### 17.3 `PreprocessedContent`
- `id`
- `material_id`
- `plain_text`
- `ocr_text`
- `page_map_json`
- `image_slices_json`
- `created_at`

### 17.4 `AISemanticParseResult`
- `id`
- `material_id`
- `material_category`
- `summary`
- `topic_relation`
- `detected_claims_json`
- `detected_evidence_json`
- `detected_requirements_json`
- `confidence_score`
- `created_at`

### 17.5 `RequirementSnapshot`
- `id`
- `workspace_id`
- `topic`
- `word_count`
- `deadline`
- `citation_style`
- `special_requirements_json`
- `version`
- `created_at`

### 17.6 `MaterialSufficiencyResult`
- `id`
- `workspace_id`
- `is_generation_eligible`
- `missing_items_json`
- `recommended_supplements_json`
- `reason`
- `created_at`

### 17.7 `DraftVersion`
- `id`
- `workspace_id`
- `version_no`
- `title_suggestion`
- `outline_json`
- `paragraph_skeletons_json`
- `draft_text`
- `source_trace_map_json`
- `created_by`
- `created_at`

### 17.8 `EvidenceBinding`
- `id`
- `draft_version_id`
- `claim_text`
- `material_id`
- `source_excerpt`
- `target_range_json`
- `confidence_score`

### 17.9 `ReviewItem`
- `id`
- `workspace_id`
- `draft_version_id`
- `review_type`
- `review_impact_level`
- `target_range_json`
- `message`
- `suggested_fix`
- `can_bypass`
- `created_at`

### 17.10 `AppealCase`
- `id`
- `review_item_id`
- `user_reason`
- `evidence_json`
- `review_outcome`
- `resolved_at`
- `created_at`

### 17.11 `GenerationJob`
- `id`
- `workspace_id`
- `job_type`
- `status`
- `input_ref_json`
- `output_ref_json`
- `error_message`
- `created_at`
- `updated_at`

### 17.12 `UserAction`
- `id`
- `workspace_id`
- `draft_version_id`
- `action_type`
- `payload_json`
- `created_at`

---

## 18. 状态定义

### 18.1 材料解析状态
- `preprocessed`
- `ai_parsed`
- `ai_partial`
- `ai_failed`

### 18.2 生成状态
- `success`
- `partial`
- `failed`
- `blocked_insufficient_material`

### 18.3 审查影响等级
- `notice`
- `local_fix`
- `must_confirm`

### 18.4 用户决策
- `accept`
- `ignore`
- `defer`
- `appeal`
- `proceed_with_risk`

### 18.5 任务状态
- `queued`
- `running`
- `success`
- `partial`
- `failed`

---

## 19. API 设计

### 19.1 项目
- `POST /workspaces`
- `GET /workspaces`
- `GET /workspaces/{id}`
- `DELETE /workspaces/{id}`

### 19.2 上传与解析
- `POST /workspaces/{id}/materials`
- `GET /workspaces/{id}/materials`
- `POST /materials/{id}/preprocess`
- `POST /materials/{id}/ai-parse`
- `POST /materials/{id}/supplement`

### 19.3 要求与材料检查
- `POST /workspaces/{id}/requirement-snapshot`
- `GET /workspaces/{id}/requirement-snapshot`
- `POST /workspaces/{id}/material-sufficiency-check`

### 19.4 生成与共写
- `POST /workspaces/{id}/generate-draft`
- `GET /workspaces/{id}/drafts`
- `GET /drafts/{id}`
- `PATCH /drafts/{id}`
- `POST /drafts/{id}/restore`
- `POST /workspaces/{id}/co-write`
- `POST /workspaces/{id}/co-write/preview`
- `POST /co-write-previews/{id}/apply`
- `POST /co-write-previews/{id}/discard`

### 19.5 审查与申诉
- `GET /drafts/{id}/review-items`
- `PATCH /review-items/{id}/status`
- `POST /review-items/{id}/appeal`
- `POST /review-items/{id}/recheck`
- `GET /appeals/{id}`

### 19.6 材料可信链与知识库
- `GET /drafts/{id}/evidence-bindings`
- `POST /drafts/{id}/evidence-bindings/rebuild`
- `PATCH /evidence-bindings/{id}/status`
- `POST /workspaces/{id}/knowledge-base/build`
- `GET /workspaces/{id}/knowledge-base/chunks`
- `POST /workspaces/{id}/knowledge-base/search`

### 19.7 导出与任务
- `POST /drafts/{id}/export`
- `GET /exports/{jobId}/download`
- `GET /jobs/{id}`

---

## 20. AI 编排

### 20.1 AI 任务类型
- `semantic_parse`
- `requirement_extract`
- `material_sufficiency_eval`
- `draft_generate`
- `outline_refine`
- `evidence_bind`
- `review_pass`
- `appeal_recheck`

### 20.2 调用顺序
1. `semantic_parse`
2. `requirement_extract`
3. `material_sufficiency_eval`
4. `draft_generate`
5. 用户触发后进入 `outline_refine / evidence_bind / review_pass`
6. 争议时进入 `appeal_recheck`

### 20.3 幂等要求
- 同一输入同一阶段重复调用可重试
- 生成失败不污染已有版本
- 新版本必须独立保存

---

## 21. 非功能要求

### 性能
- 小文件上传后应尽快进入解析
- 初稿生成应有可见进度反馈
- 所有长任务需异步化

### 可恢复性
- 任意 AI 调用失败后可重试
- 版本不可丢失
- 用户输入不可丢失

### 可追溯性
- 每个版本可追溯来源材料
- 每个审查项可追溯到目标段落
- 每个申诉可追溯原始判定

### 安全
- 用户文件隔离
- 原始文件受权限控制
- 导出文件仅用户本人可访问

---

## 22. 数据库设计建议

### 22.1 关键关系
- `workspace 1:n materials`
- `material 1:1 preprocessed_contents`
- `material 1:1 ai_semantic_parse_results`
- `workspace 1:n requirement_snapshots`
- `workspace 1:n draft_versions`
- `draft_version 1:n review_items`
- `draft_version 1:n evidence_bindings`
- `review_item 1:n appeal_cases`

### 22.2 关键索引
- `materials(workspace_id, parse_stage)`
- `draft_versions(workspace_id, version_no desc)`
- `review_items(draft_version_id, review_impact_level)`
- `generation_jobs(workspace_id, status)`

### 22.3 技术假设
- `PostgreSQL + 对象存储 + 异步任务队列`

---

## 23. 前端任务清单

1. 项目首页
2. 上传页
3. 解析状态页
4. 材料不足页
5. 共写工作台
6. 审查交互
7. 申诉复审
8. 导出页
9. 统一错误态

---

## 24. 后端任务清单

1. 文件上传服务
2. 本地预处理服务
3. AI 语义解析服务
4. 材料充足性检查服务
5. 草稿生成服务
6. 共写迭代服务
7. 审查服务
8. 申诉复审服务
9. 导出服务
10. 任务编排与状态机

---

## 25. MVP 验收标准

- 用户可上传多种写作输入并成功创建项目
- 系统可完成本地预处理与 AI 语义解析
- 材料不足时系统明确阻断生成并给出补充建议
- 材料充足时系统可生成题目建议、框架、段落骨架和初稿
- 用户可在工作台中直接编辑并调用 AI 共写
- 系统能输出推荐任务与审查项
- 用户可发起申诉并收到复审结果
- AI 调用失败时系统能明确报错并支持重试
- 用户可导出定稿

---

## 26. 推荐开发顺序

1. 上传 + `materials`
2. 预处理 + `preprocessed_contents`
3. AI 解析 + `ai_semantic_parse_results`
4. Requirement Snapshot
5. 材料充足性检查
6. 初稿生成 + `draft_versions`
7. 共写工作台
8. 审查与推荐任务
9. 申诉复审
10. 导出
11. 错误态与任务编排收尾

---

## 27. v1 历史默认前提

- v1 首版目标用户为 `本科课程论文`；v2.0 已升级为本科、硕士、博士与科研人员
- 支持常见学术格式和多文件上传
- 产品核心是 `共写创作 + 可选流程辅助`
- 用户最重要的需求是：`上传材料后，AI 先把正文框架写出来`
- 必要任务保留，但只作为推荐性脚手架，不做强制门槛
- 审查结果默认是辅助建议，不是唯一裁决
- 会影响准确性或引发大范围返工的问题，应升级为 `必须确认`
- 材料不足时，系统优先拦截生成并引导补充，而不是兜底写作
- 文件内容理解必须依赖 `AI 语义解析`，本地只做技术预处理
