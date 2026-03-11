from pathlib import Path

from docx import Document


src = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_tmp.docx")
out = Path(r"C:\Users\13377\Desktop\论文相关\毕业论back_备份_修正扩展名_论文组织结构已补全.docx")

doc = Document(str(src))
table = doc.tables[1]

rows = [
    ("第一章", "绪论", "介绍课题研究背景与意义、国内外研究现状，以及论文整体结构安排。"),
    ("第二章", "相关技术介绍", "介绍系统开发涉及的核心技术，包括 SpringBoot、WebRTC、WebSocket、Vue 3、Electron 以及 Redis 等。"),
    ("第三章", "系统设计", "从系统总体架构、功能模块、数据库设计和核心业务流程等方面展开系统设计。"),
    ("第四章", "系统实现", "结合前后端代码实现，说明用户管理、会议管理、实时通信和桌面端集成等核心功能的实现过程。"),
    ("第五章", "系统测试", "通过功能测试与关键业务场景测试，验证系统主要模块的正确性、稳定性和可用性。"),
    ("第六章", "总结与展望", "总结本文完成的主要工作、系统实现效果，并对后续优化方向和扩展内容进行展望。"),
]

for idx, (chapter, title, summary) in enumerate(rows, start=1):
    row = table.rows[idx]
    row.cells[0].text = chapter
    row.cells[1].text = title
    row.cells[2].text = summary

doc.save(str(out))
print(out)
