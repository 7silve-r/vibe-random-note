import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import unquote, urlparse

try:
    from opencc import OpenCC
    import pymysql
except ImportError:
    raise SystemExit("请先安装导入依赖：python -m pip install -r backend/scripts/requirements.txt")


BASE = Path(__file__).resolve().parents[1]
CONVERTER = OpenCC("t2s")
DATASETS = {item["code"]: item for item in json.loads(
    (BASE / "poetry/src/main/resources/poetry-datasets.json").read_text(encoding="utf-8"))}


def digest(value):
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def text(value, name, limit):
    if not isinstance(value, str) or not value.strip() or len(value.strip()) > limit:
        raise ValueError(name + "缺失或长度不正确")
    return value.strip()


def normalize(record, dataset, source_ref, version):
    definition = DATASETS[dataset]
    dynasty, kind = definition["dynasty"], definition["kind"]
    original_author = text(record.get("author") or definition.get("defaultAuthor"), "作者", 100)
    if dataset == "huajianji":
        original_author = re.sub(r"\s+[一二三四五六七八九十百零〇两\d]+首$", "", original_author)
    author = CONVERTER.convert(original_author)
    paragraphs = record.get(definition["contentField"])
    if not isinstance(paragraphs, list) or not paragraphs:
        raise ValueError("正文必须是非空数组")
    if dataset == "nalan":
        paragraphs = [line for line in paragraphs if not isinstance(line, str) or line.strip()]
    if not paragraphs:
        raise ValueError("正文不能全为空行")
    lines = [text(line, "诗句", 20000) for line in paragraphs]
    original_content = "\n".join(lines)
    if len(original_content) > 200000:
        raise ValueError("正文过长")
    rhythmic = record.get("rhythmic") or ""
    raw_title = record.get("title") or rhythmic
    if dataset == "nalan" and not raw_title:
        raw_title = "无题 · " + lines[0][:24]
    original_title = text(raw_title, "标题或词牌", 300)
    if definition.get("titleFields"):
        parts = [text(record.get(field), "篇章标题", 100) for field in definition["titleFields"]]
        original_title = " · ".join(dict.fromkeys(parts))
    title = CONVERTER.convert(original_title)
    content = CONVERTER.convert(original_content)
    if kind == "词" and not record.get("title"):
        title += " · " + CONVERTER.convert(lines[0])[:24]
    if len(title) > 300 or len(rhythmic) > 100:
        raise ValueError("标题或词牌过长")
    upstream_id = record.get("id")
    identity = "id:" + str(upstream_id) if upstream_id else "text:" + "\0".join(
        [author, CONVERTER.convert(original_title), content])
    excerpt = record.get("excerpt")
    rank = record.get("featured_rank")
    if excerpt is not None:
        excerpt = CONVERTER.convert(text(excerpt, "摘句", 1000))
        if excerpt not in content:
            raise ValueError("精选摘句不在正文中")
    if rank is not None and (not isinstance(rank, int) or rank < 1 or excerpt is None):
        raise ValueError("精选排序不正确")
    return {
        "source_key": digest(dataset + "\0" + identity),
        "dataset": dataset, "source_ref": source_ref, "source_version": version,
        "author_key": digest(dynasty + "\0" + author),
        "author_name": author, "original_author": original_author,
        "title": title, "original_title": original_title,
        "rhythmic": CONVERTER.convert(rhythmic), "dynasty": dynasty, "kind": kind,
        "content": content, "original_content": original_content,
        "excerpt": excerpt, "featured_rank": rank,
    }


def read_json(path):
    value = json.loads(path.read_text(encoding="utf-8-sig"))
    if not isinstance(value, list):
        raise ValueError(str(path) + "：顶层必须是数组")
    return value


