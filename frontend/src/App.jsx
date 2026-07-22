import { useEffect, useState } from "react";
import { ArrowRight, BookOpenText, ChevronRight, Database, FileCheck2, FileSearch, FolderKanban, LayoutDashboard, Library, LogOut, Menu, ShieldCheck, Upload, X } from "lucide-react";
import { BrowserRouter, Link, Navigate, NavLink, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { AuthProvider, useAuth } from "./auth/AuthProvider.jsx";
import { OAuthCodeCatcher } from "./auth/OAuthCodeCatcher.jsx";
import { ProtectedRoute } from "./auth/ProtectedRoute.jsx";
import { ScrollReveal } from "./components/ScrollReveal.jsx";
import { SurfaceTheme } from "./components/SurfaceTheme.jsx";

import { ErrorBanner } from "./components/ErrorBanner.jsx";
import { AcademicDocumentsPage } from "./pages/AcademicDocumentsPage.jsx";
import { AuthCallbackPage } from "./pages/AuthCallbackPage.jsx";
import { ExportPage } from "./pages/ExportPage.jsx";
import { KnowledgeBasePage } from "./pages/KnowledgeBasePage.jsx";
import { MaterialGatePage } from "./pages/MaterialGatePage.jsx";
import { ParsingStatusPage } from "./pages/ParsingStatusPage.jsx";
import { ProjectListPage } from "./pages/ProjectListPage.jsx";
import { ProjectOverviewPage } from "./pages/ProjectOverviewPage.jsx";
import { SignInPage } from "./pages/SignInPage.jsx";
import { UploadPage } from "./pages/UploadPage.jsx";
import { WorkspacePage } from "./pages/WorkspacePage.jsx";
import { api, revokeProtectedBlobUrls } from "./services/api.js";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <SurfaceTheme />
        <OAuthCodeCatcher />
        <RouteTitleSync />
        <Routes>
          <Route path="/" element={<PublicHomePage />} />
          <Route path="/about" element={<AboutPage />} />
          <Route path="/privacy" element={<LegalPage kind="privacy" />} />
          <Route path="/terms" element={<LegalPage kind="terms" />} />
          <Route path="/sign-in" element={<SignInPage />} />
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
          <Route path="/app/*" element={<ProtectedRoute><AuthenticatedApp /></ProtectedRoute>} />
          <Route path="/projects" element={<Navigate to="/app/projects" replace />} />
          <Route path="/upload" element={<Navigate to="/app/projects" replace />} />
          <Route path="/parsing" element={<Navigate to="/app/projects" replace />} />
          <Route path="/gate" element={<Navigate to="/app/projects" replace />} />
          <Route path="/knowledge-base" element={<Navigate to="/app/projects" replace />} />
          <Route path="/documents" element={<Navigate to="/app/projects" replace />} />
          <Route path="/workspace" element={<Navigate to="/app/projects" replace />} />
          <Route path="/export" element={<Navigate to="/app/projects" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

function RouteTitleSync() {
  const location = useLocation();

  useEffect(() => {
    document.title = titleForPath(location.pathname);
  }, [location.pathname]);

  return null;
}

function AuthenticatedApp() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [profile, setProfile] = useState(null);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [globalError, setGlobalError] = useState("");
  const [currentWorkspace, setCurrentWorkspace] = useState(null);
  const workspaceId = location.pathname.match(/^\/app\/projects\/([^/]+)/)?.[1];
  const projectRoot = workspaceId ? `/app/projects/${workspaceId}` : null;
  const pageMeta = appPageMeta(location.pathname);

  useEffect(() => {
    api.getCurrentUser().then(setProfile).catch((error) => setGlobalError(error.message));
  }, [user?.id]);

  useEffect(() => {
    if (!workspaceId) {
      setCurrentWorkspace(null);
      return undefined;
    }
    let active = true;
    api.getWorkspace(workspaceId)
      .then((workspace) => {
        if (active) setCurrentWorkspace(workspace);
      })
      .catch(() => {
        if (active) setCurrentWorkspace(null);
      });
    return () => { active = false; };
  }, [workspaceId]);

  useEffect(() => {
    setMobileNavOpen(false);
  }, [location.pathname]);

  async function handleSignOut() {
    await signOut();
    revokeProtectedBlobUrls();
    ["cowriting-demo-workspace", "cowriting-demo-draft", "cowriting-active-document"].forEach((key) => {
      window.localStorage.removeItem(key);
    });
    navigate("/", { replace: true });
  }

  return (
    <div className="product-shell">
      <button className="mobile-nav-toggle" type="button" aria-label="打开导航" onClick={() => setMobileNavOpen(true)}><Menu size={20} /></button>
      <aside className={`product-sidebar ${mobileNavOpen ? "is-open" : ""}`}>
        <Link className="sidebar-brand" to="/app/projects"><span>AI</span><strong>论文共写工作台</strong></Link>
        <button className="sidebar-close" type="button" aria-label="关闭导航" onClick={() => setMobileNavOpen(false)}><X size={19} /></button>
        <nav aria-label="主导航">
          <NavLink to="/app/projects" end><FolderKanban size={18} />研究项目</NavLink>
          {projectRoot && (
            <>
              <div className="sidebar-nav-group sidebar-nav-group--project">
                <span>当前项目</span>
                <NavLink to={projectRoot} end><LayoutDashboard size={17} />项目概览</NavLink>
              </div>
              <div className="sidebar-nav-group">
                <span>研究准备</span>
                <NavLink to={`${projectRoot}/upload`}><Upload size={17} />研究输入</NavLink>
                <NavLink to={`${projectRoot}/parsing`}><FileSearch size={17} />解析质量</NavLink>
                <NavLink to={`${projectRoot}/materials`}><ShieldCheck size={17} />材料检查</NavLink>
              </div>
              <div className="sidebar-nav-group">
                <span>研究资产</span>
                <NavLink to={`${projectRoot}/knowledge`}><Library size={17} />知识库</NavLink>
              </div>
              <div className="sidebar-nav-group">
                <span>写作交付</span>
                <NavLink to={`${projectRoot}/documents`}><BookOpenText size={17} />学术文档</NavLink>
              </div>
            </>
          )}
        </nav>
        <div className="sidebar-boundary-note"><FileSearch size={17} /><span>生成内容仅使用已选择并完成解析的真实材料。</span></div>
      </aside>

      <div className="product-main">
        <header className="product-topbar">
          <nav className="topbar-context" aria-label="页面位置">
            <Link to="/app/projects">研究项目</Link>
            {projectRoot && (
              <>
                <ChevronRight size={14} aria-hidden="true" />
                <Link to={projectRoot} className="topbar-project-name">{currentWorkspace?.title || "当前项目"}</Link>
                <ChevronRight size={14} aria-hidden="true" />
                <span aria-current="page">{pageMeta.label}</span>
              </>
            )}
          </nav>
          <div className="user-menu-summary"><span className="user-avatar">{initial(profile?.displayName || user?.email)}</span><span><strong>{profile?.displayName || user?.email || "学术用户"}</strong><small>{user?.email}</small></span><button type="button" onClick={handleSignOut} title="退出登录" aria-label="退出登录"><LogOut size={17} /></button></div>
        </header>
        {globalError && <ErrorBanner message={globalError} onClose={() => setGlobalError("")} />}
        <main className="product-content">
          <Routes>
            <Route path="projects" element={<ProjectListRoute onError={setGlobalError} />} />
            <Route path="projects/:workspaceId" element={<ProjectFlowRoute page="overview" onError={setGlobalError} />} />
            <Route path="projects/:workspaceId/upload" element={<ProjectFlowRoute page="upload" onError={setGlobalError} />} />
            <Route path="projects/:workspaceId/parsing" element={<ProjectFlowRoute page="parsing" onError={setGlobalError} />} />
            <Route path="projects/:workspaceId/materials" element={<ProjectFlowRoute page="materials" onError={setGlobalError} />} />
            <Route path="projects/:workspaceId/knowledge" element={<ProjectFlowRoute page="knowledge" onError={setGlobalError} />} />
            <Route path="projects/:workspaceId/documents/:documentId?" element={<ProjectFlowRoute page="documents" onError={setGlobalError} />} />
            <Route path="projects/:workspaceId/legacy-workspace/:draftId" element={<LegacyWorkspaceRoute onError={setGlobalError} />} />
            <Route path="projects/:workspaceId/legacy-export/:draftId" element={<LegacyExportRoute onError={setGlobalError} />} />
            <Route index element={<Navigate to="projects" replace />} />
            <Route path="*" element={<Navigate to="projects" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

function ProjectListRoute({ onError }) {
  const navigate = useNavigate();
  return (
    <ProjectListPage
      onWorkspaceCreated={(workspace) => navigate(`/app/projects/${workspace.id}/upload`)}
      onWorkspaceSelected={(workspace) => navigate(`/app/projects/${workspace.id}`)}
      onError={onError}
    />
  );
}

function ProjectFlowRoute({ page, onError }) {
  const { workspaceId, documentId } = useParams();
  const navigate = useNavigate();
  const [workspace, setWorkspace] = useState(null);
  const [activeDocument, setActiveDocument] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([
      api.getWorkspace(workspaceId),
      documentId ? api.getAcademicDocument(documentId) : Promise.resolve(null)
    ]).then(([nextWorkspace, nextDocument]) => {
      if (!active) return;
      setWorkspace(nextWorkspace);
      setActiveDocument(nextDocument);
      setLoading(false);
    }).catch((error) => {
      if (!active) return;
      onError(error.message);
      setLoading(false);
    });
    return () => { active = false; };
  }, [workspaceId, documentId, onError]);

  if (loading) return <PageSkeleton />;
  if (!workspace) return <ProjectMissing />;

  const root = `/app/projects/${workspace.id}`;
  if (page === "overview") {
    return (
      <ProjectOverviewPage
        workspace={workspace}
        onNavigate={(target) => navigate(`${root}/${target}`)}
        onError={onError}
      />
    );
  }
  if (page === "upload") return <UploadPage workspace={workspace} onContinue={() => navigate(`${root}/parsing`)} onError={onError} />;
  if (page === "parsing") return <ParsingStatusPage workspace={workspace} onParsed={() => navigate(`${root}/materials`)} onError={onError} />;
  if (page === "materials") return <MaterialGatePage workspace={workspace} onReady={() => navigate(`${root}/knowledge`)} onEligible={() => navigate(`${root}/knowledge`)} onBackUpload={() => navigate(`${root}/upload`)} onError={onError} />;
  if (page === "knowledge") return <KnowledgeBasePage workspace={workspace} draft={null} onContinue={() => navigate(`${root}/documents`)} onBackMaterials={() => navigate(`${root}/materials`)} onError={onError} />;
  return (
    <AcademicDocumentsPage
      workspace={workspace}
      activeDocument={activeDocument}
      onActiveDocumentChange={(next) => {
        setActiveDocument(next);
        if (next?.id) navigate(`${root}/documents/${next.id}`, { replace: true });
      }}
      onGoUpload={() => navigate(`${root}/upload`)}
      onError={onError}
    />
  );
}

function LegacyWorkspaceRoute({ onError }) {
  const { workspaceId, draftId } = useParams();
  const navigate = useNavigate();
  const [state, setState] = useState(null);
  useEffect(() => {
    Promise.all([api.getWorkspace(workspaceId), api.getDraft(draftId)])
      .then(([workspace, draft]) => setState({ workspace, draft }))
      .catch((error) => onError(error.message));
  }, [workspaceId, draftId, onError]);
  if (!state) return <PageSkeleton />;
  return <WorkspacePage workspace={state.workspace} draft={state.draft} onDraftChange={(draft) => setState((current) => ({ ...current, draft }))} onGoExport={() => navigate(`/app/projects/${workspaceId}/legacy-export/${state.draft.id}`)} onError={onError} />;
}

function LegacyExportRoute({ onError }) {
  const { workspaceId, draftId } = useParams();
  const navigate = useNavigate();
  const [state, setState] = useState(null);
  useEffect(() => {
    Promise.all([api.getWorkspace(workspaceId), api.getDraft(draftId)])
      .then(([workspace, draft]) => setState({ workspace, draft }))
      .catch((error) => onError(error.message));
  }, [workspaceId, draftId, onError]);
  if (!state) return <PageSkeleton />;
  return <ExportPage workspace={state.workspace} draft={state.draft} onBack={() => navigate(`/app/projects/${workspaceId}/legacy-workspace/${draftId}`)} onError={onError} />;
}

function PageSkeleton() {
  return <section className="page-skeleton" aria-label="正在加载"><span /><span /><span /></section>;
}

function ProjectMissing() {
  return <section className="empty-state"><FolderKanban size={26} /><h1>没有找到这个研究项目</h1><p>项目可能已删除，或不属于当前账号。</p><Link className="primary-btn" to="/app/projects">返回项目列表</Link></section>;
}

function PublicHomePage() {
  return (
    <main className="public-page">
      <PublicHeader />
      <section className="public-hero">
        <div className="public-hero-gradient" aria-hidden="true" />
        <div className="public-hero-noise" aria-hidden="true" />
        <div className="public-hero-inner">
          <ScrollReveal className="public-hero-copy" direction="left">
            <p className="eyebrow">Evidence-driven academic workspace</p>
            <h1>AI 论文共写工作台</h1>
            <p>让材料、证据、章节写作与人工审查保持同一条可信链。面向本科生、研究生、博士生与科研人员。</p>
            <div className="public-hero-actions">
              <Link className="primary-btn" to="/sign-in">进入研究空间 <ArrowRight size={17} /></Link>
              <Link className="public-text-link" to="/about">了解我们的 AI 边界</Link>
            </div>
            <div className="public-hero-proof" aria-label="产品原则">
              <span>材料有来源</span>
              <span>修改可预览</span>
              <span>结论可复查</span>
            </div>
          </ScrollReveal>
          <ScrollReveal className="public-hero-preview" direction="right" delay={80}>
            <article className="preview-card preview-card--sections">
              <span className="preview-label">章节树</span>
              <strong>学位论文 · 第三章</strong>
              <ul>
                <li className="is-active">3.1 研究方法</li>
                <li>3.2 实验设计</li>
                <li>3.3 结果分析</li>
              </ul>
            </article>
            <article className="preview-card preview-card--evidence">
              <span className="preview-label">可信链</span>
              <p>段落已绑定 2 份材料 · 覆盖率 86%</p>
              <div className="preview-meter"><span style={{ width: "86%" }} /></div>
            </article>
            <article className="preview-card preview-card--review">
              <span className="preview-label">审查</span>
              <p>1 项待确认 · 2 项局部修正</p>
            </article>
          </ScrollReveal>
        </div>
      </section>

      <ScrollReveal as="section" className="public-principle-band scroll-reveal-stagger" threshold={0.15}>
        <div><span className="public-index">01</span><strong>研究者拥有最终决定权</strong><p>AI 提供候选结构与修改，不替用户判断。</p></div>
        <div><span className="public-index">02</span><strong>正文能够回到原始材料</strong><p>段落、证据、文献和引用保持关联。</p></div>
        <div><span className="public-index">03</span><strong>材料不足时不虚构</strong><p>系统给出补充清单和真实文献入口。</p></div>
      </ScrollReveal>

      <ScrollReveal as="section" className="public-section public-workflow-section" threshold={0.12}>
        <div className="public-section-heading"><p className="eyebrow">Research workflow</p><h2>从研究输入到可交付学术文档</h2><p>一个研究项目可以共享材料与知识库，并承载多个彼此独立的学术文档。</p></div>
        <ScrollReveal className="public-workflow-line scroll-reveal-stagger" delay={40} threshold={0.18}>
          <div><Upload size={20} /><span>01</span><strong>汇集研究输入</strong><p>文件、文本、数据、要求与文献。</p></div>
          <div><Database size={20} /><span>02</span><strong>形成可信知识底座</strong><p>AI 解析、质量确认与来源定位。</p></div>
          <div><BookOpenText size={20} /><span>03</span><strong>按章节共写</strong><p>预览差异、保留数据与引用约束。</p></div>
          <div><FileCheck2 size={20} /><span>04</span><strong>审查并交付</strong><p>复查问题、组装整篇与受控导出。</p></div>
        </ScrollReveal>
      </ScrollReveal>

      <ScrollReveal as="section" className="public-section public-boundary-section" threshold={0.12}>
        <div className="public-section-heading"><p className="eyebrow">Clear boundaries</p><h2>我们坚持什么，也明确拒绝什么</h2></div>
        <ScrollReveal className="public-boundary-grid scroll-reveal-stagger" delay={30}>
          <div><strong>我们坚持</strong><p>真实材料优先、修改前预览、证据链保留、争议判断可推翻、AI 使用可记录。</p></div>
          <div><strong>我们拒绝</strong><p>虚构文献与数据、替代研究过程、承诺规避检测、绕过学校或出版方规范。</p></div>
        </ScrollReveal>
      </ScrollReveal>

      <ScrollReveal as="section" className="public-section public-trust-section" threshold={0.12}>
        <ScrollReveal className="public-trust-copy" direction="left">
          <p className="eyebrow">Trust by design</p><h2>不是替你“写得像人”，而是帮你写得更有依据</h2><p>平台不承诺规避 AI 检测。它识别空泛论证、证据缺口与模板化表达，优先引导补充真实案例、数据和来源。</p><Link className="public-text-link" to="/about">查看可信共写原则 <ArrowRight size={16} /></Link>
        </ScrollReveal>
        <ScrollReveal className="public-trust-list scroll-reveal-stagger" direction="right" delay={50}>
          <div><ShieldCheck size={19} /><span><strong>证据绑定</strong><small>每个关键论点都能追溯到材料。</small></span></div>
          <div><FileSearch size={19} /><span><strong>人工复查</strong><small>争议判断可申诉、确认与重新审查。</small></span></div>
          <div><Library size={19} /><span><strong>真实文献回流</strong><small>候选文献需上传原文并解析后才参与生成。</small></span></div>
        </ScrollReveal>
      </ScrollReveal>

      <ScrollReveal as="section" className="public-final-cta" threshold={0.15}>
        <p className="eyebrow">Start with your evidence</p><h2>把研究材料变成可控制、可追溯的学术写作过程</h2><Link className="primary-btn" to="/sign-in">创建研究空间 <ArrowRight size={17} /></Link>
      </ScrollReveal>
      <PublicFooter />
    </main>
  );
}

function AboutPage() {
  return (
    <main className="public-page">
      <PublicHeader />
      <section className="about-hero"><p className="eyebrow">About the product</p><h1>AI 论文共写工作台</h1><p>我们把 AI 定义为研究共创助手：能够组织、生成和修改，但不能替代证据、研究过程与研究者责任。</p></section>
      <section className="about-statement"><span className="public-index">Our position</span><blockquote>高质量学术写作的核心，不是隐藏 AI 的存在，而是让每一个关键判断都有材料依据，并由研究者确认。</blockquote></section>
      <section className="public-section about-boundaries"><div className="public-section-heading"><p className="eyebrow">Clear boundaries</p><h2>平台做什么，也明确不做什么</h2></div><div className="about-boundary-grid"><div><strong>我们坚持</strong><p>真实材料优先、修改前预览、证据链保留、争议判断可推翻、AI 使用可记录。</p></div><div><strong>我们拒绝</strong><p>虚构文献与数据、替代研究过程、承诺规避检测、绕过学校或出版方规范。</p></div></div></section>
      <section className="public-section about-audience"><div><p className="eyebrow">Built for academic depth</p><h2>同一研究底座，适配不同学术阶段</h2></div><div className="about-audience-rows"><p><span>本科</span>课程论文、毕业论文与研究入门</p><p><span>硕士</span>开题、学位论文与方法论证</p><p><span>博士</span>研究问题、原创贡献与长文一致性</p><p><span>科研人员</span>研究计划、论文稿件与项目报告</p></div></section>
      <PublicFooter />
    </main>
  );
}

function LegalPage({ kind }) {
  return <main className="public-page"><PublicHeader /><article className="public-article"><h1>{kind === "privacy" ? "隐私说明" : "服务条款"}</h1><p>{kind === "privacy" ? "账号用于隔离研究项目与文件。平台不会通过前端公开业务数据库。" : "用户应确保上传材料具有合法使用权，并对最终提交的学术内容负责。"}</p></article></main>;
}

function PublicHeader() {
  return <header className="public-header"><Link to="/" className="public-brand"><span>AI</span>论文共写工作台</Link><nav><Link to="/about">产品理念</Link><Link to="/sign-in" className="primary-btn">登录</Link></nav></header>;
}

function PublicFooter() {
  return <footer className="public-footer"><Link className="public-brand" to="/"><span>AI</span>论文共写工作台</Link><p>证据驱动的研究共创平台</p><nav><Link to="/privacy">隐私</Link><Link to="/terms">条款</Link><Link to="/sign-in">登录</Link></nav></footer>;
}

function initial(value) {
  return String(value || "U").trim().slice(0, 1).toUpperCase();
}

function titleForPath(pathname) {
  if (pathname === "/") return "AI 论文共写工作台";
  if (pathname === "/about") return "产品理念 · AI 论文共写工作台";
  if (pathname === "/privacy") return "隐私说明 · AI 论文共写工作台";
  if (pathname === "/terms") return "服务条款 · AI 论文共写工作台";
  if (pathname === "/sign-in") return "登录研究空间 · AI 论文共写工作台";
  if (pathname === "/auth/callback") return "登录处理中 · AI 论文共写工作台";
  if (pathname === "/app/projects") return "研究项目 · AI 论文共写工作台";
  if (/^\/app\/projects\/[^/]+$/.test(pathname)) return "项目概览 · AI 论文共写工作台";
  if (/^\/app\/projects\/[^/]+\/upload$/.test(pathname)) return "研究输入 · AI 论文共写工作台";
  if (/^\/app\/projects\/[^/]+\/parsing$/.test(pathname)) return "解析质量 · AI 论文共写工作台";
  if (/^\/app\/projects\/[^/]+\/materials$/.test(pathname)) return "材料检查 · AI 论文共写工作台";
  if (/^\/app\/projects\/[^/]+\/knowledge$/.test(pathname)) return "知识库 · AI 论文共写工作台";
  if (/^\/app\/projects\/[^/]+\/documents(?:\/[^/]+)?$/.test(pathname)) return "学术文档 · AI 论文共写工作台";
  if (/^\/app\/projects\/[^/]+\/legacy-workspace\/[^/]+$/.test(pathname)) return "写作工作台 · AI 论文共写工作台";
  if (/^\/app\/projects\/[^/]+\/legacy-export\/[^/]+$/.test(pathname)) return "导出文稿 · AI 论文共写工作台";
  return "AI 论文共写工作台 · Evidence-driven Research";
}

function appPageMeta(pathname) {
  if (pathname === "/app/projects") return { label: "研究项目" };
  if (/\/upload$/.test(pathname)) return { label: "研究输入" };
  if (/\/parsing$/.test(pathname)) return { label: "解析质量" };
  if (/\/materials$/.test(pathname)) return { label: "材料检查" };
  if (/\/knowledge$/.test(pathname)) return { label: "知识库" };
  if (/\/documents(?:\/[^/]+)?$/.test(pathname)) return { label: "学术文档" };
  if (/\/legacy-workspace\//.test(pathname)) return { label: "兼容工作台" };
  if (/\/legacy-export\//.test(pathname)) return { label: "兼容导出" };
  if (/^\/app\/projects\/[^/]+$/.test(pathname)) return { label: "项目概览" };
  return { label: "研究空间" };
}
