import { useState } from "react";
import { RECOMMENDED_TASKS, SIDEBAR_TABS } from "./constants.js";
import { ReviewItem } from "./ReviewItem.jsx";
import { WorkspacePanel } from "./WorkspacePanel.jsx";
import { countMustConfirm } from "./workspaceUtils.js";

export function WorkspaceReviewSidebar({
  reviews,
  reviewsLoading,
  appealState,
  latestFeedback,
  onOpenReview,
  onOpenAppeal,
  onFixReview,
  onUpdateReviewStatus
}) {
  const [activeTab, setActiveTab] = useState("reviews");
  const mustConfirmCount = countMustConfirm(reviews);

  return (
    <WorkspacePanel title="辅助面板" compact>
      <div className="panel-body">
        <div className="workspace-tabs" role="tablist">
          {SIDEBAR_TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              role="tab"
              aria-selected={activeTab === tab.key}
              className={`workspace-tab ${activeTab === tab.key ? "active" : ""}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
              {tab.key === "reviews" && reviews.length > 0 && (
                <span className="workspace-tab-count">{reviews.length}</span>
              )}
            </button>
          ))}
        </div>

        {activeTab === "tasks" && (
          <div className="sidebar-section">
            {RECOMMENDED_TASKS.map((task) => (
              <article className="task-card" key={task.id}>
                <strong>{task.title}</strong>
                <p className="muted">{task.detail}</p>
              </article>
            ))}
          </div>
        )}

        {activeTab === "reviews" && (
          <div className="sidebar-section">
            <div className="sidebar-summary">
              <span className="meta-chip">共 {reviews.length} 条</span>
              {mustConfirmCount > 0 && (
                <span className="meta-chip meta-chip--warn">{mustConfirmCount} 项必须确认</span>
              )}
            </div>
            {reviewsLoading && (
              <div className="empty-state">
                <p className="muted">正在加载审查结果…</p>
              </div>
            )}
            {!reviewsLoading && reviews.length === 0 && (
              <div className="empty-state">
                <p className="muted">当前版本暂无审查项，继续编辑或发起共写后会自动更新。</p>
              </div>
            )}
            {reviews.map((review) => (
              <ReviewItem
                key={review.id}
                review={review}
                appealResult={appealState[review.id]}
                onOpenReview={onOpenReview}
                onOpenAppeal={onOpenAppeal}
                onFixReview={onFixReview}
                onUpdateReviewStatus={onUpdateReviewStatus}
              />
            ))}
          </div>
        )}

        {activeTab === "activity" && (
          <div className="sidebar-section">
            {latestFeedback ? (
              <article className="activity-card">
                <span className="activity-time">最近操作</span>
                <p>{latestFeedback}</p>
              </article>
            ) : (
              <div className="empty-state">
                <p className="muted">保存正文、发起共写或提交申诉后，操作记录会显示在这里。</p>
              </div>
            )}
          </div>
        )}
      </div>
    </WorkspacePanel>
  );
}
