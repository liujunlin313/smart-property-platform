#!/usr/bin/env python3
"""Small localhost-only HTTP workload runner. Credentials stay in ignored .env.docker."""
from __future__ import annotations
import argparse, concurrent.futures, json, random, statistics, threading, time, urllib.error, urllib.request
from datetime import datetime, timezone
from collections import Counter
from pathlib import Path

def load_env(path: Path) -> dict[str, str]:
    values = {}
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if line and not line.lstrip().startswith("#") and "=" in line:
            key, value = line.split("=", 1); values[key.strip()] = value.strip()
    return values

def request(url, method="GET", token=None, load_id=""):
    headers = {"Accept":"application/json", "X-Load-Test-ID":load_id}
    if token: headers["Authorization"] = f"Bearer {token}"
    started = time.perf_counter(); status = 0
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers, method=method), timeout=8) as response:
            response.read(512); status = response.status
    except urllib.error.HTTPError as exc: status = exc.code; exc.read(512)
    except Exception: status = 0
    return status, round((time.perf_counter()-started)*1000, 2)

def login(base, username, password, load_id):
    headers={"Content-Type":"application/json", "Accept":"application/json", "X-Load-Test-ID":load_id}
    started=time.perf_counter(); status=0
    try:
        body=json.dumps({"username":username,"password":password}).encode("utf-8")
        with urllib.request.urlopen(urllib.request.Request(base+"/api/v1/auth/login", data=body, headers=headers, method="POST"), timeout=8) as response:
            status=response.status; value=json.loads(response.read().decode("utf-8"))
            return (value.get("data") or {}).get("accessToken"), status, round((time.perf_counter()-started)*1000,2)
    except urllib.error.HTTPError as exc: return None, exc.code, round((time.perf_counter()-started)*1000,2)
    except Exception: return None, 0, round((time.perf_counter()-started)*1000,2)

def main():
    parser=argparse.ArgumentParser(); parser.add_argument("--env-file",required=True); parser.add_argument("--report",required=True)
    parser.add_argument("--scenario",choices=("system","database"),required=True); parser.add_argument("--users",type=int,required=True)
    parser.add_argument("--duration",type=float,default=4.0); parser.add_argument("--load-id",required=True); parser.add_argument("--base-url",default="http://127.0.0.1:18080")
    args=parser.parse_args(); values=load_env(Path(args.env_file)); username=values.get("LOAD_TEST_USERNAME",""); password=values.get("LOAD_TEST_PASSWORD","")
    if not username or not password: raise SystemExit("LOAD_TEST_USERNAME and LOAD_TEST_PASSWORD must be set in the ignored .env.docker file.")
    lock=threading.Lock(); statuses=Counter(); latency=[]; actions=Counter(); gate=threading.Barrier(args.users); deadline=time.monotonic()+max(3.0,min(5.0,args.duration))
    system_actions=[("dashboard","GET","/api/v1/dashboard/summary"),("communities","GET","/api/v1/communities"),("buildings","GET","/api/v1/buildings"),("properties","GET","/api/v1/properties?page=1&pageSize=20"),("identity","GET","/api/v1/auth/me")]
    database_actions=[("dashboard","GET","/api/v1/dashboard/summary"),("properties_search","GET","/api/v1/properties?page=1&pageSize=20&keyword=1"),("communities","GET","/api/v1/communities"),("session_logout","POST","/api/v1/auth/logout")]
    def record(action,status,elapsed):
        with lock: statuses[str(status)]+=1; actions[action]+=1; latency.append(elapsed)
    def worker(index):
        token,status,elapsed=login(args.base_url,username,password,args.load_id); record("login",status,elapsed)
        try: gate.wait(timeout=10)
        except threading.BrokenBarrierError: return
        if not token: return
        cycle=0
        while time.monotonic()<deadline:
            if args.scenario=="system": action,method,path=system_actions[cycle%len(system_actions)]
            else:
                roll=random.random(); action,method,path=(database_actions[cycle%3] if roll<.70 else database_actions[1] if roll<.90 else database_actions[3])
            status,elapsed=request(args.base_url+path,method,token,args.load_id); record(action,status,elapsed); cycle+=1; time.sleep(.04)
    started_at=datetime.now(timezone.utc).astimezone().isoformat(); started=time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.users) as executor: list(executor.map(worker,range(args.users)))
    elapsed=time.perf_counter()-started; ordered=sorted(latency); total=sum(statuses.values()); success=sum(n for code,n in statuses.items() if 200<=int(code)<400)
    def percentile(p): return ordered[min(len(ordered)-1,max(0,int(len(ordered)*p)-1))] if ordered else None
    report={"scenario":args.scenario,"load_test_id":args.load_id,"base_url":args.base_url,"started_at":started_at,"ended_at":datetime.now(timezone.utc).astimezone().isoformat(),"virtual_users":args.users,"burst_seconds":round(elapsed,2),"requests":total,"successes":success,"failures":total-success,"success_rate_percent":round(success/total*100,2) if total else 0,"requests_per_second":round(total/elapsed,2) if elapsed else 0,"status_counts":dict(statuses),"action_counts":dict(actions),"latency_ms":{"min":min(ordered) if ordered else None,"p50":percentile(.50),"p95":percentile(.95),"max":max(ordered) if ordered else None,"mean":round(statistics.mean(ordered),2) if ordered else None},"note":"X-Load-Test-ID is correlation-only; it does not change authentication, authorization, data scope, or rate limiting."}
    Path(args.report).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps({k:report[k] for k in ("scenario","virtual_users","burst_seconds","requests","success_rate_percent","requests_per_second","status_counts","latency_ms")},ensure_ascii=False)); return 0
if __name__=="__main__": raise SystemExit(main())
