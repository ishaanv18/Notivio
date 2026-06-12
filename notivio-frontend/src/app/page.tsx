'use client';

import { motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Sparkles, Mail, Brain, BellRing, ArrowRight, CalendarClock, Shield } from 'lucide-react';
import { useAuthStore } from '@/store/useAuthStore';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import Link from 'next/link';

export default function LandingPage() {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    if (isAuthenticated) {
      router.push('/dashboard');
    }
  }, [isAuthenticated, router]);

  const handleLogin = () => {
    const backendUrl = process.env.NEXT_PUBLIC_API_URL?.replace('/api', '') || 'http://localhost:8080';
    window.location.href = `${backendUrl}/oauth2/authorization/google`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#05050a] via-[#0a0a14] to-[#0f0f1e] text-foreground selection:bg-neon-blue/30 relative overflow-hidden">
      {/* Animated Background Elements */}
      <div className="absolute inset-0 z-0 bg-grid-pattern opacity-50" />
      <div className="absolute top-0 inset-x-0 h-[800px] bg-hero-glow z-0" />
      
      {/* Floating orbs */}
      <motion.div
        className="absolute top-20 left-10 w-72 h-72 bg-neon-purple/20 rounded-full blur-3xl"
        animate={{ 
          y: [0, 50, 0],
          x: [0, 30, 0],
          scale: [1, 1.2, 1]
        }}
        transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
      />
      <motion.div
        className="absolute bottom-20 right-10 w-96 h-96 bg-neon-blue/20 rounded-full blur-3xl"
        animate={{ 
          y: [0, -50, 0],
          x: [0, -30, 0],
          scale: [1, 1.3, 1]
        }}
        transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }}
      />
      
      {/* Floating Premium Navbar */}
      <motion.div 
        className="fixed top-6 inset-x-0 z-50 flex justify-center px-4"
        initial={{ y: -100, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
      >
        <nav className="flex items-center justify-between px-6 py-3 ios-blur border border-white/10 rounded-full w-full max-w-5xl shadow-2xl">
          <Link href="/">
            <motion.div 
              className="flex items-center gap-2"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              <motion.div 
                className="w-9 h-9 rounded-full bg-gradient-to-br from-neon-blue to-neon-purple flex items-center justify-center shadow-lg shadow-neon-blue/50"
                animate={{ rotate: [0, 360] }}
                transition={{ duration: 20, repeat: Infinity, ease: "linear" }}
              >
                <Sparkles className="w-4 h-4 text-white" />
              </motion.div>
              <span className="text-xl font-bold font-heading text-white tracking-tight hidden sm:block">Notivio</span>
            </motion.div>
          </Link>
          <div className="hidden md:flex items-center gap-6 text-sm font-medium text-white/70">
            <Link href="#features" className="hover:text-white transition-colors">Features</Link>
            <Link href="/how-it-works" className="hover:text-white transition-colors">How it Works</Link>
          </div>
          <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
            <Button 
              onClick={handleLogin} 
              variant="outline" 
              className="bg-white text-black hover:bg-gray-100 border-none rounded-full px-6 h-10 font-semibold shadow-lg shadow-white/20 btn-haptic"
            >
              Sign In
            </Button>
          </motion.div>
        </nav>
      </motion.div>

      {/* Hero Section with enhanced animations */}
      <main className="relative z-10 flex flex-col items-center pt-32 md:pt-40 pb-20 px-4 text-center">
        
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-full ios-card border-neon-blue/30 mb-8 shadow-lg shadow-neon-blue/20"
        >
          <motion.span 
            className="flex h-2.5 w-2.5 rounded-full bg-neon-blue"
            animate={{ scale: [1, 1.2, 1], opacity: [1, 0.7, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
          />
          <span className="text-sm font-bold text-neon-blue tracking-wide uppercase">Notivio AI is Live</span>
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.1, ease: [0.16, 1, 0.3, 1] }}
          className="text-5xl md:text-7xl lg:text-8xl font-bold font-heading leading-[1.1] tracking-tighter mb-6 max-w-5xl mx-auto text-white"
        >
          Never Miss An Interview <br className="hidden md:block" />
          <motion.span 
            className="text-transparent bg-clip-text bg-gradient-to-r from-neon-blue via-neon-purple to-neon-green animate-gradient"
            animate={{ backgroundPosition: ['0%', '100%', '0%'] }}
            transition={{ duration: 5, repeat: Infinity, ease: 'linear' }}
          >
            Or Deadline Again.
          </motion.span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.2, ease: [0.16, 1, 0.3, 1] }}
          className="text-base md:text-xl text-white/60 mb-10 max-w-2xl mx-auto leading-relaxed"
        >
          Notivio securely connects to your Gmail, automatically extracts important events using advanced AI, and sends smart push notifications directly to your iPhone.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.6, delay: 0.4, ease: [0.16, 1, 0.3, 1] }}
          className="flex flex-col sm:flex-row gap-4 mb-20 md:mb-32"
        >
          <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
            <Button 
              onClick={handleLogin}
              size="lg" 
              className="h-14 px-8 text-base md:text-lg rounded-full bg-gradient-to-r from-white via-gray-100 to-white text-black hover:shadow-2xl transition-all shadow-xl shadow-white/30 font-bold btn-haptic"
            >
              Get Started for Free
              <motion.div
                animate={{ x: [0, 5, 0] }}
                transition={{ duration: 1.5, repeat: Infinity }}
              >
                <ArrowRight className="ml-2 w-5 h-5" />
              </motion.div>
            </Button>
          </motion.div>
          <Link href="/how-it-works">
            <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
              <Button 
                size="lg" 
                variant="outline"
                className="h-14 px-8 text-base md:text-lg rounded-full bg-transparent border-white/20 text-white hover:bg-white/10 transition-all w-full sm:w-auto backdrop-blur-xl"
              >
                See How it Works
              </Button>
            </motion.div>
          </Link>
        </motion.div>

        {/* Enhanced iPhone-style Mockups Section */}
        <motion.div 
          initial={{ opacity: 0, y: 60 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.6, ease: "easeOut" }}
          className="w-full max-w-6xl mx-auto relative hidden md:flex justify-center items-center gap-6 perspective-1000"
        >
          <motion.div 
            className="w-80 translate-y-8"
            animate={{ 
              y: [32, 40, 32],
              rotateY: [-4, -6, -4]
            }}
            transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
            whileHover={{ rotateY: 0, y: -8, scale: 1.05, zIndex: 30 }}
            style={{ transformStyle: 'preserve-3d' }}
          >
            <MockTaskCard type="ASSIGNMENT" title="CS400 Final Project" desc="Submit source code and report via Canvas." />
          </motion.div>

          <motion.div 
            className="w-96 z-20 scale-110"
            animate={{ 
              y: [0, -8, 0],
              scale: [1.1, 1.15, 1.1]
            }}
            transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
            whileHover={{ y: -16, scale: 1.2, zIndex: 40 }}
            style={{ transformStyle: 'preserve-3d' }}
          >
            <MockTaskCard type="INTERVIEW" title="Google - Software Engineer" desc="On-site final round technical interview." mode="ONSITE" highlight />
          </motion.div>

          <motion.div 
            className="w-80 translate-y-8"
            animate={{ 
              y: [32, 40, 32],
              rotateY: [4, 6, 4]
            }}
            transition={{ duration: 4.5, repeat: Infinity, ease: "easeInOut" }}
            whileHover={{ rotateY: 0, y: -8, scale: 1.05, zIndex: 30 }}
            style={{ transformStyle: 'preserve-3d' }}
          >
             <MockTaskCard type="DEADLINE" title="Registration Fee" desc="Last day to pay university registration fee." />
          </motion.div>
        </motion.div>
        
        {/* Mobile mockup */}
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.6 }}
          className="md:hidden w-full max-w-sm mx-auto"
        >
          <MockTaskCard type="INTERVIEW" title="Google - Software Engineer" desc="On-site final round technical interview." mode="ONSITE" highlight />
        </motion.div>
      </main>

      {/* Enhanced Features Grid */}
      <section id="features" className="relative z-10 py-20 md:py-32 px-6 bg-black/40 border-t border-white/5 backdrop-blur-xl">
        <div className="max-w-6xl mx-auto">
          <motion.div 
            className="text-center mb-16 md:mb-20"
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
          >
            <h2 className="text-3xl md:text-5xl font-bold font-heading text-white mb-4 md:mb-6">
              Built for ultimate productivity
            </h2>
            <p className="text-base md:text-xl text-white/60 max-w-2xl mx-auto">
              Everything you need to stay on top of your schedule, powered entirely by artificial intelligence.
            </p>
          </motion.div>

          <motion.div 
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6"
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            variants={{
              visible: { transition: { staggerChildren: 0.1 } }
            }}
          >
            <FeatureCard 
              icon={<Brain className="w-6 h-6 text-neon-purple" />}
              gradient="from-violet-500 to-purple-500"
              title="LLaMA 3 AI Engine"
              description="Powered by Groq & Meta's LLaMA 3 for instant, accurate email analysis."
            />
            <FeatureCard 
              icon={<Mail className="w-6 h-6 text-neon-blue" />}
              gradient="from-blue-500 to-cyan-500"
              title="Secure Gmail Sync"
              description="Read-only access ensures your data is safe. We only look for deadlines."
            />
            <FeatureCard 
              icon={<BellRing className="w-6 h-6 text-neon-green" />}
              gradient="from-emerald-500 to-green-500"
              title="Native iOS Push"
              description="Install as a PWA and get native lock-screen notifications on your iPhone."
            />
            <FeatureCard 
              icon={<Shield className="w-6 h-6 text-white" />}
              gradient="from-slate-400 to-gray-500"
              title="Spam Filtering"
              description="Smartly ignores newsletters and promotional emails, focusing only on real events."
            />
          </motion.div>
        </div>
      </section>
      
      {/* Enhanced Footer */}
      <footer className="border-t border-white/5 py-8 md:py-12 text-center text-white/40 text-sm relative z-10 backdrop-blur-xl">
        <motion.p
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
        >
          © 2026 Notivio. All rights reserved.
        </motion.p>
      </footer>
    </div>
  );
}

