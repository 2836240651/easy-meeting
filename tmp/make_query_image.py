from PIL import Image, ImageDraw, ImageFont


W, H = 1880, 692
img = Image.new("RGB", (W, H), "#f5f7fa")
draw = ImageDraw.Draw(img)

font_path = r"C:\Windows\Fonts\simhei.ttf"
font_ui = ImageFont.truetype(font_path, 16)
font_ui_small = ImageFont.truetype(font_path, 14)
font_query = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 17)
font_watermark = ImageFont.truetype(font_path, 28)

line = "#c9d1db"
blue = "#1f6fd1"
green = "#2ab673"
text = "#334155"

# top header
draw.rectangle((0, 0, W, 52), fill="#eef2f6", outline=line, width=1)
draw.text((8, 10), "数据库", font=font_ui, fill=text)
draw.rectangle((70, 6, 265, 38), outline="#b9c3cf", width=1, fill="white")
draw.text((84, 12), "搜索数据源", font=font_ui, fill="#8a94a3")
draw.rectangle((271, 0, W, 52), fill="#f3f5f8", outline=line, width=1)
draw.rectangle((280, 16, 160 + 280, 46), outline="#b9c3cf", width=1, fill="#f8fafc")
draw.text((292, 22), "查询条数,<1000", font=font_ui_small, fill="#7b8794")

icons = ["▶", "💾", "⎘", "⟳", "⎙", "⌕", "◐", "⛶", "⛶"]
ix = 455
for idx, item in enumerate(icons):
    color = green if idx == 0 else "#64748b"
    draw.text((ix, 16), item, font=font_ui_small, fill=color)
    ix += 28

# left tree panel
draw.rectangle((0, 52, 270, H), fill="#f3f6f9", outline=line, width=1)
for y in [76, 108, 140, 172, 204, 236, 268, 300]:
    draw.rounded_rectangle((22, y, 132, y + 20), radius=3, fill="#dde5ec")
    draw.rounded_rectangle((48, y, 154, y + 20), radius=3, fill="#d5dde6")
draw.rectangle((0, 76, 270, 32 + 76), fill="#e8f0fa")
draw.text((26, 86), "▾", font=font_ui, fill="#64748b")
draw.text((52, 86), "数据库目录", font=font_ui_small, fill="#6b7280")
draw.text((52, 182), "函数", font=font_ui, fill="#475569")
draw.text((8, 310), "▸", font=font_ui, fill="#64748b")
draw.text((20, 310), "数据库", font=font_ui_small, fill="#64748b")

# editor area
draw.rectangle((270, 52, W, 372), fill="white", outline=line, width=1)
draw.rectangle((270, 52, 308, 372), fill="#eef2f6", outline=line, width=1)
draw.text((285, 58), "1", font=font_ui, fill="#6b7280")
draw.text((317, 58), "SELECT", font=font_query, fill="#001dff")
draw.text((392, 58), "*", font=font_query, fill="#111827")
draw.text((410, 58), "FROM", font=font_query, fill="#001dff")
draw.text((455, 58), "/`SR20250429013`/", font=font_query, fill="#0a8a2a")
draw.text((595, 58), "dm_huadu_forest_fire_user_stay_d", font=font_query, fill="#111827")

# watermark
for wx in [360, 780, 1220, 1640]:
    for wy in [120, 320, 520]:
        draw.text((wx, wy), "dwhwzhongrongcha\n2026年03月10日 20\n时46分15秒", font=font_watermark, fill="#e7e9ed")

# splitter and tabs
draw.rectangle((0, 372, W, 40 + 372), fill="#eef2f6", outline=line, width=1)
draw.text((2, 382), "数据源控制", font=font_ui, fill="#334155")
draw.rectangle((105, 372, 305, 412), fill="#dbe7f5", outline="#9db4d0", width=1)
draw.text((116, 382), "BI自助分析 - bizzfxfwdb", font=font_ui, fill="#334155")
draw.text((288, 382), "×", font=font_ui, fill="#334155")

# result toolbar
draw.rectangle((0, 412, W, 84 + 412), fill="white", outline=line, width=1)
draw.text((10, 423), "|||", font=font_ui, fill="#94a3b8")
draw.text((48, 423), "▶", font=font_ui, fill="#94a3b8")
draw.text((67, 423), "Output", font=font_ui, fill="#334155")
draw.rectangle((126, 412, 196, 444), fill="#f5f9ff", outline="#9db4d0", width=1)
draw.text((138, 423), "结果1 ×", font=font_ui, fill="#334155")
draw.text((8, 462), "≪", font=font_ui, fill="#94a3b8")
draw.text((38, 462), "‹", font=font_ui, fill="#94a3b8")
draw.rectangle((88, 450, 101, 470), outline="#c7d0da", width=1, fill="#f8fafc")
draw.text((99, 458), "50条", font=font_ui, fill="#334155")
draw.text((196, 458), "›", font=font_ui, fill="#64748b")
draw.text((214, 458), "≫", font=font_ui, fill="#64748b")
draw.text((236, 458), "第 1 / 10 页，共 499 条", font=font_ui, fill="#475569")

# grid
grid_top = 498
draw.rectangle((0, grid_top, W, H), fill="white", outline=line, width=1)
col_x = [0, 38, 106, 292, 474, 660, 846, 1032, 1214, 1400, 1586, 1772, W]
for x in col_x:
    draw.line((x, grid_top, x, H), fill="#d5dbe3", width=1)
for y in [498, 530, 562, 594, 626, 658]:
    draw.line((0, y, W, y), fill="#d5dbe3", width=1)
draw.rectangle((0, 498, W, 30 + 498), fill="#eef2f6")

for i, y in enumerate([536, 568, 600, 632, 664], start=1):
    draw.text((12, y - 4), str(i), font=font_ui, fill="#475569")
    for x in [110, 296, 482, 668, 854, 1040, 1222, 1408, 1594, 1780]:
        draw.rounded_rectangle((x, y - 6, min(x + 80, W - 12), y + 10), radius=2, fill="#e5e7eb")

out = r"D:\JavaPartical\easymeeting-java\tmp\dm_huadu_forest_fire_user_stay_d_query.png"
img.save(out)
print(out)
