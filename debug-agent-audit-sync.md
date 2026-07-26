# Debug Session: agent-audit-sync

**Status**: [OPEN]
**Created**: 2026-07-26
**Symptom**: Agent audit execution records (Agent执行记录) are not displaying/syncing on the frontend Agent page

## Hypotheses

### H1: Frontend proxy misconfiguration
- The Vite dev server proxy may not be correctly forwarding `/api` requests to the backend container
- Evidence: curl to `localhost:3000/api/agent/runs` returned 403 without token
- Verification: Test proxy forwarding with and without token

### H2: JWT token not being sent
- The token stored in localStorage might not be properly retrieved by the request interceptor
- Evidence: request.ts interceptor calls `getStoredToken()` but may return null
- Verification: Check token storage and interceptor behavior

### H3: Backend rejecting requests
- The Spring Security filter chain might be rejecting valid authenticated requests
- Evidence: 403 response from backend
- Verification: Check JwtAuthenticationFilter logic

### H4: Agent page needs manual refresh
- The Agent page only fetches data on mount, not after new records are created
- Evidence: onMounted() only calls fetchAgentRuns()
- Verification: Check if polling or refresh is needed

## Steps Taken
- [ ] Step 1: Generate session ID and create debug file
- [ ] Step 2: List hypotheses
- [ ] Step 3: Instrument and collect evidence
- [ ] Step 4: Determine root cause
- [ ] Step 5: Implement fix
- [ ] Step 6: Verify fix
