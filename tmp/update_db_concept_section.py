import os

from docx import Document
from docx.shared import Pt


SRC = r"C:\Users\13377\Desktop\论文相关\1.0.1_v1.0.3.docx"
BASE, EXT = os.path.splitext(SRC)
OUT = BASE[:-7] + "_v1.0.4" + EXT if BASE.endswith("_v1.0.3") else BASE + "_v1.0.4" + EXT

doc = Document(SRC)

start = 330
end = 343

items = [
    "用户信息表（user_info）：存储用户账号和个人基本信息。",
    "会议信息表（meeting_info）：存储会议的基本信息，包括会议号、主题、创建者等。",
    "会议成员表（meeting_member）：存储会议参与者的关联关系和状态。",
    "预约会议表（meeting_reserve）：存储预约会议的信息，包括预约时间、参与人员等。",
    "预约成员表（meeting_reserve_member）：存储预约会议的参与人员列表。",
    "会议聊天消息表（meeting_chat_message）：存储会议内的聊天记录。",
    "联系人表（user_contact）：存储用户的好友关系。",
    "联系人申请表（user_contact_apply）：存储好友申请记录。",
    "通知表（user_notification）：存储系统通知信息。",
    "用户设置表（user_settings）：存储用户的偏好设置。",
    "AI会话表（ai_conversation）：存储AI助手会话记录。",
    "AI建议表（ai_suggestion）：存储AI生成的会议建议。",
    "会议记录表（meeting_record）：存储会议录制信息。",
    "会议摘要表（meeting_summary）：存储AI生成的会议摘要。",
]

for idx, text in enumerate(items, start=1):
    p = doc.paragraphs[start + idx - 1]
    p.text = f"({idx}) {text}"
    fmt = p.paragraph_format
    fmt.first_line_indent = Pt(21)
    fmt.left_indent = Pt(0)
    fmt.right_indent = Pt(0)

doc.save(OUT)
print(OUT)
