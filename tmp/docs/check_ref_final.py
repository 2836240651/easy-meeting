from docx import Document
from pathlib import Path

DOCX = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节_第5章表题字号字体修正_最终.docx")

doc = Document(DOCX)
for p in doc.paragraphs:
    t=(p.text or '').strip()
    if '如表' in t:
        print(t)
