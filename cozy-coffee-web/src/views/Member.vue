<template>
  <div class="member-layout">
    <!-- Left Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="brand-logo">COZY</h1>
      </div>

      <div class="user-profile">
        <div class="avatar-ring" @click="showAvatarModal = true">
          <img :src="userStore.userInfo?.avatar || '/images/default-avatar.png'" alt="Avatar" class="user-avatar">
          <div class="avatar-overlay">
            <span>更换</span>
          </div>
        </div>
        <div class="user-meta">
          <h2 class="user-name">{{ userStore.userInfo?.nickname || '访客' }}</h2>
          <span class="member-id">ID: {{ userStore.userInfo?.memberCode || '---' }}</span>
        </div>
      </div>


      <nav class="navigation">
        <a href="#" class="nav-link" :class="{ active: currentTab === 'member-center' }"
          @click.prevent="currentTab = 'member-center'">
          <span class="indicator"></span>
          <span class="nav-text">会员中心</span>
        </a>
        <a href="#" class="nav-link" :class="{ active: currentTab === 'member-benefits' }"
          @click.prevent="currentTab = 'member-benefits'">
          <span class="indicator"></span>
          <span class="nav-text">会员权益</span>
        </a>
        <a href="#" class="nav-link" :class="{ active: currentTab === 'menu-browse' }"
          @click.prevent="currentTab = 'menu-browse'; loadCoffeeProducts()">
          <span class="indicator"></span>
          <span class="nav-text">☕ 菜单浏览</span>
        </a>
        <a href="#" class="nav-link" :class="{ active: currentTab === 'personal-info' }"
          @click.prevent="currentTab = 'personal-info'">
          <span class="indicator"></span>
          <span class="nav-text">个人信息</span>
        </a>
        <a href="#" class="nav-link" :class="{ active: currentTab === 'my-orders' }"
          @click.prevent="currentTab = 'my-orders'; loadOrders()">
          <span class="indicator"></span>
          <span class="nav-text">历史订单</span>
        </a>
      </nav>

      <div class="sidebar-footer">
        <button @click="router.push('/')" class="footer-link">
          <i class="icon-arrow-left"></i> 返回首页
        </button>
        <button @click="handleLogout" class="footer-link logout">
          <i class="icon-power"></i> 退出登录
        </button>
      </div>
    </aside>

    <!-- Main Content Area -->
    <main class="main-content">
      <!-- Dashboard View -->
      <transition name="fade" mode="out-in">
        <div v-if="currentTab === 'member-center'" class="dashboard-view" key="dashboard">
          <header class="content-header">
            <h3>会员概览</h3>
            <span class="date-badge">{{ new Date().toLocaleDateString() }}</span>
          </header>

          <!-- Digital Member Card -->
          <div class="digital-card" :class="userStore.userLevel">
            <div class="card-chip"></div>
            <div class="card-balance">
              <span class="label">当前积分</span>
              <span class="value">{{ userStore.userInfo?.currentPoints || 0 }}</span>
            </div>
            <div class="card-holder">
              <span class="label">会员姓名</span>
              <span class="value">{{ userStore.userInfo?.nickname || 'COZY MEMBER' }}</span>
            </div>
            <div class="card-tier">{{ levelName }}</div>
          </div>

          <!-- Stats Grid -->
          <div class="stats-grid">
            <div class="stat-card">
              <span class="stat-label">累计积分</span>
              <span class="stat-value">{{ userStore.userInfo?.totalPoints || 0 }}</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">连续签到</span>
              <span class="stat-value">{{ userStore.userInfo?.signInDays || 0 }} 天</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">下一等级需要</span>
              <span class="stat-value highlight">{{ Math.max(0, nextLevelPoints - (userStore.userInfo?.totalPoints ||
                0)) }} 积分</span>
            </div>
          </div>

          <!-- Sign In Widget -->
          <div class="widget-section">
            <div class="widget-header">
              <h4>每日签到</h4>
              <button class="signin-btn" @click="handleSignIn" :disabled="isSignedToday">
                {{ isSignedToday ? '今日已签到' : '立即签到' }}
              </button>
            </div>
            <div class="progress-track">
              <div class="step" v-for="day in 7" :key="day"
                :class="{ active: day <= (userStore.userInfo?.signInDays || 0) }">
                <div class="step-circle">{{ day }}</div>
              </div>
              <div class="track-line">
                <div class="fill"
                  :style="{ width: Math.max(0, ((userStore.userInfo?.signInDays || 0) - 1) / 6 * 100) + '%' }"></div>
              </div>
            </div>
          </div>

          <!-- 积分获取渠道展示 -->
          <div class="points-guide-section">
            <div class="section-title">
              <h4>💎 赚取积分</h4>
              <span class="subtitle">完成任务获取积分，加速升级</span>
            </div>

            <div class="points-channels">
              <!-- 签到积分 -->
              <div class="channel-card" :class="{ done: isSignedToday }">
                <div class="channel-icon">📅</div>
                <div class="channel-info">
                  <span class="channel-name">每日签到</span>
                  <span class="channel-desc">连续签到额外奖励</span>
                </div>
                <div class="channel-points">
                  <span class="points-value">+{{ getSigninPointsByLevel() }}</span>
                  <span class="points-label">积分/天</span>
                </div>
                <span v-if="isSignedToday" class="status-badge done">已完成</span>
              </div>

              <!-- 完善资料 -->
              <div class="channel-card" :class="{ done: profileComplete }">
                <div class="channel-icon">📝</div>
                <div class="channel-info">
                  <span class="channel-name">完善资料</span>
                  <span class="channel-desc">填写手机号和邮箱</span>
                </div>
                <div class="channel-points">
                  <span class="points-value">+50</span>
                  <span class="points-label">积分</span>
                </div>
                <span v-if="profileComplete" class="status-badge done">已完成</span>
                <button v-else @click="currentTab = 'personal-info'" class="go-btn">去完成</button>
              </div>

              <!-- 消费获积分 -->
              <div class="channel-card">
                <div class="channel-icon">☕</div>
                <div class="channel-info">
                  <span class="channel-name">消费赚积分</span>
                  <span class="channel-desc">每消费1元得{{ getConsumeMultiplier() }}积分</span>
                </div>
                <div class="channel-points">
                  <span class="points-value">1元={{ getConsumeMultiplier() }}</span>
                  <span class="points-label">积分</span>
                </div>
                <span class="status-hint">移动端下单</span>
              </div>

              <!-- 邀请好友 -->
              <div class="channel-card invite-card" :class="{ done: userStore.userInfo?.hasAppliedInviteCode }">
                <div class="channel-icon">👥</div>
                <div class="channel-info">
                  <span class="channel-name">邀请好友</span>
                  <span class="channel-desc">邀请人+150，填写邀请码+80</span>
                </div>
                <div class="channel-points">
                  <span class="points-value">+80</span>
                  <span class="points-label">积分(填写)</span>
                </div>
                <span v-if="userStore.userInfo?.hasAppliedInviteCode" class="status-badge done">已完成</span>
                <button v-else @click="currentTab = 'personal-info'" class="go-btn invite">去完成</button>
              </div>
            </div>

            <!-- 等级权益提示 -->
            <div class="level-tip" v-if="userStore.userLevel !== 'black'">
              <span class="tip-icon">🚀</span>
              <span class="tip-text">
                升级到 <strong>{{ nextLevelName }}</strong> 可享受更多权益！还需 <strong>{{ Math.max(0, nextLevelPoints -
                  (userStore.userInfo?.totalPoints || 0)) }}</strong> 积分
              </span>
              <button @click="currentTab = 'member-benefits'" class="view-benefits-btn">查看权益</button>
            </div>
            <div class="level-tip max-level" v-else>
              <span class="tip-icon">👑</span>
              <span class="tip-text">恭喜！您已达到最高等级 <strong>黑金会员</strong>，尊享全部特权！</span>
            </div>

            <!-- 查看积分明细按钮 -->
            <div class="points-detail-link">
              <button @click="openPointsDetailModal" class="detail-btn">
                📊 查看积分明细
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="currentTab === 'personal-info'" class="profile-view" key="profile">
          <header class="content-header">
            <h3>个人信息</h3>
          </header>

          <div class="form-container">
            <div class="form-group">
              <label>昵称</label>
              <div class="input-wrapper">
                <input v-if="isEditingNickname" v-model="editNickname" type="text" class="modern-input">
                <span v-else class="static-value">{{ userStore.userInfo?.nickname || '--' }}</span>
                <button v-if="!isEditingNickname" @click="startEditNickname" class="edit-btn">修改</button>
                <div v-else class="actions">
                  <button @click="saveNickname" class="save-btn">保存</button>
                  <button @click="cancelEditNickname" class="cancel-btn">取消</button>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>手机号</label>
              <div class="input-wrapper">
                <input v-if="isEditingPhone" v-model="editPhone" type="text" class="modern-input">
                <span v-else class="static-value">{{ userStore.userInfo?.phone || '未绑定' }}</span>
                <button v-if="!isEditingPhone" @click="startEditPhone" class="edit-btn">修改</button>
                <div v-else class="actions">
                  <button @click="saveField('phone', editPhone)" class="save-btn">保存</button>
                  <button @click="cancelEditPhone" class="cancel-btn">取消</button>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>邮箱</label>
              <div class="input-wrapper">
                <input v-if="isEditingEmail" v-model="editEmail" type="text" class="modern-input">
                <span v-else class="static-value">{{ userStore.userInfo?.email || '未绑定' }}</span>
                <button v-if="!isEditingEmail" @click="startEditEmail" class="edit-btn">修改</button>
                <div v-else class="actions">
                  <button @click="saveField('email', editEmail)" class="save-btn">保存</button>
                  <button @click="cancelEditEmail" class="cancel-btn">取消</button>
                </div>
              </div>
            </div>

            <!-- 邀请码相关 -->
            <div class="form-group">
              <label>我的邀请码</label>
              <div class="input-wrapper invite-wrapper">
                <span class="invite-code-text">{{ userStore.userInfo?.inviteCode || '生成中...' }}</span>
                <button @click="copyInviteCode" class="copy-code-btn">复制</button>
                <span class="invite-tip">分享给好友各得积分奖励</span>
              </div>
            </div>

            <div class="form-group" v-if="!userStore.userInfo?.hasAppliedInviteCode">
              <label>好友邀请码</label>
              <div class="input-wrapper">
                <input v-model="inputInviteCode" type="text" class="modern-input invite-input" placeholder="输入8位邀请码"
                  maxlength="8" @input="inputInviteCode = inputInviteCode.toUpperCase()">
                <button @click="applyInviteCode" class="save-btn verify-btn"
                  :disabled="isApplyingInviteCode || inputInviteCode.length < 8">
                  {{ isApplyingInviteCode ? '验证中...' : '提交' }}
                </button>
              </div>
              <p class="field-hint">填写好友邀请码可获得 80 积分奖励，仅限一次</p>
            </div>
            <div class="form-group" v-else>
              <label>好友邀请码</label>
              <div class="input-wrapper">
                <span class="static-value completed">✅ 已填写完成</span>
              </div>
            </div>
          </div>

          <!-- 收货地址管理 -->
          <div class="address-management">
            <div class="section-header">
              <h4>收货地址</h4>
              <button @click="openAddAddressModal" class="add-btn">+ 添加地址</button>
            </div>
            <div class="address-list" v-if="addresses.length > 0">
              <div class="address-card" v-for="addr in addresses" :key="addr.id" :class="{ default: addr.isDefault }">
                <div class="addr-info">
                  <div class="addr-header">
                    <span class="receiver-name">{{ addr.receiverName }}</span>
                    <span class="receiver-phone">{{ addr.receiverPhone }}</span>
                    <span v-if="addr.isDefault" class="default-badge">默认</span>
                  </div>
                  <p class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district || '' }}{{
                    addr.detailAddress }}</p>
                </div>
                <div class="addr-actions">
                  <button @click="openEditAddressModal(addr)" class="action-btn">编辑</button>
                  <button v-if="!addr.isDefault" @click="setDefaultAddress(addr.id)" class="action-btn">设为默认</button>
                  <button @click="deleteAddress(addr.id)" class="action-btn delete">删除</button>
                </div>
              </div>
            </div>
            <div v-else class="no-address">
              <p>暂无收货地址</p>
            </div>
          </div>
        </div>

        <!-- 会员权益视图 -->
        <div v-else-if="currentTab === 'member-benefits'" class="benefits-view" key="benefits">
          <header class="content-header">
            <h3>会员权益</h3>
          </header>

          <div class="benefits-cards">
            <!-- 基础会员 -->
            <div class="benefit-card basic" :class="{ current: userStore.userLevel === 'basic' }">
              <div class="benefit-header">
                <h4>基础会员</h4>
                <span class="level-badge">0 - 999 积分</span>
              </div>
              <ul class="benefit-list">
                <li>每日签到获得 10 积分</li>
                <li>积分商城基础商品兑换</li>
                <li>生日当月双倍积分</li>
              </ul>
            </div>

            <!-- 白银会员 -->
            <div class="benefit-card silver" :class="{ current: userStore.userLevel === 'silver' }">
              <div class="benefit-header">
                <h4>白银会员</h4>
                <span class="level-badge">1000 - 2999 积分</span>
              </div>
              <ul class="benefit-list">
                <li>每日签到获得 15 积分</li>
                <li>积分兑换 9.5 折</li>
                <li>每月专属优惠券 x1</li>
                <li>生日当月双倍积分</li>
              </ul>
            </div>

            <!-- 黄金会员 -->
            <div class="benefit-card gold" :class="{ current: userStore.userLevel === 'gold' }">
              <div class="benefit-header">
                <h4>黄金会员</h4>
                <span class="level-badge">3000 - 7999 积分</span>
              </div>
              <ul class="benefit-list">
                <li>每日签到获得 20 积分</li>
                <li>积分兑换 9 折</li>
                <li>每月专属优惠券 x2</li>
                <li>新品优先体验权</li>
                <li>生日当月三倍积分</li>
              </ul>
            </div>

            <!-- 黑金会员 -->
            <div class="benefit-card black" :class="{ current: userStore.userLevel === 'black' }">
              <div class="benefit-header">
                <h4>黑金会员</h4>
                <span class="level-badge">8000+ 积分</span>
              </div>
              <ul class="benefit-list">
                <li>每日签到获得 30 积分</li>
                <li>积分兑换 8.5 折</li>
                <li>每月专属优惠券 x3</li>
                <li>新品优先体验权</li>
                <li>专属客服通道</li>
                <li>生日当月三倍积分 + 神秘礼物</li>
              </ul>
            </div>
          </div>

          <div class="current-level-info">
            <p>您当前的等级：<strong>{{ levelName }}</strong></p>
            <p>累计积分：<strong>{{ userStore.userInfo?.totalPoints || 0 }}</strong></p>
            <p v-if="userStore.userLevel !== 'black'">
              距离下一等级还需：<strong>{{ Math.max(0, nextLevelPoints - (userStore.userInfo?.totalPoints || 0)) }}</strong> 积分
            </p>
          </div>
        </div>

        <!-- 我的订单视图 -->
        <div v-else-if="currentTab === 'my-orders'" class="orders-view" key="orders">
          <header class="content-header">
            <h3>我的订单</h3>
          </header>

          <div class="orders-list" v-if="orders.length > 0">
            <div class="order-card" v-for="order in orders" :key="order.id">
              <div class="order-header">
                <span class="order-no">订单号: {{ order.orderNo }}</span>
                <span class="order-status" :class="order.status">{{ getStatusText(order.status) }}</span>
              </div>
              <div class="order-body">
                <img :src="order.productImage || '/images/products/default.png'" class="order-img" />
                <div class="order-info">
                  <p class="product-name">{{ order.productName }}</p>
                  <p class="order-quantity">数量: {{ order.quantity || 1 }}</p>
                  <p class="points-cost">消耗积分: {{ order.pointsCost }}</p>
                  <p class="order-time">{{ formatDate(order.createdAt) }}</p>
                </div>
              </div>
              <div class="order-footer">
                <p class="receiver-info">收货人: {{ order.receiverName }} {{ order.receiverPhone }}</p>
                <p class="receiver-address">{{ order.receiverAddress }}</p>
              </div>
              <!-- 订单操作区 -->
              <div class="order-actions" v-if="order.status === 'pending' || order.status === 'processing'">
                <button class="cancel-order-btn" @click="handleCancelOrder(order)"
                  :disabled="cancellingOrderId === order.id">
                  {{ cancellingOrderId === order.id ? '取消中...' : '取消订单' }}
                </button>
                <span class="action-hint">发货前可取消订单，积分将返还</span>
              </div>
            </div>
          </div>
          <div v-else class="no-data">
            <p>暂无订单记录</p>
            <button @click="currentTab = 'points-mall'" class="go-mall-btn">去积分商城看看</button>
          </div>
        </div>

        <!-- Menu Browse View -->
        <div v-else-if="currentTab === 'menu-browse'" class="menu-browse-view" key="menu-browse">
          <header class="content-header">
            <h3>☕ 菜单浏览</h3>
            <span class="points-badge">扫码点单享 {{ getConsumeMultiplier() }}倍积分</span>
          </header>

          <div class="menu-products-grid">
            <div class="menu-card" v-for="product in coffeeProducts" :key="product.id" @click="openProductDetail(product)">
              <div class="menu-image" :style="{ backgroundImage: `url(${product.imageUrl || '/images/cafe1.png'})` }">
              </div>
              <div class="menu-details">
                <h4>{{ product.name }}</h4>
                <p class="menu-desc">{{ product.description }}</p>
                <div class="menu-price-row">
                  <span class="menu-price">¥{{ product.price }}</span>
                  <span class="view-detail-btn">查看详情</span>
                </div>
              </div>
            </div>
            <div v-if="coffeeProducts.length === 0" class="no-data">
              <p>暂无商品</p>
            </div>
          </div>

          <div class="menu-tip">
            <p>💡 请使用移动端扫码点单，享受会员积分加成</p>
          </div>
        </div>

        <!-- Points Mall View -->
        <div v-else-if="currentTab === 'points-mall'" class="mall-view" key="mall">
          <header class="content-header">
            <h3>积分商城</h3>
            <span class="points-badge">{{ userStore.userInfo?.currentPoints || 0 }} 积分</span>
          </header>

          <!-- 商品列表 -->
          <div class="products-grid" v-if="!showOrderHistory">
            <div class="mall-card" v-for="product in products" :key="product.id">
              <div class="card-image"
                :style="{ backgroundImage: `url(${product.imageUrl || '/images/products/default.png'})` }"></div>
              <div class="card-details">
                <h4>{{ product.name }}</h4>
                <p class="product-desc">{{ product.description }}</p>
                <span class="price">{{ product.pointsPrice }} 积分</span>
                <span class="stock" :class="{ low: product.stock < 10 }">库存: {{ product.stock }}</span>
                <button class="redeem-btn" @click="openRedeemDialog(product)"
                  :disabled="product.stock === 0 || (userStore.userInfo?.currentPoints || 0) < product.pointsPrice">
                  {{ product.stock === 0 ? '已售罄' : '立即兑换' }}
                </button>
              </div>
            </div>
            <div v-if="products.length === 0" class="no-data">暂无商品</div>
          </div>

        </div>
      </transition>

      <!-- 兑换确认弹窗 - 移到 transition 外部 -->
      <div class="redeem-modal" v-if="showRedeemModal" @click.self="showRedeemModal = false">
        <div class="modal-content">
          <h3>确认兑换</h3>
          <div class="product-preview">
            <img :src="selectedProduct?.imageUrl || '/images/products/default.png'" />
            <div>
              <p class="name">{{ selectedProduct?.name }}</p>
              <p class="unit-price">单价: {{ selectedProduct?.pointsPrice }} 积分</p>
              <p class="stock-info">库存: {{ selectedProduct?.stock }}</p>
            </div>
          </div>

          <!-- 兑换数量选择 -->
          <div class="quantity-section">
            <label>兑换数量：</label>
            <div class="quantity-control">
              <button class="qty-btn" @click="redeemQuantity > 1 && redeemQuantity--"
                :disabled="redeemQuantity <= 1">−</button>
              <input type="number" v-model.number="redeemQuantity" min="1"
                :max="Math.min(selectedProduct?.stock || 1, 10)" class="qty-input" />
              <button class="qty-btn"
                @click="redeemQuantity < Math.min(selectedProduct?.stock || 1, 10) && redeemQuantity++"
                :disabled="redeemQuantity >= Math.min(selectedProduct?.stock || 1, 10)">+</button>
            </div>
            <span class="qty-tip">单次最多兑换10件</span>
          </div>

          <div class="total-cost">
            <span>总消耗积分:</span>
            <strong :class="{ 'has-discount': getRedeemDiscount() < 1 }">
              {{ getDiscountedCost() }}
            </strong>
            <span v-if="getRedeemDiscount() < 1" class="original-price">
              原价 {{ (selectedProduct?.pointsPrice || 0) * redeemQuantity }}
            </span>
            <span v-if="getRedeemDiscount() < 1" class="discount-badge">
              {{ Math.round(getRedeemDiscount() * 100) }}%
            </span>
            <span class="balance-hint">(当前积分: {{ userStore.userInfo?.currentPoints || 0 }})</span>
          </div>

          <div class="address-section">
            <h4>收货地址</h4>
            <div v-if="addresses.length > 0">
              <div v-for="addr in addresses" :key="addr.id" class="address-item"
                :class="{ selected: selectedAddressId === addr.id }" @click="selectedAddressId = addr.id">
                <span class="receiver">{{ addr.receiverName }} {{ addr.receiverPhone }}</span>
                <span class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district || '' }}{{
                  addr.detailAddress }}</span>
                <span v-if="addr.isDefault" class="default-tag">默认</span>
              </div>
            </div>
            <div v-else class="no-address">
              <p>暂无收货地址，请先添加</p>
              <button @click="showAddAddressModal = true" class="add-addr-btn">添加地址</button>
            </div>
          </div>

          <div class="modal-actions">
            <button @click="showRedeemModal = false" class="cancel-btn">取消</button>
            <button @click="confirmRedeem" class="confirm-btn"
              :disabled="!selectedAddressId || isRedeeming || ((selectedProduct?.pointsPrice || 0) * redeemQuantity > (userStore.userInfo?.currentPoints || 0))">
              {{ isRedeeming ? '兑换中...' : '确认兑换' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 头像更换模态框 -->
      <div class="avatar-modal" v-if="showAvatarModal" @click.self="showAvatarModal = false">
        <div class="modal-content">
          <h3>更换头像</h3>
          <div class="avatar-preview-area">
            <img :src="avatarPreview || userStore.userInfo?.avatar || '/images/default-avatar.png'"
              class="preview-img" />
          </div>
          <input type="file" ref="avatarInput" accept="image/*" @change="handleAvatarChange" style="display: none" />
          <div class="modal-actions">
            <button @click="$refs.avatarInput?.click()" class="select-btn">选择图片</button>
            <button @click="saveAvatar" class="confirm-btn" :disabled="!avatarPreview">保存头像</button>
            <button @click="showAvatarModal = false; avatarPreview = ''" class="cancel-btn">取消</button>
          </div>
        </div>
      </div>

      <!-- 添加/编辑地址模态框 -->
      <div class="address-modal" v-if="showAddAddressModal" @click.self="closeAddressModal">
        <div class="modal-content address-modal-content">
          <h3>{{ isEditingAddress ? '编辑收货地址' : '添加收货地址' }}</h3>

          <div class="form-row">
            <div class="form-item">
              <label>收货人姓名 <span class="required">*</span></label>
              <input v-model="newAddress.receiverName" type="text" placeholder="请输入收货人姓名" />
            </div>
            <div class="form-item">
              <label>联系电话 <span class="required">*</span></label>
              <input v-model="newAddress.receiverPhone" type="text" placeholder="请输入手机号码" />
            </div>
          </div>

          <div class="form-row region-row">
            <div class="form-item">
              <label>省份 <span class="required">*</span></label>
              <select v-model="selectedProvinceCode" class="region-select">
                <option value="">请选择省份</option>
                <option v-for="p in provinces" :key="p.code" :value="p.code">{{ p.name }}</option>
              </select>
            </div>
            <div class="form-item">
              <label>城市 <span class="required">*</span></label>
              <select v-model="selectedCityCode" class="region-select" :disabled="!selectedProvinceCode">
                <option value="">请选择城市</option>
                <option v-for="c in cities" :key="c.code" :value="c.code">{{ c.name }}</option>
              </select>
            </div>
            <div class="form-item">
              <label>区/县</label>
              <select v-model="selectedDistrictCode" class="region-select" :disabled="!selectedCityCode">
                <option value="">请选择区县</option>
                <option v-for="d in districts" :key="d.code" :value="d.code">{{ d.name }}</option>
              </select>
            </div>
          </div>

          <div v-if="isEditingAddress && newAddress.province" class="current-region-hint">
            当前地址: {{ newAddress.province }} {{ newAddress.city }} {{ newAddress.district }}
            <span class="hint-text">（如需更改请重新选择）</span>
          </div>

          <div class="form-item full-width">
            <label>详细地址 <span class="required">*</span></label>
            <input v-model="newAddress.detailAddress" type="text" placeholder="请输入街道、门牌号等详细信息" />
          </div>

          <div class="form-item checkbox">
            <label>
              <input type="checkbox" v-model="newAddress.isDefault" />
              设为默认收货地址
            </label>
          </div>

          <div class="modal-actions">
            <button @click="closeAddressModal" class="cancel-btn">取消</button>
            <button @click="saveAddress" class="confirm-btn">{{ isEditingAddress ? '保存修改' : '确认添加' }}</button>
          </div>
        </div>
      </div>

      <!-- 模拟消费弹窗 -->
      <div class="simulate-modal" v-if="showSimulateConsumeModal" @click.self="showSimulateConsumeModal = false">
        <div class="modal-content">
          <h3>☕ 模拟消费</h3>
          <p class="modal-desc">输入消费金额，体验积分获取流程</p>

          <div class="consume-form">
            <div class="amount-input-group">
              <span class="currency">¥</span>
              <input type="number" v-model.number="simulateAmount" min="1" max="1000" placeholder="消费金额"
                class="amount-input" />
            </div>

            <div class="points-preview">
              <div class="preview-item">
                <span class="label">消费金额</span>
                <span class="value">¥{{ simulateAmount }}</span>
              </div>
              <div class="preview-item">
                <span class="label">积分倍率</span>
                <span class="value multiplier">x{{ getConsumeMultiplier() }}</span>
              </div>
              <div class="preview-item total">
                <span class="label">预计获得</span>
                <span class="value points">+{{ Math.floor((simulateAmount || 0) * getConsumeMultiplier()) }} 积分</span>
              </div>
            </div>

            <div class="level-bonus-tip" v-if="userStore.userLevel !== 'basic'">
              🎉 {{ levelName }} 专属加成！积分倍率 x{{ getConsumeMultiplier() }}
            </div>
          </div>

          <div class="modal-actions">
            <button @click="showSimulateConsumeModal = false" class="cancel-btn">取消</button>
            <button @click="handleSimulateConsume" class="confirm-btn" :disabled="isSimulating || simulateAmount < 1">
              {{ isSimulating ? '处理中...' : '确认消费' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 积分明细弹窗 -->
      <div class="points-detail-modal" v-if="showPointsDetailModal" @click.self="showPointsDetailModal = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>📊 积分明细</h3>
            <button @click="showPointsDetailModal = false" class="close-btn">×</button>
          </div>

          <div class="balance-summary">
            <div class="balance-item">
              <span class="label">当前积分</span>
              <span class="value">{{ userStore.userInfo?.currentPoints || 0 }}</span>
            </div>
            <div class="balance-item">
              <span class="label">累计获得</span>
              <span class="value">{{ userStore.userInfo?.totalPoints || 0 }}</span>
            </div>
          </div>

          <div class="transactions-list" v-if="!isLoadingTransactions">
            <div v-if="pointsTransactions.length === 0" class="empty-state">
              暂无积分记录
            </div>
            <div v-else class="transaction-item" v-for="item in pointsTransactions" :key="item.id">
              <div class="transaction-left">
                <span class="transaction-type" :class="item.changeAmount > 0 ? 'income' : 'expense'">
                  {{ getSourceTypeName(item.sourceType) }}
                </span>
                <span class="transaction-desc">{{ item.description }}</span>
                <span class="transaction-time">{{ formatDateTime(item.createdAt) }}</span>
              </div>
              <div class="transaction-right">
                <span class="transaction-amount" :class="item.changeAmount > 0 ? 'income' : 'expense'">
                  {{ item.changeAmount > 0 ? '+' : '' }}{{ item.changeAmount }}
                </span>
                <span class="transaction-balance">余额：{{ item.balanceAfter }}</span>
              </div>
            </div>
          </div>
          <div v-else class="loading-state">
            加载中...
          </div>
        </div>
      </div>

      <!-- 商品详情弹窗 -->
      <div class="product-detail-modal" v-if="showProductDetailModal" @click.self="closeProductDetail">
        <div class="modal-content">
          <button class="close-btn" @click="closeProductDetail">×</button>
          <div v-if="detailProduct" class="detail-content">
            <div class="detail-image" :style="{ backgroundImage: `url(${detailProduct.imageUrl || '/images/cafe1.png'})` }"></div>
            <div class="detail-info">
              <h3>{{ detailProduct.name }}</h3>
              <p class="price">¥{{ detailProduct.price }}</p>
              <p class="description">{{ detailProduct.description }}</p>
              <div class="points-info">
                <span class="points-label">消费可获得</span>
                <span class="points-value">+{{ Math.floor(detailProduct.price * getConsumeMultiplier()) }} 积分</span>
              </div>
              <div class="mobile-tip">
                <p>💡 请使用移动端扫码点单，享受会员专属积分加成</p>
              </div>
            </div>
          </div>
        </div>
      </div>

    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import chinaRegions from '@/data/china-regions.json'

const userStore = useUserStore()
const router = useRouter()
const currentTab = ref('member-center')

// Editing States
const isEditingNickname = ref(false)
const editNickname = ref('')
const isEditingPhone = ref(false)
const editPhone = ref('')
const isEditingEmail = ref(false)
const editEmail = ref('')

// Points Mall States
const products = ref([])
const orders = ref([])
const addresses = ref([])
const showOrderHistory = ref(false)
const showRedeemModal = ref(false)
const showAddressForm = ref(false)
const showAddAddressModal = ref(false)
const showAvatarModal = ref(false)
const selectedProduct = ref(null)
const selectedAddressId = ref(null)
const isRedeeming = ref(false)
const redeemQuantity = ref(1)
const cancellingOrderId = ref(null)

// Avatar upload
const avatarInput = ref(null)
const avatarPreview = ref('')

// 省市区级联选择
const selectedProvinceCode = ref('')
const selectedCityCode = ref('')
const selectedDistrictCode = ref('')

// 地址编辑
const isEditingAddress = ref(false)
const editingAddressId = ref(null)

// 咖啡点单
const coffeeProducts = ref([])
const coffeeOrders = ref([])
const isOrdering = ref(false)

// 商品详情弹窗
const showProductDetailModal = ref(false)
const detailProduct = ref(null)

// 积分明细
const showPointsDetailModal = ref(false)
const pointsTransactions = ref([])
const isLoadingTransactions = ref(false)

// 邀请功能
const showInviteModal = ref(false)
const inputInviteCode = ref('')
const isApplyingInviteCode = ref(false)

// 计算省份列表
const provinces = computed(() => {
  const list = chinaRegions['86'] || {}
  return Object.entries(list).map(([code, name]) => ({ code, name }))
})

// 计算城市列表
const cities = computed(() => {
  if (!selectedProvinceCode.value) return []
  const list = chinaRegions[selectedProvinceCode.value] || {}
  return Object.entries(list).map(([code, name]) => ({ code, name }))
})

// 计算区县列表
const districts = computed(() => {
  if (!selectedCityCode.value) return []
  const list = chinaRegions[selectedCityCode.value] || {}
  return Object.entries(list).map(([code, name]) => ({ code, name }))
})

// 监听省份变化，重置城市和区县
watch(selectedProvinceCode, () => {
  selectedCityCode.value = ''
  selectedDistrictCode.value = ''
})

// 监听城市变化，重置区县
watch(selectedCityCode, () => {
  selectedDistrictCode.value = ''
})

// New Address Form
const newAddress = ref({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

// Computed
const levelName = computed(() => {
  const lvl = userStore.userLevel || 'basic';
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', black: '黑金会员' }
  return map[lvl] || '基础会员'
})

const nextLevelPoints = computed(() => {
  const map = { basic: 1000, silver: 3000, gold: 8000, black: 99999 }
  return map[userStore.userLevel] || 1000
})

const nextLevelName = computed(() => {
  const map = { basic: '白银会员', silver: '黄金会员', gold: '黑金会员', black: '黑金会员' }
  return map[userStore.userLevel] || '白银会员'
})

const isSignedToday = computed(() => {
  const d = new Date()
  const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const lastSign = userStore.userInfo?.lastSigninDate || userStore.userInfo?.lastSignIn
  return lastSign === today
})

const profileComplete = computed(() => {
  const phone = userStore.userInfo?.phone
  const email = userStore.userInfo?.email
  return (phone && phone.length > 0) && (email && email.length > 0)
})

// 根据等级获取签到积分
const getSigninPointsByLevel = () => {
  const level = userStore.userLevel || 'basic'
  const map = { basic: 10, silver: 15, gold: 20, black: 30 }
  return map[level] + '~' + (map[level] + 14)
}

// 根据等级获取消费积分倍率
const getConsumeMultiplier = () => {
  const level = userStore.userLevel || 'basic'
  const map = { basic: 1, silver: 1.1, gold: 1.2, black: 1.5 }
  return map[level]
}

// 获取积分兑换折扣（与后端一致）
const getRedeemDiscount = () => {
  const level = userStore.userLevel || 'basic'
  const map = { basic: 1, silver: 0.95, gold: 0.90, black: 0.85 }
  return map[level] || 1
}

// 计算折扣后的兑换价格
const getDiscountedCost = () => {
  const original = (selectedProduct.value?.pointsPrice || 0) * redeemQuantity.value
  return Math.ceil(original * getRedeemDiscount())
}

// 加载咖啡商品列表
const loadCoffeeProducts = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/order/products', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      coffeeProducts.value = (data.data || []).map(p => ({ ...p, orderQty: 1 }))
    }
  } catch (error) {
    console.error('加载咖啡商品失败:', error)
  }
  // 同时加载消费订单记录
  loadCoffeeOrders()
}

// 加载咖啡消费订单
const loadCoffeeOrders = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/order/list', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      coffeeOrders.value = data.data || []
    }
  } catch (error) {
    console.error('加载消费订单失败:', error)
  }
}

