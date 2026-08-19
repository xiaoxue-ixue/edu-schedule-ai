"""
003_apply_foreign_keys.py —— 一键执行：备份 → 清理孤立数据 → 建外键 → 验证
Usage:
    python 003_apply_foreign_keys.py          # 默认不备份（有备份情况下）
    python 003_apply_foreign_keys.py --backup # 先 mysqldump 备份
"""
import pymysql
import subprocess
import sys
import os
from datetime import datetime

DB = dict(host='localhost', port=3306, user='root', password='123456',
          database='edu_schedule_ai', charset='utf8mb4')

SQL_DIR = os.path.dirname(os.path.abspath(__file__))

def print_section(title):
    bar = '=' * 68
    print(f'\n{bar}\n  {title}\n{bar}')

def run_sql_file(conn, file_path, label):
    print_section(f'执行 {label}: {os.path.basename(file_path)}')
    with open(file_path, 'r', encoding='utf-8') as f:
        raw = f.read()
    # 去掉 -- 单行注释和空行，按 ; 切分语句（不处理存储过程，本项目没有）
    stmts = []
    for line in raw.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith('--'):
            continue
        stmts.append(stripped if stmts else stripped)
    joined = '\n'.join(stmts)
    statements = [s.strip() for s in joined.split(';') if s.strip()]
    cur = conn.cursor()
    ok = fail = 0
    for i, stmt in enumerate(statements, 1):
        try:
            cur.execute(stmt)
            ok += 1
        except Exception as e:
            fail += 1
            print(f'  ❌ [{i}/{len(statements)}] 失败: {e}')
            print(f'     SQL: {stmt[:200]}')
    conn.commit()
    print(f'  ✅ 成功 {ok} 条 / ❌ 失败 {fail} 条')
    return fail == 0

def backup():
    print_section('① mysqldump 备份整库（强烈推荐）')
    ts = datetime.now().strftime('%Y%m%d_%H%M%S')
    backup_path = os.path.join(os.path.dirname(SQL_DIR), f'backup_edu_schedule_ai_{ts}.sql')
    cmd = ['mysqldump', '-uroot', '-p123456', '--single-transaction', '--routines', '--triggers', 'edu_schedule_ai']
    try:
        result = subprocess.run(cmd, capture_output=True, timeout=300)
        if result.returncode != 0:
            raise RuntimeError(result.stderr.decode('utf-8', errors='ignore'))
        with open(backup_path, 'wb') as f:
            f.write(result.stdout)
        size_mb = os.path.getsize(backup_path) / 1024 / 1024
        print(f'  ✅ 备份成功: {backup_path}  ({size_mb:.2f} MB)')
        return True
    except FileNotFoundError:
        print('  ⚠️  mysqldump 不在 PATH 中，跳过自动备份，请手动备份后再继续')
        ans = input('     确认要跳过备份吗？(yes/no): ').strip().lower()
        return ans == 'yes'
    except Exception as e:
        print(f'  ❌ 备份失败: {e}')
        return False

def verify_foreign_keys(conn):
    print_section('④ 验证外键是否全部创建成功')
    cur = conn.cursor()
    # 注意：DELETE_RULE / UPDATE_RULE 位于 REFERENTIAL_CONSTRAINTS 而非 KEY_COLUMN_USAGE
    cur.execute("""
        SELECT KCU.TABLE_NAME, KCU.CONSTRAINT_NAME, KCU.COLUMN_NAME,
               KCU.REFERENCED_TABLE_NAME, KCU.REFERENCED_COLUMN_NAME,
               RC.DELETE_RULE, RC.UPDATE_RULE
        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU
        LEFT JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS RC
               ON RC.CONSTRAINT_SCHEMA = KCU.TABLE_SCHEMA
              AND RC.CONSTRAINT_NAME   = KCU.CONSTRAINT_NAME
              AND RC.TABLE_NAME        = KCU.TABLE_NAME
        WHERE KCU.TABLE_SCHEMA = 'edu_schedule_ai'
          AND KCU.CONSTRAINT_NAME LIKE 'fk_%'
          AND KCU.REFERENCED_TABLE_NAME IS NOT NULL
        ORDER BY KCU.TABLE_NAME, KCU.CONSTRAINT_NAME, KCU.ORDINAL_POSITION
    """)
    rows = cur.fetchall()
    # 分组统计
    by_table = {}
    for r in rows:
        by_table.setdefault(r['TABLE_NAME'], []).append(r)
    print(f'  外键总数: {len(rows)} 条（期望 40 条）')
    print(f'  涉及表:  {len(by_table)} 张')
    for tbl, fks in by_table.items():
        names = ', '.join(sorted({f['CONSTRAINT_NAME'] for f in fks}))
        print(f'    · {tbl:<28} ({len(fks):>2}条) → {names}')
    if len(rows) == 40:
        print('\n  🎉 外键验证通过 ✅（40/40 全部生效）')
    else:
        print(f'\n  ⚠️  数量不符，请人工核对（当前 {len(rows)} / 期望 40）')

    # 再做一次功能验证：尝试插入一条不存在的 teacher_id 到 schedule_detail，看是否被拦
    print_section('⑤ 功能验证（尝试写入不存在的 teacher_id → 预期被拦 1452）')
    try:
        cur.execute("INSERT INTO schedule_detail (task_id, term_id, course_id, teacher_id, class_id, room_id, day_of_week, slot_id) "
                    "VALUES (1, 1, 1, 99999, 1, 1, 1, 1)")
        conn.commit()
        print('  ❌ 居然插入成功了！外键没有生效！')
    except pymysql.IntegrityError as e:
        code = e.args[0]
        if code == 1452:
            print(f'  ✅ 正确拦截（1452 外键约束）: {e.args[1][:120]}')
        else:
            print(f'  ⚠️  其他完整性错误({code}): {e.args[1][:120]}')
    except Exception as e:
        print(f'  ⚠️  其他错误: {e}')
    finally:
        conn.rollback()  # 防止脏数据

def main():
    do_backup = '--backup' in sys.argv
    if do_backup and not backup():
        sys.exit(1)

    conn = pymysql.connect(**DB, cursorclass=pymysql.cursors.DictCursor, autocommit=False)
    try:
        # Step 1 + 2
        ok1 = run_sql_file(conn, os.path.join(SQL_DIR, '001_cleanup_orphans.sql'), 'Step 1 清理孤立数据')
        ok2 = run_sql_file(conn, os.path.join(SQL_DIR, '002_add_foreign_keys.sql'),  'Step 2 建立外键约束')
        if not (ok1 and ok2):
            print('\n❌ 关键步骤有失败，终止验证。请修复后再运行')
            sys.exit(2)
        verify_foreign_keys(conn)
    finally:
        conn.close()
    print('\n✅ 全部完成，数据库现在处于外键保护状态！')

if __name__ == '__main__':
    main()