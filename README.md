# cloud-itonami-isco-6123

Open Occupation Blueprint for **ISCO-08 6123**: Apiarists and Sericulturists.

**Maturity: `:implemented`** — ApiaryAdvisor ⊣ ApiaryOperationsGovernor
as a langgraph StateGraph (`intake → advise → govern → decide →
commit/hold`, human-approval interrupt), modeled on
cloud-itonami-isco-4311's bookkeeping actor. 13 tests / 27 assertions
green. The governor never dispatches hardware — it only gates what
the hive-monitoring robot below may execute.

The harvest HARD invariant — arithmetic, not judgement: a proposed
harvest must not exceed the hive's registered sustainable-yield
ceiling (exceeding it endangers the colony). `:administer-hive-treatment`
and `:approve-defensive-colony-operation` **always** escalate to human
sign-off regardless of confidence, per this repo's Trust Controls
(business-model.md) — no hive-disease treatment or defensive-colony
operation is ever auto-committed.

This repository designs a forkable OSS business for an independent apiarist: a hive-monitoring robot performs temperature/humidity sensing and external inspection under a governor-gated actor, so the operator keeps their own hive-health and harvest records instead of renting a closed apiary-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a hive-monitoring robot performs temperature/humidity sensing and external-hive inspection under an actor that proposes
actions and an independent **Apiary Operations Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
administering a hive-disease treatment, or operating near an actively defensive colony) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
apiary management plan + hive health protocol + harvest schedule
        |
        v
Apiary Advisor -> Apiary Operations Governor -> inspect-hive/monitor-health, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `6123`). Required capabilities:

- :robotics
- :telemetry
- :dmn
- :bpmn
- :audit-ledger
- :forms

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
