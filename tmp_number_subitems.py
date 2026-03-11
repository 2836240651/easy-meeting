from pathlib import Path
import re

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


SRC = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全.docx")
OUT = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题格式统一_v3.docx")


def set_run_font(run):
    run.font.name = "Times New Roman"
    run.font.size = Pt(12)
    run.font.bold = False
    run.font.color.rgb = RGBColor(0, 0, 0)
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.rFonts
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:ascii"), "Times New Roman")
    rfonts.set(qn("w:hAnsi"), "Times New Roman")
    rfonts.set(qn("w:eastAsia"), "宋体")


def style_title_paragraph(paragraph):
    paragraph.style = "Body Text"
    paragraph.alignment = WD_ALIGN_PARAGRAPH.LEFT
    fmt = paragraph.paragraph_format
    fmt.first_line_indent = Cm(0.74)
    fmt.left_indent = None
    fmt.right_indent = None
    fmt.space_before = Pt(0)
    fmt.space_after = Pt(0)
    fmt.line_spacing = 1.5


def style_body_paragraph(paragraph):
    if paragraph.style.name not in {"Heading 1", "Heading 2", "Heading 3"}:
        paragraph.style = "Normal"
    paragraph.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    fmt = paragraph.paragraph_format
    fmt.first_line_indent = Cm(0.74)
    fmt.left_indent = None
    fmt.right_indent = None
    fmt.space_before = Pt(0)
    fmt.space_after = Pt(0)
    fmt.line_spacing = 1.5


def is_heading(para):
    return para.style.name in {"Heading 1", "Heading 2", "Heading 3", "章标题", "节标题", "条标题"}


def clean_title_text(text):
    return re.sub(r"^\(\d+\)\s*", "", text).strip()


def is_short_title(text):
    stripped = clean_title_text(text)
    if not stripped:
        return False
    if len(stripped) > 24:
        return False
    if stripped.endswith("。"):
        return False
    if "：" in stripped or ":" in stripped:
        return False
    if stripped.startswith("（") or stripped.startswith("("):
        return False
    return True


doc = Document(str(SRC))
paras = doc.paragraphs

block_starters = (
    "包括",
    "如下",
    "以下几个方面",
    "以下核心特性",
    "主要应用",
    "主要工作和创新点",
)

i = 0
while i < len(paras):
    text = paras[i].text.strip()
    if text and any(key in text for key in block_starters):
        j = i + 1
        count = 1
        while j + 1 < len(paras) and not is_heading(paras[j]) and not is_heading(paras[j + 1]):
            title = paras[j].text.strip()
            body = paras[j + 1].text.strip()
            if not title or not body:
                break
            if not is_short_title(title):
                break
            if len(body) < 25:
                break

            title = clean_title_text(title)
            paras[j].text = f"({count})\t{title}"
            style_title_paragraph(paras[j])
            for run in paras[j].runs:
                set_run_font(run)

            style_body_paragraph(paras[j + 1])
            for run in paras[j + 1].runs:
                set_run_font(run)

            count += 1
            j += 2

        i = j
    else:
        i += 1

doc.save(str(OUT))
print(OUT)
