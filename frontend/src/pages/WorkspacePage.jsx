import { useWorkspace } from "../hooks/useWorkspace.js";
import { WorkspaceHeader } from "../components/workspace/WorkspaceHeader.jsx";
import { WorkspaceReviewSidebar } from "../components/workspace/WorkspaceReviewSidebar.jsx";
import { WorkspaceEditorPanel } from "../components/workspace/WorkspaceEditorPanel.jsx";
import { WorkspaceAiPanel } from "../components/workspace/WorkspaceAiPanel.jsx";
import { WorkspaceReviewDrawer } from "../components/workspace/WorkspaceReviewDrawer.jsx";
import { WorkspaceAppealModal } from "../components/workspace/WorkspaceAppealModal.jsx";
import { WorkspaceVersionPanel } from "../components/workspace/WorkspaceVersionPanel.jsx";
import { WorkspaceCoWritePreviewDrawer } from "../components/workspace/WorkspaceCoWritePreviewDrawer.jsx";

export function WorkspacePage({ workspace, draft, onDraftChange, onGoExport, onError }) {
  const {
    draftText,
    setDraftText,
    reviews,
    reviewsLoading,
    coWriting,
    savingDraft,
    latestFeedback,
    appealState,
    activeReview,
    setActiveReview,
    appealReview,
    setAppealReview,
    appealLoading,
    versions,
    materials,
    citationStyle,
    setCitationStyle,
    selectedRange,
    knowledgeResults,
    knowledgeSearching,
    restoringVersion,
    evidenceSummary,
    evidenceLoading,
    evidenceRebuildJob,
    writingRisks,
    writingRisksLoading,
    coWritePreview,
    applyingPreview,
    discardingPreview,
    recheckingReviewId,
    handleCoWrite,
    handleFixReview,
    handleReviewStatus,
    handleRecheckReview,
    handleSaveDraft,
    handleRebuildEvidenceBindings,
    handleUpdateEvidenceBindingStatus,
    handlePreviewMaterial,
    handleLocateEvidence,
    handleLocateWritingRisk,
    handleFixWritingRisk,
    handleInsertCitation,
    handleKnowledgeSearch,
    handleAcceptCurrentVersion,
    handleRestoreVersion,
    handleApplyCoWritePreview,
    handleApplySelectedCoWriteDiffRows,
    handleApplySelectedCoWriteParagraphs,
    handleDiscardCoWritePreview,
    handleSubmitAppeal,
    handleSelectionChange,
    openReview,
    openAppeal
  } = useWorkspace({ workspace, draft, onDraftChange, onError });

  return (
    <div className="workspace-page">
      <WorkspaceHeader
        workspace={workspace}
        draft={draft}
        reviews={reviews}
        onGoExport={onGoExport}
        onOpenReviews={() => {
          const firstReview = reviews[0];
          if (firstReview) openReview(firstReview);
        }}
      />

      <div className="workspace-main">
        <WorkspaceReviewSidebar
          reviews={reviews}
          reviewsLoading={reviewsLoading}
          appealState={appealState}
          latestFeedback={latestFeedback}
          onOpenReview={openReview}
          onOpenAppeal={openAppeal}
          onFixReview={handleFixReview}
          onUpdateReviewStatus={handleReviewStatus}
        />

        <WorkspaceEditorPanel
          workspace={workspace}
          draft={draft}
          draftText={draftText}
          saving={savingDraft}
          materials={materials}
          citationStyle={citationStyle}
          selectedRange={selectedRange}
          onDraftTextChange={setDraftText}
          onSelectionChange={handleSelectionChange}
          onCitationStyleChange={setCitationStyle}
          onInsertCitation={handleInsertCitation}
          evidenceSummary={evidenceSummary}
          evidenceLoading={evidenceLoading}
          evidenceRebuildJob={evidenceRebuildJob}
          onRebuildEvidenceBindings={handleRebuildEvidenceBindings}
          onUpdateEvidenceBindingStatus={handleUpdateEvidenceBindingStatus}
          onPreviewMaterial={handlePreviewMaterial}
          onLocateEvidence={handleLocateEvidence}
          writingRisks={writingRisks}
          writingRisksLoading={writingRisksLoading}
          onLocateWritingRisk={handleLocateWritingRisk}
          onFixWritingRisk={handleFixWritingRisk}
          knowledgeResults={knowledgeResults}
          knowledgeSearching={knowledgeSearching}
          onKnowledgeSearch={handleKnowledgeSearch}
          onSaveDraftText={handleSaveDraft}
        />

        <WorkspaceAiPanel
          coWriting={coWriting}
          latestFeedback={latestFeedback}
          selectedRange={selectedRange}
          onCoWrite={handleCoWrite}
        />
      </div>

      <WorkspaceVersionPanel
        versions={versions}
        currentDraftId={draft?.id}
        restoring={restoringVersion}
        onAcceptCurrent={handleAcceptCurrentVersion}
        onRestoreVersion={handleRestoreVersion}
      />

      <WorkspaceReviewDrawer
        review={activeReview}
        onClose={() => setActiveReview(null)}
        onOpenAppeal={openAppeal}
        onFixReview={handleFixReview}
        onUpdateReviewStatus={handleReviewStatus}
        onRecheckReview={handleRecheckReview}
        rechecking={activeReview?.id === recheckingReviewId}
      />

      <WorkspaceCoWritePreviewDrawer
        preview={coWritePreview}
        currentDraftText={draftText}
        reviews={reviews}
        applying={applyingPreview}
        discarding={discardingPreview}
        onApply={handleApplyCoWritePreview}
        onApplySelectedRows={handleApplySelectedCoWriteDiffRows}
        onApplySelectedParagraphs={handleApplySelectedCoWriteParagraphs}
        onDiscard={handleDiscardCoWritePreview}
      />

      <WorkspaceAppealModal
        review={appealReview}
        loading={appealLoading}
        onClose={() => setAppealReview(null)}
        onSubmit={handleSubmitAppeal}
      />
    </div>
  );
}
