import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { CountryFlag } from '@/components/ui/CountryFlag';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { Users, TrendingUp, Globe } from 'lucide-react';
import { formatCurrency } from '@/lib/utils';

interface CountryTradePartnersProps {
    countryCode: string;
}

// Mock trade partners data
const generateTradePartners = (_countryCode: string) => [
    { code: 'US', name: 'United States', volume: 2400000000, share: 28.5, change: '+12.3%', type: 'both' },
    { code: 'CN', name: 'China', volume: 1800000000, share: 21.4, change: '+8.7%', type: 'both' },
    { code: 'DE', name: 'Germany', volume: 1200000000, share: 14.2, change: '+5.2%', type: 'export' },
    { code: 'JP', name: 'Japan', volume: 980000000, share: 11.6, change: '-2.1%', type: 'import' },
    { code: 'GB', name: 'United Kingdom', volume: 750000000, share: 8.9, change: '+15.4%', type: 'both' },
    { code: 'FR', name: 'France', volume: 620000000, share: 7.4, change: '+3.8%', type: 'export' },
    { code: 'IT', name: 'Italy', volume: 450000000, share: 5.3, change: '+7.2%', type: 'both' },
    { code: 'KR', name: 'South Korea', volume: 320000000, share: 3.8, change: '+9.1%', type: 'import' }
];

const pieData = [
    { name: 'United States', value: 28.5, color: '#3b82f6' },
    { name: 'China', value: 21.4, color: '#ef4444' },
    { name: 'Germany', value: 14.2, color: '#f59e0b' },
    { name: 'Japan', value: 11.6, color: '#10b981' },
    { name: 'United Kingdom', value: 8.9, color: '#8b5cf6' },
    { name: 'Others', value: 15.4, color: '#6b7280' }
];

export const CountryTradePartners: React.FC<CountryTradePartnersProps> = ({ countryCode }) => {
    const partners = generateTradePartners(countryCode);
    const totalVolume = partners.reduce((sum, partner) => sum + partner.volume, 0);

    return (
        <div className="grid gap-6 lg:grid-cols-2">
            {/* Trade Partners List */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center">
                        <Users className="w-5 h-5 mr-2" />
                        Top Trade Partners
                    </CardTitle>
                    <CardDescription>
                        Countries by trade volume and growth
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="space-y-4">
                        {partners.map((partner, index) => (
                            <div key={partner.code} className="flex items-center justify-between p-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 rounded-lg transition-colors">
                                <div className="flex items-center space-x-3">
                                    <div className="text-sm font-medium text-muted-foreground w-6">
                                        #{index + 1}
                                    </div>
                                    <CountryFlag countryCode={partner.code} size="md" />
                                    <div>
                                        <div className="font-medium">{partner.name}</div>
                                        <div className="text-sm text-muted-foreground">
                                            {formatCurrency(partner.volume, 'USD')} • {partner.share}%
                                        </div>
                                    </div>
                                </div>
                                <div className="text-right space-y-1">
                                    <Badge
                                        variant={partner.change.startsWith('+') ? 'success' : 'destructive'}
                                        className="text-xs"
                                    >
                                        {partner.change}
                                    </Badge>
                                    <div className="flex space-x-1">
                                        {partner.type === 'both' && (
                                            <>
                                                <Badge variant="outline" className="text-xs">Import</Badge>
                                                <Badge variant="outline" className="text-xs">Export</Badge>
                                            </>
                                        )}
                                        {partner.type === 'import' && (
                                            <Badge variant="secondary" className="text-xs">Import</Badge>
                                        )}
                                        {partner.type === 'export' && (
                                            <Badge variant="secondary" className="text-xs">Export</Badge>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="mt-6 p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
                        <div className="flex items-center justify-between">
                            <div>
                                <div className="font-medium text-green-900 dark:text-green-100">
                                    Total Trade Volume
                                </div>
                                <div className="text-sm text-green-700 dark:text-green-300">
                                    All partners combined
                                </div>
                            </div>
                            <div className="text-2xl font-bold text-green-600 dark:text-green-400">
                                {formatCurrency(totalVolume, 'USD')}
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Trade Share Visualization */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center">
                        <Globe className="w-5 h-5 mr-2" />
                        Trade Share Distribution
                    </CardTitle>
                    <CardDescription>
                        Market share by trading partner
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <ResponsiveContainer width="100%" height={300}>
                        <PieChart>
                            <Pie
                                data={pieData}
                                cx="50%"
                                cy="50%"
                                outerRadius={100}
                                fill="#8884d8"
                                dataKey="value"
                                label={({ name, value }) => `${name}: ${value}%`}
                                labelLine={false}
                            >
                                {pieData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={entry.color} />
                                ))}
                            </Pie>
                            <Tooltip
                                formatter={(value: number) => [`${value}%`, 'Market Share']}
                                contentStyle={{
                                    backgroundColor: 'hsl(var(--card))',
                                    border: '1px solid hsl(var(--border))',
                                    borderRadius: '8px'
                                }}
                            />
                        </PieChart>
                    </ResponsiveContainer>

                    <div className="mt-4 grid grid-cols-2 gap-2">
                        {pieData.slice(0, 4).map((item, index) => (
                            <div key={index} className="flex items-center space-x-2 text-sm">
                                <div
                                    className="w-3 h-3 rounded-full"
                                    style={{ backgroundColor: item.color }}
                                />
                                <span className="text-muted-foreground">{item.name}</span>
                            </div>
                        ))}
                    </div>

                    <div className="mt-4 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                        <div className="flex items-start space-x-2">
                            <TrendingUp className="w-4 h-4 text-blue-600 dark:text-blue-400 mt-0.5" />
                            <div className="text-sm">
                                <div className="font-medium text-blue-900 dark:text-blue-100">
                                    Growth Trend
                                </div>
                                <div className="text-blue-700 dark:text-blue-300">
                                    Most partners showing positive growth this quarter
                                </div>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
};