<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, reactive, ref} from 'vue';
import {useRoute} from 'vue-router';
import http from '../api/http';
import type {ScreenSchema, WidgetConfig} from '../types/screen';

// ==================== 栅格系统配置 ====================
const GRID_SIZE = 20;  // 栅格大小（像素）
const SNAP_DISTANCE = 10;  // 吸附距离阈值（像素）

interface AlignmentLine {
  type: 'vertical' | 'horizontal';
  position: number;  // px 坐标
  start: number;
  end: number;
}

interface DragState {
  widgetId: string | null;
  startX: number;
  startY: number;
  offsetX: number;
  offsetY: number;
  isDragging: boolean;
}

// ==================== 栅格对齐函数 ====================
function snapToGrid(value: number): number {
  return Math.round(value / GRID_SIZE) * GRID_SIZE;
}

function snapToGridRange(value: number, snap: number = GRID_SIZE): number {
  return Math.round(value / snap) * snap;
}

// ==================== 对齐检测算法 ====================
function detectAlignment(widget: WidgetConfig, allWidgets: WidgetConfig[]): AlignmentLine[] {
  const lines: AlignmentLine[] = [];
  const otherWidgets = allWidgets.filter(w => w.id !== widget.id);

  if (otherWidgets.length === 0) return lines;

  const w = widget.position;
  const tolerance = SNAP_DISTANCE;

  otherWidgets.forEach(other => {
    const o = other.position;

    // 左对齐
    if (Math.abs(w.x - o.x) < tolerance) {
      lines.push({
        type: 'vertical',
        position: w.x,
        start: Math.min(w.y, o.y) - 10,
        end: Math.max(w.y + w.h, o.y + o.h) + 10
      });
    }

    // 右对齐
    if (Math.abs(w.x + w.w - (o.x + o.w)) < tolerance) {
      const pos = w.x + w.w;
      lines.push({
        type: 'vertical',
        position: pos,
        start: Math.min(w.y, o.y) - 10,
        end: Math.max(w.y + w.h, o.y + o.h) + 10
      });
    }

    // 顶部对齐
    if (Math.abs(w.y - o.y) < tolerance) {
      lines.push({
        type: 'horizontal',
        position: w.y,
        start: Math.min(w.x, o.x) - 10,
        end: Math.max(w.x + w.w, o.x + o.w) + 10
      });
    }

    // 底部对齐
    if (Math.abs(w.y + w.h - (o.y + o.h)) < tolerance) {
      const pos = w.y + w.h;
      lines.push({
        type: 'horizontal',
        position: pos,
        start: Math.min(w.x, o.x) - 10,
        end: Math.max(w.x + w.w, o.x + o.w) + 10
      });
    }

    // 水平居中对齐
    const wCenterX = w.x + w.w / 2;
    const oCenterX = o.x + o.w / 2;
    if (Math.abs(wCenterX - oCenterX) < tolerance) {
      lines.push({
        type: 'vertical',
        position: wCenterX,
        start: Math.min(w.y, o.y) - 10,
        end: Math.max(w.y + w.h, o.y + o.h) + 10
      });
    }

    // 垂直居中对齐
    const wCenterY = w.y + w.h / 2;
    const oCenterY = o.y + o.h / 2;
    if (Math.abs(wCenterY - oCenterY) < tolerance) {
      lines.push({
        type: 'horizontal',
        position: wCenterY,
        start: Math.min(w.x, o.x) - 10,
        end: Math.max(w.x + w.w, o.x + o.w) + 10
      });
    }
  });

  return lines;
}

// ==================== 状态管理 ====================
// 路由参数中的大屏 code
const route = useRoute();
const code = route.params.code as string;

// 大屏 schema（默认 1920x1080，可在属性面板修改）
const screen = reactive<ScreenSchema>({
  code,
  name: `${code} 设计中`,
  canvasConfig: {
    width: 1920,
    height: 1080,
    bgColor: '#020617'
  },
  widgets: []
});

// 当前选中的组件
const selectedWidgetId = ref<string | null>(null);

// 拖动状态
const dragState = reactive<DragState>({
  widgetId: null,
  startX: 0,
  startY: 0,
  offsetX: 0,
  offsetY: 0,
  isDragging: false
});

// 对齐线
const alignmentLines = ref<AlignmentLine[]>([]);

// ==================== 缩放功能 ====================
const zoom = ref(1);  // 缩放等级 (1 = 100%)
const MIN_ZOOM = 0.1;   // 最小缩放 10% (确保大屏完整显示)
const MAX_ZOOM = 3;     // 最大缩放 300%
const ZOOM_STEP = 0.1;  // 每次缩放步长 10%

// ==================== 面板隐藏状态 ====================
const showLeftSidebar = ref(true);   // 显示左侧组件面板
const showRightSidebar = ref(true);  // 显示右侧属性面板

// ==================== 画布平移模式 ====================
const canvasPanMode = ref(false);    // 是否进入平移模式（按空格时）
const canvasOffset = ref({ x: 0, y: 0 });  // 画布偏移量
const panStart = ref({ x: 0, y: 0 });      // 开始平移时的鼠标位置
const isPanning = ref(false);        // 正在平移中

// ==================== 组件 Resize 模式 ====================
interface ResizeState {
  widgetId: string | null;
  handle: 'tl' | 'tr' | 'bl' | 'br' | 't' | 'b' | 'l' | 'r' | null;  // resize handle 位置
  startX: number;
  startY: number;
  startWidth: number;
  startHeight: number;
  startLeft: number;
  startTop: number;
}

const resizeState = reactive<ResizeState>({
  widgetId: null,
  handle: null,
  startX: 0,
  startY: 0,
  startWidth: 0,
  startHeight: 0,
  startLeft: 0,
  startTop: 0
});

