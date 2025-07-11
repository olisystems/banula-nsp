import { getLocation, saveLocation } from "@/services/api/locations";
import { LocationEditForm } from "../components/locations/location-edit-form";
import { useNavigate, useParams } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { SmartLocation } from "@/types/location";

export function EditLocation() {
  const { id, countryCode, partyId } = useParams();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const {
    data: location,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["location", id],
    queryFn: () =>
      id
        ? getLocation(id, {
            countryCode: countryCode || "",
            partyId: partyId || "",
          })
        : Promise.reject("No ID provided"),
    enabled: !!id,
    networkMode: "always",
    staleTime: 0,
    refetchOnMount: "always", // Force refetch when component mounts
  });

  const mutation = useMutation({
    mutationFn: (formData: {
      marketLocationId: string;
      meteringLocationId: string;
      smartMeterId: string;
      messageQueueUrl: string;
      dsoMarketPartnerId: string;
      tsoMarketPartnerId: string;
      mpoMarketPartnerId: string;
      meteringDataSource: "MSCONS" | "SMART_METER" | "COUNTROL_BACKEND";
      defaultSupplier: {
        supplier_market_partner_id: string;
        bkv_id: string;
        balancing_group_eic_id: string;
      };
      published: boolean;
    }) => {
      if (!id) throw new Error("No ID provided");
      
      const smartLocation: Partial<SmartLocation> = {
        id,
        malo: formData.marketLocationId, // Using marketLocationId as malo
        smart_meter_id: formData.smartMeterId,
        message_queue_url: formData.messageQueueUrl,
        publish: formData.published,
        market_location_id: formData.marketLocationId,
        metering_location_id: formData.meteringLocationId,
        dso_market_partner_id: formData.dsoMarketPartnerId,
        tso_market_partner_id: formData.tsoMarketPartnerId,
        mpo_market_partner_id: formData.mpoMarketPartnerId,
        metering_data_source: formData.meteringDataSource,
        default_supplier: formData.defaultSupplier
      };
      
      return saveLocation(
        {
          countryCode: countryCode || "",
          partyId: partyId || "",
        },
        smartLocation
      );
    },
    onSuccess: () => {
      // Invalidate locations query to force a refetch
      queryClient.invalidateQueries({ queryKey: ["location"] }).then(() => {
        // Then navigate back
        navigate(-1);
      });
    },
  });

  if (isError) {
    return (
      <div className="space-y-6">
        <Button variant="ghost" className="mb-4" onClick={() => navigate(-1)}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Locations
        </Button>
        <div className="rounded-lg border bg-destructive/10 text-destructive p-4">
          <h2 className="text-lg font-semibold">Error Loading Location</h2>
          <p className="mt-2">{error?.message || "Unknown error occurred"}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" className="mb-4" onClick={() => navigate(-1)}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to Locations
      </Button>

      <div>
        <h1 className="text-3xl font-bold">
          {isLoading ? <Skeleton className="h-9 w-48" /> : "Edit Location"}
        </h1>
        <p className="text-muted-foreground mt-2">
          {isLoading ? (
            <Skeleton className="h-6 w-96" />
          ) : (
            "Update location information and settings"
          )}
        </p>
      </div>

      <div className="rounded-lg border bg-card">
        <div className="p-6">
          {isLoading ? (
            <div className="space-y-6">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : (
            <LocationEditForm
              initialData={{
                marketLocationId: location?.market_location_id || "",
                meteringLocationId: location?.metering_location_id || "",
                smartMeterId: location?.smart_meter_id || "",
                messageQueueUrl: location?.message_queue_url || "",
                dsoMarketPartnerId: location?.dso_market_partner_id || "",
                tsoMarketPartnerId: location?.tso_market_partner_id || "",
                mpoMarketPartnerId: location?.mpo_market_partner_id || "",
                meteringDataSource: location?.metering_data_source || "SMART_METER",
                defaultSupplier: location?.default_supplier || {
                  supplier_market_partner_id: "",
                  bkv_id: "",
                  balancing_group_eic_id: ""
                },
                published: location?.publish || false
              }}
              onSubmit={(data) => mutation.mutate(data)}
              isLoading={mutation.isPending}
            />
          )}
        </div>
      </div>
    </div>
  );
}
