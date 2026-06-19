import { useMemo, useRef, useState } from "react";
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
  const fileInputRef = useRef(null);
  const dragDepthRef = useRef(0);

  const totalPendingCount = useMemo(() => {
    let count = pendingFiles.length;
    if (plainText.trim()) count += 1;
    if (externalLink.trim()) count += 1;
    return count;
  }, [pendingFiles, plainText, externalLink]);

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

  async function uploadPendingFile(item) {
    updateFile(item.localId, (current) => ({ ...current, status: "uploading", error: "" }));
    const formData = new FormData();
    formData.append("files", item.file);
    formData.append("sourceType", "upload");
    formData.append("isKeyMaterial", String(item.isKeyMaterial));
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

      if (plainText.trim()) {
        const formData = new FormData();
        formData.append("plainText", plainText.trim());
        formData.append("sourceType", "pasted_text");
        formData.append("isKeyMaterial", String(textIsKey));
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
        try {
          await api.uploadMaterials(workspace.id, formData);
          successCount += 1;
        } catch (error) {
          failedCount += 1;
          onError(error.message);
        }
      }

      for (const item of pendingFiles) {
        const success = await uploadPendingFile(item);
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
    <section className="page-card">
      <h3 className="page-section-title">上传待解析的写作输入</h3>
      <p className="section-help">
        这里上传的是会进入解析、充足性检查和正文生成的写作输入。你可以一次补文本、链接和多个文件。
      </p>

      <form className="grid-2" onSubmit={handleSubmit}>
        <div className="card-block">
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
            <strong>拖入或选择文件</strong>
            <p className="muted">
              {draggingFiles
                ? "松开鼠标即可加入文件队列。"
                : "支持 PDF / DOCX / TXT / Markdown / 图片 / XLSX / CSV / PPTX / ZIP。用于真实解析、OCR 与后续初稿生成。"}
            </p>
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
          <div className="upload-queue">
            <div className="queue-header">
              <h4>文件队列</h4>
              <span className="muted">共 {pendingFiles.length} 个文件</span>
            </div>
            {pendingFiles.length === 0 ? (
              <div className="mini-card">
                <p className="muted">当前还没有选择文件。可以先上传文本，也可以补充一个或多个文件。</p>
              </div>
            ) : (
              <div className="list-stack">
                {pendingFiles.map((item) => (
                  <div className="mini-card" key={item.localId}>
                    <strong>{item.file.name}</strong>
                    <p className="muted">
                      类型：{item.file.name.split(".").pop()?.toLowerCase() || "unknown"} ｜ 大小：{formatBytes(item.file.size)}
                    </p>
                    <div className="button-row" style={{ marginTop: 10 }}>
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
                        关键材料
                      </label>
                      <span className={`status-pill ${item.status}`}>{renderFileStatus(item.status)}</span>
                      <button type="button" className="ghost-btn" onClick={() => removeFile(item.localId)}>
                        移除
                      </button>
                    </div>
                    {item.error && <p className="review-fix">上传失败：{item.error}</p>}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="card-block">
          <div className="field">
            <label>粘贴研究内容 / 草稿 / 作业要求</label>
            <textarea
              value={plainText}
              onChange={(event) => setPlainText(event.target.value)}
              placeholder="粘贴作业要求、研究笔记、已有草稿等"
            />
          </div>
          <div className="field">
            <label className="inline-check">
              <input
                type="checkbox"
                checked={textIsKey}
                onChange={(event) => setTextIsKey(event.target.checked)}
              />
              将这段文本作为关键材料
            </label>
          </div>
          <div className="field">
            <label>外部链接 / DOI</label>
            <input
              value={externalLink}
              onChange={(event) => setExternalLink(event.target.value)}
              placeholder="https://..."
            />
          </div>
          <div className="field">
            <label className="inline-check">
              <input
                type="checkbox"
                checked={linkIsKey}
                onChange={(event) => setLinkIsKey(event.target.checked)}
              />
              将链接作为关键材料
            </label>
          </div>
          {uploadSummary && (
            <div className="mini-card">
              <strong>上传结果</strong>
              <p className="muted">
                成功 {uploadSummary.successCount} 项 ｜ 失败 {uploadSummary.failedCount} 项
              </p>
            </div>
          )}
          <div className="button-row">
            <button className="primary-btn" disabled={submitting}>
              {submitting ? "上传中..." : "提交并进入解析"}
            </button>
          </div>
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
