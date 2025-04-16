SELECT SUM(10 * lift_id) AS total_vertical
FROM LiftRides
WHERE skier_id = ?
  AND resort_id = ?
  AND season_id = ?
  AND day_id = ?;