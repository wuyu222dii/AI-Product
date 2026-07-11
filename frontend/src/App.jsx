import { useEffect, useMemo, useState } from "react";
import { BrowserRouter, Navigate, NavLink, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { ProjectListPage } from "./pages/ProjectListPage.jsx";
import { UploadPage } from "./pages/UploadPage.jsx";
import { ParsingStatusPage } from "./pages/ParsingStatusPage.jsx";
import { MaterialGatePage } from "./pages/MaterialGatePage.jsx";
import { KnowledgeBasePage } from "./pages/KnowledgeBasePage.jsx";
import { AcademicDocumentsPage } from "./pages/AcademicDocumentsPage.jsx";
import { WorkspacePage } from "./pages/WorkspacePage.jsx";
import { ExportPage } from "./pages/ExportPage.jsx";
import { ErrorBanner } from "./components/ErrorBanner.jsx";

const STEPS = [
  { key: "projects", label: "项目首页", path: "/projects" },
  { key: "upload", label: "上传输入", path: "/upload" },
  { key: "parsing", label: "解析状态", path: "/parsing" },
  { key: "gate", label: "材料检查", path: "/gate" },
  { key: "knowledge", label: "知识库", path: "/knowledge-base" },
  { key: "documents", label: "学术文档", path: "/documents" }
];

const STORAGE_KEYS = {
  workspace: "cowriting-demo-workspace",
  draft: "cowriting-demo-draft",
  activeDocument: "cowriting-active-document"
};

export default function App() {
  return (
    <BrowserRouter>
      <DemoApp />
    </BrowserRouter>
  );
}

function DemoApp() {
  const location = useLocation();
  const navigate = useNavigate();
  const [workspace, setWorkspace] = usePersistentState(STORAGE_KEYS.workspace, null);
  const [draft, setDraft] = usePersistentState(STORAGE_KEYS.draft, null);
  const [activeDocument, setActiveDocument] = usePersistentState(STORAGE_KEYS.activeDocument, null);
  const [globalError, setGlobalError] = useState("");

  const layoutTitle = useMemo(() => {
    return STEPS.find((step) => step.path === location.pathname)?.label ?? "AI 论文共写工作台";
  }, [location.pathname]);

  useEffect(() => {
    const routeWithoutState = ["/upload", "/parsing", "/gate", "/knowledge-base", "/documents", "/workspace", "/export"].includes(location.pathname);
    if (routeWithoutState && !workspace) {
      navigate("/projects", { replace: true });
    }
    if ((location.pathname === "/workspace" || location.pathname === "/export") && !draft) {
      navigate("/gate", { replace: true });
    }
  }, [location.pathname, workspace, draft, navigate]);

  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="brand-block">
          <h1>AI 论文共写工作台</h1>
          <p>面向本科、硕士、博士与科研人员的证据驱动型研究共创平台。</p>
        </div>
        <nav className="nav-list">
          {STEPS.map((step) => (
            <NavLink
              key={step.key}
              to={step.path}
              className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
            >
              {step.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <main className="app-main">
        <header className="page-hero">
          <div>
            <h2>{layoutTitle}</h2>
            <p>研究项目 → 学术画像 → 共享材料与知识库 → 多文档 → 章节共写 → 审查与导出。</p>
          </div>
          {workspace && (
            <div className="hero-meta">
              <span className="hero-chip">{workspace.title}</span>
              <span className="hero-chip">{workspace.status}</span>
              {activeDocument && <span className="hero-chip">{activeDocument.documentType}</span>}
              {draft && <span className="hero-chip">当前版本 v{draft.versionNo}</span>}
            </div>
          )}
        </header>

        {globalError && <ErrorBanner message={globalError} onClose={() => setGlobalError("")} />}

        <Routes>
          <Route path="/" element={<Navigate to="/projects" replace />} />
          <Route
            path="/projects"
            element={
              <ProjectListPage
                onWorkspaceCreated={(created) => {
                  setWorkspace(created);
                  setDraft(null);
                  setActiveDocument(null);
                  navigate("/upload");
                }}
                onWorkspaceSelected={(selected) => {
                  setWorkspace(selected);
                  setDraft(null);
                  setActiveDocument(null);
                  navigate("/upload");
                }}
                onError={setGlobalError}
              />
            }
          />
          <Route
            path="/upload"
            element={
              workspace ? (
                <UploadPage
                  workspace={workspace}
                  onContinue={() => navigate("/parsing")}
                  onError={setGlobalError}
                />
              ) : (
                <Navigate to="/projects" replace />
              )
            }
          />
          <Route
            path="/parsing"
            element={
              workspace ? (
                <ParsingStatusPage
                  workspace={workspace}
                  onParsed={() => navigate("/gate")}
                  onError={setGlobalError}
                />
              ) : (
                <Navigate to="/projects" replace />
              )
            }
          />
          <Route
            path="/gate"
            element={
              workspace ? (
                <MaterialGatePage
                  workspace={workspace}
                  onReady={() => navigate("/knowledge-base")}
                  onEligible={(generatedDraft) => {
                    setDraft(generatedDraft);
                    navigate("/knowledge-base");
                  }}
                  onBackUpload={() => navigate("/upload")}
                  onError={setGlobalError}
                />
              ) : (
                <Navigate to="/projects" replace />
              )
            }
          />
          <Route
            path="/knowledge-base"
            element={
              workspace ? (
                <KnowledgeBasePage
                  workspace={workspace}
                  draft={draft}
                  onContinue={() => navigate("/documents")}
                  onBackParsing={() => navigate("/parsing")}
                  onError={setGlobalError}
                />
              ) : (
                <Navigate to="/projects" replace />
              )
            }
          />
          <Route
            path="/documents"
            element={
              workspace ? (
                <AcademicDocumentsPage
                  workspace={workspace}
                  activeDocument={activeDocument}
                  onActiveDocumentChange={setActiveDocument}
                  onGoUpload={() => navigate("/upload")}
                  onError={setGlobalError}
                />
              ) : (
                <Navigate to="/projects" replace />
              )
            }
          />
          <Route
            path="/workspace"
            element={
              workspace && draft ? (
                <WorkspacePage
                  workspace={workspace}
                  draft={draft}
                  onDraftChange={setDraft}
                  onGoExport={() => navigate("/export")}
                  onError={setGlobalError}
                />
              ) : (
                <Navigate to={workspace ? "/gate" : "/projects"} replace />
              )
            }
          />
          <Route
            path="/export"
            element={
              draft ? (
                <ExportPage
                  workspace={workspace}
                  draft={draft}
                  onBack={() => navigate("/workspace")}
                  onError={setGlobalError}
                />
              ) : (
                <Navigate to={workspace ? "/gate" : "/projects"} replace />
              )
            }
          />
          <Route path="*" element={<Navigate to="/projects" replace />} />
        </Routes>
      </main>
    </div>
  );
}

function usePersistentState(storageKey, initialValue) {
  const [state, setState] = useState(() => {
    try {
      const raw = window.localStorage.getItem(storageKey);
      return raw ? JSON.parse(raw) : initialValue;
    } catch {
      return initialValue;
    }
  });

  useEffect(() => {
    try {
      if (state == null) {
        window.localStorage.removeItem(storageKey);
      } else {
        window.localStorage.setItem(storageKey, JSON.stringify(state));
      }
    } catch {
      // ignore storage failures in demo mode
    }
  }, [storageKey, state]);

  return [state, setState];
}
