import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from urllib.parse import urlparse

parser = argparse.ArgumentParser(description="重建本机VIBE_RANDOM_NOTE项目表；默认只检查，--apply 才会执行。")
parser.add_argument("--database", required=True)
parser.add_argument("--apply", action="store_true")
args = parser.parse_args()
base = Path(__file__).resolve().parents[1]
env = dict(os.environ)
env_file = base.parent / ".env"
if env_file.exists():
    for raw in env_file.read_text(encoding="utf-8-sig").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        key, sep, value = line.partition("=")
        if not sep or not re.fullmatch(r"[A-Z][A-Z0-9_]*", key):
            raise SystemExit(".env 格式错误，请使用 NAME=value")
        env[key] = value.strip().strip("\"'")

url = env.get("DB_URL", "")
if not url.startswith("jdbc:mysql://"):
    raise SystemExit("请在 .env 或环境变量中明确设置 DB_URL，工具不会猜测数据库。")
target = urlparse(url[5:])
database = target.path.lstrip("/")
if target.hostname not in ("localhost", "127.0.0.1", "::1"):
    raise SystemExit("只允许连接本机 MySQL。")
if database != args.database or not re.fullmatch(r"[a-zA-Z0-9_]+", database):
    raise SystemExit("--database 必须与 DB_URL 中的库名完全一致。")
if "DB_PASSWORD" not in env:
    raise SystemExit("缺少 DB_PASSWORD。")
mysql = shutil.which("mysql")
if not mysql and env.get("MYSQL_HOME"):
    candidate = Path(env["MYSQL_HOME"]) / "bin/mysql.exe"
    if candidate.is_file(): mysql = str(candidate)
if not mysql:
    raise SystemExit("找不到 mysql 客户端，请将 MySQL 的 bin 目录加入 PATH。")
process_env = dict(os.environ)
process_env["MYSQL_PWD"] = env["DB_PASSWORD"]
command = [mysql, "--protocol=TCP", "--host=" + target.hostname,
           "--port=" + str(target.port or 3306), "--user=" + env.get("DB_USER", "root"),
           "--default-character-set=utf8mb4", "--batch", "--skip-column-names", "--database=" + database]
def run(sql):
    result = subprocess.run(command, input=sql, text=True, encoding="utf-8", env=process_env,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise SystemExit(result.stderr)
    return result.stdout

known = [
    "music_song_artist",
    "poetry_favorite",
    "poetry_poem",
    "poetry_author",
    "diary_article_comment_like",
    "diary_article_comment",
    "diary_article_favorite",
    "diary_article_like",
    "diary_article",
    "diary_category",
    "account_email_code",
    "music_comment_like",
    "music_comment",
    "music_user_favorite",
    "music_feedback",
    "music_genre",
    "music_playlist_binding",
    "music_song",
    "music_playlist",
    "music_artist",
    "music_style",
    "music_banner",
    "account_user",
]
existing = set(run("SHOW TABLES;").splitlines())
unknown = existing - set(known)
if unknown:
    raise SystemExit("发现非项目预期的表，已停止：" + ", ".join(sorted(unknown)))
print("目标：" + target.hostname + ":" + str(target.port or 3306) + "/" + database)
for table in sorted(existing):
    count = run("SELECT COUNT(*) FROM `" + table + "`;").strip()
    print(table + ": " + count + " 行")
if not args.apply:
    print("仅检查，未修改任何数据。确认后加 --apply 执行。")
    sys.exit(0)

sql = "SET FOREIGN_KEY_CHECKS=0;\n" + "\n".join("DROP TABLE IF EXISTS `" + table + "`;" for table in known)
for file in [base / "account/sql/schema.sql", base / "diary/sql/schema.sql", base / "music/sql/schema.sql", base / "poetry/sql/schema.sql", base / "poetry/sql/seed.sql"]:
    schema = file.read_text(encoding="utf-8")
    schema = re.sub(r"(?m)^CREATE DATABASE[^;]*;|^USE [^;]*;", "", schema)
    sql += "\n" + schema
sql += "\nSET FOREIGN_KEY_CHECKS=1;\n"
run(sql)
print("项目表已清空并重建。首次启动时使用 ADMIN_PASSWORD 初始化 ADMIN。上传目录和 MinIO 对象未删除。")