// 咖啡下单
const handleCoffeeOrder = async (product) => {
  if (isOrdering.value) return

  isOrdering.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/order/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        productId: product.id,
        quantity: product.orderQty || 1
      })
    })

    const data = await response.json()
    if (data.success) {
      const order = data.data
      ElMessage.success(`下单成功！获得 ${order.pointsEarned} 积分`)

      // 更新本地积分
      userStore.userInfo.currentPoints = (userStore.userInfo.currentPoints || 0) + order.pointsEarned
      userStore.userInfo.totalPoints = (userStore.userInfo.totalPoints || 0) + order.pointsEarned
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))

      // 刷新订单列表
      await loadCoffeeOrders()

      // 重新加载会员信息以获取最新等级
      try {
        await userStore.fetchMemberInfo()
      } catch (e) {
        console.warn('刷新会员信息失败', e)
      }
    } else {
      ElMessage.error(data.message || '下单失败')
    }
  } catch (error) {
    console.error('咖啡下单失败:', error)
    ElMessage.error('网络错误，请重试')
  } finally {
    isOrdering.value = false
  }
}

// 打开商品详情弹窗
const openProductDetail = (product) => {
  detailProduct.value = product
  showProductDetailModal.value = true
}

// 关闭商品详情弹窗
const closeProductDetail = () => {
  showProductDetailModal.value = false
  detailProduct.value = null
}

