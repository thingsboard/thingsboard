\set ON_ERROR_STOP on

SELECT format('CREATE ROLE thing_model LOGIN PASSWORD %L', :'thing_model_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'thing_model')
\gexec

SELECT format('ALTER ROLE thing_model WITH LOGIN PASSWORD %L', :'thing_model_password')
WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'thing_model')
\gexec

SELECT 'CREATE DATABASE thingmodel OWNER thing_model'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'thingmodel')
\gexec

SELECT 'ALTER DATABASE thingmodel OWNER TO thing_model'
WHERE EXISTS (SELECT 1 FROM pg_database WHERE datname = 'thingmodel')
\gexec
