<script setup lang="ts">import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { DisasterTypeLabel, IncidentLevelLabel, IncidentStatusLabel, DisposalPlanStatusLabel, ResourceDispatchStatusLabel } from '@/types/enums';
import type { DisasterTypeValue, IncidentStatusValue } from '@/types/enums';
import type { MapIncident } from '@/types/dashboard';
const props = withDefaults(defineProps<{
 incidents: MapIncident[];
 center?: [
 number,
 number
 ];
 zoom?: number;
}>(), {
 center: () => [25.04, 102.68],
 zoom: 7,
});
const emit = defineEmits<{
 (e: 'select', incident: MapIncident): void;
}>();
const mapContainer = ref<HTMLDivElement>();
let map: L.Map | null = null;
let markerGroup: L.LayerGroup | null = null;
let legendControl: L.Control | null = null;
const validIncidents = computed(() => {
 return props.incidents.filter(inc => inc.latitude != null &&
 inc.longitude != null &&
 !isNaN(inc.latitude) &&
 !isNaN(inc.longitude) &&
 inc.latitude >= -90 && inc.latitude <= 90 &&
 inc.longitude >= -180 && inc.longitude <= 180);
});
const disasterTypeColors: Record<string, string> = {
 earthquake: '#ff4d4f',
 mudslide: '#fa8c16',
 flood: '#1890ff',
 drought: '#faad14',
 landslide: '#722ed1',
 fire: '#f5222d',
 other: '#13c2c2',
};
const statusColors: Record<string, string> = {
 pending: '#909399',
 confirmed: '#409eff',
 processing: '#f56c6c',
 completed: '#67c23a',
};
const disasterTypes = Object.entries(DisasterTypeLabel) as [DisasterTypeValue, string][];
const incidentStatuses: [IncidentStatusValue, string][] = [
 ['pending', '待确认'],
 ['confirmed', '已确认'],
 ['processing', '处置中'],
 ['completed', '已结束'],
];
function getStatusColor(status: string): string {
 return statusColors[status] || '#909399';
}
function getDisasterColor(disasterType: string): string {
 return disasterTypeColors[disasterType] || '#409eff';
}
function computeDisposalProgress(inc: MapIncident): number {
 let steps = 0;
 let total = 4;
 if (inc.status === 'completed')
 return 100;
 if (inc.disposalPlanStatus && inc.disposalPlanStatus !== '') {
 steps++;
 }
 if (inc.disposalPlanStatus === 'accepted') {
 steps++;
 }
 if (inc.resourceDispatchStatus && inc.resourceDispatchStatus !== '') {
 steps++;
 }
 if (inc.resourceDispatchStatus === 'completed') {
 steps++;
 }
 if (inc.status === 'processing') {
 steps++;
 }
 return Math.round((steps / total) * 100);
}
function getProgressColor(progress: number): string {
 if (progress >= 100)
 return '#67c23a';
 if (progress >= 60)
 return '#409eff';
 if (progress >= 30)
 return '#e6a23c';
 return '#f56c6c';
}
function createLegend(): L.Control {
 const legend = new L.Control({ position: 'bottomright' });
 legend.onAdd = function () {
 const div = L.DomUtil.create('div', 'map-legend');
 div.innerHTML = `
 <div class="map-legend__title">灾害类型</div>
 <div class="map-legend__items">
 ${disasterTypes.map(([key, label]) => `
 <div class="map-legend__item">
 <span class="map-legend__dot" style="background:${getDisasterColor(key)}"></span>
 <span class="map-legend__label">${label}</span>
 </div>
 `).join('')}
 </div>
 <div class="map-legend__title" style="margin-top:8px">事件状态</div>
 <div class="map-legend__items">
 ${incidentStatuses.map(([key, label]) => `
 <div class="map-legend__item">
 <span class="map-legend__dot" style="background:${getStatusColor(key)}"></span>
 <span class="map-legend__label">${label}</span>
 </div>
 `).join('')}
 </div>
 `;
 return div;
 };
 return legend;
}
function createTooltip(inc: MapIncident): string {
 const statusColor = getStatusColor(inc.status);
 const disasterColor = getDisasterColor(inc.disasterType);
 const progress = computeDisposalProgress(inc);
 const progressColor = getProgressColor(progress);
 const planStatus = inc.disposalPlanStatus ? (DisposalPlanStatusLabel[inc.disposalPlanStatus] || inc.disposalPlanStatus) : '未拟定';
 const dispatchStatus = inc.resourceDispatchStatus ? (ResourceDispatchStatusLabel[inc.resourceDispatchStatus] || inc.resourceDispatchStatus) : '未调度';
 const deathInfo = inc.deathCount ? `<div style="color:#f56c6c">💀 死亡：${inc.deathCount}人</div>` : '';
 const lossInfo = inc.propertyLoss ? `<div>💰 损失：${inc.propertyLoss}万元</div>` : '';
 return `
 <div style="min-width:220px;max-width:280px;padding:0">
 <div style="background:linear-gradient(135deg,${disasterColor}dd,${statusColor}dd);padding:8px 12px;border-radius:8px 8px 0 0;color:#fff">
 <div style="font-weight:700;font-size:14px">${inc.incidentName}</div>
 <div style="font-size:11px;opacity:0.9;margin-top:2px">${inc.location || ''} ${inc.occurTime ? '· ' + inc.occurTime : ''}</div>
 </div>
 <div style="padding:10px 12px;background:#fff">
 <div style="display:flex;gap:12px;font-size:12px;color:#555;margin-bottom:8px">
 <div>🌀 <span style="color:${disasterColor};font-weight:600">${DisasterTypeLabel[inc.disasterType] ?? inc.disasterType}</span></div>
 <div>⚡ <strong>${IncidentLevelLabel[inc.incidentLevel] ?? inc.incidentLevel}</strong></div>
 <div>📊 <span style="color:${statusColor};font-weight:600">${IncidentStatusLabel[inc.status] ?? inc.status}</span></div>
 </div>
 <div style="border-top:1px dashed #eee;padding-top:8px;margin-top:4px">
 <div style="font-size:11px;color:#999;margin-bottom:4px">处置进度</div>
 <div style="background:#f0f2f5;border-radius:4px;height:8px;overflow:hidden;margin-bottom:6px">
 <div style="width:${progress}%;height:100%;background:${progressColor};border-radius:4px;transition:width 0.3s"></div>
 </div>
 <div style="display:flex;justify-content:space-between;font-size:11px;color:#666">
 <span>📋 方案：${planStatus}</span>
 <span>🚨 调度：${dispatchStatus}</span>
 </div>
 ${deathInfo}
 ${lossInfo}
 </div>
 </div>
 </div>
 `;
}
function createMarkers(): void {
 if (!map)
 return;
 if (markerGroup) {
 markerGroup.clearLayers();
 }
 else {
 markerGroup = L.layerGroup().addTo(map);
 }
 const valid = validIncidents.value;
 if (valid.length === 0)
 return;
 const bounds: L.LatLngTuple[] = [];
 for (const inc of valid) {
 const statusColor = getStatusColor(inc.status);
 const disasterColor = getDisasterColor(inc.disasterType);
 const isProcessing = inc.status === 'processing';
 const icon = L.divIcon({
 className: 'incident-marker',
 html: `
 <div style="position:relative">
 <div style="
 width:18px;height:18px;border-radius:50%;
 background:${statusColor};
 border:2px solid ${disasterColor};
 box-shadow:0 0 10px ${statusColor}cc;
 ${isProcessing ? 'animation:pulse 1.5s infinite;' : ''}
 display:flex;align-items:center;justify-content:center;
 font-size:10px;color:#fff;font-weight:bold;">
 ${inc.incidentLevel || ''}
 </div>
 ${isProcessing ? `<div style="position:absolute;top:-4px;left:-4px;width:26px;height:26px;border-radius:50%;border:2px solid ${statusColor};opacity:0.4;animation:ripple 1.5s infinite"></div>` : ''}
 </div>
 `,
 iconSize: [18, 18],
 iconAnchor: [9, 9],
 });
 const lat = inc.latitude!;
 const lng = inc.longitude!;
 bounds.push([lat, lng]);
 const marker = L.marker([lat, lng], { icon });
 marker.bindTooltip(createTooltip(inc), {
 direction: 'top',
 offset: [0, -10],
 opacity: 1,
 sticky: true,
 });
 marker.on('click', () => {
 emit('select', inc);
 });
 markerGroup!.addLayer(marker);
 }
 if (bounds.length > 0) {
 map.fitBounds(L.latLngBounds(bounds), { padding: [80, 80], maxZoom: 12 });
 }
}
onMounted(() => {
 if (!mapContainer.value)
 return;
 map = L.map(mapContainer.value, {
 center: props.center,
 zoom: props.zoom,
 zoomControl: true,
 attributionControl: false,
 });
 L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
 maxZoom: 18,
 }).addTo(map);
 legendControl = createLegend().addTo(map);
 createMarkers();
 setTimeout(() => map?.invalidateSize(), 200);
});
onUnmounted(() => {
 if (markerGroup) {
 markerGroup.clearLayers();
 markerGroup = null;
 }
 if (legendControl && map) {
 map.removeControl(legendControl);
 legendControl = null;
 }
 if (map) {
 map.remove();
 map = null;
 }
});
watch(() => props.incidents, () => {
 createMarkers();
}, { deep: true });
</script>

