const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({region: "southamerica-east1"});

exports.notifyNewAppointment = onDocumentCreated("appointments/{id}", async (event) => {
  const appt = event.data.data();
  try {
    const profile = await admin.firestore().collection("profiles").document(appt.salonId).get();
    if (!profile.exists || !profile.data().fcmToken) return null;

    const message = {
      notification: {
        title: "Novo Agendamento! 📅",
        body: `${appt.clientName} agendou ${appt.serviceName}.`,
      },
      token: profile.data().fcmToken,
      android: {
        priority: "high",
        notification: {channelId: "agendamentos_channel"},
      },
    };

    await admin.messaging().send(message);
  } catch (e) {
    console.error(e);
  }
  return null;
});

exports.notifyStatusChanged = onDocumentUpdated("appointments/{id}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();

  if (before.status !== "CONFIRMED" && after.status === "CONFIRMED") {
    try {
      const profile = await admin.firestore().collection("profiles").document(after.clientUid).get();
      const salon = await admin.firestore().collection("salons").document(after.salonId).get();
      const salonName = salon.exists ? salon.data().name : "O Salão";

      if (!profile.exists || !profile.data().fcmToken) return null;

      const message = {
        notification: {
          title: "Agendamento Confirmado! ✅",
          body: `${salonName} aceitou seu horário de ${after.serviceName}.`,
        },
        token: profile.data().fcmToken,
        android: {
          priority: "high",
          notification: {channelId: "agendamentos_channel"},
        },
      };

      await admin.messaging().send(message);
    } catch (e) {
      console.error(e);
    }
  }
  return null;
});
