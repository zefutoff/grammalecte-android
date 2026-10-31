#!/usr/bin/env python3
"""Report meaningful public GitLab MR changes in one GitHub tracking issue."""

import base64
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
    # The public MR endpoint is accessible anonymously. GitLab may reject
    # the discussions and pipelines endpoints with 401 even for public MRs.
    # Use metadata returned by this single endpoint instead.
    mr, _ = request(API_URL)
    if not isinstance(mr, dict) or "state" not in mr:
        raise ValueError("Unexpected GitLab merge request response")
    pipeline = mr.get("head_pipeline") or {}
    merge_status = mr.get("detailed_merge_status")
    if merge_status in ("unchecked", "checking", "approvals_syncing"):
        merge_status = None
    return {
        "state": mr["state"],
        "draft": bool(mr.get("draft") or mr.get("work_in_progress")),
        "sha": mr.get("sha") or (mr.get("diff_refs") or {}).get("head_sha"),
        "merge_status": merge_status,
        "pipeline_id": pipeline.get("id"),
        "pipeline_status": pipeline.get("status"),
        "user_notes_count": mr.get("user_notes_count"),
        "updated_at": mr.get("updated_at"),
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
        f"- Public comment count: {state['user_notes_count'] if state['user_notes_count'] is not None else 'unknown'}\n"
        f"- Last MR update: {state['updated_at'] or 'unknown'}\n"
        "\nThis public-only monitor cannot identify comment authors or retrieve their "
        "text. Open GitLab when the comment count or update timestamp changes.\n\n"
        f"<!-- fdroid-mr-watch:{encode(state)} -->"
    )


def changes(old, new):
    events = []
    fields = (
        "state", "draft", "sha", "merge_status", "pipeline_id",
        "pipeline_status", "user_notes_count", "updated_at",
    )
    for field in fields:
        if old.get(field) != new.get(field):
            events.append(f"- {field}: `{old.get(field)}` → `{new.get(field)}`")
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
