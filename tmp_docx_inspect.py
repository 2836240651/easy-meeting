from docx import Document

path = r"C:\Users\13377\Desktop\论文相关\第2章_相关技术介绍.docx"
doc = Document(path)

for i, p in enumerate(doc.paragraphs, 1):
    text = p.text.strip().replace("\t", "    ")
    if text:
        print(f"{i:03d}\tstyle={p.style.name}\t{text[:120]}")
