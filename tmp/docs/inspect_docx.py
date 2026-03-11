from docx import Document
from pathlib import Path
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph

DOCX = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节.docx")

def iter_block_items(parent):
    body = parent.element.body
    for child in body.iterchildren():
        if isinstance(child, CT_P):
            yield Paragraph(child, parent)
        elif isinstance(child, CT_Tbl):
            yield Table(child, parent)


def is_chapter_heading(text: str, chapter: int) -> bool:
    t = text.strip()
    return t.startswith(f"第{chapter}章") or t.startswith(f"第{chapter} 章") or t.startswith(["一","二","三","四","五","六","七","八","九","十"][chapter-1] + "、")


def matches_chapter(text: str, chapter: int) -> bool:
    t = text.strip()
    if chapter == 5:
        return t.startswith("第5章") or t.startswith("第五章")
    if chapter == 6:
        return t.startswith("第6章") or t.startswith("第六章")
    return False


doc = Document(DOCX)
blocks = list(iter_block_items(doc))

chap = None
ch5_tables = []

for idx, b in enumerate(blocks):
    if isinstance(b, Paragraph):
        t = b.text.strip()
        if matches_chapter(t, 5):
            chap = 5
        elif matches_chapter(t, 6):
            chap = 6
    if chap == 5 and isinstance(b, Table):
        ch5_tables.append(idx)

print(f"Total blocks: {len(blocks)}; Chapter 5 tables: {len(ch5_tables)}")

for n, tbl_idx in enumerate(ch5_tables, start=1):
    print("\n---")
    print(f"Table #{n} at block index {tbl_idx}")
    # show previous 3 paragraphs
    prev_paras = []
    j = tbl_idx - 1
    while j >= 0 and len(prev_paras) < 3:
        if isinstance(blocks[j], Paragraph):
            text = blocks[j].text.strip()
            if text:
                prev_paras.append((j, text))
        j -= 1
    for j, text in reversed(prev_paras):
        print(f"Prev para [{j}]: {text[:120]}")
    # show first row texts
    t = blocks[tbl_idx]
    first_row = t.rows[0].cells if t.rows else []
    row_text = " | ".join(c.text.strip().replace("\n"," ") for c in first_row)
    print(f"First row: {row_text[:200]}")