def records(source, datasets, version):
    seed = BASE / "poetry/data/selection.json"
    for record in read_json(seed):
        yield record, record["dataset"], record["source_ref"], record["source_version"]
    if source is None:
        return
    for dataset in datasets:
        folder, pattern = DATASETS[dataset]["folder"], DATASETS[dataset]["pattern"]
        paths = sorted((source / folder).glob(pattern))
        if not paths:
            raise ValueError("未找到数据文件：" + str(source / folder / pattern))
        for path in paths:
            for record in read_json(path):
                yield record, dataset, path.relative_to(source).as_posix(), version


def config():
    values = {}
    path = BASE.parent / ".env"
    if path.exists():
        for raw in path.read_text(encoding="utf-8-sig").splitlines():
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            key, sep, value = line.partition("=")
            if not sep or not re.fullmatch(r"[A-Z][A-Z0-9_]*", key):
                raise ValueError(".env 格式错误")
            values[key] = value.strip().strip("\"'")
    values.update(os.environ)
    url = values.get("DB_URL", "")
    if not url.startswith("jdbc:mysql://"):
        raise ValueError("请配置明确的 DB_URL")
    target = urlparse(url[5:])
    database = unquote(target.path.lstrip("/"))
    if not re.fullmatch(r"[a-zA-Z0-9_]+", database) or not target.hostname:
        raise ValueError("DB_URL 中的数据库名或主机无效")
    if "DB_PASSWORD" not in values:
        raise ValueError("缺少 DB_PASSWORD")
    return dict(host=target.hostname, port=target.port or 3306,
                user=values.get("DB_USER", "root"), password=values["DB_PASSWORD"],
                database=database, charset="utf8mb4", connect_timeout=10)


def init_schema(connection):
    schema = (BASE / "poetry/sql/schema.sql").read_text(encoding="utf-8")
    schema = re.sub(r"(?m)^CREATE DATABASE[^;]*;|^USE [^;]*;", "", schema)
    with connection.cursor() as cursor:
        for statement in schema.split(";"):
            if statement.strip():
                cursor.execute(statement)
    connection.commit()


def write_batch(connection, batch):
    counts = [0, 0, 0]
    try:
        with connection.cursor() as cursor:
            for poem in batch:
                cursor.execute(
                    "INSERT INTO poetry_author (source_key,name,original_name,dynasty) "
                    "VALUES (%s,%s,%s,%s) ON DUPLICATE KEY UPDATE name=VALUES(name)",
                    (poem["author_key"], poem["author_name"], poem["original_author"], poem["dynasty"]))
                cursor.execute("SELECT id FROM poetry_author WHERE source_key=%s", (poem["author_key"],))
                author_id = cursor.fetchone()[0]
                columns = ["source_key", "dataset", "source_ref", "source_version", "author_name",
                           "title", "original_title", "rhythmic", "dynasty", "kind", "content",
                           "original_content", "excerpt", "featured_rank"]
                updates = [name + "=VALUES(" + name + ")" for name in columns
                           if name not in ("source_key", "excerpt", "featured_rank")]
                updates += ["author_id=VALUES(author_id)",
                            "excerpt=COALESCE(VALUES(excerpt),excerpt)",
                            "featured_rank=COALESCE(VALUES(featured_rank),featured_rank)"]
                cursor.execute(
                    "INSERT INTO poetry_poem (author_id," + ",".join(columns) + ") VALUES ("
                    + ",".join(["%s"] * (len(columns) + 1)) + ") ON DUPLICATE KEY UPDATE "
                    + ",".join(updates), [author_id] + [poem[name] for name in columns])
                counts[0 if cursor.rowcount == 1 else 1 if cursor.rowcount == 2 else 2] += 1
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    return counts


def import_authors(connection, source, datasets):
    if source is None:
        return
    for dataset in datasets:
        definition = DATASETS[dataset]
        filename = definition.get("authors")
        if not filename:
            continue
        folder, dynasty = definition["folder"], definition["dynasty"]
        path = source / folder / filename
        if not path.exists():
            continue
        with connection.cursor() as cursor:
            for record in read_json(path):
                name = record.get("name")
                description = record.get("desc") or record.get("description") or ""
                if not isinstance(name, str) or not isinstance(description, str):
                    continue
                cursor.execute("UPDATE poetry_author SET description=%s WHERE source_key=%s",
                               (CONVERTER.convert(description), digest(dynasty + "\0" + CONVERTER.convert(name.strip()))))
        connection.commit()