function zoomIn() {
  zoom.value = Math.min(MAX_ZOOM, zoom.value + ZOOM_STEP);
}

function zoomOut() {
  zoom.value = Math.max(MIN_ZOOM, zoom.value - ZOOM_STEP);
}

function resetZoom() {
  zoom.value = 1;
}

function fitToScreen() {
  if (!canvasRef.value) return;
  const wrapper = canvasRef.value.parentElement?.parentElement;  // 获取滚动容器的父容器
  if (!wrapper) return;

  const wrapperWidth = wrapper.clientWidth;
  const wrapperHeight = wrapper.clientHeight;
  const canvasWidth = screen.canvasConfig?.width || 1920;
  const canvasHeight = screen.canvasConfig?.height || 1080;

  const scaleX = wrapperWidth / canvasWidth;
  const scaleY = wrapperHeight / canvasHeight;
  zoom.value = Math.min(scaleX, scaleY, MAX_ZOOM) * 0.95;  // 留一点边距
}

import { widgetRegistry } from '../widgets';

// 导入所有组件用于设计器中的实时预览
import StatCard from '../widgets/StatCard/StatCard.vue';
import LineChart from '../widgets/LineChart/LineChart.vue';
import BarChart from '../widgets/BarChart/BarChart.vue';
import PieChart from '../widgets/PieChart/PieChart.vue';
import NumberFlop from '../widgets/NumberFlop/NumberFlop.vue';
import ProgressBar from '../widgets/ProgressBar/ProgressBar.vue';
import Table from '../widgets/Table/Table.vue';
import Map from '../widgets/Map/Map.vue';

// 组件类型到 Vue 组件的映射
const widgetComponentMap: Record<string, any> = {
  'stat-card': StatCard,
  'line-chart': LineChart,
  'bar-chart': BarChart,
  'pie-chart': PieChart,
  'number-flop': NumberFlop,
  'progress-bar': ProgressBar,
  'table': Table,
  'map': Map
};

const palette = widgetRegistry.map(w => ({
  type: w.type,
  name: w.name,
  defaultProps: w.schema.defaultProps,
  defaultSize: w.schema.defaultSize
}));

// 初始化时尝试从后端加载已有 schema（如果有）
async function loadScreen() {
  try {
    const resp = await http.get(`/screens/${code}`);
    Object.assign(screen, resp.data);
  } catch (e) {
    // 如果没查到就用默认的，不报错
    console.warn('未找到已有大屏，使用默认新建模式', e);
  }
}

onMounted(() => {
  loadScreen();
  // 延迟以确保 DOM 完全渲染
  setTimeout(() => {
    fitToScreen();
  }, 100);

  // 添加全局键盘事件监听
  window.addEventListener('keydown', onKeyDown);
  window.addEventListener('keyup', onKeyUp);
});

onBeforeUnmount(() => {
  // 清理事件监听
  window.removeEventListener('keydown', onKeyDown);
  window.removeEventListener('keyup', onKeyUp);
});

/**
 * 键盘按下：检测空格键进入平移模式
 */
function onKeyDown(event: KeyboardEvent) {
  if (event.code === 'Space' && !canvasPanMode.value) {
    event.preventDefault();
    canvasPanMode.value = true;
  }
}

/**
 * 键盘释放：检测空格键退出平移模式
 */
function onKeyUp(event: KeyboardEvent) {
  if (event.code === 'Space' && canvasPanMode.value) {
    event.preventDefault();
    canvasPanMode.value = false;
    isPanning.value = false;
  }
}

/**
 * 鼠标按下：记录平移起始位置
 */
function onCanvasMouseDown(event: MouseEvent) {
  if (!canvasPanMode.value) return;

  isPanning.value = true;
  panStart.value = { x: event.clientX, y: event.clientY };
}

/**
 * 鼠标移动：实时计算平移偏移
 */
function onCanvasMouseMove(event: MouseEvent) {
  if (!isPanning.value || !canvasPanMode.value) return;

  const deltaX = event.clientX - panStart.value.x;
  const deltaY = event.clientY - panStart.value.y;

  canvasOffset.value.x += deltaX;
  canvasOffset.value.y += deltaY;

  panStart.value = { x: event.clientX, y: event.clientY };
}

/**
 * 鼠标释放：结束平移
 */
function onCanvasMouseUp() {
  isPanning.value = false;
}

// ==================== Resize 处理 ====================

/**
 * 开始 Resize 组件
 */
function onResizeStart(event: MouseEvent, widgetId: string, handle: ResizeState['handle']) {
  event.stopPropagation();
  event.preventDefault();

  const widget = screen.widgets.find(w => w.id === widgetId);
  if (!widget) return;

  resizeState.widgetId = widgetId;
  resizeState.handle = handle;
  resizeState.startX = event.clientX;
  resizeState.startY = event.clientY;
  resizeState.startWidth = widget.position.w;
  resizeState.startHeight = widget.position.h;
  resizeState.startLeft = widget.position.x;
  resizeState.startTop = widget.position.y;
}

/**
 * 处理 Resize 拖动
 */
