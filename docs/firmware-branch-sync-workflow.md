# Firmware Branch Sync Workflow

Date: 2026-06-04
Primary working branch: `ananthu-dev`
Integration branch: `develop`

## Goal

Use small firmware-only commits and keep `ananthu-dev` and `develop` synchronized after each safe step.

## Rules

- Work on one firmware step at a time.
- Prefer one logical change per commit.
- Build or verify only the scope touched by that step.
- Do not mix unrelated cleanup into a functional firmware commit.
- Do not overwrite unrelated local changes without checking the worktree first.

## Standard Step Loop

Run this loop for each firmware step:

```powershell
git checkout ananthu-dev
git pull origin ananthu-dev
git status --short --branch
```

Make one small change, then:

```powershell
git add -A
git commit -m "<small firmware step message>"
git push origin ananthu-dev
```

Sync through `develop`:

```powershell
git checkout develop
git pull origin develop
git merge ananthu-dev --no-edit
git push origin develop
```

Bring the latest `develop` state back into the working branch:

```powershell
git checkout ananthu-dev
git merge develop --no-edit
git push origin ananthu-dev
```

## If `develop` Changed During Your Step

If `origin/develop` advanced before your merge:

```powershell
git checkout develop
git pull origin develop
git merge ananthu-dev --no-edit
```

Resolve only the conflicts required for that single step, then continue with:

```powershell
git push origin develop
git checkout ananthu-dev
git merge develop --no-edit
git push origin ananthu-dev
```

## Verification Checklist Per Step

- Branch is correct before editing.
- `git status` is reviewed before commit.
- Commit message describes one exact firmware change.
- `ananthu-dev` is pushed before touching `develop`.
- `develop` is updated only after the step is locally verified.
- `ananthu-dev` is re-synced from `develop` after integration.

## Recommended Commit Style

- `docs(firmware): ...`
- `refactor(firmware/common): ...`
- `feat(firmware/pen-unit): ...`
- `feat(firmware/inner-unit): ...`
- `feat(firmware/outer-unit): ...`
- `fix(firmware/outer-unit): ...`
- `test(firmware): ...`

## First Planned Firmware Sequence

1. Centralize shared packet definitions in `firmware/common`.
2. Add shared event identity fields.
3. Add shared ACK protocol.
4. Persist pen events before BLE transfer.
5. Add outer-unit pen fetch and ACK handling.
6. Introduce outer-unit source coordination and priority scheduling.
