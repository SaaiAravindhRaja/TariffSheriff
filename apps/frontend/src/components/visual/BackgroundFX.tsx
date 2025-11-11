import React from 'react'
import { useTheme } from '@/contexts/ThemeContext'

interface BackgroundFXProps {
  variant?: 'app' | 'hero'
}

// Fixed-position decorative background with subtle blobs and vignette.
// Non-interactive and does not affect layout.
export default function BackgroundFX({ variant = 'app' }: BackgroundFXProps) {
  const { theme } = useTheme()
  const intensity = variant === 'hero' ? '' : 'opacity-60'

  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      {/* Base gradient (blue → purple). Brighter tones in light theme. */}
      <div className={
        theme === 'dark'
          ? "absolute inset-0 bg-[radial-gradient(1200px_600px_at_20%_-10%,#1e1b4b_0%,transparent_60%),radial-gradient(900px_500px_at_80%_0%,#312e81_0%,transparent_55%),radial-gradient(700px_500px_at_50%_110%,#1e40af_0%,transparent_60%)]"
          : "absolute inset-0 bg-[radial-gradient(1100px_550px_at_20%_-10%,#c7d2fe_0%,transparent_60%),radial-gradient(820px_480px_at_80%_0%,#ddd6fe_0%,transparent_55%),radial-gradient(680px_480px_at_50%_110%,#bfdbfe_0%,transparent_60%)]"
      } />

      {/* Soft color blobs (limited to blue/purple hues) */}
      <div className={`absolute -top-24 -left-32 h-[26rem] w-[26rem] rounded-full bg-violet-600/70 blur-3xl mix-blend-screen ${intensity} animate-float-slow`} />
      <div className={`absolute bottom-[-8rem] right-[-6rem] h-[32rem] w-[32rem] rounded-full bg-indigo-600/70 blur-3xl mix-blend-screen ${intensity} animate-drift-slower`} />

      {/* Low-opacity central glow so no area feels empty */}
      <div className={`absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 h-[40rem] w-[40rem] rounded-full bg-indigo-500/25 blur-[180px] ${intensity}`} />

      {/* Faint angled grid lines */}
      <div className="absolute inset-0 opacity-[0.05] mix-blend-overlay"
           style={{
             backgroundImage:
               'repeating-linear-gradient(115deg, #ffffff 0px, #ffffff 1px, transparent 1px, transparent 22px)'
           }}
      />

      {/* Vignette to maintain readability. Dark uses black tint, light uses white tint. */}
      {theme === 'dark' ? (
        <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-black/40 to-black/70" />
      ) : (
        <div className="absolute inset-0 bg-gradient-to-b from-white/60 via-white/40 to-white/20" />
      )}
    </div>
  )
}
