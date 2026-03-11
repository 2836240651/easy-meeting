from PIL import Image, ImageDraw, ImageFont


def draw_table(draw, x, y, widths, heights, line_color):
    total_w = sum(widths)
    total_h = sum(heights)
    draw.rectangle((x, y, x + total_w, y + total_h), outline=line_color, width=1)

    cur_x = x
    for width in widths[:-1]:
        cur_x += width
        draw.line((cur_x, y, cur_x, y + total_h), fill=line_color, width=1)

    cur_y = y
    for height in heights[:-1]:
        cur_y += height
        draw.line((x, cur_y, x + total_w, cur_y), fill=line_color, width=1)


W, H = 1848, 1233
img = Image.new("RGB", (W, H), "#f3f4f6")
draw = ImageDraw.Draw(img)

font_path = r"C:\Windows\Fonts\simhei.ttf"
font_title = ImageFont.truetype(font_path, 20)
font_label = ImageFont.truetype(font_path, 16)
font_value = ImageFont.truetype(font_path, 17)
font_log = ImageFont.truetype(font_path, 14)

blue = "#4ea3df"
line_color = "#d3d7dc"
text_dark = "#202124"
text_gray = "#5f6368"

left = 10
top = 12

# Section 1: 基本信息
draw.rectangle((left + 14, top + 4, left + 24, top + 14), fill=blue)
draw.text((left + 36, top), "任务基本信息", font=font_title, fill=text_dark)

table_x = left
table_y = top + 34
widths = [165, 1020, 165, 488]
heights = [36, 36]
draw_table(draw, table_x, table_y, widths, heights, line_color)

rows = [
    ("任务编号：", "dm_huadu_forest_fire_user_stay_d", "任务名称：", "花都区应急局森林防火用户驻留分析日表"),
    ("周期批次：", "20251108", "执行信息：", "执行完成"),
]

for row_idx, row in enumerate(rows):
    base_y = table_y + row_idx * 36 + 8
    draw.text((table_x + 12, base_y), row[0], font=font_label, fill=text_gray)
    draw.text((table_x + widths[0] + 12, base_y), row[1], font=font_value, fill="#44546a")
    draw.text((table_x + widths[0] + widths[1] + 12, base_y), row[2], font=font_label, fill=text_gray)
    draw.text((table_x + widths[0] + widths[1] + widths[2] + 12, base_y), row[3], font=font_value, fill="#44546a")

# Section 2: 日志
section2_y = table_y + sum(heights) + 18
draw.rectangle((left + 14, section2_y + 4, left + 24, section2_y + 14), fill=blue)
draw.text((left + 36, section2_y), "任务日志文本", font=font_title, fill=text_dark)

log_start_y = section2_y + 42
log_x = left

lines = [
    "/20251112/20251108-dm_huadu_forest_fire_user_stay_d-J4HXtd.log",
    "",
    "Exec Cmd: sh -c bin/go.sh \"/bin/runDp.sh -f dm_huadu_forest_fire_user_stay_d -t 20251108 -proclogid 20251108-dm_huadu_forest_fire_user_stay_d-J4HXtd\"",
    "debug: sh bin/runDp.sh -f dm_huadu_forest_fire_user_stay_d -t 20251108 -proclogid 20251108-dm_huadu_forest_fire_user_stay_d-J4HXtd",
    "",
    "Nov 12, 2025 7:26:56 PM redis.clients.jedis.JedisSentinelPool initSentinels",
    "INFO: Trying to find master from available Sentinels...",
    "",
    "Nov 12, 2025 7:26:56 PM redis.clients.jedis.JedisSentinelPool initSentinels",
    "INFO: Redis master running at 192.252.108.9:6379, starting Sentinel listeners...",
    "",
    "Nov 12, 2025 7:26:56 PM redis.clients.jedis.JedisSentinelPool initPool",
    "INFO: Created JedisPool to master at 192.252.108.9:6379",
    "",
    "2025-11-12 19:26:56.557 [main] INFO  - dacp_dp_executor.properties加载成功",
    "2025-11-12 19:26:56.561 [main] INFO  - dacp_metaforce_file.properties加载成功",
    "2025-11-12 19:26:56.561 [main] INFO  - dacp_metaforce_sql_parser.properties加载成功",
    "2025-11-12 19:26:56.562 [main] INFO  - 加载classpath*:conf/dacp_*.properties配置文件",
    "2025-11-12 19:26:56.564 [main] INFO  - dacp_dp_executor.properties加载成功",
    "2025-11-12 19:26:56.564 [main] INFO  - 加载file:conf/dacp_*.properties配置文件",
    "2025-11-12 19:26:56.566 [main] INFO  - 加载classpath*:conf/delivery_*.properties配置文件",
    "2025-11-12 19:26:56.566 [main] INFO  - 加载file:conf/delivery_*.properties配置文件",
    "2025-11-12 19:26:56 [main] [INFO] dp_executor logger创建成功!",
    "2025-11-12 19:26:56 [main] [INFO] 外部参数: -isDebug false -ignoreRedis false -ignore true -f dm_huadu_forest_fire_user_stay_d -t 20251108",
    "2025-11-12 19:26:59 [main] [INFO] 初始化数据库连接信息",
    "2025-11-12 19:26:59 [main] [INFO] dm_huadu_forest_fire_user_stay_d logger创建成功!",
    "2025-11-12 19:26:59 [main] [INFO] ----------------dm_huadu_forest_fire_user_stay_d开始执行----------------",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 开始执行[步骤Id:2(2.变量赋值)]",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 赋值成功 V_DAY -> 20251108",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 赋值成功 V_MONTH -> 202511",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 赋值成功 V1_DAY -> 20251107",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 赋值成功 V2_DAY -> 20251106",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 赋值成功 V3_DAY -> 20251105",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 赋值成功 V6_DAY -> 20251102",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 赋值成功 V14_DAY -> 20251025",
    "2025-11-12 19:26:59 [pool-1-thread-2] [INFO] 赋值成功 V1_YEAR1DAY -> 20241107",
    "",
    "insert overwrite table dm_huadu_forest_fire_user_stay_d",
    "select",
    "    ${V_DAY} as stat_dt,",
    "    phone as usr_nbr,",
    "    md5(phone) as usr_nbr_md5,",
    "    cgi,",
    "    station_id,",
    "    count(*) as stay_record_cnt,",
    "    sum(duration) as total_duration,",
    "    avg(duration) as avg_duration,",
    "    max(duration) as max_duration,",
    "    ${V_MONTH} as month,",
    "    ${V_DAY} as day",
    "from tmp_proc_huadu_emergency_forest_fire_big_data_d_${V_DAY} a",
    "where duration > 0",
    "group by phone, cgi, station_id;",
]

line_height = 34
for idx, line in enumerate(lines):
    draw.text((log_x, log_start_y + idx * line_height), line, font=font_log, fill="#1f1f1f")

out = r"D:\JavaPartical\easymeeting-java\tmp\dm_huadu_forest_fire_user_stay_d_v2.png"
img.save(out)
print(out)