function onCanvasResizeMove(event: MouseEvent) {
  if (!resizeState.widgetId || !resizeState.handle) return;

  const widget = screen.widgets.find(w => w.id === resizeState.widgetId);
  if (!widget) return;

  const deltaX = event.clientX - resizeState.startX;
  const deltaY = event.clientY - resizeState.startY;

  // 根据 zoom 调整偏移
  const scaledDeltaX = deltaX / zoom.value;
  const scaledDeltaY = deltaY / zoom.value;

  const handle = resizeState.handle;
  let newX = resizeState.startLeft;
  let newY = resizeState.startTop;
  let newW = resizeState.startWidth;
  let newH = resizeState.startHeight;

  // 计算新的宽高和位置
  if (handle.includes('l')) {
    // 左边拖动
    newX = snapToGrid(resizeState.startLeft + scaledDeltaX);
    newW = resizeState.startWidth - (newX - resizeState.startLeft);
  }
  if (handle.includes('r')) {
    // 右边拖动
    newW = snapToGrid(resizeState.startWidth + scaledDeltaX);
  }
  if (handle.includes('t')) {
    // 顶部拖动
    newY = snapToGrid(resizeState.startTop + scaledDeltaY);
    newH = resizeState.startHeight - (newY - resizeState.startTop);
  }
  if (handle.includes('b')) {
    // 底部拖动
    newH = snapToGrid(resizeState.startHeight + scaledDeltaY);
  }

  // 最小尺寸限制
  const MIN_SIZE = 40;
  newW = Math.max(MIN_SIZE, newW);
  newH = Math.max(MIN_SIZE, newH);

  // 限制在 canvas 范围内
  const canvasWidth = screen.canvasConfig?.width || 1920;
  const canvasHeight = screen.canvasConfig?.height || 1080;
  newX = Math.max(0, Math.min(newX, canvasWidth - newW));
  newY = Math.max(0, Math.min(newY, canvasHeight - newH));

  // 更新组件
  widget.position.x = newX;
  widget.position.y = newY;
  widget.position.w = newW;
  widget.position.h = newH;
}

/**
 * 结束 Resize
 */
function onCanvasResizeEnd() {
  resizeState.widgetId = null;
  resizeState.handle = null;
}

// 画布 DOM 引用，用于计算 drop 坐标
const canvasRef = ref<HTMLElement | null>(null);

// 拖拽开始：把组件类型塞进 dataTransfer
function onDragStart(event: DragEvent, type: string) {
  event.dataTransfer?.setData('widget-type', type);
}

// 画布允许放置
function onCanvasDragOver(event: DragEvent) {
  event.preventDefault();
}

// 画布放置：根据鼠标位置创建一个 widget
function onCanvasDrop(event: DragEvent) {
  event.preventDefault();
  const type = event.dataTransfer?.getData('widget-type');
  if (!type || !canvasRef.value) return;

  const rect = canvasRef.value.getBoundingClientRect();
  // 考虑缩放因子计算真实位置
  const x = (event.clientX - rect.left) / zoom.value;
  const y = (event.clientY - rect.top) / zoom.value;

  const paletteItem = palette.find((p) => p.type === type);
  if (!paletteItem) return;

  const id = `${type}-${Date.now()}`;

  const widget: WidgetConfig = {
    id,
    type,
    props: {...paletteItem.defaultProps},
    position: {
      x: Math.round(x),
      y: Math.round(y),
      w: paletteItem.defaultSize.w,
      h: paletteItem.defaultSize.h
    },
    dataBinding: {
      mode: 'http',
      api: '/api/data/online-users',
      interval: 5000
    }
  };

  screen.widgets.push(widget);
  selectedWidgetId.value = id;
}

// 选中组件
function selectWidget(widgetId: string) {
  selectedWidgetId.value = widgetId;
}

// 当前选中的组件对象
const selectedWidget = computed(() =>
    screen.widgets.find((w) => w.id === selectedWidgetId.value) || null
);

// 根据选中组件类型获取对应的 schema
const selectedWidgetSchema = computed(() => {
  if (!selectedWidget.value) return null;
  const widget = widgetRegistry.find(w => w.type === selectedWidget.value?.type);
  return widget?.schema || null;
});

// 删除组件
function removeSelectedWidget() {
  if (!selectedWidgetId.value) return;
  const idx = screen.widgets.findIndex((w) => w.id === selectedWidgetId.value);
  if (idx >= 0) {
    screen.widgets.splice(idx, 1);
    selectedWidgetId.value = null;
  }
}

// ==================== 已放置组件的拖动处理 ====================

/**
 * 组件拖动开始
 */
function onWidgetDragStart(event: DragEvent, widgetId: string) {
  event.stopPropagation();
  event.dataTransfer!.effectAllowed = 'move';

  dragState.widgetId = widgetId;
  dragState.startX = event.clientX;
  dragState.startY = event.clientY;
  dragState.isDragging = true;

  const widget = screen.widgets.find(w => w.id === widgetId);
  if (widget) {
    dragState.offsetX = widget.position.x;
    dragState.offsetY = widget.position.y;
  }

  selectWidget(widgetId);
}

/**
 * 组件拖动过程中 - 实时显示对齐线
 */
function onWidgetDragOver(event: DragEvent) {
  event.preventDefault();
  event.dataTransfer!.dropEffect = 'move';

  if (!dragState.isDragging || !dragState.widgetId || !canvasRef.value) return;

  const widget = screen.widgets.find(w => w.id === dragState.widgetId);
  if (!widget) return;

  // 计算新的位置（相对于 canvas，考虑缩放）
  const rect = canvasRef.value.getBoundingClientRect();
  const deltaX = event.clientX - dragState.startX;
  const deltaY = event.clientY - dragState.startY;

  // 根据缩放倍率调整偏移
  const scaledDeltaX = deltaX / zoom.value;
  const scaledDeltaY = deltaY / zoom.value;

  const newX = snapToGrid(dragState.offsetX + scaledDeltaX);
  const newY = snapToGrid(dragState.offsetY + scaledDeltaY);

  // 限制在 canvas 范围内（使用动态 canvas 尺寸）
  const canvasWidth = screen.canvasConfig?.width || 1920;
  const canvasHeight = screen.canvasConfig?.height || 1080;
  widget.position.x = Math.max(0, Math.min(newX, canvasWidth - widget.position.w));
  widget.position.y = Math.max(0, Math.min(newY, canvasHeight - widget.position.h));

  // 检测并显示对齐线
  alignmentLines.value = detectAlignment(widget, screen.widgets);
}

