/**
 * User-friendly error messages for auth flows.
 *
 * Never surfaces raw stack traces. Prefers the backend's `message` from the
 * { success, message, data } envelope when it is informative; otherwise maps
 * the HTTP status to a friendly, consistent message.
 */
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
