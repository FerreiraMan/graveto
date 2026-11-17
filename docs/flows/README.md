# Flows

One file per feature, describing what the code does today: data model, flows, rules, errors and where the code lives. Diagrams use Mermaid, which GitHub renders and which stays readable as plain text.

| Flow | Module | Status |
|---|---|---|
| [Transaction groups](transaction-groups.md) | moneytracker/transactions | Implemented, tests pending |

## Conventions
- English, one file per feature, named after the feature (`kebab-case.md`).
- Keep each diagram under about 15 nodes. Split a diagram before it sprawls.
- Mark anything not yet confirmed by a test as **unverified**, and remove the mark when a test proves it. "Confirmed" means a test asserts it, which you can read in the test source. You do not need to run the test. Say in the doc that the code was read and not run.
- Decisions and their rationale belong in `docs/decisions/` (ADRs), not here. A flow file says what happens, not why it was chosen.
- Update the flow file in the same change that alters the behavior.
- Add the new flow to the table above.

## How to write a flow doc

**The code is the source of truth.** Read it. Do not infer behavior from class or method names, and do not copy from another flow doc. Use [transaction-groups.md](transaction-groups.md) as the template for structure and tone, never for content.

### Procedure
1. **Scope one feature.** Name it, and list its endpoints. If the scope is unclear, ask before writing.
2. **Find the entry points.** Controllers live in `src/main/java/me/ferreira/graveto/<module>/.../web/`. Read each route: HTTP method, path (the API prefix is `/api`), request DTO, validation annotations, response DTO.
3. **Follow each route down.** Controller, then the service impl, then the repositories and the domain entities. Read every branch: each `if`, `throw`, `Optional` and early `return` is a decision in the flow.
4. **Read the schema.** Open the Flyway migrations in `src/main/resources/db/migration/<module>/` for tables, columns, constraints and indexes. Later migrations can change earlier ones, so read all that touch the tables.
5. **Collect the errors.** Exceptions live in `src/main/java/me/ferreira/graveto/common/web/exception/`. Each one sets an HTTP status and a safe message. Validation errors come from annotations on the request DTOs and are returned under `invalid_params`.
6. **Write the file** in the section order below.
7. **Run the checklist** before finishing.

### File structure
In this order:
1. **Title**, then a `Status:` line and a `Last verified:` line with the date and the commit the code was read at (`git rev-parse --short HEAD`), plus the branch if the work is not merged.
2. **Purpose**: two or three lines, plus what is out of scope.
3. **Data model**: a Mermaid `erDiagram`, then a list of constraints and indexes.
4. **Flows**: one section per endpoint, with the route, the request fields, a Mermaid `flowchart` of the decisions and the response fields. Add a `sequenceDiagram` for the main flow when call order matters.
5. **Rules and invariants**: bullets of what must always hold. Name the test that asserts a rule when one exists.
6. **Errors**: a table with the columns Status, Cause and Body.
7. **Where the code lives**: a table with the columns Concern and Class, using paths relative to the module package.
8. **Known limits and unverified points**.

### Accuracy checklist
Every item must be true before the doc is done:
- [ ] Every decision node in a flowchart matches a real `if`, `throw` or early return in the service.
- [ ] Every status code in the errors table comes from an exception class, a validation annotation, or a handler in `GlobalExceptionHandler` (for example type-mismatch and malformed-body errors).
- [ ] Every class in the code table exists at that path.
- [ ] Every table, column, constraint and index in the data model exists in a migration.
- [ ] Request and response field names match the DTOs, including validation limits.
- [ ] Each rule that a test covers names that test. Rules with no test are listed as **unverified**.
- [ ] Anything you could not confirm is marked **unverified**, with the question stated.

### Mermaid rules
- Use alphanumeric node ids (`A`, `B2`, `checkPermission`). Never use `end` as an id.
- Put any label containing parentheses, brackets, quotes or punctuation inside double quotes: `A["Caller can update?"]`.
- Use `<br/>` for line breaks inside a flowchart label.
- Use `erDiagram` for tables, `flowchart TD` for decisions and `sequenceDiagram` for call order. Do not mix purposes in one diagram.
- Preview the diagram before finishing. GitHub or an editor preview both work. If you cannot render it, say so in your summary.

### Do not
- Speculate about future features or planned work.
- Paste code. Name the class and method instead.
- Repeat full request or response schemas that the DTOs already define. List the fields that matter to the flow.
- Include secrets, tokens or real user data in examples.
- State how a query or framework behaves unless you read it or a test shows it. Mark it **unverified**.
- Explain why a design was chosen. That belongs in an ADR.

### Existing flows
Documenting a flow that already exists follows the same procedure. Start from the controller, not from memory of the feature. When two parts of the code disagree, or behavior is ambiguous, write the question under known limits and mark it **unverified**. Do not choose an answer.
