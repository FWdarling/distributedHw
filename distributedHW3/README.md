## 分布式系统作业3

----



```
github链接： https://github.com/FWdarling/distributedHw/tree/main/distributedHW3
```



### 作业要求

（与作业2进行集成）

 设计和实现一个分布式组成员服务(distributed group membership service)，可以使得N个存储虚拟机形成一个分布式组系统，每个存储虚拟机通过一个后台进程（daemon），维护其他组成员列表（membershiplist），该列表维护所有在线(live)且连接到该组的节点成员ID。支持节点join、leave、failure-rejoin时动态显式组成员列表
 通过heart-heating和gossip这两个机制进行故障检测，在进行故障检测的进程中，可以考虑这些虚拟机组成员所形成的拓扑结构。在实现该组服务，要求带宽开销较小，比如避免使用多到多N*N的ping消息来侦测节点失效。为了确保带宽有效性，必须通过UDP网络协议来完成。

### 实现

#### 整体架构

本次作业将所有节点共有的一些属性和功能放在了Daemon类中， 又分别实现了引荐人introducer和普通节点Node类， 继承于Daemon类。 结点心跳采用gossip思想， 减小了带宽开销， 所有消息的监听和处理都使用了多线程来实现， 以应对当结点数量变多时， 泛洪带来的高并发请求处理需求。

#### 节点join && failuer-rejoin

当有新节点加入时， 该节点与引荐人节点建立tcp连接进行通信， 引荐人节点会将目前组里的所有成员信息通过刚才的tcp连接发给新节点， 同时引荐人会将新节点加入的信息使用udp广播给组内所有成员

考虑到当一个节点断线重连时， 可能会出现有的节点还未更新节点断线， 就收到了该节点重新加入的信息的情况， 作业中采用ip+port作为唯一标识， 同时添加了timestamp用于标记节点的最新信息

#### 节点leave

当有节点主动离开时， 该节点会广播给组内所有成员一条信息“我已经离开”， 让其他成员更新自己的节点信息， 而后晴空自己的成员表

#### 节点crush

为了解决节点因为不可控原因导致的被动离开或崩溃， 本作业采用了heart-beating心跳以及定期check来实现时间约束完整性， 即组成员能够动态获取最新的成员信息， 去除掉崩溃的节点。

对于心跳机制， 采用了gossip， 每隔一段时间随机选择自己的几个邻居节点发送自己alive的信息， 避免了完全泛洪带来的大量贷款开销， 同时根据gossip， 保证了最终所有结点都能收到该信息， 节点收到信息后会更新自己的成员表， 把对应节点的时间更新为最新的时间

对于check， 每隔一段时间节点会查询自己的成员表， 如果有成员最新消息的时间和当前时间的差距过大， 则认为该节点已经崩溃， 删除本地成员表中的该节点， 同时广播该节点崩溃的信息

#### 消息格式（摘自源代码注释）

##### 对于所有结点的广播信息：

```java
/* @param msg
 * "add_{ip and port}_{time}"
 * "del_{ip and port}"
 * "H-B_{ip and port}_{time}"
 */
```

##### 对于引荐人和节点之间的tcp信息：

```java
/* @param msg
 * "join_{ip and port}_{time}"
 * "leave_{ip and port}"
 * {ip}:{port}-{time}_{ip}:{port}-{time}_...
 */
```

#### 性能优化

本作业中涉及的消息监听全都开在新的线程中， 保证了消息能够被及时处理， 同时对于某些复杂的信息处理过程， 也使用了多线程异步事件进行优化。

### 使用

1.  将server目录拷贝到每一个查询服务器（实体或虚拟的存储机）上(Node.java只有普通结点需要且只需要一份， 重复的文件是为了单机多端口模拟多个端， 根据实际情况进行删减)

2.  将一个服务器选作introducer节点， 修改该机器上的daemon/Introducer.java代码中的main函数， 将introducer构造函数参数列表中的ip和端口改成本机的ip和两个空闲端口（分别为udp和tcp端口）， 同时也可以修改心跳和检测的开始时间和周期（代码如下）

    ```java
    Introducer introducer = new Introducer("127.0.0.1", 11111, 11112);
    timer1.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    introducer.heartBeating();
                }
            }, 5000, 5000);
    
            Timer timer2 = new Timer();
            timer2.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    introducer.check();
                }
            }, 20000, 20000);
    ```

3.  其他服务器均为node节点， 同理修改所有机器上的daemon/Node.java代码中的main函数， 将Node构造函数参数列表中的第一对ip和port改为introducer机器的ip和tcp port， 第二对改成本机的ip和任意空闲端口， 同时也可以修改心跳和检测的开始时间和周期（代码如下）

    ```java
    Node node = new Node("127.0.0.1", 12111, "127.0.0.1", 11112);
            Timer timer1 = new Timer();
            timer1.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    node.heartBeating();
                }
            }, 5000, 5000);
    
            Timer timer2 = new Timer();
            timer2.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    node.check();
                }
            }, 20000, 20000);
    ```

4.  先运行introducer机器上的main， 在运行每个node节点上的main， 会有提示信息输出到标准输出， 节点更新信息也会放在日志中

