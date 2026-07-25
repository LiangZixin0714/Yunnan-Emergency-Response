"""API Key认证中间件，验证Spring Boot后端调用时携带的X-API-Key。"""

import os
from fastapi import Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware

API_KEY = os.environ.get("AI_SERVICE_API_KEY", "emergency-platform-ai-service-key-2024")


class ApiKeyAuthMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        if request.url.path == "/health":
            return await call_next(request)

        api_key = request.headers.get("X-API-Key", "")
        if api_key != API_KEY:
            raise HTTPException(status_code=401, detail="Invalid or missing API Key")

        return await call_next(request)