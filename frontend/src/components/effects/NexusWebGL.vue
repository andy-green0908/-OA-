<template>
  <div ref="host" class="nexus-webgl" :data-scene="variant" aria-hidden="true"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as THREE from 'three'

const props = withDefaults(defineProps<{
  variant?: 'auth' | 'dashboard'
}>(), {
  variant: 'auth'
})

const host = ref<HTMLDivElement>()
let renderer: THREE.WebGLRenderer | undefined
let scene: THREE.Scene | undefined
let camera: THREE.PerspectiveCamera | undefined
let frameId = 0
let resizeObserver: ResizeObserver | undefined
let pointerHandler: ((event: PointerEvent) => void) | undefined
let downHandler: (() => void) | undefined

onMounted(() => {
  if (!host.value) return

  const container = host.value
  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(48, 1, 0.1, 80)
  camera.position.set(0, 0.3, 7.4)

  renderer = new THREE.WebGLRenderer({
    alpha: true,
    antialias: true,
    powerPreference: 'high-performance'
  })
  renderer.setClearColor(0x000000, 0)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2.5))
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.domElement.dataset.webgl = 'nexus-data-field'
  container.appendChild(renderer.domElement)

  const world = new THREE.Group()
  world.position.z = props.variant === 'dashboard' ? -0.5 : 0
  scene.add(world)

  const particleCount = 1800
  const positions = new Float32Array(particleCount * 3)
  const scales = new Float32Array(particleCount)
  const phases = new Float32Array(particleCount)
  const mixes = new Float32Array(particleCount)

  for (let i = 0; i < particleCount; i += 1) {
    const radius = 2.2 + Math.random() * 5.8
    const theta = Math.random() * Math.PI * 2
    const spread = (Math.random() - 0.5) * 5.4
    positions[i * 3] = Math.cos(theta) * radius + spread * 0.35
    positions[i * 3 + 1] = Math.sin(theta) * radius * 0.54 + (Math.random() - 0.5) * 1.8
    positions[i * 3 + 2] = -2.5 - Math.random() * 8.5
    scales[i] = 0.7 + Math.random() * 2.4
    phases[i] = Math.random() * Math.PI * 2
    mixes[i] = Math.random()
  }

  const particleGeometry = new THREE.BufferGeometry()
  particleGeometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  particleGeometry.setAttribute('aScale', new THREE.BufferAttribute(scales, 1))
  particleGeometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  particleGeometry.setAttribute('aMix', new THREE.BufferAttribute(mixes, 1))

  const particleMaterial = new THREE.ShaderMaterial({
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    uniforms: {
      uTime: { value: 0 },
      uPointer: { value: new THREE.Vector2() },
      uImpulse: { value: 0 }
    },
    vertexShader: `
      attribute float aScale;
      attribute float aPhase;
      attribute float aMix;
      uniform float uTime;
      uniform vec2 uPointer;
      uniform float uImpulse;
      varying float vMix;
      varying float vDepth;

      void main() {
        vec3 p = position;
        float wave = sin(p.x * 0.72 + uTime * 0.55 + aPhase) * 0.12;
        p.y += wave + uPointer.y * 0.18 * aMix;
        p.x += cos(p.y * 0.82 + uTime * 0.32 + aPhase) * 0.08 + uPointer.x * 0.15;
        p.xy += normalize(p.xy + 0.001) * uImpulse * 0.34 * (0.35 + aMix);
        vec4 mvPosition = modelViewMatrix * vec4(p, 1.0);
        gl_Position = projectionMatrix * mvPosition;
        gl_PointSize = aScale * (24.0 / max(1.0, -mvPosition.z));
        vMix = aMix;
        vDepth = clamp((-mvPosition.z - 3.0) / 10.0, 0.0, 1.0);
      }
    `,
    fragmentShader: `
      varying float vMix;
      varying float vDepth;

      void main() {
        float d = length(gl_PointCoord - vec2(0.5));
        float alpha = smoothstep(0.5, 0.06, d) * (0.9 - vDepth * 0.52);
        vec3 emerald = vec3(0.0, 0.92, 0.66);
        vec3 signal = vec3(0.90, 1.0, 0.31);
        vec3 electric = vec3(0.16, 0.47, 1.0);
        vec3 color = mix(emerald, signal, smoothstep(0.45, 0.95, vMix));
        color = mix(color, electric, smoothstep(0.0, 0.16, vMix) * 0.7);
        gl_FragColor = vec4(color, alpha);
      }
    `
  })
  const particles = new THREE.Points(particleGeometry, particleMaterial)
  world.add(particles)

  const grid = new THREE.GridHelper(15, 36, 0x00d99a, 0x28444e)
  grid.position.set(0, -2.2, -2.2)
  grid.rotation.x = 0.08
  const gridMaterial = grid.material as THREE.Material
  gridMaterial.transparent = true
  gridMaterial.opacity = props.variant === 'dashboard' ? 0.34 : 0.23
  world.add(grid)

  const core = new THREE.Group()
  core.position.set(props.variant === 'dashboard' ? 3.1 : 2.6, 0.15, -1.5)
  const coreGeometry = new THREE.IcosahedronGeometry(0.72, 1)
  const coreMaterial = new THREE.MeshBasicMaterial({
    color: 0x00e8ac,
    wireframe: true,
    transparent: true,
    opacity: 0.68
  })
  const coreMesh = new THREE.Mesh(coreGeometry, coreMaterial)
  core.add(coreMesh)

  const ringColors = [0xe5ff4f, 0x00a878, 0x2878ff]
  const rings: THREE.Mesh[] = []
  ringColors.forEach((color, index) => {
    const geometry = new THREE.TorusGeometry(1.08 + index * 0.38, 0.008 + index * 0.003, 8, 120)
    const material = new THREE.MeshBasicMaterial({
      color,
      transparent: true,
      opacity: 0.32 - index * 0.06,
      depthWrite: false
    })
    const ring = new THREE.Mesh(geometry, material)
    ring.rotation.set(0.35 + index * 0.42, index * 0.58, index * 0.28)
    rings.push(ring)
    core.add(ring)
  })
  world.add(core)

  const barCount = 48
  const barGeometry = new THREE.BoxGeometry(0.045, 1, 0.045)
  const barMaterial = new THREE.MeshBasicMaterial({ color: 0xe5ff4f, transparent: true, opacity: 0.48 })
  const bars = new THREE.InstancedMesh(barGeometry, barMaterial, barCount)
  const barMatrix = new THREE.Matrix4()
  for (let i = 0; i < barCount; i += 1) {
    const height = 0.18 + Math.random() * 1.25
    const x = -5.5 + (i / (barCount - 1)) * 11
    const z = -2.8 - Math.random() * 1.4
    barMatrix.compose(
      new THREE.Vector3(x, -1.95 + height * 0.5, z),
      new THREE.Quaternion(),
      new THREE.Vector3(1, height, 1)
    )
    bars.setMatrixAt(i, barMatrix)
  }
  bars.instanceMatrix.needsUpdate = true
  world.add(bars)

  const targetPointer = new THREE.Vector2()
  const smoothPointer = new THREE.Vector2()
  pointerHandler = (event: PointerEvent) => {
    const rect = container.getBoundingClientRect()
    targetPointer.x = ((event.clientX - rect.left) / Math.max(rect.width, 1)) * 2 - 1
    targetPointer.y = -(((event.clientY - rect.top) / Math.max(rect.height, 1)) * 2 - 1)
  }
  downHandler = () => {
    particleMaterial.uniforms.uImpulse.value = 1
  }
  container.addEventListener('pointermove', pointerHandler)
  container.addEventListener('pointerdown', downHandler)

  const resize = () => {
    if (!renderer || !camera) return
    const width = Math.max(container.clientWidth, 1)
    const height = Math.max(container.clientHeight, 1)
    renderer.setSize(width, height, false)
    camera.aspect = width / height
    camera.updateProjectionMatrix()
  }
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(container)
  resize()

  const clock = new THREE.Clock()
  const render = () => {
    if (!renderer || !scene || !camera) return
    const elapsed = clock.getElapsedTime()
    smoothPointer.lerp(targetPointer, 0.045)
    particleMaterial.uniforms.uTime.value = elapsed
    particleMaterial.uniforms.uPointer.value.copy(smoothPointer)
    particleMaterial.uniforms.uImpulse.value *= 0.93
    particles.rotation.z = elapsed * 0.006
    particles.rotation.y = smoothPointer.x * 0.025
    grid.position.x = smoothPointer.x * -0.18
    core.rotation.y = elapsed * 0.18 + smoothPointer.x * 0.3
    core.rotation.x = Math.sin(elapsed * 0.35) * 0.12 + smoothPointer.y * 0.16
    rings[0].rotation.z = elapsed * 0.22
    rings[1].rotation.x = elapsed * -0.16
    rings[2].rotation.y = elapsed * 0.12
    bars.rotation.y = Math.sin(elapsed * 0.18) * 0.012
    camera.position.x += (smoothPointer.x * 0.34 - camera.position.x) * 0.035
    camera.position.y += (0.3 + smoothPointer.y * 0.2 - camera.position.y) * 0.035
    camera.lookAt(0, 0, -1.5)
    renderer.render(scene, camera)
    frameId = requestAnimationFrame(render)
  }
  render()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(frameId)
  resizeObserver?.disconnect()
  if (host.value && pointerHandler) host.value.removeEventListener('pointermove', pointerHandler)
  if (host.value && downHandler) host.value.removeEventListener('pointerdown', downHandler)
  scene?.traverse((object) => {
    const item = object as THREE.Mesh
    item.geometry?.dispose()
    const materials = Array.isArray(item.material) ? item.material : [item.material]
    materials.filter(Boolean).forEach((material) => material.dispose())
  })
  renderer?.dispose()
  renderer?.domElement.remove()
})
</script>

<style scoped>
.nexus-webgl {
  position: absolute;
  inset: 0;
  z-index: 1;
  overflow: hidden;
  pointer-events: auto;
}

.nexus-webgl :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
}

.nexus-webgl[data-scene='dashboard'] {
  opacity: 0.82;
}
</style>
