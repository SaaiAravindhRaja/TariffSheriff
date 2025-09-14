import React from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { User, Mail, Briefcase, MapPin } from 'lucide-react'

export default function Profile() {
  return (
    <div className="p-8 max-w-4xl mx-auto">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-4">
              <div className="w-20 h-20 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white text-2xl">
                <User className="w-8 h-8" />
              </div>
              <div>
                <CardTitle className="text-2xl">John Doe</CardTitle>
                <CardDescription>Trade Analyst</CardDescription>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Button variant="secondary">Edit Profile</Button>
              <Button>Sign Out</Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <h3 className="text-lg font-semibold">Contact</h3>
              <div className="flex items-center gap-3 text-sm text-muted-foreground">
                <Mail className="w-4 h-4" />
                <div>
                  <div className="font-medium">john.doe@example.com</div>
                  <div className="text-xs">Primary email</div>
                </div>
              </div>
              <div className="flex items-center gap-3 text-sm text-muted-foreground">
                <MapPin className="w-4 h-4" />
                <div>
                  <div className="font-medium">New York, USA</div>
                  <div className="text-xs">Location</div>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <h3 className="text-lg font-semibold">About</h3>
              <p className="text-sm text-muted-foreground">
                John is a Trade Analyst with a focus on tariff policy and supply chain impacts. He helps teams
                understand tariff exposure, compliance risks, and identifies cost optimization opportunities.
              </p>
              <div className="mt-4">
                <h4 className="text-sm font-medium">Role</h4>
                <div className="text-sm text-muted-foreground flex items-center gap-2"><Briefcase className="w-4 h-4" /> Trade Analyst</div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
