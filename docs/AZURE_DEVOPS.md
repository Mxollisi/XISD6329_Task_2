# Azure DevOps board — template and import instructions
**Status:** this is a *template*. No work items in a real Azure DevOps organisation have been created or updated by this project.
1. Create a project (Agile or Scrum process). 2. Boards ▸ Work items ▸ **Import Work Items** ▸ choose `docs/azure-devops-backlog.csv`
   (hierarchical: `Title 1` Epic, `Title 2` User Story, `Title 3` Task; columns `Description`, `Acceptance Criteria`, `Tags`).
   If your process template lacks *Acceptance Criteria*, remove that column or rename it to match. 3. Review, **Save items**.
4. Add custom/text fields or use the Discussion tab for **Test evidence** (commit/PR link, test run, device model, date, screenshot path).
5. Each sprint: move items New → Active → Resolved/Closed, link commits/PRs (`AB#<id>` in commit messages) and GitHub Actions runs.
6. Evidence for the lecturer: board screenshot per sprint, burndown/CFD, work-item↔commit links, peer-evaluation notes on regular meetings.
