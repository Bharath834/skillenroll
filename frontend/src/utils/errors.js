/**
 * User-friendly error messages.
 *
 * Never surfaces raw stack traces. Prefers the backend's `message` from the
 * { success, message, data } envelope when it is informative; otherwise maps
 * the HTTP status to a friendly, consistent message.
 */

/**
 * Generic API error mapper used by catalog/details/list pages. The apiClient
 * already normalizes `error.message` from the backend envelope, so this only
 * replaces it for the statuses with a canonical friendly phrasing.
 */
export function getApiErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error) return fallback;
  const { status } = error;

  if (status === undefined || status === null) {
    return 'Unable to reach the server. Check your connection and try again.';
  }
  if (status === 401) return 'Please log in to view this content.';
  if (status === 403) return 'You do not have permission to access this.';
  if (status === 404) return 'The requested item could not be found.';
  if (status === 409) return 'That item already exists.';

  return error.message || fallback;
}
export function getAuthErrorMessage(error) {
  if (!error) return 'Something went wrong. Please try again.';

  const status = error.status;

  if (status === 401) return 'Invalid email or password. Please try again.';
  if (status === 403) return 'Your account does not have access to this action.';
  if (status === 409) return 'An account with this email or phone number already exists.';
  if (status === 400) {
    // Prefer a backend field-level message (e.g. "Phone number must be…").
    const firstFieldMessage = error.details
      ? Object.values(error.details).find((value) => typeof value === 'string')
      : null;
    return firstFieldMessage || error.message || 'Please check the information you entered and try again.';
  }
  if (status === undefined || status === null) {
    return 'Unable to reach the server. Check your connection and try again.';
  }

  return error.message || 'Something went wrong. Please try again.';
}

export default getAuthErrorMessage;
