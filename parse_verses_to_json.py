import re
import json
import glob
import zipfile
import os

# 압축파일명과 출력 파일 경로
ZIP_FILE = 'es25_KO.txt.zip'  # 필요시 변경
OUTPUT_FILE = 'app/src/main/assets/daily_verses.json'  # 연도와 무관하게 사용

# 압축 해제
with zipfile.ZipFile(ZIP_FILE, 'r') as zip_ref:
    zip_ref.extractall('.')

# 압축에서 추출된 txt 파일 목록
INPUT_FILES = sorted(glob.glob('es25_KO_*.txt'))

# 날짜 패턴 (예: 6월 3일 화요일)
date_pattern = re.compile(r'([1-9]|1[0-2])월 ([1-9]|[12][0-9]|3[01])일 [일월화수목금토]요일')

# 여러 파일 읽어서 하나의 문자열로 합치기
text = ''
for fname in INPUT_FILES:
    with open(fname, encoding='utf-8') as f:
        text += f.read() + '\n'

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
    body_text = ''.join([l.strip() for l in lines[body_start_idx:body_end_idx] if l.strip()])
    # 닫는 괄호 뒤에 공백이 없으면 하나 추가 (중복 방지)
    body_text = re.sub(r'\)(?! )', ') ', body_text)
    # 남아 있는 모든 줄바꿈(\n, \r) 문자를 공백으로 치환
    body_text = body_text.replace('\n', ' ').replace('\r', ' ')
    # 여러 연속 공백을 하나로 치환
    body_text = re.sub(r'\s+', ' ', body_text)
    # 참고문헌 줄도 본문에 추가
    if last_ref:
        if body_text:
            body_text += '\n' + last_ref
        else:
            body_text = last_ref
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

# 사용한 txt 파일 자동 삭제
for fname in INPUT_FILES:
    try:
        os.remove(fname)
    except Exception as e:
        print(f"Failed to remove {fname}: {e}")

print(f"Saved {len(verses)} verses to {OUTPUT_FILE}")
print(f"Used txt files deleted: {INPUT_FILES}") 