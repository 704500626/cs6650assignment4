SELECT SUM(10 * lift_id)
FROM LiftRides
WHERE skier_id = ?
  AND resort_id = ?
  AND season_id = ?;