SELECT COUNT(DISTINCT skier_id)
FROM LiftRides
WHERE resort_id = ?
  AND season_id = ?
  AND day_id = ?;