// 打开积分明细弹窗
const openPointsDetailModal = async () => {
  showPointsDetailModal.value = true
  isLoadingTransactions.value = true
  pointsTransactions.value = []

  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/member/points/transactions?limit=50', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    const data = await response.json()
    if (data.success) {
      pointsTransactions.value = data.data || []
    } else {
      ElMessage.error(data.message || '获取积分明细失败')
    }
  } catch (error) {
    ElMessage.error('系统错误')
  } finally {
    isLoadingTransactions.value = false
  }
}

// 获取积分来源类型的显示名称
const getSourceTypeName = (type) => {
  const map = {
    'signin': '每日签到',
    'register': '新用户注册',
    'profile': '完善资料',
    'consume': '消费赚积分',
    'redeem': '积分兑换',
    'cancel': '订单取消退还',
    'invite': '邀请好友',
    'invited': '受邀奖励'
  }
  return map[type] || type
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// ========== 邀请功能方法 ==========


// 复制邀请码
const copyInviteCode = async () => {
  const code = userStore.userInfo?.inviteCode
  if (!code) {
    ElMessage.warning('邀请码加载中，请稍后再试')
    return
  }

  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('邀请码已复制到剪贴板！')
  } catch (e) {
    // 备用方案
    const textarea = document.createElement('textarea')
    textarea.value = code
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('邀请码已复制！')
  }
}

