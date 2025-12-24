# CozyCoffee Backend - 快速启动指南

## 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0
- Nacos 2.2.3

## 启动步骤

### 1. 启动Nacos
```bash
cd nacos/bin
startup.cmd -m standalone  # Windows
# 访问 http://localhost:8848/nacos
# 默认账号密码: nacos/nacos
```

### 2. 初始化数据库
```sql
mysql -uroot -p
source mysql/init.sql
```

### 3. 编译项目
```bash
cd cozy-coffee-backend
mvn clean install -DskipTests
```

### 4. 启动服务

#### 启动 user-service (端口 8081)
```bash
cd cozy-user-service
mvn spring-boot:run
```

#### 启动 member-service (端口 8082)
```bash
cd cozy-member-service
mvn spring-boot:run
```

## API测试

### 注册
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456","nickname":"测试用户"}'
```

### 登录
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

### 获取会员信息
```bash
curl -X GET http://localhost:8082/api/member/info \
  -H "Authorization: Bearer <your_token>"
```

## 服务端口
- user-service: 8081 (Dubbo: 20881)
- member-service: 8082 (Dubbo: 20882)
- Nacos: 8848

## 下一步
- [ ] 前后端联调
- [ ] 实施Week 3: points-service
