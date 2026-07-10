import { useEffect, useMemo, useRef, useState } from "react";
import { api } from "../services/api";

export function UploadPage({ workspace, onContinue, onError }) {
  const [plainText, setPlainText] = useState("");
  const [externalLink, setExternalLink] = useState("");
  const [textIsKey, setTextIsKey] = useState(true);
  const [linkIsKey, setLinkIsKey] = useState(false);
  const [pendingFiles, setPendingFiles] = useState([]);
  const [draggingFiles, setDraggingFiles] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [uploadSummary, setUploadSummary] = useState(null);
  const [literatureCandidates, setLiteratureCandidates] = useState([]);
  const [selectedCandidateId, setSelectedCandidateId] = useState("");
  const fileInputRef = useRef(null);
  const dragDepthRef = useRef(0);

  useEffect(() => {
    if (!workspace?.id) return;
    let cancelled = false;
    async function loadCandidates() {
      try {
        const response = await api.listLiteratureCandidates(workspace.id);
        if (!cancelled) {
          setLiteratureCandidates(Array.isArray(response) ? response : []);
        }
      } catch {
        if (!cancelled) {
          setLiteratureCandidates([]);
        }
      }
    }
    loadCandidates();
    return () => {
      cancelled = true;
    };
  }, [workspace?.id]);

  const totalPendingCount = useMemo(() => {
    let count = pendingFiles.length;
    if (plainText.trim()) count += 1;
    if (externalLink.trim()) count += 1;
    return count;
  }, [pendingFiles, plainText, externalLink]);
  const keyMaterialCount = useMemo(() => {
    let count = pendingFiles.filter((item) => item.isKeyMaterial).length;
    if (plainText.trim() && textIsKey) count += 1;
    if (externalLink.trim() && linkIsKey) count += 1;
    return count;
  }, [pendingFiles, plainText, textIsKey, externalLink, linkIsKey]);
  const hasAnyInput = totalPendingCount > 0;

  function handleFilesSelected(event) {
    const selected = Array.from(event.target.files ?? []);
    appendFiles(selected);
    event.target.value = "";
  }

  function appendFiles(files) {
    if (files.length === 0) return;
    const next = files.map((file) => ({
      localId: crypto.randomUUID(),
      file,
      isKeyMaterial: /\.(pdf|docx|png|jpg|jpeg|webp|heic|xlsx|csv|pptx|zip)$/i.test(file.name),
      status: "pending",
      error: ""
    }));
    setPendingFiles((current) => [...current, ...next]);
  }

  function handleDragEnter(event) {
    event.preventDefault();
    event.stopPropagation();
    dragDepthRef.current += 1;
    if (hasDraggedFiles(event)) {
      setDraggingFiles(true);
    }
  }

  function handleDragOver(event) {
    event.preventDefault();
    event.stopPropagation();
    if (hasDraggedFiles(event)) {
      event.dataTransfer.dropEffect = "copy";
      setDraggingFiles(true);
    }
  }

  function handleDragLeave(event) {
    event.preventDefault();
    event.stopPropagation();
    dragDepthRef.current = Math.max(0, dragDepthRef.current - 1);
    if (dragDepthRef.current === 0) {
      setDraggingFiles(false);
    }
  }

  function handleDrop(event) {
    event.preventDefault();
    event.stopPropagation();
    dragDepthRef.current = 0;
    setDraggingFiles(false);

    const dropped = Array.from(event.dataTransfer.files ?? []);
    if (dropped.length === 0) {
      onError("没有读取到可上传文件，请重新拖入文件或使用选择文件。");
      return;
    }
    appendFiles(dropped);
  }

  function updateFile(localId, updater) {
    setPendingFiles((current) =>
      current.map((item) => (item.localId === localId ? updater(item) : item))
    );
  }

  function removeFile(localId) {
    setPendingFiles((current) => current.filter((item) => item.localId !== localId));
  }

  async function uploadPendingFile(item, literatureCandidateId = "") {
    updateFile(item.localId, (current) => ({ ...current, status: "uploading", error: "" }));
    const formData = new FormData();
    formData.append("files", item.file);
    formData.append("sourceType", "upload");
    formData.append("isKeyMaterial", String(item.isKeyMaterial));
    if (literatureCandidateId) {
      formData.append("literatureCandidateId", literatureCandidateId);
    }
    try {
      const result = await api.uploadMaterials(workspace.id, formData);
      updateFile(item.localId, (current) => ({
        ...current,
        status: "success",
        uploadedMaterialId: result.items?.[0]?.id ?? null
      }));
      return true;
    } catch (error) {
      updateFile(item.localId, (current) => ({
        ...current,
        status: "error",
        error: error.message
      }));
      return false;
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!workspace?.id) {
      onError("请先创建项目");
      return;
    }
    if (totalPendingCount === 0) {
      onError("请至少提供一项写作输入");
      return;
    }
    try {
      setSubmitting(true);
      setUploadSummary(null);

      let successCount = 0;
      let failedCount = 0;
      let candidateIdForNextUpload = selectedCandidateId;

      function appendCandidateOnce(formData) {
        if (!candidateIdForNextUpload) return;
        formData.append("literatureCandidateId", candidateIdForNextUpload);
        candidateIdForNextUpload = "";
      }

      if (plainText.trim()) {
        const formData = new FormData();
        formData.append("plainText", plainText.trim());
        formData.append("sourceType", "pasted_text");
        formData.append("isKeyMaterial", String(textIsKey));
        appendCandidateOnce(formData);
        try {
          await api.uploadMaterials(workspace.id, formData);
          successCount += 1;
        } catch (error) {
          failedCount += 1;
          onError(error.message);
        }
      }

      if (externalLink.trim()) {
        const formData = new FormData();
        formData.append("externalLink", externalLink.trim());
        formData.append("sourceType", "external_link");
        formData.append("isKeyMaterial", String(linkIsKey));
        appendCandidateOnce(formData);
        try {
          await api.uploadMaterials(workspace.id, formData);
          successCount += 1;
        } catch (error) {
          failedCount += 1;
          onError(error.message);
        }
      }

      for (const item of pendingFiles) {
        const success = await uploadPendingFile(item, candidateIdForNextUpload);
        candidateIdForNextUpload = "";
        if (success) {
          successCount += 1;
        } else {
          failedCount += 1;
        }
      }

      setUploadSummary({ successCount, failedCount });
      if (successCount > 0) {
        onContinue();
      }
    } catch (error) {
      onError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="page-card upload-workbench">
      <div className="upload-page-head">
        <div>
          <span className="eyebrow">Step 1 · 写作输入</span>
          <h3 className="page-section-title">把会影响论文生成的资料放进来</h3>
          <p className="section-help">
            可以只上传一种，也可以混合上传。提交后系统会自动做文字抽取、OCR、AI 语义解析和解析质量检查。
          </p>
        </div>
        <div className="upload-next-card">
          <strong>下一步</strong>
          <span>进入解析质量检查</span>
        </div>
      </div>

      <div className="upload-step-strip" aria-label="上传流程">
        <div className="upload-step active">
          <span>1</span>
          <strong>选择输入</strong>
          <small>文件 / 文本 / 链接任选</small>
        </div>
        <div className="upload-step">
          <span>2</span>
          <strong>标记关键依据</strong>
          <small>影响后续生成权重</small>
        </div>
        <div className="upload-step">
          <span>3</span>
          <strong>提交解析</strong>
          <small>自动进入质量清单</small>
        </div>
      </div>

      <form className="upload-flow" onSubmit={handleSubmit}>
        <div className="upload-source-grid">
          <article className="upload-source-card upload-source-card--file">
            <div className="upload-source-head">
              <span className="source-index">A</span>
              <div>
                <h4>上传文件</h4>
                <p>适合论文 PDF、Word、图片、表格、PPT、压缩包等。</p>
              </div>
            </div>
            <div
              className={`upload-dropzone ${draggingFiles ? "is-dragging" : ""}`}
              role="button"
              tabIndex={0}
              onClick={() => fileInputRef.current?.click()}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  fileInputRef.current?.click();
                }
              }}
              onDragEnter={handleDragEnter}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
            >
              <strong>{draggingFiles ? "松开即可加入文件队列" : "拖入文件，或点击选择"}</strong>
              <p className="muted">支持 PDF / DOCX / TXT / Markdown / 图片 / XLSX / CSV / PPTX / ZIP。</p>
              <button type="button" className="secondary-btn">
                选择文件
              </button>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                hidden
                onChange={handleFilesSelected}
                accept=".pdf,.docx,.txt,.md,.png,.jpg,.jpeg,.webp,.heic,.gif,.xlsx,.csv,.ppt,.pptx,.zip"
              />
            </div>
          </article>

          <article className="upload-source-card">
            <div className="upload-source-head">
              <span className="source-index">B</span>
              <div>
                <h4>粘贴文字</h4>
                <p>适合作业要求、研究笔记、已有草稿或老师口头要求整理。</p>
              </div>
            </div>
            <div className="field">
              <label>文本内容</label>
              <textarea
                value={plainText}
                onChange={(event) => setPlainText(event.target.value)}
                placeholder="例如：课程论文要求、研究结论、问卷结果、已有草稿片段..."
              />
            </div>
            <label className="key-toggle">
              <input
                type="checkbox"
                checked={textIsKey}
                onChange={(event) => setTextIsKey(event.target.checked)}
              />
              <span>
                <strong>作为生成正文的重要依据</strong>
                <small>建议作业要求、研究成果、核心数据勾选</small>
              </span>
            </label>
          </article>

          <article className="upload-source-card">
            <div className="upload-source-head">
              <span className="source-index">C</span>
              <div>
                <h4>添加链接 / DOI</h4>
                <p>适合在线文章、数据来源、参考文献链接或 DOI。</p>
              </div>
            </div>
            <div className="field">
              <label>外部链接 / DOI</label>
              <input
                value={externalLink}
                onChange={(event) => setExternalLink(event.target.value)}
                placeholder="https://... 或 DOI"
              />
            </div>
            <label className="key-toggle">
              <input
                type="checkbox"
                checked={linkIsKey}
                onChange={(event) => setLinkIsKey(event.target.checked)}
              />
              <span>
                <strong>作为关键来源处理</strong>
                <small>如果链接会支撑正文观点，建议勾选</small>
              </span>
            </label>
          </article>
        </div>

        <div className="upload-bottom-grid">
          <section className="upload-queue-panel">
            <div className="queue-header">
              <div>
                <h4>文件队列</h4>
                <p className="muted">文件会逐个上传，失败项会保留在这里方便重试。</p>
              </div>
              <span className="meta-chip">共 {pendingFiles.length} 个文件</span>
            </div>
            {pendingFiles.length === 0 ? (
              <div className="empty-upload-card">
                <strong>还没有文件</strong>
                <p className="muted">如果暂时只有文字，也可以直接提交文本进入解析。</p>
              </div>
            ) : (
              <div className="list-stack">
                {pendingFiles.map((item) => (
                  <div className="upload-file-row" key={item.localId}>
                    <div className="upload-file-main">
                      <strong>{item.file.name}</strong>
                      <p className="muted">
                        {item.file.name.split(".").pop()?.toLowerCase() || "unknown"} · {formatBytes(item.file.size)}
                      </p>
                    </div>
                    <label className="inline-check">
                      <input
                        type="checkbox"
                        checked={item.isKeyMaterial}
                        onChange={(event) =>
                          updateFile(item.localId, (current) => ({
                            ...current,
                            isKeyMaterial: event.target.checked
                          }))
                        }
                      />
                      重要依据
                    </label>
                    <span className={`status-pill ${item.status}`}>{renderFileStatus(item.status)}</span>
                    <button type="button" className="ghost-btn" onClick={() => removeFile(item.localId)}>
                      移除
                    </button>
                    {item.error && <p className="review-fix">上传失败：{item.error}</p>}
                  </div>
                ))}
              </div>
            )}
          </section>

          <aside className="upload-submit-panel">
            <span className="eyebrow">准备提交</span>
            <h4>{hasAnyInput ? `已准备 ${totalPendingCount} 项输入` : "先添加至少一项输入"}</h4>
            <div className="upload-submit-stats">
              <span>文件 {pendingFiles.length}</span>
              <span>文本 {plainText.trim() ? 1 : 0}</span>
              <span>链接 {externalLink.trim() ? 1 : 0}</span>
              <span>重要依据 {keyMaterialCount}</span>
            </div>
            <p className="muted">
              提交后不会直接生成正文，会先进入“解析质量清单”，让你确认 AI 是否理解正确。
            </p>
            {literatureCandidates.length > 0 && (
              <div className="field candidate-link-field">
                <label>关联待下载候选文献（可选）</label>
                <select
                  value={selectedCandidateId}
                  onChange={(event) => setSelectedCandidateId(event.target.value)}
                >
                  <option value="">本次上传不关联候选</option>
                  {literatureCandidates.map((candidate) => (
                    <option key={candidate.id} value={candidate.id} disabled={candidate.status === "LINKED"}>
                      {candidate.status === "LINKED" ? "已关联｜" : "待下载｜"}
                      {candidate.title}
                    </option>
                  ))}
                </select>
                <small className="muted">适合从材料不足页加入清单后，下载原文再上传。候选本身不会直接参与生成。</small>
              </div>
            )}
            {uploadSummary && (
              <div className="mini-card">
                <strong>上传结果</strong>
                <p className="muted">
                  成功 {uploadSummary.successCount} 项 ｜ 失败 {uploadSummary.failedCount} 项
                </p>
              </div>
            )}
            <button className="primary-btn" disabled={submitting || !hasAnyInput}>
              {submitting ? "上传中..." : "提交并进入解析"}
            </button>
          </aside>
        </div>
      </form>
    </section>
  );
}

function formatBytes(size) {
  if (!size) return "0 B";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function renderFileStatus(status) {
  switch (status) {
    case "uploading":
      return "上传中";
    case "success":
      return "已上传";
    case "error":
      return "失败";
    default:
      return "待上传";
  }
}

function hasDraggedFiles(event) {
  return Array.from(event.dataTransfer?.types ?? []).includes("Files");
}
