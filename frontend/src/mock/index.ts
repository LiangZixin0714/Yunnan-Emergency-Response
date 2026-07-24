import type { ViteDevServer } from 'vite'

const MOCK_USERS: Record<string, any>[] = [
  { id: 1, username: 'admin', realName: '系统管理员', roleName: 'ADMIN', roleId: 1, status: 1, email: null, phone: null, createdAt: '2026-07-22T16:30:00', updatedAt: '2026-07-22T16:30:00' },
  { id: 2, username: 'operator', realName: '李指挥', roleName: 'OPERATOR', roleId: 2, status: 1, email: null, phone: null, createdAt: '2026-07-22T16:30:00', updatedAt: '2026-07-22T16:30:00' },
  { id: 3, username: 'resource', realName: '王资源', roleName: 'RESOURCE_MANAGER', roleId: 3, status: 1, email: null, phone: null, createdAt: '2026-07-22T16:30:00', updatedAt: '2026-07-22T16:30:00' },
  { id: 4, username: 'viewer', realName: '张信息员', roleName: 'VIEWER', roleId: 4, status: 1, email: null, phone: null, createdAt: '2026-07-22T16:30:00', updatedAt: '2026-07-22T16:30:00' },
]

const MOCK_PASSWORDS: Record<string, string> = { admin: '123456', operator: '123456', resource: '123456', viewer: '123456' }
const MOCK_TOKEN = 'mock-jwt-token-2026'

const DISASTER_TYPES = ['earthquake', 'mudslide', 'flood', 'drought', 'landslide', 'fire', 'other']
const INCIDENT_LEVELS = ['I', 'II', 'III', 'IV']

const RESOURCE_TYPES = ['team', 'medical', 'vehicle', 'shelter']
const YUNNAN_REGIONS = ['昆明市', '曲靖市', '玉溪市', '保山市', '昭通市', '丽江市', '普洱市', '临沧市', '楚雄州', '红河州', '文山州', '西双版纳州', '大理州', '德宏州', '怒江州', '迪庆州']

const YUNNAN_COORDS: Record<string, [number, number]> = {
  '昆明市': [25.04, 102.68], '曲靖市': [25.49, 103.80], '玉溪市': [24.35, 102.54],
  '保山市': [25.11, 99.17], '昭通市': [27.34, 103.72], '丽江市': [26.87, 100.23],
  '普洱市': [22.79, 100.97], '临沧市': [23.88, 100.09], '楚雄州': [25.03, 101.55],
  '红河州': [23.36, 103.38], '文山州': [23.37, 104.24], '西双版纳州': [22.01, 100.80],
  '大理州': [25.61, 100.27], '德宏州': [24.44, 98.58], '怒江州': [25.85, 98.86],
  '迪庆州': [27.82, 99.71],
}

function getCoords(location: string): [number, number] {
  for (const [region, coords] of Object.entries(YUNNAN_COORDS)) {
    if (location.includes(region)) return coords
  }
  return [25.04, 102.68]
}

function randomItem<T>(arr: T[]): T { return arr[Math.floor(Math.random() * arr.length)] }
function randomInt(min: number, max: number): number { return Math.floor(Math.random() * (max - min + 1)) + min }

let mockIncidents: Record<string, unknown>[] = []
let mockResources: Record<string, unknown>[] = []
let mockDispatchOrders: Record<string, unknown>[] = []
let incidentIdCounter = 0

let mockRoleApplications: Record<string, any>[] = []
let mockDisposalPlans: Record<string, any>[] = []
let mockPlans: Record<string, any>[] = []
let mockShortageWarnings: Record<string, any>[] = []
let mockResourceRequests: Record<string, any>[] = []
let roleAppIdCounter = 0
let planIdCounter = 0

interface LinkedStatus {
  status: string
  disposalPlanStatus: string | null
  resourceDispatchStatus: string | null
}

function generateLinkedStatus(): LinkedStatus {
  const r = Math.random()
  if (r < 0.25) {
    return { status: 'processing', disposalPlanStatus: null, resourceDispatchStatus: null }
  } else if (r < 0.50) {
    const rdRand = Math.random()
    let rd: string | null
    if (rdRand < 0.20) rd = 'executing'
    else if (rdRand < 0.45) rd = 'shortage'
    else if (rdRand < 0.70) rd = 'completed'
    else rd = null
    return { status: 'processing', disposalPlanStatus: 'submitted', resourceDispatchStatus: rd }
  } else if (r < 0.70) {
    return { status: 'completed', disposalPlanStatus: 'accepted', resourceDispatchStatus: 'completed' }
  } else if (r < 0.85) {
    return { status: 'processing', disposalPlanStatus: 'rejected', resourceDispatchStatus: 'shortage' }
  } else {
    return { status: 'processing', disposalPlanStatus: 'submitted', resourceDispatchStatus: null }
  }
}


