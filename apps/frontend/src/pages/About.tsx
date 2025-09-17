import React from 'react'

export function About() {
  return (
    <div className="p-8 max-w-4xl">
      <h2 className="text-3xl font-extrabold mb-4">About TariffSheriff</h2>

      <p className="mb-4 text-muted-foreground">TariffSheriff is a trade intelligence platform built for supply chain, procurement, and compliance teams. Our goal is to make tariff and trade policy data actionable: we help teams understand the real cost, compliance impact, and risk of cross-border trade decisions so they can optimise sourcing and reduce unexpected duties.</p>

      <h3 className="text-xl font-semibold mt-6">Mission</h3>
      <p className="text-muted-foreground">We aim to reduce friction in international trade by providing fast, accurate, and explainable tariff intelligence. TariffSheriff surfaces the right data at the right time so teams can make decisions with confidence.</p>

      <h3 className="text-xl font-semibold mt-6">What we do</h3>
      <ul className="list-disc pl-6 mt-2 text-muted-foreground">
        <li>Lookup and decode HS (Harmonized System) codes and tariff schedules across countries.</li>
        <li>Compare tariff liabilities across alternative routes, suppliers, and product classifications.</li>
        <li>Simulate scenarios — e.g. change of origin, routing, or supplier — and surface estimated duty and compliance impacts.</li>
        <li>Provide visual analytics and exportable reports for procurement and finance teams.</li>
      </ul>

      <h3 className="text-xl font-semibold mt-6">Architecture & Data</h3>
      <p className="text-muted-foreground">TariffSheriff combines curated tariff tables, country schedules, and HS mapping tables with optional customer datasets. The platform is built as a monorepo (frontend: Vite + React; backend: Spring Boot) and ingests data from public sources (e.g., WITS) and licensed datasets where available. We prioritise reproducible pipelines and deterministic transforms so results are auditable.</p>

      <h3 className="text-xl font-semibold mt-6">Integrations</h3>
      <p className="text-muted-foreground">Common integrations include ERP systems, procurement tools, and customs classification services. We also expose a REST API for programmatic queries and batch processing.</p>

      <h3 className="text-xl font-semibold mt-6">Security & Compliance</h3>
      <p className="text-muted-foreground">Security is central to our design. Access is controlled via role-based authentication, data in transit is encrypted (TLS), and sensitive environments are hardened. For customers with specific compliance needs we support on-prem or private cloud deployments.</p>

      <h3 className="text-xl font-semibold mt-6">Roadmap</h3>
      <ul className="list-disc pl-6 mt-2 text-muted-foreground">
        <li>Expanded tariff rule coverage and historical rates visualization.</li>
        <li>AI-assisted HS code suggestions and classification quality scoring.</li>
        <li>Automated alerts for tariff changes and trade policy updates.</li>
      </ul>

      <h3 className="text-xl font-semibold mt-6">Get in touch</h3>
      <p className="text-muted-foreground">For partnerships, data licensing, or support please reach out via the Contact page or open an issue in the GitHub repository.</p>
    </div>
  )
}
