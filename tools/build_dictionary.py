import csv, re, collections, glob, os
from wordfreq import top_n_list, zipf_frequency
import eng_to_ipa as ipa

# --- 1. Читаем OpenRussian (CC BY-SA 4.0) ---
entries = []
for f, pos in [('nouns','n'),('verbs','v'),('adjectives','a'),('others','o')]:
    for row in csv.DictReader(open(f'ord/{f}.csv', encoding='utf-8'), delimiter='\t'):
        ru, tr = row['bare'].strip(), row['translations_en'].strip()
        if not ru or not tr or ru[0].isupper(): continue      # собственные имена пропускаем
        if not re.fullmatch(r"[а-яё\- ]+", ru): continue
        entries.append((ru, pos, [p.strip() for p in re.split(r'[;,]', tr) if p.strip()]))

inv = collections.defaultdict(dict)
for ru, pos, parts in entries:
    rz = zipf_frequency(ru, 'ru')
    for i, p in enumerate(parts):
        p = re.sub(r'\(.*?\)', '', p).strip().lower()
        if pos == 'v' and p.startswith('to '): p = p[3:]
        if not re.fullmatch(r"[a-z][a-z'-]*", p): continue
        s = rz - 0.6 * i
        if ru not in inv[p] or inv[p][ru] < s: inv[p][ru] = s

# --- 2. Ручные переводы самых частых служебных слов (автоинверсия тут ошибается) ---
manual = {
 'the':'(артикль)','to':'к, до; (частица to)','and':'и','of':'из, о (предлог of)','a':'(артикль)','in':'в','is':'есть, является',
 'that':'тот, что','it':'оно, это','for':'для, за','you':'ты, вы','was':'был, была','on':'на','with':'с','as':'как',
 'be':'быть','at':'в, у, на','by':'у, рядом, посредством','this':'этот','have':'иметь','from':'из, от','or':'или','are':'есть, являются',
 'not':'не','but':'но','what':'что, какой','all':'все, всё','were':'были','we':'мы','when':'когда','your':'твой, ваш','can':'мочь, уметь',
 'said':'сказал','there':'там, (есть)','use':'использовать','an':'(артикль)','each':'каждый','which':'который, какой','she':'она','do':'делать',
 'how':'как','their':'их','if':'если','will':'будет (вспом. глагол)','up':'вверх','other':'другой','about':'о, около','out':'вне, наружу',
 'many':'много','then':'потом, тогда','them':'им, их','these':'эти','so':'так, поэтому','some':'некоторые, несколько','her':'её, ей','would':'бы (вспом. глагол)',
 'make':'делать, создавать','like':'нравиться, как','him':'ему, его','into':'в, внутрь','time':'время, раз','has':'имеет','look':'смотреть, взгляд',
 'two':'два','more':'больше, более','write':'писать','go':'идти, ехать','see':'видеть','number':'число, номер','no':'нет, никакой','way':'путь, способ',
 'could':'мог бы','people':'люди','my':'мой','than':'чем','first':'первый','water':'вода','been':'был (причастие be)','call':'звать, звонить','who':'кто',
 'oil':'масло, нефть','its':'его, её (о предмете)','now':'сейчас','find':'находить','long':'длинный, долго','down':'вниз','day':'день','did':'делал (прош. от do)',
 'get':'получать, становиться','come':'приходить','made':'сделал, сделанный','may':'мочь, можно','part':'часть','i':'я','he':'он','his':'его','they':'они',
 'me':'мне, меня','just':'просто, только что','also':'также, тоже','our':'наш','us':'нас, нам','am':'есть (я есть)','any':'любой, какой-нибудь','only':'только',
 'very':'очень','because':'потому что','over':'над, через','such':'такой','even':'даже','new':'новый','most':'наиболее, большинство','much':'много',
}

# --- 3. Список слов по частотности ---
cands = [w for w in top_n_list('en', 80000) if re.fullmatch(r"[a-z][a-z'-]*", w) and len(w) > 1]
rows = []
for w in cands:
    if w in manual: ru = manual[w]
    elif w in inv:
        best = sorted(inv[w].items(), key=lambda x: -x[1])
        top = best[0][1]
        best = [b for b in best if b[1] >= top - 2.5][:3]          # убираем совсем редкие синонимы
        ru = ', '.join(b[0] for b in best)
    else: continue
    rows.append((w, ru))
    if len(rows) >= 30000: break
print('Слов с переводом:', len(rows))

# --- 4. Ручные слова с примерами (из первой версии) идут первыми в своих уровнях ---
hand = {}
for lvl in range(1, 6):
    path = f'/home/claude/WordCards/app/src/main/assets/words_{lvl}.csv'
    if os.path.exists(path + '.hand'): path += '.hand'
    lines = [l.strip() for l in open(path, encoding='utf-8') if l.strip() and not l.startswith('#')]
    hand[lvl] = lines
hand_en = {l.split(';')[0].lower() for ls in hand.values() for l in ls}
rows = [(w, r) for w, r in rows if w not in hand_en]

# --- 5. Деление на уровни по рангу частотности ---
bounds = [500, 1000, 2000, 3500, 5000]          # размеры уровней 1..5, шестой — остаток
levels = {i: [] for i in range(1, 7)}
i = 0
for lvl, size in zip(range(1, 6), bounds):
    levels[lvl] = rows[i:i+size]; i += size
levels[6] = rows[i:]

names = {1:'A1 Начальный',2:'A2 Элементарный',3:'B1 Средний',4:'B2 Выше среднего',5:'C1 Продвинутый',6:'C2 Эксперт / редкая лексика'}
outdir = '/home/claude/WordCards/app/src/main/assets'
for lvl in range(1, 7):
    src = f'{outdir}/words_{lvl}.csv'
    if lvl <= 5 and not os.path.exists(src + '.hand'): os.rename(src, src + '.hand')
    with open(src, 'w', encoding='utf-8') as f:
        f.write(f'# Уровень {lvl}: {names[lvl]}. Формат: english;русский;транскрипция;пример EN;пример RU\n')
        f.write('# Переводы: OpenRussian.org (CC BY-SA 4.0), частотность: wordfreq, IPA: CMUdict\n')
        for l in hand.get(lvl, []): f.write(l + '\n')
        for w, ru in levels[lvl]:
            t = ipa.convert(w)
            t = f'/{t}/' if '*' not in t else ''
            f.write(f'{w};{ru};{t};;\n')
    print(lvl, len(hand.get(lvl, [])) + len(levels[lvl]))
