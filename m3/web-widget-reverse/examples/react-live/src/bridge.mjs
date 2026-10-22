// SPDX-License-Identifier: Apache-2.0
// Presentation-only transport: the SWT/Nebula CPU model owns every row decision.
export const MAX_VISIBLE_ROWS = 256;
export const MAX_FRAME_CHARS = 262144;
const COLOR = /^#[0-9a-fA-F]{6}$/;
const ACTIONS = new Set(["select", "activate", "focus", "scroll"]);

function index(value) {
  return Number.isSafeInteger(value) && value >= 0 && value <= 2147483647;
}

export function normalizeFrame(revision, jsonText) {
  if (!Number.isSafeInteger(revision) || revision <= 0 ||
      typeof jsonText !== "string" || jsonText.length < 2 ||
      jsonText.length > MAX_FRAME_CHARS) {
    throw new TypeError("invalid bounded M3 frame");
  }
  const frame = JSON.parse(jsonText);
  if (frame === null || Array.isArray(frame) || typeof frame !== "object" ||
      frame.version !== 1 || frame.generation !== revision ||
      !index(frame.first) || !index(frame.total) ||
      frame.first > frame.total || !Array.isArray(frame.rows) ||
      frame.rows.length > MAX_VISIBLE_ROWS ||
      frame.first + frame.rows.length > frame.total) {
    throw new TypeError("invalid CPU viewport geometry");
  }
  if (typeof frame.locale !== "string" || frame.locale.length > 32 ||
      (frame.direction !== "ltr" && frame.direction !== "rtl")) {
    throw new TypeError("invalid presentation locale/direction");
  }
  const theme = frame.theme ?? {};
  if (typeof theme !== "object" || theme === null || Array.isArray(theme)) {
    throw new TypeError("invalid theme");
  }
  for (const key of ["foreground", "background", "accent"]) {
    if (theme[key] !== undefined && !COLOR.test(theme[key])) {
      throw new TypeError("invalid theme token");
    }
  }
  for (let i = 0; i < frame.rows.length; i++) {
    const row = frame.rows[i];
    if (row === null || typeof row !== "object" ||
        row.index !== frame.first + i || typeof row.key !== "string" ||
        row.key.length === 0 || row.key.length > 256 ||
        typeof row.label !== "string" || row.label.length > 4096 ||
        typeof row.selected !== "boolean") {
      throw new TypeError("invalid visible row at " + i);
    }
  }
  return Object.freeze({
    version: 1,
    generation: revision,
    first: frame.first,
    total: frame.total,
    locale: frame.locale,
    direction: frame.direction,
    theme: Object.freeze({ ...theme }),
    rows: Object.freeze(frame.rows.map(row => Object.freeze({ ...row }))),
  });
}

/** One current frame is retained by React; no independent data model is created. */
export function attachNebulaReceiver(browserWindow, onFrame) {
  if (!browserWindow || typeof onFrame !== "function") {
    throw new TypeError("receiver and callback required");
  }
  const previous = browserWindow.m3NebulaReceive;
  browserWindow.m3NebulaReceive = (revision, json) => {
    try {
      onFrame(normalizeFrame(revision, json));
      return true;
    } catch (error) {
      console.error("M3 frame refused", error);
      return false;
    }
  };
  if (typeof browserWindow.m3NebulaDispatch === "function") {
    browserWindow.m3NebulaDispatch(0, "ready", 0);
  }
  return () => {
    if (browserWindow.m3NebulaReceive !== undefined) {
      browserWindow.m3NebulaReceive = previous;
    }
  };
}

export function sendNebulaInput(browserWindow, frame, action, absoluteRowIndex) {
  if (!frame || !ACTIONS.has(action) || !index(absoluteRowIndex) ||
      typeof browserWindow?.m3NebulaDispatch !== "function") {
    return false;
  }
  return browserWindow.m3NebulaDispatch(
      frame.generation, action, absoluteRowIndex) === true;
}
