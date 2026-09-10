-- Search filters and the price sort each get an index; without them the
-- listing endpoint degrades to a sequential scan as the table grows.
CREATE INDEX ix_kosts_owner_deleted ON kosts (owner_id, deleted_at);
CREATE INDEX ix_kosts_price ON kosts (price_per_month);
CREATE INDEX ix_kosts_city_active ON kosts (address_city, is_active, deleted_at);
CREATE INDEX ix_kosts_created_at ON kosts (created_at DESC);

-- Case-insensitive LIKE only uses an index if the index matches the expression.
CREATE INDEX ix_kosts_name_lower ON kosts (LOWER(name));
CREATE INDEX ix_kosts_city_lower ON kosts (LOWER(address_city));
CREATE INDEX ix_kosts_district_lower ON kosts (LOWER(address_district));
