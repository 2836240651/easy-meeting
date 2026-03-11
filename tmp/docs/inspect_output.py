from docx import Document
from pathlib import Path
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph

DOCX = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节_第5章表题字号字体修正.docx")


def iter_block_items(doc):
    body = doc.element.body
    for child in body.iterchildren():
        if isinstance(child, CT_P):
            yield Paragraph(child, doc)
        elif isinstance(child, CT_Tbl):
            yield Table(child, doc)


def is_chapter_heading(text: str, chapter: int) -> bool:
    t = text.strip()
    return t.startswith(f"第{chapter}章") or t.startswith("第" + "一二三四五六七八九十"[chapter-1] + "章")


doc = Document(DOCX)
blocks = list(iter_block_items(doc))

in_ch5 = False
ch5_tables = []

for idx, b in enumerate(blocks):
    if isinstance(b, Paragraph):
        t = (b.text or '').strip()
        if is_chapter_heading(t, 5):
            in_ch5 = True
        elif is_chapter_heading(t, 6) and in_ch5:
            in_ch5 = False
    if in_ch5 and isinstance(b, Table):
        ch5_tables.append(idx)

print('ch5 tables', len(ch5_tables))

for k, t_idx in enumerate(ch5_tables, start=1):
    # immediate prev non-empty paragraph
    j = t_idx - 1
    caption = None
    while j >= 0:
        if isinstance(blocks[j], Paragraph):
            tx = (blocks[j].text or '').strip()
            if tx:
                caption = tx
                break
        elif isinstance(blocks[j], Table):
            break
        j -= 1
    print(k, caption)
