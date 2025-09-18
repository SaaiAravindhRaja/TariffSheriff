# Release and Versioning Guide

This project uses semantic versioning (SemVer). Below are recommended steps and policies to make releases repeatable and safe for a monorepo that contains both Node (frontend, packages) and Maven (backend) modules.

## Versioning policy

- Use `MAJOR.MINOR.PATCH` SemVer.
- Increment:
  - `PATCH` for bug fixes and non-breaking changes.
  - `MINOR` for new features that are backwards compatible.
  - `MAJOR` for breaking changes.
- For monorepos, coordinate the top-level repo version and module versions (if you publish packages separately) using release notes and changelogs.

## Tagging releases

1. Ensure the code on the target branch (e.g. `main`) is tested and CI green.
2. Update any package manifest `version` fields as needed (see section below for npm and Maven suggestions), or use an automated release tool (semantic-release, lerna, changesets).
3. Create a signed tag locally and push it:

```bash
# create an annotated tag
git tag -a vX.Y.Z -m "Release vX.Y.Z"
# push the tag
git push origin vX.Y.Z
```

## Suggested release flow (manual)

- Branch: create a `release/x.y` branch for coordinated release work if multiple changes across modules are required.
- Merge PRs into `main` and keep a CHANGELOG entry in PR description or use `CHANGELOG.md`.
- When ready, create the tag on the commit that should represent the release.

## NPM package versioning (apps/frontend and packages)

- If you publish packages from this repo, update each package's `package.json` `version` field (or use a tool like `changesets` or `lerna` to manage versions across packages).
- Example publish sequence for a single package:

```bash
# from the package directory
npm version patch   # or minor / major
npm publish --access public
```

## Maven backend versioning

- Update `pom.xml` `version` tags for the backend module(s) before releasing.
- Use your internal release CI to publish artifacts to your Maven repository (Nexus, Artifactory, or GitHub Packages).

## Automation options (recommended)

- `changesets` — great for monorepos with multiple npm packages.
- `semantic-release` — automates versioning and release publishing based on commit messages.
- GitHub Actions — create workflows to tag and publish automatically when `main` is updated with a release PR.

## Rollbacks and hotfixes

- For a critical fix, create a `hotfix/x.y.z` branch from the latest tag, apply the fix, bump patch version, and publish a patch release.

## Example commands to create a release for the repo

```bash
# from repository root
# ensure main is up-to-date
git checkout main
git pull origin main
# create annotated tag
git tag -a v0.3.0 -m "Release v0.3.0"
# push tag
git push origin v0.3.0
```
