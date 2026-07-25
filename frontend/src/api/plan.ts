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
      // 如果流自然结束，跳出循环
      if (done) break
      
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      // 保留最后一行未接收完整的字符串片段
      buffer = lines.pop() || ''
      
      for (const line of lines) {
        const trimmedLine = line.trim()
        if (!trimmedLine) continue
        
        if (trimmedLine.startsWith('data:')) {
          const data = trimmedLine.substring(5).trim()
          if (!data) continue
          
          // 【修复 1】拦截标准的纯文本结束标识
          if (data === '[DONE]') {
            onDone()
            return // 立刻终止，不再死等
          }
          
          try {
            const parsed = JSON.parse(data)
            if (parsed.error) {
              onError(new Error(parsed.error))
              return // 遇到明确的错误，也立刻终止
            } else if (parsed.chunk) {
              onChunk(parsed.chunk)
            } else if (parsed.done) {
              // 【修复 2】收到后端 JSON 格式的结束信号，直接结束，不再等待底层连接断开
              onDone()
              return 
            }
          } catch {
            // 解析 JSON 失败且不是 [DONE] 时，才作为普通文本输出
            onChunk(data)
          }
        }
      }
    }
    // 正常走完流，触发完成
    onDone()
  } catch (e) {
    onError(e instanceof Error ? e : new Error(String(e)))
  }
}