/**
 * 处理鼠标滚轮缩放
 */
function onCanvasWheel(event: WheelEvent) {
  if (!event.ctrlKey && !event.metaKey) return;
  event.preventDefault();

  // 向上滚动放大，向下滚动缩小
  if (event.deltaY < 0) {
    zoomIn();
  } else {
    zoomOut();
  }
}

/**
 * 组件拖动结束 - 清除对齐线
 */
function onWidgetDragEnd(event: DragEvent) {
  event.preventDefault();
  dragState.isDragging = false;
  alignmentLines.value = [];
}

// ==================== 数据测试功能 ====================

interface TestResult {
  widgetId: string;
  widgetType: string;
  api: string;
  status: 'success' | 'error' | 'pending';
  data?: any;
  error?: string;
}

const testResults = ref<TestResult[]>([]);
const showTestResults = ref(false);
const isTesting = ref(false);

/**
 * 测试单个组件的数据接口
 */
async function testWidgetData() {
  if (!selectedWidget.value || !selectedWidget.value.dataBinding?.api) {
    alert('请先配置数据接口');
    return;
  }

  const widget = selectedWidget.value;
  const api = widget.dataBinding.api.replace(/^\/api/, '');

  try {
    const resp = await http.get(api);
    alert(
      `✅ 接口测试成功\n\n接口: ${widget.dataBinding.api}\n\n` +
      `返回数据: ${JSON.stringify(resp.data, null, 2)}`
    );
  } catch (e: any) {
    alert(
      `❌ 接口测试失败\n\n接口: ${widget.dataBinding.api}\n\n` +
      `错误信息: ${e.message || '未知错误'}`
    );
    console.error('测试失败详情:', e);
  }
}

/**
 * 全局测试所有组件的数据接口
 */
async function testAllWidgetData() {
  if (screen.widgets.length === 0) {
    alert('画布中没有组件');
    return;
  }

  isTesting.value = true;
  testResults.value = screen.widgets
    .filter(w => w.dataBinding?.api)
    .map(w => ({
      widgetId: w.id,
      widgetType: w.type,
      api: w.dataBinding!.api,
      status: 'pending'
    }));

  for (const result of testResults.value) {
    try {
      const api = result.api.replace(/^\/api/, '');
      const resp = await http.get(api);
      result.status = 'success';
      result.data = resp.data;
    } catch (e: any) {
      result.status = 'error';
      result.error = e.message || '未知错误';
    }
  }

  isTesting.value = false;
  showTestResults.value = true;
}

/**
 * 关闭测试结果面板
 */
function closeTestResults() {
  showTestResults.value = false;
}

// ==================== 保存功能 ====================

// 保存 schema 到后端
const saving = ref(false);

