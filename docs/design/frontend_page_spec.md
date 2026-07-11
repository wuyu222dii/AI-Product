# AI 论文共写工作台 v1 前端页面字段清单与组件树

> 本文档保留 v1.x 页面规格。v2.0 新增项目画像、多文档与章节工作台以 [V2_ACADEMIC_UPGRADE.md](../product/V2_ACADEMIC_UPGRADE.md) 和 `frontend/src/pages/AcademicDocumentsPage.jsx` 为准。

## 1. 文档目标

本文档用于补齐前端实现所需的两类信息：

- `页面字段清单`
  明确每个页面展示什么、编辑什么、触发什么操作
- `组件树`
  明确每个页面的组件拆分结构，方便前端开发、设计对齐与后续复用

默认以 [PRD.md](../product/PRD.md) 为产品基线，以 [api_field_spec.md](../engineering/api_field_spec.md) 为接口基线。

---

## 2. 前端信息架构总览

页面主线：

1. `项目首页`
2. `上传页`
3. `解析状态页`
4. `解析补全页`
5. `材料不足页`
6. `生成中页`
7. `共写工作台主页面`
8. `审查详情抽屉`
9. `申诉复审页`
10. `导出页`
11. `统一错误态`

全局共用能力：

- 顶部导航
- Toast / Notification
- 全局 Loading
- 状态标签组件
- 文件预览组件
- 重试按钮组件
- 空状态组件
- 版本差异组件

---

## 3. 全局共享组件

### 3.1 `AppShell`
职责：
- 提供全局布局
- 承载顶部导航、主内容区、全局提示层

子组件：
- `TopNav`
- `MainContent`
- `GlobalToastLayer`
- `GlobalModalHost`

### 3.2 `StatusBadge`
用于展示：
- 项目状态
- 解析状态
- 生成状态
- 审查影响等级

输入字段：
- `type`
- `label`
- `tone`

### 3.3 `RetryButton`
用于：
- AI 调用失败重试
- 解析失败重试
- 生成失败重试

输入字段：
- `label`
- `loading`
- `disabled`
- `onRetry`

### 3.4 `EmptyState`
用于：
- 项目为空
- 材料为空
- 无审查项
- 无版本记录

输入字段：
- `title`
- `description`
- `primaryAction`
- `secondaryAction`

### 3.5 `FilePreviewCard`
用于预览上传输入：
- 文件名
- 类型
- 解析状态
- 是否关键材料
- 查看详情

输入字段：
- `filename`
- `fileType`
- `parseStage`
- `isKeyMaterial`
- `confidenceScore`

---

## 4. 页面字段清单与组件树

## P1 项目首页

### 页面目标
让用户查看历史论文项目并创建新项目。

### 页面字段清单

#### 页面级字段
- 页面标题：`我的论文项目`
- 搜索框：按项目名称搜索
- 筛选项：按项目状态筛选
- 新建按钮：`新建论文项目`

#### 项目卡片字段
- `title`
- `status`
- `updatedAt`
- `currentDraftVersionId`
- 最近版本摘要

#### 用户操作
- 创建项目
- 进入项目
- 删除项目
- 归档项目

### 组件树

```text
ProjectListPage
├── PageHeader
│   ├── PageTitle
│   ├── SearchInput
│   ├── StatusFilter
│   └── CreateWorkspaceButton
├── WorkspaceList
│   ├── WorkspaceCard
│   │   ├── WorkspaceTitle
│   │   ├── WorkspaceStatusBadge
│   │   ├── WorkspaceUpdatedAt
│   │   ├── WorkspaceLatestVersionSummary
│   │   └── WorkspaceActions
│   └── WorkspaceCard...
└── EmptyState
```

---

## P2 上传页

### 页面目标
接收用户上传的待解析写作输入。

### 页面字段清单

#### 页面级字段
- 页面标题：`上传写作输入`
- 上传说明文案
- 支持格式说明

#### 上传区字段
- 文件拖拽区域
- 文件选择按钮
- 粘贴文本输入框
- 外部链接 / DOI 输入框
- 是否标记为关键材料
- 来源类型选择

#### 上传队列字段
- `filename`
- `fileType`
- 上传进度
- 是否关键材料
- 上传结果

#### 用户操作
- 选择文件
- 批量上传
- 删除待上传项
- 补传
- 提交进入解析

### 组件树

