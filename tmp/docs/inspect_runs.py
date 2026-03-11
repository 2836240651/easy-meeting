from docx import Document
from pathlib import Path
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph

DOCX = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节_第5章表题字号字体修正.docx")

def iter_blocks(doc):
    body = doc.element.body
    for child in body.iterchildren():
        if isinstance(child, CT_P):
            yield Paragraph(child, doc)
        elif isinstance(child, CT_Tbl):
            yield Table(child, doc)

def is_chapter_heading(text, chapter):
    t = text.strip()
    return t.startswith(f"第{chapter}章") or t.startswith("第" + "一二三四五六七八九十"[chapter-1] + "章")

doc = Document(DOCX)
blocks = list(iter_blocks(doc))

in_ch5=False
ch5_table_idxs=[]
for i,b in enumerate(blocks):
    if isinstance(b, Paragraph):
        tx=(b.text or '').strip()
        if is_chapter_heading(tx,5):
            in_ch5=True
        elif is_chapter_heading(tx,6) and in_ch5:
            in_ch5=False
    if in_ch5 and isinstance(b, Table):
        ch5_table_idxs.append(i)

for wanted in (11,12):
    t_idx=ch5_table_idxs[wanted-1]
    j=t_idx-1
    cap=None
    while j>=0:
        if isinstance(blocks[j], Paragraph):
            if (blocks[j].text or '').strip():
                cap=blocks[j]
                break
        elif isinstance(blocks[j], Table):
            break
        j-=1
    print('Table',wanted,'caption para text:',repr(cap.text))
    print(' runs:',[repr(r.text) for r in cap.runs])
