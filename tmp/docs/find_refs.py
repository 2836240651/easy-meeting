import re
from pathlib import Path
from docx import Document

SRC = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节.docx")

doc = Document(SRC)

in_ch5=False
paras=[]
for p in doc.paragraphs:
    t=(p.text or '').strip()
    if t.startswith('第5章') or t.startswith('第五章'):
        in_ch5=True
    elif in_ch5 and (t.startswith('第6章') or t.startswith('第六章')):
        in_ch5=False
    if in_ch5 and t:
        paras.append(t)

pat=re.compile(r"表\s*[-–—－]?\s*\d+(?:[\.-]\d+)*")
found=[]
for t in paras:
    if '表' in t:
        ms=pat.findall(t)
        if ms:
            found.append((t, ms))

print('found paras with table refs:', len(found))
for t, ms in found[:50]:
    print('-', t)
