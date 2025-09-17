import React from 'react'

export default function Footer() {
  return (
    <footer className="border-t bg-background/95 text-sm text-muted-foreground">
      <div className="container mx-auto px-4 py-8 flex flex-col md:flex-row items-start md:items-center justify-between">
        <div className="mb-4 md:mb-0">
          <a href="/" className="flex items-center space-x-3">
            <img src="https://github.com/user-attachments/assets/aafe9ae4-9f11-47c1-998c-7012a57c0e72" alt="TariffSheriff" className="w-8 h-8 object-contain" />
            <span className="font-semibold">TariffSheriff</span>
          </a>
          <p className="text-xs text-muted-foreground mt-2">Trade intelligence platform helping teams reduce tariff risk and optimize costs.</p>
        </div>

        <div className="flex space-x-12">
          <nav aria-label="Footer navigation">
            <ul className="space-y-2">
              <li><a href="/about" className="hover:underline block">About</a></li>
              <li><a href="/privacy" className="hover:underline block">Privacy Policy</a></li>
              <li><a href="/team" className="hover:underline block">Team</a></li>
              <li><a href="/terms" className="hover:underline block">Terms</a></li>
              <li><a href="/contact" className="hover:underline block">Contact</a></li>
            </ul>
          </nav>

          <div>
            <p className="font-medium">Follow us</p>
            <div className="flex space-x-3 mt-2">
              <span aria-hidden className="hover:opacity-80">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
                  <path d="M22 5.92c-.63.28-1.3.48-2 .56.72-.43 1.27-1.12 1.53-1.94-.68.4-1.42.69-2.22.85C18.61 4.5 17.7 4 16.68 4c-1.53 0-2.77 1.24-2.77 2.77 0 .22.02.44.06.65C11.01 7.4 7.47 5.33 5 2.33c-.24.4-.38.88-.38 1.38 0 .95.48 1.79 1.21 2.28-.56-.02-1.09-.17-1.55-.43v.04c0 1.33.95 2.44 2.21 2.69-.23.06-.47.09-.72.09-.18 0-.36-.02-.53-.05.36 1.12 1.4 1.93 2.63 1.95C6 13 4.9 13.6 3.63 13.6c-.24 0-.48-.01-.71-.04 1.24.8 2.71 1.27 4.29 1.27 5.15 0 7.97-4.27 7.97-7.97v-.36c.56-.4 1.05-.9 1.44-1.47z" />
                </svg>
              </span>
              <span aria-hidden className="hover:opacity-80">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
                  <path d="M4.98 3.5C4.98 4.88 3.88 6 2.5 6S0 4.88 0 3.5 1.12 1 2.5 1 4.98 2.12 4.98 3.5zM0 8h5v13H0zM8 8h4.78v1.8h.07c.66-1.25 2.27-2.57 4.67-2.57C22.02 7.23 24 9.77 24 14.22V21H19v-6.1c0-1.45-.03-3.33-2.03-3.33-2.03 0-2.34 1.58-2.34 3.22V21H8V8z" />
                </svg>
              </span>
              <a href="https://github.com/SaaiAravindhRaja/TariffSheriff" aria-label="TariffSheriff on GitHub" className="hover:opacity-80" target="_blank" rel="noopener noreferrer">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
                  <path d="M12 .5C5.73.5.5 5.73.5 12c0 5.09 3.29 9.41 7.86 10.94.58.1.79-.25.79-.56 0-.28-.01-1.02-.02-2-3.2.7-3.88-1.54-3.88-1.54-.53-1.36-1.3-1.72-1.3-1.72-1.06-.73.08-.71.08-.71 1.18.08 1.8 1.21 1.8 1.21 1.04 1.78 2.72 1.27 3.38.97.1-.76.41-1.27.74-1.56-2.56-.29-5.26-1.28-5.26-5.72 0-1.26.45-2.29 1.2-3.1-.12-.29-.52-1.47.11-3.06 0 0 .98-.31 3.2 1.18a11.04 11.04 0 012.91-.39c.99 0 1.99.13 2.91.39 2.22-1.5 3.2-1.18 3.2-1.18.63 1.59.23 2.77.11 3.06.75.81 1.2 1.84 1.2 3.1 0 4.45-2.71 5.43-5.29 5.71.42.36.8 1.08.8 2.18 0 1.57-.01 2.84-.01 3.22 0 .31.21.67.8.56C20.71 21.41 24 17.09 24 12c0-6.27-5.23-11.5-12-11.5z" />
                </svg>
              </a>
            </div>
          </div>
        </div>
      </div>
      <div className="border-t pt-3">
        <div className="container mx-auto px-4 text-xs text-muted-foreground flex items-center justify-between">
          <span>© {new Date().getFullYear()} TariffSheriff. All rights reserved.</span>
        </div>
      </div>
    </footer>
  )
}
