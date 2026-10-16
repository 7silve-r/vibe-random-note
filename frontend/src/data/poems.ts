export interface Poem {
  id: number;
  title: string;
  author: string;
  authorId: number;
  dynasty: string;
  kind: string;
  dataset: string;
  rhythmic?: string;
  paragraphs: string[];
  excerpt?: string;
  authorDescription?: string;
  collected: boolean;
  sourceRef: string;
  sourceVersion: string;
}
export interface PoetryCatalog {
  collections: { code: string; name: string; total: number }[];
  dynasties: string[];
  kinds: string[];
}
export function localDay(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function poemQuote(poem: Poem) {
  return (
    poem.paragraphs.map((line) => '> ' + line).join('\n') +
    `\n> ——${poem.author}《${poem.title}》\n\n此刻想到……\n`
  );
}
