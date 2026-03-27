CREATE COLLATION IF NOT EXISTS ignore_accents (
    provider = icu,
    locale = 'und-u-ks-level1-kc-true',
    deterministic = false
);