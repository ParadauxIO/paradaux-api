SELECT
    os,
    COUNT(*) AS count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) AS percentage_share
FROM (
         SELECT
             CASE

                 WHEN user_agent ~* 'Windows NT 10.0' THEN 'Windows'
                 WHEN user_agent ~* 'Windows NT 6.3' THEN 'Windows'
                 WHEN user_agent ~* 'Windows NT 6.2' THEN 'Windows'
                 WHEN user_agent ~* 'Windows NT 6.1' THEN 'Windows'
                 WHEN user_agent ~* 'Windows NT 6.0' THEN 'Windows'
                 WHEN user_agent ~* 'Windows NT 5.1' THEN 'Windows'
                 WHEN user_agent ~* 'iPhone|iPad' THEN 'iOS'
                 WHEN user_agent ~* 'Macintosh|Mac OS X' THEN 'macOS'
                 WHEN user_agent ~* 'Android' THEN 'Android'
                 WHEN user_agent ~* 'Linux' THEN 'Linux'
                 ELSE 'Other/Unknown'
                 END AS os
         FROM analytics.visits
     ) AS derived
GROUP BY os
ORDER BY count DESC;

REFRESH MATERIALIZED VIEW CONCURRENTLY analytics.visit_count_view;

-- Get the IP/User-Agent pairs for the project 'ifuckedur.mom' with their viewer number
SELECT ROW_NUMBER() OVER (ORDER BY id) AS row_number, ip_address, user_agent
FROM analytics.visits
WHERE project = 'ifuckedur.mom';

-- Get visitor counts for each project in the last 24 hours
SELECT
    project,
    COUNT(*) AS total_count
FROM
    analytics.visits
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '1 day'
GROUP BY
    project;

-- Get visitor counts for each project in the last 7 days
SELECT
    TO_CHAR(created_at, 'FMDay') AS day_of_week,
    COUNT(*) FILTER (WHERE project = 'cans.ie') AS cans_ie,
    COUNT(*) FILTER (WHERE project = 'ifuckedur.mom') AS ifuckedur_mom,
    COUNT(*) FILTER (WHERE project = 'isbetterthandubl.in') AS isbetterthandublin
FROM
    analytics.visits
WHERE
    created_at >= CURRENT_DATE - INTERVAL '6 days'
GROUP BY
    day_of_week,
    EXTRACT(DOW FROM created_at)
ORDER BY
    EXTRACT(DOW FROM created_at);