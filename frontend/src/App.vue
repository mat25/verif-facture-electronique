<template>
  <div class="page">
    <!-- Header -->
    <header class="header">
      <div class="logo">
        <span class="logo-icon">📄</span>
        <div>
          <h1 class="logo-title">VerifFacture</h1>
          <p class="logo-sub">Vérification de facture électronique UBL</p>
        </div>
      </div>
    </header>

    <!-- Card principale -->
    <main class="card">
      <h2 class="card-title">Valider une facture</h2>
      <p class="card-desc">Sélectionnez le format de la norme et importez votre fichier XML pour lancer la validation.</p>

      <form @submit.prevent="valider" class="form">

        <!-- Sélection format -->
        <div class="field">
          <label class="label" for="format">Format de validation</label>
          <div class="select-wrapper">
            <select id="format" v-model="format" class="select">
              <option value="" disabled>— Choisir un format —</option>
              <option value="EN16931">EN16931 — Norme européenne</option>
              <option value="EXTENDED">EXTENDED — Profil étendu (CTC-FR + BR-FR)</option>
            </select>
            <span class="select-chevron">▾</span>
          </div>
        </div>

        <!-- Upload fichier -->
        <div class="field">
          <label class="label">Fichier XML de la facture</label>
          <div
            class="dropzone"
            :class="{ 'dropzone--active': dragging, 'dropzone--filled': fichier }"
            @dragenter.prevent="dragging = true"
            @dragleave.prevent="dragging = false"
            @dragover.prevent
            @drop.prevent="onDrop"
            @click="$refs.fileInput.click()"
          >
            <input
              ref="fileInput"
              type="file"
              accept=".xml"
              class="file-input"
              @change="onFileChange"
            />
            <div v-if="!fichier" class="dropzone-placeholder">
              <span class="dropzone-icon">📂</span>
              <p>Glisser-déposer un fichier XML ici</p>
              <span class="dropzone-hint">ou cliquer pour parcourir</span>
            </div>
            <div v-else class="dropzone-filled">
              <span class="dropzone-icon">✅</span>
              <p class="file-name">{{ fichier.name }}</p>
              <span class="dropzone-hint">{{ formatBytes(fichier.size) }}</span>
            </div>
          </div>
        </div>

        <!-- Bouton -->
        <button
          type="submit"
          class="btn"
          :disabled="loading || !format || !fichier"
          :class="{ 'btn--loading': loading }"
        >
          <span v-if="!loading">🔍 Lancer la validation</span>
          <span v-else class="spinner-wrap"><span class="spinner"></span> Validation en cours…</span>
        </button>
      </form>
    </main>

    <!-- Résultats -->
    <transition name="fade-up">
      <section v-if="resultats.length" class="results">
        <h3 class="results-title">Résultats de la validation</h3>
        <ul class="results-list">
          <li
            v-for="(msg, i) in resultats"
            :key="i"
            class="result-item"
            :class="getClass(msg)"
          >
            <span class="result-text">{{ msg }}</span>
          </li>
        </ul>
        <button class="btn-reset" @click="reset">↩ Nouvelle validation</button>
      </section>
    </transition>

    <!-- Erreur réseau -->
    <transition name="fade-up">
      <div v-if="erreur" class="banner banner--error">
        <span>⚠️ {{ erreur }}</span>
        <button class="banner-close" @click="erreur = null">✕</button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const API_URL = 'http://localhost:8080/api/v1/validation/ubl'

const format   = ref('')
const fichier  = ref(null)
const loading  = ref(false)
const resultats = ref([])
const erreur   = ref(null)
const dragging = ref(false)
const fileInput = ref(null)

function onFileChange(e) {
  const f = e.target.files[0]
  if (f) fichier.value = f
}

function onDrop(e) {
  dragging.value = false
  const f = e.dataTransfer.files[0]
  if (f && f.name.endsWith('.xml')) fichier.value = f
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' o'
  return (bytes / 1024).toFixed(1) + ' Ko'
}

function getClass(msg) {
  if (msg.startsWith('✅')) return 'result-item--success'
  if (msg.startsWith('❌')) return 'result-item--error'
  if (msg.includes('🔴 [FATAL]')) return 'result-item--fatal'
  if (msg.includes('🟡 [AVERTISSEMENT]')) return 'result-item--warning'
  if (msg.startsWith('  •')) return 'result-item--detail'
  return ''
}

async function valider() {
  if (!fichier.value || !format.value) return
  loading.value = true
  resultats.value = []
  erreur.value = null

  const body = new FormData()
  body.append('file', fichier.value)
  body.append('format', format.value)

  try {
    const res = await fetch(API_URL, { method: 'POST', body })
    if (!res.ok) throw new Error(`Erreur HTTP ${res.status}`)
    resultats.value = await res.json()
  } catch (e) {
    erreur.value = 'Impossible de joindre le serveur : ' + e.message
  } finally {
    loading.value = false
  }
}

