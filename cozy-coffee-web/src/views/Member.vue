<template>
  <div class="member-layout" :class="themeClass">
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
          <LayoutDashboard :size="20" class="nav-icon" />
          <span class="nav-text">会员中心</span>
        </a>

        <a href="#" class="nav-link" :class="{ active: currentTab === 'coffee-order' }"
          @click.prevent="currentTab = 'coffee-order'">
          <span class="indicator"></span>
          <Coffee :size="20" class="nav-icon" />
          <span class="nav-text">咖啡下单</span>
        </a>
        <a href="#" class="nav-link" :class="{ active: currentTab === 'points-mall' }"
          @click.prevent="currentTab = 'points-mall'; loadProducts()">
          <span class="indicator"></span>
          <ShoppingBag :size="20" class="nav-icon" />
          <span class="nav-text">积分商城</span>
        </a>
        
        <!-- 可展开的历史订单菜单 -->
        <div class="nav-group">
          <a href="#" class="nav-link nav-parent" 
            :class="{ active: isOrdersExpanded || currentTab === 'coffee-orders-history' || currentTab === 'redeem-orders-history' }"
            @click.prevent="isOrdersExpanded = !isOrdersExpanded">
            <span class="indicator"></span>
            <ClipboardList :size="20" class="nav-icon" />
            <span class="nav-text">历史订单</span>
            <ChevronRight :size="16" class="expand-icon" :class="{ expanded: isOrdersExpanded }" />
          </a>
          <div class="nav-submenu" :class="{ expanded: isOrdersExpanded }">
            <a href="#" class="nav-link sub-link" :class="{ active: currentTab === 'coffee-orders-history' }"
              @click.prevent="currentTab = 'coffee-orders-history'; loadCoffeeOrders()">
              <span class="left-dot"></span>
              <span class="nav-text">咖啡订单</span>
            </a>
            <a href="#" class="nav-link sub-link" :class="{ active: currentTab === 'redeem-orders-history' }"
              @click.prevent="currentTab = 'redeem-orders-history'; loadRedeemOrders()">
              <span class="left-dot"></span>
              <span class="nav-text">兑换订单</span>
            </a>
          </div>
        </div>

        <a href="#" class="nav-link" :class="{ active: currentTab === 'personal-info' }"
          @click.prevent="currentTab = 'personal-info'">
          <span class="indicator"></span>
          <User :size="20" class="nav-icon" />
          <span class="nav-text">个人信息</span>
        </a>
        <a href="#" class="nav-link" :class="{ active: currentTab === 'my-coupons' }"
          @click.prevent="currentTab = 'my-coupons'">
          <span class="indicator"></span>
          <Ticket :size="20" class="nav-icon" />
          <span class="nav-text">我的券包</span>
        </a>
        <a href="#" class="nav-link" :class="{ active: currentTab === 'member-benefits' }"
          @click.prevent="currentTab = 'member-benefits'">
          <span class="indicator"></span>
          <Crown :size="20" class="nav-icon" />
          <span class="nav-text">会员权益</span>
        </a>
      </nav>


      <div class="sidebar-footer">
        <button @click="router.push('/')" class="footer-link">
          <Home :size="18" /> 返回首页
        </button>
        <button @click="handleLogout" class="footer-link logout">
          <LogOut :size="18" /> 退出登录
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
            <div class="header-badges">
              <span class="cozy-day-badge" v-if="isCozyDay">
                <span class="badge-icon">☕</span>
                <span class="badge-text">今日会员日 · 积分+0.5x</span>
              </span>
              <span class="date-badge">{{ new Date().toLocaleDateString() }}</span>
            </div>
          </header>

          <div class="expiring-alert" v-if="userStore.userInfo?.expiringPoints > 0">
            <span class="alert-icon">⏰</span>
            <span>您有 <strong>{{ userStore.userInfo.expiringPoints }}</strong> 积分即将过期，快去使用吧</span>
            <button @click="currentTab = 'member-benefits'" class="use-btn">去兑换</button>
          </div>

          <!-- 1. Hero Card: 5-Tier High Fidelity System -->
          <div class="digital-card premium-hero-card" :class="[userStore.userLevel || 'base', { dormant: userStore.userInfo?.memberStatus === 'DORMANT' }]">
             <!-- Texture & Effect Layers -->
             <div class="card-layer texture"></div>
             <div class="card-layer shine-effect"></div>
             <div class="card-layer holographic-overlay" v-if="userStore.userLevel === 'diamond'"></div>
             <div class="card-layer pattern-overlay"></div>

             <div class="hero-content-grid">
                <!-- Top Left: Brand -->
                <div class="brand-area">
                   <div class="logo-circle">
                      <!-- Cozy Cup Logo -->
                      <svg class="start-logo" viewBox="0 0 24 24" fill="currentColor">
                         <path d="M18.5,8H19C20.66,8 22,9.34 22,11V13C22,14.66 20.66,16 19,16H18.28C17.76,18.29 15.63,20 13,20H7C4.24,20 2,17.76 2,15V8H18.5ZM19,10H18V14H19C19.55,14 20,13.55 20,13V11C20,10.45 19.55,10 19,10ZM7,3H9V6H7V3ZM11,3H13V6H11V3ZM15,3H17V6H15V3Z"/>
                      </svg>
                   </div>
                   <span class="brand-text">CozyCoffee</span>
                </div>

                <!-- Middle Left: Points -->
                <div class="points-area">
                   <span class="caption">CURRENT POINTS</span>
                   <span class="points-val">{{ userStore.userInfo?.currentPoints || 0 }}</span>
                </div>

                <!-- Right Side: Tier Badge 3D -->
                <div class="tier-emblem-area">
                    <div class="emblem-3d-wrapper">
                        <!-- Crown (Black) -->
                        <div class="emblem-shape crown" v-if="userStore.userLevel === 'black'">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                               <path d="M5 16L3 5L8.5 10L12 4L15.5 10L21 5L19 16H5M19 19C19 19.55 18.55 20 18 20H6C5.45 20 5 19.55 5 19V18H19V19Z" />
                            </svg>
                        </div>
                        <!-- Diamond (Diamond) -->
                        <div class="emblem-shape diamond" v-else-if="userStore.userLevel === 'diamond'">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                               <path d="M19,12l-7,10l-7,-10l3.5,-8h7l3.5,8z M12,3.5L8.5,8h7L12,3.5z"/>
                            </svg>
                        </div>
                        <!-- Medal (Gold/Silver) -->
                        <div class="emblem-shape medal" v-else-if="['gold','silver'].includes(userStore.userLevel)">
                            <svg viewBox="0 0 24 24" fill="currentColor">
                               <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                            </svg>
                        </div>
                        <!-- Coffee Bean (Base) -->
                        <div class="emblem-shape bean" v-else>
                            <svg viewBox="0 0 24 24" fill="currentColor">
                               <path d="M12,2 C17.5,2 22,6.5 22,12 C22,17.5 17.5,22 12,22 C6.5,22 2,17.5 2,12 C2,6.5 6.5,2 12,2 Z" />
                               <path d="M12,5 C14,5 16,8 16,11 C16,15 13,18 12,18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                            </svg>
                        </div>
                        
                        <div class="shine-overlay"></div>
                    </div>
                    <div class="tier-text">{{ levelName }}</div>
                </div>

                <!-- Bottom Left: ID -->
                <div class="footer-area">
                   <span class="member-code">ID: {{ userStore.userInfo?.memberCode }}</span>
                   <span class="expiry" v-if="userStore.userInfo?.levelExpireDate">EXP: {{ userStore.userInfo.levelExpireDate }}</span>
                </div>
             </div>
          </div>

          <!-- 2. Stats Dashboard: Icons & Divider -->
          <div class="stats-premium-bar">
            <div class="stat-item">
               <div class="stat-icon-bg"><TrendingUp :size="25" /></div>
               <div class="stat-text">
                  <span class="val">{{ userStore.userInfo?.expTotal || 0 }}</span>
                  <span class="lbl">成长值</span>
               </div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
               <div class="stat-icon-bg"><CalendarCheck :size="25" /></div>
               <div class="stat-text">
                  <span class="val">{{ userStore.userInfo?.signInDays || 0 }} <small>天</small></span>
                  <span class="lbl">连续签到</span>
               </div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
               <div class="stat-icon-bg"><Target :size="25" /></div>
               <div class="stat-text">
                  <span class="val highlight">{{ Math.max(0, nextLevelPoints - (userStore.userInfo?.expTotal || 0)) }}</span>
                  <span class="lbl">距下一级</span>
               </div>
            </div>
          </div>

          <!-- 3. Sign In: Premium Bean Tracker -->
          <div class="signin-premium-widget">
             <div class="widget-header">
                <div>
                  <h4>每日签到</h4>
                  <span class="sub">连续签到 7 天可领惊喜礼包</span>
                </div>
                <button class="signin-action-btn" @click="handleSignIn" :disabled="isSignedToday">
                   {{ isSignedToday ? '今日已签' : '签到领豆' }}
                </button>
             </div>
             
             <div class="bean-track-wrapper">
                <div class="track-line-base">
                   <div class="track-line-fill" :style="{ width: Math.max(0, (currentSignInCycleDay - 1) / 6 * 100) + '%' }"></div>
                </div>
                
                <div class="bean-steps">
                   <div class="bean-step" v-for="day in 7" :key="day"
                        :class="{ 'is-active': day <= currentSignInCycleDay, 'is-today': day === currentSignInCycleDay }">
                        <div class="bean-icon-box" :class="{ 'is-gift': day === 7 }">
                           <template v-if="day === 7">
                              <div class="gift-box-3d">
                                 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="gift-svg">
                                    <polyline points="20 12 20 22 4 22 4 12"></polyline>
                                    <rect x="2" y="7" width="20" height="5"></rect>
                                    <line x1="12" y1="22" x2="12" y2="7"></line>
                                    <path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"></path>
                                    <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"></path>
                                 </svg>
                              </div>
                              <div class="gift-glow" v-if="day <= currentSignInCycleDay"></div>
                           </template>
                           <template v-else>
                              <!-- Bean SVG -->
                              <svg class="bean-svg" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M12,2 C6.5,2 2,6.5 2,12 C2,17.5 6.5,22 12,22 C17.5,22 22,17.5 22,12 C2,6.5 17.5,2 12,2 Z M12.5,5 C12.5,5 14,8 14,12 C14,16 12.5,19 12.5,19 C11,19 8,16 8,12 C8,8 11,5 12.5,5 Z" />
                              </svg>
                           </template>
                        </div>
                        <span class="step-label">{{ day === 7 ? '礼包' : `+2` }}</span>
                   </div>
                </div>
             </div>
             
             <div class="widget-footer-info" v-if="currentSignInCycleDay < 7">
                 <span class="info-icon">✨</span>
                 <span>再签 <strong>{{ 7 - currentSignInCycleDay }}</strong> 天领满35-10券</span>
             </div>
             <div class="widget-footer-info success" v-else>
                 <span class="info-icon">🎁</span>
                 <span>连续签到7天！礼包已到账</span>
             </div>
          </div>

          <!-- Web Split Layout -->
          <div class="dashboard-split-layout">
            
            <!-- Left Column: Points Guide -->
            <div class="layout-col-left">
              <div class="points-guide-section">
                <div class="section-title">
                  <h4>积分获取</h4>
                  <div class="header-right-action">
                    <span class="subtitle">完成任务积攒成长值</span>
                    <button @click="openPointsDetailModal" class="link-btn-small">明细 ></button>
                  </div>
                </div>

                <div class="points-channels" style="gap: 20px;">
                  <!-- 签到积分 -->
                  <div class="channel-card" :class="{ done: isSignedToday }">
                    <div class="channel-icon-bg"><i class="icon-calendar"></i></div>
                    <div class="channel-info">
                      <span class="channel-name">每日签到</span>
                      <span class="channel-desc">连续签到额外奖励</span>
                    </div>
                    <div class="channel-points simple-row">
                      <span>+{{ getSigninPointsByLevel() }} 积分/天</span>
                    </div>
                    <span v-if="isSignedToday" class="status-check"><Check :size="20" /></span>
                  </div>

                  <!-- 完善资料 -->
                  <div class="channel-card" :class="{ done: profileComplete }">
                    <div class="channel-icon-bg"><i class="icon-edit"></i></div>
                    <div class="channel-info">
                      <span class="channel-name">完善资料</span>
                      <span class="channel-desc">填写手机号和邮箱</span>
                    </div>
                    <div class="channel-points simple-row">
                      <span>+20 积分</span>
                    </div>
                    <span v-if="profileComplete" class="status-check"><Check :size="20" /></span>
                    <button v-else @click="currentTab = 'personal-info'" class="go-btn">去完成</button>
                  </div>

                  <!-- 消费获积分 -->
                  <div class="channel-card">
                    <div class="channel-icon-bg"><i class="icon-coffee"></i></div>
                    <div class="channel-info">
                      <span class="channel-name">消费赚积分</span>

                    </div>
                    <div class="channel-points simple-row">
                      <span>1元={{ getConsumeMultiplier() }} 积分</span>
                    </div>
                    <button @click="currentTab = 'coffee-order'" class="go-btn consume">去下单</button>
                  </div>

                </div>

                <!-- 等级权益提示 -->
                <div class="level-tip" v-if="userStore.userLevel !== 'black'">
                   <div class="tip-content">
                      <span class="tip-icon"><Rocket :size="18" /></span>
                      <span class="tip-text">
                        升级到 <strong>{{ nextLevelName }}</strong> 还需 <strong>{{ Math.max(0, nextLevelPoints - (userStore.userInfo?.expTotal || 0)) }}</strong> EXP
                      </span>
                   </div>
                   <button @click="currentTab = 'member-benefits'" class="view-benefits-text-btn">查看权益</button>
                </div>
                <!-- 查看积分明细按钮 已移至标题栏 -->
                
                <div class="promo-banner">
                  <img src="/images/banner-promo.png" alt="Promo" />
                </div>
              </div>
            </div>

            <!-- Right Column: Monthly Challenge -->
            <div class="layout-col-right">
              <div class="task-center-section">
                <div class="section-title">
                  <h4>本月挑战</h4>
                  <span class="subtitle">Monthly Challenge</span>
                  <button 
                    class="refresh-btn" 
                    @click="handleRefreshMonthlyTask" 
                    :disabled="isRefreshingTask"
                    title="刷新任务进度"
                  >
                    <RefreshCw :size="16" :class="{ 'spinning': isRefreshingTask }" />
                  </button>
                </div>

                <!-- 黑卡专属加速包 -->
                <div class="black-accelerate-box" v-if="userStore.userLevel === 'black'">
                  <div class="box-header">
                    <div class="box-title">
                      <span class="premium-icon">✨</span>
                      <span class="title-text">黑卡加速包</span>
                    </div>
                    <div class="box-status">
                      剩 <strong>¥{{ userStore.userInfo?.monthlyAccelerateRemaining ?? 0 }}</strong>
                    </div>
                  </div>
                  <div class="accelerate-progress">
                    <div class="progress-bar-bg">
                      <div class="progress-fill" :style="{ width: accelerateProgressPercent + '%' }"></div>
                    </div>
                    <div class="progress-info">
                      <span>已加速 ¥{{ (300 - (userStore.userInfo?.monthlyAccelerateRemaining ?? 0)).toFixed(2) }}</span>
                    </div>
                  </div>
                </div>

                <div class="task-list-premium" :key="taskRefreshKey">
                  <!-- 打卡达人 -->
                  <div class="task-item-row" :class="{ 'task-completed': isOrderTaskCompleted }">
                    <div class="task-icon-bg"><CalendarCheck :size="20" /></div>
                    <div class="task-main">
                       <div class="task-top">
                          <span class="name">打卡达人</span>
                          <span class="reward" v-if="!isOrderTaskCompleted">+40 积分</span>
                          <span class="reward claimed" v-else>✓ 已领取</span>
                       </div>
                       <div class="task-desc">本月完成订单 4 次</div>
                       <div class="task-progress-bar">
                          <div class="fill orange" :style="{ width: Math.min(100, ((monthlyTaskData.monthlyOrderCount ?? userStore.userInfo?.monthlyOrderCount ?? 0) / 4) * 100) + '%' }"></div>
                       </div>
                    </div>
                    <div class="task-action">
                       <span class="status-text" :class="{ 'completed': isOrderTaskCompleted }">{{ monthlyTaskData.monthlyOrderCount ?? userStore.userInfo?.monthlyOrderCount ?? 0 }}/4</span>
                    </div>
                  </div>

                  <!-- 晨间唤醒 -->
                  <div class="task-item-row" :class="{ 'task-completed': isMorningTaskCompleted }">
                    <div class="task-icon-bg"><Sun :size="20" /></div>
                    <div class="task-main">
                       <div class="task-top">
                          <span class="name">晨间唤醒</span>
                          <span class="reward" v-if="!isMorningTaskCompleted">+60 积分</span>
                          <span class="reward claimed" v-else>✓ 已领取</span>
                       </div>
                       <div class="task-desc">10:00完成订单 3 次</div>
                       <div class="task-progress-bar">
                          <div class="fill yellow" :style="{ width: Math.min(100, ((monthlyTaskData.morningOrderCount ?? userStore.userInfo?.morningOrderCount ?? 0) / 3) * 100) + '%' }"></div>
                       </div>
                    </div>
                    <div class="task-action">
                       <span class="status-text" :class="{ 'completed': isMorningTaskCompleted }">{{ monthlyTaskData.morningOrderCount ?? userStore.userInfo?.morningOrderCount ?? 0 }}/3</span>
                    </div>
                  </div>

                  <!-- 外卖尝鲜 -->
                  <div class="task-item-row" :class="{ 'task-completed': isDeliveryTaskCompleted }">
                    <div class="task-icon-bg"><Truck :size="20" /></div>
                    <div class="task-main">
                       <div class="task-top">
                          <span class="name">外卖尝鲜</span>
                          <span class="reward" v-if="!isDeliveryTaskCompleted">+50 积分</span>
                          <span class="reward claimed" v-else>✓ 已领取</span>
                       </div>
                       <div class="task-desc">完成 2 笔外卖订单</div>
                       <div class="task-progress-bar">
                          <div class="fill green" :style="{ width: Math.min(100, ((monthlyTaskData.currentDeliveryOrders ?? userStore.userInfo?.monthlyDeliveryOrders ?? 0) / 2) * 100) + '%' }"></div>
                       </div>
                    </div>
                    <div class="task-action">
                       <span class="status-text" :class="{ 'completed': isDeliveryTaskCompleted }">{{ monthlyTaskData.currentDeliveryOrders ?? userStore.userInfo?.monthlyDeliveryOrders ?? 0 }}/2</span>
                    </div>
                  </div>

                  <!-- 新品猎人 -->
                  <div class="task-item-row" :class="{ 'task-completed': isNewProductTaskCompleted }">
                    <div class="task-icon-bg"><ShoppingBag :size="20" /></div>
                    <div class="task-main">
                       <div class="task-top">
                          <span class="name">新品猎人</span>
                          <span class="reward" v-if="!isNewProductTaskCompleted">+80 积分</span>
                          <span class="reward claimed" v-else>✓ 已领取</span>
                       </div>
                       <div class="task-desc">尝试 3 款限定新品</div>
                       <div class="task-progress-bar">
                          <div class="fill purple" :style="{ width: Math.min(100, ((monthlyTaskData.newProductCount ?? userStore.userInfo?.newProductCount ?? 0) / 3) * 100) + '%' }"></div>
                       </div>
                    </div>
                    <div class="task-action">
                       <span class="status-text" :class="{ 'completed': isNewProductTaskCompleted }">{{ monthlyTaskData.newProductCount ?? userStore.userInfo?.newProductCount ?? 0 }}/3</span>
                    </div>
                  </div>

                </div>
              </div>
            </div>
            
          </div>
        </div>

        <div v-else-if="currentTab === 'my-coupons'" class="coupons-view" key="coupons">
          <header class="content-header">
            <h3>我的券包</h3>
            <button 
              class="refresh-btn" 
              @click="handleRefreshCoupons" 
              :disabled="isRefreshingCoupons"
              title="刷新优惠券"
            >
              <RefreshCw :size="18" :class="{ 'spinning': isRefreshingCoupons }" />
            </button>
          </header>
          
          <div class="coupons-container">
            <CouponTabs :key="couponTabsKey" @use-coupon="handleUseCouponFromTabs" />
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
              <label>生日</label>
              <div class="input-wrapper birthday-picker-wrapper">
                <el-date-picker
                  v-if="isEditingBirthday"
                  v-model="editBirthday"
                  type="date"
                  placeholder="选择您的生日"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  :clearable="false"
                  class="premium-date-picker"
                />
                <span v-else class="static-value">
                  <i class="icon-cake" v-if="userStore.userInfo?.birthday"></i>
                  {{ userStore.userInfo?.birthday || '未设置' }}
                </span>
                <button v-if="!isEditingBirthday" @click="startEditBirthday" class="edit-btn">
                  {{ userStore.userInfo?.birthday ? '修改' : '设置' }}
                </button>
                <div v-else class="actions">
                  <button @click="saveField('birthday', editBirthday)" class="save-btn">保存</button>
                  <button @click="cancelEditBirthday" class="cancel-btn">取消</button>
                </div>
              </div>
              <p class="field-hint" v-if="userStore.userInfo?.birthday">
                注意：生日每年仅限修改一次（下次可改：{{ formatExpireDate(userStore.userInfo?.nextBirthdayResetAt) }}）
              </p>
              <p class="field-hint" v-else>生日月可获得会员等级专属生日权益包</p>
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
                <span class="invite-tip">邀请新用户注册首单后即可获得买一送一券</span>
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
              <p class="field-hint">填写好友邀请码绑定关系，下单享优惠</p>
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

          <!-- Tabbed Member Privileges UI -->
          <div class="member-privileges-section">
            <div class="level-tabs">
              <button 
                v-for="level in ['basic', 'silver', 'gold', 'diamond', 'black']" 
                :key="level"
                @click="activeLevelTab = level"
                class="level-tab-btn"
                :class="{ active: activeLevelTab === level, [level]: true }"
              >
                {{ getLevelName(level) }}
              </button>
            </div>

            <!-- Feature Card Content -->
            <div class="level-feature-view">
              
              <!-- Black Gold Special Card -->
              <div v-if="activeLevelTab === 'black'" class="feature-card black-gold-theme">
                <div class="card-visual-side">
                  <div class="physical-card-3d">
                    <div class="card-face">
                      <div class="card-brand-mark"><Coffee :size="32" /></div>
                      <div class="card-logo">COZY BLACK</div>
                      <div class="card-number">8888 8888 8888 8888</div>
                      <div class="card-member-name">MEMBER</div>
                    </div>
                  </div>
                  <div class="ambient-glow"></div>
                </div>
                <div class="card-content-side">
                  <h3 class="feature-header">The Ultimate Experience <br><span>尊享极致体验</span></h3>
                  <div class="feature-list">
                     <div class="f-item"><span class="f-icon">⚡</span> <div><strong>消费 1元 = 1.5 积分</strong><p>黑卡加速包前300元享 1.7x</p></div></div>
                     <div class="f-item"><span class="f-icon">☕</span> <div><strong>每月：全通兑免单券×2 + 买一赠一券×5</strong><p>Monthly Premium Coupons</p></div></div>
                     <div class="f-item"><span class="f-icon">🚚</span> <div><strong>无限次免配送费</strong><p>Unlimited Free Delivery</p></div></div>
                     <div class="f-item"><span class="f-icon">🌟</span> <div><strong>新品免费试饮券</strong><p>New Product Trial</p></div></div>
                     <div class="f-item"><span class="f-icon">🎂</span> <div><strong>生日：全通兑免单券+免费蛋糕券+888积分</strong><p>Birthday Ultimate Pack</p></div></div>
                     <div class="f-item"><span class="f-icon">💎</span> <div><strong>积分兑换 8.5 折</strong><p>Premium Redemption Discount</p></div></div>
                  </div>
                  
                  <!-- v5.3: Manual Receive Button -->
                  <div class="benefit-action-area" v-if="userStore.userLevel === 'black'">
                      <button 
                        class="receive-btn" 
                        :disabled="!monthlyBenefitStatus.canClaim || monthlyBenefitStatus.claimed || isReceivingBenefit"
                        @click="handleReceiveBenefit"
                      >
                         <span v-if="isReceivingBenefit">领取中...</span>
                         <span v-else-if="monthlyBenefitStatus.claimed">本月权益已领取</span>
                         <span v-else-if="monthlyBenefitStatus.canClaim">领取黑金专属权益包</span>
                         <span v-else>本月暂无可领权益</span>
                      </button>
                      <p class="upgrade-tip" v-if="shouldShowUpgradeTip" style="font-size: 12px; color: #fbbf24; margin-top: 8px;">
                         恭喜升级！您的黑金月度权益将在下月 1 日生效。
                      </p>
                  </div>
                  <button v-else class="cta-btn secondary">查看升级路径</button>
                </div>
              </div>

               <!-- Other Levels (Premium 3D Cards) -->
               <div v-else class="feature-card generic-theme" :class="activeLevelTab">
                  <div class="card-visual-side">
                     <!-- Reusing 3D structure for all cards -->
                     <div class="physical-card-3d" :class="activeLevelTab">
                        <div class="card-face">
                           <div class="card-brand-mark" :class="activeLevelTab"><Coffee :size="32" /></div>
                           <div class="card-logo">COZY {{ activeLevelTab.toUpperCase() }}</div>
                           <div class="card-number">8888 8888 8888 8888</div>
                           <div class="card-member-name">MEMBER</div>
                        </div>
                     </div>
                  </div>
                  <div class="card-content-side">
                     <h3 class="feature-header">{{ getLevelName(activeLevelTab) }} <br><span>等级权益</span></h3>
                     <ul class="generic-benefit-list">
                        <li v-for="(benefit, idx) in getLevelBenefits(activeLevelTab)" :key="idx">
                           <component :is="benefit.icon" :size="18" class="b-icon" /> {{ benefit.text }}
                        </li>
                     </ul>

                     <!-- v5.5: Manual Receive Button for Diamond/Others -->
                     <!-- v5.5: Manual Receive Button for Diamond/Others -->
                     <div class="benefit-action-area" v-if="userStore.userLevel === activeLevelTab">
                        <button 
                            class="receive-btn" 
                            :class="activeLevelTab"
                            :disabled="!monthlyBenefitStatus.canClaim || monthlyBenefitStatus.claimed || isReceivingBenefit"
                            @click="handleReceiveBenefit"
                        >
                            <span v-if="isReceivingBenefit">领取中...</span>
                            <span v-else-if="monthlyBenefitStatus.claimed">本月权益已领取</span>
                            <span v-else-if="monthlyBenefitStatus.canClaim">领取月度权益礼包</span>
                            <span v-else>本月暂无可领权益</span>
                        </button>
                        <p class="upgrade-tip" v-if="shouldShowUpgradeTip" style="font-size: 12px; color: #e6a23c; margin-top: 8px;">
                           恭喜升级！您的{{ getLevelName(userStore.userLevel) }}月度权益将在下月 1 日生效。
                        </p>
                     </div>
                  </div>
               </div>

            </div>
          </div>

          <!-- Member Progress Footer (Static) -->
          <div class="member-progress-footer">
             <div class="progress-info">
                <div class="current-status">
                   <span class="status-icon">
                      <span v-if="userStore.userLevel === 'basic'">☕</span>
                      <span v-else-if="userStore.userLevel === 'silver'">🥈</span>
                      <span v-else-if="userStore.userLevel === 'gold'">🏆</span>
                      <span v-else-if="userStore.userLevel === 'diamond'">💎</span>
                      <span v-else-if="userStore.userLevel === 'black'">👑</span>
                   </span>
                   <span class="status-text">{{ levelName }} {{ (userStore.userLevel || 'basic').toUpperCase() }}</span>
                </div>
                <div class="progress-numbers">
                   <span class="current-exp">{{ levelProgress.current }}</span>
                   <span class="total-exp">/ {{ levelProgress.target }} EXP</span>
                   <component :is="levelProgress.nextLevelIcon" :size="24" :fill="levelProgress.nextLevelColor || 'currentColor'" fill-opacity="0.2" class="m-icon" 
                      :style="{ color: levelProgress.nextLevelColor || '#999', marginLeft: '8px' }" />
                </div>
             </div>
             
             <div class="progress-bar-container">
                <div class="progress-track">
                   <div class="progress-fill" :class="userStore.userLevel || 'basic'" :style="{ width: levelProgress.percentage + '%' }"></div>
                </div>
             </div>

             <div class="progress-motivation">
                <span v-if="!levelProgress.isMax">再积 <strong>{{ levelProgress.remaining }}</strong> EXP {{ levelProgress.benefitText }}</span>
                <span v-else>🎉 {{ levelProgress.benefitText }}</span>
             </div>
          </div>
        </div>


        <!-- Coffee Orders History View -->
        <div v-else-if="currentTab === 'coffee-orders-history'" class="orders-view" key="coffee-orders">
          <header class="content-header">
            <h3>咖啡订单记录</h3>
            <button 
              class="refresh-btn" 
              @click="handleRefreshOrders" 
              :disabled="isRefreshingOrders"
              title="刷新订单"
            >
              <RefreshCw :size="18" :class="{ 'spinning': isRefreshingOrders }" />
            </button>
          </header>

          <div class="orders-list" v-if="coffeeOrders.length > 0">
            <div class="order-card" v-for="order in coffeeOrders" :key="order.id">
              <div class="order-header">
                <span class="order-no">订单号: {{ order.orderNo }}</span>
                <div class="header-right">
                   <span class="dining-badge" v-if="order.diningMethod">{{ getDiningMethodText(order.diningMethod) }}</span>
                   <span class="order-status" :class="order.status">{{ getStatusText(order.status) }}</span>
                </div>
              </div>
              <div class="order-body">
                <div class="order-info">
                  <!-- New Detailed Items List -->
                  <div class="order-items-detail">
                     <div class="order-line-item" v-for="(item, idx) in order.items" :key="idx">
                        <img :src="getImageUrl(item.productImage)" class="line-item-img" />
                        <div class="line-item-info">
                           <span class="line-item-name">{{ item.productName }} <span class="qty">x{{ item.quantity }}</span></span>
                           <span class="line-item-specs">{{ formatSpecs(item) }}</span>
                        </div>
                        <span class="line-item-price">¥{{ item.itemAmount }}</span>
                     </div>
                  </div>

                  <div class="order-meta-row">
                     <p class="order-amount">实付: ¥{{ order.payAmount }}</p>
                     <p class="points-earned" v-if="order.pointsEarned">获得积分: +{{ order.pointsEarned }}</p>
                  </div>
                  <div class="order-meta-secondary">
                      <div class="meta-left">
                         <p class="order-time">{{ formatDate(order.createdAt) }}</p>
                         <p class="pickup-code" v-if="order.pickupCode">取餐码: <strong>{{ order.pickupCode }}</strong></p>
                      </div>
                      <div class="meta-right" v-if="order.status === 'pending'">
                         <button class="cancel-btn-small" @click.stop="cancelCoffeeOrder(order.id)">取消订单</button>
                      </div>
                  </div>
                </div>

              </div>
            </div>
          </div>
          <div v-else class="no-data">
            <p>暂无咖啡订单记录</p>
            <button @click="currentTab = 'coffee-order'" class="go-order-btn">去点单</button>
          </div>
        </div>

        <!-- Redeem Orders History View -->
        <div v-else-if="currentTab === 'redeem-orders-history'" class="orders-view" key="redeem-orders">
          <header class="content-header">
            <h3>兑换订单记录</h3>
          </header>

          <div class="orders-list" v-if="redeemOrders.length > 0">
            <div class="order-card" v-for="order in redeemOrders" :key="order.id">
              <div class="order-header">
                <span class="order-no">订单号: {{ order.orderNo }}</span>
                <span class="order-status" :class="order.status">{{ getStatusText(order.status) }}</span>
              </div>
              <div class="order-body">
                <img :src="getImageUrl(order.productImage)" class="order-img" />
                <div class="order-info">
                  <p class="product-name">{{ order.productName }}</p>
                  <p class="order-quantity">数量: {{ order.quantity || 1 }}</p>
                  <p class="points-cost">消耗积分: {{ order.pointsCost }}</p>
                  <p class="order-time">{{ formatDate(order.createdAt) }}</p>
                </div>
              </div>
              <div class="order-footer" v-if="order.fulfillmentType !== 'VIRTUAL'">
                <p class="receiver-info">收货人: {{ order.receiverName }} {{ order.receiverPhone }}</p>
                <p class="receiver-address">{{ order.receiverAddress }}</p>
              </div>
              <div class="order-footer virtual" v-else>
                <p class="virtual-status">已自动发放至您的券包</p>
              </div>
            </div>
          </div>
          <div v-else class="no-data">
            <p>暂无兑换订单记录</p>
            <button @click="currentTab = 'points-mall'" class="go-mall-btn">去积分商城看看</button>
          </div>
        </div>

        <!-- Coffee Order View -->
        <div v-else-if="currentTab === 'coffee-order'" class="coffee-order-view-wrapper" key="coffee-order">
          <CoffeeOrderView 
            :user-info="userStore.userInfo" 
            :points-multiplier="getConsumeMultiplier()" 
            @order-created="handleCoffeeOrderCreated"
            @refresh-user="refreshUserInfo" 
          />
        </div>

        <!-- Points Mall View -->
        <div v-else-if="currentTab === 'points-mall'" class="mall-view" key="mall">
          <header class="content-header">
            <h3>积分商城</h3>
            <span class="points-badge">{{ userStore.userInfo?.currentPoints || 0 }} 积分</span>
          </header>

          <!-- 分类标签栏 -->
          <div class="mall-category-tabs">
            <button 
              v-for="cat in mallCategories" 
              :key="cat.value" 
              class="mall-category-tab"
              :class="{ active: activeMallCategory === cat.value }"
              @click="activeMallCategory = cat.value"
            >
              <component :is="cat.icon" :size="18" :stroke-width="1.8" class="tab-icon-svg" />
              <span>{{ cat.label }}</span>
            </button>
          </div>

          <!-- 商品列表 -->
          <div class="products-grid" v-if="!showOrderHistory">
            <div class="mall-card" v-for="product in filteredMallProducts" :key="product.id">
              <div class="card-image"
                :style="{ backgroundImage: `url(${getImageUrl(product.imageUrl)})` }"></div>
              <div class="card-details">
                <h4>{{ product.name }}</h4>
                <p class="product-desc">{{ product.description }}</p>
                <span class="price">{{ product.pointsPrice }} 积分</span>
                <span class="stock" :class="{ low: product.stock < 10 }">库存: {{ product.stock }}</span>
                <!-- 月度限购提示 -->
                <div v-if="product.monthlyLimit && product.monthlyLimit > 0" class="limit-info">
                  <span class="limit-tag">月限 {{ product.monthlyLimit }}</span>
                  <span class="redeemed-text">已兑: {{ product.currentUserMonthlyRedeemed || 0 }}</span>
                </div>
                <button class="redeem-btn" @click="openRedeemDialog(product)"
                  :disabled="product.stock === 0 || (userStore.userInfo?.currentPoints || 0) < product.pointsPrice || (product.monthlyLimit && (product.currentUserMonthlyRedeemed || 0) >= product.monthlyLimit)">
                  {{ product.stock === 0 ? '已售罄' : ((product.monthlyLimit && (product.currentUserMonthlyRedeemed || 0) >= product.monthlyLimit) ? '本月限额已满' : '立即兑换') }}
                </button>
              </div>
            </div>
            <div v-if="filteredMallProducts.length === 0" class="no-data">暂无商品</div>
          </div>

        </div>
      </transition>

      <!-- 兑换确认弹窗 - 移到 transition 外部 -->
      <div class="redeem-modal" v-if="showRedeemModal" @click.self="showRedeemModal = false">
        <div class="modal-content">
          <h3>确认兑换</h3>
          <div class="product-preview">
            <img :src="getImageUrl(selectedProduct?.imageUrl)" />
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

          <!-- 交付方式 -->
          <div class="fulfillment-section" v-if="redeemFulfillmentType !== 'VIRTUAL' && selectedProduct?.productType !== 'VIRTUAL' && !selectedProduct?.name?.includes('券')">
            <label>交付方式：</label>
            <div class="radio-group-modern">
               <label class="radio-card" :class="{ active: redeemFulfillmentType === 'PICKUP' }">
                 <input type="radio" v-model="redeemFulfillmentType" value="PICKUP" class="hidden-radio">
                 <span class="icon">🏠</span>
                 <span class="text">门店自提</span>
               </label>
               <label class="radio-card" :class="{ active: redeemFulfillmentType === 'DELIVERY' }">
                 <input type="radio" v-model="redeemFulfillmentType" value="DELIVERY" class="hidden-radio">
                 <span class="icon">🚚</span>
                 <span class="text">快递配送</span>
               </label>
            </div>
          </div>

          <!-- 地址选择 -->
          <div class="address-section" v-if="redeemFulfillmentType === 'DELIVERY'">
             <div class="section-title-row">
               <label>收货地址：</label>
               <button v-if="!addresses || addresses.length === 0" @click="showAddAddressModal = true" class="text-btn">去添加</button>
             </div>
             
             <select v-model="redeemAddressId" class="modern-select">
                <option value="" disabled>请选择收货地址</option>
                <option v-for="addr in addresses" :key="addr.id" :value="addr.id">
                  {{ addr.receiverName }} {{ addr.receiverPhone }} ({{ addr.province }}{{ addr.city }}{{ addr.district || '' }} {{ addr.detailAddress }})
                </option>
             </select>
          </div>
          
          <div class="pickup-hint" v-if="redeemFulfillmentType === 'PICKUP' && selectedProduct?.productType !== 'VIRTUAL'">
             <p>📍 取货门店: <strong>Cozy Coffee 旗舰店</strong></p>
             <p class="sub-text">下单后请凭取货码到店领取</p>
          </div>

          <div class="redeem-summary-card">
            <div class="row">
               <span>原价</span>
               <span class="original">{{ (selectedProduct?.pointsPrice || 0) * redeemQuantity }} 积分</span>
            </div>
            <div class="row" v-if="getRedeemDiscount() < 1">
               <span>会员折扣 ({{ Math.round((1 - getRedeemDiscount()) * 100) }}% OFF)</span>
               <span class="discount">-{{ Math.round(((selectedProduct?.pointsPrice || 0) * redeemQuantity) * (1 - getRedeemDiscount())) }} 积分</span>
            </div>
            <div class="divider"></div>
            <div class="row total">
               <span>实付</span>
               <strong>{{ getDiscountedCost() }} 积分</strong>
            </div>
            <p class="balance-refer">当前余额: {{ userStore.userInfo?.currentPoints || 0 }} 积分</p>
          </div>
          
          <div class="redeem-warning-box">
             ⚠️ 温馨提示：积分商品兑换后不支持取消或退换。
          </div>

          <div class="modal-actions">
            <button @click="showRedeemModal = false" class="cancel-btn">取消</button>
            <button @click="handleRedeemProduct" class="confirm-btn" 
              :disabled="isRedeeming || (redeemFulfillmentType === 'DELIVERY' && !redeemAddressId)">
              {{ isRedeeming ? '处理中...' : '确认兑换' }}
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

      <!-- 积分明细弹窗 -->
      <div class="points-detail-modal" v-if="showPointsDetailModal" @click.self="showPointsDetailModal = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3><List :size="20" style="margin-right: 8px; vertical-align: text-bottom;" />积分明细</h3>
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
import { ref, computed, onMounted, watch, onUnmounted, markRaw } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import chinaRegions from '@/data/china-regions.json'
import { cancelOrder } from '@/api/order'
import CreateOrderPanel from '@/components/order/CreateOrderPanel.vue'
import CoffeeOrderView from '@/components/CoffeeOrderView.vue'
import CouponTabs from '@/components/coupon/CouponTabs.vue'
import { Tag, Ticket, Gift, TrendingUp, CalendarCheck, Target, Check, Sun, Truck, ShoppingBag, Rocket, List, 
  LayoutDashboard, Coffee, ClipboardList, User, Crown, ChevronRight, Home, LogOut, Coins, Zap, Gem, Medal, Trophy, RefreshCw } from 'lucide-vue-next'