```text
UploadPage
├── PageHeader
│   ├── PageTitle
│   └── HelpText
├── UploadPanel
│   ├── DragDropZone
│   ├── FilePickerButton
│   ├── PasteTextInput
│   ├── ExternalLinkInput
│   ├── SourceTypeSelect
│   ├── KeyMaterialCheckbox
│   └── SubmitUploadButton
├── UploadQueueList
│   ├── UploadQueueItem
│   │   ├── FileIcon
│   │   ├── FileName
│   │   ├── FileType
│   │   ├── UploadProgressBar
│   │   ├── KeyMaterialBadge
│   │   └── RemoveButton
│   └── UploadQueueItem...
└── SupportedFormatsHint
```

---

## P3 解析状态页

### 页面目标
展示本地预处理和 AI 语义解析状态。

### 页面字段清单

#### 页面级字段
- 页面标题：`正在解析输入内容`
- 当前总进度
- 当前阶段说明

#### 文件级字段
- `filename`
- `fileType`
- `parseStage`
- `confidenceScore`
- 是否关键材料

#### 状态提示
- 本地预处理中
- AI 解析中
- AI 部分解析
- AI 解析失败

#### 用户操作
- 查看失败原因
- 重试解析
- 进入补全页

### 组件树

```text
ParsingStatusPage
├── PageHeader
│   ├── PageTitle
│   ├── OverallProgress
│   └── CurrentStageDescription
├── ParsingStatusList
│   ├── ParsingStatusItem
│   │   ├── FileName
│   │   ├── FileType
│   │   ├── ParseStageBadge
│   │   ├── ConfidenceIndicator
│   │   ├── KeyMaterialBadge
│   │   └── ItemActions
│   └── ParsingStatusItem...
└── RetrySection
```

---

## P4 解析补全页

### 页面目标
当 AI 无法完整理解关键材料时，让用户补缺口。

### 页面字段清单

#### 页面级字段
- 页面标题：`补充解析缺口`
- 说明文案：仅补缺口，不必重传全部内容

#### 缺口项字段
- 对应材料名称
- 页码 / 段落定位
- 缺口类型
- AI 当前理解摘要
- 待补说明

#### 用户输入字段
- 手动补充文本框
- 重传清晰文件按钮
- 原文件定位标注区

#### 用户操作
- 保存补充
- 提交重新解析

### 组件树

```text
ParseSupplementPage
├── PageHeader
│   ├── PageTitle
│   └── HelpText
├── GapList
│   ├── GapCard
│   │   ├── MaterialName
│   │   ├── GapLocation
│   │   ├── GapTypeBadge
│   │   ├── CurrentAISummary
│   │   ├── SupplementTextarea
│   │   ├── ReuploadButton
│   │   └── SaveAndReparseButton
│   └── GapCard...
└── SidePreviewPanel
```

---

## P5 材料不足页

### 页面目标
在材料不满足生成条件时，明确阻断并指导补充。

### 页面字段清单

#### 页面级字段
- 页面标题：`当前材料不足，暂时无法生成`
- 原因说明

#### 缺失项字段
- 缺失类型
- 缺失说明
- 建议补充内容
- 建议补充数量

#### 用户操作
- 返回上传页
- 继续补传

### 组件树

```text
InsufficientMaterialPage
├── PageHeader
│   ├── PageTitle
│   └── ReasonText
├── MissingItemList
│   ├── MissingItemCard
│   │   ├── MissingType
│   │   ├── MissingDescription
│   │   ├── SuggestedSupplement
│   │   └── SuggestedCount
│   └── MissingItemCard...
└── PageActions
    ├── BackToUploadButton
    └── ContinueUploadButton
```

---

## P6 生成中页

### 页面目标
展示草稿生成进度和生成阶段。

### 页面字段清单

#### 页面级字段
- 页面标题：`AI 正在生成初稿`
- 当前阶段
- 总体进度

#### 子任务字段
- 题目建议生成状态
- 全文框架生成状态
- 段落骨架生成状态
- 初稿生成状态

#### 用户操作
- 等待
- 取消并返回

### 组件树

```text
DraftGeneratingPage
├── PageHeader
│   ├── PageTitle
│   ├── OverallProgress
│   └── CurrentStepText
├── GenerationStepList
│   ├── GenerationStepItem
│   │   ├── StepName
│   │   ├── StepStatusBadge
│   │   └── StepProgress
│   └── GenerationStepItem...
└── CancelButton
```

---

