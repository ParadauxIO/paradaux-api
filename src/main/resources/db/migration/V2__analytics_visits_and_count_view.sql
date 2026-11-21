CREATE SCHEMA IF NOT EXISTS analytics;
ALTER TABLE ifum.visits SET SCHEMA analytics;
ALTER TABLE analytics.visits ADD COLUMN project TEXT NOT NULL DEFAULT 'ifuckedur.mom';
ALTER TABLE analytics.visits ALTER COLUMN project DROP DEFAULT;
ALTER TABLE analytics.visits DROP CONSTRAINT IF EXISTS visits_ip_address_key;
ALTER TABLE analytics.visits ADD CONSTRAINT visits_ip_project_unique UNIQUE (ip_address, project);
CREATE MATERIALIZED VIEW analytics.visit_count_view AS
SELECT
    project,
    COUNT(*) AS total_count
FROM
    analytics.visits
GROUP BY
    project;
CREATE UNIQUE INDEX visit_count_view_project_idx ON analytics.visit_count_view(project);
DROP SCHEMA IF EXISTS ifum CASCADE;