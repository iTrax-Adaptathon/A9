/**
 * Small inline SVG icon set. The app intentionally has no icon dependency, so
 * these are hand-rolled and inherit `currentColor` / `stroke`.
 */
function Svg({ size = 18, children, ...rest }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {children}
    </svg>
  );
}

export const ChevronLeft = (p) => <Svg {...p}><path d="M15 18l-6-6 6-6" /></Svg>;
export const ChevronDown = (p) => <Svg {...p}><path d="M6 9l6 6 6-6" /></Svg>;
export const ChevronRight = (p) => <Svg {...p}><path d="M9 18l6-6-6-6" /></Svg>;
export const Plus = (p) => <Svg {...p}><path d="M12 5v14M5 12h14" /></Svg>;
export const X = (p) => <Svg {...p}><path d="M18 6L6 18M6 6l12 12" /></Svg>;
export const Calendar = (p) => (
  <Svg {...p}>
    <rect x="3" y="4.5" width="18" height="16" rx="3" />
    <path d="M3 9.5h18M8 3v3M16 3v3" />
  </Svg>
);
export const CalendarCheck = (p) => (
  <Svg {...p}>
    <rect x="3" y="4.5" width="18" height="16" rx="3" />
    <path d="M3 9.5h18M8 3v3M16 3v3M9 14.5l2 2 4-4" />
  </Svg>
);
export const Grid = (p) => (
  <Svg {...p}>
    <rect x="3" y="3" width="7.5" height="7.5" rx="2" />
    <rect x="13.5" y="3" width="7.5" height="7.5" rx="2" />
    <rect x="3" y="13.5" width="7.5" height="7.5" rx="2" />
    <rect x="13.5" y="13.5" width="7.5" height="7.5" rx="2" />
  </Svg>
);
export const GridView = (p) => (
  <Svg {...p}>
    <rect x="3" y="3" width="18" height="18" rx="3" />
    <path d="M3 9.5h18M9.5 21V9.5" />
  </Svg>
);
export const ListView = (p) => (
  <Svg {...p}>
    <path d="M8 6h13M8 12h13M8 18h13" />
    <circle cx="3.5" cy="6" r="1" fill="currentColor" />
    <circle cx="3.5" cy="12" r="1" fill="currentColor" />
    <circle cx="3.5" cy="18" r="1" fill="currentColor" />
  </Svg>
);
export const Users = (p) => (
  <Svg {...p}>
    <circle cx="9" cy="8" r="3.2" />
    <path d="M3.5 19.5c0-3.3 2.5-5.5 5.5-5.5s5.5 2.2 5.5 5.5" />
    <path d="M16 5.2A3 3 0 0118.5 8M17 14c2.4.2 3.9 2.2 3.9 5" />
  </Svg>
);
export const CreditCard = (p) => (
  <Svg {...p}>
    <rect x="2.5" y="5" width="19" height="14" rx="3" />
    <path d="M2.5 10h19" />
  </Svg>
);
export const Repeat = (p) => (
  <Svg {...p}>
    <path d="M17 2.5l3 3-3 3" />
    <path d="M20 5.5H7a4 4 0 00-4 4v1" />
    <path d="M7 21.5l-3-3 3-3" />
    <path d="M4 18.5h13a4 4 0 004-4v-1" />
  </Svg>
);
export const HelpCircle = (p) => (
  <Svg {...p}>
    <circle cx="12" cy="12" r="9" />
    <path d="M9.5 9.2a2.6 2.6 0 015 .9c0 1.7-2.5 2-2.5 3.4" />
    <circle cx="12" cy="17" r="1" fill="currentColor" stroke="none" />
  </Svg>
);
export const Settings = (p) => (
  <Svg {...p}>
    <circle cx="12" cy="12" r="3" />
    <path d="M12 2.5v2M12 19.5v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M2.5 12h2M19.5 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4" />
  </Svg>
);
export const Moon = (p) => (
  <Svg {...p}>
    <path d="M20 14.5A8 8 0 019.5 4a8 8 0 1010.5 10.5z" />
  </Svg>
);
export const Share = (p) => (
  <Svg {...p}>
    <circle cx="18" cy="5.5" r="2.5" />
    <circle cx="6" cy="12" r="2.5" />
    <circle cx="18" cy="18.5" r="2.5" />
    <path d="M8.2 10.8l7.6-4M8.2 13.2l7.6 4" />
  </Svg>
);
export const Bell = (p) => (
  <Svg {...p}>
    <path d="M18 8.5a6 6 0 10-12 0c0 5-2 6.5-2 6.5h16s-2-1.5-2-6.5" />
    <path d="M13.7 19a2 2 0 01-3.4 0" />
  </Svg>
);
export const Filter = (p) => (
  <Svg {...p}>
    <path d="M3 5h18l-7 8v6l-4-2v-4z" />
  </Svg>
);
export const MoreHorizontal = (p) => (
  <Svg {...p}>
    <circle cx="5" cy="12" r="1.4" fill="currentColor" stroke="none" />
    <circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none" />
    <circle cx="19" cy="12" r="1.4" fill="currentColor" stroke="none" />
  </Svg>
);
export const Check = (p) => <Svg {...p}><path d="M20 6L9 17l-5-5" /></Svg>;
export const Pencil = (p) => (
  <Svg {...p}>
    <path d="M4 20h4l10.5-10.5a2.1 2.1 0 000-3L17 5a2.1 2.1 0 00-3 0L3.5 15.5V20z" />
    <path d="M13.5 5.5l3 3" />
  </Svg>
);
export const Trash = (p) => (
  <Svg {...p}>
    <path d="M4 7h16" />
    <path d="M9 7V5.5A1.5 1.5 0 0110.5 4h3A1.5 1.5 0 0115 5.5V7" />
    <path d="M6.5 7l.8 12a2 2 0 002 1.9h5.4a2 2 0 002-1.9l.8-12" />
    <path d="M10 11v6M14 11v6" />
  </Svg>
);
export const AlertTriangle = (p) => (
  <Svg {...p}>
    <path d="M10.3 4.3L2.6 17.5A2 2 0 004.3 20.5h15.4a2 2 0 001.7-3L13.7 4.3a2 2 0 00-3.4 0z" />
    <path d="M12 9.5v4M12 17h.01" />
  </Svg>
);

// Calendar icon created by Magnific - Flaticon (https://www.flaticon.com/free-icons/calendar)
export const LogoMark = ({ size = 34 }) => (
  <svg width={size} height={size} viewBox="0 0 34 34" fill="none" aria-hidden="true">
    <rect width="34" height="34" rx="10" fill="currentColor" />
    <rect x="8" y="10.5" width="18" height="15.5" rx="3" stroke="#fff" strokeWidth="2" />
    <path d="M8 15h18" stroke="#fff" strokeWidth="2" />
    <path d="M12.5 8v3.5M21.5 8v3.5" stroke="#fff" strokeWidth="2" strokeLinecap="round" />
    <path
      d="M12.5 20l2.3 2.3 4.4-4.4"
      stroke="#fff"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);