// 填写邀请码
const applyInviteCode = async () => {
  if (inputInviteCode.value.length < 8) {
    ElMessage.warning('请输入完整的8位邀请码')
    return
  }

  isApplyingInviteCode.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/auth/invite/apply', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        inviteCode: inputInviteCode.value
      })
    })

    const data = await response.json()
    if (data.success) {
      ElMessage.success(data.message || '邀请码填写成功！双方都已获得积分奖励')
      inputInviteCode.value = ''

      // 立即在前端更新状态（优化用户体验）
      if (userStore.userInfo) {
        // 先检查是否已经累加过（防止并发问题，虽然这里几率很小）
        if (!userStore.userInfo.hasAppliedInviteCode) {
          userStore.userInfo.currentPoints = (userStore.userInfo.currentPoints || 0) + 80
          userStore.userInfo.totalPoints = (userStore.userInfo.totalPoints || 0) + 80
          userStore.userInfo.hasAppliedInviteCode = true
          // 同步到本地缓存，防止刷新丢失
          localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
          console.log('Frontend points updated manually (+80)')
        }
      }

      // 延迟刷新用户信息 (给后端异步发放积分留出时间，确保真正同步)
      setTimeout(async () => {
        try {
          await userStore.fetchUserInfo()
          await userStore.fetchMemberInfo()
          console.log('User and Member info refetched after delay')
        } catch (e) {
          console.warn('延迟刷新失败', e)
        }
      }, 1000)
    } else {
      ElMessage.error(data.message || '填写邀请码失败')
    }
  } catch (error) {
    console.error('填写邀请码失败:', error)
    ElMessage.error('网络错误，请重试')
  } finally {
    isApplyingInviteCode.value = false
  }
}

// Methods
const handleLogout = () => {
  userStore.logout()
  router.push('/')
}

const startEditNickname = () => {
  editNickname.value = userStore.userInfo?.nickname || ''
  isEditingNickname.value = true
}

const cancelEditNickname = () => {
  isEditingNickname.value = false
  editNickname.value = ''
}

const startEditPhone = () => {
  editPhone.value = userStore.userInfo?.phone || ''
  isEditingPhone.value = true
}

const cancelEditPhone = () => {
  isEditingPhone.value = false
  editPhone.value = ''
}

const startEditEmail = () => {
  editEmail.value = userStore.userInfo?.email || ''
  isEditingEmail.value = true
}

const cancelEditEmail = () => {
  isEditingEmail.value = false
  editEmail.value = ''
}

const saveField = async (fieldName, fieldValue) => {
  try {
    const token = localStorage.getItem('token')
    const body = {}
    body[fieldName] = fieldValue

    const response = await fetch('http://localhost:8080/api/auth/profile', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(body)
    })

    const data = await response.json()
    if (data.success) {
      userStore.userInfo[fieldName] = fieldValue
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))

      if (fieldName === 'nickname') isEditingNickname.value = false
      if (fieldName === 'phone') isEditingPhone.value = false
      if (fieldName === 'email') isEditingEmail.value = false

      ElMessage.success('修改成功')

      // 完善资料可能触发积分奖励，延迟刷新会员信息
      setTimeout(async () => {
        try {
          await userStore.fetchMemberInfo()
        } catch (e) {
          console.warn('刷新会员信息失败', e)
        }
      }, 500)
    } else {
      ElMessage.error(data.message || '修改失败')
    }
  } catch (error) {
    ElMessage.error('系统错误: ' + error.message)
  }
}

const saveNickname = () => saveField('nickname', editNickname.value)

const handleSignIn = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return

    const response = await fetch('http://localhost:8080/api/member/signin', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    })

    const data = await response.json()
    if (data.success) {
      const result = data.data
      ElMessage.success(result.message || `签到成功！积分+${result.pointsEarned}`)
      userStore.userInfo.currentPoints = result.currentPoints
      userStore.userInfo.totalPoints = result.totalPoints
      userStore.userInfo.signInDays = result.consecutiveDays
      userStore.userInfo.consecutiveSignDays = result.consecutiveDays
      const d = new Date()
      userStore.userInfo.lastSigninDate = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
    } else {
      ElMessage.warning(data.message || '签到失败')
    }
  } catch (error) {
    ElMessage.error('系统错误: ' + error.message)
  }
}

const alert = (msg) => window.alert(msg)

// ========== Points Mall Methods ==========

// 加载商品列表
const loadProducts = async () => {
  try {
    const response = await fetch('http://localhost:8080/api/member/mall/products')
    const data = await response.json()
    if (data.success) {
      products.value = data.data || []
    }
  } catch (error) {
    console.error('Failed to load products:', error)
  }
}

// 加载用户地址
const loadAddresses = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/member/addresses', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      addresses.value = data.data || []
      // 默认选中第一个地址或默认地址
      const defaultAddr = addresses.value.find(a => a.isDefault)
      if (defaultAddr) {
        selectedAddressId.value = defaultAddr.id
      } else if (addresses.value.length > 0) {
        selectedAddressId.value = addresses.value[0].id
      }
    }
  } catch (error) {
    console.error('Failed to load addresses:', error)
  }
}

