export type FeatureIconName =
  | 'inbox'
  | 'image'
  | 'mic'
  | 'sparkles'
  | 'quote'
  | 'calendar'
  | 'lock'
  | 'shield'
  | 'offline';

export interface Feature {
  readonly icon: FeatureIconName;
  readonly title: string;
  readonly description: string;
}

export interface FeatureStory {
  readonly eyebrow: string;
  readonly title: string;
  readonly body: string;
  readonly icon: FeatureIconName;
  readonly sample: string;
}

export const FEATURES: readonly Feature[] = [
  {
    icon: 'inbox',
    title: 'Opt-in notification capture',
    description:
      'Choose from supported notification sources. Capture starts paused and can be revoked in Android Settings.',
  },
  {
    icon: 'image',
    title: 'Screenshot and file OCR',
    description:
      'Select images or PDFs and extract text on this device for review and memory capture.',
  },
  {
    icon: 'mic',
    title: 'Voice notes',
    description:
      'Record a note when you choose and transcribe it locally with the installed speech model.',
  },
  {
    icon: 'sparkles',
    title: 'Local memory assistant',
    description:
      'Search saved context in natural language and ask general questions with the on-device model.',
  },
  {
    icon: 'quote',
    title: 'Memory source cards',
    description:
      'Memory-backed answers can point to the retrieved cards; general answers may not cite saved memories.',
  },
  {
    icon: 'calendar',
    title: 'Commitment review and reminders',
    description:
      'Review proposed commitments, edit their details, and schedule local reminders after you confirm them.',
  },
  {
    icon: 'lock',
    title: 'Encrypted database and backups',
    description:
      'Room data is encrypted with SQLCipher. New backup exports use a password-derived encryption key.',
  },
  {
    icon: 'shield',
    title: 'Consent and deletion controls',
    description:
      'Select notification sources, revoke Android permissions, delete individual cards, or clear saved personal data.',
  },
  {
    icon: 'offline',
    title: 'Offline after model setup',
    description:
      'Local search, storage, OCR paths, and inference work without a cloud AI service once required models are available.',
  },
];

export const FEATURE_STORIES: readonly FeatureStory[] = [
  {
    eyebrow: '01 · Capture',
    title: 'Start with what you choose to save.',
    body: 'Enable only supported notification groups, select files explicitly, or record a voice note when you need it.',
    icon: 'inbox',
    sample:
      'A selected message or image enters Retenqo only through a permission or action you control.',
  },
  {
    eyebrow: '02 · Remember',
    title: 'Keep useful context in a private local library.',
    body: 'Review captured cards and retrieve them with local search. The Room database is encrypted on the device.',
    icon: 'lock',
    sample: 'Example: “The apartment inspection is on Friday at 2.”',
  },
  {
    eyebrow: '03 · Follow through',
    title: 'Turn evidence into a task you approve.',
    body: 'Review proposed commitments, correct the details, and choose whether to create a local reminder.',
    icon: 'calendar',
    sample: 'Example: a reminder suggestion stays pending until you accept it.',
  },
];
