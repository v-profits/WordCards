"""
Расстановка знаков ударения (U+0301) в русских словах по данным OpenRussian.
- accent_translation(ru, en): ударение для перевода; омографы различаем по английскому значению;
- accent_text(text): ударения в предложении по таблице всех словоформ; неоднозначные формы
  (омографы вроде «за́мок/замо́к», «все/всё») пропускаются, чтобы не поставить ударение неверно.
"""
import csv, re, collections

ACUTE = "\u0301"
VOW = set("аеёиоуыэюя")
SKIP_COLS = {'translations_en', 'translations_de', 'gender', 'partner', 'animate', 'indeclinable',
             'sg_only', 'pl_only', 'aspect', 'bare'}
WORD = re.compile(r"[А-Яа-яЁё]+")

def nvow(w): return sum(ch in VOW for ch in w.lower())
def key(w): return w.lower().replace('ё', 'е')

FORMS = collections.defaultdict(set)      # слово без ударения (ё→е) -> {варианты с апострофом}
ENTRIES = collections.defaultdict(list)   # лемма -> [(accented, {английские переводы})]

def _clean_en(p, pos):
    p = re.sub(r'\(.*?\)', '', p).strip().lower()
    return p[3:] if pos == 'v' and p.startswith('to ') else p

def load(path='data/openrussian'):
    for fname, pos in [('nouns', 'n'), ('verbs', 'v'), ('adjectives', 'a'), ('others', 'o')]:
        for row in csv.DictReader(open(f'{path}/{fname}.csv', encoding='utf-8'), delimiter='\t'):
            bare = row['bare'].strip().lower()
            acc = (row.get('accented') or '').strip().lower()
            ens = {_clean_en(p, pos) for p in re.split(r'[;,]', row.get('translations_en') or '') if p.strip()}
            ENTRIES[bare].append((acc, ens))
            if acc: FORMS[key(bare)].add(acc)
            for col, val in row.items():
                if col is None or col in SKIP_COLS or not isinstance(val, str) or not val: continue
                for f in val.split(','):
                    f = f.strip().lower()
                    if re.fullmatch(r"[а-яё'\-]+", f) and re.search(r'[а-яё]', f):
                        FORMS[key(f.replace("'", ""))].add(f)

def _insert(token, acc):
    """Вставляет знак ударения в token по варианту acc (с апострофом после ударной гласной)."""
    if acc.count("'") != 1 or 'ё' in acc: return token
    p = acc.index("'")
    if acc.replace("'", "") != token.lower().replace('ё', 'е'): return token
    return token[:p] + ACUTE + token[p:]

def accent_token(tok):
    tl = tok.lower()
    if 'ё' in tl or nvow(tl) <= 1: return tok
    v = FORMS.get(key(tl))
    if not v or len(v) != 1: return tok
    return _insert(tok, next(iter(v)))

def accent_text(text):
    return WORD.sub(lambda m: accent_token(m.group()), text)

def accent_translation(ru, en):
    """Перевод (одно слово или словосочетание). Для одного слова выбираем статью с нужным английским значением."""
    if re.fullmatch(r"[а-яё]+", ru) and nvow(ru) > 1 and 'ё' not in ru:
        ents = ENTRIES.get(ru, [])
        match = {a for a, ens in ents if en in ens and a}
        pool = match or {a for a, _ in ents if a}
        if len(pool) == 1:
            r = _insert(ru, next(iter(pool)))
            if r != ru: return r
    return accent_text(ru)