function FeatureCard({ icon, gradient, title, description }: { 
  icon: React.ReactNode; 
  gradient: string;
  title: string; 
  description: string;
}) {
  return (
    <motion.div 
      className="ios-card p-6 md:p-8 flex flex-col group relative overflow-hidden"
      variants={{
        hidden: { opacity: 0, y: 30 },
        visible: { opacity: 1, y: 0 }
      }}
      whileHover={{ y: -8, scale: 1.02 }}
      transition={{ type: 'spring', stiffness: 300, damping: 25 }}
    >
      {/* Animated gradient background */}
      <div className={`absolute inset-0 bg-gradient-to-br ${gradient} opacity-0 group-hover:opacity-10 transition-opacity duration-500`} />
      
      <motion.div 
        className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${gradient} opacity-15 flex items-center justify-center mb-6 border border-white/10 relative overflow-hidden`}
        whileHover={{ scale: 1.1, rotate: [0, -5, 5, 0] }}
        transition={{ duration: 0.5 }}
      >
        <div className={`absolute inset-0 bg-gradient-to-br ${gradient} blur-xl opacity-50`} />
        <div className="relative z-10">
          {icon}
        </div>
      </motion.div>
      
      <h3 className="text-lg md:text-xl font-bold mb-3 font-heading text-white relative z-10">{title}</h3>
      <p className="text-white/60 leading-relaxed text-sm relative z-10">{description}</p>
    </motion.div>
  );
}

function MockTaskCard({ type, title, desc, mode, highlight }: { 
  type: string; 
  title: string; 
  desc: string; 
  mode?: string;
  highlight?: boolean;
}) {
  const isInterview = type === 'INTERVIEW';
  return (
    <motion.div 
      className={`ios-card p-6 border relative overflow-hidden ${
        isInterview 
          ? 'border-neon-purple/40 bg-gradient-to-br from-violet-950/30 to-purple-950/20' 
          : 'border-white/10 bg-gradient-to-br from-white/[0.08] to-white/[0.03]'
      } ${highlight ? 'shadow-2xl shadow-neon-purple/30' : ''}`}
      whileHover={{ scale: 1.05, y: -5 }}
      transition={{ type: 'spring', stiffness: 400, damping: 25 }}
    >
      {isInterview && (
        <div className="absolute inset-0 bg-gradient-to-br from-violet-500/10 via-transparent to-purple-500/10 animate-gradient pointer-events-none" />
      )}
      
      <div className="relative z-10">
        <div className="flex justify-between items-start mb-4">
          <h4 className="font-bold text-white text-base md:text-lg">{title}</h4>
          <motion.span 
            className={`text-[10px] uppercase font-bold px-2.5 py-1 rounded-full ${
              isInterview ? 'bg-neon-purple/20 text-neon-purple border border-neon-purple/30' : 'bg-white/10 text-white/70 border border-white/10'
            }`}
            whileHover={{ scale: 1.1 }}
          >
            {type}
          </motion.span>
        </div>
        <p className="text-sm text-white/60 mb-6">{desc}</p>
        <div className="flex items-center gap-3 flex-wrap">
          <div className="flex items-center text-xs font-semibold text-neon-blue bg-neon-blue/15 px-3 py-1.5 rounded-full border border-neon-blue/30">
            <CalendarClock className="w-3.5 h-3.5 mr-1.5" /> Due Tomorrow
          </div>
          {mode && (
            <div className="flex items-center text-xs font-semibold text-white/70 bg-white/10 px-3 py-1.5 rounded-full border border-white/10">
              {mode}
            </div>
          )}
        </div>
      </div>
    </motion.div>
  );
}
