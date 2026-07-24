import { useEffect } from "react";
import { App } from "@capacitor/app";
import { useNavigate, useLocation } from "react-router-dom";

export const useHardwareBackButton = () => {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    let active = true;

    const setupListener = async () => {
      const listener = await App.addListener("backButton", (data) => {
        if (!active) return;

        // Paths where clicking "Back" should exit the application rather than route backward
        const isRootPath =
          location.pathname === "/" ||
          location.pathname === "/dashboard" ||
          location.pathname === "/login" ||
          location.pathname === "/caregiver/dashboard" ||
          location.pathname === "/doctor/dashboard" ||
          location.pathname === "/admin/dashboard";

        if (isRootPath || !data.canGoBack) {
          App.exitApp();
        } else {
          navigate(-1);
        }
      });

      return listener;
    };

    const listenerPromise = setupListener();

    return () => {
      active = false;
      listenerPromise.then((listener) => {
        listener.remove();
      });
    };
  }, [location.pathname, navigate]);
};