## P7 共写工作台主页面

### 页面目标
作为核心工作区，承载正文编辑、材料可信链、AI 共写预览、审查复查、版本切换和导出入口。

### 页面字段清单

#### 顶部区域字段
- 项目标题
- 当前版本号
- Requirement Snapshot 摘要
- 当前生成状态
- 导出按钮

#### 左侧推荐任务与审查区字段
- 推荐任务列表
- 审查项分组
- 审查影响等级
- 是否已处理

#### 中间正文编辑区字段
- 当前标题建议
- 正文全文
- 内联来源提示
- 段落定位
- 文本选区信息
- 可信链覆盖率
- 引用一致性状态
- 段落级证据卡
- 原始材料预览入口
- 引用插入按钮

#### 右侧 AI 共写面板字段
- 快捷操作按钮
- 自定义指令输入框
- 最近一次共写结果说明
- 共写预览抽屉
- 冲突提示
- 逐段接受列表
- 应用后复查建议

#### 底部版本记录字段
- 版本号
- 版本创建时间
- 创建来源
- 版本摘要

#### 用户操作
- 直接编辑正文
- 选中一段发起改写
- 调用 AI 补证据 / 调结构 / 压重复 / 改表达
- 查看可信链覆盖率与引用一致性
- 打开证据对应的原始材料预览
- 生成共写预览
- 按段落接受 AI 修改
- 应用共写预览后复查相关审查项
- 切换版本
- 查看差异
- 导出

### 组件树

```text
CoWritingWorkspacePage
├── WorkspaceHeader
│   ├── WorkspaceTitle
│   ├── CurrentVersionBadge
│   ├── RequirementSnapshotSummary
│   ├── GenerationStatusBadge
│   └── ExportButton
├── WorkspaceBody
│   ├── LeftSidebar
│   │   ├── RecommendedTaskPanel
│   │   │   ├── TaskGroup
│   │   │   └── TaskItem
│   │   └── ReviewPanel
│   │       ├── ReviewGroup
│   │       └── ReviewItemPreview
│   ├── EditorMain
│   │   ├── TitleSuggestionBar
│   │   ├── RichTextEditor
│   │   ├── InlineCitationHint
│   │   ├── SelectionToolbar
│   │   └── EvidenceTrustMap
│   │       ├── EvidenceCoverageCard
│   │       ├── CitationConsistencyCard
│   │       └── EvidenceParagraphCard
│   │           ├── SourceExcerpt
│   │           ├── SourceLocation
│   │           ├── MaterialPreviewButton
│   │           └── InsertCitationButton
│   └── RightSidebar
│       ├── AIActionPanel
│       │   ├── RewriteButton
│       │   ├── AddEvidenceButton
│       │   ├── AdjustStructureButton
│       │   ├── ReduceRepetitionButton
│       │   ├── ImproveExpressionButton
│       │   ├── PromptTextarea
│       │   └── SubmitPromptButton
│       └── AILatestResultCard
├── CoWritePreviewDrawer
│   ├── DiffSummary
│   ├── ConflictWarningList
│   ├── ParagraphAcceptList
│   ├── RelatedReviewHint
│   └── ApplyActions
└── VersionHistoryPanel
    ├── VersionList
    │   └── VersionItem
    └── VersionDiffViewer
```

---

## P8 审查详情抽屉

### 页面目标
展示审查项详情并允许用户对单条结果作出处理。

### 页面字段清单

#### 审查项字段
- 审查类型
- 影响等级
- 目标范围
- 说明文案
- 建议修正内容
- 是否可跳过

#### 用户操作
- 接受建议
- 暂时忽略
- 稍后处理
- 发起申诉
- 带风险继续

### 组件树

```text
ReviewDetailDrawer
├── DrawerHeader
│   ├── ReviewTypeTitle
│   └── ReviewImpactBadge
├── ReviewTargetRangeCard
├── ReviewMessageBlock
├── SuggestedFixBlock
└── DrawerActions
    ├── AcceptButton
    ├── IgnoreButton
    ├── DeferButton
    ├── AppealButton
    └── ProceedWithRiskButton
```

---

## P9 申诉复审页

### 页面目标
当用户认为 AI 判断有误时，提交申诉并查看复审结果。

### 页面字段清单

#### 原审查信息字段
- 原审查项类型
- 原影响等级
- 原说明文案
- 原目标范围

#### 申诉输入字段
- 用户申诉理由
- 补充证据
- 引用材料选择

