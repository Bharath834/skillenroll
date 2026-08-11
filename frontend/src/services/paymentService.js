import apiClient from './apiClient.js';
import { PAYMENT_ENDPOINTS } from './endpoints.js';

/**
 * Payment-domain service: Razorpay order creation, payment verification, and
 * lazy loading of the Razorpay Checkout.js script (no npm package needed).
 *
 * Reuses the centralized apiClient (base URL, Bearer-token injection, error
 * normalization) and the endpoint constants — no duplicate client or
 * hardcoded backend URLs. The backend wraps every response in
 * { success, message, data, timestamp }, so these helpers unwrap to `data`.
 *
 * NOTE: per the backend SecurityConfig, every /api/payment call requires a
 * valid JWT — callers must be authenticated.
 */

const RAZORPAY_CHECKOUT_URL = 'https://checkout.razorpay.com/v1/checkout.js';

const unwrap = (response) => response.data?.data ?? response.data;

/** Cache the Checkout.js load so it happens at most once per page session. */
let razorpayScriptPromise = null;

export const paymentApi = {
  /**
   * POST /api/payment/create-order { courseId } -> PaymentOrderResponse.
   * (orderId, amount in paise, currency, receipt, status, keyId, ...).
   * The amount is always derived server-side from the course price.
   */
  createOrder: (courseId) =>
    apiClient.post(PAYMENT_ENDPOINTS.createOrder, { courseId }).then(unwrap),

  /**
   * POST /api/payment/verify { orderId, paymentId, signature, courseId }
   * -> PaymentVerificationResponse (verified, orderId, paymentId, enrollmentStatus).
   */
  verifyPayment: (payload) => apiClient.post(PAYMENT_ENDPOINTS.verify, payload).then(unwrap),

  /**
   * POST /api/payment/re-verify { orderId, courseId } -> PaymentVerificationResponse.
   * Re-confirms a payment that may have been captured but not yet confirmed —
   * the server reads the payment id + signature from its ledger, so no new
   * Razorpay order is created (no double charge). 409 = no verified payment
   * recorded for the order (a new payment is needed).
   */
  reVerifyPayment: (payload) => apiClient.post(PAYMENT_ENDPOINTS.reVerify, payload).then(unwrap),
};

/**
 * Loads the Razorpay Checkout.js script once and resolves with the global
 * Razorpay constructor. Subsequent calls reuse the cached promise (or the
 * already-loaded window.Razorpay) — the script is injected only when a
 * checkout is actually started.
 */
export function loadRazorpayCheckout() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('Razorpay checkout is only available in the browser.'));
  }
  if (window.Razorpay) return Promise.resolve(window.Razorpay);
  if (!razorpayScriptPromise) {
    razorpayScriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = RAZORPAY_CHECKOUT_URL;
      script.async = true;
      script.onload = () => {
        if (window.Razorpay) {
          resolve(window.Razorpay);
        } else {
          reject(new Error('Razorpay checkout failed to initialise. Please try again.'));
        }
      };
      script.onerror = () => {
        razorpayScriptPromise = null; // allow a retry on the next attempt
        reject(new Error('Razorpay checkout could not be loaded. Check your connection and try again.'));
      };
      document.head.appendChild(script);
    });
  }
  return razorpayScriptPromise;
}

export default paymentApi;
