<!--
  ServiceHub — Team 3 | Pull Request Template
  Keep your PR focused: one feature, one bug fix, or one logical unit of work.
  Aim for under 200 lines of changes. Smaller PRs get faster, better reviews.
-->

## What does this PR do?

<!-- What changed and WHY. Two to three sentences max. Be specific — "added X so that Y" not "made some changes". -->

## Related issue

<!-- Every PR must close an issue. Replace the number below. -->
Closes #

## Type of change

- [ ] Feature — new functionality
- [ ] Bug fix — corrects broken behaviour
- [ ] Refactor — restructures existing code without changing behaviour
- [ ] Test — adds or updates tests only
- [ ] DevOps — Docker, CI/CD, infrastructure
- [ ] Documentation — README, comments, docs only

## Bug fix details

<!--
  Bug fixes only — delete this entire section if this is not a bug fix.
-->

**Root cause:**
<!-- What was the actual cause? Be specific. -->

**How to reproduce (before this fix):**
1.
2.
3.

Expected:
Actual (broken behaviour):

**Regression risk:**
- [ ] Low — isolated change, no shared logic touched
- [ ] Medium — touches shared service or utility
- [ ] High — core workflow affected (describe below)

## API changes

<!-- Delete this section if no API endpoints were added or modified. -->

| Method | Endpoint | Change | Auth required |
|--------|----------|--------|---------------|
| GET/POST/PUT/DELETE | `/api/...` | Added / Modified / Removed | EMPLOYEE / AGENT / MANAGER / Public |

- [ ] This is a breaking change — existing callers will need to update

## Database changes

<!-- Delete this section if no DB changes were made. -->

- [ ] New table or column added
- [ ] Existing column modified or removed
- [ ] New Flyway migration added
- [ ] Data migration required

## How to test

<!--
  Give the reviewer exact steps to verify this works.
  Start from docker-compose up --build if backend changed.
  Include the endpoint, payload, and expected response for API changes.
-->

1.
2.
3.

Expected result:

## Screenshots

<!-- Required for any change that affects a UI view. Delete if not applicable. -->

## Author checklist

<!--
  Complete this before tagging your reviewer.
  Do not open the PR if any box cannot be checked.
-->

- [ ] Branch created from `main` using `feature/` prefix
- [ ] This PR covers one task only — not multiple issues bundled together
- [ ] `docker-compose up --build` runs cleanly with my changes
- [ ] I tested the happy path and at least one edge case
- [ ] No secrets, passwords, API keys, or `.env` files are committed
- [ ] Reviewer assigned (CODEOWNERS auto-assigns — confirm they are tagged)
- [ ] Fewer than 200 lines of changes — if not, I have split the PR

---

<!--
  ─────────────────────────────────────────────────────
  FOR THE REVIEWER — complete within 4 hours of being tagged
  You must leave at least 2 substantive comments — not just "LGTM".
  Use "Nit:" for suggestions that are not blocking.
  ─────────────────────────────────────────────────────
-->

## Reviewer checklist

- [ ] Read the PR description and understand the goal
- [ ] **Correctness** — does the code do what the description says?
- [ ] **Security** — any hardcoded credentials, unvalidated inputs, or injection risks?
- [ ] **API contract** — if endpoints changed, does the response match what frontend/tests expect?
- [ ] **Readability** — names are clear, logic is followable without asking the author
- [ ] **Tests** — new behaviour is tested; existing tests still pass
- [ ] For bug fixes: does the fix address the root cause, not just the symptom?
- [ ] Left at least 2 meaningful comments
- [ ] Submitted review as: **Approve** / **Request Changes** / **Comment**
