<!--
  ServiceHub — Team 3 | Bug Fix Template
  Use this template for bug fixes only.
  For features, use the default template instead.
-->

## Bug summary

<!-- One sentence: what was broken and where. -->

## Root cause

<!-- What was the actual cause? Be specific — "the status transition check was missing a null guard" not "there was a bug". -->

## How to reproduce (before this fix)

1.
2.
3.

**Expected:** 
**Actual (broken behaviour):** 

## Fix description

<!-- What exactly did you change to fix it? One to two sentences. -->

## Related issue

Closes #

## API changes

<!-- Delete if no endpoints were affected. -->

| Method | Endpoint | What changed |
|--------|----------|--------------|
| | | |

## Regression risk

<!-- Does this fix risk breaking anything else? Which areas should the reviewer pay extra attention to? -->

- [ ] Low — isolated change, no shared logic touched
- [ ] Medium — touches shared service or utility
- [ ] High — core workflow affected (describe below)

## How to verify the fix

1.
2.
3.

Expected result:

## Author checklist

- [ ] Root cause is documented above — not just the symptom
- [ ] Fix is minimal — no unrelated refactoring included
- [ ] `docker-compose up --build` runs cleanly
- [ ] Test added to prevent this bug from regressing
- [ ] No secrets or `.env` files committed
- [ ] Reviewer assigned

---

## Reviewer checklist

**Focus areas for bug fixes**
- [ ] Does the fix actually address the root cause, not just the symptom?
- [ ] Could this same bug exist elsewhere in similar code?
- [ ] Is there a test that would have caught this? Is one added now?
- [ ] Does the fix introduce any new edge cases?
- [ ] Is the regression risk assessment accurate?

**Standard checks**
- [ ] Security — no new vulnerabilities introduced
- [ ] Correctness — happy path and edge cases work
- [ ] Left at least 2 meaningful comments
- [ ] Submitted review as: **Approve** / **Request Changes** / **Comment**
- [ ] Tagged author on Slack if requesting changes
