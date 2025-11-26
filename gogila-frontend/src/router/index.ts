import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import ScreenRuntime from '../runtime/ScreenRuntime.vue';
import ScreenDesigner from '../designer/ScreenDesigner.vue'; // 新增

const routes: RouteRecordRaw[] = [
    {
        path: '/screen/:code',
        name: 'ScreenRuntime',
        component: ScreenRuntime
    },
    {
        path: '/designer/:code',
        name: 'ScreenDesigner',
        component: ScreenDesigner
    },
    {
        path: '/',
        redirect: '/screen/index-overview'
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;