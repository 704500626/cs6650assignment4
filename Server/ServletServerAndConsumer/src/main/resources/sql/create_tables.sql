CREATE DATABASE IF NOT EXISTS UPIC;

USE UPIC;

CREATE TABLE IF NOT EXISTS Resorts (
    resort_id INT PRIMARY KEY,
    resort_name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS Skiers (
    skier_id INT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS Lifts (
    lift_id SMALLINT,
    resort_id INT NOT NULL,
    PRIMARY KEY (resort_id, lift_id),
    FOREIGN KEY (resort_id) REFERENCES Resorts(resort_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Seasons (
     season_id CHAR(4),
     resort_id INT NOT NULL,
     PRIMARY KEY (resort_id, season_id),
     FOREIGN KEY (resort_id) REFERENCES Resorts(resort_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS LiftRides (
    ride_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skier_id INT NOT NULL,
    resort_id INT NOT NULL,
    season_id CHAR(4) NOT NULL,
    day_id SMALLINT NOT NULL,
    lift_id SMALLINT NOT NULL,
    ride_time SMALLINT NOT NULL, -- representing minutes or seconds from day start
    FOREIGN KEY (skier_id) REFERENCES Skiers(skier_id) ON DELETE CASCADE,
    FOREIGN KEY (resort_id, lift_id) REFERENCES Lifts(resort_id, lift_id) ON DELETE CASCADE,
    FOREIGN KEY (resort_id, season_id) REFERENCES Seasons(resort_id, season_id) ON DELETE CASCADE
);

-- Crucial Indices for performance:
CREATE INDEX idx_resort_season_day ON LiftRides (resort_id, season_id, day_id);
CREATE INDEX idx_skier_resort_season_day ON LiftRides (skier_id, resort_id, season_id, day_id);