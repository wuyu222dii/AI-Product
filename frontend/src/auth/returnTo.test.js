import { describe, expect, it } from "vitest";
import { safeReturnTo } from "./returnTo.js";

describe("safeReturnTo", () => {
  it("accepts app-local routes", () => {
    expect(safeReturnTo("/app/projects/abc/documents?view=all")).toBe("/app/projects/abc/documents?view=all");
  });

  it("rejects external and protocol-relative redirects", () => {
    expect(safeReturnTo("https://attacker.example/app")).toBe("/app/projects");
    expect(safeReturnTo("//attacker.example/app")).toBe("/app/projects");
    expect(safeReturnTo("/about")).toBe("/app/projects");
  });
});
