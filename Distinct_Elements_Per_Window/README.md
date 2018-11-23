# Count distinct elements per time window  
## Problem  
To solve the problem of calculating unique users per minute, day, week, month, year.  
For the first version of the application, business wants us to provide just the unique users per minute.  
## How it works  
Please follow the report document under `Report`  [folder,](https://github.com/mohamed-taman/Kafka-Work/tree/master/Distinct_Elements_Per_Window/Report) for all the requirements handling explanation, and how the application works internally.  
## System Requirements 
The following software pieces are required in order to run the application successfully. I have developed the application on **macOS Mojave**. and you can use **Linux** as well. 
### 1. Setup Java SE 8 (JDK 1.8 latest update)  
Go to Oracle website to download the JDK at this link [download from here](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).  
After download double click on `.dmg` file and follow the installation steps, it is pretty straightforward.
 
After finishing, make sure that Java is **accessible from everywhere**, by running the following command from your terminal:  
```bash 
>  java -version  
java version  "1.8.0_192"  
Java(TM)  SE Runtime Environment  (build 1.8.0_192-b12)  
Java HotSpot(TM)  64-Bit Server VM  (build 25.192-b12, mixed mode)  
```  
### 2. Setup Kafka  
Second important pice is **Kafka v2.0.1** ,  
[Download](https://www.apache.org/dyn/closer.cgi?path=/kafka/2.0.1/kafka_2.12-2.0.1.tgz  "Kafka downloads") the 2.0.1 release and un-tar it, to whatever location you like.  
```  bash  
1 > tar -xzf kafka_2.12-2.0.1.tgz  
2 > cd kafka_2.12-2.0.1  
```  
### 3. Setup Maven  
A maven is a build tool required in order to run our application easily, you can download it from [here](https://www-us.apache.org/dist/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.zip), and  un-zip  to any location you like.  
>  **Important:**  `mvn` command should be accessible from your system. 
Follow the following [instruction](https://maven.apache.org/install.html) for a full setup, and system configurations.  
### 4. Clone the project  
From any location in your system clone this repository as the following:  
```bash 
>  git  clone https://github.com/mohamed-taman/Kafka-Work.git  
>  cd  Kafka-Work/Distinct_Elements_Per_Window  
```  
## How to Run  
Kafka steps should run in order.  
### 1. Start Kafka  
As we have installed Kafka, open a **new terminal window** from inside `/kafka_2.12-2.0.1` folder and run the following command to  
1. Start the **Zookeeper server**.  
```bash  
>  bin/zookeeper-server-start.sh config/zookeeper.properties  
```  
3. And This command to start **Kafka Broker** in a **new terminal window** too: 
```bash  
>  bin/kafka-server-start.sh config/server.properties  
```  
### 2. Create topics  
open a **a third new terminal window** from inside `/kafka_2.12-2.0.1` folder and run the following command to:  
1. Creating **log-frames-topic**, you should see a success message after this command.  
```bash  
>  bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic log-frames-topic  
```  
2. Creating **users-count-topic**  
```bash  
>  bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic users-count-topic  
```  
3. Make sure they are created successfully.  
```bash  
>  bin/kafka-topics.sh --zookeeper localhost:2181 --describe  
```  
### 3. Start a consumer  
This will start a consumer on the results topic **users-count-topic**, open a **fourth terminal window** and run:  
```bash  
>  bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic users-count-topic --from-beginning  
```  
And it will start and nothing happens, as it is waiting for data to be ingested to be polled and printed. To see this in action lets run our **streamer application** and ingest some data with **producer**.  
### 4. Run Streamer Applications  
Now lets run our *Streamer application* with the following `mvn` command in a new terminal window pointing to cloned folder `Kafka-Work/Distinct_Elements_Per_Window`, and by default all processed data will be printed on the ***standard console*** and sent to ***users-count-topic***.  
1. This command allows you to fully configure the streamer.  
```bash  
>  mvn exec:java -Dexec.mainClass="rs.com.sirius.xi.kafka.tm.stream.LogFrameStreamer"  -Dexec.args="--reset --port;9092 --host;localhost --inTopic;log-frames-topic --outTopic;users-count-topic --printToConsole;true --printToTopic;true"  
```  
2. More easer using *maven profile* which is -re-configured with all default values, run the following  mvn  command:  
```bash  
>  mvn  install  -PStreamerApp  
```  
Now Streamer is waiting for data to be ingested into `log-frames-topic`, and this is **Producer** job. So let's ingest some data.  
> Note: Streamer Application could be terminated with **`CTRL+C`** keys.  
### 5. Using Kafka Producer  
The easiest way to ingest data from a file or sample data by running Producer shipped with Kafka as the following:  
1. To send data from a command line:  
```bash  
>  bin/kafka-console-producer.sh --broker-list localhost:9092 --topic log-frames-topic  
>  |  <Insert data and press ENTER key>  
>  
```  
2. To ingest a normal file  
```bash  
>  bin/kafka-console-producer.sh --broker-list localhost:9092 --topic log-frames-topic  <  path/to/data/DataExample.json  
>  
```  
3. To ingest a zipped file  
```bash  
>  gzcat path/to/data/stream.jsonl.gz  |  bin/kafka-console-producer.sh --broker-list localhost:9092 --topic log-frames-topic  
>  
```  
### 5.1 Using Application Producer  
For the testing purpose and easy to run producer, I have developed a configurable Producer and could be run as the following:  
>**Important Note** :  Producer works with unzipped data files.  
1. To run fully configured Producer user the following command:  
```bash  
>  mvn exec:java -Dexec.mainClass="rs.com.sirius.xi.kafka.tm.producer.Producer"  -Dexec.args="--file;'path/to/data/Sample.txt' --port;9092 --host;localhost --topic;'log-frames-topic'"  
```  
2. To run with pre-configured parameters profile, run the following:  
```bash  
>  mvn  install  -PProducer  
```  
Now After running this final piece of the application, you should see, *Kafka Consumer* and *Streamer* printing a JSON formatted messages with  the count of users per a specific window which is 1 minute for now.  
```javascript  
{"Window":"From: Tuesday,November 13,2018 10:40:00, To: Tuesday,November 13,2018 10:41:00","users":23}
{"Window":"From: Tuesday,November 13,2018 10:41:00, To: Tuesday,November 13,2018 10:42:00","users":27}  
```
