\set ON_ERROR_STOP on

SELECT format('CREATE ROLE thing_model LOGIN PASSWORD %L', :'thing_model_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'thing_model')
\gexec

SELECT format('ALTER ROLE thing_model WITH LOGIN PASSWORD %L', :'thing_model_password')
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'thing_model')
\gexec

CREATE SCHEMA IF NOT EXISTS thingmodel AUTHORIZATION thing_model;
ALTER SCHEMA thingmodel OWNER TO thing_model;
GRANT USAGE, CREATE ON SCHEMA thingmodel TO thing_model;

SELECT format('GRANT CONNECT ON DATABASE %I TO thing_model', current_database())
\gexec
