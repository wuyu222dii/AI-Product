import { useEffect, useState } from "react";

export function AcademicSplitModal({ preview, applying, onClose, onApply }) {
  const [items, setItems] = useState([]);
  useEffect(() => setItems(preview?.sections ?? []), [preview]);
  if (!preview) return null;
  return (
    <div className="overlay-shell" onClick={onClose}>
      <div className="modal-card academic-split-modal" onClick={(event) => event.stopPropagation()}>
        <header><div><h3>预览章节拆分</h3><p>{preview.message}</p></div><button className="ghost-btn" type="button" onClick={onClose}>关闭</button></header>
        <div className="academic-split-list">
          {items.map((item, index) => (
            <article key={`${item.title}-${index}`}>
              <span>{index + 1}</span>
              <div className="field"><label>章节标题</label><input value={item.title} onChange={(event) => setItems((current) => current.map((entry, entryIndex) => entryIndex === index ? { ...entry, title: event.target.value } : entry))} /></div>
              <div className="field"><label>章节类型</label><input value={item.sectionType} onChange={(event) => setItems((current) => current.map((entry, entryIndex) => entryIndex === index ? { ...entry, sectionType: event.target.value.toUpperCase() } : entry))} /></div>
              <p>{item.content.slice(0, 180)}{item.content.length > 180 ? "..." : ""}</p>
            </article>
          ))}
        </div>
        <footer><button className="ghost-btn" type="button" onClick={onClose}>保留整篇正文</button><button className="primary-btn" type="button" disabled={!preview.canApply || applying} onClick={() => onApply({ baseVersionNo: preview.baseVersionNo, sections: items })}>{applying ? "拆分中" : "确认拆分"}</button></footer>
      </div>
    </div>
  );
}
