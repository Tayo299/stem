ALTER TABLE exam_attempts
    ALTER COLUMN score TYPE NUMERIC(8, 2) USING score::NUMERIC(8, 2),
    ALTER COLUMN possible_points TYPE NUMERIC(8, 2) USING possible_points::NUMERIC(8, 2);
