import { safeReturnTo } from "../auth/returnTo.js";
import { supabase } from "../auth/supabaseClient.js";

const API_BASE = "/api/v1";
const activeBlobUrls = new Set();

function protectedObjectUrl(blob) {
  const objectUrl = URL.createObjectURL(blob);
  activeBlobUrls.add(objectUrl);
  return objectUrl;
}

function releaseProtectedObjectUrl(objectUrl) {
  if (!activeBlobUrls.delete(objectUrl)) return;
  URL.revokeObjectURL(objectUrl);
}

export function revokeProtectedBlobUrls() {
  [...activeBlobUrls].forEach(releaseProtectedObjectUrl);
}

async function request(path, options = {}) {
  const response = await authenticatedFetch(`${API_BASE}${path}`, {
    headers: {
      ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...(options.headers || {})
    },
    ...options
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok || payload.success === false) {
    const error = new Error(normalizeApiError(response.status, payload?.error?.message));
    error.status = response.status;
    error.code = payload?.error?.code;
    throw error;
  }

  return payload.data;
}

async function authenticatedFetch(url, options = {}) {
  const { data } = await supabase.auth.getSession();
  const token = data.session?.access_token;
  const response = await fetch(url, {
    ...options,
    headers: {
      ...(options.headers || {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }
  });
  if (response.status === 401 && window.location.pathname.startsWith("/app")) {
    const { data: refreshed, error } = await supabase.auth.refreshSession();
    if (!error && refreshed.session?.access_token) {
      return fetch(url, {
        ...options,
        headers: {
          ...(options.headers || {}),
          Authorization: `Bearer ${refreshed.session.access_token}`
        }
      });
    }
    await supabase.auth.signOut({ scope: "local" });
    const returnTo = safeReturnTo(`${window.location.pathname}${window.location.search}`);
    window.location.assign(`/sign-in?returnTo=${encodeURIComponent(returnTo)}`);
  }
  return response;
}

async function protectedBlob(path) {
  const response = await authenticatedFetch(path.startsWith("/api/") ? path : `${API_BASE}${path}`);
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    throw new Error(normalizeApiError(response.status, payload?.error?.message));
  }
  return response.blob();
}

function normalizeApiError(status, message) {
  if (message) return message;
  if (status === 401) return "登录已失效，请重新登录";
  if (status === 502) return "后端或 AI 服务暂时没有正确响应，请确认 backend 已启动；如果正文较长，请先选中一小段再执行。";
  if (status === 503) return "后端或 AI 服务暂时不可用，请检查配置或稍后重试。";
  if (status === 504) return "请求超时，请稍后重试；如果正文较长，请先缩小处理范围。";
  if (status === 0) return "无法连接后端服务，请确认 backend 已在 8080 端口运行。";
  return "请求失败";
}

export const api = {
  getCurrentUser() {
    return request("/me");
  },
  updateCurrentUser(displayName) {
    return request("/me", { method: "PATCH", body: JSON.stringify({ displayName }) });
  },
  listWorkspaces() {
    return request("/workspaces");
  },
  getWorkspace(workspaceId) {
    return request(`/workspaces/${workspaceId}`);
  },
  createWorkspace(input) {
    const body = typeof input === "string" ? { title: input } : input;
    return request("/workspaces", {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  getAcademicProfile(workspaceId) {
    return request(`/workspaces/${workspaceId}/academic-profile`);
  },
  updateAcademicProfile(workspaceId, body) {
    return request(`/workspaces/${workspaceId}/academic-profile`, {
      method: "PATCH",
      body: JSON.stringify(body)
    });
  },
  listAcademicDocuments(workspaceId) {
    return request(`/workspaces/${workspaceId}/documents`);
  },
  createAcademicDocument(workspaceId, body) {
    return request(`/workspaces/${workspaceId}/documents`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  getAcademicDocument(documentId) {
    return request(`/documents/${documentId}`);
  },
  updateAcademicDocument(documentId, body) {
    return request(`/documents/${documentId}`, {
      method: "PATCH",
      body: JSON.stringify(body)
    });
  },
  activateAcademicDocument(documentId) {
    return request(`/documents/${documentId}/activate`, { method: "POST" });
  },
  listDocumentSections(documentId) {
    return request(`/documents/${documentId}/sections`);
  },
  createDocumentSection(documentId, body) {
    return request(`/documents/${documentId}/sections`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  reorderDocumentSections(documentId, sectionIds) {
    return request(`/documents/${documentId}/sections/order`, {
      method: "PATCH",
      body: JSON.stringify({ sectionIds })
    });
  },
  updateDocumentSection(sectionId, body) {
    return request(`/sections/${sectionId}`, {
      method: "PATCH",
      body: JSON.stringify(body)
    });
  },
  listDocumentSectionVersions(sectionId) {
    return request(`/sections/${sectionId}/versions`);
  },
  restoreDocumentSectionVersion(versionId) {
    return request(`/section-versions/${versionId}/restore`, { method: "POST" });
  },
  checkDocumentReadiness(documentId) {
    return request(`/documents/${documentId}/readiness-check`, { method: "POST" });
  },
  checkSectionReadiness(sectionId) {
    return request(`/sections/${sectionId}/readiness-check`, { method: "POST" });
  },
  generateDocumentSection(sectionId, body = {}) {
    return request(`/sections/${sectionId}/generate`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  previewSectionCoWrite(sectionId, body) {
    return request(`/sections/${sectionId}/co-write/preview`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  applySectionCoWritePreview(previewId, body = null) {
    return request(`/section-co-write-previews/${previewId}/apply`, {
      method: "POST",
      ...(body ? { body: JSON.stringify(body) } : {})
    });
  },
  discardSectionCoWritePreview(previewId) {
    return request(`/section-co-write-previews/${previewId}/discard`, { method: "POST" });
  },
  getSectionEvidenceBindings(sectionId) {
    return request(`/sections/${sectionId}/evidence-bindings`);
  },
  rebuildSectionEvidenceBindings(sectionId) {
    return request(`/sections/${sectionId}/evidence-bindings/rebuild`, { method: "POST" });
  },
  getDocumentEvidenceSummary(documentId) {
    return request(`/documents/${documentId}/evidence-summary`);
  },
  getSectionWritingRisks(sectionId) {
    return request(`/sections/${sectionId}/writing-risks`);
  },
  getDocumentWritingRisks(documentId) {
    return request(`/documents/${documentId}/writing-risks`);
  },
  getDocumentQualitySummary(documentId) {
    return request(`/documents/${documentId}/quality-summary`);
  },
  listDocumentReviewItems(documentId, filters = {}) {
    const query = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value != null && value !== "") query.set(key, value);
    });
    const suffix = query.size ? `?${query.toString()}` : "";
    return request(`/documents/${documentId}/review-items${suffix}`);
  },
  refreshSectionReviewItems(sectionId) {
    return request(`/sections/${sectionId}/review-items/refresh`, { method: "POST" });
  },
  refreshDocumentReviewItems(documentId) {
    return request(`/documents/${documentId}/review-items/refresh`, { method: "POST" });
  },
  previewSectionSplit(sectionId) {
    return request(`/sections/${sectionId}/split-preview`, { method: "POST" });
  },
  applySectionSplit(sectionId, body) {
    return request(`/sections/${sectionId}/split`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  assembleAcademicDocument(documentId) {
    return request(`/documents/${documentId}/assemble`, { method: "POST" });
  },
  exportAcademicDocument(documentId, body) {
    return request(`/documents/${documentId}/export`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  listDocumentMaterialLinks(documentId) {
    return request(`/documents/${documentId}/materials`);
  },
  linkDocumentMaterial(documentId, body) {
    return request(`/documents/${documentId}/materials`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  listDocumentAiActions(documentId) {
    return request(`/documents/${documentId}/ai-actions`);
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
  getRequirementSnapshot(workspaceId, optional = false) {
    return request(`/workspaces/${workspaceId}/requirement-snapshot${optional ? "?optional=true" : ""}`);
  },
  checkMaterialSufficiency(workspaceId, requirementSnapshotId) {
    return request(`/workspaces/${workspaceId}/material-sufficiency-check`, {
      method: "POST",
      body: JSON.stringify({ requirementSnapshotId })
    });
  },
  searchLiterature(workspaceId, body) {
    return request(`/workspaces/${workspaceId}/literature-search`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  saveLiteratureCandidate(workspaceId, body) {
    return request(`/workspaces/${workspaceId}/literature-candidates`, {
      method: "POST",
      body: JSON.stringify(body)
    });
  },
  listLiteratureCandidates(workspaceId) {
    return request(`/workspaces/${workspaceId}/literature-candidates`);
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
  searchKnowledgeBase(workspaceId, query, limit = 8, filters = {}) {
    return request(`/workspaces/${workspaceId}/knowledge-base/search`, {
      method: "POST",
      body: JSON.stringify({ query, limit, ...filters })
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
  getWritingRisks(draftId) {
    return request(`/drafts/${draftId}/writing-risks`);
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
  },
  async openProtectedFile(path) {
    const popup = window.open("about:blank", "_blank");
    if (popup) popup.opener = null;
    try {
      const blob = await protectedBlob(path);
      const objectUrl = protectedObjectUrl(blob);
      if (popup) popup.location = objectUrl;
      else window.open(objectUrl, "_blank", "noopener,noreferrer");
      window.setTimeout(() => releaseProtectedObjectUrl(objectUrl), 60_000);
    } catch (error) {
      popup?.close();
      throw error;
    }
  },
  async downloadProtectedFile(path, filename = "download") {
    const blob = await protectedBlob(path);
    const objectUrl = protectedObjectUrl(blob);
    const anchor = document.createElement("a");
    anchor.href = objectUrl;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    releaseProtectedObjectUrl(objectUrl);
  }
};