const userStore = useUserStore()
const router = useRouter()
const currentTab = ref('member-center')
const isOrdersExpanded = ref(false)  // 历史订单菜单展开状态
const activeLevelTab = ref(userStore.userLevel || 'basic') // Default to User Level

// Sync active tab with user level when loaded
watch(() => userStore.userLevel, (newVal) => {
  if (newVal) {
     activeLevelTab.value = newVal
  }
}, { immediate: true })

const getLevelName = (lvl) => {
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[lvl] || '会员'
}

const getLevelBenefits = (lvl) => {
   // v5.3 白皮书权益矩阵
   if (lvl === 'basic') return [
      { icon: markRaw(Coins), text: '消费 1元 = 1.0 积分' },
      { icon: markRaw(CalendarCheck), text: '每日签到 +2 积分，连续 7 天送优惠券' },
      { icon: markRaw(Coffee), text: '每月可领：免费加浓缩券 ×1' },
      { icon: markRaw(Gift), text: '生日：单饮品 5 折券' },
      { icon: markRaw(Zap), text: '周三会员日 1.5x 积分' }
   ]
   if (lvl === 'silver') return [
      { icon: markRaw(Coins), text: '消费 1元 = 1.1 积分' },
      { icon: markRaw(Ticket), text: '积分兑换 9.8 折' },
      { icon: markRaw(Truck), text: '每月可领：配送费抵扣券×1 + 加浓缩券×2' },
      { icon: markRaw(Gift), text: '生日：买一赠一券 ×1' },
      { icon: markRaw(Zap), text: '周三会员日 1.6x 积分' }
   ]
   if (lvl === 'gold') return [
      { icon: markRaw(Coins), text: '消费 1元 = 1.2 积分' },
      { icon: markRaw(Ticket), text: '积分兑换 9.5 折' },
      { icon: markRaw(Gift), text: '每月可领：买一赠一券×1 + 8.8折券×2 + 配送费抵扣×2' },
      { icon: markRaw(Gift), text: '生日：标准饮品免单券' },
      { icon: markRaw(Zap), text: '周三会员日 1.7x 积分' }
   ]
   if (lvl === 'diamond') return [
      { icon: markRaw(Coins), text: '消费 1元 = 1.3 积分' },
      { icon: markRaw(Ticket), text: '积分兑换 9.0 折' },
      { icon: markRaw(Gift), text: '每月可领：优选饮品免单券×1 + 买一赠一券×2 + 配送费抵扣券×5 + 新品5折券' },
      { icon: markRaw(Gift), text: '生日：优选饮品免单券 + 烘培甜品 5 折券' },
      { icon: markRaw(Zap), text: '周三会员日 1.8x 积分' }
   ]
   return []
}


