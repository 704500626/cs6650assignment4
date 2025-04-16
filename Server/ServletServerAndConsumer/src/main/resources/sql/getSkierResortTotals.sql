-- Case A: With both resort and season
SELECT season_id, SUM(lift_id * 10) AS total_vertical
FROM LiftRides
WHERE skier_id = ?
  AND resort_id = ?
  AND season_id = ?
GROUP BY season_id;

-- Case B: With resort only (no season param)
SELECT season_id, SUM(lift_id * 10) AS total_vertical
FROM LiftRides
WHERE skier_id = ?
  AND resort_id = ?
GROUP BY season_id;