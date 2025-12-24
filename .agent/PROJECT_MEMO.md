# CozyCoffee 项目备忘录

## 项目概述
CozyCoffee 是一个咖啡会员积分系统，包含前端(Vue3)和后端(Spring Boot + Dubbo 微服务)。

## 技术栈
- **前端**: Vue 3 + Vite + Element Plus + Pinia
- **后端**: Spring Boot 3 + Dubbo 3 + MyBatis Plus + Nacos
- **数据库**: MySQL

## 目录结构
- `cozy-coffee-web/` - Vue 3 前端项目
- `cozy-coffee-backend/` - Java 后端项目
  - `cozy-gateway/` - API Gateway (端口 8080)
  - `cozy-provider/cozy-member-provider/` - 会员服务
  - `cozy-provider/cozy-user-provider/` - 用户服务
  - `cozy-api/cozy-member-api/` - 会员服务 API 接口
  - `cozy-api/cozy-user-api/` - 用户服务 API 接口

## 已实现的积分系统功能

### 1. 签到积分（等级差异化）
- 基础会员: 10分/天
- 白银会员: 15分/天  
- 黄金会员: 20分/天
- 黑金会员: 30分/天
- 连续签到额外奖励（最多+14分）
- 签到记录积分流水

### 2. 新用户注册奖励
- 新用户注册自动获得 100 积分
- 自动创建会员信息

### 3. 完善资料奖励
- 手机号+邮箱都填写后一次性奖励 50 积分
- 只填写其一不发放奖励

### 4. 模拟消费赚积分
- 前端模拟消费功能
- 根据会员等级有积分倍率加成
  - 基础: 1倍, 白银: 1.1倍, 黄金: 1.2倍, 黑金: 1.5倍

### 5. 积分兑换等级折扣
- 白银会员: 95折
- 黄金会员: 90折
- 黑金会员: 85折
- 前端显示折扣价格和原价对比

### 6. 积分流水记录
- 所有积分变动记录到 points_transaction 表
- 前端积分明细弹窗展示

### 7. 会员等级自动升级
- 根据累计积分自动升级
- 500分: 白银, 2000分: 黄金, 5000分: 黑金

## 开发日志

### 2025-12-21 会话记录

#### 实现的功能

**后端修改：**

1. **SigninServiceImpl.java**
   - 添加 `PointsTransactionMapper` 注入
   - 签到成功后记录积分流水到 `points_transaction` 表
   - 签到积分根据会员等级差异化计算

2. **MemberServiceImpl.java**
   - 实现 `addPoints()` 方法：增加积分、记录流水、自动升级等级
   - 实现 `getPointsTransactions()` 方法：获取积分流水列表
   - 新用户注册自动奖励 100 积分

3. **UserServiceImpl.java**
   - 完善资料奖励逻辑修改：只有手机号+邮箱都填写才发放 50 积分

4. **PointsMallServiceImpl.java**
   - 积分兑换应用等级折扣（白银95%、黄金90%、黑金85%）
   - 订单记录实际消耗的折扣后积分

5. **MemberController.java (Gateway)**
   - 新增 `POST /api/member/points/add` - 模拟消费增加积分
   - 新增 `GET /api/member/points/transactions` - 获取积分流水

6. **PointsTransactionDTO.java** (新建)
   - 积分流水记录的 DTO 对象

**前端修改：**

1. **Member.vue**
   - 添加"💎 赚取积分"区域，展示4种积分获取渠道
   - 模拟消费弹窗（输入金额、等级倍率加成）
   - 积分明细弹窗（展示流水历史）
   - 兑换弹窗显示折扣价格（原价、折扣后价格、折扣百分比）
   - 修复添加地址按钮变量名 `showAddAddressModal`
   - 各操作后调用 `fetchMemberInfo` 刷新积分

2. **user.js (Pinia Store)**
   - 新增 `fetchMemberInfo()` 方法：异步获取最新会员信息
   - 改进 `userLevel` 计算属性：兼容 `level` 和 `memberLevel` 字段

#### 修复的 Bug

