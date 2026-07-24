<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { DisasterTypeLabel, IncidentLevelLabel, IncidentStatusLabel } from '@/types/enums'
import type { DisasterTypeValue, IncidentLevelValue, IncidentStatusValue } from '@/types/enums'

interface MapIncident {
  incidentId: string
  incidentName: string
  disasterType: DisasterTypeValue
  incidentLevel: IncidentLevelValue
  status: IncidentStatusValue
  latitude: number
  longitude: number
}

const props = withDefaults(defineProps<{
  incidents: MapIncident[]
  center?: [number, number]
  zoom?: number
}>(), {
  center: () => [25.04, 102.68],
  zoom: 7,
})

const mapContainer = ref<HTMLDivElement>()
let map: L.Map | null = null
let markerGroup: L.LayerGroup | null = null

function getStatusColor(status: IncidentStatusValue): string {
  const colors: Record<string, string> = {
    pending: '#e6a23c',
    confirmed: '#409eff',
    processing: '#f56c6c',
    completed: '#67c23a',
  }
  return colors[status] || '#909399'
}

function createMarkers(): void {
  if (!map) return
  if (markerGroup) {
    markerGroup.clearLayers()
  } else {
    markerGroup = L.layerGroup().addTo(map)
  }

  for (const inc of props.incidents) {
    const color = getStatusColor(inc.status)
    const icon = L.divIcon({
      className: 'incident-marker',
      html: `<div style="width:12px;height:12px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 4px rgba(0,0,0,0.5)"></div>`,
      iconSize: [12, 12],
      iconAnchor: [6, 6],
    })
    const marker = L.marker([inc.latitude, inc.longitude], { icon })
    const popupHtml = `
      <div style="min-width:180px">
        <div style="font-weight:600;margin-bottom:4px">${inc.incidentName}</div>
        <div style="font-size:12px;color:#666">类型：${DisasterTypeLabel[inc.disasterType] ?? inc.disasterType}</div>
        <div style="font-size:12px;color:#666">等级：${IncidentLevelLabel[inc.incidentLevel] ?? inc.incidentLevel}</div>
        <div style="font-size:12px;color:#666">状态：${IncidentStatusLabel[inc.status] ?? inc.status}</div>
      </div>
    `
    marker.bindPopup(popupHtml)
    markerGroup!.addLayer(marker)
  }
}

onMounted(() => {
  if (!mapContainer.value) return
  map = L.map(mapContainer.value, {
    center: props.center,
    zoom: props.zoom,
    zoomControl: true,
    attributionControl: false,
  })
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
  }).addTo(map)
  createMarkers()
  setTimeout(() => map?.invalidateSize(), 100)
})

onUnmounted(() => {
  if (markerGroup) {
    markerGroup.clearLayers()
    markerGroup = null
  }
  if (map) {
    map.remove()
    map = null
  }
})

watch(() => props.incidents, () => {
  createMarkers()
}, { deep: true })
</script>

<template>
  <div ref="mapContainer" class="incident-map" />
</template>

<style scoped>
.incident-map {
  width: 100%;
  height: 100%;
  min-height: 300px;
}
</style>

<style>
.leaflet-container {
  background: #1a1a2e;
}
</style>