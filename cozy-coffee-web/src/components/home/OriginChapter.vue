<template>
  <article
    class="origin-chapter"
    :class="[`origin-chapter--${chapter.type}`, { 'is-active': active, 'is-static': staticMode }]"
    :data-origin-id="chapter.id"
    :aria-current="active ? 'step' : undefined"
  >
    <template v-if="chapter.type === 'origin'">
      <p class="origin-counter">{{ number }} / 08 · {{ chapter.englishName }}</p>
      <h3>{{ chapter.name }}</h3>
      <p class="origin-region">{{ chapter.region }}</p>
      <p class="origin-story origin-story--quote">{{ chapter.story }}</p>
      <dl class="origin-facts">
        <div class="origin-fact"><dt>海拔</dt><dd>{{ chapter.altitude }}</dd></div>
        <div class="origin-fact"><dt>处理法</dt><dd>{{ chapter.process }}</dd></div>
        <div class="origin-fact"><dt>代表品种</dt><dd>{{ chapter.varieties.join(' · ') }}</dd></div>
        <div class="origin-fact origin-fact--role"><dt>风味角色</dt><dd>{{ chapter.role }}</dd></div>
      </dl>
      <ul class="origin-flavors" aria-label="代表风味">
        <li v-for="flavor in chapter.flavors" :key="flavor">{{ flavor }}</li>
      </ul>
      <p class="origin-route-caption">{{ chapter.name }} → 杭州烘焙中心</p>
    </template>

    <template v-else>
      <p class="origin-counter">09 / 09 · HANGZHOU</p>
      <h3>{{ chapter.name }}</h3>
      <p class="origin-region">{{ chapter.region }}</p>
      <p class="origin-story origin-story--summary">{{ chapter.story }}</p>
      <ol class="roastery-process" aria-label="杭州烘焙流程">
        <li v-for="step in chapter.process" :key="step">{{ step }}</li>
      </ol>
    </template>
  </article>
</template>

<script setup>
defineProps({
  chapter: { type: Object, required: true },
  number: { type: String, required: true },
  active: { type: Boolean, default: false },
  staticMode: { type: Boolean, default: false }
})
</script>

<style scoped>
.origin-chapter { min-height: calc(100svh - var(--nav-height) - 24px); display: flex; flex-direction: column; justify-content: flex-start; padding: 40px 0 80px; scroll-margin-top: calc(var(--nav-height) + 12px); border-top: 1px solid var(--cozy-border); opacity: .38; transition: opacity .42s ease, border-color .42s ease; }
.origin-chapter.is-active { border-top: 2px solid var(--cozy-primary); opacity: 1; }
.origin-chapter.is-static { opacity: 1; }
.origin-counter { margin: 0 0 24px; color: var(--cozy-muted); font-size: 12px; letter-spacing: .08em; }
.origin-chapter h3 { margin: 0; font-size: clamp(2.3rem, 3.8vw, 3.4rem); line-height: 1.08; font-weight: 650; text-wrap: balance; }
.origin-region { margin: 10px 0 0; color: var(--cozy-primary); font-size: 15px; font-weight: 650; }
.origin-story { margin: 28px 0 0; color: var(--cozy-muted); font-size: 16px; line-height: 1.8; text-wrap: pretty; }
.origin-story--quote { position: relative; color: var(--cozy-ink); font-size: clamp(1.08rem, 1.5vw, 1.3rem); line-height: 1.75; }
.origin-story--quote::before { content: ''; display: block; width: 42px; height: 2px; margin-bottom: 18px; background: var(--cozy-primary); opacity: .72; }
.origin-story--summary { color: var(--cozy-ink); font-size: clamp(1.35rem, 2.4vw, 2rem); }
.origin-facts { display: grid; grid-template-columns: 1fr 1fr; gap: 22px 28px; margin: 34px 0 0; padding-block: 26px; border-block: 1px solid var(--cozy-border); }
.origin-facts dt { color: var(--cozy-muted); font-size: 12px; letter-spacing: .02em; }
.origin-facts dd { margin: 7px 0 0; color: var(--cozy-ink); line-height: 1.55; }
.origin-fact--role dd { color: var(--cozy-primary); font-weight: 700; }
.origin-flavors { display: flex; flex-wrap: wrap; gap: 8px; margin: 24px 0 0; padding: 0; list-style: none; }
.origin-flavors li { display: inline-flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 999px; color: var(--cozy-ink); background: color-mix(in oklch, var(--cozy-primary) 8%, var(--cozy-surface)); font-size: 13px; }
.origin-flavors li::before { content: ''; width: 5px; height: 5px; border-radius: 50%; background: var(--cozy-primary); opacity: .8; }
.origin-route-caption { margin: 28px 0 0; color: var(--cozy-primary); font-size: 13px; font-weight: 600; }
.roastery-process { display: flex; flex-wrap: wrap; gap: 10px 28px; margin: 32px 0 0; padding: 0; list-style: none; }
.roastery-process li:not(:last-child)::after { content: '→'; margin-left: 28px; color: var(--cozy-muted); }

@media (max-width: 820px) {
  .origin-chapter { min-height: 58svh; padding-block: 56px; }
}

@media (max-width: 520px) {
  .origin-facts { grid-template-columns: 1fr; }
  .origin-flavors li { background: color-mix(in oklch, var(--cozy-primary) 7%, var(--cozy-bg)); }
}

@media (prefers-reduced-motion: reduce) {
  .origin-chapter { transition: none; }
}
</style>
