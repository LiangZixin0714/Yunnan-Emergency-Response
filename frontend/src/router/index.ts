import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupRouterGuard } from './guard'

const MainLayout = () => import('@/layouts/MainLayout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/index.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register/index.vue'),
    meta: { title: '注册', requiresAuth: false },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { title: '无权限', requiresAuth: false },
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home/index.vue'),
        meta: { title: '首页', requiresAuth: true, icon: 'HomeFilled' },
      },
      {
        path: 'incident',
        name: 'Incident',
        redirect: '/incident/list',
        meta: { title: '灾情管理', requiresAuth: true, icon: 'Warning' },
        children: [
          {
            path: 'list',
            name: 'IncidentList',
            component: () => import('@/views/Incident/List.vue'),
            meta: { title: '事件列表', requiresAuth: true, icon: 'List' },
          },
          {
            path: 'report',
            name: 'IncidentReport',
            component: () => import('@/views/Incident/Report.vue'),
            meta: { title: '灾情上报', requiresAuth: true, icon: 'EditPen', roles: ['VIEWER'] },
          },
          {
            path: ':id',
            name: 'IncidentDetail',
            component: () => import('@/views/Incident/Detail.vue'),
            meta: { title: '事件详情', requiresAuth: true, hidden: true },
          },
        ],
      },
      {
        path: 'plan',
        name: 'Plan',
        component: () => import('@/views/Plan/index.vue'),
        meta: { title: 'AI方案生成', requiresAuth: true, icon: 'Document', roles: ['OPERATOR'] },
      },
      {
        path: 'resource',
        name: 'Resource',
        component: () => import('@/views/Resource/index.vue'),
        meta: { title: '资源调度', requiresAuth: true, icon: 'Box', roles: ['ADMIN', 'RESOURCE_MANAGER'] },
      },
      {
        path: 'resource-request/:incidentId',
        name: 'ResourceRequest',
        component: () => import('@/views/ResourceRequest/index.vue'),
        meta: { title: '资源申请', requiresAuth: true, hidden: true, roles: ['RESOURCE_MANAGER'] },
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/Knowledge/index.vue'),
        meta: { title: '知识库管理', requiresAuth: true, icon: 'Collection', roles: ['ADMIN'] },
      },
      {
        path: 'agent',
        name: 'Agent',
        component: () => import('@/views/Agent/index.vue'),
        meta: { title: 'Agent与审计', requiresAuth: true, icon: 'Monitor', roles: ['ADMIN'] },
      },
      {
        path: 'role-application',
        name: 'RoleApplication',
        component: () => import('@/views/RoleApplication/index.vue'),
        meta: { title: '角色申请', requiresAuth: true, icon: 'UserFilled', hidden: true },
      },
      {
        path: 'member',
        name: 'Member',
        component: () => import('@/views/Member/index.vue'),
        meta: { title: '成员管理', requiresAuth: true, icon: 'Avatar', roles: ['ADMIN'] },
      },
      {
        path: 'screen',
        name: 'Screen',
        component: () => import('@/views/Screen/index.vue'),
        meta: { title: '大屏展示', requiresAuth: true, icon: 'DataAnalysis', hidden: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

setupRouterGuard(router)

export default router
