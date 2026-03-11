from pathlib import Path
p=Path(r"D:\JavaPartical\easymeeting-java\src\main\java\com\easymeeting\controller\MeetingInfoController.java")
s=p.read_text(encoding='utf-8')
patterns=[
    '@RequestMapping("/create")',
    '@RequestMapping("/join")',
    '@RequestMapping("/joinMeeting")',
    'public ResponseVO joinMeeting',
    'public ResponseVO create',
    'preJoinMeeting',
]
for pat in patterns:
    print(pat, s.find(pat))