async function saveScreen() {
  saving.value = true;
  try {
    const resp = await http.post('/screens', screen);
    Object.assign(screen, resp.data);
    alert('保存成功');
  } catch (e) {
    console.error('保存失败', e);
    alert('保存失败，请查看控制台日志');
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="designer-root">
    <!-- 左侧：组件面板 -->
    <aside v-show="showLeftSidebar" class="designer-sidebar">
      <div class="designer-sidebar-header">
        <h3>组件面板</h3>
      </div>
      <div class="designer-sidebar-body">
        <div
            v-for="item in palette"
            :key="item.type"
            class="widget-palette-item"
            draggable="true"
            @dragstart="onDragStart($event, item.type)"
        >
          {{ item.name }}
        </div>
      </div>
    </aside>

    <!-- 中间：画布 -->
    <main class="designer-canvas-wrapper">
      <header class="designer-toolbar">
        <!-- 左侧：标题 + 面板切换 -->
        <div class="toolbar-left">
          <div class="title">
            {{ screen.name }}
          </div>
          <div class="panel-toggles">
            <button
              class="toggle-btn"
              :class="{ inactive: !showLeftSidebar }"
              @click="showLeftSidebar = !showLeftSidebar"
              title="切换左侧面板"
            >
              ◀ 组件
            </button>
            <button
              class="toggle-btn"
              :class="{ inactive: !showRightSidebar }"
              @click="showRightSidebar = !showRightSidebar"
              title="切换右侧面板"
            >
              属性 ▶
            </button>
          </div>
        </div>

        <!-- 右侧：操作按钮 -->
        <div class="toolbar-actions">
          <!-- 缩放控制 -->
          <div class="zoom-controls">
            <button class="zoom-btn" @click="zoomOut" title="缩小 (Ctrl+滚轮)">−</button>
            <div class="zoom-display">{{ Math.round(zoom * 100) }}%</div>
            <button class="zoom-btn" @click="zoomIn" title="放大 (Ctrl+滚轮)">+</button>
            <button class="zoom-btn" @click="resetZoom" title="重置缩放">100%</button>
            <button class="zoom-btn" @click="fitToScreen" title="适应屏幕">自适应</button>
          </div>

          <!-- 分割线 -->
          <div class="toolbar-divider"></div>

          <!-- 其他按钮 -->
          <button class="btn-test" @click="testAllWidgetData" :disabled="isTesting">
            {{ isTesting ? '测试中...' : '刷新全部接口数据' }}
          </button>
          <button @click="saveScreen" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
          <a :href="`/screen/${screen.code}`" target="_blank">
            <button>预览</button>
          </a>
        </div>
      </header>

      <div
          class="canvas-scroll-container"
          :class="{ 'pan-mode': canvasPanMode, 'panning': isPanning, 'resizing': resizeState.widgetId }"
          @dragover="onCanvasDragOver"
          @drop="onCanvasDrop"
          @wheel="onCanvasWheel"
          @mousedown="onCanvasMouseDown"
          @mousemove="onCanvasMouseMove"
          @mouseup="onCanvasMouseUp"
          @mouseleave="onCanvasMouseUp"
      >
        <!-- 处理全局 resize 事件 -->
        <div
            v-if="resizeState.widgetId"
            class="resize-handler"
            @mousemove="onCanvasResizeMove"
            @mouseup="onCanvasResizeEnd"
            @mouseleave="onCanvasResizeEnd"
        ></div>
        <div
            class="designer-canvas"
            :style="{
            width: screen.canvasConfig?.width + 'px',
            height: screen.canvasConfig?.height + 'px',
            backgroundColor: screen.canvasConfig?.bgColor || '#020617',
            transform: `translate(${canvasOffset.x}px, ${canvasOffset.y}px) scale(${zoom})`,
            transformOrigin: 'top left'
          }"
            ref="canvasRef"
        >
        <!-- 对齐线 -->
        <div
            v-for="(line, idx) in alignmentLines"
            :key="`align-${idx}`"
            class="alignment-line"
            :class="{ 'align-vertical': line.type === 'vertical', 'align-horizontal': line.type === 'horizontal' }"
            :style="
            line.type === 'vertical'
              ? {
                left: line.position + 'px',
                top: line.start + 'px',
                height: (line.end - line.start) + 'px'
              }
              : {
                top: line.position + 'px',
                left: line.start + 'px',
                width: (line.end - line.start) + 'px'
              }
          "
        ></div>

        <!-- 栅格背景（可选，增强视觉效果） -->
        <div class="canvas-grid"></div>

        <!-- 组件 -->
        <div
            v-for="w in screen.widgets"
            :key="w.id"
            class="designer-widget"
            :class="{ active: w.id === selectedWidgetId, dragging: dragState.widgetId === w.id }"
            :style="{
            left: w.position.x + 'px',
            top: w.position.y + 'px',
            width: w.position.w + 'px',
            height: w.position.h + 'px'
          }"
            draggable="true"
            @click.stop="selectWidget(w.id)"
            @dragstart="onWidgetDragStart($event, w.id)"
            @dragover="onWidgetDragOver"
            @dragend="onWidgetDragEnd"
        >
          <!-- 组件标题栏 -->
          <div class="designer-widget-title">
            {{ w.type }}（{{ w.props?.title || '未命名' }}）
          </div>

          <!-- 实际组件内容 -->
          <div class="designer-widget-content">
            <component
                :is="widgetComponentMap[w.type]"
                v-if="widgetComponentMap[w.type]"
                v-bind="w.props"
                :data-binding="w.dataBinding"
            />
            <div v-else class="widget-not-found">
              组件类型不支持: {{ w.type }}
            </div>
          </div>

          <!-- 位置坐标提示（拖动时显示） -->
          <div v-if="dragState.widgetId === w.id" class="position-hint">
            {{ w.position.x }}, {{ w.position.y }}
          </div>

          <!-- Resize Handles（选中时显示） -->
          <template v-if="w.id === selectedWidgetId">
            <!-- 四个角 -->
            <div class="resize-handle resize-handle-tl" @mousedown.stop="onResizeStart($event, w.id, 'tl')" title="左上角"></div>
            <div class="resize-handle resize-handle-tr" @mousedown.stop="onResizeStart($event, w.id, 'tr')" title="右上角"></div>
            <div class="resize-handle resize-handle-bl" @mousedown.stop="onResizeStart($event, w.id, 'bl')" title="左下角"></div>
            <div class="resize-handle resize-handle-br" @mousedown.stop="onResizeStart($event, w.id, 'br')" title="右下角"></div>

            <!-- 四条边中点 -->
            <div class="resize-handle resize-handle-t" @mousedown.stop="onResizeStart($event, w.id, 't')" title="上边"></div>
            <div class="resize-handle resize-handle-b" @mousedown.stop="onResizeStart($event, w.id, 'b')" title="下边"></div>
            <div class="resize-handle resize-handle-l" @mousedown.stop="onResizeStart($event, w.id, 'l')" title="左边"></div>
            <div class="resize-handle resize-handle-r" @mousedown.stop="onResizeStart($event, w.id, 'r')" title="右边"></div>
          </template>
        </div>
        </div>
      </div>
    </main>

    <!-- 右侧：属性面板 -->
    <aside v-show="showRightSidebar" class="designer-props">
      <div class="designer-props-header">
        <h3>属性面板</h3>
      </div>

      <div class="designer-props-body">
        <!-- 画布设置 (总是显示) -->
        <div class="form-section canvas-settings">
          <div class="form-section-title">画布设置</div>

          <div class="flex-row">
            <div class="form-item flex-1">
              <label>宽度 (px)</label>
              <input
                  v-model.number="screen.canvasConfig.width"
                  type="number"
                  class="form-input"
                  min="800"
                  max="4000"
              />
            </div>
            <div class="form-item flex-1">
              <label>高度 (px)</label>
              <input
                  v-model.number="screen.canvasConfig.height"
                  type="number"
                  class="form-input"
                  min="600"
                  max="3000"
              />
            </div>
          </div>

          <div class="form-item">
            <label>背景色</label>
            <input
                v-model="screen.canvasConfig.bgColor"
                type="color"
                class="form-color"
            />
          </div>
        </div>

        <!-- 分割线 -->
        <div class="form-section-divider"></div>

        <!-- 组件属性 (条件显示) -->
        <div v-if="selectedWidget">
          <!-- 基础信息 -->
          <div class="form-section">
            <div class="form-section-title">基础信息</div>

          <div class="form-item">
            <label>组件ID</label>
            <div class="plain">{{ selectedWidget.id }}</div>
          </div>

          <div class="form-item">
            <label>组件类型</label>
            <div class="plain">{{ selectedWidget.type }}</div>
          </div>
        </div>

        <!-- 动态属性表单 -->
        <div v-if="selectedWidgetSchema" class="form-section">
          <div class="form-section-title">组件属性</div>

          <div
              v-for="formField in selectedWidgetSchema.form"
              :key="formField.prop"
              class="form-item"
          >
            <label>{{ formField.label }}</label>

            <!-- 文本输入 -->
            <input
                v-if="formField.type === 'input'"
                v-model="selectedWidget.props[formField.prop]"
                type="text"
                class="form-input"
            />

            <!-- 数字输入 -->
            <input
                v-else-if="formField.type === 'number'"
                v-model.number="selectedWidget.props[formField.prop]"
                type="number"
                class="form-input"
            />

            <!-- 颜色选择 -->
            <input
                v-else-if="formField.type === 'color'"
                v-model="selectedWidget.props[formField.prop]"
                type="color"
                class="form-color"
            />

            <!-- 复选框 -->
            <input
                v-else-if="formField.type === 'checkbox'"
                v-model="selectedWidget.props[formField.prop]"
                type="checkbox"
                class="form-checkbox"
            />
          </div>
        </div>

        <!-- 位置和尺寸 -->
        <div class="form-section">
          <div class="form-section-title">位置 / 尺寸</div>

          <div class="flex-row">
            <div class="form-item flex-1">
              <label>X</label>
              <input v-model.number="selectedWidget.position.x" type="number" class="form-input"/>
            </div>
            <div class="form-item flex-1">
              <label>Y</label>
              <input v-model.number="selectedWidget.position.y" type="number" class="form-input"/>
            </div>
          </div>

          <div class="flex-row">
            <div class="form-item flex-1">
              <label>宽</label>
              <input v-model.number="selectedWidget.position.w" type="number" class="form-input"/>
            </div>
            <div class="form-item flex-1">
              <label>高</label>
              <input v-model.number="selectedWidget.position.h" type="number" class="form-input"/>
            </div>
          </div>
        </div>

        <!-- 数据绑定 -->
        <div class="form-section">
          <div class="form-section-title">数据绑定</div>

          <div class="form-item">
            <label>API 接口</label>
            <div class="flex-row api-input-row">
              <input
                  v-model="selectedWidget.dataBinding!.api"
                  type="text"
                  class="form-input"
                  placeholder="/api/data/..."
              />
              <button class="btn-test-widget" @click="testWidgetData" title="测试此接口"/>
            </div>
          </div>

          <div class="form-item">
            <label>刷新间隔 (ms)</label>
            <input
                v-model.number="selectedWidget.dataBinding!.interval"
                type="number"
                class="form-input"
                min="0"
            />
          </div>
        </div>

          <!-- 操作 -->
          <div class="form-actions">
            <button class="danger" @click="removeSelectedWidget">
              删除组件
            </button>
          </div>
        </div>

        <!-- 未选中组件时 -->
        <div v-else class="designer-props-empty">
          请选择一个组件进行编辑
        </div>
      </div>
    </aside>

    <!-- 模式指示器浮窗 -->
    <div class="mode-indicator" :class="{ active: canvasPanMode }">
      <span v-if="canvasPanMode" class="mode-text">
        {{ isPanning ? '移动中...' : '按住拖动移动' }}
      </span>
      <span v-else class="mode-text">按住空格移动画布</span>
    </div>

    <!-- 测试结果面板 -->
    <div v-if="showTestResults" class="test-results-modal">
      <div class="test-results-content">
        <div class="test-results-header">
          <h3>数据接口测试结果</h3>
          <button class="close-btn" @click="closeTestResults">✕</button>
        </div>

        <div class="test-results-body">
          <div v-if="testResults.length === 0" class="test-empty">
            没有配置数据接口的组件
          </div>

          <div v-else class="test-items">
            <div v-for="result in testResults" :key="result.widgetId" class="test-item">
              <div class="test-item-header">
                <span class="test-type">{{ result.widgetType }}</span>
                <span class="test-status" :class="{ success: result.status === 'success', error: result.status === 'error', pending: result.status === 'pending' }">
                  {{ result.status === 'success' ? '成功' : result.status === 'error' ? '失败' : '测试中' }}
                </span>
              </div>

              <div class="test-api">接口: {{ result.api }}</div>

              <div v-if="result.status === 'success'" class="test-data">
                <div class="data-label">返回数据:</div>
                <pre class="data-content">{{ JSON.stringify(result.data, null, 2) }}</pre>
              </div>

              <div v-if="result.status === 'error'" class="test-error">
                <div class="error-label">错误信息:</div>
                <div class="error-content">{{ result.error }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="test-results-footer">
          <button @click="closeTestResults" class="btn-close">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.designer-root {
  width: 100%;
  height: 100vh;
  display: grid;
  grid-template-columns: auto 1fr auto;
  grid-template-rows: 1fr;
  background: #020617;
  color: #e5e7eb;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI',
  sans-serif;
}

/* 左侧面板 */
.designer-sidebar {
  width: 220px !important;
  height: 100%;
  border-right: 1px solid #1f2933;
  display: flex;
  flex-direction: column;
  background: #030712;
  grid-row: 1 / -1;
  overflow: hidden;
}

/* 右侧面板 */
.designer-props {
  width: 260px !important;
  height: 100%;
  grid-row: 1 / -1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #1f2933;
  background: #020617;
}

/* 中间区域 */
main.designer-canvas-wrapper {
  grid-column: 2;
  grid-row: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.designer-sidebar-header {
  padding: 12px 16px;
  border-bottom: 1px solid #1f2933;
  flex-shrink: 0;
}

.designer-sidebar-body {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
  min-height: 0;
}

.widget-palette-item {
  padding: 8px 10px;
  margin-bottom: 8px;
  border-radius: 6px;
  background: #111827;
  cursor: grab;
  font-size: 14px;
}

.widget-palette-item:hover {
  background: #1f2937;
}

.designer-toolbar {
  height: 48px;
  border-bottom: 1px solid #1f2933;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #020617;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.designer-toolbar .title {
  font-size: 14px;
  opacity: 0.85;
  min-width: 150px;
}

.panel-toggles {
  display: flex;
  gap: 6px;
}

.toggle-btn {
  padding: 4px 8px;
  background: rgba(51, 65, 85, 0.6);
  border: 1px solid #334155;
  border-radius: 4px;
  color: #cbd5e1;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.toggle-btn:hover {
  background: #334155;
  border-color: #475569;
  color: #e5e7eb;
}

.toggle-btn.inactive {
  opacity: 0.5;
  background: rgba(51, 65, 85, 0.3);
}

.toggle-btn.inactive:hover {
  background: rgba(51, 65, 85, 0.5);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-actions button {
  padding: 6px 12px;
  background: #0ea5e9;
  border: none;
  border-radius: 4px;
  color: #0b1120;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.toolbar-actions button:hover:not(:disabled) {
  background: #06b6d4;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
}

.toolbar-actions button:disabled {
  background: #64748b;
  cursor: not-allowed;
  opacity: 0.6;
}

.toolbar-actions button.btn-test {
  background: #8b5cf6;
  color: #f3e8ff;
}

.toolbar-actions button.btn-test:hover:not(:disabled) {
  background: #a78bfa;
}

/* 缩放控制组 */
.zoom-controls {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 6px;
}

.zoom-btn {
  padding: 4px 8px;
  background: rgba(51, 65, 85, 0.6);
  border: 1px solid #334155;
  border-radius: 4px;
  color: #cbd5e1;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s;
  min-width: 32px;
}

.zoom-btn:hover {
  background: #334155;
  border-color: #475569;
  color: #e5e7eb;
}

.zoom-btn:active {
  transform: scale(0.95);
}

.zoom-display {
  font-size: 12px;
  color: #cbd5e1;
  font-weight: 600;
  min-width: 50px;
  text-align: center;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: rgba(148, 163, 184, 0.2);
  margin: 0 4px;
}

/* 画布滚动容器 */
.canvas-scroll-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;        /* ✅ 移除滚动条 */
  display: grid;
  place-items: center;
  padding: 12px;
  background: #0b0f1f;
  scroll-behavior: smooth;
  user-select: none;       /* 禁止文本选中 */
}

/* 平移模式光标 */
.canvas-scroll-container.pan-mode {
  cursor: grab;            /* 可平移 */
}

.canvas-scroll-container.pan-mode.panning {
  cursor: grabbing;        /* 正在平移 */
}

/* 选择模式光标 */
.canvas-scroll-container:not(.pan-mode) {
  cursor: default;
}

.designer-canvas {
  border: 1px solid #1f2937;
  border-radius: 8px;
  position: relative;
  background-size: cover;
  background-position: center;
  transition: transform 0.2s ease-out;
}

.canvas-grid {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.1) 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
}

.alignment-line {
  position: absolute;
  background: #ff4444;
  opacity: 0.8;
  z-index: 100;
  box-shadow: 0 0 6px rgba(255, 68, 68, 0.6);
}

.alignment-line.align-vertical {
  width: 1px;
  border-left: 1px dashed #ff4444;
}

.alignment-line.align-horizontal {
  height: 1px;
  border-top: 1px dashed #ff4444;
}

.designer-widget {
  position: absolute;
  border: 2px solid #334155;
  border-radius: 6px;
  box-sizing: border-box;
  background: rgba(15, 23, 42, 0.9);
  cursor: grab;
  overflow: hidden;
  transition: box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}

.designer-widget:hover {
  border-color: #475569;
}

.designer-widget.active {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 2px #0ea5e9;
}

.designer-widget.dragging {
  opacity: 0.8;
  box-shadow: 0 0 12px rgba(14, 165, 233, 0.5);
}

.designer-widget-title {
  font-size: 12px;
  padding: 4px 8px;
  border-bottom: 1px solid #1f2937;
  background: rgba(15, 23, 42, 0.95);
  user-select: none;
  flex-shrink: 0;
  font-weight: 600;
  color: #cbd5e1;
}

.designer-widget-content {
  flex: 1;
  overflow: auto;
  padding: 8px;
  min-height: 0;
}

.widget-not-found {
  font-size: 12px;
  color: #fca5a5;
  padding: 12px;
  text-align: center;
}

.position-hint {
  position: absolute;
  top: -20px;
  left: 0;
  font-size: 11px;
  color: #0ea5e9;
  background: rgba(15, 23, 42, 0.95);
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid #0ea5e9;
  white-space: nowrap;
  pointer-events: none;
}

/* Resize Handles */
.resize-handle {
  position: absolute;
  width: 8px;
  height: 8px;
  background: #0ea5e9;
  border: 1px solid #06b6d4;
  border-radius: 2px;
  z-index: 101;
  opacity: 0.8;
  transition: all 0.2s;
}

.resize-handle:hover {
  background: #06b6d4;
  opacity: 1;
  box-shadow: 0 0 8px rgba(14, 165, 233, 0.6);
  width: 10px;
  height: 10px;
  margin-left: -1px;
  margin-top: -1px;
}

/* 四个角 */
.resize-handle-tl {
  top: -4px;
  left: -4px;
  cursor: nwse-resize;
}

.resize-handle-tr {
  top: -4px;
  right: -4px;
  cursor: nesw-resize;
}

.resize-handle-bl {
  bottom: -4px;
  left: -4px;
  cursor: nesw-resize;
}

.resize-handle-br {
  bottom: -4px;
  right: -4px;
  cursor: nwse-resize;
}

/* 四条边 */
.resize-handle-t {
  top: -4px;
  left: 50%;
  transform: translateX(-50%);
  cursor: ns-resize;
}

.resize-handle-b {
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  cursor: ns-resize;
}

.resize-handle-l {
  top: 50%;
  left: -4px;
  transform: translateY(-50%);
  cursor: ew-resize;
}

.resize-handle-r {
  top: 50%;
  right: -4px;
  transform: translateY(-50%);
  cursor: ew-resize;
}

/* 全局 Resize 事件捕获层 */
.resize-handler {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  cursor: grabbing;
}

/* 右侧属性面板 */
.designer-props {
  border-left: 1px solid #1f2933;
  display: flex;
  flex-direction: column;
  background: #020617;
}

.designer-props-header {
  padding: 12px 16px;
  border-bottom: 1px solid #1f2933;
  flex-shrink: 0;
}

.designer-props-body {
  flex: 1;
  padding: 12px 16px;
  font-size: 13px;
  overflow-y: auto;
  min-height: 0;
}

.designer-props-empty {
  padding: 16px;
  font-size: 13px;
  opacity: 0.7;
}

.form-section {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #1f2937;
}

.form-section:last-of-type {
  border-bottom: none;
}

.form-section.canvas-settings {
  background: rgba(139, 92, 246, 0.05);
  border: 1px solid rgba(139, 92, 246, 0.2);
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 12px;
}

.form-section-divider {
  height: 1px;
  background: #1f2937;
  margin: 12px 0;
}

.form-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #cbd5e1;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  opacity: 0.9;
}

