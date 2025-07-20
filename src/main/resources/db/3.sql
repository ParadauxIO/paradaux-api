-- Went to production on 20/7/2025
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

-- Separate tables for different data sources since they have different network ranges
CREATE TABLE geoip.city_block
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
    is_anycast                     BOOLEAN
);

CREATE TABLE geoip.asn_block
(
    network                  CIDR PRIMARY KEY,
    autonomous_system_number INTEGER REFERENCES geoip.autonomous_system (autonomous_system_number)
);

-- Indexes for performance
CREATE INDEX idx_city_block_network ON geoip.city_block USING GIST (network inet_ops);
CREATE INDEX idx_asn_block_network ON geoip.asn_block USING GIST (network inet_ops);
CREATE INDEX idx_city_block_geoname ON geoip.city_block (geoname_id);

-- Function to get complete IP information
CREATE OR REPLACE FUNCTION geoip.get_ip_info(ip_address INET)
    RETURNS TABLE
            (
                network          CIDR,
                geoname_id       INTEGER,
                city_name        TEXT,
                country_name     TEXT,
                latitude         DOUBLE PRECISION,
                longitude        DOUBLE PRECISION,
                asn_number       INTEGER,
                asn_organization TEXT
            )
AS
$$
BEGIN
    RETURN QUERY
        WITH city_data AS (SELECT cb.network,
                                  cb.geoname_id,
                                  cb.latitude,
                                  cb.longitude,
                                  l.city_name,
                                  l.country_name
                           FROM geoip.city_block cb
                                    LEFT JOIN geoip.location l ON cb.geoname_id = l.geoname_id
                           WHERE cb.network >>= ip_address
                           ORDER BY masklen(cb.network) DESC
                           LIMIT 1),
             asn_data AS (SELECT ab.autonomous_system_number, aut.autonomous_system_organization
                          FROM geoip.asn_block ab
                                   LEFT JOIN geoip.autonomous_system aut
                                             ON ab.autonomous_system_number = aut.autonomous_system_number
                          WHERE ab.network >>= ip_address
                          ORDER BY masklen(ab.network) DESC
                          LIMIT 1)
        SELECT city_data.network,
               city_data.geoname_id,
               city_data.city_name,
               city_data.country_name,
               city_data.latitude,
               city_data.longitude,
               asn_data.autonomous_system_number,
               asn_data.autonomous_system_organization
        FROM city_data
                 FULL OUTER JOIN asn_data ON true;
END;
$$ LANGUAGE plpgsql;