function reset() {
  resultats.value = []
  fichier.value = null
  format.value = ''
  if (fileInput.value) fileInput.value.value = ''
}
</script>

<style scoped>
.page {
  width: 100%;
  max-width: 680px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ── Header ── */
.header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-bottom: 8px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 14px;
}
.logo-icon {
  font-size: 2.2rem;
  filter: drop-shadow(0 0 12px #6c63ff88);
}
.logo-title {
  font-size: 1.6rem;
  font-weight: 700;
  background: linear-gradient(135deg, #a78bfa, #6c63ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.logo-sub {
  font-size: 0.8rem;
  color: var(--text-muted);
  margin-top: -4px;
}

/* ── Card ── */
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 32px;
  box-shadow: var(--shadow);
}
.card-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 6px;
}
.card-desc {
  font-size: 0.87rem;
  color: var(--text-muted);
  margin-bottom: 28px;
  line-height: 1.5;
}

/* ── Form ── */
.form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.label {
  font-size: 0.82rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
}

/* Select */
.select-wrapper {
  position: relative;
}
.select {
  width: 100%;
  padding: 12px 40px 12px 14px;
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text);
  font-size: 0.95rem;
  font-family: inherit;
  appearance: none;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  outline: none;
}
.select:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(108,99,255,0.2);
}
.select-chevron {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  color: var(--text-muted);
  font-size: 1.1rem;
}

/* Dropzone */
.dropzone {
  border: 2px dashed var(--border);
  border-radius: 10px;
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  background: var(--surface2);
  user-select: none;
}
.dropzone:hover,
.dropzone--active {
  border-color: var(--accent);
  background: rgba(108,99,255,0.06);
}
.dropzone--filled {
  border-color: var(--success);
  border-style: solid;
  background: rgba(34,197,94,0.06);
}
.file-input {
  display: none;
}
.dropzone-icon {
  font-size: 2rem;
  display: block;
  margin-bottom: 8px;
}
.dropzone p {
  font-size: 0.92rem;
  color: var(--text);
}
.dropzone-hint {
  font-size: 0.78rem;
  color: var(--text-muted);
  margin-top: 4px;
  display: block;
}
.dropzone-placeholder,
.dropzone-filled {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.file-name {
  font-weight: 600;
  color: var(--success);
  word-break: break-all;
}

/* Button */
.btn {
  padding: 14px;
  background: linear-gradient(135deg, #6c63ff, #a78bfa);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.15s, box-shadow 0.2s;
  box-shadow: 0 4px 20px rgba(108,99,255,0.4);
}
.btn:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(108,99,255,0.5);
}
.btn:active:not(:disabled) {
  transform: translateY(0);
}
.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
}
.spinner-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Results ── */
.results {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 28px 32px;
  box-shadow: var(--shadow);
}
.results-title {
  font-size: 1.05rem;
  font-weight: 600;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.results-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 22px;
}
.result-item {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.9rem;
  border-left: 3px solid transparent;
  background: var(--surface2);
}
.result-item--success {
  border-left-color: var(--success);
  background: rgba(34,197,94,0.08);
}
.result-item--error {
  border-left-color: var(--error);
  background: rgba(244,63,94,0.08);
}
.result-item--fatal {
  border-left-color: #f43f5e;
  background: rgba(244,63,94,0.12);
  padding-left: 22px;
  font-size: 0.86rem;
  font-weight: 500;
}
.result-item--warning {
  border-left-color: #f59e0b;
  background: rgba(245,158,11,0.10);
  padding-left: 22px;
  font-size: 0.86rem;
  color: #fbbf24;
}
.result-item--detail {
  border-left-color: var(--border);
  background: rgba(255,255,255,0.03);
  padding-left: 22px;
  font-size: 0.83rem;
  color: var(--text-muted);
}
.result-text {
  white-space: pre-wrap;
  word-break: break-word;
}
.btn-reset {
  margin-top: 4px;
  background: none;
  border: 1px solid var(--border);
  color: var(--text-muted);
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.85rem;
  font-family: inherit;
  transition: border-color 0.2s, color 0.2s;
}
.btn-reset:hover {
  border-color: var(--accent);
  color: var(--accent2);
}

/* ── Banner erreur réseau ── */
.banner {
  width: 100%;
  padding: 14px 18px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.88rem;
}
.banner--error {
  background: rgba(244,63,94,0.12);
  border: 1px solid var(--error);
  color: #fca5a5;
}
.banner-close {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 1rem;
  opacity: 0.7;
}
.banner-close:hover { opacity: 1; }

/* ── Animations ── */
.fade-up-enter-active {
  animation: fadeUp 0.35s ease both;
}
.fade-up-leave-active {
  animation: fadeUp 0.2s ease reverse both;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
