import { createBrowserRouter } from "react-router";
import { DefaultLayout } from "./components/layouts/default-layout";
import { Home } from "./pages/home";
import { Locations } from "./pages/locations";
import { EditLocation } from "./pages/edit-location";
import NotFound from "./pages/not-found";

export const router = createBrowserRouter(
  [
    {
      element: <DefaultLayout />,
      children: [
        {
          path: "/",
          element: <Home />,
        },
        {
          path: "/locations",
          children: [
            {
              path: "",
              element: <Locations />,
            },
            {
              path: ":countryCode/:partyId/:id/edit",
              element: <EditLocation />,
            },
          ],
        },
      ],
    },
    {
      path: "*",
      element: <NotFound />,
    },
  ],
  {
    basename: process.env.routerBasename,
  },
);
