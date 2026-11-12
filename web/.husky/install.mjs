import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";

const repoRoot = new URL("../../", import.meta.url);
if (
  process.env.HUSKY === "0" ||
  process.env.CI === "true" ||
  process.env.NODE_ENV === "production" ||
  !existsSync(new URL(".git", repoRoot))
) {
  process.exit(0);
}

process.chdir(fileURLToPath(repoRoot));
const { default: husky } = await import("husky");
const error = husky("web/.husky");
if (error) {
  console.error(error);
  process.exitCode = 1;
}
