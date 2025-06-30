import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../ui/table";
import { Location } from "../../types/location";
import { cn } from "../../lib/utils";
import { Link } from "react-router";

interface LocationsTableProps {
  locations: Location[];
  countryCode: string;
  partyId: string;
}

export function LocationsTable({
  locations,
  countryCode,
  partyId,
}: LocationsTableProps) {
  return (
    <div className="w-full">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead className="w-[50px]"></TableHead>
            <TableHead className="w-[200px]">Name</TableHead>
            <TableHead className="w-[250px]">Address</TableHead>
            <TableHead className="w-[150px]">City</TableHead>
            <TableHead className="w-[120px]">Status</TableHead>
            <TableHead className="w-[100px]">EVSE ID</TableHead>
            <TableHead className="w-[100px]">Connector</TableHead>
            <TableHead className="w-[100px] text-right">Last Updated</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {locations.map((location) => (
            <TableRow
              key={location.id}
              className="cursor-pointer hover:bg-muted/50"
            >
              <TableCell className="font-medium">
                <Link
                  to={
                    "/locations/" +
                    countryCode +
                    "/" +
                    partyId +
                    "/" +
                    location.id +
                    "/edit"
                  }
                >
                  {" "}
                  Edit{" "}
                </Link>
              </TableCell>
              <TableCell className="font-medium">{location.name}</TableCell>
              <TableCell className="text-muted-foreground">
                {location.address}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {location.city}
              </TableCell>
              <TableCell>
                <span
                  className={cn(
                    "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium",
                    {
                      "bg-green-50 text-green-700 border border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-900/30":
                        location.publish,
                      "bg-red-50 text-red-700 border border-red-200 dark:bg-red-900/20 dark:text-red-400 dark:border-red-900/30":
                        !location.publish,
                    },
                  )}
                >
                  {location.publish ? "Published" : "Draft"}
                </span>
              </TableCell>
              <TableCell className="text-left font-medium">
                <ul className="list-none">
                  {location.evses?.map((x) => <li> {x.evse_id} </li>)}
                </ul>
              </TableCell>
              <TableCell className="text-left font-medium">
                <ul className="list-none">
                  {location.evses?.map((x) =>
                    x.connectors?.map((y) => <li> {y.id} </li>),
                  )}
                </ul>
              </TableCell>
              <TableCell className="text-right text-muted-foreground">
                {location.last_updated
                  ? new Date(location.last_updated).toLocaleDateString()
                  : "-"}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
