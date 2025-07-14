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


SELECT ROW_NUMBER() OVER (ORDER BY id) AS row_number, ip_address, user_agent
FROM analytics.visits
WHERE project = 'ifuckedur.mom';