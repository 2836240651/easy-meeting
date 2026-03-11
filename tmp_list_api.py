import re, glob, os
root=r'D:\\JavaPartical\\easymeeting-java\\src\\main\\java'
files=glob.glob(root+'\\**\\*Controller*.java', recursive=True)
controllers={}
for f in files:
    txt=open(f,encoding='utf-8',errors='ignore').read()
    base=''
    m=re.search(r'@RequestMapping\(\"([^\"]+)\"\)', txt)
    if m:
        base=m.group(1)
    methods=[]
    for mm in re.finditer(r'@(GetMapping|PostMapping|PutMapping|DeleteMapping)\(\"([^\"]+)\"\)', txt):
        methods.append((mm.group(1).replace('Mapping','').upper(), mm.group(2)))
    if methods:
        controllers[os.path.basename(f)]= (base, methods)

for k,(base,methods) in sorted(controllers.items()):
    print(k, base)
    for m,p in methods:
        if p == '' or p == '/':
            path = base
        elif p.startswith('/'):
            path = base + p
        else:
            path = base + '/' + p
        print(' ',m, path)
