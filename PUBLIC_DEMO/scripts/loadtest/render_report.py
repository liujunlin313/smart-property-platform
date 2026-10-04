#!/usr/bin/env python3
"""Create a self-contained, offline HTML report from a safe test result JSON file."""
from __future__ import annotations

import argparse
import json
from pathlib import Path


def esc_json(value: object) -> str:
    return json.dumps(value, ensure_ascii=False).replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--title", required=True)
    args = parser.parse_args()
    data = json.loads(Path(args.input).read_text(encoding="utf-8-sig"))
    html = f'''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{args.title}</title><style>
*{{box-sizing:border-box}}body{{margin:0;background:#f4f7fb;color:#152238;font:14px/1.55 "Microsoft YaHei",Arial,sans-serif}}main{{max-width:1180px;margin:28px auto;padding:0 22px}}h1{{font-size:26px;margin:0 0 4px}}.sub{{color:#60708a;margin:0 0 22px}}.grid{{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px}}.card{{background:#fff;border:1px solid #e3eaf3;border-radius:12px;padding:15px;box-shadow:0 2px 10px #1d355708}}.label{{color:#64748b;font-size:12px}}.value{{font-size:22px;font-weight:700;color:#0f766e;margin-top:4px;word-break:break-word}}section{{background:#fff;border:1px solid #e3eaf3;border-radius:12px;padding:18px;margin-top:16px}}h2{{font-size:17px;margin:0 0 12px}}table{{border-collapse:collapse;width:100%}}th,td{{text-align:left;padding:9px;border-bottom:1px solid #edf1f6}}th{{color:#64748b;font-weight:600}}.ok{{color:#16803c}}.warn{{color:#bd5b00}}.bar{{height:9px;border-radius:99px;background:#dce7f4;overflow:hidden;min-width:100px}}.bar i{{display:block;height:100%;background:#14838a;border-radius:99px}}.foot{{color:#718096;font-size:12px;margin:18px 0}}@media(max-width:600px){{main{{padding:0 12px;margin:14px auto}}}}
</style></head><body><main><h1>{args.title}</h1><p class="sub">离线报告 · 真实 localhost HTTP 测试统计 · 不包含凭据或请求正文</p><div id="app"></div><p class="foot">本报告由测试脚本生成。系统持续日志仍记录在同一 runtime run 的 backend、nginx、database 与 ops 目录。</p></main>
<script>const reportData={esc_json(data)};
const n=v=>v===null||v===undefined?'—':v; const ms=v=>v===null||v===undefined?'—':Number(v).toFixed(2)+' ms';
const card=(label,value,cls='')=>`<div class="card"><div class="label">${{label}}</div><div class="value ${{cls}}">${{value}}</div></div>`;
const rows=(obj)=>Object.entries(obj||{{}}).map(([k,v])=>`<tr><td>${{k}}</td><td>${{n(v)}}</td></tr>`).join('');
const latency=reportData.latency_ms||{{}}; const status=reportData.status_counts||{{}};
let html='<div class="grid">'+card('测试时间',n(reportData.started_at))+card('并发用户',n(reportData.virtual_users))+card('持续时间',n(reportData.burst_seconds)+' 秒')+card('请求 / 操作总数',n(reportData.requests||reportData.total_operations))+card('成功率',n(reportData.success_rate_percent)+'%','ok')+card('平均延迟',ms(latency.mean))+card('P50',ms(latency.p50))+card('P95',ms(latency.p95))+card('RPS / OPS',n(reportData.requests_per_second||reportData.ops))+'</div>';
html+='<section><h2>HTTP 状态与操作分布</h2><table><thead><tr><th>项目</th><th>数量</th></tr></thead><tbody>'+rows(status)+rows(reportData.action_counts)+'</tbody></table></section>';
if(reportData.database_metrics){{html+='<section><h2>MySQL 测试窗口增量</h2><table><thead><tr><th>指标</th><th>增量 / 当前值</th></tr></thead><tbody>'+rows(reportData.database_metrics)+'</tbody></table></section>';}}
if(reportData.database_operations){{const ops=reportData.database_operations; html+='<section><h2>主要数据库业务操作</h2><table><thead><tr><th>操作</th><th>表</th><th>次数</th><th>平均耗时</th><th>返回 / 影响行</th><th>字段摘要</th></tr></thead><tbody>'+ops.map(x=>`<tr><td>${{n(x.operation)}}</td><td>${{(x.tables||[]).join(', ')||'—'}}</td><td>${{n(x.count)}}</td><td>${{ms(x.average_duration_ms)}}</td><td>${{n(x.returned_rows)}} / ${{n(x.affected_rows)}}</td><td>${{(x.changed_fields||[]).join(', ')||'—'}}</td></tr>`).join('')+'</tbody></table></section>';}}
if(reportData.integrity){{html+='<section><h2>数据完整性</h2><table><thead><tr><th>表</th><th>测试前</th><th>测试后</th><th>结果</th></tr></thead><tbody>'+reportData.integrity.map(x=>`<tr><td>${{n(x.table)}}</td><td>${{n(x.before)}}</td><td>${{n(x.after)}}</td><td class="${{x.result==='UNCHANGED'?'ok':'warn'}}">${{n(x.result)}}</td></tr>`).join('')+'</tbody></table></section>';}}
document.getElementById('app').innerHTML=html;</script></body></html>'''
    Path(args.output).write_text(html, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