1. 签到后积分流水不显示 → 添加流水记录逻辑
2. 邮箱注册用户完善邮箱不加分 → 改为手机号+邮箱都填才发放
3. 模拟消费后多余的系统错误提示 → 分离 fetchMemberInfo 异常处理
4. 会员等级更新后前端不显示 → 添加 fetchMemberInfo 并修复字段映射
5. 积分商城添加地址按钮不生效 → 修正变量名
6. 兑换商品扣原价而非折扣价 → 使用后端返回的实际消耗积分
7. 完善资料后积分不刷新 → saveField 成功后延迟刷新会员信息

#### 待重新编译的后端服务
- `cozy-member-provider` (签到流水、积分流水接口)
- `cozy-user-provider` (完善资料积分逻辑)

---

### 2025-12-21 邀请好友功能实现

#### 功能设计

**积分奖励体系：**
- 邀请人奖励: 150 积分/人
- 被邀请人奖励: 80 积分
- 设计理由: 激励用户邀请，同时不破坏等级平衡

**邀请码规则：**
- 8位字母+数字组合（排除易混淆字符0,O,1,I,L）
- 用户注册时自动生成，终身不变
- 用户只能填写一次邀请码

#### 后端修改

1. **数据库新增字段** (`mysql/invite_feature.sql`)
   - `users.invite_code` - 用户专属邀请码
   - `users.invited_by` - 邀请人用户ID
   - `users.invited_at` - 填写邀请码时间

2. **User实体类** - 添加 inviteCode, invitedBy, invitedAt 字段

3. **UserDTO** - 添加 inviteCode, hasAppliedInviteCode 字段

4. **RegisterRequest** - 添加可选的 inviterCode 字段

5. **UserService API** - 新增 applyInviteCode(), getUserByInviteCode()

6. **UserServiceImpl**
   - 注册时生成8位邀请码
   - 注册时可选填写邀请码
   - applyInviteCode() 处理邀请奖励逻辑
   - 双方异步发放积分奖励

7. **AuthController** - 新增 API
   - `POST /api/auth/invite/apply` - 填写邀请码
   - `GET /api/auth/invite/validate` - 验证邀请码

#### 前端修改

1. **Register.vue**
   - 添加可选的邀请码输入框（8位大写字母数字）
   - 注册时提交邀请码到后端

2. **Member.vue**
   - "邀请好友"卡片从"即将上线"改为可用
   - 新增邀请弹窗：显示我的邀请码、复制功能
   - 支持填写好友邀请码获取积分
   - 已填写过则显示提示

3. **api.js** - 新增 INVITE_APPLY, INVITE_VALIDATE 接口

#### 待执行的数据库脚本
```sql
-- 执行 cozy-coffee-backend/mysql/invite_feature.sql
ALTER TABLE users ADD COLUMN invite_code VARCHAR(8) UNIQUE;
ALTER TABLE users ADD COLUMN invited_by BIGINT;
ALTER TABLE users ADD COLUMN invited_at DATETIME;
```

#### 待重新编译的后端服务
- `cozy-user-api` (UserService, UserDTO, RegisterRequest)
- `cozy-user-provider` (UserServiceImpl, User实体)
- `cozy-gateway` (AuthController)

---

## 待实现功能
- [x] 邀请好友机制（已实现 2025-12-21）
- [ ] 管理端后台

## 关键文件
- `Member.vue` - 会员中心页面（积分显示、签到、积分商城等）
- `user.js` (stores) - 用户状态管理，包含 fetchMemberInfo 方法
- `SigninServiceImpl.java` - 签到服务实现
- `MemberServiceImpl.java` - 会员服务实现（addPoints、getPointsTransactions）
- `PointsMallServiceImpl.java` - 积分商城服务
- `MemberController.java` - 会员相关 API 端点

## 启动命令
```bash
# 前端
cd cozy-coffee-web && npm run dev

# 后端需要先启动 Nacos，然后依次启动各服务
# 使用 Maven 编译: mvn clean install -DskipTests
```

## 注意事项
- 后端修改后需要重新编译部署
- Java lint 警告是 IDE 配置问题，不影响功能
- 前端热更新会自动生效，后端需重启服务

