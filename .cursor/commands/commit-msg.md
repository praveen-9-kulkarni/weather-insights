# Suggest commit message

Inspect the current git working tree and suggest a commit message. Do not commit or push.

## Steps

1. Run `git status`, `git diff` (staged and unstaged), and `git log -5 --oneline` to match this repo’s commit style.
2. Include untracked files that look intentional (e.g. new source under `src/` or `.cursor/`), and note any files that look like secrets and should not be committed.
3. Draft a concise commit message (1–2 sentences) focused on **why**, not a file list.
4. Offer one shorter alternative subject line.

## Output format

```
Suggested:
<full message>

Shorter:
<one-line alternative>
```

## Constraints

- Do not run `git add`, `git commit`, or `git push`.
- Do not invent changes that are not in the diff.
