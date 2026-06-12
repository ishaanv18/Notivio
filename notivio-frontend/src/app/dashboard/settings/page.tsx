'use client';

import { useAuthStore } from '@/store/useAuthStore';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { LogOut, Mail, Settings2, Bell, Shield, Smartphone } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { NotificationSetup } from '@/components/NotificationSetup';

export default function SettingsPage() {
  const { user, logout } = useAuthStore();

  if (!user) return null;

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-4xl mx-auto">
      <div>
        <h1 className="text-3xl font-bold font-heading">Settings</h1>
        <p className="text-muted-foreground mt-1">Manage your account preferences and integrations.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        
        {/* Left Column: Profile */}
        <div className="md:col-span-1 space-y-6">
          <div className="glass-panel p-6 text-center flex flex-col items-center">
            <Avatar className="w-24 h-24 border-2 border-neon-blue/30 shadow-[0_0_20px_rgba(59,130,246,0.2)] mb-4">
              <AvatarImage src={user.profilePicture} />
              <AvatarFallback className="text-2xl">{user.name.substring(0, 2)}</AvatarFallback>
            </Avatar>
            <h2 className="text-xl font-bold text-white">{user.name}</h2>
            <p className="text-sm text-white/60 mb-6">{user.email}</p>
            
            <Button 
              onClick={logout}
              variant="destructive"
              className="w-full bg-destructive/10 text-destructive hover:bg-destructive hover:text-white border border-destructive/20"
            >
              <LogOut className="w-4 h-4 mr-2" />
              Sign Out
            </Button>
          </div>
        </div>

        {/* Right Column: Config */}
        <div className="md:col-span-2 space-y-6">
          
          <div className="glass-panel p-6">
            <h3 className="text-lg font-semibold flex items-center gap-2 mb-4">
              <Settings2 className="w-5 h-5 text-neon-purple" />
              Integrations
            </h3>
            
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 rounded-xl bg-white/5 border border-white/10">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-full bg-red-500/20 flex items-center justify-center border border-red-500/30">
                    <Mail className="w-5 h-5 text-red-500" />
                  </div>
                  <div>
                    <p className="font-medium text-white">Google Gmail</p>
                    <p className="text-xs text-white/60">Connected as {user.email}</p>
                  </div>
                </div>
                <div className="px-3 py-1 rounded-full bg-neon-green/20 text-neon-green text-xs font-semibold border border-neon-green/30">
                  Connected
                </div>
              </div>
            </div>
          </div>

          <div className="glass-panel p-6">
             <h3 className="text-lg font-semibold flex items-center gap-2 mb-4">
              <Bell className="w-5 h-5 text-neon-blue" />
              Notifications
            </h3>
            
            <div className="space-y-4">
              <NotificationSetup />
              
              <div className="flex items-center justify-between p-4 rounded-xl bg-white/5 border border-white/10">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center border border-white/20">
                    <Smartphone className="w-5 h-5 text-white/70" />
                  </div>
                  <div>
                    <p className="font-medium text-white">Mobile Push</p>
                    <p className="text-xs text-white/60">Install Notivio on your phone to receive alerts</p>
                  </div>
                </div>
                <Button variant="outline" className="text-xs h-8 border-white/20 bg-transparent text-white hover:bg-white/10">
                  Get App
                </Button>
              </div>
            </div>
          </div>

          <div className="glass-panel p-6">
             <h3 className="text-lg font-semibold flex items-center gap-2 mb-4">
              <Shield className="w-5 h-5 text-white/70" />
              Privacy & Data
            </h3>
            <p className="text-sm text-white/60 leading-relaxed mb-4">
              Notivio uses LLaMA 3 to extract deadlines from your emails. We do not store the body of your emails on our servers permanently. Only extracted deadlines and task titles are saved to provide you with reminders.
            </p>
          </div>

        </div>
      </div>
    </div>
  );
}
