INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('04b97c8f627b7661fd5eac3168dc438f32683fc99452e88af7271b7f9c06c4f2','王维','王維','唐') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='04b97c8f627b7661fd5eac3168dc438f32683fc99452e88af7271b7f9c06c4f2'),'fedda5cf1db3feb0b6c469d878c575fe8d047b60875a31820f43fc2d5a9e432a','tang','全唐诗/唐诗三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','王维','山居秋暝','山居秋暝','','唐','诗','空山新雨后，天气晚来秋。
明月松间照，清泉石上流。
竹喧归浣女，莲动下渔舟。
随意春芳歇，王孙自可留。','空山新雨後，天氣晚來秋。
明月松間照，清泉石上流。
竹喧歸浣女，蓮動下漁舟。
隨意春芳歇，王孫自可留。','明月松间照，清泉石上流。',1) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);

INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('04b97c8f627b7661fd5eac3168dc438f32683fc99452e88af7271b7f9c06c4f2','王维','王維','唐') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='04b97c8f627b7661fd5eac3168dc438f32683fc99452e88af7271b7f9c06c4f2'),'22248777ba74c1bfe84c4e97a4cd46b38341a3f624fd3f18a3f8b63f36eef0f2','tang','全唐诗/唐诗三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','王维','辋川集 鹿柴','輞川集 鹿柴','','唐','诗','空山不见人，但闻人语响。
返景入深林，复照青苔上。','空山不見人，但聞人語響。
返景入深林，復照青苔上。','返景入深林，复照青苔上。',2) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);

INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('04b97c8f627b7661fd5eac3168dc438f32683fc99452e88af7271b7f9c06c4f2','王维','王維','唐') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='04b97c8f627b7661fd5eac3168dc438f32683fc99452e88af7271b7f9c06c4f2'),'62467038c63498d8af7a3137d766b216413187adb3430b94919954e912cf2235','tang','全唐诗/唐诗三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','王维','辋川集 竹里馆','輞川集 竹里館','','唐','诗','独坐幽篁里，弹琴复长啸。
深林人不知，明月来相照。','獨坐幽篁裏，彈琴復長嘯。
深林人不知，明月來相照。','深林人不知，明月来相照。',3) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);

INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('04b97c8f627b7661fd5eac3168dc438f32683fc99452e88af7271b7f9c06c4f2','王维','王維','唐') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='04b97c8f627b7661fd5eac3168dc438f32683fc99452e88af7271b7f9c06c4f2'),'ca9861ffc52af8fa97aace3c869a80131e5beef4f1f9886911fa794c6db7c6ca','tang','全唐诗/唐诗三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','王维','相思','相思','','唐','诗','红豆生南国，秋来发故枝。
愿君多采撷，此物最相思。','紅豆生南國，秋來發故枝。
願君多采擷，此物最相思。','愿君多采撷，此物最相思。',4) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);

INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('a2d9a65f639c69e2105569e80724c02343e3752da64cd39c235d08386aba358d','李白','李白','唐') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='a2d9a65f639c69e2105569e80724c02343e3752da64cd39c235d08386aba358d'),'d0505922f531e05ceb7d22a15ed2ec65a97f30251725c05c86cbe347ebd38e04','tang','全唐诗/唐诗三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','李白','静夜思','靜夜思','','唐','诗','床前看月光，疑是地上霜。
举头望山月，低头思故乡。','牀前看月光，疑是地上霜。
舉頭望山月，低頭思故鄉。','举头望山月，低头思故乡。',5) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);

INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('a2d9a65f639c69e2105569e80724c02343e3752da64cd39c235d08386aba358d','李白','李白','唐') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='a2d9a65f639c69e2105569e80724c02343e3752da64cd39c235d08386aba358d'),'5ce5e5b77786139cf8d86b9caf88c624c86dbfcfebf383231f2673fd67c7752d','tang','全唐诗/唐诗三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','李白','早发白帝城','早發白帝城','','唐','诗','朝辞白帝彩云间，千里江陵一日还。
两岸猨声啼不尽，轻舟已过万重山。','朝辭白帝彩雲間，千里江陵一日還。
兩岸猨聲啼不盡，輕舟已過萬重山。','两岸猨声啼不尽，轻舟已过万重山。',6) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);

INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('dda32156d0384e9b41635b11423ba8bab650a25546fb945990b21d3ee059e842','王之涣','王之渙','唐') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='dda32156d0384e9b41635b11423ba8bab650a25546fb945990b21d3ee059e842'),'7de2d4ceef31ba29839d6d4af36bfa773f89a9cabb70150d4ff90965710e67bd','tang','全唐诗/唐诗三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','王之涣','登鹳雀楼','登鸛雀樓','','唐','诗','白日依山尽，黄河入海流。
欲穷千里目，更上一层楼。','白日依山盡，黃河入海流。
欲窮千里目，更上一層樓。','欲穷千里目，更上一层楼。',7) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);

INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('0346d7d996392e630c8343c3d0cecc70587d285d8314b39b7f6811b6b6331499','苏轼','苏轼','宋') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='0346d7d996392e630c8343c3d0cecc70587d285d8314b39b7f6811b6b6331499'),'d394026a8e35247a36127d2b67ee557985b4026dc20f7685070c7e1399d2007d','song-ci','宋词/宋词三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','苏轼','水调歌头 · 明月几时有，把酒问青天。','水调歌头','水调歌头','宋','词','明月几时有，把酒问青天。
不知天上宫阙，今夕是何年。
我欲乘风归去，又恐琼楼玉宇，高处不胜寒。
起舞弄清影，何似在人间。
转朱阁，低绮户，照无眠。
不应有恨，何事长向别时圆。
人有悲欢离合，月有阴晴圆缺，此事古难全。
但愿人长久，千里共婵娟。','明月几时有，把酒问青天。
不知天上宫阙，今夕是何年。
我欲乘风归去，又恐琼楼玉宇，高处不胜寒。
起舞弄清影，何似在人间。
转朱阁，低绮户，照无眠。
不应有恨，何事长向别时圆。
人有悲欢离合，月有阴晴圆缺，此事古难全。
但愿人长久，千里共婵娟。','明月几时有，把酒问青天。
不知天上宫阙，今夕是何年。',8) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);

INSERT INTO poetry_author (source_key,name,original_name,dynasty) VALUES ('0346d7d996392e630c8343c3d0cecc70587d285d8314b39b7f6811b6b6331499','苏轼','苏轼','宋') ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO poetry_poem (author_id,source_key,dataset,source_ref,source_version,author_name,title,original_title,rhythmic,dynasty,kind,content,original_content,excerpt,featured_rank) VALUES ((SELECT id FROM poetry_author WHERE source_key='0346d7d996392e630c8343c3d0cecc70587d285d8314b39b7f6811b6b6331499'),'d0cb32dc7d3eb14dc121e00799aaac2abc46b3a0980c255a373bb121fca1bb50','song-ci','宋词/宋词三百首.json','b8594f81a89752241442f2ce267d6f66f96704ee','苏轼','定风波 · 莫听穿林打叶声。','定风波','定风波','宋','词','莫听穿林打叶声。
何妨吟啸且徐行。
竹杖芒鞋轻胜马。
谁怕。
一蓑烟雨任平生。
料峭春风吹酒醒。
微冷。
山头斜照却相迎。
回首向来萧瑟处。
归去。
也无风雨也无晴。','莫听穿林打叶声。
何妨吟啸且徐行。
竹杖芒鞋轻胜马。
谁怕。
一蓑烟雨任平生。
料峭春风吹酒醒。
微冷。
山头斜照却相迎。
回首向来萧瑟处。
归去。
也无风雨也无晴。','莫听穿林打叶声。
何妨吟啸且徐行。',9) ON DUPLICATE KEY UPDATE excerpt=VALUES(excerpt),featured_rank=VALUES(featured_rank);
