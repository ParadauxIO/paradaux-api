CREATE SCHEMA IF NOT EXISTS geoip;

CREATE TABLE geoip.location
(
    geoname_id             INTEGER PRIMARY KEY,
    locale_code            TEXT,
    continent_code         TEXT,
    continent_name         TEXT,
    country_iso_code       TEXT,
    country_name           TEXT,
    subdivision_1_iso_code TEXT,
    subdivision_1_name     TEXT,
    subdivision_2_iso_code TEXT,
    subdivision_2_name     TEXT,
    city_name              TEXT,
    metro_code             TEXT,
    time_zone              TEXT,
    is_in_european_union   BOOLEAN
);

CREATE TABLE geoip.autonomous_system
(
    autonomous_system_number       INTEGER PRIMARY KEY,
    autonomous_system_organization TEXT
);

CREATE TABLE geoip.ip_block
(
    network                        CIDR PRIMARY KEY,
    geoname_id                     INTEGER REFERENCES geoip.location (geoname_id),
    registered_country_geoname_id  INTEGER,
    represented_country_geoname_id INTEGER,
    is_anonymous_proxy             BOOLEAN,
    is_satellite_provider          BOOLEAN,
    postal_code                    TEXT,
    latitude                       DOUBLE PRECISION,
    longitude                      DOUBLE PRECISION,
    accuracy_radius                INTEGER,
    is_anycast                     BOOLEAN,
    autonomous_system_number       INTEGER REFERENCES geoip.autonomous_system (autonomous_system_number)
);

-- Optional index for faster geo lookups
CREATE INDEX idx_ip_block_network ON geoip.ip_block USING GIST (network inet_ops);