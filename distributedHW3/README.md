## 分布式系统作业3

----



### 作业要求

（与作业2进行集成）

 设计和实现一个分布式组成员服务(distributed group membership service)，可以使得N个存储虚拟机形成一个分布式组系统，每个存储虚拟机通过一个后台进程（daemon），维护其他组成员列表（membershiplist），该列表维护所有在线(live)且连接到该组的节点成员ID。支持节点join、leave、failure-rejoin时动态显式组成员列表
 通过heart-heating和gossip这两个机制进行故障检测，在进行故障检测的进程中，可以考虑这些虚拟机组成员所形成的拓扑结构。在实现该组服务，要求带宽开销较小，比如避免使用多到多N*N的ping消息来侦测节点失效。为了确保带宽有效性，必须通过UDP网络协议来完成。