// Coupon States
const currentCouponTab = ref('unused')
const coupons = ref([])

const filteredCoupons = computed(() => {
  return coupons.value
})

const getDaysLeft = (dateStr) => {
  const diff = new Date(dateStr) - new Date()
  const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
  return days
}

// 加载用户优惠券 (调用真实的 PointsMallController 接口)
const loadUserCoupons = async () => {
  try {
    const token = localStorage.getItem('token')
    // 状态映射: 前端 unused/used/expired -> 后端 ISSUED/USED/EXPIRED
    const statusMap = { unused: 'ISSUED', used: 'USED', expired: 'EXPIRED' }
    const backendStatus = statusMap[currentCouponTab.value] || ''
    
    const response = await fetch(`http://localhost:8080/api/member/mall/coupons?status=${backendStatus}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      // 后端返回 UserCouponDTO 列表，需要映射字段
      coupons.value = (data.data || []).map(c => {
        const productName = c.productName || parseProductNameFromRule(c.ruleJson)
        
        // 解析 ruleJson
        let rule = {}
        try { rule = JSON.parse(c.ruleJson || '{}') } catch(e) {}
        
        // v5.3: 优先使用后端计算好的 displayTitle 和 displaySubTitle
        // 这样可以避免前端重复解析导致的显示错误（如 50% 显示为 "50折"）
        let displayVal = ''
        let title = ''
        let desc = ''
        
        if (c.displayTitle) {
          // 后端已经提供了友好的显示标题，直接使用
          title = c.displayTitle
          
          // v5.3.4: 修正并简化折扣券显示
          if (c.couponType === 'DISCOUNT') {
            // 检查是否缺少"折"字：如果标题是 "数字+优惠券" 格式
            const matchNoDiscount = title.match(/^([\d.]+)优惠券$/)
            if (matchNoDiscount) {
              title = matchNoDiscount[1] + '折'
            }
            // 删除"优惠券"后缀，只保留"8.8折"格式
            else if (title.match(/^([\d.]+)折优惠券$/)) {
              title = title.replace('优惠券', '')
            }
          }
          
          // 对于折扣券，从 displayTitle 提取数字（如 "5折" -> "5"）
          if (c.couponType === 'DISCOUNT' && title.includes('折')) {
            displayVal = title.replace(/折.*/, '')
          } else if (c.couponType === 'FULL_REDUCE' && title.startsWith('¥')) {
            displayVal = title.substring(1)
          } else {
            displayVal = String(c.value || '')
          }
        } else {
          // 兼容旧数据：如果没有 displayTitle，使用旧逻辑
          displayVal = String(c.value || '')
          if (mapCouponType(c.couponType) === 'discount') {
             try {
                if (rule.discountRate) {
                   // 0.5 -> 5, 0.85 -> 8.5
                   displayVal = parseFloat((rule.discountRate * 10).toFixed(1)).toString()
                }
             } catch (e) {}
          }
          title = c.title || productName || getTypeTagText(mapCouponType(c.couponType)) || '优惠券'
        }
        
        // 描述：优先使用 displaySubTitle，其次使用 desc
        if (c.displaySubTitle) {
          desc = c.displaySubTitle
        } else {
          desc = c.desc || getDescFromRule(c.ruleJson, c.couponType)
        }
        
        // v5.3: 特殊券类型的描述增强
        if (c.couponType === 'DELIVERY_FEE') {
          const maxDiscount = c.value || rule.maxDiscount || rule.value || 3
          if (!c.displayTitle) title = '配送费抵扣券'
          if (!c.displaySubTitle) desc = `外卖订单可抵扣配送费 ¥${maxDiscount}`
        }
        
        // 新用户5折券的友好提示
        if (c.couponCode && c.couponCode.includes('NEW_USER') && c.couponType === 'DISCOUNT') {
          if (rule.limit === 'SINGLE_ITEM' && rule.scope === 'DRINK_ONLY') {
            desc = (desc || '限饮品') + ' | 限单杯最贵' + (rule.maxDiscountAmount ? ` | 最高抵¥${rule.maxDiscountAmount}` : '')
          }
        }

        return {
          id: c.id,
          type: mapCouponType(c.couponType),
          value: displayVal,
          unit: getUnitFromType(c.couponType),
          title: title,
          desc: desc,
          expireAt: formatExpireDate(c.expiresAt),
          status: currentCouponTab.value,
          minSpend: c.minAmount || 0,
          couponType: c.couponType,
          displayTitle: c.displayTitle, // v5.3: 保留原始字段供模板使用
          displaySubTitle: c.displaySubTitle
        }
      })
    }
  } catch (error) {
    console.error('加载优惠券失败:', error)
  }
}

// 跳转去使用
const handleUseCoupon = (coupon) => {
  currentTab.value = 'coffee-order'
  // 确保加载商品
  loadCoffeeProducts()
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 从 ruleJson 中解析 productName
const parseProductNameFromRule = (ruleJson) => {
  if (!ruleJson) return ''
  try {
    // 尝试 JSON 解析
    const parsed = JSON.parse(ruleJson)
    return parsed.productName || parsed.linkedProductName || ''
  } catch {
    // 如果不是有效 JSON，用正则提取
    const match = ruleJson.match(/"productName"\s*:\s*"([^"]+)"/)
    return match ? match[1] : ''
  }
}

// 从 ruleJson 中获取描述
const getDescFromRule = (ruleJson, type) => {
  if (!ruleJson) return ''
  try {
    const parsed = JSON.parse(ruleJson)
    if (parsed.desc) return parsed.desc
    // 根据类型生成描述
    if (type === 'EXCHANGE') {
      return parsed.productName ? `免费兑换 ${parsed.productName}` : '凭券免费兑换商品'
    }
    if (type === 'DISCOUNT') {
      let val = parsed.value || 0
      if (val >= 10) val = val / 10 // 50 -> 5折
      return val ? `享${val}折优惠` : '全场折扣优惠'
    }
    if (type === 'FULL_REDUCE') {
      let val = parsed.value || 0
      return parsed.minAmount && parsed.minAmount > 0 
        ? `满${parsed.minAmount}减${val}` 
        : `立减${val}元`
    }
  } catch {}
  return ''
}

// 获取券类型中文标签 (v5.0: 支持 BOGO + 附加券)
const getCouponTypeLabel = (type) => {
  const map = { 
    'EXCHANGE': '兑换券', 'DISCOUNT': '折扣券', 'FULL_REDUCE': '满减券', 'BOGO': '买一送一',
    'SHOT': '加浓缩券', 'DELIVERY_FEE': '配送费抵扣'
  }
  return map[type] || '优惠券'
}

// 获取券类型标签文本 (v5.0: 支持 bogo + 附加券)
const getTypeTagText = (type) => {
  const map = { 
    'free': '兑换券', 'discount': '折扣券', 'amount': '满减券', 'bogo': '买一送一',
    'shot': '加浓缩', 'delivery': '配送费'
  }
  return map[type] || '优惠券'
}

// 券类型映射 (v5.0: 支持 BOGO + 附加券)
const mapCouponType = (type) => {
  const map = { 
    'EXCHANGE': 'free', 'DISCOUNT': 'discount', 'FULL_REDUCE': 'amount', 'BOGO': 'bogo',
    'SHOT': 'shot', 'DELIVERY_FEE': 'delivery'
  }
  return map[type] || 'amount'
}

// 根据类型获取单位 (v5.0: 支持 BOGO + 附加券)
const getUnitFromType = (type) => {
  const map = { 
    'EXCHANGE': '杯', 'DISCOUNT': '折', 'FULL_REDUCE': '元', 'BOGO': '单',
    'SHOT': 'shot', 'DELIVERY_FEE': '元'
  }
  return map[type] || '元'
}

// 格式化过期日期
const formatExpireDate = (dateStr) => {
  if (!dateStr) return '--'
  try {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  } catch {
    return dateStr
  }
}

// 加载月度任务进度
// v5.4: 优惠券语义化辅助函数
const getSemanticTypeClass = (type) => {
  const map = {
    'BOGO': 'type-bogo',
    'EXCHANGE': 'type-exchange',
    'DISCOUNT': 'type-discount',
    'FULL_REDUCE': 'type-cash',
    'DELIVERY_FEE': 'type-delivery',
    'SHOT': 'type-addon',
    'NEW_PRODUCT_HALF': 'type-new-product',
    'NEW_PRODUCT_FREE': 'type-new-product'
  }
  return map[type] || 'type-cash'
}

const getCouponConditionText = (coupon) => {
   // v5.3.4: 优先使用后端返回的 displaySubTitle（最准确）
   if (coupon.displaySubTitle) {
     return coupon.displaySubTitle
   }
   
   // 次优先使用 desc 字段（兼容旧数据）
   if (coupon.desc) return coupon.desc
   
   // 最后才根据券类型生成默认描述（兜底逻辑）
   if (coupon.couponType === 'DELIVERY_FEE') return '仅限外卖订单使用'
   if (coupon.couponType === 'BOGO') return '购买任意饮品赠送一杯'
   
   // v5.3.4: EXCHANGE券详细说明（仅当后端未提供时才生成）
   if (coupon.couponType === 'EXCHANGE') {
     let rule = {}
     try {
       rule = JSON.parse(coupon.ruleJson || '{}')
     } catch(e) {}
     
     // 标准饮品免单券
     if (rule.skuLimit === 'STANDARD_ONLY') {
       return '适用于【经典意式咖啡】系列，最高抵扣¥40'
     }
     
     // 指定商品兑换券
     if (rule.productName) {
       return `可兑换【${rule.productName}】一杯`
     }
     
     // 通兑券（任意饮品）
     return '可兑换任意饮品一杯，最高抵扣¥40'
   }
   
   if (coupon.couponType === 'NEW_PRODUCT_HALF') return '仅限新品饮品使用，最高优惠¥20'
   if (coupon.couponType === 'NEW_PRODUCT_FREE') return '仅限新品饮品使用，最高抵扣¥40'
   return coupon.minSpend > 0 ? `满 ¥${coupon.minSpend} 可用` : '无门槛使用'
}

// 加载月度任务进度
const loadMonthlyTask = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/member/monthly-task', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success && data.data) {
      monthlyTaskData.value = {
        currentConsumption: data.data.currentConsumption || 0,
        currentDeliveryOrders: data.data.currentDeliveryOrders || 0
      }
    }
  } catch (error) {
    console.warn('加载月度任务失败（可能使用了模拟数据）:', error)
  }
}

const isReceivingBenefit = ref(false)
const monthlyBenefitStatus = ref({
  claimed: false,
  canClaim: false,
  benefitName: '',
  claimedLevel: null,
  currentLevel: null
})

// 等级权重映射
const levelOrder = { basic: 0, silver: 1, gold: 2, diamond: 3, black: 4 }

// 是否显示升级提示
const shouldShowUpgradeTip = computed(() => {
    if (!monthlyBenefitStatus.value.claimed) return false
    const current = monthlyBenefitStatus.value.currentLevel
    const claimed = monthlyBenefitStatus.value.claimedLevel
    if (!current || !claimed) return false
    return (levelOrder[current] || 0) > (levelOrder[claimed] || 0)
})

// 检查领取状态
const checkMonthlyBenefitStatus = async () => {
    if (!userStore.isLoggedIn) return
    try {
        const token = localStorage.getItem('token')
        const res = await fetch('http://localhost:8080/api/member/benefits/status', {
             headers: { 'Authorization': `Bearer ${token}` }
        })
        const data = await res.json()
        if (data.success) {
            monthlyBenefitStatus.value = data.data 
        }
    } catch(e) {
        console.warn('Failed to check benefit status', e)
    }
}

// 领取权益
const handleReceiveBenefit = async () => {
    if (isReceivingBenefit.value) return
    isReceivingBenefit.value = true
    try {
        const token = localStorage.getItem('token')
        const res = await fetch('http://localhost:8080/api/member/benefits/receive-monthly', {
             method: 'POST',
             headers: { 'Authorization': `Bearer ${token}` }
        })
        const data = await res.json()
        if (data.success) {
            ElMessage.success('权益已发放至您的账户')
            // Refresh status immediately
            checkMonthlyBenefitStatus()
            // 刷新优惠券列表
            if (currentTab.value === 'my-coupons') {
              loadUserCoupons()
            }
        } else {
            ElMessage.error(data.message || '领取失败')
        }
    } catch (e) {
        ElMessage.error('领取失败，请稍后重试')
    } finally {
        isReceivingBenefit.value = false
    }
}

// 监听 Tab 切换，重新加载优惠券
watch(currentCouponTab, () => {
  loadUserCoupons()
})

onMounted(async () => {
  if (userStore.token) {
    if (!userStore.userInfo) {
       await userStore.fetchUserInfo()
    }
    // 加载月度任务、权益状态和优惠券
    loadMonthlyTask()
    loadMonthlyTaskData() // Ensure task data is loaded
    checkMonthlyBenefitStatus()
    loadUserCoupons()
  } else {
    router.push('/login')
  }
})

// Editing States
const isEditingNickname = ref(false)
const editNickname = ref('')
const isEditingPhone = ref(false)
const editPhone = ref('')
const isEditingEmail = ref(false)
const editEmail = ref('')
const isEditingBirthday = ref(false)
const editBirthday = ref('')

// Points Mall States
const products = ref([])
const addresses = ref([])
const showOrderHistory = ref(false)
const showRedeemModal = ref(false)
const showAddressForm = ref(false)
const showAddAddressModal = ref(false)
const showAvatarModal = ref(false)
const showSimulateConsumeModal = ref(false)
const selectedProduct = ref(null)
const selectedAddressId = ref(null)
const isRedeeming = ref(false)

// 积分商城分类 - 使用 Lucide 图标
const mallCategories = [
  { value: 'all', label: '全部', icon: markRaw(Tag) },
  { value: 'coupon', label: '优惠券', icon: markRaw(Ticket) },
  { value: 'gift', label: '实物礼品', icon: markRaw(Gift) }
]
const activeMallCategory = ref('all')

// 筛选后的积分商品
const filteredMallProducts = computed(() => {
  if (activeMallCategory.value === 'all') {
    return products.value
  }
  return products.value.filter(p => p.category === activeMallCategory.value)
})
const redeemQuantity = ref(1)
const cancellingOrderId = ref(null)

// 积分兑换扩展状态
const redeemFulfillmentType = ref('PICKUP') // 'PICKUP' | 'DELIVERY' | 'VIRTUAL'
const redeemAddressId = ref('')

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
  const map = { basic: '基础会员', silver: '白银会员', gold: '黄金会员', diamond: '钻石会员', black: '黑金会员' }
  return map[lvl] || '基础会员'
})

// Dynamic Level Progress
const levelProgress = computed(() => {
  const currentExp = userStore.userInfo?.expTotal || 0;
  // Thresholds (Updated): Silver 500, Gold 1500, Diamond 4000, Black 9000
  let target = 500;
  let nextLvlName = '白银会员';
  let benefitText = '解锁 [积分兑换9.8折]';
  let nextLevelIcon = markRaw(Medal); 
  let nextLevelColor = '#B0BEC5'; // Silver

  if (currentExp < 500) {
     target = 500; nextLvlName = '白银会员';
     benefitText = '解锁 [积分兑换9.8折] 与 [生日月免单]';
     nextLevelIcon = markRaw(Medal);
     nextLevelColor = '#B0BEC5';
  } else if (currentExp < 1500) {
     target = 1500; nextLvlName = '黄金会员';
     benefitText = '解锁 [1.2倍积分] 与 [免配送权益]';
     nextLevelIcon = markRaw(Trophy);
     nextLevelColor = '#FFB300';
  } else if (currentExp < 4000) {
     target = 4000; nextLvlName = '钻石会员';
     benefitText = '解锁 [每月2次免配送] 与 [生日大礼包]';
     nextLevelIcon = markRaw(Gem);
     nextLevelColor = '#039BE5';
  } else if (currentExp < 9000) {
     target = 9000; nextLvlName = '黑金会员';
     benefitText = '解锁 [黑金加速包] 与 [线下品鉴权]';
     nextLevelIcon = markRaw(Crown);
     nextLevelColor = '#333333';
  } else {
     target = 99999; // Max cap as per user request
     nextLvlName = '黑金会员';
     benefitText = '您已尊享最高等级权益';
     nextLevelIcon = null; // No icon for max level
     nextLevelColor = '#FFD700';
  }

  const percentage = target === 0 ? 0 : Math.min(100, (currentExp / target) * 100);
  
  return {
    current: currentExp,
    target: target,
    percentage: percentage,
    nextLevelName,
    benefitText,
    remaining: Math.max(0, target - currentExp),
    isMax: currentExp >= 9000,
    nextLevelIcon,
    nextLevelColor
  };
})

// 修复：断签先判断机制
const effectiveSignInDays = computed(() => {
  const info = userStore.userInfo
  if (!info || !info.lastSigninDate) return 0
  
  const days = info.signInDays || 0
  if (days === 0) return 0
  
  const today = new Date()
  const lastSignDate = new Date(info.lastSigninDate)
  
  // 重置时间部分
  today.setHours(0, 0, 0, 0)
  lastSignDate.setHours(0, 0, 0, 0)
  
  const diffTime = Math.abs(today - lastSignDate)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  
  // 超过1天没签（即昨天没签），视为断签（显示0天或重置状态）
  // 注意：如果已经签了今天(0)，或者签了昨天(1)，则保持
  if (diffDays > 1) return 0
  
  return days
})

// 计算当前签到周期内的天数 (1-7)
const currentSignInCycleDay = computed(() => {
  const days = effectiveSignInDays.value
  if (days === 0) return 0
  return days % 7 === 0 ? 7 : days % 7
})

// 动态主题类名
const themeClass = computed(() => {
  return `theme-${userStore.userLevel || 'basic'}`
})

// 等级基于 EXP (expTotal) - v5.0 白皮书门槛
const nextLevelPoints = computed(() => {
  const lvl = userStore.userLevel || 'basic'
  // v5.0: basic=500, silver=1500, gold=4000, diamond=9000, black=∞
  const map = { basic: 500, silver: 1500, gold: 4000, diamond: 9000, black: 99999 }
  return map[lvl] || 500
})

const nextLevelName = computed(() => {
  const lvl = userStore.userLevel || 'basic'
  const map = { basic: '白银会员', silver: '黄金会员', gold: '钻石会员', diamond: '黑金会员', black: '黑金会员' }
  return map[lvl] || '白银会员'
})

// 当前 EXP
const currentExp = computed(() => {
  return userStore.userInfo?.expTotal || userStore.userInfo?.totalPoints || 0
})

const isSignedToday = computed(() => {
  const d = new Date()
  const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const lastSign = userStore.userInfo?.lastSigninDate || userStore.userInfo?.lastSignIn
  return lastSign === today
})

// v5.0: 判断今天是否为会员日 (Cozy Day) - 每周三
const isCozyDay = computed(() => {
  return new Date().getDay() === 3 // 0=周日, 3=周三
})

const profileComplete = computed(() => {
  const phone = userStore.userInfo?.phone
  const email = userStore.userInfo?.email
  return (phone && phone.length > 0) && (email && email.length > 0)
})

// v5.0: 每日固定 +2 积分
const getSigninPointsByLevel = () => {
  return 2 // v5.0 白皮书：每日固定 +2 积分
}

// v5.0: 获取7日签到阶梯数组（用于进度展示 - 现在每天都是+2）
const signinSteps = [2, 2, 2, 2, 2, 2, '🎁']

// 月度任务真实数据
// 月度任务真实数据 (v5.0 扩展结构)
const monthlyTaskData = ref({
  currentSpent: 0,
  currentDeliveryOrders: 0,
  monthlyOrderCount: 0, // 打卡达人
  morningOrderCount: 0, // 晨间唤醒
  newProductCount: 0,   // 新品猎人
  // v5.0: 挑战任务完成状态
  challengeOrderClaimed: false,
  challengeMorningClaimed: false,
  challengeDeliveryClaimed: false,
  challengeNewproductClaimed: false
})

// SSE 连接实例
const sseEventSource = ref(null)

// 建立 SSE 连接
const connectSSE = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return

    // 1. 获取 Ticket
    const res = await fetch('http://localhost:8080/api/member/sse/ticket', {
       method: 'POST',
       headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await res.json()
    if (!data.success) {
      console.warn('SSE Ticket获取失败', data)
      return
    }

    const ticket = data.data.ticket

    // 2. 建立连接
    if (sseEventSource.value) {
      sseEventSource.value.close()
    }
    
    console.log('正在建立SSE连接...')
    sseEventSource.value = new EventSource(`http://localhost:8080/api/member/sse/events?ticket=${ticket}`)

    sseEventSource.value.onopen = () => {
       console.log('SSE 连接成功')
    }

    // 3. 监听 order_completed 事件
    sseEventSource.value.addEventListener('order_completed', (event) => {
       try {
         const payload = JSON.parse(event.data)
         console.log('收到SSE消息:', payload)
         
         ElMessage.success({
           message: payload.message || '订单已完成',
           duration: 5000,
           showClose: true
         })
         
         // 刷新数据
         userStore.fetchMemberInfo()
         loadCoffeeOrders()
         loadMonthlyTaskData()
       } catch (e) {
         console.error('SSE消息解析失败', e)
       }
    })
    
    sseEventSource.value.onerror = (err) => {
       // 连接错误时关闭，避免无限重连导致浏览器卡死（如果需要重连机制可以更复杂点）
       console.error('SSE 连接错误', err)
       sseEventSource.value.close()
    }

  } catch (e) {
    console.error('SSE 连接初始化失败', e)
  }
}

onUnmounted(() => {
  if (sseEventSource.value) {
    sseEventSource.value.close()
    console.log('SSE 连接已关闭')
  }
})

// v5.0: 本月签到积分统计（每天固定2分）
const monthlySigninPoints = computed(() => {
  const signDays = userStore.userInfo?.signInDays || 0
  // v5.0: 每天固定 +2 积分
  return signDays * 2
})

// 任务中心 - 月消费金额（优先使用后端API数据）
const monthlyConsumption = computed(() => {
  // 统一使用后端 DTO 的 monthlySpent 字段
  // 注意：使用 ?? 防止 0 值被 || 判定为 false 导致回退
  return monthlyTaskData.value.currentSpent ?? userStore.userInfo?.monthlySpent ?? 0
})

// 任务中心 - 月外卖单数
const monthlyDeliveryOrders = computed(() => {
  return monthlyTaskData.value.currentDeliveryOrders ?? userStore.userInfo?.monthlyDeliveryOrders ?? 0
})

// v5.0 新增任务统计 (已改为在 Template 中直接引用 monthlyTaskData 以解决响应式更新问题)
// const monthlyOrderCount = ... 
// const morningOrderCount = ...
// const newProductCount = ...

// 任务中心 - 等级加成比例
const levelMultiplier = computed(() => {
  const map = { basic: 0, silver: 5, gold: 10, diamond: 15, black: 20 }
  return map[userStore.userLevel] || 0
})

// 任务中心 - 当前消费目标（用于进度条计算）
const currentConsumeTarget = computed(() => {
  if (monthlyConsumption.value >= 1000) return 1000
  if (monthlyConsumption.value >= 600) return 1000
  if (monthlyConsumption.value >= 300) return 600
  return 300
})

// 动态计算阶梯奖励显示的数值（基础奖励 * 等级加成）
const getMonthlyReward = (base) => {
  const bonus = 1 + (levelMultiplier.value / 100)
  return Math.round(base * bonus)
}

// 任务中心 - 进度百分比
const consumeProgressPercent = computed(() => {
  return Math.min(100, (monthlyConsumption.value / 1000) * 100)
})

const deliveryProgressPercent = computed(() => {
  return Math.min(100, (monthlyDeliveryOrders.value / 6) * 100)
})

// =========================================================
// v5.3 乐观UI策略: 只要进度达标，前端强制显示"已领取"
// 我们信任后端的自动补发机制，消除数据同步延迟带来的困惑
// =========================================================
const isOrderTaskCompleted = computed(() => {
    const claimed = monthlyTaskData.value.challengeOrderClaimed ?? userStore.userInfo?.challengeOrderClaimed ?? false
    if (claimed) return true
    const count = monthlyTaskData.value.monthlyOrderCount ?? userStore.userInfo?.monthlyOrderCount ?? 0
    return count >= 4
})

const isMorningTaskCompleted = computed(() => {
    const claimed = monthlyTaskData.value.challengeMorningClaimed ?? userStore.userInfo?.challengeMorningClaimed ?? false
    if (claimed) return true
    const count = monthlyTaskData.value.morningOrderCount ?? userStore.userInfo?.morningOrderCount ?? 0
    return count >= 3
})

const isDeliveryTaskCompleted = computed(() => {
    const claimed = monthlyTaskData.value.challengeDeliveryClaimed ?? userStore.userInfo?.challengeDeliveryClaimed ?? false
    if (claimed) return true
    const count = monthlyTaskData.value.currentDeliveryOrders ?? userStore.userInfo?.monthlyDeliveryOrders ?? 0
    return count >= 2
})

const isNewProductTaskCompleted = computed(() => {
    const claimed = monthlyTaskData.value.challengeNewproductClaimed ?? userStore.userInfo?.challengeNewproductClaimed ?? false
    if (claimed) return true
    const count = monthlyTaskData.value.newProductCount ?? userStore.userInfo?.newProductCount ?? 0
    return count >= 3
})

// 黑卡加速包进度百分比 (总额度300)
const accelerateProgressPercent = computed(() => {
  const remaining = parseFloat(userStore.userInfo?.monthlyAccelerateRemaining ?? 300)
  const used = Math.max(0, 300 - remaining)
  return Math.min(100, (used / 300) * 100)
})

// 根据等级获取消费积分倍率
const getConsumeMultiplier = () => {
  const level = userStore.userLevel || 'basic'
  const map = { basic: 1, silver: 1.1, gold: 1.2, diamond: 1.3, black: 1.5 } // v5.0
  
  // 黑卡用户且有加速包额度时，享受1.7倍积分
  if (level === 'black' && (userStore.userInfo?.monthlyAccelerateRemaining || 0) > 0) {
    return 1.7
  }
  
  return map[level] || 1
}

// 获取积分兑换折扣（与后端一致）
const getRedeemDiscount = () => {
  const level = userStore.userLevel || 'basic'
  // v5.0: Black 0.85, Diamond 0.90, Gold 0.95, Silver 0.98
  const map = { basic: 1, silver: 0.98, gold: 0.95, diamond: 0.90, black: 0.85 }
  return map[level] || 1
}

// 计算折扣后的兑换价格
const getDiscountedCost = () => {
  const original = (selectedProduct.value?.pointsPrice || 0) * redeemQuantity.value
  return Math.ceil(original * getRedeemDiscount())
}

// 加载月度任务数据
// 加载月度任务数据
const loadMonthlyTaskData = async (retryCount = 0) => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return
    const response = await fetch('http://localhost:8080/api/member/monthly-task', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success && data.data) {
      console.log('[Member] API返回数据:', data.data)
      const d = data.data
      
      // v5.3: 修复响应式丢失问题 - 逐字段更新而不是替换整个对象
      // 保持 monthlyTaskData.value 的引用不变，确保 computed 能够监听到属性变化
      const target = monthlyTaskData.value
      
      target.currentSpent = d.currentSpent ?? target.currentSpent ?? 0
      target.currentDeliveryOrders = d.currentDeliveryOrders ?? d.deliveryOrderCount ?? 0
      
      // 显式更新关键任务计数
      target.monthlyOrderCount = d.monthlyOrderCount ?? 0
      target.morningOrderCount = d.morningOrderCount ?? 0
      target.newProductCount = d.newProductCount ?? 0
      
      // 更新领取状态
      target.challengeOrderClaimed = d.challengeOrderClaimed ?? false
      target.challengeMorningClaimed = d.challengeMorningClaimed ?? false
      target.challengeDeliveryClaimed = d.challengeDeliveryClaimed ?? false
      target.challengeNewproductClaimed = d.challengeNewproductClaimed ?? false
      
      // 更新其他可能存在的字段
      target.taskMonth = d.taskMonth
      target.userId = d.userId
      
      console.log('[Member] 本地数据已更新(逐字段):', JSON.parse(JSON.stringify(target)))
      
      // 强制触发视图更新
      taskRefreshKey.value++

      // =========================================================
      // v5.3 智能重试: 如果发现进度达标但未领取，可能是后端刚触发补发但没及时返回最新状态
      // 自动静默刷新一次，确保 UI 变绿
      // =========================================================
      if (!retryCount) retryCount = 0
      const needRetry = (
         (d.monthlyOrderCount >= 4 && !d.challengeOrderClaimed) ||
         (d.morningOrderCount >= 3 && !d.challengeMorningClaimed) ||
         ((d.currentDeliveryOrders || d.monthlyDeliveryOrders) >= 2 && !d.challengeDeliveryClaimed) ||
         (d.newProductCount >= 3 && !d.challengeNewproductClaimed)
      )

      if (needRetry && retryCount < 2) {
         console.log('[Member] 检测到状态不一致，触发自动修复重试...', retryCount + 1)
         setTimeout(() => {
             loadMonthlyTaskData(retryCount + 1)
         }, 500) // 延迟500ms给后端事务一点时间
      }
    }
  } catch (error) {
    console.error('加载月度任务数据失败:', error)
  }
}

