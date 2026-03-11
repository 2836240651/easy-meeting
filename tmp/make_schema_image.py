from PIL import Image, ImageDraw, ImageFont


W, H = 1275, 266
img = Image.new("RGB", (W, H), "white")
draw = ImageDraw.Draw(img)

font_path = r"C:\Windows\Fonts\simhei.ttf"
font_head = ImageFont.truetype(font_path, 18)
font_cell = ImageFont.truetype(font_path, 14)
font_num = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 14)

yellow = "#fff200"
grid = "#d0d0d0"
green = "#1d9f6e"
text = "#222222"

cols = [0, 370, 446, 689, 1071, 1206, 1275]
rows = [0, 46, 77, 108, 139, 170, 201, 232, 266]

# Header
draw.rectangle((0, 0, W, 46), fill=yellow)
for x in cols:
    draw.line((x, 0, x, H), fill=grid, width=1)
for y in rows:
    draw.line((0, y, W, y), fill=grid, width=1)

headers = [
    ("模型名", 0, 8),
    ("字段序号", 370, 3),
    ("字段英文名", 446, 8),
    ("字段中文名", 689, 8),
    ("数据类型", 1071, 8),
    ("长度", 1206, 8),
]

for text_value, x, y in headers:
    draw.text((x + 4, y), text_value, font=font_head, fill=text)

for fx in [400, 424, 665, 1047, 1186, 1254]:
    draw.rectangle((fx, 24, fx + 18, 18 + 24), fill="white", outline=green, width=1)
    draw.polygon([(fx + 4, 30), (fx + 14, 30), (fx + 9, 38)], fill=green)

data = [
    ("dm_huadu_forest_fire_user_stay_d", "1", "STAT_DT", "统计日期", "INT", ""),
    ("dm_huadu_forest_fire_user_stay_d", "2", "USR_NBR", "用户号码", "STRING", ""),
    ("dm_huadu_forest_fire_user_stay_d", "3", "USR_NBR_MD5", "用户号码_MD5", "STRING", ""),
    ("dm_huadu_forest_fire_user_stay_d", "4", "CGI", "位置CGI", "STRING", ""),
    ("dm_huadu_forest_fire_user_stay_d", "5", "STATION_ID", "基站ID", "STRING", ""),
    ("dm_huadu_forest_fire_user_stay_d", "6", "STAY_RECORD_CNT", "驻留记录次数", "BIGINT", ""),
    ("dm_huadu_forest_fire_user_stay_d", "7", "TOTAL_DURATION", "累计驻留时长", "BIGINT", ""),
]

for idx, row in enumerate(data):
    y = 52 + idx * 31
    draw.text((2, y), row[0], font=font_num, fill=text)
    draw.text((387, y), row[1], font=font_num, fill=text)
    draw.text((450, y), row[2], font=font_num, fill=text)
    draw.text((692, y), row[3], font=font_cell, fill=text)
    draw.text((1075, y), row[4], font=font_num, fill=text)

out = r"D:\JavaPartical\easymeeting-java\tmp\dm_huadu_forest_fire_user_stay_d_schema.png"
img.save(out)
print(out)
