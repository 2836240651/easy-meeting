from docx import Document
from pathlib import Path
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph

SRC = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节.docx")

def iter_blocks(doc):
    for child in doc.element.body.iterchildren():
        if isinstance(child, CT_P):
            yield Paragraph(child, doc)
        elif isinstance(child, CT_Tbl):
            yield Table(child, doc)

def is_chapter_heading(text, chapter):
    t=text.strip()
    cn="一二三四五六七八九十"[chapter-1]
    return t.startswith(f"第{chapter}章") or t.startswith(f"第{cn}章")

doc=Document(SRC)
blocks=list(iter_blocks(doc))
in_ch5=False
table_idxs=[]
for i,b in enumerate(blocks):
    if isinstance(b, Paragraph):
        tx=(b.text or '').strip()
        if is_chapter_heading(tx,5):
            in_ch5=True
        elif is_chapter_heading(tx,6) and in_ch5:
            in_ch5=False
    if in_ch5 and isinstance(b, Table):
        table_idxs.append(i)

wanted=18
idx=table_idxs[wanted-1]
print('block idx',idx)
# show previous and next paragraphs around table
for offset in range(-6, 7):
    j=idx+offset
    if j<0 or j>=len(blocks):
        continue
    b=blocks[j]
    if isinstance(b, Paragraph):
        t=(b.text or '').strip()
        if t:
            print(f"P[{j}]: {t}")
    else:
        print(f"T[{j}] rows={len(b.rows)} cols={len(b.columns)}")
        # print first 2 rows text
        for r in range(min(2,len(b.rows))):
            row = ' | '.join(c.text.strip().replace('\n',' ') for c in b.rows[r].cells)
            print('  row',r,':',row)
