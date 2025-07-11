import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useState } from "react";
import { Switch } from "../ui/switch";

export interface DefaultSupplier {
  supplier_market_partner_id: string;
  bkv_id: string;
  balancing_group_eic_id: string;
}

interface LocationEditFormData {
  // Location identification fields
  marketLocationId: string;  // This is the Malo ID
  meteringLocationId: string;
  
  // Smart meter fields
  smartMeterId: string;
  messageQueueUrl: string;
  
  // Partner IDs
  dsoMarketPartnerId: string;
  tsoMarketPartnerId: string;
  mpoMarketPartnerId: string;
  
  // Other fields
  meteringDataSource: "MSCONS" | "SMART_METER" | "COUNTROL_BACKEND";
  defaultSupplier: DefaultSupplier;
  published: boolean;
}

interface LocationEditFormProps {
  initialData?: Partial<LocationEditFormData>;
  onSubmit: (data: LocationEditFormData) => void;
  isLoading?: boolean;
}

export function LocationEditForm({
  initialData,
  onSubmit,
  isLoading,
}: LocationEditFormProps) {
  const [formData, setFormData] = useState<LocationEditFormData>({
    marketLocationId: initialData?.marketLocationId || "",
    meteringLocationId: initialData?.meteringLocationId || "",
    smartMeterId: initialData?.smartMeterId || "",
    messageQueueUrl: initialData?.messageQueueUrl || "",
    dsoMarketPartnerId: initialData?.dsoMarketPartnerId || "",
    tsoMarketPartnerId: initialData?.tsoMarketPartnerId || "",
    mpoMarketPartnerId: initialData?.mpoMarketPartnerId || "",
    meteringDataSource: initialData?.meteringDataSource || "SMART_METER",
    defaultSupplier: initialData?.defaultSupplier || {
      supplier_market_partner_id: "",
      bkv_id: "",
      balancing_group_eic_id: ""
    },
    published: initialData?.published || false
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="">
          <h3 className="text-lg font-medium mb-4">Metering Information</h3>
        </div>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="marketLocationId">Marktlokation-ID (MaLo)</Label>
          <Input
            id="marketLocationId"
            value={formData.marketLocationId}
            onChange={(e) =>
              setFormData((prev) => ({
                ...prev,
                marketLocationId: e.target.value,
              }))
            }
            placeholder="Enter Marktlokation-ID"
            required
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="meteringLocationId">Messlokation-ID (MeLo)</Label>
          <Input
            id="meteringLocationId"
            value={formData.meteringLocationId}
            onChange={(e) =>
              setFormData((prev) => ({
                ...prev,
                meteringLocationId: e.target.value,
              }))
            }
            placeholder="Enter Messlokation-ID"
            required
          />
        </div>
      </div>

      {/* Smart Meter Section */}
      <div className="mt-6">
        {/* Data Source */}
      <div className="space-y-2">
        <Label htmlFor="meteringDataSource">Quelle der Messdaten</Label>
        <Select
          value={formData.meteringDataSource}
          onValueChange={(value: "MSCONS" | "SMART_METER" | "COUNTROL_BACKEND") =>
            setFormData((prev) => ({ ...prev, meteringDataSource: value }))
          }
        >
          <SelectTrigger id="meteringDataSource">
            <SelectValue placeholder="Select data source" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="MSCONS">MSCONS</SelectItem>
            <SelectItem value="SMART_METER">Smart Meter</SelectItem>
            <SelectItem value="COUNTROL_BACKEND">Control Backend</SelectItem>
          </SelectContent>
        </Select>
      </div>

      </div>

      <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="smartMeterId">Smart Meter ID</Label>
            <Input
              id="smartMeterId"
              value={formData.smartMeterId}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  smartMeterId: e.target.value,
                }))
              }
              placeholder="Enter Smart Meter ID"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="messageQueueUrl">Message Queue URL</Label>
            <Input
              id="messageQueueUrl"
              value={formData.messageQueueUrl}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  messageQueueUrl: e.target.value,
                }))
              }
              placeholder="Enter Message Queue URL"
            />
          </div>
        </div>

      {/* Partner IDs */}
      <div className="grid gap-4 md:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="dsoMarketPartnerId">Verteilnetzbetreiber Partner-ID</Label>
          <Input
            id="dsoMarketPartnerId"
            value={formData.dsoMarketPartnerId}
            onChange={(e) =>
              setFormData((prev) => ({
                ...prev,
                dsoMarketPartnerId: e.target.value,
              }))
            }
            placeholder="Enter Verteilnetzbetreiber Partner-ID"
            required
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="tsoMarketPartnerId">Übertragungsnetzbetreiber Partner-ID</Label>
          <Input
            id="tsoMarketPartnerId"
            value={formData.tsoMarketPartnerId}
            onChange={(e) =>
              setFormData((prev) => ({
                ...prev,
                tsoMarketPartnerId: e.target.value,
              }))
            }
            placeholder="Enter Übertragungsnetzbetreiber Partner-ID"
            required
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="mpoMarketPartnerId">Messstellenbetreiber Partner-ID</Label>
          <Input
            id="mpoMarketPartnerId"
            value={formData.mpoMarketPartnerId}
            onChange={(e) =>
              setFormData((prev) => ({
                ...prev,
                mpoMarketPartnerId: e.target.value,
              }))
            }
            placeholder="Enter Messstellenbetreiber Partner-ID"
            required
          />
        </div>
      </div>

      {/* Default Supplier Section */}
      <div className="mt-6">
        <h3 className="text-lg font-medium mb-4">Default Supplier</h3>
        <div className="grid gap-4 md:grid-cols-3">
          <div className="space-y-2">
            <Label htmlFor="supplierMarketPartnerId">Default Lieferant-ID</Label>
            <Input
              id="supplierMarketPartnerId"
              value={formData.defaultSupplier.supplier_market_partner_id}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  defaultSupplier: {
                    ...prev.defaultSupplier,
                    supplier_market_partner_id: e.target.value,
                  },
                }))
              }
              placeholder="Enter Default Lieferant-ID"
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="bkvId">Bilanzkreisverantwortlicher Partner-ID</Label>
            <Input
              id="bkvId"
              value={formData.defaultSupplier.bkv_id}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  defaultSupplier: {
                    ...prev.defaultSupplier,
                    bkv_id: e.target.value,
                  },
                }))
              }
              placeholder="Enter BKV Partner-ID"
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="balancingGroupEicId">Bilanzkreis EIC-ID</Label>
            <Input
              id="balancingGroupEicId"
              value={formData.defaultSupplier.balancing_group_eic_id}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  defaultSupplier: {
                    ...prev.defaultSupplier,
                    balancing_group_eic_id: e.target.value,
                  },
                }))
              }
              placeholder="Enter Bilanzkreis EIC-ID"
              required
            />
          </div>
        </div>
      </div>

      {/* Availability Section */}
      <div className="mt-6">
        <h3 className="text-lg font-medium mb-4">Status</h3>
        <div className="flex items-center space-x-2">
          <Switch
            id="published"
            checked={formData.published}
            onCheckedChange={(checked) =>
              setFormData((prev) => ({ ...prev, published: checked }))
            }
          />
          <Label htmlFor="published">Published</Label>
        </div>
      </div>

      {/* Submit Button */}
      <div className="flex justify-end mt-6">
        <Button type="submit" disabled={isLoading}>
          {isLoading ? "Speichern..." : "Speichern"}
        </Button>
      </div>
    </form>
  );
}
