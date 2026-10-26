UPDATE sponsors
SET name = 'Monika', updated_at = now()
WHERE lower(name) = 'monica';

UPDATE sponsors
SET active = FALSE, updated_at = now()
WHERE lower(name) = 'sosa';

INSERT INTO sponsors (id, name, organization, display_order, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Shamaden Imole Charity Foundation', 'Shamaden Imole Charity Foundation',
       1, TRUE, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM sponsors WHERE lower(name) = lower('Shamaden Imole Charity Foundation')
);

UPDATE sponsors
SET display_order = CASE
    WHEN lower(name) = lower('Shamaden Imole Charity Foundation') THEN 1
    WHEN lower(name) = lower('Monika') THEN 2
    ELSE display_order
END,
updated_at = now()
WHERE lower(name) IN (lower('Shamaden Imole Charity Foundation'), lower('Monika'));
