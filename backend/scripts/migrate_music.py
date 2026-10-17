import argparse
from pathlib import Path
from import_poetry import config, pymysql


def migrate(connection):
    with connection.cursor() as cursor:
        cursor.execute("SHOW COLUMNS FROM music_song LIKE 'lyric_url'")
        if not cursor.fetchone():
            cursor.execute("ALTER TABLE music_song ADD COLUMN lyric_url VARCHAR(512) NULL")
        sql = (Path(__file__).resolve().parents[1] / 'music/sql/song-artists.sql').read_text(encoding='utf-8')
        for statement in sql.split(';'):
            if statement.strip():
                cursor.execute(statement)
    connection.commit()


def main():
    parser = argparse.ArgumentParser(description='增量升级音乐表，保留歌曲、资源及收藏；默认只检查。')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    connection = pymysql.connect(**config())
    try:
        if args.apply:
            migrate(connection)
            print('音乐表升级完成。')
        else:
            with connection.cursor() as cursor:
                cursor.execute("SHOW COLUMNS FROM music_song LIKE 'lyric_url'")
                print('歌词文件字段：' + ('已存在' if cursor.fetchone() else '待添加'))
                cursor.execute("SHOW TABLES LIKE 'music_song_artist'")
                print('多歌手关联表：' + ('已存在' if cursor.fetchone() else '待添加'))
            print('加 --apply 执行增量迁移。')
    finally:
        connection.close()


if __name__ == '__main__':
    main()
