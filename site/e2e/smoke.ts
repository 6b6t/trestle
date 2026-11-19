import { expect, test, type Page } from "@playwright/test";

function watchErrors(page: Page) {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("requestfailed", (request) => {
    // Navigating away cancels lazy chunks that are still loading. That is not a page error.
    if (request.failure()?.errorText === "net::ERR_ABORTED") return;
    errors.push(`${request.url()} failed`);
  });
  return errors;
}

test("shared download links hydrate, select a platform, and survive browser history", async ({
  page,
}) => {
  const errors = watchErrors(page);
  await page.goto("/download/?platform=linux");
  await expect(page.getByRole("radio", { name: "Linux" })).toBeChecked();
  await expect(
    page.getByRole("heading", { name: "DEB, RPM, AppImage, or a portable archive.", exact: true }),
  ).toBeVisible();
  await page.getByRole("radio", { name: "Android" }).check();
  await expect(page).toHaveURL(/platform=android/u);
  await expect(
    page.getByRole("heading", { name: "Vanilla Minecraft on your phone.", exact: true }),
  ).toBeVisible();
  await page.goBack();
  await expect(page.getByRole("radio", { name: "Linux" })).toBeChecked();
  await page.reload();
  await expect(page.getByRole("radio", { name: "Linux" })).toBeChecked();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect(errors).toEqual([]);
});

test("the homepage selects the visitor's platform", async ({ page }, info) => {
  const errors = watchErrors(page);
  await page.goto("/");
  const platform = info.project.name === "mobile" ? "iOS" : "Windows";
  await expect(page.getByRole("radio", { name: platform })).toBeChecked();
  await page.getByRole("tab", { name: "Phone", exact: true }).click();
  await expect(page.getByRole("img", { name: /Trestle on a phone/u })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect(errors).toEqual([]);
});

test("static search opens the matching guide section without a server", async ({ page }) => {
  const errors = watchErrors(page);
  await page.goto("/");
  await page.getByRole("button", { name: "Search documentation", exact: true }).click();
  await page.getByRole("combobox", { name: "Search", exact: true }).fill("SmartScreen");
  await page.getByRole("option").filter({ hasText: "SmartScreen" }).first().click();
  await expect(page).toHaveURL(/\/getting-started\/#install-on-windows$/u);
  await expect(
    page.getByRole("heading", { name: "Install Trestle and play", exact: true }),
  ).toBeVisible();
  expect(errors).toEqual([]);
});

test("theme preferences persist and old or missing pages remain useful", async ({ page }) => {
  const errors = watchErrors(page);
  await page.goto("/");
  await expect(page.locator("html")).toHaveClass("dark");
  await page.getByRole("button", { name: "Toggle color theme", exact: true }).click();
  await expect(page.locator("html")).toHaveClass("light");
  await page.reload();
  await expect(page.locator("html")).toHaveClass("light");

  await page.goto("/docs/ios.html");
  await expect(page).toHaveURL(/\/ios\/$/u);
  await expect(
    page.getByRole("heading", { name: "iOS launcher target", exact: true }),
  ).toBeVisible();

  const response = await page.goto("/missing-guide/");
  expect(response?.status()).toBe(404);
  await expect(page.getByRole("heading", { name: "Page not found", exact: true })).toBeVisible();
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", "noindex");
  await page.getByRole("link", { name: "Go to homepage", exact: true }).click();
  await expect(
    page.getByRole("heading", {
      name: "One Minecraft launcher for every device you play on",
      exact: true,
    }),
  ).toBeVisible();
  expect(errors).toEqual([]);
});
