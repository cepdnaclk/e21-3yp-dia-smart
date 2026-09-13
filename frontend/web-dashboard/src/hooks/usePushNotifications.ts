import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Capacitor } from "@capacitor/core";
import { PushNotifications } from "@capacitor/push-notifications";
import { pushNotificationService } from "../services/pushNotificationService";

export function usePushNotifications() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) {
      return;
    }

    const setupPushNotifications = async () => {
      try {
        let permStatus = await PushNotifications.checkPermissions();

        if (permStatus.receive === "prompt") {
          permStatus = await PushNotifications.requestPermissions();
        }

        if (permStatus.receive !== "granted") {
          console.warn("Push notification permission not granted.");
          return;
        }

        await PushNotifications.register();
      } catch (error) {
        console.error("Error setting up push notifications:", error);
      }
    };

    const registrationListener = PushNotifications.addListener(
      "registration",
      async (token) => {
        try {
          await pushNotificationService.registerDeviceToken(
            token.value,
            Capacitor.getPlatform()
          );
        } catch (error) {
          console.error("Failed to submit FCM token to backend:", error);
        }
      }
    );

    const registrationErrorListener = PushNotifications.addListener(
      "registrationError",
      (error) => {
        console.error("Push notification registration error:", error);
      }
    );

    const notificationReceivedListener = PushNotifications.addListener(
      "pushNotificationReceived",
      (notification) => {
        console.log("Push notification received in foreground:", notification);
      }
    );

    const notificationActionPerformedListener = PushNotifications.addListener(
      "pushNotificationActionPerformed",
      (action) => {
        console.log("Push notification action performed:", action);
        navigate("/alerts");
      }
    );

    setupPushNotifications();

    return () => {
      registrationListener.then((l) => l.remove());
      registrationErrorListener.then((l) => l.remove());
      notificationReceivedListener.then((l) => l.remove());
      notificationActionPerformedListener.then((l) => l.remove());
    };
  }, [navigate]);
}