#### 复审结果字段
- 复审结论
- 影响等级变化
- 处理建议

#### 用户操作
- 提交申诉
- 带风险继续
- 返回工作台

### 组件树

```text
AppealPage
├── PageHeader
│   ├── PageTitle
│   └── BackButton
├── OriginalReviewCard
│   ├── ReviewType
│   ├── ReviewImpactLevel
│   ├── ReviewMessage
│   └── ReviewTargetRange
├── AppealForm
│   ├── AppealReasonTextarea
│   ├── EvidenceSelector
│   ├── EvidenceNoteTextarea
│   └── SubmitAppealButton
└── AppealResultCard
    ├── OutcomeText
    ├── DowngradedLevelBadge
    └── ProceedWithRiskButton
```

---

## P10 导出页

### 页面目标
让用户导出最终稿，并在导出前确认参考文献、可信链、引用一致性和正文表达风险。

### 页面字段清单

#### 导出参数字段
- 格式：`docx / pdf`
- 是否包含批注
- 导出版本选择

#### 导出状态字段
- 当前导出任务状态
- 下载链接
- 错误信息
- 参考文献数量与元数据风险
- 可信链覆盖率风险
- 引用一致性风险
- 正文表达风险

#### 用户操作
- 发起导出
- 重新导出
- 下载文件
- 查看交付确认清单

### 组件树

```text
ExportPage
├── PageHeader
│   ├── PageTitle
│   └── BackButton
├── ExportOptionsForm
│   ├── FormatSelect
│   ├── IncludeCommentsSwitch
│   ├── VersionSelect
│   └── SubmitExportButton
├── ExportReadinessPanel
│   ├── ReferenceRiskItem
│   ├── EvidenceCoverageRiskItem
│   ├── CitationConsistencyRiskItem
│   └── WritingStyleRiskItem
└── ExportStatusPanel
    ├── ExportStatusBadge
    ├── DownloadLink
    ├── RetryButton
    └── ErrorMessage
```

---

## P11 统一错误态

### 页面目标
在 AI 调用失败、部分生成、任务失败时提供统一且可恢复的反馈。

### 页面字段清单

#### 错误字段
- 错误标题
- 错误说明
- 错误来源模块
- 最近一次成功版本

#### 用户操作
- 重试
- 回到上一步
- 回退到最后成功版本

### 组件树

```text
ErrorStatePanel
├── ErrorIcon
├── ErrorTitle
├── ErrorDescription
├── LastSuccessfulVersionInfo
└── ErrorActions
    ├── RetryButton
    ├── BackButton
    └── RestoreLastVersionButton
```

---

## 5. 组件复用建议

优先复用的组件：

- `StatusBadge`
  用于解析状态、生成状态、审查等级
- `FilePreviewCard`
  用于上传页、解析页、补全页
- `ReviewItemCard`
  用于左侧审查区和抽屉详情入口
- `VersionItem`
  用于版本历史
- `AsyncJobStatus`
  用于解析中、生成中、导出中

---

## 6. 前端状态管理建议

建议至少拆成以下状态域：

- `workspaceState`
  当前项目、当前版本、项目状态
- `materialState`
  上传材料、解析状态、关键材料状态
- `requirementState`
  Requirement Snapshot 与缺失项
- `draftState`
  当前草稿、版本列表、当前编辑文本
- `reviewState`
  推荐任务、审查项、影响等级、处理状态
- `jobState`
  解析任务、生成任务、导出任务状态
- `appealState`
  申诉提交状态、复审结果

---

## 7. 联调重点

前端页面实现时，优先关注以下联调点：

- `parseStage`
  决定解析状态页、补全页、生成拦截逻辑
- `isGenerationEligible`
  决定进入生成还是跳到材料不足页
- `reviewImpactLevel`
  决定侧栏展示与抽屉处理方式
- `generationStatus`
  决定主页面、生成中页、错误态展示

---

## 8. 前端验收重点

- 用户上传后能清楚知道当前在“预处理 / AI 解析 / 生成 / 共写”的哪个阶段
- 材料不足时不会错误进入生成页
- 解析补全时只要求补缺口，不让用户重来一遍
- 共写工作台主页面信息密度高但主次清晰
- 推荐任务与审查区不应压过正文编辑区
- 局部修正应尽量能直接定位到具体段落或句子
- 错误态必须可恢复，不丢用户输入
