#!/usr/bin/env python3
"""Report meaningful public GitLab MR changes in one GitHub tracking issue."""

import base64
import hashlib
import json
import os
import re
import urllib.request

MR_URL = "https://gitlab.com/fdroid/fdroiddata/-/merge_requests/54224"
API_URL = "https://gitlab.com/api/v4/projects/fdroid%2Ffdroiddata/merge_requests/54224"
TITLE = "F-Droid inclusion watch: fdroiddata !54224"
MARKER = re.compile(r"<!-- fdroid-mr-watch:([A-Za-z0-9_=-]+) -->")


def request(url, token=None, method="GET", payload=None):
    headers = {"User-Agent": "grammalecte-fdroid-monitor", "Accept": "application/json"}
    if token:
        headers.update({"Authorization": f"Bearer {token}",
                        "X-GitHub-Api-Version": "2022-11-28"})
    data = None if payload is None else json.dumps(payload).encode()
    if data is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, headers=headers, data=data, method=method)
    with urllib.request.urlopen(req, timeout=25) as response:
        return json.load(response), response.headers.get("X-Next-Page")


def snapshot():
    mr, _ = request(API_URL)
    notes = []
    for page in range(1, 21):
        discussions, next_page = request(f"{API_URL}/discussions?per_page=100&page={page}")
        if not isinstance(discussions, list):
            raise ValueError("Unexpected GitLab discussions response")
        for thread in discussions:
            for note in thread.get("notes", []):
                if not note.get("system", False):
                    notes.append({
                        "id": note["id"],
                        "author": note.get("author", {}).get("username", "unknown"),
                        "updated": note.get("updated_at") or note.get("created_at"),
                        "digest": hashlib.sha256(note.get("body", "").encode()).hexdigest(),
                    })
        if not next_page:
            break
    else:
        raise RuntimeError("Too many discussion pages")
    pipelines, _ = request(f"{API_URL}/pipelines?per_page=100")
    if not isinstance(pipelines, list):
        raise ValueError("Unexpected GitLab pipelines response")
    latest = max(pipelines, key=lambda item: item.get("id", 0), default={})
    merge_status = mr.get("detailed_merge_status")
    if merge_status in ("unchecked", "checking", "approvals_syncing"):
        merge_status = None
    return {
        "state": mr["state"], "draft": bool(mr.get("draft") or mr.get("work_in_progress")),
        "sha": mr.get("sha") or (mr.get("diff_refs") or {}).get("head_sha"),
        "merge_status": merge_status, "pipeline_id": latest.get("id"),
        "pipeline_status": latest.get("status"),
        "notes": sorted(notes, key=lambda note: note["id"]),
    }


def encode(value):
    raw = json.dumps(value, sort_keys=True, separators=(",", ":")).encode()
    return base64.urlsafe_b64encode(raw).decode()


def decode(body):
    match = MARKER.search(body or "")
    return json.loads(base64.urlsafe_b64decode(match.group(1))) if match else None


def issue_body(state):
    return (
        f"Upstream public MR: {MR_URL}\n\n"
        "Automatically checked every six hours. Subscribe to this issue for updates. "
        "No GitLab credentials are used.\n\n"
        f"- State: **{state['state']}**\n"
        f"- Merge status: **{state['merge_status'] or 'unchecked'}**\n"
        f"- Source SHA: `{state['sha'] or 'unknown'}`\n"
        f"- Latest pipeline: **{state['pipeline_status'] or 'none'}** "
        f"(#{state['pipeline_id'] or 'n/a'})\n"
        f"- Public discussion notes: {len(state['notes'])}\n\n"
        f"<!-- fdroid-mr-watch:{encode(state)} -->"
    )


def changes(old, new):
    events = []
    for field in ("state", "draft", "sha", "merge_status", "pipeline_id", "pipeline_status"):
        if old.get(field) != new.get(field):
            events.append(f"- {field}: `{old.get(field)}` → `{new.get(field)}`")
    before = {note["id"]: note for note in old.get("notes", [])}
    after = {note["id"]: note for note in new.get("notes", [])}
    for ident in sorted(after.keys() - before.keys()):
        events.append(f"- New discussion note by @{after[ident]['author']} (id {ident})")
    for ident in sorted(after.keys() & before.keys()):
        if before[ident] != after[ident]:
            events.append(f"- Updated discussion note by @{after[ident]['author']} (id {ident})")
    for ident in sorted(before.keys() - after.keys()):
        events.append(f"- Removed discussion note (id {ident})")
    return events


def github(path, token, method="GET", payload=None):
    url = f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}/{path}"
    value, _ = request(url, token, method, payload)
    return value


def find_issue(token):
    for page in range(1, 21):
        issues = github(f"issues?state=all&per_page=100&page={page}", token)
        for issue in issues:
            if issue.get("title") == TITLE and "pull_request" not in issue:
                return issue
        if len(issues) < 100:
            return None
    raise RuntimeError("Too many repository issues")


def main():
    token = os.environ["GH_TOKEN"]
    state = snapshot()  # An upstream failure must not overwrite a previous known state.
    issue = find_issue(token)
    if issue is None:
        created = github("issues", token, "POST", {"title": TITLE, "body": issue_body(state)})
        print(f"Created monitoring issue #{created['number']}")
        return
    previous = decode(issue.get("body"))
    if previous is None:
        raise RuntimeError("Existing monitoring issue has no valid state marker")
    events = changes(previous, state)
    if not events:
        print("No meaningful changes")
        return
    number = issue["number"]
    github(f"issues/{number}/comments", token, "POST", {
        "body": f"Changes detected in [fdroiddata !54224]({MR_URL}):\n\n" + "\n".join(events)
    })
    github(f"issues/{number}", token, "PATCH", {"body": issue_body(state)})
    print(f"Updated monitoring issue #{number} ({len(events)} events)")


if __name__ == "__main__":
    main()
