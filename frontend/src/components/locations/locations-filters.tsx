import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { useEffect, useState } from "react";

interface LocationFiltersProps {
  onSubmit: (data: { countryCode: string; partyId: string }) => void;
  isLoading?: boolean;
  initialValues: { countryCode: string; partyId: string };
}

export function LocationFilters({
  onSubmit,
  isLoading,
  initialValues,
}: LocationFiltersProps) {
  const [countryCode, setCountryCode] = useState(initialValues.countryCode);
  const [partyId, setPartyId] = useState(initialValues.partyId);

  // Update local state when initialValues change
  useEffect(() => {
    setCountryCode(initialValues.countryCode);
    setPartyId(initialValues.partyId);
  }, [initialValues]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      countryCode: countryCode.trim(),
      partyId: partyId.trim(),
    });
  };

  const handleCountryCodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.toUpperCase().trim();
    setCountryCode(value);
  };

  const handlePartyIdChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.trim();
    setPartyId(value);
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-4 p-4 bg-card rounded-lg border"
    >
      <div className="grid gap-4 md:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="countryCode">Country Code</Label>
          <Input
            id="countryCode"
            placeholder="e.g., NL"
            value={countryCode}
            onChange={handleCountryCodeChange}
            maxLength={2}
            className="uppercase"
            required
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="partyId">Party ID</Label>
          <Input
            id="partyId"
            placeholder="e.g., CPO1"
            value={partyId}
            onChange={handlePartyIdChange}
            required
          />
        </div>
      </div>
      <Button
        type="submit"
        className="w-full"
        disabled={isLoading || !countryCode || !partyId}
      >
        {isLoading ? "Loading..." : "Load Locations"}
      </Button>
    </form>
  );
}
