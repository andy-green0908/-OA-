<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-container">
    <RouterView />
    <div class="global-noise" aria-hidden="true"></div>
  </div>
</template>

<style>
.app-container {
  height: 100vh;
  width: 100vw;
}

* {
  box-sizing: border-box;
}

html, body {
  margin: 0;
  height: 100%;
  width: 100%;
}
</style>
