"""
Обогащение словарей: 1) удаляет нецензурные слова и переводы,
2) подбирает каждому слову пример предложения с переводом (корпус Tatoeba, CC BY 2.0 FR),
3) пишет списки блокировки для приложения (assets/blocklist_en.txt, blocklist_ru.txt).
Запуск из папки tools/:  python3 enrich_dictionary.py
Нужно: pip install wordfreq lemminflect better-profanity
"""
import csv, re, os, collections
from wordfreq import zipf_frequency
from lemminflect import getAllInflections
from better_profanity import profanity
from blocklists import EN_BAD, RU_PATTERNS, en_bad, ru_bad

ASSETS = '../app/src/main/assets'
CORPUS = 'data/tatoeba_ru_en.csv'          # столбцы: rus,eng (нижний регистр, без пунктуации)

profanity.load_censor_words()
BP = {str(w).lower() for w in profanity.CENSOR_WORDSET}    # строгий список — только для предложений

# ---------- 1. Корпус предложений ----------
EN_NAMES = {'tom','mary','john','alice','boston','australia','japan','england','english','french','german',
            'russian','japanese','chinese','spanish','italian','america','american','canada','paris','london',
            'tokyo','germany','france','russia','europe','christmas','monday','tuesday','wednesday','thursday',
            'friday','saturday','sunday','january','february','april','june','july','august','september',
            'october','november','december','boston','chicago','bob','ken','jack','mike','peter','fred','jim'}
RU_NAMES = {'том':'Том','мэри':'Мэри','джон':'Джон','алиса':'Алиса','бостон':'Бостон','япония':'Япония',
            'австралия':'Австралия','англия':'Англия','москва':'Москва','россия':'Россия','америка':'Америка',
            'париж':'Париж','лондон':'Лондон','токио':'Токио','германия':'Германия','франция':'Франция',
            'канада':'Канада','европа':'Европа','боб':'Боб','тома':'Тома','тому':'Тому','томом':'Томом','джона':'Джона','джону':'Джону','алисе':'Алисе','мэри':'Мэри','кен':'Кен','джек':'Джек','майк':'Майк','питер':'Питер'}
Q_FIRST = {'what','who','where','when','why','how','which','whose','is','are','was','were','does','did','am',
           'can','could','would','should','shall','may','might'}
PRON = {'you','i','we','they','he','she','it','there','this','that'}

def is_question(toks):
    f = toks[0]
    if f in Q_FIRST: return True
    if f in ('do', 'have', 'has', 'had', 'will') and len(toks) > 1 and toks[1] in PRON | {"n't"}: return True
    return False

def fix_en(s, q):
    s = re.sub(r"\bi\b", "I", s)
    s = re.sub(r"\b([a-z']+)\b", lambda m: m.group(1).capitalize() if m.group(1) in EN_NAMES else m.group(1), s)
    s = s[0].upper() + s[1:]
    return s + ('?' if q else '.')

def fix_ru(s, q):
    s = s.replace('\u0301', '')                            # убираем знаки ударения
    s = ' '.join(RU_NAMES.get(t, t) for t in s.split())
    s = s[0].upper() + s[1:]
    return s + ('?' if q else '.')

zc = {}
def zipf(t):
    if t not in zc: zc[t] = zipf_frequency(t, 'en')
    return zc[t]

sent = []          # (en_fixed, ru_fixed, tokens, rare_count, bp_tokens)
index = collections.defaultdict(list)
for ru, en in list(csv.reader(open(CORPUS, encoding='utf-8')))[1:]:
    toks = re.findall(r"[a-z']+", en)
    if not (4 <= len(toks) <= 11) or len(ru.split()) > 12 or re.search(r'\d', en + ru): continue
    if any(t in EN_BAD for t in toks) or ru_bad(ru): continue
    q = is_question(toks)
    bp = frozenset(t for t in toks if t in BP)
    rare = sum(1 for t in toks if zipf(t) < 3.3)
    i = len(sent)
    sent.append((fix_en(en, q), fix_ru(ru, q), toks, rare, bp))
    for t in set(toks): index[t].append(i)
print('предложений в корпусе после фильтра:', len(sent))

# ---------- 2. Формы слова ----------
def forms(w):
    s = {w}
    for fs in getAllInflections(w).values(): s.update(fs)
    s.update([w+'s', w+'es', w+'ed', w+'d', w+'ing'])
    if w.endswith('e'): s.add(w[:-1] + 'ing')
    if w.endswith('y'): s.update([w[:-1]+'ies', w[:-1]+'ied'])
    return s

# ---------- 3. Чтение словарей и фильтрация ----------
levels = {}
dropped = []
for lvl in range(1, 7):
    head, rows = [], []
    for line in open(f'base/words_{lvl}.csv', encoding='utf-8'):
        line = line.rstrip('\n')
        if line.startswith('#') or not line.strip(): head.append(line); continue
        p = line.split(';') + [''] * 5
        en, ru, tr, ee, er = [x.strip() for x in p[:5]]
        if en_bad(en): dropped.append(en); continue
        parts = [x.strip() for x in ru.split(',') if x.strip()]
        good = [x for x in parts if not ru_bad(x)]
        if not good: dropped.append(en); continue
        rows.append([en, ', '.join(good), tr, ee, er])
    levels[lvl] = (head, rows)
print('удалено слов целиком:', len(dropped))

# ---------- 4. Подбор примеров ----------
todo = []
for lvl, (_, rows) in levels.items():
    for r in rows:
        if not r[3]: todo.append(r)