def select_datasets(value):
    names = list(dict.fromkeys(name.strip() for name in value.split(",")))
    if names == ["all"]:
        return list(DATASETS)
    if any(name not in DATASETS for name in names):
        raise ValueError("dataset 支持 all 或以下类别：" + ",".join(DATASETS))
    return names


def main():
    parser = argparse.ArgumentParser(description="分批导入诗词；默认仅检查，不重置数据库。未指定 --source 时导入项目精选。")
    parser.add_argument("--source", type=Path, help="chinese-poetry 仓库根目录")
    parser.add_argument("--dataset", default="tang,song-ci", help="类别以逗号分隔，all 表示全部；用 --list-datasets 查看")
    parser.add_argument("--list-datasets", action="store_true", help="列出支持的文集及数据路径")
    parser.add_argument("--revision", help="数据对应的 Git 提交 SHA；本地仓库自动读取")
    parser.add_argument("--batch-size", type=int, default=200)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    if args.list_datasets:
        for code, definition in DATASETS.items():
            print(f"{code:12} {definition['name']}  {definition['folder']}/{definition['pattern']}")
        return 0
    try:
        datasets = select_datasets(args.dataset)
    except ValueError as error:
        parser.error(str(error))
    if not 1 <= args.batch_size <= 1000:
        parser.error("batch-size 必须在1到1000之间")
    source = args.source.resolve() if args.source else None
    if source and not source.is_dir():
        parser.error("source 目录不存在")
    version = args.revision or "local"
    if source and not args.revision and (source / ".git").exists():
        result = subprocess.run(["git", "-C", str(source), "rev-parse", "HEAD"],
                                capture_output=True, text=True)
        if result.returncode == 0:
            version = result.stdout.strip()
    if not re.fullmatch(r"[a-fA-F0-9]{40}|local", version):
        parser.error("revision 必须为40位 Git SHA")
    for dataset in datasets if source else []:
        folder, pattern = DATASETS[dataset]["folder"], DATASETS[dataset]["pattern"]
        if not any((source / folder).glob(pattern)):
            parser.error("未找到数据文件：" + str(source / folder / pattern))
    connection = None
    total = failed = 0
    counts = [0, 0, 0]
    batch = []
    try:
        if args.apply:
            options = config()
            print(f"目标：{options['host']}:{options['port']}/{options['database']}")
            connection = pymysql.connect(**options)
            init_schema(connection)
        for record, dataset, ref, revision in records(source, datasets, version):
            total += 1
            try:
                batch.append(normalize(record, dataset, ref, revision))
            except (ValueError, TypeError, AttributeError) as error:
                failed += 1
                if failed <= 20:
                    print(f"跳过无效记录 {ref}（第{total}条）：{error}", file=sys.stderr)
            if len(batch) >= args.batch_size:
                if connection:
                    counts = [a + b for a, b in zip(counts, write_batch(connection, batch))]
                batch.clear()
            if total % 10000 == 0:
                print(f"已处理 {total} 条", flush=True)
        if connection:
            counts = [a + b for a, b in zip(counts, write_batch(connection, batch))]
            import_authors(connection, source, datasets)
        print(f"读取 {total} 条，有效 {total - failed} 条，无效 {failed} 条。")
        if connection:
            print(f"新增 {counts[0]} 条，更新 {counts[1]} 条，未变化 {counts[2]} 条。")
        else:
            print("仅检查，未连接数据库。加 --apply 执行导入。")
        return 1 if failed else 0
    except (ValueError, OSError, pymysql.MySQLError) as error:
        print(f"导入未完成：{error}。已提交的批次保留，可修复后重试。", file=sys.stderr)
        return 1
    finally:
        if connection:
            connection.close()


if __name__ == "__main__":
    sys.exit(main())
