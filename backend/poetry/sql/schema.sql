CREATE DATABASE IF NOT EXISTS vibe_random_note CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE vibe_random_note;

CREATE TABLE IF NOT EXISTS poetry_author (
    id INT PRIMARY KEY AUTO_INCREMENT,
    source_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    original_name VARCHAR(100) NOT NULL,
    dynasty VARCHAR(10) NOT NULL,
    description MEDIUMTEXT,
    INDEX poetry_author_name (name)
);

CREATE TABLE IF NOT EXISTS poetry_poem (
    id INT PRIMARY KEY AUTO_INCREMENT,
    source_key VARCHAR(64) NOT NULL UNIQUE,
    dataset VARCHAR(20) NOT NULL,
    source_ref VARCHAR(512) NOT NULL,
    source_version VARCHAR(64) NOT NULL,
    author_id INT NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    title VARCHAR(300) NOT NULL,
    original_title VARCHAR(300) NOT NULL,
    rhythmic VARCHAR(100),
    dynasty VARCHAR(10) NOT NULL,
    kind VARCHAR(10) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    original_content MEDIUMTEXT NOT NULL,
    excerpt VARCHAR(1000),
    featured_rank INT UNIQUE,
    FOREIGN KEY (author_id) REFERENCES poetry_author(id),
    INDEX poetry_author_poems (author_id, id),
    INDEX poetry_filters (dynasty, kind, id),
    INDEX poetry_dataset (dataset, id)
);

CREATE TABLE IF NOT EXISTS poetry_favorite (
    user_id INT NOT NULL,
    poem_id INT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, poem_id),
    FOREIGN KEY (user_id) REFERENCES account_user(id) ON DELETE CASCADE,
    FOREIGN KEY (poem_id) REFERENCES poetry_poem(id) ON DELETE CASCADE
);