// 加载订单列表
const loadOrders = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/member/mall/orders', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      orders.value = data.data || []
    }
  } catch (error) {
    console.error('Failed to load orders:', error)
  }
}

// 打开兑换弹窗
const openRedeemDialog = async (product) => {
  selectedProduct.value = product
  redeemQuantity.value = 1  // 重置数量为1
  await loadAddresses()
  showRedeemModal.value = true
}

// 确认兑换
const confirmRedeem = async () => {
  if (!selectedProduct.value || !selectedAddressId.value) return

  // 验证数量
  if (redeemQuantity.value < 1 || redeemQuantity.value > 10) {
    ElMessage.warning('兑换数量必须在1-10之间')
    return
  }

  const totalCost = selectedProduct.value.pointsPrice * redeemQuantity.value
  if (totalCost > (userStore.userInfo?.currentPoints || 0)) {
    ElMessage.warning('积分不足')
    return
  }

  isRedeeming.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/member/mall/redeem', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        productId: selectedProduct.value.id,
        addressId: selectedAddressId.value,
        quantity: redeemQuantity.value
      })
    })

    const data = await response.json()
    if (data.success) {
      ElMessage.success(data.message || '兑换成功！')
      showRedeemModal.value = false
      redeemQuantity.value = 1

      // 使用后端返回的实际消耗积分（含折扣）来更新用户积分
      const actualCost = data.data?.pointsCost || totalCost
      userStore.userInfo.currentPoints = (userStore.userInfo.currentPoints || 0) - actualCost
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))

      // 刷新商品列表
      await loadProducts()

      // 同步刷新最新会员信息
      try {
        await userStore.fetchMemberInfo()
      } catch (e) {
        console.warn('刷新会员信息失败', e)
      }
    } else {
      ElMessage.error(data.message || '兑换失败')
    }
  } catch (error) {
    ElMessage.error('系统错误: ' + error.message)
  } finally {
    isRedeeming.value = false
  }
}

// 取消订单
const handleCancelOrder = async (order) => {
  if (!confirm('确定要取消此订单吗？积分将会返还到您的账户。')) return

  cancellingOrderId.value = order.id
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`http://localhost:8080/api/member/mall/orders/${order.id}/cancel`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    })

    const data = await response.json()
    if (data.success) {
      ElMessage.success('订单已取消，积分已返还')

      // 更新用户积分（加回来）
      userStore.userInfo.currentPoints = (userStore.userInfo.currentPoints || 0) + order.pointsCost
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))

      // 刷新订单列表和商品列表
      await loadOrders()
      await loadProducts()

      // 同步刷新最新会员信息
      try {
        await userStore.fetchMemberInfo()
      } catch (e) {
        console.warn('刷新会员信息失败', e)
      }
    } else {
      ElMessage.error(data.message || '取消失败')
    }
  } catch (error) {
    ElMessage.error('系统错误')
  } finally {
    cancellingOrderId.value = null
  }
}

// 订单状态文本
const getStatusText = (status) => {
  const map = {
    pending: '待处理',
    processing: '处理中',
    shipped: '已发货',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// ========== 地址管理方法 ==========
const setDefaultAddress = async (addressId) => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`http://localhost:8080/api/member/addresses/${addressId}/default`, {
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      ElMessage.success('设置成功')
      await loadAddresses()
    } else {
      ElMessage.error(data.message || '设置失败')
    }
  } catch (error) {
    ElMessage.error('系统错误')
  }
}

const deleteAddress = async (addressId) => {
  if (!confirm('确定要删除这个地址吗？')) return
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`http://localhost:8080/api/member/addresses/${addressId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      ElMessage.success('删除成功')
      await loadAddresses()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('系统错误')
  }
}

// 获取选中的省市区名称
const getSelectedProvinceName = () => {
  const found = provinces.value.find(p => p.code === selectedProvinceCode.value)
  return found?.name || ''
}

const getSelectedCityName = () => {
  const found = cities.value.find(c => c.code === selectedCityCode.value)
  return found?.name || ''
}

const getSelectedDistrictName = () => {
  const found = districts.value.find(d => d.code === selectedDistrictCode.value)
  return found?.name || ''
}

// 打开添加地址弹窗
const openAddAddressModal = () => {
  isEditingAddress.value = false
  editingAddressId.value = null
  selectedProvinceCode.value = ''
  selectedCityCode.value = ''
  selectedDistrictCode.value = ''
  newAddress.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false }
  showAddAddressModal.value = true
}

// 打开编辑地址弹窗
const openEditAddressModal = (addr) => {
  isEditingAddress.value = true
  editingAddressId.value = addr.id
  newAddress.value = {
    receiverName: addr.receiverName,
    receiverPhone: addr.receiverPhone,
    province: addr.province,
    city: addr.city,
    district: addr.district || '',
    detailAddress: addr.detailAddress,
    isDefault: addr.isDefault
  }
  // 不自动选择省市区代码（因为需要反向查找，较复杂），用户需要重新选择
  selectedProvinceCode.value = ''
  selectedCityCode.value = ''
  selectedDistrictCode.value = ''
  showAddAddressModal.value = true
}

// 保存地址（添加或编辑）
const saveAddress = async () => {
  // 如果选择了省市区，使用选择的值；否则使用手动输入的值
  const provinceName = getSelectedProvinceName() || newAddress.value.province
  const cityName = getSelectedCityName() || newAddress.value.city
  const districtName = getSelectedDistrictName() || newAddress.value.district

  if (!newAddress.value.receiverName || !newAddress.value.receiverPhone ||
    !provinceName || !cityName || !newAddress.value.detailAddress) {
    ElMessage.warning('请填写完整的地址信息')
    return
  }

  const addressData = {
    ...newAddress.value,
    province: provinceName,
    city: cityName,
    district: districtName
  }

  try {
    const token = localStorage.getItem('token')
    let response

    if (isEditingAddress.value && editingAddressId.value) {
      // 更新地址
      response = await fetch(`http://localhost:8080/api/member/addresses/${editingAddressId.value}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(addressData)
      })
    } else {
      // 添加地址
      response = await fetch('http://localhost:8080/api/member/addresses', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(addressData)
      })
    }

    const data = await response.json()
    if (data.success) {
      ElMessage.success(isEditingAddress.value ? '修改成功' : '添加成功')
      closeAddressModal()
      await loadAddresses()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('系统错误')
  }
}

// 关闭地址弹窗
const closeAddressModal = () => {
  showAddAddressModal.value = false
  isEditingAddress.value = false
  editingAddressId.value = null
  selectedProvinceCode.value = ''
  selectedCityCode.value = ''
  selectedDistrictCode.value = ''
  newAddress.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: false }
}

// ========== 头像更换方法 ==========
const handleAvatarChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过2MB')
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      avatarPreview.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

const saveAvatar = async () => {
  // 由于没有后端文件上传接口，这里暂时使用本地预览作为演示
  // 实际项目中需要调用后端上传接口
  if (avatarPreview.value) {
    userStore.userInfo.avatar = avatarPreview.value
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
    showAvatarModal.value = false
    avatarPreview.value = ''
    ElMessage.success('头像更新成功')
  }
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    console.log('Redirecting to login...')
  } else {
    // 加载全部必要信息
    userStore.fetchUserInfo()
    userStore.fetchMemberInfo()
    loadProducts()
    loadAddresses()
  }
})

// 监听标签切换，进入个人信息页时刷新数据
watch(currentTab, (newTab) => {
  if (newTab === 'personal-info') {
    userStore.fetchUserInfo()
  }
})
</script>

<style scoped>
/* 
  Premium UI Design - Scoped to avoid conflicts 
  Theme: Modern Coffee Lounge (White, Warm Grey, Latte Gold)
*/

.member-layout {
  display: flex;
  min-height: 100vh;
  background-color: #F8F5F2;
  /* Warm paper white background */
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  color: #333;
  user-select: none;
  /* Disable text selection globally to prevent cursors */
}

.modern-input,
.invite-input,
input,
textarea {
  user-select: text !important;
  /* Re-enable selection for inputs */
}

/* --- Sidebar --- */
.sidebar {
  width: 280px;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
  padding: 40px 30px;
  box-shadow: 1px 0 20px rgba(0, 0, 0, 0.03);
  z-index: 100;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  overflow-y: auto;
  scrollbar-width: none;
  /* Firefox */
  -ms-overflow-style: none;
  /* IE 10+ */
}

.sidebar::-webkit-scrollbar {
  display: none;
  /* Chrome, Safari, Edge */
}

.brand-logo {
  font-size: 24px;
  letter-spacing: 4px;
  color: #333;
  margin: 0 0 50px 0;
  font-weight: 800;
}

.user-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 35px;
}

.avatar-ring {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  padding: 3px;
  border: 1px solid #E0E0E0;
  margin-bottom: 20px;
}

.user-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #2C2C2C;
}

.member-id {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  letter-spacing: 1px;
}

.navigation {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.nav-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #888;
  padding: 10px 0;
  transition: all 0.3s ease;
  position: relative;
}

.nav-link:hover,
.nav-link.active {
  color: #C69C6D;
  /* Latte Gold */
}

.indicator {
  width: 0;
  height: 2px;
  background: #C69C6D;
  margin-right: 0;
  transition: all 0.3s ease;
}

.nav-link.active .indicator {
  width: 20px;
  margin-right: 15px;
}

.nav-text {
  font-size: 15px;
  font-weight: 500;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: auto;
  border-top: 1px solid #F0F0F0;
  padding-top: 30px;
}

.footer-link {
  background: none;
  border: none;
  text-align: left;
  color: #999;
  cursor: pointer;
  font-size: 13px;
  transition: color 0.2s;
  padding: 0;
}

.footer-link:hover {
  color: #333;
}

/* --- Main Content --- */
.main-content {
  flex: 1;
  padding: 30px 60px;
  overflow-y: auto;
  margin-left: 280px;
  /* 为固定侧边栏留出空间 */
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}

.content-header h3 {
  font-size: 28px;
  font-weight: 300;
  color: #1a1a1a;
  margin: 0;
}

.date-badge,
.points-badge {
  color: #888;
  font-size: 14px;
}

/* Digital Card */
.digital-card {
  width: 100%;
  height: 220px;
  border-radius: 20px;
  background: linear-gradient(135deg, #1a1a1a, #2c2c2c);
  padding: 25px 30px;
  position: relative;
  color: white;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  margin-bottom: 30px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* Card Themes - Premium Design */
.digital-card.basic {
  background: linear-gradient(135deg, #804A00, #B87333, #804A00);
  /* Bronze/Copper Theme */
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.digital-card.basic::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.1) 0%, transparent 60%);
}

.digital-card.silver {
  background: linear-gradient(135deg, #C0C0C0, #A8A8A8, #D8D8D8);
  color: #333;
}

.digital-card.silver .label {
  color: #555;
}

.digital-card.silver .card-tier {
  color: #666;
}

.digital-card.silver::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 40%, rgba(255, 255, 255, 0.4) 50%, transparent 60%);
  animation: shine 3s ease-in-out infinite;
}

