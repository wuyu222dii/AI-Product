const API_BASE = "/api/v1";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...(options.headers || {})
    },
    ...options
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok || payload.success === false) {
    const error = normalizeApiError(response.status, payload?.error?.message);
    throw new Error(error);
  }

  return payload.data;
}

function normalizeApiError(status, message) {
  if (message) return message;
  if (status === 502) return "AI 服务暂时没有正确响应，请稍后重试；如果正文较长，请先选中一小段再执行。";
  if (status === 503) return "AI 服务暂时不可用，请检查配置或稍后重试。";
  if (status === 504) return "AI 请求超时，请稍后重试；如果正文较长，请先缩小处理范围。";
  return "请求失败";
}

export const api = {
  listWorkspaces() {
    return request("/workspaces");
  },
  createWorkspace(title) {
    return request("/workspaces", {
      method: "POST",
      body: JSON.stringify({ title })
    });
  },
  uploadMaterials(workspaceId, formData) {
    return request(`/workspaces/${workspaceId}/materials`, {
      method: "POST",
      body: formData
    });
  },
  listMaterials(workspaceId) {
    return request(`/workspaces/${workspaceId}/materials`);
  },
  updateMaterialCategory(materialId, materialCategory) {
    return request(`/materials/${materialId}/category`, {
      method: "PATCH",
      body: JSON.stringify({ materialCategory })
    });
  },
  updateBibliographicMetadata(materialId, body) {
    return request(`/materials/${materialId}/bibliographic-metadata`, {
      method: "PATCH",
      body: JSON.stringify(body)
    });
  },
  previewMaterial(materialId) {
    return request(`/materials/${materialId}/preview`);
  },
  preprocessMaterial(materialId) {
    return request(`/materials/${materialId}/preprocess`, {
      method: "POST"
    });
  },
  aiParseMaterial(materialId, forceRetry = false) {
    return request(`/materials/${materialId}/ai-parse`, {
      method: "POST",
      body: JSON.stringify({ forceRetry })
    });
  },
  supplementMaterial(materialId, supplementText, pageRef) {
    const formData = new FormData();
    formData.append("supplementText", supplementText);
    if (pageRef != null && pageRef !== "") {
      formData.append("pageRef", String(pageRef));
    }
    return request(`/materials/${materialId}/supplement`, {
      method: "POST",
      body: formData
    });
  },
  createRequirementSnapshot(workspaceId, body) {
    return request(`/workspaces/${workspaceId}/requirement-snapshot`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  getRequirementSnapshot(workspaceId) {
    return request(`/workspaces/${workspaceId}/requirement-snapshot`);
  },
  checkMaterialSufficiency(workspaceId, requirementSnapshotId) {
    return request(`/workspaces/${workspaceId}/material-sufficiency-check`, {
      method: "POST",
      body: JSON.stringify({ requirementSnapshotId })
    });
  },
  generateDraft(workspaceId, requirementSnapshotId, mode = "stable") {
    return request(`/workspaces/${workspaceId}/generate-draft`, {
      method: "POST",
      body: JSON.stringify({ requirementSnapshotId, mode })
    });
  },
  buildKnowledgeBase(workspaceId) {
    return request(`/workspaces/${workspaceId}/knowledge-base/build`, {
      method: "POST"
    });
  },
  listKnowledgeChunks(workspaceId) {
    return request(`/workspaces/${workspaceId}/knowledge-base/chunks`);
  },
  searchKnowledgeBase(workspaceId, query, limit = 8) {
    return request(`/workspaces/${workspaceId}/knowledge-base/search`, {
      method: "POST",
      body: JSON.stringify({ query, limit })
    });
  },
  listDrafts(workspaceId) {
    return request(`/workspaces/${workspaceId}/drafts`);
  },
  getDraft(draftId) {
    return request(`/drafts/${draftId}`);
  },
  updateDraft(draftId, body) {
    return request(`/drafts/${draftId}`, {
      method: "PATCH",
      body: JSON.stringify(body)
    });
  },
  restoreDraft(draftId) {
    return request(`/drafts/${draftId}/restore`, {
      method: "POST"
    });
  },
  listReviewItems(draftId) {
    return request(`/drafts/${draftId}/review-items`);
  },
  getEvidenceBindings(draftId) {
    return request(`/drafts/${draftId}/evidence-bindings`);
  },
  rebuildEvidenceBindings(draftId) {
    return request(`/drafts/${draftId}/evidence-bindings/rebuild`, {
      method: "POST"
    });
  },
  updateEvidenceBindingStatus(bindingId, body) {
    return request(`/evidence-bindings/${bindingId}/status`, {
      method: "PATCH",
      body: JSON.stringify(body)
    });
  },
  coWrite(workspaceId, body) {
    return request(`/workspaces/${workspaceId}/co-write`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  previewCoWrite(workspaceId, body) {
    return request(`/workspaces/${workspaceId}/co-write/preview`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  applyCoWritePreview(previewId) {
    return request(`/co-write-previews/${previewId}/apply`, {
      method: "POST"
    });
  },
  discardCoWritePreview(previewId) {
    return request(`/co-write-previews/${previewId}/discard`, {
      method: "POST"
    });
  },
  createAppeal(reviewItemId, body) {
    return request(`/review-items/${reviewItemId}/appeal`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  getAppeal(appealId) {
    return request(`/appeals/${appealId}`);
  },
  updateReviewStatus(reviewItemId, body) {
    return request(`/review-items/${reviewItemId}/status`, {
      method: "PATCH",
      body: JSON.stringify(body)
    });
  },
  recheckReviewItem(reviewItemId) {
    return request(`/review-items/${reviewItemId}/recheck`, {
      method: "POST"
    });
  },
  exportDraft(draftId, body) {
    return request(`/drafts/${draftId}/export`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  getJob(jobId) {
    return request(`/jobs/${jobId}`);
  }
};
