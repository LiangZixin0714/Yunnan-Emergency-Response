import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'
import type { Plan } from '@/types/plan'

export function getPlanList(incidentId: string): Promise<ApiResponse<Plan[]>> {
  return request.get('/plan/list', { params: { incidentId } })
}



export function getPlanDetail(planId: string): Promise<ApiResponse<Plan>> {
  return request.get('/plan/detail', { params: { planId } })
}

export function deletePlan(planId: string): Promise<ApiResponse<void>> {
  return request.delete('/plan/delete', { params: { planId } })
}

// export async function streamPlan(
//   incidentId: string,
//   token: string,
//   onChunk: (text: string) => void,
//   onDone: () => void,
//   onError: (err: Error) => void,
// ): Promise<void> {
//   try {
//     const response = await fetch(`/api/plan/stream?incidentId=${encodeURIComponent(incidentId)}`, {
//       headers: {
//         Accept: 'text/event-stream',
//         'Cache-Control': 'no-cache',
//         Authorization: `Bearer ${token}`,
//       },
//     })
//     if (!response.ok) {
//       throw new Error(`SSE连接失败: ${response.status}`)
//     }
//     const reader = response.body?.getReader()
//     if (!reader) throw new Error('无法获取响应流')
//     const decoder = new TextDecoder('utf-8')
//     let buffer = ''
//     while (true) {
//       const { done, value } = await reader.read()
//       if (done) break
//       buffer += decoder.decode(value, { stream: true })
//       const lines = buffer.split('\n')
//       buffer = lines.pop() || ''
//       for (const line of lines) {
//         const trimmedLine = line.trim()
//         if (!trimmedLine) continue
        
//         if (trimmedLine.startsWith('data:')) {
//           const data = trimmedLine.substring(5).trim()
//           if (!data) continue
//           try {
//             const parsed = JSON.parse(data)
//             if (parsed.error) {
//               onError(new Error(parsed.error))
//             } else if (parsed.chunk) {
//               onChunk(parsed.chunk)
//             } else if (parsed.done) {
//               // 完成信号，继续等待流结束
//             }
//           } catch {
//             onChunk(data)
//           }
//         }
//       }
//     }
//     onDone()
//   } catch (e) {
//     onError(e instanceof Error ? e : new Error(String(e)))
//   }
// }
export async function streamPlan(
  incidentId: string,
  token: string,
  onChunk: (text: string) => void,
  onDone: () => void,
  onError: (err: Error) => void,
): Promise<void> {
  const controller = new AbortController()
  let timeoutId: number | undefined

  // 【关键调整】：将看门狗超时时间从 3 秒延长到 15 秒，适应大模型生成间歇
  const resetWatchdog = () => {
    if (timeoutId) clearTimeout(timeoutId)
    timeoutId = window.setTimeout(() => {
      controller.abort()
      onDone()
    }, 15000) 
  }

  try {
    resetWatchdog()

    const url = `/api/plan/stream?incidentId=${encodeURIComponent(incidentId)}&token=${encodeURIComponent(token)}`
    
    const response = await fetch(url, {
      headers: {
        Accept: 'text/event-stream',
        'Cache-Control': 'no-cache',
        Authorization: `Bearer ${token}`,
      },
      signal: controller.signal,
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
      resetWatchdog() // 收到数据，重置 15 秒计时
      
      if (done) {
        clearTimeout(timeoutId)
        onDone()
        return
      }
      
      buffer += decoder.decode(value, { stream: true })
      
      if (buffer.includes('[DONE]') || buffer.includes('"done":true') || buffer.includes('"done": true')) {
        clearTimeout(timeoutId)
        onDone()
        return 
      }

      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      
      for (const line of lines) {
        const trimmedLine = line.trim()
        if (!trimmedLine) continue
        
        if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.substring(5).trim()
          if (!data) continue
          
          if (data === '[DONE]') {
            clearTimeout(timeoutId)
            onDone()
            return 0 as any
          }
          
          try {
            const parsed = JSON.parse(data)
            if (parsed.error) {
              clearTimeout(timeoutId)
              onError(new Error(parsed.error))
              return 
            } else if (parsed.chunk) {
              onChunk(parsed.chunk)
            } else if (parsed.done) {
              clearTimeout(timeoutId)
              onDone()
              return 
            }
          } catch {
            onChunk(data)
          }
        }
      }
    }
  } catch (e: any) {
    clearTimeout(timeoutId)
    onDone() // 正常收工
  }
}