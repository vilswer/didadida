# 数据库层配置文档

## 1. 接口文档配置

### 1.1 Knife4j接口文档配置

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs


knife4j:
  enable: true
  setting:
    language: zh_cn
```

## 2. Sentinel限流配置

### 2.1 Sentinel配置

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: ${SENTINEL_DASHBOARD:127.0.0.1:8080} # 可选
      eager: true
```

## 3. 文件上传配置

### 3.1 文件上传大小限制

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 1GB  # 单个文件最大大小
      max-request-size: 1GB  # 整个请求最大大小
```

## 4. 数据源配置

### 1.1 MySQL数据库配置

```yaml
spring:
  # 数据源配置
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://your-mysql-ip:3306/didadida?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: your-username
    password: your-password
    # Druid 连接池配置
    druid:
      initial-size: 20  # 初始连接数
      min-idle: 10  # 最小空闲连接数
      max-active: 100  # 最大连接数
      max-wait: 5000ms  # 最大等待时间
      test-while-idle: true  # 空闲时测试连接
      test-on-borrow: false  # 借用时测试连接
      test-on-return: false  # 归还时测试连接
      validation-query: SELECT 1  # 验证查询语句
      time-between-eviction-runs-millis: 60000  # 两次清理间隔
      min-evictable-idle-time-millis: 300000  # 最小空闲时间
      web-stat-filter:
        enabled: true
        url-pattern: /*
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        reset-enable: false
```

## 2. Redis配置

### 2.1 Redis连接池配置

```yaml
spring:
  # Redis 配置
  data:
    redis:
      host: 192.168.116.132
      port: 6379
      database: 0
      timeout: 5000ms  # 连接超时时间
      lettuce:
        pool:
          max-active: 200  # 最大连接数
          max-wait: 1000ms  # 最大等待时间
          max-idle: 50  # 最大空闲连接数
          min-idle: 10  # 最小空闲连接数
```

## 3. Kafka配置

### 3.1 Kafka消息队列配置

```yaml
spring:
  # Kafka 配置 (视频、弹幕、互动服务需要，用户服务可不配)
  kafka:
    bootstrap-servers: 192.168.116.132:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      retries: 3  # 重试次数
    consumer:
      group-id: didadida-video-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest  # 自动偏移量重置
```

## 4. MyBatis-Plus配置

### 4.1 MyBatis-Plus基础配置

```yaml
# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml  # Mapper XML文件位置
  type-aliases-package: com.didadida.*.entity  # 实体类包路径
  configuration:
    map-underscore-to-camel-case: true  # 下划线转驼峰
    cache-enabled: false  # 关闭缓存
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发环境打印SQL
  global-config:
    db-config:
      id-type: auto  # 主键自增
      logic-delete-field: deleted  # 逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 5. 服务器配置

### 5.1 Tomcat服务器配置

```yaml
server:
  port: 8002  # 视频服务端口
  tomcat:
    threads:
      max: 500  # 最大线程数
      min-spare: 100  # 最小空闲线程数
    connection-timeout: 10000  # 连接超时时间（毫秒）
    max-connections: 20000  # 最大连接数
    accept-count: 1000  # 等待队列长度
    keep-alive-timeout: 60000  # 保持连接超时时间（毫秒）
```

## 6. MinIO配置

### 6.1 MinIO对象存储配置

```yaml
# MinIO 配置 (仅视频服务需要)
minio:
  endpoint: http://your-minio-ip:9002
  accessKey: your-access-key
  secretKey: your-secret-key
  bucketName: your-bucket-name
```

## 7. 配置使用说明

### 7.1 Nacos配置方式

1. 登录Nacos控制台
2. 进入配置管理 -> 配置列表
3. 点击 "+" 按钮，创建新配置
4. 填写配置信息：
   - 数据ID: didadida-video-datasource.yml
   - 组: DIDADIDA_GROUP
   - 配置格式: YAML
   - 配置内容: 复制上述配置到内容框中
5. 点击 "发布" 按钮，完成配置

### 7.2 服务启动配置

在服务的 bootstrap.yml 文件中，添加以下配置：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: 192.168.2.4:8848
        namespace: dev
        group: DIDADIDA_GROUP
        file-extension: yml
  config:
    import:
      - nacos:didadida-video-datasource.yml?group=DIDADIDA_GROUP&refresh=true
```

这样，服务启动时会自动从Nacos加载配置信息。

## 8. 性能优化建议

1. **数据库连接池优化**：根据实际并发量调整连接池大小，避免连接数过多或过少
2. **Redis连接池优化**：根据缓存访问量调整连接池大小
3. **Kafka优化**：根据消息量调整生产者和消费者配置
4. **服务器优化**：根据服务器硬件配置调整Tomcat线程数和连接数
5. **SQL优化**：使用索引，优化查询语句，避免全表扫描
6. **缓存策略**：合理使用缓存，减少数据库访问次数
7. **异步处理**：将耗时操作异步处理，提高响应速度
8. **监控告警**：设置数据库、Redis、Kafka的监控告警，及时发现问题

## 9. 故障排查

### 9.1 数据库连接问题

- 检查数据库服务是否正常运行
- 检查数据库连接字符串是否正确
- 检查数据库用户名和密码是否正确
- 检查数据库防火墙是否开放了端口

### 9.2 Redis连接问题

- 检查Redis服务是否正常运行
- 检查Redis连接地址和端口是否正确
- 检查Redis密码是否正确
- 检查Redis防火墙是否开放了端口

### 9.3 Kafka连接问题

- 检查Kafka服务是否正常运行
- 检查Kafka连接地址和端口是否正确
- 检查Kafka主题是否存在
- 检查Kafka防火墙是否开放了端口

### 9.4 性能问题

- 检查数据库慢查询
- 检查Redis缓存命中率
- 检查服务器CPU和内存使用情况
- 检查网络延迟

通过以上配置和优化，可以提高系统的性能和稳定性，减少TTFB时间，提升用户体验。