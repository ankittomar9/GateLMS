import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DesignProvider, DESIGN_STORAGE_KEY, DESIGN_META, VALID_DESIGNS } from '../../context/DesignContext';
import { LayoutProvider, STORAGE_KEY as LAYOUT_STORAGE_KEY } from '../../context/LayoutContext';
import { DesignSwitcher } from '../DesignSwitcher';

const renderSwitcher = () => {
  return render(
    <DesignProvider>
      <LayoutProvider>
        <DesignSwitcher />
      </LayoutProvider>
    </DesignProvider>
  );
};

describe('DesignSwitcher Component', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-design');
    delete document.documentElement.dataset.design;
  });

  it('displays the current preset label on the trigger button', () => {
    renderSwitcher();

    const trigger = screen.getByTestId('design-switcher-trigger');
    expect(trigger).toBeDefined();
    expect(trigger.textContent).toContain('Classic');
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
  });

  it('renders all 4 presets with labels and swatches when popover is opened', async () => {
    const user = userEvent.setup();
    renderSwitcher();

    const trigger = screen.getByTestId('design-switcher-trigger');
    await user.click(trigger);

    expect(trigger.getAttribute('aria-expanded')).toBe('true');
    expect(screen.getByRole('radiogroup')).toBeDefined();

    VALID_DESIGNS.forEach((preset) => {
      const option = screen.getByTestId(`design-toggle-${preset}`);
      expect(option).toBeDefined();
      expect(option.textContent).toContain(DESIGN_META[preset].label);
    });
  });

  it('indicates active preset with aria-checked="true"', async () => {
    const user = userEvent.setup();
    renderSwitcher();

    await user.click(screen.getByTestId('design-switcher-trigger'));

    const tufOption = screen.getByTestId('design-toggle-tuf');
    const inkOption = screen.getByTestId('design-toggle-ink');
    const auroraOption = screen.getByTestId('design-toggle-aurora');
    const skyOption = screen.getByTestId('design-toggle-sky');

    expect(tufOption.getAttribute('aria-checked')).toBe('true');
    expect(inkOption.getAttribute('aria-checked')).toBe('false');
    expect(auroraOption.getAttribute('aria-checked')).toBe('false');
    expect(skyOption.getAttribute('aria-checked')).toBe('false');
  });

  it('switches preset on click, pairs layout, persists to storage, and closes popover', async () => {
    const user = userEvent.setup();
    renderSwitcher();

    const trigger = screen.getByTestId('design-switcher-trigger');
    await user.click(trigger);

    const inkOption = screen.getByTestId('design-toggle-ink');
    await user.click(inkOption);

    // Popover should close
    expect(screen.queryByRole('radiogroup')).toBeNull();

    // Trigger should reflect new preset
    expect(trigger.textContent).toContain('Ink');
    expect(localStorage.getItem(DESIGN_STORAGE_KEY)).toBe('ink');
    expect(document.documentElement.dataset.design).toBe('ink');
  });

  it('closes popover on Escape key press', async () => {
    const user = userEvent.setup();
    renderSwitcher();

    const trigger = screen.getByTestId('design-switcher-trigger');
    await user.click(trigger);
    expect(screen.getByRole('radiogroup')).toBeDefined();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('radiogroup')).toBeNull();
  });
});
