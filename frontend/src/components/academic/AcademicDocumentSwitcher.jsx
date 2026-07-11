import { FilePlus2 } from "lucide-react";
import { DOCUMENT_TYPE_LABELS, PARADIGM_LABELS, STAGE_LABELS } from "./academicOptions.js";

export function AcademicDocumentSwitcher({ documents, activeDocument, profile, onSelect, onCreate }) {
  return (
    <div className="academic-document-bar">
      <div className="academic-profile-summary">
        <span>{STAGE_LABELS[profile?.academicStage] ?? "学术阶段待确认"}</span>
        <span>{PARADIGM_LABELS[profile?.researchParadigm] ?? "研究范式待确认"}</span>
        <span>{profile?.defaultCitationStyle ?? "APA"}</span>
      </div>
      <div className="academic-document-tabs" role="tablist" aria-label="项目学术文档">
        {documents.map((document) => (
          <button
            type="button"
            role="tab"
            aria-selected={document.id === activeDocument?.id}
            className={`academic-document-tab ${document.id === activeDocument?.id ? "is-active" : ""}`}
            key={document.id}
            onClick={() => onSelect(document)}
          >
            <strong>{document.title}</strong>
            <span>{DOCUMENT_TYPE_LABELS[document.documentType] ?? document.documentType} · {document.sectionCount} 章</span>
          </button>
        ))}
        <button className="academic-new-document" type="button" onClick={onCreate} title="新建学术文档">
          <FilePlus2 size={17} aria-hidden="true" />
          新建文档
        </button>
      </div>
    </div>
  );
}
