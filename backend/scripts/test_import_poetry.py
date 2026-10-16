import unittest
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from import_poetry import DATASETS, normalize, records, select_datasets


class PoetryImportTest(unittest.TestCase):
    def poem(self, **changes):
        value = {"author": "王維", "title": "相思", "paragraphs": ["紅豆生南國，春來發幾枝。"], "id": "stable-id"}
        value.update(changes)
        return value

    def normalize(self, record, dataset="tang", ref="全唐诗/poet.tang.0.json"):
        return normalize(record, dataset, ref, "local")

    def test_original_and_simplified(self):
        poem = self.normalize(self.poem())
        self.assertEqual(poem["author_name"], "王维")
        self.assertIn("红豆生南国", poem["content"])
        self.assertIn("紅豆生南國", poem["original_content"])

    def test_id_survives_corrections_and_file_changes(self):
        first = self.normalize(self.poem())
        second = self.normalize(self.poem(paragraphs=["红豆生南国。"]), ref="another.json")
        self.assertEqual(first["source_key"], second["source_key"])

    def test_no_id_same_title_different_poems(self):
        first = self.normalize(self.poem(id=None, title=None, rhythmic="浣溪沙"), "song-ci")
        second = self.normalize(self.poem(id=None, title=None, rhythmic="浣溪沙", paragraphs=["一曲新词酒一杯。"]), "song-ci")
        self.assertNotEqual(first["source_key"], second["source_key"])
        self.assertTrue(first["title"].startswith("浣溪沙 · "))

    def test_duplicate_collections_same_key(self):
        poem = self.poem(id=None)
        self.assertEqual(self.normalize(poem)["source_key"], self.normalize(poem, ref="selection.json")["source_key"])

    def test_invalid_data(self):
        for changes in [{"paragraphs": []}, {"paragraphs": "正文"}, {"author": ""}, {"excerpt": "不在诗中", "featured_rank": 1}]:
            with self.assertRaises(ValueError):
                self.normalize(self.poem(**changes))

    def test_bundled_selection(self):
        poems = [normalize(*item) for item in records(None, [], "local")]
        self.assertEqual(len(poems), 9)
        self.assertEqual(len({p["source_key"] for p in poems}), 9)
        self.assertTrue(all(p["excerpt"] and p["featured_rank"] for p in poems))

    def test_new_collection_formats(self):
        cases = [
            ("shijing", {"title": "关雎", "chapter": "国风", "section": "周南", "content": ["关关雎鸠。"]}, "佚名", "国风 · 周南 · 关雎", "诗"),
            ("chuci", {"title": "湘君", "section": "九歌", "author": "屈原", "content": ["君不行兮夷犹。"]}, "屈原", "九歌 · 湘君", "辞赋"),
            ("yuanqu", {"title": "天净沙·秋思", "author": "马致远", "paragraphs": ["枯藤老树昏鸦。"]}, "马致远", "天净沙·秋思", "曲"),
            ("huajianji", {"title": "临江仙", "author": "尹鹗 六首", "paragraphs": ["一番荷芰生池沼。"]}, "尹鹗", "临江仙", "词"),
            ("nantang", {"title": "虞美人", "author": "李煜", "paragraphs": ["春花秋月何时了。"]}, "李煜", "虞美人", "词"),
            ("caocao", {"title": "观沧海", "paragraphs": ["东临碣石，以观沧海。"]}, "曹操", "观沧海", "诗"),
            ("nalan", {"title": "长相思", "author": "纳兰性德", "para": ["山一程，水一程。", "", "风一更，雪一更。"]}, "纳兰性德", "长相思", "诗词"),
        ]
        for code, record, author, title, kind in cases:
            with self.subTest(dataset=code):
                result = self.normalize(record, code)
                self.assertEqual(result["author_name"], author)
                self.assertEqual(result["title"], title)
                self.assertEqual(result["kind"], kind)
                self.assertNotIn("\n\n", result["content"])

    def test_sections_distinguish_same_titles(self):
        record = {"title": "柏舟", "chapter": "国风", "section": "邶风", "content": ["泛彼柏舟。"]}
        first = self.normalize(record, "shijing")
        second = self.normalize(dict(record, section="鄘风"), "shijing")
        self.assertNotEqual(first["source_key"], second["source_key"])

    def test_empty_text_is_not_imported_and_untitled_nalan_is_readable(self):
        for code, body in [("yuanqu", {"paragraphs": []}), ("nalan", {"para": ["", " "]})]:
            with self.assertRaises(ValueError):
                self.normalize(dict(self.poem(), **body), code)
        result = self.normalize({"author": "纳兰性德", "title": "", "para": ["朔风吹古柳"]}, "nalan")
        self.assertEqual(result["title"], "无题 · 朔风吹古柳")

    def test_huajianji_excludes_preface(self):
        with TemporaryDirectory() as directory:
            source = Path(directory)
            folder = source / DATASETS["huajianji"]["folder"]
            folder.mkdir(parents=True)
            for filename in ["huajianji-0-preface.json", "huajianji-1-juan.json", "huajianji-x-juan.json"]:
                (folder / filename).write_text(json.dumps([self.poem()]), encoding="utf-8")
            imported = [item for item in records(source, ["huajianji"], "local") if item[1] == "huajianji"]
            self.assertEqual(len(imported), 2)
            self.assertTrue(all("preface" not in item[2] for item in imported))

    def test_dataset_selection(self):
        self.assertEqual(select_datasets("all"), list(DATASETS))
        self.assertEqual(select_datasets("shijing, chuci,shijing"), ["shijing", "chuci"])
        for value in ["unknown", "all,tang", ""]:
            with self.assertRaises(ValueError):
                select_datasets(value)


if __name__ == "__main__":
    unittest.main()
