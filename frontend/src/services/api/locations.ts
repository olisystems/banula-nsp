import { Location, SmartLocation } from "@/types/location";

interface GetLocationsParams {
  countryCode: string;
  partyId: string;
}

interface LocationsResponse {
  data: SmartLocation[];
}

export async function getLocations({
  countryCode,
  partyId,
}: GetLocationsParams): Promise<Location[]> {
  const url =
    process.env.serviceUrl +
    (process.env.nonOcpiPath || "") +
    `/locations/${countryCode}/${partyId}`;
  const locationsRequest = await fetch(url, {
    method: "GET",
    headers: {
      authorization: "Token " + btoa(process.env.tokenB || ""),
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
  });

  if (locationsRequest.ok) {
    const locationsData = (await locationsRequest.json()) as LocationsResponse;
    return locationsData.data;
  }

  throw Error("Could not fetch locations");
}

export async function getLocation(
  id: string,
  { countryCode, partyId }: GetLocationsParams,
) {
  const url =
    process.env.serviceUrl +
    (process.env.nonOcpiPath || "") +
    `/locations/${countryCode}/${partyId}/${id}`;
  const locationRequest = await fetch(url, {
    method: "GET",
    headers: {
      authorization: "Token " + btoa(process.env.tokenB || ""),
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
  });

  if (!locationRequest.ok) {
    throw new Error("Could not fetch location");
  }

  const locationData = await locationRequest.json();
  return locationData.data as SmartLocation;
}

export async function saveLocation(
  { countryCode, partyId }: GetLocationsParams,
  location: Partial<SmartLocation>,
): Promise<void> {
  const url =
    process.env.serviceUrl +
    (process.env.nonOcpiPath || "") +
    `/locations/${countryCode}/${partyId}/${location.id}`;
  const locationsRequest = await fetch(url, {
    method: "POST",
    headers: {
      authorization: "Token " + btoa(process.env.tokenB || ""),
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
    body: JSON.stringify(location),
  });

  if (!locationsRequest.ok) {
    throw Error("Could not save location");
  }
}