cand = {}
for r in todo:
    w = r[0].lower()
    toks = w.split()
    fs = forms(toks[0]) if len(toks) == 1 else forms(toks[0])
    ids = set()
    for f in fs: ids.update(index.get(f, []))
    if len(toks) > 1:
        ids = {i for i in ids if w.split()[-1] in sent[i][2]}
    cand[r[0]] = (fs, ids)
used = collections.Counter()
found = 0
for r in sorted(todo, key=lambda r: len(cand[r[0]][1])):     # сначала редкие слова
    fs, ids = cand[r[0]]
    best, bs = None, 1e9
    for i in ids:
        en, ru, toks, rare, bp = sent[i]
        if bp - fs: continue                                  # строгий фильтр, кроме самого слова
        w = r[0].lower()
        if ' ' in w and w not in ' '.join(toks): continue
        own_rare = sum(1 for t in toks if t in fs and zipf(t) < 3.3)
        sc = abs(len(toks) - 7) * 0.6 + (rare - own_rare) * 2.5 + (0 if w in toks else 0.7) + used[i] * 3
        if sc < bs: best, bs = i, sc
    if best is not None:
        r[3], r[4] = sent[best][0].replace(';', ','), sent[best][1].replace(';', ',')
        used[best] += 1; found += 1
print(f'примеры найдены для {found} из {len(todo)} слов без примера')

# ---------- 4b. Шаблонные предложения для редких слов без реального примера ----------
# Используются ТОЛЬКО если часть речи совпала в английском (lemminflect) и русском (OpenRussian);
# русские формы берутся из таблиц склонения, поэтому грамматика верна в обоих языках.
from lemminflect import getAllLemmas
def unacc(x): return (x or '').replace("'", '').strip()
OR = collections.defaultdict(list)        # русская лемма -> [(pos, строка таблицы)]
for fname, pos in [('nouns', 'n'), ('verbs', 'v'), ('adjectives', 'a')]:
    for row in csv.DictReader(open(f'data/openrussian/{fname}.csv', encoding='utf-8'), delimiter='\t'):
        OR[row['bare'].strip()].append((pos, row))

ADV_RU = {r['bare'].strip() for r in csv.DictReader(open('data/openrussian/others.csv', encoding='utf-8'), delimiter='\t')}

def template(en, ru_list):
    if not re.fullmatch(r'[a-z]+', en): return None
    lem = getAllLemmas(en)
    pos_en = {p for p, ls in lem.items() if en in ls}      # слово — начальная форма этой части речи
    for ru in ru_list:
        for pos, row in OR.get(ru, []):
            if pos == 'v' and 'VERB' in pos_en:
                if len(en) % 2 == 0: return f'I want to {en}.', f'Я хочу {ru}.'
                return f'You can {en}.', f'Ты можешь {ru}.'
            if pos == 'a' and 'ADJ' in pos_en:
                n = unacc(row.get('decl_n_nom'))
                if n: return f'It is {en}.', f'Оно {n}.'
            if pos == 'n' and 'NOUN' in pos_en:
                acc = unacc(row.get('pl_acc' if row.get('pl_only') == '1' else 'sg_acc'))
                if acc:
                    if len(en) % 2 == 0: return f'Look at the {en}!', f'Посмотри на {acc}!'
                    return f'I can see the {en}.', f'Я вижу {acc}.'
    # наречия образа действия: английское -ly ↔ русское -о (неизменяемое слово)
    if en.endswith('ly') and 'ADV' in getAllLemmas(en):
        for ru in ru_list:
            if ru in ADV_RU and ru.endswith('о') and ' ' not in ru:
                return f'He did it {en}.', f'Он сделал это {ru}.'
    return None

tmpl = 0
for lvl, (_, rows) in levels.items():
    for r in rows:
        if r[3]: continue
        t = template(r[0].lower(), [x.strip() for x in r[1].split(',')])
        if t: r[3], r[4] = t; tmpl += 1
print('шаблонных предложений добавлено:', tmpl)

# ---------- 4c. Знаки ударения (U+0301) в русских словах и предложениях ----------
# Нужны тем, кто изучает русский. В приложении показываются только при изучении русского,
# а при поиске, проверке ответов и озвучке игнорируются.
import accents
accents.load()
n_tr = n_ex = 0
for lvl, (_, rows) in levels.items():
    for r in rows:
        parts = [x.strip() for x in r[1].split(',') if x.strip()]
        new = [accents.accent_translation(p, r[0].lower()) for p in parts]
        if new != parts: n_tr += 1
        r[1] = ', '.join(new)
        if r[4]:
            e = accents.accent_text(r[4])
            if e != r[4]: n_ex += 1
            r[4] = e
print(f'ударения: переводов с отметкой {n_tr}, русских предложений с отметкой {n_ex}')

# ---------- 5. Запись ----------
total = covered = 0
for lvl, (head, rows) in levels.items():
    with open(f'{ASSETS}/words_{lvl}.csv', 'w', encoding='utf-8') as f:
        for h in head: f.write(h + '\n')
        for r in rows: f.write(';'.join(r) + '\n')
    c = sum(1 for r in rows if r[3]); total += len(rows); covered += c
    print(f'уровень {lvl}: {len(rows)} слов, с примером {c} ({100*c//len(rows)}%)')
print('ИТОГО', total, 'с примером', covered)

open(f'{ASSETS}/blocklist_en.txt', 'w', encoding='utf-8').write('\n'.join(sorted(EN_BAD)) + '\n')
open(f'{ASSETS}/blocklist_ru.txt', 'w', encoding='utf-8').write('\n'.join(RU_PATTERNS) + '\n')
