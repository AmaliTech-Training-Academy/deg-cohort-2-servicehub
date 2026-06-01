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

## How to test

<!--
  Give the reviewer exact steps to verify this works.
  Start from docker-compose up --build if backend changed.
  Include the endpoint, payload, and expected response for API changes.
  Include the URL and what to click for UI changes.
-->

1.
2.
3.

Expected result:

## Author checklist

<!--
  Complete this before tagging your reviewer.
  Do not open the PR if any box cannot be checked.
-->

- [ ] This branch was created from `main` using the `feature/` prefix
- [ ] This PR covers one task only — not multiple issues bundled together
- [ ] `docker-compose up --build` runs cleanly with my changes
- [ ] I have tested the happy path and at least one edge case
- [ ] No secrets, passwords, API keys, or `.env` files are committed
- [ ] I have assigned my designated code reviewer
- [ ] The PR has fewer than 200 lines of changes — if not, I have split it

## Screenshots

<!-- Required for any change that affects an HTML template or UI. Delete this section if not applicable. -->

---

<!--
  ─────────────────────────────────────────────────────
  FOR THE REVIEWER — complete within 4 hours of being tagged
  Target: 15–20 minutes. If it takes longer the PR is too large.
  You must leave at least 2 substantive comments — not just "LGTM".
  Use "Nit:" or "Optional:" to prefix suggestions that are not blocking.
  Save "Request changes" for real problems: bugs, broken logic, security issues.
  ─────────────────────────────────────────────────────
-->

## Reviewer checklist

**Before you start**
- [ ] Read the PR description and understand the goal
- [ ] Check the Files Changed tab to get a sense of scope

**During the review — in priority order**
- [ ] **Correctness** — does the code do what the description says? Are there logic errors or unhandled edge cases?
- [ ] **Security** — any hardcoded credentials, SQL injection risks, or unvalidated inputs?
- [ ] **Readability** — can you understand the code without asking the author? Are names clear?
- [ ] **Structure** — is logic organised sensibly? Is there unnecessary duplication? Are responsibilities separated?
- [ ] **Tests** — are there tests for the new behaviour? Do existing tests still pass?
- [ ] **Style** — does it follow the team's conventions? (Lowest priority — do not nitpick formatting)

**After the review**
- [ ] Left at least 2 meaningful comments (findings, suggestions, or genuine praise)
- [ ] Submitted review as: **Approve** / **Request Changes** / **Comment**
- [ ] Tagged the author on Slack if you requested changes
