# Team Branching and PR Guide

## Branching model

- `main`: stable release-ready branch.
- `develop`: integration branch for daily team work.
- `feature/<module>-<short-task>`: new feature work from `develop`.
- `fix/<module>-<short-task>`: bug fix work from `develop`.
- `hotfix/<short-task>`: urgent production fix from `main`, then merge back to `main` and `develop`.

## Branch naming examples

- `feature/auth-login-form`
- `feature/recruitment-candidate-crud`
- `fix/security-password-validation`
- `fix/ui-thymeleaf-template-error`

## Daily flow

1. Update local branches:
   - `git checkout main && git pull`
   - `git checkout develop && git pull`
2. Create working branch from `develop`:
   - `git checkout develop`
   - `git checkout -b feature/<name>`
3. Commit frequently with clear messages.
4. Push branch and create PR into `develop`.
5. After testing/review, merge `develop` into `main` on release.

## Commit message format

Use this format:

- `feat: add candidate create form`
- `fix: resolve postgres connection config`
- `refactor: split service input models`
- `docs: add team branching guide`

## Pull request checklist

- [ ] Branch name follows `feature/*` or `fix/*`.
- [ ] PR target branch is `develop` (except hotfix/release cases).
- [ ] Code builds successfully (`./mvnw -DskipTests package` or `.\mvnw.cmd -DskipTests package`).
- [ ] No hardcoded secrets in code or config.
- [ ] Database/config changes are documented.
- [ ] Scope is focused (no unrelated file changes).
- [ ] Self-review completed.
- [ ] Clear PR title and description (what, why, impact).
- [ ] Screenshots attached for UI changes.
- [ ] At least one reviewer assigned.

## Merge rules

- Squash merge for feature/fix PRs.
- Keep `main` protected and merge by PR only.
- Delete merged feature/fix branches to keep repository clean.
