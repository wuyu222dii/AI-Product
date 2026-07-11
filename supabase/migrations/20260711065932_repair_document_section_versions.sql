-- Keep the section pointer aligned with the highest persisted history version.
-- This is idempotent and repairs rows left inconsistent by older write paths.
with latest_versions as (
    select section_id, max(version_no) as max_version_no
    from public.document_section_versions
    group by section_id
)
update public.document_sections as section
set version_no = latest.max_version_no
from latest_versions as latest
where section.id = latest.section_id
  and section.version_no < latest.max_version_no;
