import re
from pathlib import Path
from docx import Document

DOCX = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节_第5章表题字号字体修正_最终_第3章同步修正_第4章代码按项目更新_注释小四宋体.docx")

doc = Document(DOCX)

pat = re.compile(r"//[^\n]*[\u4e00-\u9fff]")

for p in doc.paragraphs:
    if not (p.style and p.style.name=='Source Code'):
        continue
    t = p.text or ''
    if pat.search(t):
        print('FOUND PARA first line:', (t.splitlines()[0] if t.splitlines() else '')[:120])
        # show first line with comment
        for line in t.splitlines():
            if pat.search(line):
                print('COMMENT LINE:', line)
                break
        # show runs containing // and after
        for r in p.runs:
            if '//' in (r.text or '') or (r.text and re.search(r"[\u4e00-\u9fff]", r.text)):
                txt = (r.text or '').replace('\n','\\n')
                name = r.font.name
                size = r.font.size.pt if r.font.size else None
                print(' RUN',repr(txt),'font',name,'size',size)
        break
else:
    print('No matching comment paragraph found')