import { nextTick, onBeforeUnmount, onMounted, type Ref } from 'vue'
import { gsap } from 'gsap'

export const useNexusMotion = (root: Ref<HTMLElement | undefined>) => {
  const cleanups: Array<() => void> = []
  let context: gsap.Context | undefined

  onMounted(async () => {
    await nextTick()
    if (!root.value) return

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    context = gsap.context(() => {
      gsap.fromTo(
        '[data-reveal]',
        { y: reduceMotion ? 0 : 28, opacity: 0, clipPath: 'inset(0 0 100% 0)' },
        {
          y: 0,
          opacity: 1,
          clipPath: 'inset(0 0 0% 0)',
          duration: reduceMotion ? 0.01 : 1.05,
          stagger: reduceMotion ? 0 : 0.09,
          ease: 'expo.out',
          clearProps: 'clipPath'
        }
      )
      gsap.fromTo(
        '[data-line-reveal]',
        { scaleX: 0, transformOrigin: 'left center' },
        { scaleX: 1, duration: reduceMotion ? 0.01 : 1.4, delay: 0.35, ease: 'expo.inOut' }
      )
    }, root.value)

    if (!window.matchMedia('(pointer: fine)').matches) return

    root.value.querySelectorAll<HTMLElement>('[data-tilt]').forEach((element) => {
      const move = (event: PointerEvent) => {
        const rect = element.getBoundingClientRect()
        const x = (event.clientX - rect.left) / rect.width - 0.5
        const y = (event.clientY - rect.top) / rect.height - 0.5
        gsap.to(element, {
          rotationY: x * 8,
          rotationX: y * -8,
          x: x * 3,
          y: y * 3,
          transformPerspective: 900,
          duration: 0.45,
          ease: 'power3.out',
          overwrite: true
        })
      }
      const leave = () => gsap.to(element, {
        rotationX: 0,
        rotationY: 0,
        x: 0,
        y: 0,
        duration: 0.7,
        ease: 'elastic.out(1, 0.55)'
      })
      element.addEventListener('pointermove', move)
      element.addEventListener('pointerleave', leave)
      cleanups.push(() => {
        element.removeEventListener('pointermove', move)
        element.removeEventListener('pointerleave', leave)
      })
    })

    root.value.querySelectorAll<HTMLElement>('[data-magnetic]').forEach((element) => {
      const move = (event: PointerEvent) => {
        const rect = element.getBoundingClientRect()
        const x = event.clientX - rect.left - rect.width / 2
        const y = event.clientY - rect.top - rect.height / 2
        gsap.to(element, { x: x * 0.16, y: y * 0.18, duration: 0.35, ease: 'power3.out' })
      }
      const leave = () => gsap.to(element, { x: 0, y: 0, duration: 0.65, ease: 'elastic.out(1, 0.45)' })
      element.addEventListener('pointermove', move)
      element.addEventListener('pointerleave', leave)
      cleanups.push(() => {
        element.removeEventListener('pointermove', move)
        element.removeEventListener('pointerleave', leave)
      })
    })
  })

  onBeforeUnmount(() => {
    cleanups.forEach((cleanup) => cleanup())
    context?.revert()
  })
}
