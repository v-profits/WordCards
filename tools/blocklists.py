"""Списки нецензурной лексики: английские базовые слова и русские шаблоны (целое слово)."""
import re
from lemminflect import getAllInflections

# Базовые английские слова: вульгарная лексика, оскорбления, унизительные ярлыки.
# Нейтральные слова (god, kill, sex, vodka, medical terms…) сюда намеренно НЕ входят.
EN_BASE = """
ass asshole arse arsehole bastard bitch bollocks bullshit bugger chink cock coon crap cum cunt damn dammit
dick dyke douche dumbass jackass dipshit fag faggot fart fuck fucker fucked fucking goddamn horny jerkoff
kike knob masturbate masturbation motherfucker muff negro nigga nigger orgasm orgy ejaculate ejaculation
piss pissed pimp porn porno pornography prick pussy queer retard retarded shag shit shite shitty slut smut
smutty spic tit tits tosser twat wank wanker whore wop hooker tranny cumshot blowjob handjob jizz boob boobs
bloody bugger gook paki scumbag sod dildo
""".split()

# Русские шаблоны (re.fullmatch по слову в нижнем регистре, «ё» заменено на «е»)
PFX = r"(за|вы|от|у|на|по|съ|под|про|до|об|раз|при|пере|ис|рас|с)?"   # допустимые приставки
RU_PATTERNS = [
    r"(на|по|о|ох|за|вы|до|раз|ни|не)?ху[йяюе]\w*",
    r"\w*пизд\w*",
    PFX + r"еб(ат\w*|ан\w*|ну\w*|ал\w*|лан\w*|уч\w*|ыр\w*|ис\w*|ня)",
    PFX + r"бл(яд|ят)\w*", r"бля",
    r"муда[кч]\w*", r"мудил\w*", r"мудозвон\w*",
    r"\w*залуп\w*",
    r"пидор\w*", r"пидар\w*", r"пидр\w*", r"педик(а|у|ом|и|ов|е)?", r"педераст\w*",
    r"\w*говн\w*", r"дерьм\w*",
    r"сука", r"суки", r"суку", r"суке", r"сукой", r"сучк(а|и|е|у|ой|ам|ами)", r"сучар\w*", r"сукин\w*",
    r"шлюх\w*", r"шалав\w*", r"курв\w*",
    r"дроч(ить|ит|у|ишь|ил\w*|ун\w*|ка)", r"\w*жоп\w*", r"срак\w*", r"срать", r"срал\w*", r"срань", r"сран\w*",
    r"хер", r"хера", r"херу", r"хером", r"хере", r"херн\w*", r"херов\w*",
    r"перд(еть|еж|ун\w*|ит\w*|ел\w*)", r"перну\w*",
    r"ссыт", r"ссыш\w*", r"ссыкун\w*", r"ссыкл\w*", r"ссан\w*", r"сса(ть|л\w*)",
    r"трах(ать|нуть|нул\w*|ал\w*)",
    r"гандон\w*", r"гондон\w*", r"ублюд\w*", r"падла", r"падлюк\w*", r"чмо", r"мандавошк\w*",
    r"долбо[её]б\w*",
]
RU_RE = [re.compile(p) for p in RU_PATTERNS]

def norm_ru(t):
    return t.lower().replace("ё", "е")

def ru_bad(text):
    return any(r.fullmatch(t) for t in re.findall(r"[а-яё]+", norm_ru(text)) for r in RU_RE)

def en_expand():
    out = set()
    for w in EN_BASE:
        out.add(w)
        for forms in getAllInflections(w).values():
            out.update(forms)
        out.update([w + "s", w + "es", w + "ed", w + "ing"])
    return out

EN_BAD = en_expand()

def en_bad(text):
    return any(t in EN_BAD for t in re.findall(r"[a-z']+", text.lower()))

if __name__ == "__main__":
    # Проверка шаблонов на ложные срабатывания по всем русским леммам OpenRussian
    import csv
    hits = []
    for f in ["nouns", "verbs", "adjectives", "others"]:
        for row in csv.DictReader(open(f"/tmp/ord/{f}.csv", encoding="utf-8"), delimiter="\t"):
            if ru_bad(row["bare"]): hits.append(row["bare"])
    print(len(hits), "русских лемм сработало:", sorted(set(hits)))
