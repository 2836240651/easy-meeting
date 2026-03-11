from pathlib import Path
from docx import Document

DOCX = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全_条标题正文统一格式_v7_接口设计更新_接口设计按代码更新_5_2_1表格三线_三线表修正_第6章更新_精简技术细节_第5章表题字号字体修正_最终_第3章同步修正_第4章代码按项目更新_注释小四宋体.docx")

doc = Document(DOCX)

# locate a code paragraph containing the sample comment
sample = '// 获取会议'
found = 0
for p in doc.paragraphs:
    if p.style and p.style.name=='Source Code' and sample in (p.text or ''):
        print('PARA:', (p.text or '').splitlines()[0][:120])
        for r in p.runs[:8]:
            txt = r.text.replace('\n','\\n')
            if not txt:
                continue
            name = r.font.name
            size = r.font.size.pt if r.font.size else None
            print(' RUN',repr(txt),'font',name,'size',size)
        found += 1
        break

print('found', found)