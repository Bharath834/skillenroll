import { useEffect } from 'react';

const BRAND = 'SkillEnroll';

/**
 * Sets document.title for a page, e.g. useDocumentTitle('Courses')
 * -> "Courses · SkillEnroll". Restores the previous title on unmount.
 */
export default function useDocumentTitle(title) {
  useEffect(() => {
    const previous = document.title;
    document.title = title ? `${title} · ${BRAND}` : BRAND;
    return () => {
      document.title = previous;
    };
  }, [title]);
}
