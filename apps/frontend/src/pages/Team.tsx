import React from 'react'

const contributors = [
  { name: 'Saai', handle: 'SaaiAravindhRaja' },
  { name: 'Billy', handle: 'thanh913' },
  { name: 'Min yi', handle: 'minyiseah' },
  { name: 'Sing Ho', handle: 'LSH-Tech-tp' },
  { name: 'Garvit', handle: 'GarvitSobti' },
  { name: 'Nathan', handle: 'nathan11474' },
]

export function Team() {
  return (
    <div className="p-8">
      <h2 className="text-2xl font-bold mb-4">Team & Contributors</h2>
      <p className="text-muted-foreground mb-6">The TariffSheriff project is built by a growing group of contributors. Meet the core contributors below.</p>

      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-6">
        {contributors.map((c) => (
          <a key={c.handle} href={`https://github.com/${c.handle}`} className="flex flex-col items-center text-center hover:shadow-lg p-4 rounded-md bg-card" target="_blank" rel="noopener noreferrer">
            <img src={`https://github.com/${c.handle}.png`} alt={c.name} className="w-20 h-20 rounded-full object-cover mb-2" />
            <div className="font-medium">{c.name}</div>
            <div className="text-xs text-muted-foreground">@{c.handle}</div>
          </a>
        ))}
      </div>
    </div>
  )
}