@keyframes shine {

  0%,
  100% {
    transform: translateX(-50%) translateY(-50%) rotate(25deg);
  }

  50% {
    transform: translateX(50%) translateY(50%) rotate(25deg);
  }
}

.digital-card.gold {
  background: linear-gradient(135deg, #FFD700, #DAA520, #FFB90F);
  color: #4a3000;
}

.digital-card.gold .label {
  color: #6a4a00;
}

.digital-card.gold .card-tier {
  color: #8a5a00;
}

.digital-card.gold::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 30%, rgba(255, 255, 255, 0.4) 50%, transparent 70%);
  animation: gold-shine 3s ease-in-out infinite;
}

@keyframes gold-shine {

  0%,
  100% {
    transform: translateX(-50%) translateY(-50%) rotate(25deg);
  }

  50% {
    transform: translateX(50%) translateY(50%) rotate(25deg);
  }
}

/* 黑金会员 - 最尊贵的设计 */
.digital-card.black {
  background: linear-gradient(135deg, #0D0D0D, #1A1A1A, #2D2D2D, #0D0D0D);
  border: 1px solid #D4AF37;
  color: #D4AF37;
  animation: black-pulse 4s ease-in-out infinite;
}

@keyframes black-pulse {

  0%,
  100% {
    box-shadow: 0 20px 60px rgba(212, 175, 55, 0.2), inset 0 0 40px rgba(212, 175, 55, 0.05);
  }

  50% {
    box-shadow: 0 25px 70px rgba(212, 175, 55, 0.35), inset 0 0 60px rgba(212, 175, 55, 0.1);
  }
}

.digital-card.black .label {
  color: #B8A054;
}

.digital-card.black .value {
  color: #FFD700;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.3);
}

.digital-card.black .card-tier {
  color: #FFD700;
  font-weight: 600;
  text-shadow: 0 0 15px rgba(255, 215, 0, 0.5);
}

.digital-card.black .card-chip {
  background: linear-gradient(135deg, #D4AF37, #FFD700);
  border: none;
}

.digital-card.black::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 40%, rgba(212, 175, 55, 0.15) 50%, transparent 60%);
  animation: black-shine 5s ease-in-out infinite;
}

@keyframes black-shine {

  0%,
  100% {
    transform: translateX(-100%) rotate(25deg);
  }

  50% {
    transform: translateX(100%) rotate(25deg);
  }
}

.digital-card.black::after {
  content: '♦';
  position: absolute;
  bottom: 15px;
  right: 20px;
  font-size: 50px;
  opacity: 0.15;
  color: #D4AF37;
}

.card-chip {
  width: 50px;
  height: 35px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.card-balance .label,
.card-holder .label {
  display: block;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  opacity: 0.6;
  margin-bottom: 5px;
}

.card-balance .value {
  font-size: 36px;
  font-weight: 300;
}

.card-holder .value {
  font-size: 16px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.card-tier {
  position: absolute;
  top: 30px;
  right: 30px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 2px;
  opacity: 0.8;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
}

.stat-label {
  color: #999;
  font-size: 12px;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-value.highlight {
  color: #C69C6D;
}

/* Widget Section */
.widget-section {
  background: white;
  padding: 20px 25px;
  border-radius: 12px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.02);
  margin-bottom: 25px;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.widget-header h4 {
  margin: 0;
  font-weight: 500;
  color: #333;
}

.signin-btn {
  background: #333;
  color: white;
  border: none;
  padding: 10px 25px;
  border-radius: 30px;
  cursor: pointer;
  font-size: 14px;
  transition: transform 0.2s;
}

.signin-btn:hover {
  transform: translateY(-2px);
  background: #111;
}

.signin-btn:disabled {
  background: #eee;
  color: #999;
  transform: none;
  cursor: not-allowed;
}

.progress-track {
  position: relative;
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
}

.track-line {
  position: absolute;
  top: 50%;
  left: 20px;
  right: 20px;
  height: 2px;
  background: #f0f0f0;
  z-index: 1;
  transform: translateY(-50%);
}

.track-line .fill {
  height: 100%;
  background: #C69C6D;
  transition: width 0.5s ease;
}

.step {
  z-index: 2;
  background: white;
  border-radius: 50%;
  padding: 5px;
  /* Creates gap around line */
}

.step-circle {
  width: 30px;
  height: 30px;
  background: #f0f0f0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
  transition: all 0.3s;
}

.step.active .step-circle {
  background: #C69C6D;
  color: white;
}

/* Form Styles */
.form-container {
  max-width: 600px;
}

.form-group {
  margin-bottom: 30px;
}

.form-group label {
  display: block;
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.static-value {
  font-size: 16px;
  color: #333;
}

.modern-input {
  border: none;
  outline: none;
  font-size: 16px;
  width: 100%;
  background: transparent;
  color: #333;
  padding: 5px 0;
}

.edit-btn,
.save-btn,
.cancel-btn {
  background: none;
  border: none;
  font-size: 14px;
  cursor: pointer;
  padding: 5px 15px;
}

.edit-btn {
  color: #C69C6D;
}

.save-btn {
  color: #333;
  font-weight: 600;
}

.cancel-btn {
  color: #999;
}

/* Mall Styles */
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 30px;
}

.mall-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.03);
  transition: transform 0.3s;
}

.mall-card:hover {
  transform: translateY(-5px);
}

.card-image {
  height: 200px;
  background-color: #f9f9f9;
  background-size: cover;
  background-position: center;
}

.placeholder-cup {
  background-image: url('/images/cup.png');
}

.placeholder-beans {
  background-image: url('/images/beans.jpg');
}

.card-details {
  padding: 20px;
}

.card-details h4 {
  margin: 0 0 5px 0;
}

.card-details .price {
  display: block;
  color: #C69C6D;
  font-weight: 600;
  margin-bottom: 15px;
}

.redeem-btn {
  width: 100%;
  padding: 10px;
  background: #f0f0f0;
  border: none;
  color: #333;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.redeem-btn:hover {
  background: #333;
  color: white;
}

/* Animations */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ========== Points Mall Styles ========== */

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}

.mall-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
}

.mall-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.mall-card .card-image {
  height: 180px;
  background-size: cover;
  background-position: center;
  background-color: #f5f0eb;
}

.mall-card .card-details {
  padding: 16px;
}

.mall-card .card-details h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #333;
}

.product-desc {
  font-size: 12px;
  color: #888;
  margin-bottom: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mall-card .stock {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.mall-card .stock.low {
  color: #e74c3c;
}

.mall-card .redeem-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.no-data {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 14px;
}

.mall-footer {
  margin-top: 24px;
  text-align: center;
}

.history-btn {
  padding: 12px 32px;
  background: transparent;
  border: 2px solid #B8956B;
  color: #B8956B;
  border-radius: 30px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.history-btn:hover {
  background: #B8956B;
  color: white;
}

/* Orders Section */
.orders-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
}

.orders-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.orders-section .section-header h4 {
  margin: 0;
  font-size: 18px;
}

.back-btn {
  padding: 8px 20px;
  background: transparent;
  border: 1px solid #ddd;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
}

.back-btn:hover {
  background: #f5f5f5;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f9f9f9;
  font-size: 13px;
}

.order-no {
  color: #666;
}

.order-status {
  font-weight: 500;
}

.order-status.pending {
  color: #f39c12;
}

.order-status.processing {
  color: #3498db;
}

.order-status.shipped {
  color: #9b59b6;
}

.order-status.completed {
  color: #27ae60;
}

.order-status.cancelled {
  color: #e74c3c;
}

.order-body {
  display: flex;
  gap: 16px;
  padding: 16px;
}

.order-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  background: #f5f5f5;
}

.order-info {
  flex: 1;
}

.order-info .product-name {
  font-weight: 500;
  margin-bottom: 6px;
}

.order-info .points-cost {
  color: #B8956B;
  font-size: 14px;
}

.order-info .order-time {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

/* Redeem Modal */
.redeem-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 20px;
  padding: 32px;
  width: 90%;
  max-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin: 0 0 24px;
  text-align: center;
  font-size: 20px;
}

.product-preview {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 12px;
  margin-bottom: 24px;
}

.product-preview img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.product-preview .name {
  font-weight: 500;
  margin-bottom: 8px;
}

.product-preview .cost {
  color: #666;
}

.product-preview .cost strong {
  color: #B8956B;
  font-size: 18px;
}

.address-section {
  margin-bottom: 24px;
}

.address-section h4 {
  margin: 0 0 12px;
  font-size: 15px;
}

.address-item {
  padding: 12px 16px;
  border: 2px solid #eee;
  border-radius: 10px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.address-item:hover {
  border-color: #ddd;
}

.address-item.selected {
  border-color: #B8956B;
  background: #FDF8F3;
}

.address-item .receiver {
  font-weight: 500;
  display: block;
  margin-bottom: 4px;
}

.address-item .addr-detail {
  font-size: 13px;
  color: #666;
}

.default-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  background: #B8956B;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
}

.no-address {
  text-align: center;
  padding: 24px;
  color: #999;
}

.add-addr-btn {
  margin-top: 12px;
  padding: 10px 24px;
  background: #B8956B;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-actions .cancel-btn {
  padding: 12px 32px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 30px;
  cursor: pointer;
}

.modal-actions .confirm-btn {
  padding: 12px 32px;
  background: #B8956B;
  color: white;
  border: none;
  border-radius: 30px;
  cursor: pointer;
}

.modal-actions .confirm-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* ========== 头像悬浮效果 ========== */
.avatar-ring {
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 50%;
}

.avatar-overlay span {
  color: white;
  font-size: 12px;
}

.avatar-ring:hover .avatar-overlay {
  opacity: 1;
}

/* ========== 会员权益视图 ========== */
.benefits-view {
  max-width: 1200px;
}

.benefits-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 40px;
}

.benefit-card {
  background: white;
  border-radius: 15px;
  padding: 25px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.benefit-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
}

.benefit-card.current {
  border-color: #C69C6D;
}

/* 基础会员卡片 - 铜色调 */
.benefit-card.basic {
  background: linear-gradient(135deg, #FDF5EE 0%, #E8D5C4 50%, #F5E6D8 100%);
  border-color: rgba(184, 134, 91, 0.2);
}

.benefit-card.basic::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #B8865B, #D4A574, #B8865B);
}

