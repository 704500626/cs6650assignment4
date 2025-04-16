SELECT COUNT(DISTINCT skier_id) AS unique_skiers
FROM LiftRides
WHERE resort_id = ?
  AND season_id = ?
  AND day_id = ?;