.form-item {
  margin-bottom: 10px;
}

.form-item label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  opacity: 0.8;
  color: #cbd5e1;
}

.form-input {
  width: 100%;
  box-sizing: border-box;
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #334155;
  background: rgba(2, 6, 23, 0.8);
  color: #e5e7eb;
  font-size: 13px;
  transition: all 0.2s;
}

.form-input:focus {
  border-color: #0ea5e9;
  outline: none;
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.1);
}

.form-color {
  width: 100%;
  height: 32px;
  padding: 2px;
  border-radius: 4px;
  border: 1px solid #334155;
  background: rgba(2, 6, 23, 0.8);
  cursor: pointer;
}

.form-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #0ea5e9;
}

.form-item .plain {
  font-size: 12px;
  opacity: 0.7;
  word-break: break-all;
  color: #94a3b8;
  padding: 6px;
  background: rgba(2, 6, 23, 0.6);
  border-radius: 4px;
  border: 1px solid #1f2937;
}

.flex-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 6px;
}

.flex-row > .form-item {
  flex: 1;
}

.flex-1 {
  flex: 1;
}

.form-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.form-actions .danger {
  background: #ef4444;
  border: none;
  color: #fee2e2;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.form-actions .danger:hover {
  background: #dc2626;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

/* API 输入框行 */
.api-input-row {
  display: flex !important;
  gap: 8px !important;
  align-items: center !important;
  margin-top: 6px !important;
}

.api-input-row .form-input {
  flex: 1;
  margin-bottom: 0 !important;
}

.btn-test-widget {
  padding: 6px 10px;
  background: #8b5cf6;
  border: 1px solid #8b5cf6;
  border-radius: 4px;
  color: #f3e8ff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.btn-test-widget:hover {
  background: #a78bfa;
  border-color: #a78bfa;
  transform: scale(1.05);
}

/* 测试结果面板 */
.test-results-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.test-results-content {
  background: #020617;
  border: 1px solid #1f2937;
  border-radius: 8px;
  width: 90%;
  max-width: 800px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  color: #e5e7eb;
}

.test-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #1f2937;
}