.benefit-card.basic .benefit-header h4 {
  color: #8B6914;
}

.benefit-card.basic .level-badge {
  color: #A67C52;
}

.benefit-card.basic .benefit-list li::before {
  color: #B8865B;
}

/* 白银会员卡片 - 银色渐变 */
.benefit-card.silver {
  background: linear-gradient(135deg, #f8f9fa 0%, #dee2e6 50%, #e9ecef 100%);
  border-color: rgba(192, 192, 192, 0.3);
}

.benefit-card.silver::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #C0C0C0, #E8E8E8, #C0C0C0);
}

.benefit-card.silver .benefit-header h4 {
  color: #6c757d;
}

.benefit-card.silver .level-badge {
  color: #868e96;
}

/* 黄金会员卡片 - 金色渐变 */
.benefit-card.gold {
  background: linear-gradient(135deg, #FFF9E6 0%, #FFE8B3 50%, #FFF4D6 100%);
  border-color: rgba(218, 165, 32, 0.3);
}

.benefit-card.gold::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #DAA520, #FFD700, #DAA520);
}

.benefit-card.gold .benefit-header h4 {
  color: #B8860B;
}

.benefit-card.gold .level-badge {
  color: #C68B00;
}

.benefit-card.gold .benefit-list li::before {
  color: #DAA520;
}

/* 黑金会员卡片 - 奢华黑金 */
.benefit-card.black {
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 50%, #1a1a1a 100%);
  border-color: rgba(212, 175, 55, 0.5);
  color: #D4AF37;
}

.benefit-card.black::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #D4AF37, #FFD700, #D4AF37);
  animation: gold-glow 2s ease-in-out infinite;
}

@keyframes gold-glow {

  0%,
  100% {
    opacity: 0.7;
  }

  50% {
    opacity: 1;
    box-shadow: 0 0 10px rgba(212, 175, 55, 0.5);
  }
}

.benefit-card.black .benefit-header h4 {
  color: #FFD700;
}

.benefit-card.black .level-badge {
  color: #D4AF37;
}

.benefit-card.black .benefit-list li {
  color: #B8A054;
}

.benefit-card.black .benefit-list li::before {
  color: #FFD700;
}

.benefit-card.black .benefit-header {
  border-bottom-color: rgba(212, 175, 55, 0.3);
}

.benefit-card.current::after {
  content: '当前等级';
  position: absolute;
  top: 15px;
  right: -30px;
  background: #C69C6D;
  color: white;
  font-size: 11px;
  padding: 3px 35px;
  transform: rotate(45deg);
}

.benefit-header {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.benefit-header h4 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}

.level-badge {
  font-size: 12px;
  color: #999;
}

.benefit-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.benefit-list li {
  padding: 8px 0;
  padding-left: 20px;
  position: relative;
  font-size: 14px;
  color: #666;
}

.benefit-list li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #C69C6D;
}

.current-level-info {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.02);
}

.current-level-info p {
  margin: 10px 0;
  color: #666;
}

.current-level-info strong {
  color: #C69C6D;
  font-size: 18px;
}

/* ========== 地址管理 ========== */
.address-management {
  margin-top: 40px;
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.02);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.add-btn {
  background: #C69C6D;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.add-btn:hover {
  background: #B88A5A;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.address-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f8f8;
  border-radius: 10px;
  border: 1px solid #eee;
  transition: all 0.3s;
}

.address-card.default {
  border-color: #C69C6D;
  background: #FFF9F0;
}

.addr-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.receiver-name {
  font-weight: 600;
  font-size: 15px;
}

.receiver-phone {
  color: #888;
  font-size: 14px;
}

.default-badge {
  background: #C69C6D;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
}

.addr-detail {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.addr-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  background: none;
  border: 1px solid #ddd;
  padding: 6px 12px;
  border-radius: 15px;
  font-size: 12px;
  cursor: pointer;
  color: #666;
  transition: all 0.3s;
}

.action-btn:hover {
  border-color: #C69C6D;
  color: #C69C6D;
}

.action-btn.delete:hover {
  border-color: #e74c3c;
  color: #e74c3c;
}

/* ========== 我的订单视图 ========== */
.orders-view .orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.orders-view .order-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.02);
}

