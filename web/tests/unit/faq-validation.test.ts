import { describe, expect, it } from 'vitest';
import { z } from 'zod';
import { FAQ_ITEMS } from '../../src/data/faq';

const faqItemSchema = z.object({
  id: z
    .string()
    .min(1)
    .regex(/^[a-z0-9-]+$/),
  category: z.enum([
    'General',
    'Installation & Updates',
    'Privacy & Security',
    'AI & Features',
    'Troubleshooting',
  ]),
  question: z.string().min(5),
  answer: z.string().min(10),
});

describe('FAQ data validation', () => {
  it('validates all FAQ items against schema', () => {
    expect(FAQ_ITEMS.length).toBeGreaterThan(0);
    for (const item of FAQ_ITEMS) {
      expect(() => faqItemSchema.parse(item)).not.toThrow();
    }
  });

  it('has unique FAQ identifiers', () => {
    const ids = FAQ_ITEMS.map((item) => item.id);
    const uniqueIds = new Set(ids);
    expect(uniqueIds.size).toBe(ids.length);
  });
});
