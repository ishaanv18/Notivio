'use client';

import { useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuthStore } from '@/store/useAuthStore';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';

/**
 * SECURITY: This page receives an opaque one-time code (NOT the actual JWT).
 * It exchanges the code for real JWT tokens via a POST to /api/auth/exchange.
 * Tokens NEVER appear in the URL — they are only stored in Zustand memory.
 *
 * Why this matters:
 *  - URL query params are saved in browser history
 *  - URL query params appear in server access logs
 *  - URL query params leak via Referer headers to any third-party scripts
 *
 * With this approach, the URL only ever contains an opaque 60-second code.
 */
function AuthCallbackInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const login = useAuthStore((state) => state.login);

  useEffect(() => {
    const code = searchParams.get('code');

    if (!code) {
      router.push('/');
      return;
    }

    // Exchange the opaque code for real JWT tokens
    fetch('/api/auth/exchange', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code }),
    })
      .then(async (res) => {
        if (!res.ok) {
          throw new Error(`Exchange failed: ${res.status}`);
        }
        return res.json();
      })
      .then(async ({ accessToken }) => {
        // Store token so subsequent API calls are authenticated
        useAuthStore.setState({ token: accessToken });

        // Fetch user profile
        const profileRes = await api.get('/auth/me');
        login(profileRes.data, accessToken);

        // Replace history so the code URL is gone from the browser history
        router.replace('/dashboard');
      })
      .catch((error) => {
        console.error('Auth callback failed:', error);
        router.replace('/');
      });
  }, [searchParams, login, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background flex-col gap-4">
      <div className="relative">
        <div className="w-16 h-16 rounded-full bg-violet-500/10 border border-violet-500/20 flex items-center justify-center">
          <Loader2 className="w-8 h-8 text-violet-400 animate-spin" />
        </div>
      </div>
      <div className="text-center">
        <p className="text-white font-semibold text-lg">Authenticating securely...</p>
        <p className="text-white/40 text-sm mt-1">Verifying your identity</p>
      </div>
    </div>
  );
}

export default function AuthCallback() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-background flex-col gap-4">
          <div className="w-16 h-16 rounded-full bg-violet-500/10 border border-violet-500/20 flex items-center justify-center">
            <Loader2 className="w-8 h-8 text-violet-400 animate-spin" />
          </div>
        </div>
      }
    >
      <AuthCallbackInner />
    </Suspense>
  );
}
