```py
py -m pip install -r backend/scripts/requirements.txt

# 只检查文件，不连接数据库
py backend/scripts/import_poetry.py --source "D:\Data\chinese-poetry" --check

py backend/scripts/import_poetry.py --source "D:\Data\chinese-poetry" --dataset shijing,chuci,yuanqu,huajianji,nantang,caocao,nalan,tang,song-ci --apply
```

