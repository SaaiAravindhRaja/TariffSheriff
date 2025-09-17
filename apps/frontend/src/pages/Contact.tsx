import React from 'react'

export function Contact() {
  return (
    <div className="p-8 max-w-3xl">
      <h2 className="text-2xl font-bold mb-4">Contact Us</h2>

      <p className="mb-4 text-muted-foreground">Have questions, need data access, or want to partner? Fill out the form below or open an issue on GitHub.</p>

      <form className="space-y-4">
        <div>
          <label className="block text-sm font-medium">Name</label>
          <input className="mt-1 block w-full rounded-md border px-3 py-2 bg-background" placeholder="Your name" />
        </div>

        <div>
          <label className="block text-sm font-medium">Email</label>
          <input className="mt-1 block w-full rounded-md border px-3 py-2 bg-background" placeholder="you@example.com" />
        </div>

        <div>
          <label className="block text-sm font-medium">Message</label>
          <textarea className="mt-1 block w-full rounded-md border px-3 py-2 bg-background" rows={6} placeholder="How can we help?" />
        </div>

        <div className="flex items-center space-x-3">
          <button type="button" className="inline-flex items-center px-4 py-2 bg-brand-600 text-white rounded-md">Send message</button>
          <a href="https://github.com/SaaiAravindhRaja/TariffSheriff/issues" target="_blank" rel="noopener noreferrer" className="text-sm text-muted-foreground hover:underline">Or open an issue on GitHub</a>
        </div>
      </form>
    </div>
  )
}