.order-footer {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.receiver-info {
  font-weight: 500;
  margin: 0 0 5px 0;
}

.receiver-address {
  color: #888;
  font-size: 14px;
  margin: 0;
}

.order-quantity {
  color: #888;
  font-size: 14px;
  margin: 4px 0;
}

.go-mall-btn {
  background: #C69C6D;
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 25px;
  cursor: pointer;
  margin-top: 15px;
  transition: background 0.3s;
}

.go-mall-btn:hover {
  background: #B88A5A;
}

/* ========== 头像和地址模态框 ========== */
.avatar-modal,
.address-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.avatar-preview-area {
  text-align: center;
  padding: 30px 0;
}

.preview-img {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #eee;
}

.select-btn {
  background: #666;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  margin-right: 10px;
}

.address-modal .modal-content {
  width: 450px;
}

/* 优化后的地址模态框样式 */
.address-modal-content {
  width: 600px !important;
  max-width: 90vw;
  max-height: 85vh;
  overflow-y: auto;
  scrollbar-width: none;
  /* Firefox */
  -ms-overflow-style: none;
  /* IE/Edge */
}

.address-modal-content::-webkit-scrollbar {
  display: none;
  /* Chrome/Safari */
}

.address-modal-content h3 {
  margin: 0 0 25px 0;
  font-size: 20px;
  font-weight: 500;
  color: #333;
  text-align: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

/* 表单行布局 */
.form-row {
  display: flex;
  gap: 15px;
  margin-bottom: 5px;
}

.form-row .form-item {
  flex: 1;
}

.region-row {
  margin-bottom: 15px;
}

.region-row .form-item {
  min-width: 0;
}

/* 下拉选择框样式 */
.region-select {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  background-color: #fff;
  color: #333;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  transition: border-color 0.3s;
}

.region-select:focus {
  border-color: #C69C6D;
  outline: none;
}

.region-select:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

.region-select option {
  padding: 10px;
}

/* 必填标记 */
.required {
  color: #e74c3c;
}

/* 当前地址提示 */
.current-region-hint {
  background: #FFF9F0;
  padding: 10px 15px;
  border-radius: 8px;
  margin-bottom: 15px;
  font-size: 13px;
  color: #666;
  border: 1px dashed #C69C6D;
}

.hint-text {
  color: #999;
  font-size: 12px;
}

/* 全宽表单项 */
.form-item.full-width {
  width: 100%;
}

.form-item {
  margin-bottom: 15px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  color: #555;
  font-size: 14px;
  font-weight: 500;
}

.form-item input[type="text"] {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.3s;
}

.form-item input[type="text"]:focus {
  border-color: #C69C6D;
  outline: none;
}

.form-item.checkbox label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-weight: 400;
}

.form-item.checkbox input {
  width: 18px;
  height: 18px;
  accent-color: #C69C6D;
}

/* 隐藏主内容区滚动条 */
.main-content {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.main-content::-webkit-scrollbar {
  display: none;
}

/* ========== 数量选择器 ========== */
.quantity-section {
  margin: 20px 0;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 10px;
}

.quantity-section label {
  font-weight: 500;
  color: #333;
  margin-right: 15px;
}

.quantity-control {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
}

.qty-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  font-size: 18px;
  cursor: pointer;
  color: #333;
  transition: all 0.2s;
}

.qty-btn:hover:not(:disabled) {
  border-color: #C69C6D;
  color: #C69C6D;
}

.qty-btn:disabled {
  background: #f0f0f0;
  color: #ccc;
  cursor: not-allowed;
}

.qty-input {
  width: 60px;
  height: 32px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
}

.qty-input:focus {
  border-color: #C69C6D;
  outline: none;
}

.qty-tip {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.total-cost {
  padding: 15px;
  background: #FFF9F0;
  border-radius: 8px;
  text-align: center;
  margin-bottom: 20px;
}

.total-cost strong {
  font-size: 24px;
  color: #C69C6D;
  margin: 0 8px;
}

.total-cost strong.has-discount {
  color: #E91E63;
}

.original-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
  margin-right: 8px;
}

.discount-badge {
  display: inline-block;
  padding: 2px 6px;
  background: linear-gradient(135deg, #E91E63 0%, #C2185B 100%);
  color: white;
  font-size: 11px;
  border-radius: 4px;
  font-weight: 600;
  margin-right: 8px;
}

.balance-hint {
  font-size: 13px;
  color: #888;
}

.unit-price,
.stock-info {
  font-size: 13px;
  color: #666;
  margin: 4px 0;
}

/* ========== 订单操作按钮 ========== */
.order-actions {
  padding: 12px 0;
  border-top: 1px solid #eee;
  margin-top: 12px;
  text-align: right;
}

.cancel-order-btn {
  padding: 8px 20px;
  background: none;
  border: 1px solid #e74c3c;
  color: #e74c3c;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-order-btn:hover:not(:disabled) {
  background: #e74c3c;
  color: white;
}

.cancel-order-btn:disabled {
  border-color: #ccc;
  color: #ccc;
  cursor: not-allowed;
}

.order-quantity {
  font-size: 13px;
  color: #666;
  margin: 4px 0;
}

/* 订单状态颜色 */
.order-status.cancelled {
  color: #999;
  background: #f0f0f0;
}

/* 操作提示 */
.action-hint {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

/* ========== 积分获取渠道 ========== */
.points-guide-section {
  margin-top: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #FFF9F0 0%, #FFF5E6 100%);
  border-radius: 16px;
  border: 1px solid rgba(198, 156, 109, 0.2);
}

.points-guide-section .section-title {
  margin-bottom: 20px;
}

.points-guide-section .section-title h4 {
  margin: 0 0 4px 0;
  font-size: 18px;
  color: #2C1810;
}

.points-guide-section .section-title .subtitle {
  font-size: 13px;
  color: #888;
}

.points-channels {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.channel-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #eee;
  transition: all 0.3s ease;
}

.channel-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.channel-card.done {
  background: #f8fff8;
  border-color: #4CAF50;
}

.channel-card.coming-soon {
  opacity: 0.7;
  background: #f9f9f9;
}

.channel-icon {
  font-size: 28px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FFF5E6;
  border-radius: 12px;
}

.channel-info {
  flex: 1;
}

.channel-name {
  display: block;
  font-weight: 600;
  color: #2C1810;
  font-size: 14px;
}

.channel-desc {
  display: block;
  font-size: 12px;
  color: #888;
  margin-top: 2px;
}

.channel-points {
  text-align: right;
}

.channel-points .points-value {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: #C69C6D;
}

.channel-points .points-label {
  display: block;
  font-size: 11px;
  color: #999;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.status-badge.done {
  background: #E8F5E9;
  color: #4CAF50;
}

.status-badge.coming {
  background: #f0f0f0;
  color: #999;
}

.go-btn {
  padding: 6px 14px;
  background: linear-gradient(135deg, #C69C6D 0%, #A67C52 100%);
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.go-btn:hover {
  transform: scale(1.05);
}

.go-btn.simulate {
  background: linear-gradient(135deg, #FF9800 0%, #F57C00 100%);
}

.level-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding: 12px 16px;
  background: white;
  border-radius: 10px;
  border: 1px dashed #C69C6D;
}

.level-tip.max-level {
  background: linear-gradient(135deg, #2C1810 0%, #4A3728 100%);
  border: none;
  color: #FFD700;
}

.level-tip .tip-icon {
  font-size: 20px;
}

.level-tip .tip-text {
  flex: 1;
  font-size: 13px;
  color: #666;
}

.level-tip.max-level .tip-text {
  color: #FFD700;
}

.level-tip .tip-text strong {
  color: #C69C6D;
}

.level-tip.max-level .tip-text strong {
  color: #FFD700;
}

.view-benefits-btn {
  padding: 6px 12px;
  background: none;
  border: 1px solid #C69C6D;
  color: #C69C6D;
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.view-benefits-btn:hover {
  background: #C69C6D;
  color: white;
}

/* ========== 模拟消费弹窗 ========== */
.simulate-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.simulate-modal .modal-content {
  background: white;
  border-radius: 20px;
  padding: 30px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.simulate-modal h3 {
  margin: 0 0 8px 0;
  font-size: 22px;
  text-align: center;
}

.simulate-modal .modal-desc {
  text-align: center;
  color: #888;
  font-size: 14px;
  margin-bottom: 24px;
}

.consume-form {
  margin-bottom: 24px;
}

.amount-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f5f5f5;
  border-radius: 12px;
  margin-bottom: 20px;
}

.amount-input-group .currency {
  font-size: 24px;
  font-weight: 600;
  color: #C69C6D;
}

.amount-input {
  flex: 1;
  border: none;
  background: none;
  font-size: 28px;
  font-weight: 600;
  color: #2C1810;
  outline: none;
  width: 100%;
}

.amount-input::-webkit-inner-spin-button {
  display: none;
}

.points-preview {
  background: #FFF9F0;
  border-radius: 12px;
  padding: 16px;
}

.preview-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
}

.preview-item:last-child {
  border-bottom: none;
}

.preview-item .label {
  color: #888;
  font-size: 14px;
}

.preview-item .value {
  font-weight: 600;
  color: #2C1810;
}

.preview-item .value.multiplier {
  color: #FF9800;
}

.preview-item .value.points {
  color: #C69C6D;
  font-size: 18px;
}

.preview-item.total {
  padding-top: 12px;
  margin-top: 4px;
}

.level-bonus-tip {
  margin-top: 12px;
  padding: 10px;
  background: linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%);
  border-radius: 8px;
  text-align: center;
  font-size: 13px;
  color: #E65100;
}

.simulate-modal .modal-actions {
  display: flex;
  gap: 12px;
}

.simulate-modal .cancel-btn {
  flex: 1;
  padding: 14px;
  background: #f5f5f5;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.simulate-modal .confirm-btn {
  flex: 2;
  padding: 14px;
  background: linear-gradient(135deg, #FF9800 0%, #F57C00 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.simulate-modal .confirm-btn:hover:not(:disabled) {
  transform: scale(1.02);
}

.simulate-modal .confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ========== 查看积分明细按钮 ========== */
.points-detail-link {
  margin-top: 20px;
  text-align: center;
}

.detail-btn {
  padding: 10px 24px;
  background: white;
  border: 1px solid #C69C6D;
  color: #C69C6D;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.detail-btn:hover {
  background: #C69C6D;
  color: white;
}

/* ========== 积分明细弹窗 ========== */
.points-detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.points-detail-modal .modal-content {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.points-detail-modal .modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.points-detail-modal h3 {
  margin: 0;
  font-size: 20px;
}

.points-detail-modal .close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.points-detail-modal .close-btn:hover {
  color: #333;
}

.balance-summary {
  display: flex;
  gap: 20px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #FFF9F0 0%, #FFF5E6 100%);
}

.balance-summary .balance-item {
  flex: 1;
  text-align: center;
}

.balance-summary .label {
  display: block;
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
}

.balance-summary .value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #C69C6D;
}

.transactions-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.transaction-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.transaction-item:last-child {
  border-bottom: none;
}

.transaction-left {
  flex: 1;
}

.transaction-type {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.transaction-type.income {
  background: #E8F5E9;
  color: #4CAF50;
}

.transaction-type.expense {
  background: #FFF3E0;
  color: #FF9800;
}

.transaction-desc {
  display: block;
  font-size: 13px;
  color: #333;
  margin-top: 4px;
}

.transaction-time {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.transaction-right {
  text-align: right;
}

.transaction-amount {
  display: block;
  font-size: 16px;
  font-weight: 600;
}

.transaction-amount.income {
  color: #4CAF50;
}

.transaction-amount.expense {
  color: #FF9800;
}

.transaction-balance {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.loading-state,
.empty-state {
  padding: 40px;
  text-align: center;
  color: #999;
}

/* ========== 邀请功能（个人信息页）样式 ========== */
.invite-wrapper {
  display: flex !important;
  align-items: center;
  gap: 12px;
}

.invite-code-text {
  font-family: 'Courier New', monospace;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  letter-spacing: 1px;
  background: #f5f5f5;
  padding: 6px 12px;
  border-radius: 6px;
}

.copy-code-btn {
  background: #e0e0e0;
  color: #666;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-code-btn:hover {
  background: #d0d0d0;
  color: #333;
}

.invite-tip {
  color: #999;
  font-size: 12px;
}

.verify-btn {
  margin-left: 12px;
  background: #d4a762;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  min-width: 80px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.verify-btn:hover:not(:disabled) {
  background: #c39651;
}

.verify-btn:disabled {
  background: #e0e0e0;
  cursor: not-allowed;
}

.invite-input[type="text"] {
  text-transform: uppercase;
  letter-spacing: 1px;
}

.field-hint {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.static-value.completed {
  color: #52c41a;
  font-weight: 500;
}

/* 邀请好友卡片 - 已完成状态 */
.channel-card.invite-card.done {
  border: 1px dashed #52c41a;
  background: #f6ffed;
}

.channel-card.invite-card.done .channel-name,
.channel-card.invite-card.done .channel-desc,
.channel-card.invite-card.done .points-value,
.channel-card.invite-card.done .points-label {
  opacity: 0.6;
}

.status-badge.done {
  display: inline-block;
  padding: 4px 12px;
  background: #52c41a;
  color: white;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

/* ========== 咖啡点单页面样式 ========== */
.coffee-order-view {
  padding: 0;
}

.coffee-products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-top: 24px;
}

.coffee-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
}

.coffee-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.coffee-image {
  height: 160px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.coffee-details {
  padding: 16px 20px 20px;
}

.coffee-details h4 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.coffee-desc {
  font-size: 13px;
  color: #888;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.coffee-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.coffee-price {
  font-size: 22px;
  font-weight: 700;
  color: #8B4513;
}

.earn-points {
  font-size: 13px;
  color: #52c41a;
  background: #f6ffed;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.quantity-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.quantity-row .qty-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #ddd;
  border-radius: 50%;
  background: white;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.quantity-row .qty-btn:hover {
  background: #f5f5f5;
  border-color: #8B4513;
}

.quantity-row .qty-value {
  font-size: 18px;
  font-weight: 600;
  min-width: 30px;
  text-align: center;
}

.order-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #8B4513, #A0522D);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.order-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #A0522D, #8B4513);
  box-shadow: 0 4px 15px rgba(139, 69, 19, 0.3);
}

.order-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 消费订单记录 */
.coffee-order-history {
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #eee;
}

.coffee-order-history h4 {
  font-size: 18px;
  color: #333;
  margin: 0 0 20px 0;
}

.coffee-order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.coffee-order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 12px;
}

.order-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-product {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.order-time {
  font-size: 12px;
  color: #999;
}

.order-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.order-amount {
  font-size: 16px;
  font-weight: 600;
  color: #8B4513;
}

.order-points {
  font-size: 13px;
  color: #52c41a;
  font-weight: 500;
}

/* 菜单浏览视图 */
.menu-browse-view {
  padding: 30px;
}

.menu-products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-top: 24px;
}

.menu-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.menu-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.menu-image {
  width: 100%;
  height: 180px;
  background-size: cover;
  background-position: center;
  background-color: #f5f5f5;
}

.menu-details {
  padding: 20px;
}

.menu-details h4 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.menu-desc {
  font-size: 14px;
  color: #666;
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.menu-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.menu-price {
  font-size: 20px;
  font-weight: 700;
  color: #8B4513;
}

.view-detail-btn {
  font-size: 13px;
  color: #8B4513;
  padding: 6px 12px;
  border-radius: 20px;
  background: rgba(139, 69, 19, 0.1);
  font-weight: 500;
}

.menu-tip {
  margin-top: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #fff8e1 0%, #fffde7 100%);
  border-radius: 12px;
  text-align: center;
}

.menu-tip p {
  margin: 0;
  font-size: 14px;
  color: #8B4513;
}

/* 商品详情弹窗 */
.product-detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.product-detail-modal .modal-content {
  background: #fff;
  border-radius: 20px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow: hidden;
  position: relative;
}

.product-detail-modal .close-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  z-index: 10;
}

.product-detail-modal .detail-image {
  width: 100%;
  height: 250px;
  background-size: cover;
  background-position: center;
  background-color: #f5f5f5;
}

.product-detail-modal .detail-info {
  padding: 24px;
}

.product-detail-modal .detail-info h3 {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 10px 0;
  color: #333;
}

.product-detail-modal .price {
  font-size: 28px;
  font-weight: 700;
  color: #8B4513;
  margin: 0 0 16px 0;
}

.product-detail-modal .description {
  font-size: 15px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 20px 0;
}

.product-detail-modal .points-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #f9f9f9;
  border-radius: 10px;
  margin-bottom: 16px;
}

.product-detail-modal .points-label {
  font-size: 14px;
  color: #666;
}

.product-detail-modal .points-value {
  font-size: 16px;
  font-weight: 600;
  color: #52c41a;
}

.product-detail-modal .mobile-tip {
  padding: 16px;
  background: linear-gradient(135deg, #fff8e1 0%, #fffde7 100%);
  border-radius: 10px;
}

.product-detail-modal .mobile-tip p {
  margin: 0;
  font-size: 14px;
  color: #8B4513;
  text-align: center;
}
</style>

