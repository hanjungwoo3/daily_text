#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Google Sheets의 성서 읽기 일정을 JSON으로 변환하는 스크립트

새 스프레드시트 구조:
  날짜 (YYYY-MM-DD), 링크, 한정우, 김선진, 한효인
  - 일차는 행 순서로 자동 부여 (1행 = 1일차)
  - 빈 날 없이 연속된 날짜
"""

import csv
import io
import json
import os
import sys
import urllib.request

SHEET_ID = "147Jr6U_vzzQmbtPYnDUhZQdoHZRaMPOsK56gTelodE0"
GID = "626588040"
CSV_URL = f"https://docs.google.com/spreadsheets/d/{SHEET_ID}/export?format=csv&gid={GID}"


def fetch_csv(url):
    print(f"📥 스프레드시트 다운로드: {url}")
    with urllib.request.urlopen(url) as resp:
        return resp.read().decode("utf-8")


def parse_csv(csv_text):
    """
    CSV에서 (MM-DD, day_number, reading) 추출
    - 첫 행은 헤더이므로 스킵
    - 일차는 데이터 행 순서대로 1부터 증가
    """
    schedule = {}
    reader = csv.reader(io.StringIO(csv_text))
    rows = list(reader)

    if not rows:
        print("❌ CSV에 데이터가 없습니다.")
        return schedule

    header = rows[0]
    print(f"📋 헤더: {header}")

    day_number = 0
    for row_idx, row in enumerate(rows[1:], start=2):
        if len(row) < 2:
            continue

        date_str = row[0].strip()
        reading_str = row[1].strip()

        if not date_str or not reading_str:
            continue

        # YYYY-MM-DD → MM-DD
        try:
            parts = date_str.split("-")
            if len(parts) != 3:
                print(f"⚠️  행 {row_idx}: 날짜 형식 오류 - {date_str}")
                continue
            month = int(parts[1])
            day = int(parts[2])
            mm_dd = f"{month:02d}-{day:02d}"
        except ValueError:
            print(f"⚠️  행 {row_idx}: 날짜 파싱 실패 - {date_str}")
            continue

        if mm_dd in schedule:
            print(f"⚠️  중복 날짜: {mm_dd} (행 {row_idx}) - 덮어씁니다")

        day_number += 1
        schedule[mm_dd] = {
            "day": day_number,
            "reading": reading_str,
        }

    return schedule


def main():
    print("📖 성서 읽기 일정을 파싱합니다...")

    try:
        csv_text = fetch_csv(CSV_URL)
    except Exception as e:
        print(f"❌ CSV 다운로드 실패: {e}")
        sys.exit(1)

    schedule = parse_csv(csv_text)

    if not schedule:
        print("❌ 파싱된 일정이 없습니다.")
        sys.exit(1)

    output_dir = os.path.join("app", "src", "main", "assets")
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "bible_reading_schedule.json")

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(schedule, f, ensure_ascii=False, indent=2)

    print(f"\n✅ {output_path}에 저장되었습니다.")
    print(f"📊 총 {len(schedule)}일의 일정 저장")

    sorted_items = sorted(schedule.items(), key=lambda kv: kv[1]["day"])
    print("\n📖 처음 3개:")
    for mm_dd, data in sorted_items[:3]:
        print(f"  {mm_dd}: {data['day']}일차 - {data['reading'][:60]}")
    print("\n📖 마지막 3개:")
    for mm_dd, data in sorted_items[-3:]:
        print(f"  {mm_dd}: {data['day']}일차 - {data['reading'][:60]}")


if __name__ == "__main__":
    main()
