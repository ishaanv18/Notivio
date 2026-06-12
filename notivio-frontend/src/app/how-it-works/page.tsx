'use client';

import { motion } from 'framer-motion';
import { Sparkles, Mail, Brain, BellRing, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import Link from 'next/link';

export default function HowItWorks() {
  const handleLogin = () => {
    const backendUrl = process.env.NEXT_PUBLIC_API_URL?.replace('/api', '') || 'http://localhost:8080';
    window.location.href = `${backendUrl}/oauth2/authorization/google`;
  };

  const steps = [
    {
      title: "Securely Connect Gmail",
      description: "Sign in with Google. We only request read-only access to scan for specific patterns like 'interview', 'assignment', or 'due date'.",
      icon: <Mail className="w-8 h-8 text-neon-blue" />,
      color: "border-neon-blue/30 bg-neon-blue/5"
    },
    {
      title: "AI Analysis",
      description: "Our LLaMA 3 powered engine reads the context of the emails to extract exact deadlines, companies, and interview modes (Virtual vs On-site).",
      icon: <Brain className="w-8 h-8 text-neon-purple" />,
      color: "border-neon-purple/30 bg-neon-purple/5"
    },
    {
      title: "Smart Extraction",
      description: "Tasks are instantly added to your beautifully designed dashboard. We automatically filter out promotional spam.",
      icon: <Sparkles className="w-8 h-8 text-neon-green" />,
      color: "border-neon-green/30 bg-neon-green/5"
    },
    {
      title: "Native Notifications",
      description: "Add Notivio to your iPhone home screen to receive native lock-screen push notifications right before your deadlines.",
      icon: <BellRing className="w-8 h-8 text-white" />,
      color: "border-white/20 bg-white/5"
    }
  ];

  return (
    <div className="min-h-screen bg-[#05050a] text-foreground selection:bg-neon-blue/30 relative">
      <div className="absolute inset-0 z-0 bg-grid-pattern opacity-50" />
      <div className="absolute top-0 inset-x-0 h-screen bg-hero-glow z-0 opacity-50" />

      {/* Floating Premium Navbar */}
      <div className="fixed top-6 inset-x-0 z-50 flex justify-center px-4">
        <nav className="flex items-center justify-between px-6 py-3 bg-white/[0.03] backdrop-blur-2xl border border-white/10 rounded-full w-full max-w-4xl shadow-2xl shadow-black/50">
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-neon-blue to-neon-purple flex items-center justify-center shadow-[0_0_15px_rgba(59,130,246,0.5)]">
              <Sparkles className="w-4 h-4 text-white" />
            </div>
            <span className="text-xl font-bold font-heading text-white tracking-tight hidden sm:block">Notivio</span>
          </Link>
          <div className="flex items-center gap-6 text-sm font-medium text-white/70">
            <Link href="/#features" className="hover:text-white transition-colors">Features</Link>
            <Link href="/how-it-works" className="text-white">How it Works</Link>
          </div>
          <Button onClick={handleLogin} variant="outline" className="bg-white text-black hover:bg-gray-200 border-none rounded-full px-6 h-10 font-semibold shadow-[0_0_20px_rgba(255,255,255,0.2)]">
            Sign In
          </Button>
        </nav>
      </div>

      <main className="relative z-10 pt-40 pb-32 px-6 max-w-4xl mx-auto text-center">
        <motion.h1 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-5xl md:text-7xl font-bold font-heading text-white mb-6"
        >
          How It <span className="text-transparent bg-clip-text bg-gradient-to-r from-neon-blue to-neon-purple">Works</span>
        </motion.h1>
        <motion.p 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="text-lg text-white/60 mb-20 max-w-2xl mx-auto"
        >
          Notivio runs completely in the background. Once connected, you never have to manually enter a deadline again.
        </motion.p>

        <div className="relative">
          {/* Vertical Line */}
          <div className="absolute left-1/2 top-0 bottom-0 w-px bg-gradient-to-b from-neon-blue via-neon-purple to-transparent -translate-x-1/2 hidden md:block" />
          
          <div className="space-y-12 md:space-y-24">
            {steps.map((step, index) => (
              <motion.div 
                key={index}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-100px" }}
                transition={{ duration: 0.6 }}
                className={`flex flex-col md:flex-row items-center gap-8 ${index % 2 === 1 ? 'md:flex-row-reverse' : ''}`}
              >
                <div className={`w-full md:w-1/2 flex ${index % 2 === 1 ? 'md:justify-start' : 'md:justify-end'}`}>
                  <div className={`glass-panel p-8 text-left border ${step.color} w-full max-w-sm hover:scale-105 transition-transform duration-300`}>
                    <div className="mb-6 bg-black/40 w-16 h-16 rounded-2xl flex items-center justify-center border border-white/10">
                      {step.icon}
                    </div>
                    <h3 className="text-2xl font-bold text-white mb-3 font-heading">{step.title}</h3>
                    <p className="text-white/60 leading-relaxed">{step.description}</p>
                  </div>
                </div>
                
                {/* Center Node */}
                <div className="hidden md:flex w-12 h-12 rounded-full bg-[#05050a] border-4 border-neon-purple z-10 items-center justify-center font-bold text-white shadow-[0_0_20px_rgba(139,92,246,0.5)]">
                  {index + 1}
                </div>
                
                <div className="hidden md:block w-full md:w-1/2" />
              </motion.div>
            ))}
          </div>
        </div>

        <motion.div 
          initial={{ opacity: 0, scale: 0.9 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          className="mt-32"
        >
          <Button 
            onClick={handleLogin}
            size="lg" 
            className="h-14 px-10 text-lg rounded-full bg-white text-black hover:bg-gray-100 hover:scale-105 transition-all shadow-[0_0_40px_rgba(255,255,255,0.3)]"
          >
            Start Using Notivio Now
            <ArrowRight className="ml-2 w-5 h-5" />
          </Button>
        </motion.div>
      </main>
    </div>
  );
}
