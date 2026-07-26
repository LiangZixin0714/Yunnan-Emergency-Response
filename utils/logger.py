"""统一日志模块，同时输出到控制台和 data/logs/ 目录。"""

import logging
import os
import sys
from datetime import datetime
from typing import Optional


_LOG_FORMAT = "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s"
_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"

_loggers: dict[str, logging.Logger] = {}


def get_logger(name: str, log_dir: Optional[str] = None) -> logging.Logger:
    """获取或创建一个同时输出到控制台和文件的 Logger。

    Args:
        name: Logger 名称，通常为模块名。
        log_dir: 日志文件目录，默认为 data/logs/。

    Returns:
        配置好的 Logger 实例。
    """
    if name in _loggers:
        return _loggers[name]

    logger = logging.getLogger(name)
    logger.setLevel(logging.DEBUG)

    if logger.handlers:
        _loggers[name] = logger
        return logger

    formatter = logging.Formatter(_LOG_FORMAT, datefmt=_DATE_FORMAT)

    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.DEBUG)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    if log_dir is None:
        log_dir = os.environ.get("LOGS_DIR", "data/logs")
    try:
        os.makedirs(log_dir, exist_ok=True)
        today = datetime.now().strftime("%Y-%m-%d")
        file_handler = logging.FileHandler(
            os.path.join(log_dir, f"{today}.log"),
            encoding="utf-8",
        )
        file_handler.setLevel(logging.DEBUG)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
    except OSError:
        pass

    _loggers[name] = logger
    return logger


def setup_logger() -> logging.Logger:
    """配置全局日志记录器（兼容 ai-service 调用）。"""
    return get_logger("ai-service")