<template>
  <router-view />
</template>

<script setup>
import { ref, onMounted, provide } from 'vue'
import { systemService } from './api/services'

// 系统设置状态
const systemSetting = ref(null)
const loadingSetting = ref(true)
const settingError = ref(null)

// 加载系统设置
const loadSystemSetting = async () => {
  try {
    loadingSetting.value = true
    settingError.value = null
    const response = await systemService.loadSystemSetting()
    systemSetting.value = response.data.data
    console.log('系统设置加载成功:', response.data.data)
  } catch (error) {
    console.error('系统设置加载失败:', error)
    settingError.value = error.message || '加载系统设置失败'
  } finally {
    loadingSetting.value = false
  }
}

// 在应用启动时加载系统设置
onMounted(() => {
  loadSystemSetting()
})

// 提供系统设置给所有子组件
provide('systemSetting', systemSetting)
provide('loadingSetting', loadingSetting)
provide('settingError', settingError)
provide('reloadSystemSetting', loadSystemSetting)
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: Arial, sans-serif;
  background-color: #f5f5f5;
}
</style>