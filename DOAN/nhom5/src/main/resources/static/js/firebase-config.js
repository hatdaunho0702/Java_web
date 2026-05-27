import {
  getApp,
  getApps,
  initializeApp,
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";

let appInstance = null;
let authInstance = null;
let initPromise = null;

async function loadFirebaseConfig() {
  const response = await fetch("/api/auth/firebase-config", {
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(
      `Không thể tải cấu hình Firebase (HTTP ${response.status})`,
    );
  }

  return await response.json();
}

async function initFirebase() {
  if (authInstance && appInstance) {
    return { app: appInstance, auth: authInstance };
  }

  if (initPromise) {
    return initPromise;
  }

  initPromise = (async () => {
    const firebaseConfig = await loadFirebaseConfig();
    console.log("Firebase config loaded:", firebaseConfig);
    appInstance = getApps().length ? getApp() : initializeApp(firebaseConfig);
    authInstance = getAuth(appInstance);
    return { app: appInstance, auth: authInstance };
  })();

  try {
    return await initPromise;
  } catch (error) {
    initPromise = null;
    throw error;
  }
}

export async function getFirebaseAuth() {
  const firebase = await initFirebase();
  return firebase.auth;
}

export async function getFirebaseApp() {
  const firebase = await initFirebase();
  return firebase.app;
}
