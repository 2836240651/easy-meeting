from docx import Document
from pathlib import Path
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph

DOCX = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节_第5章表题字号字体修正_最终.docx")


def iter_blocks(doc):
    for child in doc.element.body.iterchildren():
        if isinstance(child, CT_P):
            yield Paragraph(child, doc)
        elif isinstance(child, CT_Tbl):
            yield Table(child, doc)

def is_chapter_heading(text, chapter):
    t=text.strip(); cn="一二三四五六七八九十"[chapter-1]
    return t.startswith(f"第{chapter}章") or t.startswith(f"第{cn}章")

def is_blank_table(t: Table):
    for row in t.rows:
        for cell in row.cells:
            if cell.text and cell.text.strip():
                return False
    return True

doc=Document(DOCX)
blocks=list(iter_blocks(doc))

in_ch=False
idxs=[]
for i,b in enumerate(blocks):
    if isinstance(b, Paragraph):
        tx=(b.text or '').strip()
        if is_chapter_heading(tx,3):
            in_ch=True
        elif is_chapter_heading(tx,4) and in_ch:
            in_ch=False
    if in_ch and isinstance(b, Table) and not is_blank_table(b):
        idxs.append(i)

print('chapter3 nonblank tables', len(idxs))
for k,tidx in enumerate(idxs[:25], start=1):
    # immediate prev non-empty paragraph
    j=tidx-1
    cap=None
    while j>=0:
        if isinstance(blocks[j], Paragraph):
            t=(blocks[j].text or '').strip()
            if t:
                cap=t
                break
        elif isinstance(blocks[j], Table):
            break
        j-=1
    t = blocks[tidx]
    header = ' | '.join(c.text.strip().replace('\n',' ') for c in t.rows[0].cells) if t.rows else ''
    print(k, 'cap:', cap)
    print('  header:', header[:120])
