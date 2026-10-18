Сборка словаря (запускать из папки tools/):
  pip install wordfreq eng-to-ipa lemminflect better-profanity
  1) build_dictionary.py  — первичная сборка слов (OpenRussian + wordfreq) → assets/words_1..6.csv
  2) enrich_dictionary.py — читает base/words_N.csv, удаляет нецензурные слова, добавляет примеры
                            предложений и пишет assets/words_N.csv + assets/blocklist_*.txt
Данные (в .gitignore, лежат в tools/data/):
  data/tatoeba_ru_en.csv  — пары предложений (Tatoeba, CC BY 2.0 FR), столбцы rus,eng
  data/openrussian/*.csv  — https://github.com/Badestrand/russian-dictionary (CC BY-SA 4.0)
blocklists.py — списки нецензурной лексики (английские слова и русские шаблоны).
*.hand — ручные слова с примерами.