function initMockData(): void {
  for (let i = 0; i < 50; i++) {
    incidentIdCounter++
    const dt = `2026-${String(randomInt(1, 7)).padStart(2, '0')}-${String(randomInt(1, 28)).padStart(2, '0')} ${String(randomInt(0, 23)).padStart(2, '0')}:${String(randomInt(0, 59)).padStart(2, '0')}:00`
    const loc = `${randomItem(YUNNAN_REGIONS)}某区域`
    const [lat, lng] = getCoords(loc)
    const linked = generateLinkedStatus()
    mockIncidents.push({
      id: i + 1, incidentId: `mock-${String(incidentIdCounter).padStart(4, '0')}`,
      incidentName: `${randomItem(YUNNAN_REGIONS)}${randomItem(['地震', '泥石流', '洪涝', '干旱', '山体滑坡', '森林火灾'])}灾害${randomInt(1, 999)}`,
      disasterType: randomItem(DISASTER_TYPES), incidentLevel: randomItem(INCIDENT_LEVELS),
      occurTime: dt, location: loc,
      description: '该地区发生自然灾害，造成一定程度的影响，需要紧急处置。',
      status: linked.status, imageUrls: i % 3 === 0 ? '["/api/image/mock.jpg"]' : null,
      reporterId: randomInt(1, 4), reportTime: dt, createdAt: dt, updatedAt: dt,
      latitude: lat + (Math.random() - 0.5) * 0.5, longitude: lng + (Math.random() - 0.5) * 0.5,
      deathCount: randomInt(0, 50),
      propertyLoss: parseFloat((Math.random() * 5000).toFixed(2)),
      disposalPlanStatus: linked.disposalPlanStatus,
      resourceDispatchStatus: linked.resourceDispatchStatus,
    })
  }
  const rn: Record<string, string[]> = { team: ['地震救援队', '山岳救援队', '水域救援队', '医疗救护队'], medical: ['急救药品', '外科手术包', '消毒用品'], vehicle: ['救护车', '消防车', '工程抢险车'], shelter: ['救灾帐篷', '临时安置房', '避难场所'] }
  for (let i = 0; i < 100; i++) {
    const rType = randomItem(RESOURCE_TYPES)
    mockResources.push({
      id: i + 1, resourceId: `res-${String(i + 1).padStart(4, '0')}`,
      name: `${randomItem(YUNNAN_REGIONS)}${randomItem(rn[rType])}`, resourceType: rType,
      quantity: randomInt(5, 500), unit: rType === 'vehicle' ? '辆' : rType === 'team' ? '人' : '件',
      dispatchedCount: randomInt(0, 20),
      status: randomItem(['available', 'dispatched', 'used']), storageAddress: `${randomItem(YUNNAN_REGIONS)}仓库${randomInt(1, 10)}号`,
      createdAt: '2026-07-22T10:00:00', updatedAt: '2026-07-22T10:00:00',
    })
  }
  const priorities = ['高', '中', '低']
  for (let i = 0; i < 8; i++) {
    const dt = `2026-07-${String(randomInt(15, 22)).padStart(2, '0')} ${String(randomInt(8, 18)).padStart(2, '0')}:${String(randomInt(0, 59)).padStart(2, '0')}:00`
    mockDispatchOrders.push({
      id: i + 1, orderId: `dispatch-${String(i + 1).padStart(4, '0')}`,
      incidentId: `mock-${String(randomInt(1, 50)).padStart(4, '0')}`,
      content: `紧急调配${randomItem(['救援队伍', '医疗物资', '运输车辆', '避难帐篷'])}前往灾区支援`,
      targetUsers: `${randomItem(YUNNAN_REGIONS)}应急指挥部`,
      priority: randomItem(priorities),
      status: i < 2 ? 'shortage' : randomItem(['pending', 'executing', 'completed']),
      dispatchTime: dt, createdAt: dt, updatedAt: dt,
    })
  }

  roleAppIdCounter = 4
  mockRoleApplications = [
    { id: 1, applicationId: 'role-app-001', userId: 4, username: 'viewer', realName: '张信息员', targetRole: 'OPERATOR', reason: '本人具备应急指挥相关经验，申请成为指挥人员', status: 'pending', reviewerId: null, reviewerName: null, rejectReason: null, reviewedAt: null, createdAt: '2026-07-20T09:00:00', updatedAt: '2026-07-20T09:00:00' },
    { id: 2, applicationId: 'role-app-002', userId: 4, username: 'viewer', realName: '张信息员', targetRole: 'RESOURCE_MANAGER', reason: '希望参与资源管理工作', status: 'approved', reviewerId: 1, reviewerName: '系统管理员', rejectReason: null, reviewedAt: '2026-07-21T10:00:00', createdAt: '2026-07-19T14:00:00', updatedAt: '2026-07-21T10:00:00' },
    { id: 3, applicationId: 'role-app-003', userId: 3, username: 'resource', realName: '王资源', targetRole: 'OPERATOR', reason: '申请转岗至指挥岗位', status: 'rejected', reviewerId: 1, reviewerName: '系统管理员', rejectReason: '当前岗位人员不足，暂不支持转岗', reviewedAt: '2026-07-22T08:00:00', createdAt: '2026-07-21T16:00:00', updatedAt: '2026-07-22T08:00:00' },
    { id: 4, applicationId: 'role-app-004', userId: 2, username: 'operator', realName: '李指挥', targetRole: 'ADMIN', reason: '需要系统管理权限进行平台配置', status: 'pending', reviewerId: null, reviewerName: null, rejectReason: null, reviewedAt: null, createdAt: '2026-07-22T11:00:00', updatedAt: '2026-07-22T11:00:00' },
  ]


  mockDisposalPlans = [
    { id: 1, disposalPlanId: 'dp-001', incidentId: 'mock-0001', planContent: '一、灾情概况\n震中位于昆明市某区域，震级5.2级。\n二、响应措施\n1. 启动Ⅲ级应急响应\n2. 组织救援队伍赶赴现场\n3. 设置临时安置点', status: 'accepted', submittedBy: 2, submittedAt: '2026-07-20T11:00:00', rejectReason: null, createdAt: '2026-07-20T10:30:00', updatedAt: '2026-07-20T14:00:00' },
    { id: 2, disposalPlanId: 'dp-002', incidentId: 'mock-0002', planContent: '一、灾情概况\n泥石流发生在昭通市某山区。\n二、响应措施\n1. 组织群众转移\n2. 封闭危险路段\n3. 开展搜救工作', status: 'submitted', submittedBy: 2, submittedAt: '2026-07-21T09:00:00', rejectReason: null, createdAt: '2026-07-21T08:00:00', updatedAt: '2026-07-21T09:00:00' },
    { id: 3, disposalPlanId: 'dp-003', incidentId: 'mock-0003', planContent: '一、灾情概况\n洪涝灾害影响大理州多个乡镇。\n二、响应措施\n1. 启动防汛应急响应\n2. 调配救援船只\n3. 转移低洼地区群众', status: 'draft', submittedBy: null, submittedAt: null, rejectReason: null, createdAt: '2026-07-21T15:00:00', updatedAt: '2026-07-21T15:00:00' },
    { id: 4, disposalPlanId: 'dp-004', incidentId: 'mock-0001', planContent: '一、灾情更新\n根据余震情况，调整响应级别。\n二、调整措施\n1. 提升响应级别至Ⅱ级\n2. 增派救援力量', status: 'rejected', submittedBy: 2, submittedAt: '2026-07-22T08:00:00', rejectReason: '方案内容不够详细，请补充具体执行步骤', createdAt: '2026-07-22T07:00:00', updatedAt: '2026-07-22T10:00:00' },
    { id: 5, disposalPlanId: 'dp-005', incidentId: 'mock-0005', planContent: '一、灾情概况\n干旱影响普洱市多个村庄。\n二、响应措施\n1. 调配供水车辆\n2. 启动应急水源', status: 'draft', submittedBy: null, submittedAt: null, rejectReason: null, createdAt: '2026-07-22T13:00:00', updatedAt: '2026-07-22T13:00:00' },
    { id: 6, disposalPlanId: 'dp-006', incidentId: 'mock-0008', planContent: '一、灾情概况\n森林火灾发生在丽江市某林区。\n二、响应措施\n1. 组织扑火队伍\n2. 设置防火隔离带\n3. 疏散周边群众', status: 'submitted', submittedBy: 2, submittedAt: '2026-07-22T16:00:00', rejectReason: null, createdAt: '2026-07-22T15:00:00', updatedAt: '2026-07-22T16:00:00' },
  ]

  planIdCounter = 2
  mockPlans = [
    { id: 1, planId: 'plan-001', incidentId: 'mock-0001', planTitle: '地震应急方案-初版', planContent: '一、灾情概况\n本次地震发生在云南省某地区，震级5.2级。\n\n二、响应措施\n1. 立即启动Ⅲ级应急响应\n2. 组织救援队伍赶赴现场\n3. 设置临时安置点\n4. 开展伤员救治\n\n三、资源调配\n调配救援队伍3支、医疗物资若干、运输车辆10台。', generateTime: '2026-07-20T10:30:00', status: 'approved', createdAt: '2026-07-20T10:30:00', updatedAt: '2026-07-20T10:30:00' },
    { id: 2, planId: 'plan-002', incidentId: 'mock-0001', planTitle: '地震应急方案-修订版', planContent: '一、灾情更新\n根据最新灾情评估，调整响应级别为Ⅱ级。\n\n二、调整措施\n1. 提升应急响应级别至Ⅱ级\n2. 增派救援力量\n3. 扩大疏散范围', generateTime: '2026-07-21T14:00:00', status: 'draft', createdAt: '2026-07-21T14:00:00', updatedAt: '2026-07-21T14:00:00' },
  ]


  mockShortageWarnings = [
    { id: 1, warningId: 'sw-001', incidentId: 'mock-0001', orderId: 'dispatch-0001', resourceType: 'team', requiredQuantity: 50, availableQuantity: 20, shortageQuantity: 30, reportedBy: 3, reportedAt: '2026-07-22T10:00:00', status: 'pending', handledBy: null, handledAt: null, handleResult: null },
    { id: 2, warningId: 'sw-002', incidentId: 'mock-0003', orderId: 'dispatch-0002', resourceType: 'medical', requiredQuantity: 200, availableQuantity: 80, shortageQuantity: 120, reportedBy: 3, reportedAt: '2026-07-22T11:00:00', status: 'pending', handledBy: null, handledAt: null, handleResult: null },
    { id: 3, warningId: 'sw-003', incidentId: 'mock-0005', orderId: 'dispatch-0001', resourceType: 'vehicle', requiredQuantity: 15, availableQuantity: 5, shortageQuantity: 10, reportedBy: 3, reportedAt: '2026-07-22T14:00:00', status: 'replenished', handledBy: 1, handledAt: '2026-07-22T16:00:00', handleResult: 'replenished' },
  ]

  mockResourceRequests = []
}
initMockData()

