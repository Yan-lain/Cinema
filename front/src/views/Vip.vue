<template>
  <div class="vip-page">
    <div class="page-header">
      <h1>会员中心</h1>
    </div>
    <div class="vip-content">
      <div class="vip-banner">
        <div class="vip-icon">💎</div>
        <h2>尊享会员</h2>
        <p>感谢您的支持</p>
      </div>

      <div class="vip-info">
        <div class="info-card">
          <div class="info-icon">🎫</div>
          <div class="info-text">
            <h3>购票优惠</h3>
            <p>享受会员专属折扣</p>
          </div>
        </div>
        <div class="info-card">
          <div class="info-icon">🍿</div>
          <div class="info-text">
            <h3>爆米花套餐</h3>
            <p>免费爆米花饮料</p>
          </div>
        </div>
        <div class="info-card">
          <div class="info-icon">🎯</div>
          <div class="info-text">
            <h3>优先选座</h3>
            <p>提前选择最佳位置</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.vip-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 30px 0;
}

.vip-content {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.vip-banner {
  text-align: center;
  padding: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
  margin-bottom: 30px;
}

.vip-icon {
  font-size: 60px;
  margin-bottom: 16px;
}

.vip-banner h2 {
  margin: 0 0 8px 0;
  font-size: 28px;
}

.vip-banner p {
  margin: 0;
  opacity: 0.9;
  font-size: 16px;
}

.vip-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.info-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 12px;
  transition: transform 0.2s;
}

.info-card:hover {
  transform: translateY(-4px);
}

.info-icon {
  font-size: 40px;
}

.info-text h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #333;
}

.info-text p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

@media (max-width: 768px) {
  .vip-info {
    grid-template-columns: 1fr;
  }
}
</style>
