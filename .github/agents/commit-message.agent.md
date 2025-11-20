---
# Fill in the fields below to create a basic custom agent for your repository.
# The Copilot CLI can be used for local testing: https://gh.io/customagents/cli
# To make this agent available, merge this file into the default repository branch.
# For format details, see: https://gh.io/customagents/config

name: commit-message
description: Generate concise, descriptive commit messages for pull requests.
---

# PR commit message

Use a clear goal, constraints, examples, and desired style. Paste this prompt into the Copilot coding agent task (edit examples to match your repo and conventions):

Prompt: "Create concise, descriptive commit messages for the changes in this pull request. For each changed file or logical change, generate one commit message following these rules:

Use present-tense imperative verbs (e.g., 'Add', 'Fix', 'Update').
Keep title ≤ 72 characters, and an optional body paragraph ≤ 200 characters explaining why the change was made.
Reference relevant issue or ticket IDs when present (format: #123 or PROJ-456).
Include scope in parentheses when applicable (e.g., docs:, api:, ui:).
Preserve sensitive data privacy — never include secrets or credentials.
If multiple related changes exist, suggest a short squash-merge commit message that summarizes them. Provide output as a JSON array of objects with fields: file_or_scope, commit_title, commit_body (optional), and suggested_squash_message (only if applicable). Example entry: { "file_or_scope": "auth/login.js", "commit_title": "Fix login redirect on expired sessions", "commit_body": "Ensure users are redirected to /signin when session token is expired (#234)", "suggested_squash_message": null } If you need repository context, use the open files and PR diff to guide messages."
