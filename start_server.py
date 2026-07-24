import uvicorn
import logging

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('test_log.txt', encoding='utf-8'),
        logging.StreamHandler()
    ]
)

if __name__ == "__main__":
    print("Starting server on port 8002...")
    uvicorn.run(
        'ai-service.main:app',
        host='0.0.0.0',
        port=8002,
        log_level='info',
        reload=False
    )
