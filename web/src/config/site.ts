export interface NavItem {
  readonly label: string;
  readonly href: string;
}
import { RELEASE_CONFIG } from './product';

export const SITE_CONFIG = {
  name: 'Retenqo',
  tagline: 'Your life. Your memories. Your intelligence.',
  description:
    'Retenqo is a private, on-device AI personal memory assistant for Android. Capture important information, track commitments, and retrieve memories locally.',
  url: 'https://retenqo.pages.dev',
  appRepoUrl: `https://github.com/${RELEASE_CONFIG.githubOwner}/${RELEASE_CONFIG.githubRepository}`,
  appReleasesUrl: `https://github.com/${RELEASE_CONFIG.githubOwner}/${RELEASE_CONFIG.githubRepository}/releases`,
  issuesUrl: `https://github.com/${RELEASE_CONFIG.githubOwner}/${RELEASE_CONFIG.githubRepository}/issues`,
  webRepoUrl: `https://github.com/${RELEASE_CONFIG.githubOwner}/${RELEASE_CONFIG.githubRepository}/tree/main/web`,
  contactEmail: 'priyanshuchawda20@gmail.com',
  nav: [
    { label: 'Features', href: '/features' },
    { label: 'How It Works', href: '/how-it-works' },
    { label: 'Privacy', href: '/privacy' },
    { label: 'Security', href: '/security' },
    { label: 'FAQ', href: '/faq' },
    { label: 'Download', href: '/download' },
  ] as readonly NavItem[],
} as const;