<template>
  <div ref="mapContainer" class="incident-map">
    <div v-if="validIncidents.length === 0 && incidents.length > 0" class="incident-map__empty">
      <div class="incident-map__empty-icon">📍</div>
      <div class="incident-map__empty-text">暂无坐标数据</div>
      <div class="incident-map__empty-hint">请在灾情上报时填写详细地址，系统将自动获取坐标</div>
    </div>
    <div v-else-if="incidents.length === 0" class="incident-map__empty">
      <div class="incident-map__empty-icon">🗺️</div>
      <div class="incident-map__empty-text">暂无灾情数据</div>
      <div class="incident-map__empty-hint">请先上报灾情信息</div>
    </div>
    <div v-else-if="validIncidents.length < incidents.length" class="incident-map__count">
      共 {{ incidents.length }} 条灾情，{{ validIncidents.length }} 条已在地图上显示
    </div>
    <div v-else class="incident-map__count">
      共 {{ incidents.length }} 条灾情已在地图上显示
    </div>
  </div>
</template>

<style scoped>
.incident-map {
  width: 100%;
  height: 100%;
  min-height: 300px;
  position: relative;
}

.incident-map__empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  z-index: 1000;
  pointer-events: none;
}

.incident-map__empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.incident-map__empty-text {
  font-size: 14px;
  font-weight: 500;
}

.incident-map__empty-hint {
  font-size: 12px;
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.4);
}

.incident-map__count {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(13, 27, 62, 0.85);
  color: #fff;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  z-index: 1000;
  border: 1px solid rgba(64, 158, 255, 0.3);
}
</style>

<style>
.leaflet-container {
  background: #0d1b3e;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.map-legend {
  background: rgba(13, 27, 62, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  padding: 10px 14px;
  color: #fff;
  font-size: 12px;
  min-width: 140px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.map-legend__title {
  font-weight: 600;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.map-legend__items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.map-legend__item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.map-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 4px rgba(255, 255, 255, 0.3);
}

.map-legend__label {
  color: rgba(255, 255, 255, 0.85);
}

.leaflet-tooltip {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
}

.leaflet-tooltip-top:before {
  display: none;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.1); }
}

@keyframes ripple {
  0% { transform: scale(1); opacity: 0.6; }
  100% { transform: scale(1.8); opacity: 0; }
}
</style>