.test-results-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 20px;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(148, 163, 184, 0.1);
  color: #e5e7eb;
}

.test-results-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.test-empty {
  text-align: center;
  color: #94a3b8;
  padding: 32px 16px;
  font-size: 14px;
}

.test-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.test-item {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid #334155;
  border-radius: 6px;
  padding: 12px;
}

.test-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.test-type {
  background: #334155;
  color: #cbd5e1;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.test-status {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 4px;
}

.test-status.success {
  color: #86efac;
  background: rgba(34, 197, 94, 0.1);
}

.test-status.error {
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.1);
}

.test-status.pending {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.1);
}

.test-api {
  font-size: 12px;
  color: #cbd5e1;
  margin-bottom: 8px;
  padding: 6px;
  background: rgba(2, 6, 23, 0.4);
  border-radius: 4px;
  word-break: break-all;
}

.test-data,
.test-error {
  margin-top: 8px;
}

.data-label,
.error-label {
  font-size: 12px;
  color: #cbd5e1;
  font-weight: 600;
  margin-bottom: 6px;
}

.data-content {
  background: #0b1120;
  border: 1px solid #1f2937;
  border-radius: 4px;
  padding: 8px;
  margin: 0;
  font-size: 11px;
  color: #a1f6ff;
  overflow-x: auto;
  max-height: 200px;
}

.error-content {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 4px;
  padding: 8px;
  font-size: 12px;
  color: #fca5a5;
  word-break: break-word;
}

.test-results-footer {
  padding: 12px 16px;
  border-top: 1px solid #1f2937;
  display: flex;
  justify-content: flex-end;
}

.btn-close {
  padding: 6px 16px;
  background: #334155;
  border: 1px solid #475569;
  border-radius: 4px;
  color: #e5e7eb;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #475569;
  color: #fff;
}

/* 模式指示器浮窗 */
.mode-indicator {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 8px 16px;
  background: rgba(51, 65, 85, 0.9);
  border: 1px solid #334155;
  border-radius: 20px;
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  pointer-events: none;
  z-index: 100;
  backdrop-filter: blur(4px);
}

.mode-indicator.active {
  background: rgba(139, 92, 246, 0.9);
  border-color: #8b5cf6;
  color: #f3e8ff;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.mode-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>