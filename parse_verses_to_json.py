import re
import json

# 입력 파일과 출력 파일 경로
INPUT_FILE = 'verses_6_12.txt'
OUTPUT_FILE = 'app/src/main/assets/daily_verses_2025.json'

# 날짜 패턴 (예: 6월 3일 화요일)
date_pattern = re.compile(r'([1-9]|1[0-2])월 ([1-9]|[12][0-9]|3[01])일 [일월화수목금토]요일')

# 파일 읽기
with open(INPUT_FILE, encoding='utf-8') as f:
    text = f.read()

# 날짜별로 split
splits = date_pattern.split(text)

# split 결과는 [이전내용, 월, 일, 본문, 월, 일, 본문, ...] 구조
verses = []
for i in range(1, len(splits)-2, 3):
    month = int(splits[i])
    day = int(splits[i+1])
    body = splits[i+2].strip()
    lines = body.splitlines()
    # 제목(성구)
    title_lines = []
    reference = ''
    body_start_idx = 0
    for idx, line in enumerate(lines):
        # 제목 줄에 성구(—)가 붙어 있는 경우 자동 분리
        if '—' in line:
            parts = line.split('—', 1)
            title_candidate = parts[0].strip()
            reference_candidate = '—' + parts[1].strip()
            if title_candidate:
                title_lines.append(title_candidate)
            reference = reference_candidate
            body_start_idx = idx + 1
            break
        if line.strip().startswith('—'):
            reference = line.strip()
            body_start_idx = idx + 1
            break
        if line.strip():
            title_lines.append(line.strip())
    title = ' '.join(title_lines)
    # 참고문헌(마지막 줄에 '파', '면', '항' 포함)
    last_ref = ''
    body_end_idx = len(lines)
    for idx in range(len(lines)-1, -1, -1):
        if any(x in lines[idx] for x in ['파', '면', '항']):
            last_ref = lines[idx].strip()
            body_end_idx = idx
            break
    # 본문: 줄바꿈 없이 한 줄로
    body_text = ' '.join([l.strip() for l in lines[body_start_idx:body_end_idx] if l.strip()])
    # 성구와 본문 사이에 한 줄만 추가
    if reference and body_text:
        body_text = '\n' + body_text
    # 참고문헌 줄도 본문에 추가
    if last_ref:
        body_text += '\n' + last_ref
    # body_text에서 줄바꿈(\n) 문자를 모두 공백으로 대체
    body_text = body_text.replace('\n', ' ')
    # 닫는 괄호 뒤에 줄바꿈 추가 (이미 줄바꿈이 없을 때만)
    body_text = re.sub(r'\)(?!\s*\n)', ')\n', body_text)
    # 날짜 포맷 MM-DD
    date_str = f"{month:02d}-{day:02d}"
    verses.append({
        "date": date_str,
        "title": title,
        "reference": reference,
        "body": body_text
    })

# JSON 저장
with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
    json.dump(verses, f, ensure_ascii=False, indent=2)

print(f"Saved {len(verses)} verses to {OUTPUT_FILE}") 