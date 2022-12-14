configs=$(pwd)/redis-sentinel

docker run -v $configs/redis-1.conf:/usr/local/etc/redis/redis.conf -v $configs/redis-sentinel.conf:/data/sentinel.conf -d --net=host --name master-1 redis redis-server /usr/local/etc/redis/redis.conf
docker run -v $configs/redis-2.conf:/usr/local/etc/redis/redis.conf -v $configs/redis-sentinel.conf:/data/sentinel.conf -d --net=host --name master-2 redis redis-server /usr/local/etc/redis/redis.conf
docker run -v $configs/redis-3.conf:/usr/local/etc/redis/redis.conf -v $configs/redis-sentinel.conf:/data/sentinel.conf -d --net=host --name master-3 redis redis-server /usr/local/etc/redis/redis.conf
docker run -v $configs/redis-4.conf:/usr/local/etc/redis/redis.conf -v $configs/redis-sentinel.conf:/data/sentinel.conf -d --net=host --name slave-1 redis redis-server /usr/local/etc/redis/redis.conf
docker run -v $configs/redis-5.conf:/usr/local/etc/redis/redis.conf -v $configs/redis-sentinel.conf:/data/sentinel.conf -d --net=host --name slave-2 redis redis-server /usr/local/etc/redis/redis.conf
docker run -v $configs/redis-6.conf:/usr/local/etc/redis/redis.conf -v $configs/redis-sentinel.conf:/data/sentinel.conf -d --net=host --name slave-3 redis redis-server /usr/local/etc/redis/redis.conf

docker exec -it master-1 redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 --cluster-replicas 1

docker exec -d -it master-1 redis-sentinel sentinel.conf
docker exec -d -it master-2 redis-sentinel sentinel.conf
docker exec -d -it master-3 redis-sentinel sentinel.conf
docker exec -d -it slave-1 redis-sentinel sentinel.conf
docker exec -d -it slave-2 redis-sentinel sentinel.conf
docker exec -d -it slave-3 redis-sentinel sentinel.conf