function success(data: unknown) { return { code: 0, message: 'success', data, timestamp: new Date().toISOString() } }
function error(message: string, code = 400) { return { code, message, data: null, timestamp: new Date().toISOString() } }

function parseBody(req: any): Promise<any> {
  return new Promise((resolve) => {
    const ct = req.headers?.['content-type'] || ''
    if (ct.includes('multipart/form-data')) { resolve({}); return }
    let body = ''
    req.on('data', (chunk: string) => { body += chunk })
    req.on('end', () => { try { resolve(JSON.parse(body)) } catch { resolve({}) } })
  })
}

function parseFormData(req: any): Promise<Record<string, any>> {
  return new Promise((resolve) => {
    const chunks: Buffer[] = []
    req.on('data', (chunk: Buffer) => { chunks.push(chunk) })
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf-8')
      const result: Record<string, any> = {}
      const re = /name="([^"]+)"\r\n\r\n([\s\S]*?)\r\n(?=--)/g
      let m: RegExpExecArray | null
      while ((m = re.exec(raw)) !== null) { if (m[1] !== 'images') result[m[1]] = m[2].trim() }
      resolve(result)
    })
  })
}

export function mockPlugin() {
  return {
    name: 'vite-plugin-mock-api',
    configureServer(server: ViteDevServer) {
      server.middlewares.use(async (req: any, res: any, next: any) => {
        const url = req.url?.split('?')[0]
        if (!url?.startsWith('/api/')) return next()
        res.setHeader('Content-Type', 'application/json')

        if (url === '/api/auth/login' && req.method === 'POST') {
          const body = await parseBody(req)
          const user = MOCK_USERS.find((u) => u.username === body.username)
          if (user && MOCK_PASSWORDS[body.username] === body.password) {
            res.end(JSON.stringify(success({ token: MOCK_TOKEN, tokenType: 'Bearer', expiresIn: 86400000, username: user.username, realName: user.realName, roleName: user.roleName })))
          } else { res.end(JSON.stringify(error('用户名或密码错误', 401))) }
          return
        }

        if (url === '/api/auth/register' && req.method === 'POST') {
          const body = await parseBody(req)
          if (MOCK_USERS.find((u) => u.username === body.username)) { res.end(JSON.stringify(error('用户名已存在'))); return }
          const nu = { id: MOCK_USERS.length + 1, username: body.username, realName: body.realName || null, email: null, phone: null, roleId: 4, status: 1, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }
          MOCK_PASSWORDS[body.username] = body.password; MOCK_USERS.push(nu as any)
          res.end(JSON.stringify(success(nu))); return
        }

        const authHeader = req.headers?.authorization
        if (!authHeader || !authHeader.includes(MOCK_TOKEN)) { res.statusCode = 401; res.end(JSON.stringify(error('未授权', 401))); return }

        if (url === '/api/dashboard/overview') {
          const today = new Date().toISOString().slice(0, 10)
          res.end(JSON.stringify(success({ todayCount: mockIncidents.filter((i) => (i.occurTime as string)?.slice(0, 10) === today).length, activeCount: mockIncidents.filter((i) => i.status === 'processing').length, completedCount: mockIncidents.filter((i) => i.status === 'completed').length }))); return
        }
        if (url === '/api/dashboard/trend') {
          const dates: string[] = [], counts: number[] = []
          for (let i = 29; i >= 0; i--) { const d = new Date(); d.setDate(d.getDate() - i); const ds = d.toISOString().slice(0, 10); dates.push(`${d.getMonth() + 1}/${d.getDate()}`); counts.push(mockIncidents.filter((inc) => (inc.occurTime as string)?.slice(0, 10) === ds).length) }
          res.end(JSON.stringify(success({ dates, counts }))); return
        }
        if (url === '/api/dashboard/distribution') {
          res.end(JSON.stringify(success({ types: DISASTER_TYPES, counts: DISASTER_TYPES.map((t) => mockIncidents.filter((i) => i.disasterType === t).length) }))); return
        }
        if (url === '/api/dashboard/screen') {
          const today = new Date().toISOString().slice(0, 10)
          const mapIncidents = mockIncidents.slice(0, 20).map((i) => ({ incidentId: i.incidentId, incidentName: i.incidentName, disasterType: i.disasterType, incidentLevel: i.incidentLevel, status: i.status, latitude: i.latitude, longitude: i.longitude }))
          res.end(JSON.stringify(success({ statistics: { todayCount: mockIncidents.filter((i) => (i.occurTime as string)?.slice(0, 10) === today).length, activeCount: mockIncidents.filter((i) => i.status === 'processing').length, completedCount: mockIncidents.filter((i) => i.status === 'completed').length }, incidents: mockIncidents.slice(0, 10), resources: mockResources.slice(0, 20), mapIncidents }))); return
        }

        if (url === '/api/incident/list') {
          const p = new URL(req.url, 'http://localhost').searchParams
          const page = Number(p.get('page') || 1), size = Number(p.get('size') || 10)
          let filtered = [...mockIncidents]
          if (p.get('disasterType')) filtered = filtered.filter((i) => i.disasterType === p.get('disasterType'))
          if (p.get('incidentLevel')) filtered = filtered.filter((i) => i.incidentLevel === p.get('incidentLevel'))
          if (p.get('status')) filtered = filtered.filter((i) => i.status === p.get('status'))
          if (p.get('keyword')) filtered = filtered.filter((i) => (i.incidentName as string).includes(p.get('keyword')!))
          const start = (page - 1) * size
          res.end(JSON.stringify(success({ total: filtered.length, list: filtered.slice(start, start + size) }))); return
        }
        if (url === '/api/incident/detail') {
          const id = new URL(req.url, 'http://localhost').searchParams.get('incidentId')
          res.end(JSON.stringify(success(mockIncidents.find((i) => i.incidentId === id) || null))); return
        }
        if (url === '/api/incident/report' && req.method === 'POST') {
          const fields = await parseFormData(req); incidentIdCounter++
          const now = new Date().toISOString()
          const ni = {
            id: mockIncidents.length + 1, incidentId: `mock-${String(incidentIdCounter).padStart(4, '0')}`,
            incidentName: fields.incidentName || '新上报灾情事件', disasterType: fields.disasterType || 'earthquake',
            incidentLevel: fields.incidentLevel || 'III', occurTime: fields.occurTime || now,
            location: fields.location || '云南省昆明市', description: fields.description || '',
            status: 'processing', imageUrls: null, reporterId: 1, reportTime: now, createdAt: now, updatedAt: now,
            deathCount: fields.deathCount ? Number(fields.deathCount) : 0,
            propertyLoss: fields.propertyLoss ? parseFloat(fields.propertyLoss) : 0,
            disposalPlanStatus: null, resourceDispatchStatus: null,
          }
          mockIncidents.unshift(ni); res.end(JSON.stringify(success({ incidentId: ni.incidentId, imageUrls: [] }))); return
        }

        if (url === '/api/plan/list') {
          const p = new URL(req.url, 'http://localhost').searchParams
          const incidentId = p.get('incidentId')
          let filtered = [...mockPlans]
          if (incidentId) filtered = filtered.filter((pl) => pl.incidentId === incidentId)
          res.end(JSON.stringify(success(filtered))); return
        }
        if (url === '/api/plan/generate' && req.method === 'POST') {
          res.end(JSON.stringify(success({ planId: `plan-${Date.now()}` }))); return
        }
        if (url === '/api/plan/detail') {
          const id = new URL(req.url, 'http://localhost').searchParams.get('planId')
          const plan = mockPlans.find((pl) => pl.planId === id)
          res.end(JSON.stringify(success(plan || null))); return
        }
        if (url === '/api/plan/stream') {
          res.setHeader('Content-Type', 'text/event-stream'); res.setHeader('Cache-Control', 'no-cache'); res.setHeader('Connection', 'keep-alive')
          const chunks = ['event: connect\ndata: connected\n\n', 'event: message\ndata: 【应急预案】\n\n', 'event: message\ndata: \n\n', 'event: message\ndata: 一、灾情概况\n\n', 'event: message\ndata: 灾害类型：暴雨\n发生地点：昆明市五华区\n\n', 'event: message\ndata: \n\n', 'event: message\ndata: 二、响应级别\n\n', 'event: message\ndata: 根据灾情严重程度，启动三级响应。\n\n', 'event: message\ndata: \n\n', 'event: message\ndata: 三、组织指挥体系\n\n', 'event: message\ndata: 成立应急指挥部，统一指挥协调救援工作。\n\n', 'event: message\ndata: \n\n', 'event: message\ndata: 四、应急处置措施\n\n', 'event: message\ndata: 1. 组织人员疏散\n2. 调配应急资源\n3. 开展伤员救治\n4. 保障通信畅通\n\n']
          let i = 0; const iv = setInterval(() => { if (i < chunks.length) { res.write(chunks[i]); i++ } else { clearInterval(iv); res.end() } }, 200)
          return
        }

        if (url === '/api/resource/list') { res.end(JSON.stringify(success(mockResources))); return }

        if (url === '/api/dispatch/list') { res.end(JSON.stringify(success(mockDispatchOrders))); return }

        if (url === '/api/role-application/submit' && req.method === 'POST') {
          const body = await parseBody(req)
          const userId = body.userId || 4
          const user = MOCK_USERS.find((u) => u.id === userId)
          const duplicate = mockRoleApplications.find(
            (a) => a.userId === userId && a.targetRole === body.targetRole && a.status === 'pending'
          )
          if (duplicate) { res.end(JSON.stringify(error('已存在相同的待审核申请'))); return }
          roleAppIdCounter++
          const now = new Date().toISOString()
          const app = {
            id: mockRoleApplications.length + 1,
            applicationId: `role-app-${String(roleAppIdCounter).padStart(3, '0')}`,
            userId, username: user?.username || '', realName: user?.realName || null,
            targetRole: body.targetRole, reason: body.reason || '',
            status: 'pending', reviewerId: null, reviewerName: null, rejectReason: null, reviewedAt: null,
            createdAt: now, updatedAt: now,
          }
          mockRoleApplications.push(app)
          res.end(JSON.stringify(success(app))); return
        }

        if (url === '/api/role-application/list') {
          const p = new URL(req.url, 'http://localhost').searchParams
          let filtered = [...mockRoleApplications]
          if (p.get('userId')) filtered = filtered.filter((a) => a.userId === Number(p.get('userId')))
          if (p.get('status')) filtered = filtered.filter((a) => a.status === p.get('status'))
          res.end(JSON.stringify(success(filtered))); return
        }

        if (url === '/api/role-application/review' && req.method === 'POST') {
          const body = await parseBody(req)
          const app = mockRoleApplications.find((a) => a.id === body.id)
          if (!app) { res.end(JSON.stringify(error('申请不存在'))); return }
          if (app.status !== 'pending') { res.end(JSON.stringify(error('该申请已被审核'))); return }
          const now = new Date().toISOString()
          const reviewer = MOCK_USERS.find((u) => u.id === (body.reviewerId || 1))
          if (body.approved) {
            app.status = 'approved'
            const targetUser = MOCK_USERS.find((u) => u.id === app.userId)
            if (targetUser) {
              targetUser.roleName = app.targetRole
              targetUser.updatedAt = now
            }
          } else {
            app.status = 'rejected'
            app.rejectReason = body.reason || null
          }
          app.reviewerId = body.reviewerId || 1
          app.reviewerName = reviewer?.realName || null
          app.reviewedAt = now
          app.updatedAt = now
          res.end(JSON.stringify(success(app))); return
        }

        if (url === '/api/member/list') {
          const p = new URL(req.url, 'http://localhost').searchParams
          let filtered = MOCK_USERS.map((u) => ({
            userId: u.id, username: u.username, realName: u.realName,
            email: u.email, phone: u.phone, roleName: u.roleName,
            status: u.status, registeredAt: u.createdAt, lastLoginAt: null,
          }))
          if (p.get('keyword')) {
            const kw = p.get('keyword')!.toLowerCase()
            filtered = filtered.filter((m) => m.username.toLowerCase().includes(kw) || (m.realName && m.realName.includes(kw)))
          }
          if (p.get('roleName')) filtered = filtered.filter((m) => m.roleName === p.get('roleName'))
          res.end(JSON.stringify(success(filtered))); return
        }

        if (url === '/api/member/change-role' && req.method === 'POST') {
          const body = await parseBody(req)
          const targetUser = MOCK_USERS.find((u) => u.id === body.userId)
          if (!targetUser) { res.end(JSON.stringify(error('用户不存在'))); return }
          if (targetUser.roleName === 'ADMIN' && body.targetRole !== 'ADMIN') {
            const adminCount = MOCK_USERS.filter((u) => u.roleName === 'ADMIN').length
            if (adminCount <= 1) { res.end(JSON.stringify(error('系统至少需要保留一个管理员角色'))); return }
          }
          targetUser.roleName = body.targetRole
          targetUser.updatedAt = new Date().toISOString()
          res.end(JSON.stringify(success({ userId: targetUser.id, roleName: targetUser.roleName }))); return
        }

        if (url === '/api/disposal-plan/list') {
          const p = new URL(req.url, 'http://localhost').searchParams
          const incidentId = p.get('incidentId')
          let filtered = [...mockDisposalPlans]
          if (incidentId) {
            filtered = filtered.filter((d) => d.incidentId === incidentId)
            if (filtered.length === 0) {
              const incident = mockIncidents.find((i: any) => i.incidentId === incidentId)
              if (incident && (incident as any).disposalPlanStatus) {
                const now = new Date().toISOString()
                const autoPlan = {
                  id: mockDisposalPlans.length + 1,
                  disposalPlanId: `dp-auto-${String(mockDisposalPlans.length + 1).padStart(4, '0')}`,
                  incidentId,
                  planContent: '一、灾情概况\n根据上报信息，该地区发生自然灾害。\n二、响应措施\n1. 启动应急响应\n2. 组织救援队伍\n3. 设置临时安置点',
                  status: (incident as any).disposalPlanStatus,
                  submittedBy: 2,
                  submittedAt: (incident as any).disposalPlanStatus !== 'draft' ? now : null,
                  rejectReason: (incident as any).disposalPlanStatus === 'rejected' ? '方案内容需要补充' : null,
                  createdAt: now,
                  updatedAt: now,
                }
                mockDisposalPlans.push(autoPlan)
                filtered = [autoPlan]
              }
            }
          }
          res.end(JSON.stringify(success(filtered))); return
        }

        if (url === '/api/disposal-plan/submit' && req.method === 'POST') {
          const body = await parseBody(req)
          let plan = mockDisposalPlans.find((d) => d.id === body.id)
          const now = new Date().toISOString()
          if (!plan) {
            plan = {
              id: mockDisposalPlans.length + 1,
              disposalPlanId: `dp-${String(mockDisposalPlans.length + 1).padStart(3, '0')}`,
              incidentId: body.incidentId || 'mock-0001',
              planContent: body.planContent || '',
              status: 'submitted',
              submittedBy: body.submittedBy || 2,
              submittedAt: now,
              rejectReason: null,
              createdAt: now,
              updatedAt: now,
            }
            mockDisposalPlans.push(plan)
            planIdCounter++
            const newPlan = {
              id: planIdCounter,
              planId: `plan-${String(planIdCounter).padStart(3, '0')}`,
              incidentId: body.incidentId || 'mock-0001',
              planTitle: `AI生成方案-${String(planIdCounter).padStart(3, '0')}`,
              planContent: body.planContent || '',
              generateTime: now,
              status: 'submitted',
              createdAt: now,
              updatedAt: now,
            }
            mockPlans.push(newPlan)
          } else {
            if (plan!.status !== 'draft' && plan!.status !== 'rejected') { res.end(JSON.stringify(error('仅草稿或已驳回状态的方案可以提交'))); return }
            if (body.planContent) plan!.planContent = body.planContent
            plan!.status = 'submitted'
            plan!.submittedBy = body.submittedBy || 2
            plan!.submittedAt = now
            plan!.updatedAt = now
            const linkedPlan = mockPlans.find((pl) => pl.incidentId === plan!.incidentId && pl.planContent === plan!.planContent)
            if (linkedPlan) {
              linkedPlan.planContent = plan!.planContent
              linkedPlan.status = 'submitted'
              linkedPlan.updatedAt = now
            }
          }
          res.end(JSON.stringify(success(plan))); return
        }

        if (url === '/api/disposal-plan/save-draft' && req.method === 'POST') {
          const body = await parseBody(req)
          let plan = mockDisposalPlans.find((d) => d.id === body.id)
          const now = new Date().toISOString()
          if (!plan) {
            plan = {
              id: mockDisposalPlans.length + 1,
              disposalPlanId: `dp-${String(mockDisposalPlans.length + 1).padStart(3, '0')}`,
              incidentId: body.incidentId || 'mock-0001',
              planContent: body.planContent || '',
              status: 'draft',
              submittedBy: null,
              submittedAt: null,
              rejectReason: null,
              createdAt: now,
              updatedAt: now,
            }
            mockDisposalPlans.push(plan)
            planIdCounter++
            const newPlan = {
              id: planIdCounter,
              planId: `plan-${String(planIdCounter).padStart(3, '0')}`,
              incidentId: body.incidentId || 'mock-0001',
              planTitle: `AI生成方案-${String(planIdCounter).padStart(3, '0')}`,
              planContent: body.planContent || '',
              generateTime: now,
              status: 'draft',
              createdAt: now,
              updatedAt: now,
            }
            mockPlans.push(newPlan)
          } else {
            plan!.planContent = body.planContent || plan!.planContent
            plan!.status = 'draft'
            plan!.updatedAt = now
            const linkedPlan = mockPlans.find((pl) => pl.incidentId === plan!.incidentId && pl.planContent === plan!.planContent)
            if (linkedPlan) {
              linkedPlan.planContent = plan!.planContent
              linkedPlan.status = 'draft'
              linkedPlan.updatedAt = now
            }
          }
          res.end(JSON.stringify(success(plan))); return
        }

        if (url === '/api/disposal-plan/reject' && req.method === 'POST') {
          const body = await parseBody(req)
          let plan = mockDisposalPlans.find((d) => d.id === body.id)
          if (!plan && body.incidentId) {
            const incident = mockIncidents.find((i: any) => i.incidentId === body.incidentId)
            if (incident && (incident as any).disposalPlanStatus === 'submitted') {
              const now = new Date().toISOString()
              plan = {
                id: mockDisposalPlans.length + 1,
                disposalPlanId: `dp-reject-${String(mockDisposalPlans.length + 1).padStart(4, '0')}`,
                incidentId: body.incidentId,
                planContent: '一、灾情概况\n根据上报信息，该地区发生自然灾害。\n二、响应措施\n1. 启动应急响应\n2. 组织救援队伍\n3. 设置临时安置点',
                status: 'submitted',
                submittedBy: 2,
                submittedAt: now,
                rejectReason: null,
                createdAt: now,
                updatedAt: now,
              }
              mockDisposalPlans.push(plan)
            }
          }
          if (!plan) { res.end(JSON.stringify(error('处置方案不存在'))); return }
          if (plan.status !== 'submitted') { res.end(JSON.stringify(error('仅已提交的方案可以驳回'))); return }
          const now = new Date().toISOString()
          plan.status = 'rejected'
          plan.rejectReason = body.rejectReason || null
          plan.updatedAt = now
          const incident = mockIncidents.find((i: any) => i.incidentId === plan.incidentId)
          if (incident) {
            incident.disposalPlanStatus = 'rejected'
            incident.resourceDispatchStatus = 'shortage'
            incident.updatedAt = now
          }
          res.end(JSON.stringify(success(plan))); return
        }

        if (url === '/api/resource-request/submit' && req.method === 'POST') {
          const body = await parseBody(req)
          if (!body.items || !Array.isArray(body.items) || body.items.length === 0) {
            res.end(JSON.stringify(error('参数校验失败：items不能为空'))); return
          }
          const incident = mockIncidents.find((i: any) => i.incidentId === body.incidentId)
          if (!incident) { res.end(JSON.stringify(error('关联灾情不存在'))); return }
          if ((incident as any).disposalPlanStatus !== 'submitted') {
            res.end(JSON.stringify(error('仅已提交处置方案的灾情可请求资源'))); return
          }
          const resourceIds = body.items.map((item: any) => item.resourceId)
          if (new Set(resourceIds).size !== resourceIds.length) {
            res.end(JSON.stringify(error('不允许重复选择同一种资源'))); return
          }
          const now = new Date().toISOString()
          const createdRequests: any[] = []
          for (const item of body.items) {
            if (!item.resourceId || !item.quantity || item.quantity <= 0) continue
            const req2 = {
              id: mockResourceRequests.length + 1,
              requestId: `rr-${String(mockResourceRequests.length + 1).padStart(4, '0')}`,
              incidentId: body.incidentId,
              incidentName: (incident as any).incidentName || null,
              resourceId: item.resourceId,
              resourceName: item.resourceName,
              quantity: item.quantity,
              status: 'pending',
              requestedBy: 3,
              requestedAt: now,
              handledBy: null,
              handledAt: null,
              handleResult: null,
              createdAt: now,
              updatedAt: now,
            }
            mockResourceRequests.push(req2)
            createdRequests.push(req2)
          }
          ;(incident as any).resourceDispatchStatus = 'executing'
          ;(incident as any).updatedAt = now
          res.end(JSON.stringify(success(createdRequests))); return
        }

        if (url === '/api/resource-shortage/list') {
          res.end(JSON.stringify(success(mockShortageWarnings))); return
        }

        if (url === '/api/resource-shortage/handle' && req.method === 'POST') {
          const body = await parseBody(req)
          const warning = mockShortageWarnings.find((w) => w.id === body.id)
          if (!warning) { res.end(JSON.stringify(error('警告记录不存在'))); return }
          if (warning.status !== 'pending') { res.end(JSON.stringify(error('该警告已被处理'))); return }
          const now = new Date().toISOString()
          if (body.action === 'replenish') {
            const resource = mockResources.find((r) => r.resourceType === warning.resourceType)
            if (resource) {
              (resource as any).quantity = (resource.quantity as number) + warning.shortageQuantity
              resource.updatedAt = now
            }
            warning.status = 'replenished'
            warning.handleResult = 'replenished'
          } else if (body.action === 'reject') {
            warning.status = 'rejected'
            warning.handleResult = 'rejected'
          }
          warning.handledBy = body.handledBy || 1
          warning.handledAt = now
          res.end(JSON.stringify(success(warning))); return
        }

        next()
      })
    },
  }
}