// ========== v5.3: 轮询与手动刷新机制 ==========
const isRefreshingTask = ref(false)
const taskRefreshKey = ref(0) // v5.3: 强制渲染Key，解决响应式卡死问题
let monthlyTaskPollingTimer = null
const POLLING_INTERVAL = 30000 // 30秒轮询一次

// 手动刷新月度任务
const handleRefreshMonthlyTask = async () => {
  if (isRefreshingTask.value) return
  isRefreshingTask.value = true
  try {
    await loadMonthlyTaskData()
    // 强制刷新视图
    taskRefreshKey.value++
    ElMessage.success({ message: '任务进度已更新', duration: 1500 })
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    isRefreshingTask.value = false
  }
}

// 手动刷新优惠券
const isRefreshingCoupons = ref(false)
const couponTabsKey = ref(0) // v5.3.8: 强制重挂载券包组件，确保UI与字段映射一致
const handleRefreshCoupons = async () => {
  if (isRefreshingCoupons.value) return
  isRefreshingCoupons.value = true
  try {
    couponTabsKey.value++
    ElMessage.success({ message: '优惠券已更新', duration: 1500 })
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    isRefreshingCoupons.value = false
  }
}

// v5.3.9: 从券包页切换到咖啡下单选项卡
const handleUseCouponFromTabs = (couponInfo) => {
  console.log('使用优惠券，切换到咖啡下单:', couponInfo)
  // 切换到咖啡下单选项卡
  currentTab.value = 'coffee-order'
  // 可以在此保存选中的券信息供后续使用
  ElMessage.success({ message: '已切换到点单页面，请选择商品后使用优惠券', duration: 2000 })
}

// 手动刷新订单
const isRefreshingOrders = ref(false)
const handleRefreshOrders = async () => {
  if (isRefreshingOrders.value) return
  isRefreshingOrders.value = true
  try {
    await loadCoffeeOrders()
    ElMessage.success({ message: '订单列表已更新', duration: 1500 })
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    isRefreshingOrders.value = false
  }
}

// 启动轮询
const startMonthlyTaskPolling = () => {
  stopMonthlyTaskPolling() // 先清理旧的
  monthlyTaskPollingTimer = setInterval(() => {
    loadMonthlyTaskData()
  }, POLLING_INTERVAL)
  console.log('[Member] 月度任务轮询已启动，间隔:', POLLING_INTERVAL, 'ms')
}

// 停止轮询
const stopMonthlyTaskPolling = () => {
  if (monthlyTaskPollingTimer) {
    clearInterval(monthlyTaskPollingTimer)
    monthlyTaskPollingTimer = null
    console.log('[Member] 月度任务轮询已停止')
  }
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

// 咖啡订单取消
const cancelCoffeeOrder = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定要取消这个订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 调用 API
    const res = await cancelOrder(orderId)
    // 根据 API response 结构判断成功
    if (res.success || (res.data && res.data.success) || res.status === 200) {
      ElMessage.success('订单已取消')
      loadCoffeeOrders()
    } else {
      ElMessage.error(res.message || (res.data && res.data.message) || '取消失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('操作失败')
    }
  }
}

// 打开兑换弹窗（增强版）
const openRedeemDialog = (product) => {
  selectedProduct.value = product
  redeemQuantity.value = 1
  
  // 自动判断交付类型
  if (product.productType === 'VIRTUAL' || (product.name && product.name.includes('券'))) {
     redeemFulfillmentType.value = 'VIRTUAL'
  } else {
     redeemFulfillmentType.value = 'PICKUP' // 默认自提
  }

  // 默认选中默认地址
  if (addresses.value && addresses.value.length > 0) {
    const def = addresses.value.find(a => a.isDefault)
    redeemAddressId.value = def ? def.id : addresses.value[0].id
  } else {
    redeemAddressId.value = ''
  }

  showRedeemModal.value = true
}

