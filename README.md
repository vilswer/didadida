# DidaDida 视频平台

> 一个仿 B 站风格的分布式视频平台，支持视频播放、弹幕、评论等核心互动功能。

## 项目概述

DidaDida 采用微服务架构构建，后端基于 Spring Boot 3.0+ 和 Spring Cloud Alibaba 技术栈，提供完整的视频平台解决方案。

## 技术选型

表格



| 组件     | 技术                 | 说明               |
| :------- | :------------------- | :----------------- |
| 后端框架 | Spring Boot 3.0+     | 核心开发框架       |
| 微服务   | Spring Cloud Alibaba | 服务治理           |
| 注册中心 | Nacos                | 服务发现与配置管理 |
| 限流熔断 | Sentinel             | 流量防护           |
| 数据库   | MySQL + MyBatis-Plus | 数据持久化         |
| 缓存     | Redis                | 热点数据缓存       |
| 消息队列 | Kafka                | 异步解耦           |
| 对象存储 | MinIO                | 视频文件存储       |
| API 文档 | Knife4j              | 接口文档生成       |

## 服务模块

文本



```
1didadida/
2├── didadida-common       # 公共模块（工具类、统一响应）
3├── didadida-user         # 用户服务（8001）
4├── didadida-video        # 视频服务（8002）
5├── didadida-danmaku      # 弹幕服务（8003）
6├── didadida-interaction  # 互动服务（8004）
7├── didadida-gateway      # 网关服务（8000）
8└── pom.xml               # 父工程配置
```

### 模块职责

表格



| 服务        | 端口 | 核心功能                 |
| :---------- | :--- | :----------------------- |
| gateway     | 8000 | 统一入口、路由转发、限流 |
| user        | 8001 | 注册登录、用户信息管理   |
| video       | 8002 | 视频上传、播放、互动数据 |
| danmaku     | 8003 | 弹幕发送与拉取           |
| interaction | 8004 | 评论、回复、点赞         |

## 环境要求

- JDK 11+
- MySQL 5.7+
- Redis 6.0+
- Kafka 2.8+
- MinIO RELEASE.2023-01-18T04-36-03Z+
- Nacos 2.0+

## 快速启动

### 1. 数据库初始化

bash



```
1# 创建表结构
2mysql -u root -p < script.sql
3
4# 导入测试数据
5mysql -u root -p < insert_test_data.sql
```

### 2. 配置调整

修改各服务的配置文件：

- `bootstrap.yml` - 配置 Nacos 地址
- `application.yml` - 配置数据库、Redis、Kafka 连接信息

### 3. 启动顺序

文本



```
11. Nacos
22. MySQL → Redis → Kafka → MinIO
33. 微服务（user → video → danmaku → interaction）
44. gateway
```

### 4. 访问接口文档

表格



| 服务     | 地址                           |
| :------- | :----------------------------- |
| 用户服务 | http://localhost:8001/doc.html |
| 视频服务 | http://localhost:8002/doc.html |
| 弹幕服务 | http://localhost:8003/doc.html |
| 互动服务 | http://localhost:8004/doc.html |

## 架构设计

### 核心特性

- **高并发**：Redis 缓存 + Kafka 异步 + Sentinel 限流
- **高可用**：服务降级 + 消息队列保证数据一致性
- **安全性**：密码加密 + 验证码防刷 + 接口权限控制

### 架构示意

文本



```
1前端应用 → 网关 → 微服务集群 → (MySQL/Redis/Kafka/MinIO)
2                ↓
3             Nacos (注册/配置)
```

## 开发规范

- 代码风格：遵循 Java 编码规范，使用 Lombok 简化样板代码
- 命名规范：包名小写，类名大驼峰，方法/变量小驼峰
- 接口设计：RESTful 风格，统一响应格式
- 日志记录：关键业务操作必须记录日志

## 注意事项

1. 启动前确保所有依赖服务正常运行
2. 生产环境需配置 HTTPS 及更严格的安全策略
3. 建议接入 Prometheus + Grafana 进行监控告警
4. 根据实际负载调整中间件配置参数