import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Plan } from '@/types/plan'

export function getPlanList(incidentId: string): Promise<ApiResponse<Plan[]>> {
  return request.get('/plan/list', { params: { incidentId } })
}



export function getPlanDetail(planId: string): Promise<ApiResponse<Plan>> {
  return request.get('/plan/detail', { params: { planId } })
}

export async function streamPlan(
  incidentId: string,
  token: string,
  onChunk: (text: string) => void,
  onDone: () => void,
  onError: (err: Error) => void,
): Promise<void> {
  try {
    const response = await fetch(`/api/plan/stream?incidentId=${encodeURIComponent(incidentId)}`, {
      headers: {
        Accept: 'text/event-stream',
        'Cache-Control': 'no-cache',
        Authorization: `Bearer ${token}`,
      },
    })
    if (!response.ok) {
      throw new Error(`SSE连接失败: ${response.status}`)
    }
    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法获取响应流')
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('data: ')) {
          onChunk(line.slice(6))
        }
      }
    }
    onDone()
  } catch (e) {
    onError(e instanceof Error ? e : new Error(String(e)))
  }
}