// 确认兑换（增强版）
const handleRedeemProduct = async () => {
  if (!selectedProduct.value) return
  if (isRedeeming.value) return
  
  // 校验
  if (redeemFulfillmentType.value === 'DELIVERY' && !redeemAddressId.value) {
    ElMessage.warning('请选择收货地址')
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
        quantity: redeemQuantity.value,
        fulfillmentType: redeemFulfillmentType.value,
        addressId: redeemFulfillmentType.value === 'DELIVERY' ? redeemAddressId.value : null
      })
    })

    const data = await response.json()
    if (data.success) {
      ElMessage.success('兑换成功！')
      showRedeemModal.value = false
      
      // 更新积分
      if (userStore.userInfo) {
        userStore.userInfo.currentPoints -= (data.data.pointsCost || 0)
        // 刷新 Store
        userStore.fetchMemberInfo()
      }
      
      // 刷新列表
      if (typeof loadProducts === 'function') loadProducts() 
    } else {
      ElMessage.error(data.message || '兑换失败')
    }
  } catch (error) {
    console.error('兑换出错:', error)
    ElMessage.error('网络错误，请重试')
  } finally {
    isRedeeming.value = false
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
      
      // 刷新月度任务数据
      await loadMonthlyTaskData()
      connectSSE() // 启动 SSE 连接

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
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const formatSpecs = (item) => {
  if (!item) return ''
  
  // v5.3 修复：甜品不显示饮品参数
  const isBakeryItem = !item.temperature && !item.cupSize
  if (isBakeryItem) return ''
  
  const parts = []

  // 1. 基础规格翻译（兼容大小写）
  const map = {
    // Cup
    'STANDARD': '标准杯', 'standard': '标准杯', 'LARGE': '大杯', 'large': '大杯', 'MEDIUM': '中杯', 'medium': '中杯',
    // Temp
    'HOT': '热', 'hot': '热', 'COLD': '冰', 'cold': '冰', 'iced': '冰', 'WARM': '温', 'warm': '温',
    // Sugar (v5.3.1: 添加LESS和HALF标准映射)
    'NONE': '无糖', 'none': '无糖', 
    'LESS': '少糖', 'less': '少糖', 
    'HALF': '半糖', 'half': '半糖', 
    'LIGHT': '微甜', 'light': '微甜', 
    'full': '标准甜', 'STANDARD_SUGAR': '标准甜', 'MEDIUM': '少甜',
    // Strength
    'NORMAL': '标准浓度', 'STRONG': '加浓'
  }
  
  if (item.cupSize) parts.push(map[item.cupSize] || item.cupSize)
  if (item.temperature) parts.push(map[item.temperature] || item.temperature)
  
  // v5.3 修复：Sugar 只在非标准时显示
  if (item.sugarLevel) {
     if (item.sugarLevel === 'STANDARD' || item.sugarLevel === 'standard' || item.sugarLevel === 'full') {
       // 标准甜不显示
     } else {
       parts.push(map[item.sugarLevel] || item.sugarLevel)
     }
  }
  
  // 浓度只在加浓时显示
  if (item.coffeeStrength && item.coffeeStrength === 'STRONG') {
    parts.push(map[item.coffeeStrength] || item.coffeeStrength)
  }

  // 2. v5.3 修复：奶类只在非默认值时显示（避免美式显示"全脂奶"）
  if (item.milkType && item.milkType !== 'WHOLE') {
    const milkMap = { 'OAT': '燕麦奶', 'COCONUT': '椰奶', 'SOY': '豆奶' }
    const m = milkMap[item.milkType]
    if (m) parts.push(m)
  } else if (item.optionsJson) {
     try {
       const opts = JSON.parse(item.optionsJson)
       if (opts.milkType && opts.milkType !== 'WHOLE') {
          const milkMap = { 'OAT': '燕麦奶', 'COCONUT': '椰奶', 'SOY': '豆奶' }
          const m = milkMap[opts.milkType]
          if (m) parts.push(m)
       }
     } catch (e) {}
  }
  
  return parts.length > 0 ? parts.join(' / ') : ''
}

const getDiningMethodText = (method) => {
  const map = { 'DINE_IN': '堂食', 'TAKEOUT': '自提', 'DELIVERY': '外卖' }
  return map[method] || '自提'
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
      ElMessage.success(data.message || '邀请码绑定成功！')
      inputInviteCode.value = ''

      // 立即在前端更新状态（优化用户体验）
      if (userStore.userInfo) {
        // 先检查是否已经累加过（防止并发问题，虽然这里几率很小）
        if (!userStore.userInfo.hasAppliedInviteCode) {
          // userStore.userInfo.currentPoints = (userStore.userInfo.currentPoints || 0) + 80
          // userStore.userInfo.totalPoints = (userStore.userInfo.totalPoints || 0) + 80
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

// 图片 URL 处理：相对路径加后端地址
const getImageUrl = (url) => {
  if (!url) return '/images/products/default.png'
  if (url.startsWith('http')) return url
  return `http://localhost:8080${url.startsWith('/') ? '' : '/'}${url}`
}

const handleLogout = async () => {
  const ok = await userStore.logout()
  if (ok) {
    router.push('/')
    return
  }
  window.alert('退出失败，请检查后端服务或网络后重试')
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
      if (fieldName === 'birthday') isEditingBirthday.value = false

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

const startEditBirthday = () => {
  editBirthday.value = userStore.userInfo?.birthday || '2000-01-01'
  isEditingBirthday.value = true
}

const cancelEditBirthday = () => {
  isEditingBirthday.value = false
}

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
    // v6.0 修复: 必须传入 token，后端才能识别用户，返回正确的已兑换数量
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/member/mall/products', {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    })
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

// 订单相关状态
const redeemOrders = ref([])  // 兑换订单


// 加载兑换订单列表
const loadRedeemOrders = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/member/mall/orders', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    const data = await response.json()
    if (data.success) {
      redeemOrders.value = data.data || []
    }
  } catch (error) {
    console.error('Failed to load redeem orders:', error)
  }
}

// 兼容旧的loadOrders（用于兑换订单）
const loadOrders = loadRedeemOrders


// 处理下单成功
const handleOrderSuccess = async (data) => {
  showSimulateConsumeModal.value = false
  // 刷新会员积分信息
  await userStore.fetchMemberInfo()
  // 刷新月度任务数据
  await loadMonthlyTaskData()
  // 刷新订单列表
  await loadOrders()
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

// ========== 咖啡订单相关方法 ==========
const handleCoffeeOrderCreated = (order) => {
  ElMessage.success(`订单创建成功！订单号：${order.orderNo}`)
  // 切换到咖啡订单记录列表
  currentTab.value = 'coffee-orders-history'
  loadCoffeeOrders()
  // v5.0: 刷新月度任务数据（更新打卡达人、晨间唤醒等进度）
  loadMonthlyTaskData()
}

const refreshUserInfo = async () => {
  await userStore.fetchUserInfo()
  await userStore.fetchMemberInfo()
}

onMounted(async () => {
  console.log('[Member] onMounted triggered, isLoggedIn:', userStore.isLoggedIn)
  if (!userStore.isLoggedIn) {
    console.log('Redirecting to login...')
  } else {
    // 加载全部必要信息
    userStore.fetchUserInfo()
    userStore.fetchMemberInfo()
    loadProducts()
    loadAddresses()
    
    // v5.3: 等待月度任务数据加载完成后再启动轮询
    await loadMonthlyTaskData()
    console.log('[Member] 月度任务数据加载完成:', monthlyTaskData.value)
    
    connectSSE()
    // v5.3: 启动月度任务轮询
    startMonthlyTaskPolling()
  }
})

// 监听标签切换，进入个人信息页时刷新数据
watch(currentTab, (newTab) => {
  if (newTab === 'personal-info') {
    userStore.fetchUserInfo()
  }
  // v5.3: 切换到会员中心时刷新月度任务
  if (newTab === 'member-center') {
    loadMonthlyTaskData()
  }
  // v5.5: 切换到权益页时检查本月领取状态
  if (newTab === 'member-benefits') {
     checkMonthlyBenefitStatus()
  }
})

// v5.3: 组件卸载时清理轮询和 SSE 连接
onUnmounted(() => {
  stopMonthlyTaskPolling()
  if (sseEventSource.value) {
    sseEventSource.value.close()
    sseEventSource.value = null
  }
})
</script>

<style scoped>
/* 
  Premium UI Design - Scoped to avoid conflicts 
  Theme: Modern Coffee Lounge (Dynamic Themes)
*/

/* --- 动态主题变量定义 --- */
.theme-basic {
  --primary-color: #C69C6D;
  --primary-light: #FDF8F3;
  --primary-dark: #8D6E63;
  --accent-color: #E6B07A;
  --card-bg-gradient: linear-gradient(135deg, #1f1f1f, #2c2c2c); /* Basic Card: Dark Charcoal */
  --highlight-text: #C69C6D;
  --button-hover: #b08d55;
}

.theme-silver {
  --primary-color: #90A4AE;
  --primary-light: #ECEFF1;
  --primary-dark: #546E7A;
  --accent-color: #CFD8DC;
  --card-bg-gradient: linear-gradient(135deg, #78909C, #B0BEC5); /* Silver Card: Metallic */
  --highlight-text: #546E7A;
  --button-hover: #78909C;
}

.theme-gold {
  --primary-color: #D4AF37;
  --primary-light: #FFF8E1;
  --primary-dark: #A68B29;
  --accent-color: #FFD700;
  --card-bg-gradient: linear-gradient(135deg, #D4AF37, #FDD835); /* Gold Card: Shining Gold */
  --highlight-text: #D4AF37;
  --button-hover: #c49f2b;
}

.theme-diamond {
  --primary-color: #64B5F6;
  --primary-light: #E3F2FD;
  --primary-dark: #1976D2;
  --accent-color: #64B5F6;
  --card-bg-gradient: linear-gradient(135deg, #42A5F5, #90CAF9); /* Diamond: Ethereal Blue */
  --highlight-text: #1976D2;
  --button-hover: #1E88E5;
}

.theme-black {
  --primary-color: #212121;
  --primary-light: #FAFAFA;
  --primary-dark: #000000;
  --accent-color: #C69C6D;
  --card-bg-gradient: linear-gradient(135deg, #000000, #2c2c2c); /* Black Card: Premium Black */
  --highlight-text: #C69C6D; /* 金色字体点缀 */
  --button-hover: #333333;
}

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
  padding: 40px 20px 14px 20px;
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
  margin: 0 0 30px 0;
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
  text-align: center;
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
  margin-right: 8px;
}

.nav-text {
  font-size: 15px;
  font-weight: 500;
}

.sidebar-footer {
  align-items: flex-start;
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

/* v5.0 休眠态 UI - 流金磨砂滤镜 */
.digital-card.dormant {
  background: linear-gradient(135deg, #2a2a2a, #3a3a3a, #2a2a2a) !important;
  border: 1px solid rgba(212, 175, 55, 0.3) !important;
  color: #888 !important;
  filter: saturate(0.5) brightness(0.8);
  animation: dormant-pulse 5s ease-in-out infinite;
}

.digital-card.dormant::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.05) 0%, transparent 50%, rgba(212, 175, 55, 0.05) 100%);
  pointer-events: none;
}

.digital-card.dormant .value,
.digital-card.dormant .label {
  color: #999 !important;
  text-shadow: none !important;
}

.digital-card.dormant .card-tier::after {
  content: ' (休眠中)';
  font-size: 10px;
  opacity: 0.7;
}

@keyframes dormant-pulse {
  0%, 100% {
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }
  50% {
    box-shadow: 0 15px 40px rgba(212, 175, 55, 0.15);
  }
}

/* 钻石会员 - 璀璨闪耀设计 */
.digital-card.diamond {
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 25%, #4ca1af 50%, #2a5298 75%, #1e3c72 100%);
  border: 1px solid rgba(120, 200, 255, 0.4);
  color: #ffffff;
  animation: diamond-pulse 4s ease-in-out infinite;
  box-shadow: 0 20px 60px rgba(74, 161, 175, 0.3);
}

@keyframes diamond-pulse {
  0%, 100% {
    box-shadow: 0 20px 60px rgba(74, 161, 175, 0.3), inset 0 0 40px rgba(120, 200, 255, 0.1);
  }
  50% {
    box-shadow: 0 25px 70px rgba(74, 161, 175, 0.5), inset 0 0 60px rgba(120, 200, 255, 0.2);
  }
}

.digital-card.diamond .label {
  color: rgba(255, 255, 255, 0.8);
}

.digital-card.diamond .value {
  color: #ffffff;
  text-shadow: 0 0 15px rgba(120, 200, 255, 0.5);
}

.digital-card.diamond .card-tier {
  color: #7dd3e8;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(125, 211, 232, 0.6);
}

.digital-card.diamond .card-chip {
  background: linear-gradient(135deg, #7dd3e8, #4ca1af);
  border: none;
}

.digital-card.diamond::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 35%, rgba(255, 255, 255, 0.3) 50%, transparent 65%);
  animation: diamond-shine 3s ease-in-out infinite;
}

@keyframes diamond-shine {
  0%, 100% {
    transform: translateX(-100%) rotate(25deg);
  }
  50% {
    transform: translateX(100%) rotate(25deg);
  }
}

.digital-card.diamond::after {
  content: '💎';
  position: absolute;
  bottom: 12px;
  right: 20px;
  font-size: 40px;
  opacity: 0.3;
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
  color: var(--highlight-text);
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
  background: var(--primary-dark);
  color: white;
  border: none;
  padding: 10px 25px;
  border-radius: 30px;
  cursor: pointer;
  font-size: 14px;
  transition: transform 0.2s, background 0.3s;
}

.signin-btn:hover {
  transform: translateY(-2px);
  background: var(--button-hover);
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
  background: var(--primary-color);
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
  width: 45px;
  height: 45px;
  background: #f0f0f0;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
  transition: all 0.3s;
}

.step-circle .day-num {
  font-weight: bold;
  font-size: 14px;
}

.step-circle .day-points {
  font-size: 10px;
  color: var(--primary-color);
}

.step.active .step-circle {
  background: var(--primary-color);
  color: white;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
}

.step.active .step-circle .day-points {
  color: rgba(255, 255, 255, 0.8);
}

.signin-bonus-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: var(--primary-light);
  border-radius: 8px;
  font-size: 14px;
  color: #555;
}

.signin-bonus-hint .bonus-icon {
  font-size: 20px;
}

.signin-bonus-hint strong {
  color: var(--primary-color);
  font-size: 16px;
}

.signin-bonus-hint.pending {
  background: #f5f5f5;
  color: #888;
}

.signin-bonus-hint.pending strong {
  color: #666;
}

/* 签到统计 */
.signin-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.month-points {
  font-size: 13px;
  color: #666;
}

.month-points strong {
  color: var(--primary-color);
  font-size: 18px;
  font-weight: 600;
}

/* 任务中心样式 */
.task-center-section {
  background: white;
  padding: 24px;
  border-radius: 16px;
  margin-top: 24px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.04);
}

/* 黑卡加速包样式 */
.black-accelerate-box {
  background: linear-gradient(135deg, #1a1a1a 0%, #333 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  color: #D4AF37;
  border: 1px solid rgba(212, 175, 55, 0.3);
  box-shadow: 0 4px 15px rgba(0,0,0,0.2);
}

.black-accelerate-box .box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.black-accelerate-box .box-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.black-accelerate-box .box-status strong {
  font-size: 18px;
  color: #FFD700;
}

.accelerate-progress {
  margin-bottom: 12px;
}

.accelerate-progress .progress-bar-bg {
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.accelerate-progress .progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #D4AF37, #FFD700);
  border-radius: 4px;
  transition: width 0.6s ease;
}

.accelerate-progress .progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(212, 175, 55, 0.7);
}

.accelerate-tip {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.05);
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 2px solid #D4AF37;
}

.accelerate-tip strong {
  color: #FFD700;
}

.task-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

.task-card {
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #eee;
}


.task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.task-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-title-group i {
  font-size: 20px;
  color: var(--primary-color);
}

.task-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.task-boost {
  background: rgba(198, 156, 109, 0.1);
  color: var(--primary-color);
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.task-body {
  position: relative;
  padding-bottom: 24px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
}

.current-val {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.target-val {
  color: #999;
  margin-top: 6px;
}

.task-progress-track {
  position: relative;
  margin-top: 12px;
}

.task-progress-track .progress-bar {
  height: 6px;
  background: #eee;
  border-radius: 3px;
  overflow: visible;
}

.task-progress-track .progress-fill {
  height: 100%;
  background: var(--primary-color);
  border-radius: 3px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.task-progress-track .progress-fill::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translate(50%, -50%); /* 确保圆点中心正好在进度条末端 */
  width: 12px;
  height: 12px;
  background: white;
  border: 2px solid var(--primary-color);
  border-radius: 50%;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  z-index: 2;
}

.progress-nodes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.progress-nodes .node {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 10px;
  height: 10px;
}

.node-dot {
  width: 8px;
  height: 8px;
  background: #e0e0e0;
  border-radius: 50%;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
  flex-shrink: 0;
}

.node.active .node-dot {
  background: var(--primary-color);
  box-shadow: 0 0 0 2px white, 0 0 0 3px var(--primary-color);
  transform: scale(1.1);
}

.node-label {
  position: absolute;
  top: 12px;
  font-size: 11px;
  color: #aaa;
  white-space: nowrap;
}

.node-reward {
  position: absolute;
  top: 26px;
  font-size: 10px;
  color: var(--primary-color);
  background: rgba(198, 156, 109, 0.1);
  padding: 1px 4px;
  border-radius: 4px;
  white-space: nowrap;
  opacity: 0;
  transform: translateY(-4px);
  transition: all 0.3s;
}

.node.active .node-reward {
  opacity: 1;
  transform: translateY(0);
}

.node.active .node-label {
  color: #333;
  font-weight: 500;
}

.task-note {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fffbe6;
  border-radius: 8px;
  font-size: 13px;
  color: #8b6914;
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-note strong {
  color: var(--primary-color);
}

.task-note i {
  font-size: 16px;
  color: #d4b106;
}

/* Coupon Styles */
.coupons-container {
  max-width: 800px;
  margin: 0 auto;
}

.coupon-tabs {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.tab-item {
  padding: 12px 0;
  background: none;
  border: none;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
}

.tab-item.active {
  color: var(--primary-color);
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--primary-color);
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.coupon-card {
  display: flex;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  position: relative;
  overflow: hidden;
  transition: transform 0.2s;
  height: 110px;
}

.coupon-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}

.coupon-left {
  width: 110px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  border-right: 1px dashed rgba(255,255,255,0.3);
}

/* 满减券 - 红色 */
.coupon-left.amount {
  background: linear-gradient(135deg, #f56c6c, #e55b5b);
}

/* 折扣券 - 橙色 */
.coupon-left.discount {
  background: linear-gradient(135deg, #e6a23c, #d99630);
}

/* 兑换券 - 绿色 */
.coupon-left.free {
  background: linear-gradient(135deg, #67c23a, #5daf34);
}

.coupon-amount {
  display: flex;
  align-items: baseline;
}

.coupon-amount .value {
  font-size: 32px;
  font-weight: bold;
}

.coupon-amount .unit {
  font-size: 12px;
  margin-left: 2px;
}

.coupon-amount .prefix {
  font-size: 14px;
  margin-right: 2px;
}

.coupon-condition {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.9;
}

.coupon-main {
  flex: 1;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.coupon-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: #fdf6ec;
  color: #e6a23c;
  border-radius: 4px;
  font-weight: normal;
}

/* 兑换券 - 绿色 */
.type-tag.free {
  background: #e8f5e9;
  color: #43a047;
}

/* 折扣券 - 橙色 */
.type-tag.discount {
  background: #fff3e0;
  color: #ef6c00;
}

/* 满减券 - 红色 */
.type-tag.amount {
  background: #ffebee;
  color: #e53935;
}

.coupon-desc {
  font-size: 12px;
  color: #999;
  margin: 0;
}

.coupon-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
}

.expire-date {
  color: #999;
}

.urgent-tag {
  color: #ff4d4f;
  background: #fff1f0;
  padding: 2px 6px;
  border-radius: 4px;
}

.coupon-right {
  width: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-left: 1px dashed #eee;
}

.use-btn {
  padding: 6px 16px;
  border-radius: 16px;
  background: var(--primary-color);
  color: white;
  font-size: 13px;
  border: none;
  cursor: pointer;
  transition: all 0.3s;
}

.use-btn:hover {
  background: var(--primary-dark);
}

/* Status Styles */
.coupon-card.used .coupon-left,
.coupon-card.expired .coupon-left {
  background: #d9d9d9;
}

.coupon-card.used .type-tag,
.coupon-card.expired .type-tag {
  background: #f5f5f5;
  color: #999;
}

.coupon-info, .coupon-amount {
  opacity: 0.6;
}

.status-text {
  color: #999;
  font-size: 13px;
  font-weight: 500;
}

/* Sawtooth decoration using masking (simple version) */
.sawtooth-left, .sawtooth-right {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 6px;
  background-image: radial-gradient(circle at 1px 8px, transparent 4px, white 4px);
  background-size: 10px 16px;
  z-index: 2;
}

.sawtooth-left {
  left: -3px;
  background-position: 0 0;
}

.sawtooth-right {
  right: -3px;
  background-position: 100% 0;
  transform: rotate(180deg);
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
  /* v5.8: 自动推到底部 */
  margin-top: auto;
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
  /* v5.8: Flex 布局让按钮固定在底部 */
  display: flex;
  flex-direction: column;
  height: 100%;
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
  flex-shrink: 0;
}

.mall-card .card-details {
  padding: 16px;
  /* v5.8: 让内容区域自动撑开，按钮推到底部 */
  flex: 1;
  display: flex;
  flex-direction: column;
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

.benefit.member-layout {
  display: flex;
  min-height: 100vh;
  background-color: #F9F5F0; /* Warm Beige */
  color: #333;
}

.dashboard-view {
  animation: fadeIn 0.4s ease-out;
  max-width: 1400px;
  margin: 0 auto;
}

/* Split Layout Equal Height */
.dashboard-split-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
  margin-top: 24px;
  align-items: start;
}

.layout-col-left, .layout-col-right {
  display: flex;
  flex-direction: column;
}

.points-guide-section,
.task-center-section {
  background: #fff;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03); /* Unified Soft Shadow */
  height: 100%; /* Fill height */
  display: flex;
  flex-direction: column;
}

/* Left Column Promo Banner */
.promo-banner {
  margin-top: auto; /* Push to bottom */
  padding-top: 24px;
  width: 100%;
}
.promo-banner img {
  width: 100%;
  border-radius: 16px;
  display: block;
  object-fit: cover;
  transition: transform 0.3s ease;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.promo-banner img:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
}

/* Done Check Icon */
.status-check {
  color: #D97706; /* Gold */
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #FEF3C7; /* Soft Gold bg */
  border-radius: 50%;
}

/* Champagne Gold Points Booster */
.black-accelerate-box {
  background: linear-gradient(135deg, #FDE68A 0%, #D97706 100%);
  border-radius: 16px;
  padding: 20px;
  color: #451a03;
  margin-bottom: 24px;
  box-shadow: 0 8px 16px rgba(217, 119, 6, 0.2);
}
.black-accelerate-box .box-title { color: #451a03; }
.black-accelerate-box .box-status { color: #451a03; opacity: 0.9; }
.progress-bar-bg { background: rgba(255,255,255,0.3); }
.progress-fill { background: #fff; }

/* Clean Headers */
.section-title h4 {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: -0.5px;
}
.section-title .subtitle {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
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
  --ribbon-color: #B8865B;
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
  --ribbon-color: #6c757d;
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
  --ribbon-color: #B8860B;
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
  --ribbon-color: #D4AF37;
}

/* 钻石会员卡片 - 幻彩蓝 */
.benefit-card.diamond {
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 50%, #90CAF9 100%);
  border-color: rgba(33, 150, 243, 0.3);
  --ribbon-color: #1976D2;
}
.benefit-card.diamond::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #64B5F6, #42A5F5, #1E88E5);
}
.benefit-card.diamond .benefit-header h4 { color: #1565C0; }
.benefit-card.diamond .level-badge { color: #1976D2; }
.benefit-card.diamond .benefit-list li::before { color: #2196F3; }

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
  background: var(--ribbon-color, #C69C6D);
  color: white;
  font-size: 11px;
  padding: 3px 35px;
  transform: rotate(45deg);
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
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



/* 当前等级信息 */
.current-level-info {
  background: linear-gradient(135deg, #FFF9F0 0%, #FFF5E6 100%);
  padding: 24px;
  border-radius: 16px;
  text-align: center;
}

.current-level-info p {
  margin: 8px 0;
  font-size: 15px;
  color: #666;
}

.current-level-info strong {
  color: #C69C6D;
  font-size: 18px;
}
/* 积分到期提醒 */
.expiring-alert {
  background: #FFF3E0;
  border: 1px solid #FFE0B2;
  color: #E65100;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  
  .alert-icon {
    font-size: 18px;
  }
  
  strong {
    font-weight: 600;
    margin: 0 4px;
  }
  
  .use-btn {
    margin-left: auto;
    background: #fff;
    border: 1px solid #FF9800;
    color: #FF9800;
    padding: 4px 12px;
    border-radius: 14px;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      background: #FF9800;
      color: #fff;
    }
  }
}

/* ========== 生日选择器高级感样式 ========== */
.birthday-picker-wrapper {
  display: flex !important;
  align-items: center;
  gap: 12px;
}

.birthday-picker-wrapper .static-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.birthday-picker-wrapper .icon-cake {
  color: #D4AF37;
  font-size: 18px;
}

:deep(.premium-date-picker.el-input) {
  width: 220px;
}

:deep(.premium-date-picker .el-input__wrapper) {
  background-color: #fdfaf3;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e0e0e0 inset;
  padding: 0 12px;
  height: 38px;
  transition: all 0.3s;
}

:deep(.premium-date-picker .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #C69C6D inset !important;
  background-color: #fff;
}

:deep(.premium-date-picker .el-input__inner) {
  color: #2C1810;
  font-weight: 500;
  font-family: 'Helvetica Neue', Helvetica, sans-serif;
}

:deep(.premium-date-picker .el-input__prefix-icon) {
  color: #C69C6D;
}

/* ========== 咖啡订单视图样式 ========== */
.coffee-order-view-wrapper {
  width: 100%;
}

/* ========== 按钮样式 ========== */
.nav-link {
  display: flex;
  align-items: center;
  position: relative;
  /* ... existing styles ... */
  gap: 10px; /* Add gap between icon and text */
}

.nav-icon {
  color: #8D6E63;
  transition: color 0.3s ease;
  flex-shrink: 0;
}
.nav-link.active .nav-icon {
  color: #5D4037;
}

.left-dot {
  width: 4px; 
  height: 4px; 
  background: #888; 
  border-radius: 50%; 
  margin-right: 12px; 
  transition: background 0.3s;
}
.nav-link.sub-link.active .left-dot {
  background: var(--primary-color, #C69C6D);
  transform: scale(1.2);
}

.footer-link {
  display: flex !important;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.go-order-btn,
.go-mall-btn {
  padding: 10px 24px;
  background: #d97706;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  margin-top: 16px;
  transition: background 0.2s;
}

.go-order-btn:hover,
.go-mall-btn:hover {
  background: #b45309;
}

/* ========== 可展开导航菜单样式 ========== */
.nav-group {
  width: 100%;
}

.nav-parent {
  display: flex;
  align-items: center;
  position: relative;
}

.expand-icon {
  margin-left: auto;
  font-size: 16px;
  transition: transform 0.2s;
  color: #888;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.nav-submenu {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
  padding-left: 16px;
}

.nav-submenu.expanded {
  max-height: 120px;
}

.nav-link.sub-link {
  padding: 10px 16px 10px 24px;
  font-size: 14px;
}

.nav-link.sub-link .nav-text {
  font-size: 14px;
}


/* ========== 空状态样式优化 ========== */
.no-data {
  text-align: center;
  padding: 60px 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px dashed #e5e7eb;
  margin-top: 20px;
}

.no-data p {
  color: #9ca3af;
  font-size: 15px;
  margin-bottom: 20px;
}
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.06);
  border-color: #e6e6e6;
}

.order-header {
  padding: 16px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-size: 13px;
  color: #6b7280;
  font-family: monospace;
}

.order-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.order-status.pending { background: #fff7ed; color: #d97706; }
.order-status.completed { background: #ecfdf5; color: #059669; }
.order-status.cancelled { background: #fef2f2; color: #dc2626; }

.header-right {
  display: flex;
  align-items: center;
}
.dining-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #4b5563;
  margin-right: 8px;
  font-weight: 500;
  border: 1px solid #e5e7eb;
}

.order-body {
  padding: 20px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.order-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #f5f5f5;
}

.order-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.items-summary,
.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

/* Detailed Order Items Styling */
.order-items-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
  width: 100%;
}
.order-line-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #eee;
}
.order-line-item:last-child {
  border-bottom: none;
}
.line-item-img {
  width: 40px; height: 40px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid #f0f0f0;
}
.line-item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.line-item-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.line-item-name .qty {
  color: #999;
  font-size: 12px;
  margin-left: 4px;
}
.line-item-specs {
  font-size: 12px;
  color: #888;
}
.line-item-price {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.order-meta-row {
  display: flex;
  gap: 16px;
  margin-top: 4px;
  align-items: center;
}
.order-meta-secondary {
  display: flex;
  justify-content: space-between;
  align-items: flex-end; /* Align bottom */
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f9f9f9;
}
.meta-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-right {
  display: flex;
  align-items: center;
}

.order-amount,
.points-cost {
  font-size: 15px;
  font-weight: 700;
  color: #d97706;
}

.pickup-code {
  display: inline-block;
  background: #fff7ed;
  color: #d97706;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: bold;
  margin-top: 4px;
  border: 1px dashed #fdba74;
}

/* ========== 弹窗样式优化 ========== */
.modal-overlay {
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
}

.modal-content {
  border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border: none;
  overflow: hidden;
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.modal-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  padding: 20px 24px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn {
  padding: 10px 20px;
  background: #fff;
  border: 1px solid #d1d5db;
  color: #374151;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.confirm-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #d97706, #b45309);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(217, 119, 6, 0.2);
  transition: all 0.2s;
}

.confirm-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 8px -1px rgba(217, 119, 6, 0.3);
}

/* 积分兑换弹窗新样式 */
.fulfillment-section {
  margin: 16px 0;
}
.fulfillment-section label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #374151;
}
.radio-group-modern {
  display: flex;
  gap: 12px;
}
.radio-card {
  flex: 1;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #f9fafb;
}
.radio-card.active {
  border-color: #d97706;
  background: #fffbeb;
  color: #d97706;
  font-weight: 500;
}
.radio-card:hover {
  border-color: #d1d5db;
}
.hidden-radio {
  display: none;
}
.radio-card .icon {
  font-size: 1.2rem;
}

/* 动态地址选择 */
.address-section .section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.text-btn {
  color: #d97706;
  font-size: 0.875rem;
  background: none;
  border: none;
  cursor: pointer;
}
.pickup-hint {
  background: #f3f4f6;
  padding: 12px;
  border-radius: 8px;
  margin: 12px 0;
  border-left: 3px solid #d97706;
}
.pickup-hint p {
  margin: 0;
  font-size: 0.9rem;
  color: #374151;
}
.pickup-hint .sub-text {
  font-size: 0.8rem;
  color: #6b7280;
  margin-top: 4px;
}

/* 费用汇总卡片 */
.redeem-summary-card {
  background: #fffbeb;
  padding: 16px;
  border-radius: 8px;
  margin-top: 20px;
}
.redeem-summary-card .row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 0.9rem;
  color: #4b5563;
}
.redeem-summary-card .divider {
  border-top: 1px dashed #d1d5db;
  margin: 8px 0;
}
.redeem-summary-card .row.total {
  font-weight: 600;
  font-size: 1.1rem;
  color: #d97706;
  margin-bottom: 0;
}
.balance-refer {
  text-align: right;
  font-size: 0.8rem;
  color: #9ca3af;
  margin-top: 4px;
}

/* 温馨提示 */
.redeem-warning-box {
  margin-top: 16px;
  font-size: 0.8rem;
  color: #ef4444;
  background: #fef2f2;
  padding: 8px 12px;
  border-radius: 6px;
}

.cancel-btn-small {
  background: #fff;
  border: 1px solid #d1d5db;
  color: #4b5563;
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: auto; /* Push to right if in flex container */
}
.cancel-btn-small:hover {
  background: #fee2e2;
  color: #ef4444;
  border-color: #fca5a5;
}

/* 优化兑换弹窗尺寸 */
.redeem-modal .modal-content {
  width: 650px !important;
  max-width: 95vw !important;
  max-height: 90vh !important;
  overflow-y: auto !important;
  padding: 30px !important;
}
@media (max-width: 768px) {
  .redeem-modal .modal-content {
    width: 95vw !important;
    padding: 20px !important;
  }
}

/* --------------- 积分兑换弹窗UI优化 --------------- */

/* 1. 美化收货地址下拉框 */
.modern-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background-color: #f9fafb;
  font-size: 0.95rem;
  color: #374151;
  outline: none;
  transition: all 0.2s;
  appearance: none; /* 移除默认箭头 */
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 16px;
  margin-top: 8px; /* 增加与标签的距离 */
}

.modern-select:focus {
  border-color: #d97706;
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.1);
}

/* 2. 优化底部按钮间距 */
.redeem-modal .modal-actions {
  display: flex !important;
  gap: 20px !important; /* 增加按钮之间的间距 */
  justify-content: center !important; /* 居中显示 */
  margin-top: 24px !important;
}

.redeem-modal .cancel-btn,
.redeem-modal .confirm-btn {
  min-width: 120px; /* 统一按钮宽度 */
}

/* 签到第7天特殊样式 */
.step-circle.is-seventh {
  border-color: #d97706 !important;
  background-color: #fffbeb !important;
  position: relative;
  overflow: visible !important; /* 允许提示溢出 */
}

.step.active .step-circle.is-seventh {
  background-color: #d97706 !important;
  color: white !important;
}

.step-circle.is-seventh .day-num {
  font-size: 1.2rem;
}

.coupon-tip {
  position: absolute;
  top: 105%;
  left: 50%;
  transform: translateX(-50%);
  background-color: #ef4444;
  color: white;
  font-size: 9px;
  padding: 2px 6px;
  border-radius: 10px;
  white-space: nowrap;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  z-index: 10;
  line-height: 1.2;
}

/* 小三角 */
.coupon-tip::before {
  content: '';
  position: absolute;
  top: -4px;
  left: 50%;
  transform: translateX(-50%);
  border-width: 0 4px 4px 4px;
  border-style: solid;
  border-color: transparent transparent #ef4444 transparent;
}

/* v5.0 任务卡片样式 */
.task-cards.v5-tasks {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.task-card.mini {
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: white;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.task-card.mini:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border-color: #e5e7eb;
}

.task-card.mini .task-icon {
  font-size: 24px;
  width: 44px;
  height: 44px;
  background: #fff8f0;
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.task-card.mini .task-content {
  flex: 1;
  min-width: 0; /* 防止溢出 */
}

.task-card.mini .task-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 2px;
}

.task-card.mini .task-desc {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-progress-mini {
  height: 6px;
  background: #f3f4f6;
  border-radius: 3px;
  margin-bottom: 4px;
  overflow: hidden;
}

.task-progress-mini .bar {
  height: 100%;
  background: linear-gradient(90deg, #d4af37, #f59e0b);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.task-card.mini .task-status {
  font-size: 10px;
  color: #9ca3af;
  text-align: right;
}

.task-card.mini .task-reward {
  position: absolute;
  top: 10px;
  right: 10px;
  color: #d97706;
  font-weight: 700;
  font-size: 12px;
  background: rgba(251, 191, 36, 0.1);
  padding: 2px 6px;
  border-radius: 6px;
}

/* 移动端适配 */
@media (max-width: 640px) {
  .task-cards.v5-tasks {
    grid-template-columns: 1fr;
  }
}

/* v5.0 月度任务图标优化样式 */
.task-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.task-svg-icon {
  width: 24px;
  height: 24px;
  stroke: white;
}

/* 渐变背景色 */
.gradient-orange {
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
}

.gradient-sunrise {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 50%, #f97316 100%);
}

.gradient-green {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.gradient-purple {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

/* 不同任务的进度条颜色 */
.task-progress-mini .bar.sunrise {
  background: linear-gradient(90deg, #fbbf24, #f97316);
}

.task-progress-mini .bar.green {
  background: linear-gradient(90deg, #10b981, #059669);
}

.task-progress-mini .bar.purple {
  background: linear-gradient(90deg, #8b5cf6, #7c3aed);
}

/* 任务卡片hover效果增强 */
.task-card.mini.task-check-in:hover .task-icon-wrapper {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(249, 115, 22, 0.3);
}

.task-card.mini.task-morning:hover .task-icon-wrapper {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(251, 191, 36, 0.3);
}

.task-card.mini.task-delivery:hover .task-icon-wrapper {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.3);
}

.task-card.mini.task-newproduct:hover .task-icon-wrapper {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(139, 92, 246, 0.3);
}

.task-icon-wrapper {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

/* v5.0 会员日徽章样式 */
.header-badges {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cozy-day-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
  animation: cozy-day-pulse 2s ease-in-out infinite;
}

.cozy-day-badge .badge-icon {
  font-size: 16px;
}

@keyframes cozy-day-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
}

/* 积分商城分类标签栏 - 复用 Coffee Payment 风格 */
.mall-category-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 28px;
  padding: 4px;
  background: rgba(245, 240, 235, 0.6);
  border-radius: 28px;
  width: fit-content;
}

.mall-category-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  border: none;
  font-size: 14px;
  color: #8D6E63;
  cursor: pointer;
  border-radius: 24px;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.mall-category-tab.active {
  background: #fff;
  color: #5D4037;
  box-shadow: 0 2px 8px rgba(166, 124, 82, 0.15);
  font-weight: 600;
}

.mall-category-tab:hover:not(.active) {
  background: rgba(255, 255, 255, 0.5);
}

/* 月度限购信息 */
.mall-card .limit-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
}

.mall-card .limit-tag {
  background: rgba(234, 182, 118, 0.15);
  color: #eab676;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid rgba(234, 182, 118, 0.3);
}

.mall-card .redeemed-text {
  color: #666;
}

/* 售罄或限购已满按钮变灰 */
.mall-card .redeem-btn:disabled {
  background: #ccc !important;
  color: #fff !important;
  cursor: not-allowed;
  transform: none !important;
}

.mall-category-tab .tab-icon-svg {
  flex-shrink: 0;
}
</style>

<style scoped>
/* ========================================= */
/*       PREMIUM WEB DESIGN (v5.0)          */
/* ========================================= */

/* ========================================= */
/*       5-TIER PREMIUM CARD SYSTEM         */
/* ========================================= */

.digital-card.premium-hero-card {
  position: relative;
  width: 100%;
  min-height: 240px;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px -10px rgba(0,0,0,0.3);
  transition: all 0.3s ease;
  font-family: 'Inter', sans-serif;
  color: #3E2723; /* Default text */
}

/* --- Layout Grid --- */
.hero-content-grid {
  position: relative;
  z-index: 10;
  height: 100%;
  padding: 32px;
  display: grid;
  grid-template-areas:
    "brand emblem"
    "points emblem"
    "footer emblem";
  grid-template-columns: 1fr auto;
  grid-template-rows: auto 1fr auto;
  gap: 16px;
}

/* --- Layers --- */
.card-layer {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  pointer-events: none;
  z-index: 1;
}

/* --- Brand --- */
.brand-area { grid-area: brand; display: flex; align-items: center; gap: 12px; }

/* Global Brand Logo Reset: No Circle, Minimalist Silhouette */
.logo-circle {
  width: auto; height: auto;
  background: none !important;
  border-radius: 0;
  box-shadow: none !important;
  padding: 0;
  display: flex; align-items: center; justify-content: center;
  color: inherit;
}
.start-logo { width: 28px; height: 28px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1)); }

/* Brand Typography: Wide Sans-Serif */
.brand-text { 
  font-family: 'Inter', sans-serif; /* Fallback to clean sans */
  font-size: 14px; 
  font-weight: 800; 
  letter-spacing: 3px; 
  text-transform: uppercase; 
  opacity: 1; 
}

/* --- Points --- */
.points-area { grid-area: points; display: flex; flex-direction: column; justify-content: center; }
.points-area .caption { font-size: 11px; letter-spacing: 2px; opacity: 0.7; margin-bottom: 4px; font-weight: 600; }
.points-area .points-val { font-family: 'Playfair Display', serif; font-size: 56px; line-height: 1; font-weight: 700; }

/* --- Footer --- */
.footer-area { grid-area: footer; display: flex; align-items: flex-end; gap: 16px; font-size: 12px; opacity: 0.8; font-family: monospace; }

/* --- Right Side Emblem --- */
.tier-emblem-area {
  grid-area: emblem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 100px;
}
.emblem-3d-wrapper {
  width: 80px; height: 80px;
  position: relative;
  display: flex; align-items: center; justify-content: center;
}
.emblem-shape {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  filter: drop-shadow(0 10px 20px rgba(0,0,0,0.3));
}
.emblem-shape svg { width: 64px; height: 64px; }
.tier-text { margin-top: 12px; font-weight: 700; font-size: 14px; letter-spacing: 1px; text-transform: uppercase; }

/* ========================================= */
/*              TIER STYLES                 */
/* ========================================= */

/* 1. Base Member (Cozy Cream) */
.premium-hero-card.base {
  background: #F5F0E6;
  color: #5D4037;
}
.premium-hero-card.base .card-layer.texture {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.1'/%3E%3C/svg%3E");
  opacity: 0.4;
}
.premium-hero-card.base .logo-circle { background: #5D4037; color: #F5F0E6; }
.premium-hero-card.base .emblem-shape { color: #8D6E63; }

/* 2. Silver Member */
.premium-hero-card.silver {
  background: linear-gradient(135deg, #E0E0E0 0%, #BDBDBD 100%);
  color: #424242;
}
.premium-hero-card.silver .card-layer.texture {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='1.5' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.08'/%3E%3C/svg%3E");
}
.premium-hero-card.silver .logo-circle { color: #616161; }
.premium-hero-card.silver .brand-text {
  background: linear-gradient(180deg, #757575 0%, #424242 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 0 rgba(255,255,255,0.4);
}

/* 3. Gold Member */
.premium-hero-card.gold {
  background: linear-gradient(135deg, #F3E5AB 0%, #D4AF37 100%);
  color: #4E342E;
}
.premium-hero-card.gold .card-layer.texture {
  background: repeating-linear-gradient(90deg, rgba(255,255,255,0.1) 0px, rgba(255,255,255,0.1) 1px, transparent 1px, transparent 3px);
  opacity: 0.3;
}
.premium-hero-card.gold .logo-circle { color: #795548; }
.premium-hero-card.gold .brand-text {
  background: linear-gradient(180deg, #8D6E63 0%, #4E342E 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 1px 0 rgba(255,255,255,0.3);
}

/* 4. Diamond Member (Laser Blue / Chrome) */
.premium-hero-card.diamond {
  background: linear-gradient(135deg, #CFD8DC 0%, #ECEFF1 50%, #B0BEC5 100%);
  color: #0D47A1;
  overflow: hidden;
}
.premium-hero-card.diamond .card-layer.holographic-overlay {
  background: linear-gradient(45deg, rgba(33,150,243,0.1), rgba(0,229,255,0.1), rgba(101,31,255,0.1));
  mix-blend-mode: color-dodge;
  animation: hologram 6s infinite linear;
}
@keyframes hologram { 0% { filter: hue-rotate(0deg); } 100% { filter: hue-rotate(360deg); } }

.premium-hero-card.diamond .logo-circle { color: #0277BD; }
.premium-hero-card.diamond .start-logo { filter: drop-shadow(0 0 5px rgba(2, 119, 189, 0.4)); }
.premium-hero-card.diamond .brand-text {
  background: linear-gradient(135deg, #0288D1 0%, #00B0FF 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 900;
  text-shadow: 0 2px 8px rgba(0, 176, 255, 0.3);
}

/* 5. Black Gold (Hot Stamping Gold) */
.premium-hero-card.black {
  background: #121212;
  color: #FFD700;
}
.premium-hero-card.black .card-layer.texture {
   background-image: url("data:image/svg+xml,%3Csvg width='20' height='20' viewBox='0 0 20 20' xmlns='http://www.w3.org/2000/svg'%3E%3Ccircle cx='2' cy='2' r='1' fill='%23FFFFFF' fill-opacity='0.1'/%3E%3C/svg%3E");
   background-size: 8px 8px;
   opacity: 0.2;
}
.premium-hero-card.black .logo-circle { 
  color: #D4AF37; 
  /* Gold Foil Icon Effect */
  filter: drop-shadow(0 1px 0 rgba(0,0,0,0.8)) drop-shadow(0 0 10px rgba(255, 215, 0, 0.3));
}
.premium-hero-card.black .brand-text {
  /* Hot Stamping Gradient */
  background: linear-gradient(135deg, #FDE08D 0%, #D4AF37 50%, #FDE08D 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 -1px 0 rgba(0,0,0,1); /* Embossed indent */
  font-weight: 800;
}
.premium-hero-card.black .brand-text, 
.premium-hero-card.black .points-val,
.premium-hero-card.black .tier-text {
  background: linear-gradient(135deg, #FFD700 0%, #FDB931 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}
.premium-hero-card.black .emblem-shape.crown {
  color: #FFD700;
  filter: drop-shadow(0 0 20px rgba(255, 215, 0, 0.5));
}
.premium-hero-card.black .labels, .premium-hero-card.black .caption, .premium-hero-card.black .member-code {
  color: rgba(255, 215, 0, 0.6);
}

/* Stats Premium Bar */
.stats-premium-bar {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto 1fr;
  align-items: center;
  background: #fff;
  padding: 24px;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03);
  margin: 24px 0;
  border: 1px solid rgba(0,0,0,0.03);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: center;
  position: relative;
}

.stat-icon-bg {
  width: 48px; height: 48px;
  border-radius: 12px;
  background: #F9F9F9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8D6E63;
  font-size: 20px;
}

.stat-text {
  display: flex;
  flex-direction: column;
}
.stat-text .val {
  font-size: 20px;
  font-weight: 700;
  color: #3E2723;
}
.stat-text .lbl {
  font-size: 12px;
  color: #9E9E9E;
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: #EEEEEE;
}

/* Signin Premium Widget */
.signin-premium-widget {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.04);
  border: 1px solid rgba(0,0,0,0.02);
  margin-bottom: 24px;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}
.widget-header h4 {
  font-size: 18px;
  font-weight: 700;
  color: #3E2723;
  margin-bottom: 4px;
}
.widget-header .sub {
  font-size: 13px;
  color: #8D6E63;
}

.signin-action-btn {
  background: #3E2723;
  color: #FBEEA8;
  border: none;
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.signin-action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(62, 39, 35, 0.3);
}
.signin-action-btn:disabled {
  background: #E0E0E0;
  color: #9E9E9E;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.bean-track-wrapper {
  position: relative;
  padding: 20px 0;
}
/* Track Line Calculation:
   Nodes are 40px wide. First node center is at 20px.
   Line should start from the center of the first node to the center of the last node.
   Track Width = 100% - 40px (one node width). Left = 20px.
*/
.track-line-base {
  position: absolute;
  top: 36px;
  left: 20px; right: 20px; /* Start/End at center of first/last node */
  height: 4px;
  background: #F5F5F5;
  border-radius: 2px;
  transform: translateY(-50%);
  z-index: 1;
}
.track-line-fill {
  height: 100%;
  background: #C69C6D;
  border-radius: 2px;
  transition: width 0.5s ease;
}

.bean-steps {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.bean-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 40px;
}

.bean-icon-box {
  width: 32px; height: 32px;
  background: #EFEBE9;
  border-radius: 50%;
  border: 2px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #BCAAA4;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.bean-step.is-active .bean-icon-box {
  background: #5D4037;
  color: #FBEEA8;
  transform: scale(1.1);
}
.bean-step.is-today .bean-icon-box {
  box-shadow: 0 0 0 4px rgba(198, 156, 109, 0.3);
}

.bean-svg {
  width: 18px; height: 18px;
}

.bean-icon-box.is-gift {
  width: 48px; height: 48px;
  border-radius: 16px;
  /* Use Bean-Theme Colors (Coffee Gradient) instead of Red */
  background: linear-gradient(135deg, #8D6E63 0%, #5D4037 100%);
  color: #FBEEA8;
  margin-top: -8px; /* Lift up to align center with others */
  display: flex; align-items: center; justify-content: center;
}
.gift-svg { width: 24px; height: 24px; }

.step-label {
  font-size: 11px;
  color: #9E9E9E;
  font-weight: 500;
}
.bean-step.is-active .step-label {
  color: #5D4037;
  font-weight: 700;
}

/* Layout Split */
/* Premium Layout & Theme */
.member-layout {
  display: flex;
  min-height: 100vh;
  background-color: #F9F5F0; /* Warm Beige */
  color: #333;
}

.dashboard-view {
  animation: fadeIn 0.4s ease-out;
  max-width: 1400px;
  margin: 0 auto;
}

.dashboard-split-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 24px;
  margin-top: 24px;
  align-items: stretch; /* Ensure equal height */
}

.layout-col-left, .layout-col-right {
  display: flex;
  flex-direction: column;
  height: 100%; /* Fill the grid area */
}

.points-guide-section,
.task-center-section {
  background: #fff;
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03); /* Unified Soft Shadow */
  height: 100%; /* Fill height */
  display: flex;
  flex-direction: column;
}

/* Left Column Promo Banner */
.promo-banner {
  margin-top: auto; /* Push to bottom */
  padding-top: 24px;
  width: 100%;
}
.promo-banner img {
  width: 100%;
  border-radius: 16px;
  display: block;
  object-fit: cover;
  transition: transform 0.3s ease;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.promo-banner img:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.1);
}

/* Done Check Icon */
.status-check {
  color: #D97706; /* Gold */
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #FEF3C7; /* Soft Gold bg */
  border-radius: 50%;
}

/* Champagne Gold Points Booster */
.black-accelerate-box {
  background: linear-gradient(135deg, #FDE68A 0%, #D97706 100%);
  border-radius: 16px;
  padding: 20px;
  color: #451a03;
  margin-bottom: 24px;
  box-shadow: 0 8px 16px rgba(217, 119, 6, 0.2);
}
.black-accelerate-box .box-title { color: #451a03; }
.black-accelerate-box .box-status { color: #451a03; opacity: 0.9; }
.progress-bar-bg { background: rgba(255,255,255,0.3); }
.progress-fill { background: #fff; }

/* Clean Headers */
/* Clean Headers */
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.section-title h4 {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: -0.5px;
  margin: 0;
}
.header-right-action {
  display: flex;
  align-items: center;
  gap: 12px;
}
.section-title .subtitle {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.link-btn-small {
  background: none;
  border: none;
  font-size: 12px;
  color: #8D6E63;
  cursor: pointer;
  padding: 0;
  font-weight: 600;
}
.link-btn-small:hover { text-decoration: underline; color: #5D4037; }

@media (max-width: 1024px) {
  .dashboard-split-layout {
    grid-template-columns: 1fr;
  }
}

/* Premium Task List (v5.0) */
.channel-card {
  display: flex;
  align-items: center;
  justify-content: space-between; /* Space out items */
  gap: 16px;
  padding: 16px; 
  border: 1px solid #F5F5F5;
  border-radius: 16px;
  background: #FAFAFA; 
  transition: all 0.2s;
}
.channel-info {
  flex: 1; /* Take up available space */
  display: flex;
  flex-direction: column;
}
.channel-points.simple-row span {
  font-size: 14px;
  color: #C69C6D;
  font-weight: 400; /* Regular weight as requested */
  white-space: nowrap;
}
.channel-points.column-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.channel-points .points-label {
  font-size: 10px;
  color: #C69C6D;
}
.channel-card:hover {
  background: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
  transform: translateY(-2px);
  border-color: #EFEBE9;
}
.go-btn {
  background: none;
  border: none;
  font-size: 13px;
  font-weight: 600;
  color: #8D6E63;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
  white-space: nowrap;
}
.go-btn:hover {
  background: #EFEBE9;
  color: #5D4037;
}

.task-list-premium {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-item-row {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #F6F6F6;
  transition: all 0.2s;
}
/* Matte Black Points Booster */
.black-accelerate-box {
  background: linear-gradient(135deg, #1a1a1a 0%, #2c2c2c 100%); /* Matte Black */
  border-radius: 16px;
  padding: 20px;
  color: #F7E7CE; /* Luxury Champagne Gold Text - High Contrast */
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}
.black-accelerate-box .box-status strong { color: #fff; } /* Value in White */
.black-accelerate-box .title-text { color: #F7E7CE; font-weight: 700; } 
.progress-bar-bg { background: rgba(255,255,255,0.15); }
.progress-fill { background: linear-gradient(90deg, #FDE68A 0%, #D97706 100%); /* Gold Bar */ }

.task-item-row:hover {
  border-color: #E0E0E0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
}

/* v5.0: 任务完成状态样式 */
.task-item-row.task-completed {
  background: linear-gradient(135deg, #E8F5E9 0%, #C8E6C9 100%);
  border-color: #81C784;
}
.task-item-row.task-completed .task-progress-bar .fill {
  background: linear-gradient(90deg, #66BB6A 0%, #43A047 100%) !important;
}
.task-item-row.task-completed .reward.claimed {
  color: #43A047;
  font-weight: 600;
}
.task-item-row.task-completed .status-text.completed {
  color: #2E7D32;
  font-weight: 600;
}

.task-icon-bg {
  width: 40px; height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  color: #5D4037; /* Dark Brown Icon */
  background: #FAFAFA; /* Transparent/Lightest Beige */
  border: 1px solid #EFEBE9;
}

/* 3D Small Gift Icon - Smaller */
.gift-box-icon-sm {
  width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center;
  color: #D4AF37;
  filter: drop-shadow(0 2px 4px rgba(212, 175, 55, 0.3));
}
.gift-svg-sm { width: 100%; height: 100%; fill: url(#gold-gradient); }

.task-main {
  flex: 1;
}

.task-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.task-top .name {
  font-weight: 600;
  font-size: 14px;
  color: #3E2723;
}
.task-top .reward {
  font-size: 12px;
  font-weight: 700;
  color: #C69C6D;
}

.task-desc {
  font-size: 12px;
  color: #9E9E9E;
  margin-bottom: 6px;
}

.task-progress-bar {
  height: 4px;
  background: #F5F5F5;
  border-radius: 2px;
  width: 100%;
}
.task-progress-bar .fill {
  height: 100%;
  border-radius: 2px;
}
.fill.orange { background: linear-gradient(90deg, #FFCC80 0%, #EF6C00 100%); }
.fill.yellow { background: linear-gradient(90deg, #FFF59D 0%, #FBC02D 100%); }
.fill.green { background: linear-gradient(90deg, #A5D6A7 0%, #43A047 100%); }
.fill.purple { background: linear-gradient(90deg, #CE93D8 0%, #8E24AA 100%); }

.task-action .status-text {
  font-size: 12px;
  font-weight: 600;
  color: #3E2723;
  background: #F5F5F5;
  padding: 4px 10px;
  border-radius: 12px;
}

.detail-btn-simple {
  width: 100%;
  padding: 12px;
  border: 1px dashed #D7CCCC;
  background: transparent;
  color: #8D6E63;
  border-radius: 12px;
  cursor: pointer;
  margin-top: 24px;
  transition: all 0.2s;
}
.detail-btn-simple:hover {
  background: #FDFBF9;
  border-color: #C69C6D;
  color: #5D4037;
}

.view-benefits-text-btn {
  background: none;
  border: none;
  color: #C69C6D;
  font-weight: 600;
  cursor: pointer;
  font-size: 12px;
}
/* Ticket Coupon UI */
.coupon-list-ticket {
  display: flex; flex-direction: column; gap: 16px; width: 100%;
}
.ticket-card {
  display: flex;
  height: 110px;
  filter: drop-shadow(0 4px 12px rgba(0,0,0,0.05));
  transition: all 0.2s;
}
.ticket-card:hover { transform: translateY(-2px); filter: drop-shadow(0 8px 16px rgba(0,0,0,0.08)); }
.ticket-card.used, .ticket-card.expired { filter: grayscale(1); opacity: 0.6; pointer-events: none; }

/* Left Section */
.ticket-left {
  width: 32%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  position: relative;
  border-radius: 12px 0 0 12px;
  
  /* Punch Holes on Right Edge */
  mask-image: radial-gradient(circle at 100% 0, transparent 8px, black 9px), 
              radial-gradient(circle at 100% 100%, transparent 8px, black 9px);
  mask-size: 100% 51%;
  mask-position: 0 0, 0 100%;
  mask-repeat: no-repeat;
  -webkit-mask-image: radial-gradient(circle at 100% 0, transparent 8px, black 9px), 
              radial-gradient(circle at 100% 100%, transparent 8px, black 9px);
  -webkit-mask-size: 100% 51%;
  -webkit-mask-position: 0 0, 0 100%;
  -webkit-mask-repeat: no-repeat;
}

/* Backgrounds */
.ticket-left { background: #8D6E63; } /* Default Brown */
.ticket-left.free, .ticket-left.exchange { background: linear-gradient(135deg, #FDE68A 0%, #D97706 100%); color: #4E342E; }

/* 配送费抵扣券专属样式 - 蓝色系 */
.ticket-left.delivery-badge,
.ticket-left.delivery { 
  background: linear-gradient(135deg, #42A5F5 0%, #1976D2 100%); 
  background: linear-gradient(135deg, #42A5F5 0%, #1976D2 100%); 
  color: #fff; 
}

/* 附加券样式 (浓缩/糖浆) - 紫蓝渐变 */
.ticket-left.addon,
.visual-area.type-addon,
.type-addon {
  background: linear-gradient(135deg, #AB47BC 0%, #7B1FA2 100%);
  color: #fff;
}

.ticket-left .delivery-icon {
  font-size: 28px;
  margin-bottom: 4px;
}

.ticket-left .val-text-small {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
}

/* 附加券标识 */
.ticket-left .addon-badge {
  position: absolute;
  top: 6px;
  right: -8px;
  background: #FF6B6B;
  color: #fff;
  font-size: 8px;
  padding: 2px 6px;
  border-radius: 0 4px 4px 0;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 4px rgba(255, 107, 107, 0.3);
}

.ticket-val-group {
  display: flex; align-items: baseline; font-family: 'Didot', 'Times New Roman', serif; line-height: 1;
}
.val-num { font-size: 36px; font-weight: 700; }
.val-unit { font-size: 14px; margin-right: 2px; font-weight: 500; }
.val-text { font-size: 24px; font-weight: 700; letter-spacing: 1px; }

.ticket-condition { font-size: 10px; margin-top: 6px; opacity: 0.9; letter-spacing: 0.5px; text-transform: uppercase; }

/* Right Section */
.ticket-right {
  flex: 1;
  background: #fff;
  border-radius: 0 12px 12px 0;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  position: relative;
  
  /* Punch Holes on Left Edge */
  mask-image: radial-gradient(circle at 0 0, transparent 8px, black 9px), 
              radial-gradient(circle at 0 100%, transparent 8px, black 9px);
  mask-size: 100% 51%;
  mask-position: 0 0, 0 100%;
  mask-repeat: no-repeat;
  -webkit-mask-image: radial-gradient(circle at 0 0, transparent 8px, black 9px), 
              radial-gradient(circle at 0 100%, transparent 8px, black 9px);
  -webkit-mask-size: 100% 51%;
  -webkit-mask-position: 0 0, 0 100%;
  -webkit-mask-repeat: no-repeat;
}

/* Dashed Divider Line */
.ticket-right::before {
  content: ''; position: absolute; left: -1px; top: 10px; bottom: 10px;
  border-left: 2px dashed #E0E0E0; z-index: 2;
}

.ticket-info-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 4px; }
.ticket-title { font-size: 15px; font-weight: 700; color: #3E2723; }

.urgent-badge {
  font-size: 10px; color: #D84315; background: #FFCCBC;
  padding: 2px 6px; border-radius: 4px; font-weight: 700;
}

.ticket-info-desc { font-size: 12px; color: #9E9E9E; flex: 1; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.ticket-info-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.ticket-expiry { font-size: 11px; color: #BDBDBD; font-family: monospace; }

.use-btn-ghost {
  border: 1px solid #8D6E63; color: #8D6E63; background: transparent;
  padding: 4px 16px; border-radius: 20px; font-size: 12px; cursor: pointer; font-weight: 600;
  transition: all 0.2s;
}
.use-btn-ghost:hover { background: #8D6E63; color: #fff; }

.stamp-mark {
  border: 3px double #BDBDBD; color: #BDBDBD; padding: 2px 8px; font-weight: 900;
  font-size: 12px; transform: rotate(-10deg); border-radius: 4px; opacity: 0.6;
}

/* 3D Current Level Badge - Removed */
.benefit-card.current {
  transform: scale(1.02);
}
.validity-info {
  font-size: 10px; opacity: 0.7; margin-top: 4px; letter-spacing: 0.5px;
}
.highlight-gold-bold {
  color: #D4AF37; font-weight: 900; font-size: 1.1em; text-shadow: 0 1px 0 rgba(0,0,0,0.1);
  margin-left: 2px;
}
.b-icon { margin-right: 6px; font-size: 14px; vertical-align: middle; }
</style>

<style scoped>
/* --- New Tabbed Privilege UI --- */
.member-privileges-section {
  margin-top: 20px;
}
.level-tabs {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 0;
}
.level-tab-btn {
  background: transparent;
  border: none;
  padding: 12px 24px;
  cursor: pointer;
  color: #9E9E9E;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px; 
  transition: all 0.3s;
}
.level-tab-btn:hover { color: #666; }
.level-tab-btn.active { color: #333; border-bottom-color: #333; }

/* Active Tab Colors */
.level-tab-btn.active.silver { color: #757575; border-bottom-color: #BDBDBD; }
.level-tab-btn.active.gold { color: #D4AF37; border-bottom-color: #D4AF37; }
.level-tab-btn.active.diamond { color: #1565C0; border-bottom-color: #1565C0; }
.level-tab-btn.active.black { color: #212121; border-bottom-color: #000; }

/* Feature Card Layout */
.level-feature-view {
  animation: fadeIn 0.4s ease;
}
.feature-card {
  display: flex;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
  min-height: 380px;
  transition: all 0.3s ease;
}

/* Black Gold Theme */
.feature-card.black-gold-theme {
  background: #000;
  color: #FFD700;
  box-shadow: 0 30px 60px rgba(0,0,0,0.4);
}
.black-gold-theme .card-visual-side {
  width: 45%;
  background: radial-gradient(circle at center, #2c2c2c 0%, #000 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}
.black-gold-theme .ambient-glow {
  position: absolute;
  width: 200px; height: 200px;
  background: rgba(255, 215, 0, 0.15);
  filter: blur(60px);
  border-radius: 50%;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
}

/* 3D Physical Card */
.physical-card-3d {
  width: 280px; height: 176px;
  background: linear-gradient(135deg, #1a1a1a, #000);
  border-radius: 12px;
  border: 1px solid rgba(255,215,0,0.3);
  position: relative;
  transform: perspective(1000px) rotateY(-15deg) rotateX(8deg);
  box-shadow: -15px 15px 30px rgba(0,0,0,0.5), inset 0 0 20px rgba(0,0,0,0.8);
  transition: transform 0.5s ease;
}
/* Hover on the entire card triggers the 3D effect */
.feature-card:hover .physical-card-3d {
  transform: perspective(1000px) rotateY(-5deg) rotateX(2deg) scale(1.05);
}
.card-face {
  padding: 20px; height: 100%;
  display: flex; flex-direction: column; justify-content: space-between;
  background-image: linear-gradient(45deg, rgba(255,255,255,0.05) 25%, transparent 25%, transparent 50%, rgba(255,255,255,0.05) 50%, rgba(255,255,255,0.05) 75%, transparent 75%, transparent);
  background-size: 20px 20px;
}
.card-chip {
  width: 40px; height: 28px;
  background: linear-gradient(135deg, #FFD700, #B8860B);
  border-radius: 4px;
  box-shadow: inset 1px 1px 2px rgba(0,0,0,0.3);
}
.card-logo {
  font-family: serif;
  font-weight: 900;
  letter-spacing: 2px;
  font-size: 18px;
  color: #FFD700;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
  margin-left: auto; margin-top: -30px;
}
.card-member-name {
  font-size: 10px; color: rgba(255,255,255,0.5); letter-spacing: 1px;
}
.card-number {
  font-family: monospace; letter-spacing: 2px; color: #fff; opacity: 0.9; text-shadow: 0 1px 2px #000;
}

/* Content Side */
.black-gold-theme .card-content-side {
  flex: 1;
  padding: 40px;
  display: flex; flex-direction: column; justify-content: center;
  background: linear-gradient(to right, #080808, #111);
}
.feature-header {
  font-family: serif;
  font-size: 28px;
  color: #fff;
  margin-bottom: 30px;
  line-height: 1.2;
}
.feature-header span {
  font-size: 16px; color: #D4AF37; display: block; margin-top: 8px; font-family: sans-serif; opacity: 0.9;
}
.feature-list {
  display: grid; grid-template-columns: 1fr 1fr; gap: 20px;
  margin-bottom: 40px;
}
.f-item {
  display: flex; align-items: flex-start;
}
.f-icon { font-size: 24px; margin-right: 12px; filter: drop-shadow(0 0 5px rgba(255,215,0,0.3)); }
.f-item div strong { color: #fff; font-size: 14px; display: block; margin-bottom: 2px; }
.f-item div p { color: #666; font-size: 12px; margin: 0; }

.cta-btn {
  align-self: flex-start;
  padding: 12px 32px;
  background: linear-gradient(135deg, #FFD700, #B8860B);
  border: none;
  font-weight: 700;
  color: #000;
  border-radius: 30px;
  cursor: pointer;
  box-shadow: 0 5px 15px rgba(184, 134, 11, 0.3);
  transition: all 0.3s;
}
.cta-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(184, 134, 11, 0.5); }

/* v5.7: 会员权益领取按钮 - 根据等级显示不同颜色 */
.benefit-action-area {
  margin-top: 32px;
}

.receive-btn {
  width: 100%;
  padding: 14px 32px;
  background: linear-gradient(135deg, #D4AF37 0%, #B8860B 100%);
  border: none;
  color: #000;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 2px;
  border-radius: 30px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3);
}

.receive-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(212, 175, 55, 0.5);
}

.receive-btn:disabled {
  background: #E0E0E0 !important;
  color: #9E9E9E !important;
  cursor: not-allowed;
  box-shadow: none !important;
  transform: none !important;
}

/* 黑金等级 - 金色系 */
.receive-btn.black {
  background: linear-gradient(135deg, #D4AF37 0%, #B8860B 100%);
  color: #000;
  box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3);
}
.receive-btn.black:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(212, 175, 55, 0.5);
}

/* 钻石等级 - 蓝宝石色 */
.receive-btn.diamond {
  background: linear-gradient(135deg, #42A5F5 0%, #1565C0 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(66, 165, 245, 0.3);
}
.receive-btn.diamond:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(66, 165, 245, 0.5);
}

/* 黄金等级 - 金黄色 */
.receive-btn.gold {
  background: linear-gradient(135deg, #FFC107 0%, #FFA000 100%);
  color: #000;
  box-shadow: 0 4px 12px rgba(255, 193, 7, 0.3);
}
.receive-btn.gold:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(255, 193, 7, 0.5);
}

/* 白银等级 - 银灰色 */
.receive-btn.silver {
  background: linear-gradient(135deg, #9E9E9E 0%, #616161 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(158, 158, 158, 0.3);
}
.receive-btn.silver:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(158, 158, 158, 0.5);
}

/* 基础等级 - 咖啡棕色 */
.receive-btn.basic {
  background: linear-gradient(135deg, #8D6E63 0%, #5D4037 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(141, 110, 99, 0.3);
}
.receive-btn.basic:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(141, 110, 99, 0.5);
}

/* Generic Theme (Fallback) */
.generic-theme { border: 1px solid #eee; background: #fff; }
.generic-theme .card-visual-side {
  width: 40%;
  display: flex; justify-content: center; align-items: center;
  background: #fafafa;
}
.physical-card-flat {
  width: 240px; height: 150px;
  border-radius: 12px;
  display: flex; justify-content: center; align-items: center;
  color: #fff; font-weight: 800; font-size: 18px;
  box-shadow: 0 10px 20px rgba(0,0,0,0.1);
}
.physical-card-flat.basic { background: #8D6E63; }
.physical-card-flat.silver { background: linear-gradient(135deg, #E0E0E0, #9E9E9E); }
.physical-card-flat.gold { background: linear-gradient(135deg, #FFD700, #FFA000); }
.physical-card-flat.diamond { background: linear-gradient(135deg, #42A5F5, #1565C0); }
.generic-theme .card-content-side {
  flex: 1; padding: 40px;
}
.generic-benefit-list { list-style: none; padding: 0; }
.generic-benefit-list li { margin-bottom: 12px; display: flex; align-items: center; color: #555; }
.check-icon { color: #4CAF50; margin-right: 10px; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

/* --- Premium Card Styles (New) --- */

/* Classic (Kraft Paper) */
.physical-card-3d.basic {
  background: #A1887F;
  background-image: repeating-linear-gradient(45deg, rgba(0,0,0,0.03) 0px, transparent 1px, transparent 3px);
  border: 1px solid rgba(0,0,0,0.1);
}
.physical-card-3d.basic .card-face { background-image: none; }
.physical-card-3d.basic .card-chip { background: linear-gradient(135deg, #Bcaaa4, #8d6e63); }
.physical-card-3d.basic { color: #5D4037; }
.physical-card-3d.basic .card-logo, .physical-card-3d.basic .card-number { color: #3E2723; text-shadow: 0 1px 0 rgba(255,255,255,0.2); }
.physical-card-3d.basic .card-member-name { color: rgba(62, 39, 35, 0.6); }

/* Silver (Brushed Metal) */
.physical-card-3d.silver {
  background: linear-gradient(135deg, #E0E0E0 0%, #BDBDBD 40%, #FFFFFF 50%, #BDBDBD 60%, #9E9E9E 100%);
  box-shadow: -15px 15px 30px rgba(0,0,0,0.2);
}
.physical-card-3d.silver .card-face { background-image: none; }
.physical-card-3d.silver .card-chip { background: linear-gradient(135deg, #cfd8dc, #90a4ae); }
.physical-card-3d.silver .card-logo, .physical-card-3d.silver .card-number { color: #424242; text-shadow: 0 1px 1px rgba(255,255,255,0.8); }
.physical-card-3d.silver .card-member-name { color: #616161; }

/* Gold (Silk/Champagne - Fluid Texture) */
.physical-card-3d.gold {
  background: linear-gradient(135deg, #FFECB3 0%, #FFC107 40%, #FF8F00 100%);
  position: relative;
  overflow: hidden;
}
.physical-card-3d.gold::before {
  content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
  background: 
    conic-gradient(from 20deg at 50% 50%, transparent 0deg, rgba(255, 255, 255, 0.3) 25deg, transparent 50deg, rgba(139, 69, 19, 0.1) 70deg, transparent 100deg),
    radial-gradient(ellipse at 70% 30%, rgba(255, 255, 255, 0.5) 0%, transparent 60%);
  filter: blur(25px); /* Soften into silk folds */
  transform: rotate(-10deg) scale(1.2);
  mix-blend-mode: overlay;
  opacity: 0.9;
}
.physical-card-3d.gold .card-face {
  /* Subtle inner glow */
  box-shadow: inset 0 0 30px rgba(255, 215, 0, 0.3);
  z-index: 2; /* Content above texture */
  position: relative;
}
.physical-card-3d.gold .card-logo, .physical-card-3d.gold .card-number { 
  color: #795548; /* Warm Coffee Brown for contrast */
  text-shadow: 0 1px 1px rgba(255,255,255,0.5); 
}
.physical-card-3d.gold .card-member-name { color: #8D6E63; }
.physical-card-3d.gold .card-brand-mark { color: #BF360C; opacity: 0.8; }

/* Diamond (Jewel Blue / Crystal - Low Poly Facets) */
.physical-card-3d.diamond {
  background: linear-gradient(135deg, #01579B 0%, #0288D1 50%, #29B6F6 100%);
  border: 1px solid rgba(255,255,255,0.5);
  position: relative;
  overflow: hidden;
}
.physical-card-3d.diamond::before {
  content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
  background: 
    conic-gradient(from 45deg at 50% 50%, rgba(255,255,255,0.05) 0deg, transparent 30deg, rgba(255,255,255,0.1) 60deg, transparent 90deg, rgba(255,255,255,0.2) 120deg, transparent 150deg, rgba(255,255,255,0.05) 180deg),
    linear-gradient(60deg, transparent 40%, rgba(255,255,255,0.15) 40%, transparent 41%),
    linear-gradient(-60deg, transparent 60%, rgba(255,255,255,0.15) 60%, transparent 61%);
  transform: rotate(0deg) scale(1.1);
  mix-blend-mode: soft-light;
  opacity: 1;
}
.physical-card-3d.diamond .card-face {
  z-index: 2; position: relative;
  /* Glass gloss */
  background: linear-gradient(135deg, rgba(255,255,255,0.1), transparent 40%, rgba(255,255,255,0.05));
}
.physical-card-3d.diamond .card-brand-mark { color: #E1F5FE; filter: drop-shadow(0 0 8px rgba(255,255,255,0.8)); opacity: 1; }
.physical-card-3d.diamond .card-logo, .physical-card-3d.diamond .card-number { 
  color: #E1F5FE; 
  text-shadow: 0 0 10px rgba(255,255,255,0.5); 
}
.physical-card-3d.diamond .card-member-name { color: #81D4FA; }

/* Deprecated Chip - Replaced by Brand Mark */
.card-chip { display: none; } 
.card-brand-mark {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  color: #D4AF37; /* Default Gold */
  opacity: 0.9;
}
.card-brand-mark.basic { color: #3E2723; opacity: 0.5; }
.card-brand-mark.silver { color: #fff; text-shadow: 0 1px 2px rgba(0,0,0,0.3); }
.card-brand-mark.gold { color: #BF360C; }
.card-brand-mark.diamond { color: #fff; }

/* --- Dynamic Backgrounds for Level Cards --- */

/* Basic (Latte) */
.feature-card.basic .card-visual-side { background: #EFEBE9; } /* Light Latte */
.feature-card.basic .card-content-side { background: #FAF8F6; }

/* Silver (Cool Grey) */
.feature-card.silver .card-visual-side { background: #ECEFF1; } /* Blue Grey Light */
.feature-card.silver .card-content-side { background: #F9FAFB; }

/* Gold (Warm Ivory) */
.feature-card.gold .card-visual-side { background: #FFF8E1; } /* Amber Light */
.feature-card.gold .card-content-side { background: #FFFCF2; }

/* Diamond (Ice Blue) */
.feature-card.diamond .card-visual-side { background: #E3F2FD; } /* Light Blue */
.feature-card.diamond .card-content-side { background: #F1F8FF; }

/* Visual transition for backgrounds */
.card-visual-side, .card-content-side { transition: background 0.3s ease; }

/* --- Optimized Text Contrast for Generic Cards --- */
/* Basic */
.feature-card.basic .feature-header { color: #5D4037; }
.feature-card.basic .feature-header span { color: #8D6E63; }
.feature-card.basic .generic-benefit-list li { color: #5D4037; }

/* Silver */
.feature-card.silver .feature-header { color: #37474F; }
.feature-card.silver .feature-header span { color: #607D8B; }
.feature-card.silver .generic-benefit-list li { color: #455A64; }

/* Gold */
.feature-card.gold .feature-header { color: #795548; }
.feature-card.gold .feature-header span { color: #A1887F; }
.feature-card.gold .generic-benefit-list li { color: #5D4037; }

/* Diamond */
.feature-card.diamond .feature-header { color: #01579B; }
.feature-card.diamond .feature-header span { color: #0288D1; }
.feature-card.diamond .generic-benefit-list li { color: #0277BD; }

/* --- Member Progress Footer (Refined SaaS Style - Static) --- */
.member-progress-footer {
  position: relative; /* Changed from fixed */
  width: 100%;
  max-width: none;
  margin-top: 30px;
  background: #FFFFFF;
  border-radius: 16px;
  padding: 30px 40px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.03); 
  border: 1px solid rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
}

/* Removed slideUp animation since it is static now */

.progress-info {
  display: flex; justify-content: space-between; align-items: center;
  width: 100%;
  margin-bottom: 20px; /* Space before bar */
}

/* Left: Status */
.current-status { display: flex; align-items: center; gap: 12px; }
.status-text { font-size: 18px; font-weight: 800; color: #333; }
.status-icon { font-size: 24px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1)); }

/* Right: Numbers */
.progress-numbers { display: flex; align-items: baseline; gap: 8px; }
.current-exp { font-size: 32px; font-weight: 800; color: #222; font-family: 'Inter', sans-serif; letter-spacing: -1px; }
.total-exp { font-size: 15px; color: #999; font-weight: 500; }
.progress-numbers .m-icon { margin-left: 8px; color: #FFD700; transform: translateY(2px); }

/* Middle: Bar */
.progress-bar-container { position: relative; height: 16px; width: 100%; }
.progress-track {
  width: 100%; height: 100%;
  background: #F5F5F5;
  border-radius: 100px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  /* Default handled by themes below, but base fallback */
  background: #E0E0E0;
  border-radius: 100px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  transition: width 1s ease-out, background 0.3s ease;
}

/* Dynamic Level Progress Gradients */
.progress-fill.basic { background: linear-gradient(90deg, #D7CCC8, #A1887F); box-shadow: 0 4px 10px rgba(141, 110, 99, 0.3); }
.progress-fill.silver { background: linear-gradient(90deg, #ECEFF1, #B0BEC5); box-shadow: 0 4px 10px rgba(176, 190, 197, 0.3); }
.progress-fill.gold { background: linear-gradient(90deg, #FFF176, #FFB300); box-shadow: 0 4px 10px rgba(255, 179, 0, 0.3); }
.progress-fill.diamond { background: linear-gradient(90deg, #64B5F6, #1976D2); box-shadow: 0 4px 10px rgba(25, 118, 210, 0.3); }
.progress-fill.black { background: linear-gradient(90deg, #757575, #212121); box-shadow: 0 4px 10px rgba(33, 33, 33, 0.4); }

/* Hide milestones for clean look */
.progress-milestones { display: none; }

/* Bottom: Motivation */
.progress-motivation {
  margin-top: 16px;
  text-align: center;
  font-size: 13px; color: #9E9E9E;
  background: transparent;
  padding: 0;
}
.progress-motivation strong { color: #555; font-weight: 600; margin: 0 2px; }
.progress-motivation .highlight { color: #555; font-weight: 600; }

/* ========== v5.3: 刷新按钮样式 ========== */
.section-title,
.content-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title .refresh-btn,
.content-header .refresh-btn {
  margin-left: auto;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8B4513;
  transition: all 0.3s ease;
}

.section-title .refresh-btn:hover:not(:disabled),
.content-header .refresh-btn:hover:not(:disabled) {
  background: rgba(139, 69, 19, 0.1);
  transform: rotate(15deg);
}

.section-title .refresh-btn:disabled,
.content-header .refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.section-title .refresh-btn .spinning,
.content-header .refresh-btn .spinning {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ========== v5.4: 语义化优惠券样式 ========== */
.semantic-card {
  display: flex !important; /* Override old flex if needed */
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  margin-bottom: 12px;
  min-height: 100px;
  transition: transform 0.2s;
}
.semantic-card:active { transform: scale(0.98); }

/* Left Visual Area (30%) */
.visual-area {
  flex: 0 0 30%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  position: relative;
  /* Perforated edge effect using radial-gradient could be added here if desired */
  border-right: 2px dashed rgba(255,255,255,0.3);
}

/* Visual Variants */
.visual-area.type-bogo {
  background: linear-gradient(135deg, #E6B07A, #C69C6D); /* Brand Gold */
}
.visual-area.type-exchange {
  background: #A1887F; /* Caramel/Brown */
}
.visual-area.type-discount {
  background: #FF7043; /* Warm Red */
}
.visual-area.type-cash {
  background: #5D4037; /* Dark Brown */
}
.visual-area.type-delivery {
  background: #42A5F5; /* Blue */
}
/* v5.3: SHOT Coupon - Deep Coffee Theme */
.visual-area.type-addon {
  background: linear-gradient(135deg, #3E2723, #5D4037); /* Deep Coffee */
  color: #D7CCC8;
}

/* v5.3.4: 新品券 - 暖橙色主题 (Fresh & Vibrant) */
.visual-area.type-new-product {
  background: linear-gradient(135deg, #FF7043 0%, #FFAB91 100%); /* 珊瑚橙渐变 */
  color: #FFF;
  position: relative;
  overflow: visible;
}

/* NEW 角标 */
.new-product-corner-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: linear-gradient(135deg, #FFD54F, #FFC107);
  color: #E65100;
  font-size: 9px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 0 8px 0 8px;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 4px rgba(255, 193, 7, 0.4);
  z-index: 2;
}

/* 折扣数字 - 大字体强调 */
.new-product-discount {
  display: flex;
  align-items: baseline;
  margin-bottom: 4px;
}

.new-product-discount .discount-num {
  font-family: 'DIN Alternate', 'Arial Black', sans-serif;
  font-size: 42px;
  font-weight: 900;
  line-height: 1;
  text-shadow: 0 2px 4px rgba(0,0,0,0.15);
}

.new-product-discount .discount-unit {
  font-size: 16px;
  font-weight: 700;
  margin-left: 2px;
}

/* 新品免单图标 */
.new-product-free-icon {
  font-size: 36px;
  margin-bottom: 4px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
}

/* 新品标签 */
.new-product-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  background: rgba(255,255,255,0.25);
  padding: 3px 10px;
  border-radius: 12px;
  backdrop-filter: blur(4px);
}

/* 新品券卡片整体增强 */
.semantic-card.type-new-product {
  box-shadow: 0 4px 16px rgba(255, 112, 67, 0.2);
  border: 1px solid rgba(255, 112, 67, 0.1);
}

.semantic-card.type-new-product:hover {
  box-shadow: 0 6px 20px rgba(255, 112, 67, 0.3);
  transform: translateY(-2px);
}

/* 新品券专属按钮样式 */
.semantic-card.type-new-product .use-btn {
  background: linear-gradient(90deg, #FF7043, #F4511E);
  box-shadow: 0 2px 8px rgba(244, 81, 30, 0.3);
}

.semantic-card.type-new-product .use-btn:hover {
  background: linear-gradient(90deg, #F4511E, #E64A19);
  box-shadow: 0 4px 12px rgba(244, 81, 30, 0.4);
}

/* Used/Expired State Overrides */
.semantic-card.used .visual-area,
.semantic-card.expired .visual-area {
  background: #B0BEC5 !important;
  opacity: 0.8;
}

/* Typography in Visual Area */
.visual-text-large { font-size: 24px; font-weight: 800; line-height: 1; }
.visual-sub { font-size: 12px; margin-top: 4px; opacity: 0.9; }
.visual-icon { margin-bottom: 4px; }
.visual-value-group { display: flex; align-items: baseline; }
.visual-unit { font-size: 14px; margin-right: 2px; }
.visual-num { font-size: 28px; font-weight: 800; line-height: 1; }

/* Right Info Area (70%) */
.info-area {
  flex: 1;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.info-header { display: flex; justify-content: space-between; align-items: flex-start; }
.coupon-title { font-weight: 700; color: #333; font-size: 15px; }
.urgent-tag { 
  font-size: 10px; color: #D32F2F; border: 1px solid #D32F2F; 
  padding: 1px 4px; border-radius: 4px; white-space: nowrap; margin-left: 8px;
}
.stackable-tag {
  background: #FFF3E0; color: #EF6C00; font-size: 10px; padding: 2px 4px; border-radius: 4px; white-space: nowrap;
}

.coupon-condition { font-size: 12px; color: #757575; margin-top: 4px; margin-bottom: 8px; }

.info-footer { display: flex; justify-content: space-between; align-items: center; }
.expiry-date { font-size: 11px; color: #9E9E9E; }

.use-btn {
  background: #C69C6D; color: white; border: none;
  padding: 4px 12px; border-radius: 100px;
  font-size: 12px; font-weight: 600;
  cursor: pointer;
}
.status-stamp {
  font-size: 12px; font-weight: 700; color: #B0BEC5;
  border: 2px solid #B0BEC5; padding: 2px 6px; border-radius: 4px;
  transform: rotate(-10deg);
}

/* ========== v5.5: 手动领取按钮样式 ========== */
.benefit-action-area {
  margin-top: 20px;
  width: 100%;
}

.benefit-action-area .receive-btn {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: none;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  /* Default Gold Theme */

  color: white;
  box-shadow: 0 4px 15px rgba(198, 156, 109, 0.3);
}

.benefit-action-area .receive-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(198, 156, 109, 0.4);
}

.benefit-action-area .receive-btn:disabled {
  background: #E0E0E0;
  color: #9E9E9E;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* Level Specific Overrides */
.benefit-action-area .receive-btn.silver {
  background: linear-gradient(135deg, #ECEFF1, #CFD8DC);
  color: #546E7A;
  box-shadow: 0 4px 15px rgba(135, 147, 152, 0.3);
}

.benefit-action-area .receive-btn.diamond {
  background: linear-gradient(135deg, #64B5F6, #1976D2);
  color: white;
  box-shadow: 0 4px 15px rgba(25, 118, 210, 0.3);
}
</style>
