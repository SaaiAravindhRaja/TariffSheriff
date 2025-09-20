import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  ArrowLeft, 
  TrendingUp, 
  DollarSign, 
  Globe,
  BarChart3,
  MapPin,
  Users,
  Building2
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useCountry } from '@/hooks/useCountries';
import { CountryFlag } from '@/components/ui/CountryFlag';
import { CountryTradeChart } from '@/components/country/CountryTradeChart';
import { CountryTariffRates } from '@/components/country/CountryTariffRates';
import { CountryTradePartners } from '@/components/country/CountryTradePartners';
import { CountryProductCategories } from '@/components/country/CountryProductCategories';
import { CountryTradeAgreements } from '@/components/country/CountryTradeAgreements';
import { CountryEconomicIndicators } from '@/components/country/CountryEconomicIndicators';

export function CountryDashboard() {
  const { countryCode } = useParams<{ countryCode: string }>();
  const navigate = useNavigate();
  const country = useCountry(countryCode?.toUpperCase());

  if (!countryCode) {
    navigate('/');
    return null;
  }

  if (!country) {
    return (
      <div className="flex-1 p-6">
        <div className="text-center py-12">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            Country Not Found
          </h1>
          <p className="text-gray-600 dark:text-gray-400 mt-2">
            The country code "{countryCode}" was not found in our database.
          </p>
          <Button 
            onClick={() => navigate('/')} 
            className="mt-4"
            variant="outline"
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Dashboard
          </Button>
        </div>
      </div>
    );
  }

  // Mock data - replace with real API calls
  const countryStats = {
    totalTradeVolume: '$2.4B',
    avgTariffRate: '8.5%',
    activeRoutes: 156,
    tradePartners: 89,
    gdp: '$1.2T',
    population: '67.4M',
    tradeBalance: '+$45.2B',
    lastUpdated: new Date().toISOString()
  };

  return (
    <div className="flex-1 space-y-6 p-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center justify-between"
      >
        <div className="flex items-center space-x-4">
          <Button 
            variant="ghost" 
            size="icon"
            onClick={() => navigate(-1)}
            className="hover:bg-gray-100 dark:hover:bg-gray-800"
          >
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <div className="flex items-center space-x-3">
            <CountryFlag countryCode={country.code} size="lg" />
            <div>
              <h1 className="text-3xl font-bold tracking-tight">{country.name}</h1>
              <div className="flex items-center space-x-4 text-sm text-muted-foreground">
                <span className="flex items-center">
                  <MapPin className="w-4 h-4 mr-1" />
                  {country.region}
                </span>
                <span className="flex items-center">
                  <DollarSign className="w-4 h-4 mr-1" />
                  {country.currency}
                </span>
                <Badge variant="secondary">{country.code}</Badge>
              </div>
            </div>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline">
            Export Data
          </Button>
          <Button variant="gradient">
            Calculate Tariff
          </Button>
        </div>
      </motion.div>

      {/* Key Metrics */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[
          {
            title: 'Trade Volume',
            value: countryStats.totalTradeVolume,
            change: '+12.5%',
            icon: BarChart3,
            trend: 'up'
          },
          {
            title: 'Avg Tariff Rate',
            value: countryStats.avgTariffRate,
            change: '-2.1%',
            icon: TrendingUp,
            trend: 'down'
          },
          {
            title: 'Active Routes',
            value: countryStats.activeRoutes.toString(),
            change: '+8.3%',
            icon: Globe,
            trend: 'up'
          },
          {
            title: 'Trade Partners',
            value: countryStats.tradePartners.toString(),
            change: '+5.7%',
            icon: Users,
            trend: 'up'
          }
        ].map((stat, index) => {
          const Icon = stat.icon;
          const isPositive = stat.trend === 'up';
          
          return (
            <motion.div
              key={stat.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
            >
              <Card className="card-hover">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">
                    {stat.title}
                  </CardTitle>
                  <Icon className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{stat.value}</div>
                  <p className={`text-xs ${
                    isPositive ? 'text-success-600' : 'text-danger-600'
                  }`}>
                    {stat.change} from last month
                  </p>
                </CardContent>
              </Card>
            </motion.div>
          );
        })}
      </div>

      {/* Economic Overview */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.4 }}
      >
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Building2 className="w-5 h-5 mr-2" />
              Economic Overview
            </CardTitle>
            <CardDescription>
              Key economic indicators for {country.name}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-3">
              <div className="text-center p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                  {countryStats.gdp}
                </div>
                <div className="text-sm text-muted-foreground">GDP</div>
              </div>
              <div className="text-center p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
                <div className="text-2xl font-bold text-green-600 dark:text-green-400">
                  {countryStats.population}
                </div>
                <div className="text-sm text-muted-foreground">Population</div>
              </div>
              <div className="text-center p-4 bg-purple-50 dark:bg-purple-900/20 rounded-lg">
                <div className="text-2xl font-bold text-purple-600 dark:text-purple-400">
                  {countryStats.tradeBalance}
                </div>
                <div className="text-sm text-muted-foreground">Trade Balance</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </motion.div>

      {/* Detailed Analytics Tabs */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.5 }}
      >
        <Tabs defaultValue="trade" className="space-y-4">
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="trade">Trade Data</TabsTrigger>
            <TabsTrigger value="tariffs">Tariff Rates</TabsTrigger>
            <TabsTrigger value="partners">Partners</TabsTrigger>
            <TabsTrigger value="products">Products</TabsTrigger>
            <TabsTrigger value="agreements">Agreements</TabsTrigger>
          </TabsList>

          <TabsContent value="trade" className="space-y-4">
            <CountryTradeChart countryCode={country.code} />
          </TabsContent>

          <TabsContent value="tariffs" className="space-y-4">
            <CountryTariffRates countryCode={country.code} />
          </TabsContent>

          <TabsContent value="partners" className="space-y-4">
            <CountryTradePartners countryCode={country.code} />
          </TabsContent>

          <TabsContent value="products" className="space-y-4">
            <CountryProductCategories countryCode={country.code} />
          </TabsContent>

          <TabsContent value="agreements" className="space-y-4">
            <CountryTradeAgreements countryCode={country.code} />
          </TabsContent>
        </Tabs>
      </motion.div>

      {/* Economic Indicators */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.6 }}
      >
        <CountryEconomicIndicators countryCode={country.code} />
      </motion.div>
    </div>
  );
}