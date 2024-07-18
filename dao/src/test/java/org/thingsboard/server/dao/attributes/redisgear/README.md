
docker run --network=host --name redis redislabs/redisgears:1.2.5-001

cat > /usr/local/redisgear/scripts/write_behind.py
redis-cli RG.PYEXECUTE "$(cat /usr/local/redisgear/scripts/write_behind_stream.py)"
redis-cli RG.PYEXECUTE "$(cat /usr/local/redisgear/scripts/write_behind.py)"
redis-cli RG.PYEXECUTE "$(cat /usr/local/redisgear/scripts/read_through.py)"
redis-cli 
get a
XADD dataStream * key "b" value "2"
XADD dataStream * key "c" value "3"

docker exec -it postgres
    
psql -U postgres
\c thingsboard

CREATE TABLE your_table (
key TEXT PRIMARY KEY,
value TEXT
);



