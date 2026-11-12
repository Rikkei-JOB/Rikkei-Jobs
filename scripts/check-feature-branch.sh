#!/bin/sh

branch_pattern='^feature/([A-Z]+-[0-9]+|[A-Z]+[0-9][A-Z0-9]*)-[a-z0-9]+(-[a-z0-9]+)*$'
branch=$1

if ! printf '%s\n' "$branch" | LC_ALL=C grep -Eq "$branch_pattern"; then
  printf 'Tên branch "%s" phải theo mẫu feature/<mã-task>-<tên>, ví dụ feature/NT-01-login.\n' "$branch" >&2
  exit 1
fi
