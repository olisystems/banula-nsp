import { useQuery } from "@tanstack/react-query";
import { LocationsTable } from "../components/locations/locations-table";
import { LocationFilters } from "../components/locations/locations-filters";
import { getLocations } from "../services/api/locations";
import { Skeleton } from "../components/ui/skeleton";
import { useSearchParams } from "react-router";

export function Locations() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Get query params from URL
  const queryParams = {
    countryCode: searchParams.get("countryCode") || "",
    partyId: searchParams.get("partyId") || "",
  };

  const hasValidParams =
    queryParams.countryCode !== "" && queryParams.partyId !== "";

  const {
    data: locations,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["locations", queryParams.countryCode, queryParams.partyId],
    queryFn: () =>
      hasValidParams ? getLocations(queryParams) : Promise.reject("No params"),
    enabled: hasValidParams,
    networkMode: "always",
    staleTime: 0,
  });

  const handleFiltersSubmit = (params: {
    countryCode: string;
    partyId: string;
  }) => {
    setSearchParams(params);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Charging Locations</h1>
          <p className="text-muted-foreground mt-2">
            Manage and monitor your charging locations
          </p>
        </div>
      </div>

      <LocationFilters
        onSubmit={handleFiltersSubmit}
        isLoading={isLoading}
        initialValues={queryParams} // Pass the initial values from URL to the filters
      />

      {hasValidParams ? (
        <div className="rounded-lg border bg-card">
          <div className="p-6">
            {isLoading ? (
              <div className="space-y-4">
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
            ) : isError ? (
              <div className="text-center py-6 text-destructive">
                <p>Error loading locations</p>
                <p className="text-sm">{error?.message}</p>
              </div>
            ) : locations && locations.length > 0 ? (
              <LocationsTable
                locations={locations}
                countryCode={queryParams.countryCode}
                partyId={queryParams.partyId}
              />
            ) : (
              <div className="text-center py-6 text-muted-foreground">
                <p>No locations found for the selected criteria</p>
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="text-center py-12 text-muted-foreground border rounded-lg">
          <p>Please select a country code and party ID to view locations</p>
        </div>
      )}
    </div>
  );
}
