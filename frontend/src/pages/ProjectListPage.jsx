import { useEffect, useState } from "react";
import { api } from "../services/api";
import { StatusBadge } from "../components/StatusBadge";

export function ProjectListPage({ onWorkspaceCreated, onWorkspaceSelected, onError }) {
  const [title, setTitle] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  async function load() {
    try {
      const data = await api.listWorkspaces();
      setItems(data.items ?? []);
    } catch (error) {
      onError(error.message);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate(event) {
    event.preventDefault();
    if (!title.trim()) return;
    try {
      setLoading(true);
      const created = await api.createWorkspace(title.trim());
      setTitle("");
      await load();
      onWorkspaceCreated(created);
    } catch (error) {
      onError(error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-card">
      <h3 className="page-section-title">我的论文项目</h3>
      <p className="section-help">先创建一个论文项目，后续上传材料、生成初稿与共写都会围绕这个项目进行。</p>

      <form className="card-block" onSubmit={handleCreate}>
        <div className="field">
          <label>项目标题</label>
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="例如：人工智能对大学生学习方式的影响"
          />
        </div>
        <div className="button-row">
          <button className="primary-btn" disabled={loading}>
            {loading ? "创建中..." : "新建论文项目"}
          </button>
        </div>
      </form>

      <div className="list-stack" style={{ marginTop: 18 }}>
        {items.length === 0 ? (
          <div className="card-block">
            <p className="muted">当前还没有项目，先创建一个项目开始。</p>
          </div>
        ) : (
          items.map((item) => (
            <div className="mini-card" key={item.id}>
              <strong>{item.title}</strong>
              <p className="muted">更新时间：{item.updatedAt}</p>
              <div className="button-row" style={{ marginTop: 10 }}>
                <StatusBadge level={item.status === "READY" || item.status === "ready" ? "ready" : "notice"}>
                  {item.status}
                </StatusBadge>
                <button className="secondary-btn" type="button" onClick={() => onWorkspaceSelected(item)}>
                  进入项目
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </section>
  );
}
