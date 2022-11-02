ALTER TABLE query_info ALTER COLUMN info TYPE jsonb USING info::jsonb;
