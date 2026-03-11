from pathlib import Path
import re

from docx import Document


SRC = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v5.docx")
OUT = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v6.docx")


doc = Document(str(SRC))
for para in doc.paragraphs:
    text = para.text
    if text.startswith("("):
        para.text = re.sub(r"^\((\d+)\)", r"（\1）", text, count=1)

doc.save(str(OUT))
print(OUT)
