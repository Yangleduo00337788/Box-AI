UPDATE plugin_catalog
SET review_status = 'APPROVED',
    status = 'LISTED',
    visibility = 'WORKSPACE',
    updated_at = NOW()
WHERE deleted = 0
  AND source_type = 'USER';
