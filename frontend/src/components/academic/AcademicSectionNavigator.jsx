import {
  DndContext,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  closestCenter,
  useSensor,
  useSensors
} from "@dnd-kit/core";
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { CheckCircle2, Circle, GripVertical, Plus } from "lucide-react";

export function AcademicSectionNavigator({
  sections,
  selectedSectionId,
  onSelect,
  onCreate,
  onReorder,
  reordering = false
}) {
  const completed = sections.filter((item) => item.content?.trim()).length;
  const progress = sections.length === 0 ? 0 : Math.round((completed / sections.length) * 100);
  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 6 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 180, tolerance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  function handleDragEnd({ active, over }) {
    if (reordering || !over || active.id === over.id) return;
    const oldIndex = sections.findIndex((section) => section.id === active.id);
    const newIndex = sections.findIndex((section) => section.id === over.id);
    if (oldIndex < 0 || newIndex < 0) return;
    onReorder(arrayMove(sections, oldIndex, newIndex).map((section) => section.id));
  }

  return (
    <aside className="academic-section-rail">
      <div className="academic-rail-head">
        <div>
          <span>章节树</span>
          <strong>{completed}/{sections.length} 已形成正文</strong>
        </div>
        <button className="icon-btn" type="button" onClick={onCreate} title="新增章节" aria-label="新增章节">
          <Plus size={17} aria-hidden="true" />
        </button>
      </div>
      <div className="academic-progress-track" aria-label={`章节完成度 ${progress}%`}>
        <span style={{ width: `${progress}%` }} />
      </div>
      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
        <SortableContext items={sections.map((section) => section.id)} strategy={verticalListSortingStrategy}>
          <nav className="academic-section-list" aria-label="文档章节">
            {sections.map((section, index) => (
              <SortableSectionItem
                key={section.id}
                section={section}
                index={index}
                active={section.id === selectedSectionId}
                disabled={reordering}
                onSelect={onSelect}
              />
            ))}
          </nav>
        </SortableContext>
      </DndContext>
    </aside>
  );
}

function SortableSectionItem({ section, index, active, disabled, onSelect }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging
  } = useSortable({ id: section.id, disabled });
  const hasContent = Boolean(section.content?.trim());
  const style = {
    transform: CSS.Transform.toString(transform),
    transition
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`academic-section-row ${active ? "is-active" : ""} ${isDragging ? "is-dragging" : ""}`}
    >
      <button
        className="academic-section-drag-handle"
        type="button"
        aria-label={`拖动章节 ${section.title}`}
        title="拖动调整章节顺序"
        disabled={disabled}
        {...attributes}
        {...listeners}
      >
        <GripVertical size={16} aria-hidden="true" />
      </button>
      <button
        className={`academic-section-item ${active ? "is-active" : ""}`}
        type="button"
        onClick={() => onSelect(section)}
      >
        {hasContent ? <CheckCircle2 size={16} aria-hidden="true" /> : <Circle size={16} aria-hidden="true" />}
        <span>
          <small>{String(index + 1).padStart(2, "0")} · {section.sectionType}</small>
          <strong>{section.title}</strong>
        </span>
        <em>v{section.versionNo}</em>
      </button>
    </div>
  );
}
