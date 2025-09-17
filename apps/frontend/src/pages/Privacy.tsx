import React from 'react'

export function Privacy() {
  return (
    <div className="p-8 max-w-4xl">
      <h2 className="text-3xl font-extrabold mb-4">Privacy Policy</h2>

      <p className="mb-4 text-muted-foreground">This Privacy Policy explains how TariffSheriff collects, uses, stores, and discloses Personal Data. We are committed to protecting the privacy of our users and customers while providing valuable trade intelligence services.</p>

      <h3 className="text-xl font-semibold mt-6">Information we collect</h3>
      <ul className="list-disc pl-6 mt-2 text-muted-foreground">
        <li>Account and profile data you provide (name, email, company).</li>
        <li>Transactional and usage data (queries, reports generated, feature usage).</li>
        <li>Optional data uploaded by customers (product catalogs, customs declarations) — treated as customer data.</li>
      </ul>

      <h3 className="text-xl font-semibold mt-6">How we use information</h3>
      <p className="text-muted-foreground">We use collected information to deliver and improve the service, provide support, process payments (if applicable), and to comply with legal obligations. Aggregated and anonymized data may be used for analytics and product improvement.</p>

      <h3 className="text-xl font-semibold mt-6">Legal basis and sharing</h3>
      <p className="text-muted-foreground">For users in applicable jurisdictions we rely on consent and contract fulfilment as our legal basis. We may share data with trusted service providers (hosting, monitoring, payment processors) under strict contracts. We do not sell personal data.</p>

      <h3 className="text-xl font-semibold mt-6">Third-party services and integrations</h3>
      <p className="text-muted-foreground">TariffSheriff may integrate with third-party services (ERP, customs data providers). Those providers may process data under their own policies — review provider documentation for details.</p>

      <h3 className="text-xl font-semibold mt-6">Data retention</h3>
      <p className="text-muted-foreground">We retain personal data for as long as necessary to provide the service, comply with legal obligations, and resolve disputes. Customers may request deletion of their data; contact us via the Contact page.</p>

      <h3 className="text-xl font-semibold mt-6">Security</h3>
      <p className="text-muted-foreground">We implement administrative, technical, and physical safeguards to protect data. Data in transit is encrypted with TLS. Access to production systems is logged and restricted to authorized personnel.</p>

      <h3 className="text-xl font-semibold mt-6">Your rights</h3>
      <p className="text-muted-foreground">Depending on your jurisdiction, you may have rights to access, correct, or delete personal data. To exercise these rights, contact us via the Contact page.</p>

      <h3 className="text-xl font-semibold mt-6">Contact</h3>
      <p className="text-muted-foreground">For questions about this policy or data requests, please contact us via the project GitHub or the Contact page.</p>

      <h3 className="text-xl font-semibold mt-6">Policy updates</h3>
      <p className="text-muted-foreground">We may update this policy from time to time. Material changes will be communicated via the application and documented in the project repository.</p>
    </div>
  )
}
