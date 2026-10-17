import { Page } from '@playwright/test';
export async function select(page: Page, label: string, option: string) {
  await page.getByRole('combobox', { name: label, exact: true }).click();
  await page.getByRole('option', { name: option, exact: true }).click();
  await page.keyboard.press('Escape');
}
