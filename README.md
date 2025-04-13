# Cs6650assignment4

# Client Setup

To configure the API end point for the load test, the relevant information is stored in `cs6650assignment3/Client/src/main/resources/config.properties`.

Since we will test EC2 instances, local should be false. If you want to test springboot, you should set springboot=true, otherwise it assumes testing a servlet.

For URL part, please replace it with http://{your ec2 ip}:8080/{web app name}

The following is the sample of the file:

```properties
local=false
springboot=false
local.servlet.url=http://127.0.0.1:8080/cs6650Server_war_exploded
local.springboot.url=http://127.0.0.1:8080
remote.servlet.url=http://ServletELB-502283295.us-west-2.elb.amazonaws.com:8080/cs6650Server_war
remote.springboot.url=http://44.247.153.18:8080
```

For my servlet, the war file name is cs6650Server_war.war, so here I used cs6650Server_war. And then you can run any of the main() in clientpart1 or clientpart2.

# Server Setup

Under `cs6650assignment3/Server`, cs6650Server_war.war is the WAR file after I compile and package the servlet project. Simply copy it to the webapps directory under your tomcat directory and restart it.

server.xml is the configuration that I used for my tomcat server running on a t2.micro EC2 instance. The main difference from the default settings is the following part:

```XML
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443"
               maxParameterCount="1000"
               maxThreads="1500"
               acceptCount="1000"
               maxKeepAliveRequests="-1"
               keepAliveTimeout="5000"
               minSpareThreads="100"
               enableLookups="false"
               />
```

cs6650Consumer.jar is the JAR file after I compile and package the consumer program. To deploy it on your EC2 instance, use the following command:

```Bash
java -jar cs6650Consumer.jar
```

I installed RabbitMQ on one of my EC2 instance. Not too much configuration but the following one line is added to let it make better use of the RAM.

```Properties
vm_memory_high_watermark.relative = 0.8
```

Inside config.properties you can modify RABBITMQ_HOST(remember to use the private IP), RABBITMQ_USERNAME and RABBITMQ_PASSWORD to connect to your own RMQ. The same goes for MYSQL.

```
local=false
RABBITMQ_HOST=172.31.26.212
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin
EXCHANGE_NAME=skiers_exchange
ROUTING_KEY=skiers.route
QUEUE_NAME=skiers_queue

MYSQL_URL=jdbc:mysql://database-2.cluster-ctou2s6cq6li.us-west-2.rds.amazonaws.com:3306/UPIC
MYSQL_USERNAME=admin
MYSQL_PASSWORD=adminadmin
MYSQL_INSERT_SQL=INSERT INTO LiftRides (skier_id, resort_id, season_id, day_id, lift_id, ride_time) VALUES (?, ?, ?, ?, ?, ?)
MYSQL_BATCH_SIZE=50
MYSQL_FLUSH_INTERVAL_MS=100

EVENT_QUEUE_SIZE=5000
WORKER_POOL_SIZE=20

MAX_QUEUED_MSG=700
CIRCUIT_BREAKER_THRESHOLD=3000
CIRCUIT_BREAKER_TIMEOUT_MS=2000
QUEUE_MONITOR_THREAD_COUNT=10
QUEUE_MONITOR_INTERVAL_MS=50
MAX_BACKOFF_MS=10000

MAX_TOKENS=100
REFILL_RATE_PER_SECOND=1600

THREAD_POOL_SIZE=500
MAX_CHANNELS=200
MIN_CONSUMERS=100
MAX_CONSUMERS=2000
MAX_RETRIES=5
INITIAL_RETRY_DELAY_MS=200
REQUEST_HEART_BEAT=30
NUM_CHANNELS_PER_CONNECTION=50
NUM_CONNECTIONS=1
NUM_QUEUES=1
NUM_CHANNELS_PER_QUEUE=50
CHANNEL_POOL_SIZE=100
PREFETCH_COUNT=20
RABBITMQ_BATCH_SIZE=10
RABBITMQ_FLUSH_INTERVAL_MS=100
```