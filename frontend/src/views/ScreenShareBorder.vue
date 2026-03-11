<template>
  <div class="border-container">
    <svg width="60" height="60" class="border-svg">
      <!-- 左上角 -->
      <g v-if="corner === 'top-left'">
        <line x1="0" y1="20" x2="0" y2="0" stroke="#4caf50" stroke-width="4" />
        <line x1="0" y1="0" x2="20" y2="0" stroke="#4caf50" stroke-width="4" />
      </g>
      
      <!-- 右上角 -->
      <g v-if="corner === 'top-right'">
        <line x1="40" y1="0" x2="60" y2="0" stroke="#4caf50" stroke-width="4" />
        <line x1="60" y1="0" x2="60" y2="20" stroke="#4caf50" stroke-width="4" />
      </g>
      
      <!-- 左下角 -->
      <g v-if="corner === 'bottom-left'">
        <line x1="0" y1="40" x2="0" y2="60" stroke="#4caf50" stroke-width="4" />
        <line x1="0" y1="60" x2="20" y2="60" stroke="#4caf50" stroke-width="4" />
      </g>
      
      <!-- 右下角 -->
      <g v-if="corner === 'bottom-right'">
        <line x1="60" y1="40" x2="60" y2="60" stroke="#4caf50" stroke-width="4" />
        <line x1="40" y1="60" x2="60" y2="60" stroke="#4caf50" stroke-width="4" />
      </g>
    </svg>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const corner = ref('top-left')

onMounted(() => {
  // 从 URL 参数获取角位置
  corner.value = route.query.corner || 'top-left'
  console.log('🎯 边框组件已挂载，位置:', corner.value)
})
</script>

<style scoped>
.border-container {
  width: 60px;
  height: 60px;
  pointer-events: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.border-svg {
  filter: drop-shadow(0 0 4px rgba(76, 175, 80, 0.6));
  animation: glow 2s ease-in-out infinite;
}

@keyframes glow {
  0%, 100% {
    filter: drop-shadow(0 0 4px rgba(76, 175, 80, 0.6));
  }
  50% {
    filter: drop-shadow(0 0 8px rgba(76, 175, 80, 0.9));
  }
}
</style>
