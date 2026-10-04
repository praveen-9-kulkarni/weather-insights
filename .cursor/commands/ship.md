# Ship changes to a PR

Turn local work (usually on `main`/`master`) into a feature/fix branch, commit, push, and open a PR — **only after the user approves** the suggested branch name, commit message, PR title, summary, and test plan.

## Phase 1 — Suggest only (no git writes)

1. Run `git status`, `git diff` (staged + unstaged), `git diff main...HEAD` / `master...HEAD` if already on a feature branch, and `git log -5 --oneline` for commit style. Prefer `main` as base; use `master` if that is the default branch.
2. Include intentional untracked files; warn on likely secrets (`.env`, credentials, private keys). Public CA certs used for truststores are OK to note but not treat as secrets.
3. From the real diff only, draft suggestions:
   - **Branch**: `feature/<short-kebab>` or `fix/<short-kebab>` (one primary proposal + optional alternate).
   - **Commit message**: full (1–2 sentences, why-focused) + one shorter subject line (same spirit as `/commit-msg`).
   - **PR title**: concise.
   - **PR Summary**: 1–3 bullets.
   - **Test plan**: checklist of concrete verification steps.

### Phase 1 output format

```
Branch:
  Suggested: <name>
  Alternate: <optional>

Commit:
  Suggested:
  <full message>

  Shorter:
  <one-line>

PR title:
  <title>

Summary:
  - <bullet>
  - <bullet>

Test plan:
  - [ ] <step>
  - [ ] <step>
```

Then stop and ask the user to confirm or edit. Do **not** run `git add`, `git switch -c`, `git commit`, `git push`, or `gh pr create` in this phase.

## Phase 2 — Wait for approval

Proceed to Phase 3 only when the user clearly approves execution (e.g. **Go**, **create the PR**, **ship it**) and has settled:

- branch name (theirs, or “use suggested”)
- commit message (theirs, or “use suggested” / “use shorter”)
- PR title, Summary, and Test plan (theirs, or “use suggested”)

If anything is still ambiguous, ask once (max a few questions), then wait again.

## Phase 3 — Execute (after explicit Go)

1. If on `main`/`master` (or user asked for a new branch): `git switch -c <approved-branch>`. If already on the approved feature/fix branch, stay there.
2. Stage the intended files (exclude secrets).
3. Commit with the **approved** message via HEREDOC (`git commit -m "$(cat <<'EOF' ... EOF)"`). Do not skip hooks.
4. Push with `-u` if the branch has no upstream: `git push -u origin HEAD`.
5. Create the PR with `gh pr create` against `main`/`master`, using the **approved** title and a body with `## Summary` and `## Test plan` from the approved text (HEREDOC).
6. Return the PR URL.

## Constraints

- Never invent changes that are not in the diff.
- Never push or open a PR unless Phase 2 approval included that intent (Phase 3 “Go” counts).
- Never commit onto `main`/`master`.
- If there is nothing to ship (clean tree and no commits ahead of base), say so and stop.
- Do not amend commits unless the user explicitly asks and amend rules allow it.
- Do not put “Built with Cursor” or similar in commit or PR text.
